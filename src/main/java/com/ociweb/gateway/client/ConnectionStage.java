package com.ociweb.gateway.client;

import static com.ociweb.pronghorn.ring.RingBuffer.roomToLowLevelWrite;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.ring.RingReader;
import com.ociweb.pronghorn.ring.RingWriter;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class ConnectionStage extends PronghornStage {
	
	 final RingBuffer apiIn;
	 private final RingBuffer apiOut;
	 final RingBuffer idGenOut;
	 
	 private final int inFlightLimit;  //TODO: Where is this constraint enforced?
	 private SocketChannel channel;
	 private StringBuilder commonBuilder = new StringBuilder();
	
	 private byte state;	 
	 private static final byte STATE_DISCONNECTED = 0;
	 private static final byte STATE_CONNECTING = 1;
	 private static final byte STATE_CONNECTED = 2;
	 
	 
	 private ByteBuffer inputSocketBuffer;
	 private ByteBuffer DISCONNECT_MESSAGE;
	 private ByteBuffer CONNECT_MESSAGE;
	 private ByteBuffer PING_MESSAGE;
	 	 
	 private int[] CON_ACK_MSG;
	 private int prev = -100;
	 
	 private ByteBuffer[] pendingWriteBuffers;
	 
	 private ActivityAfterWrite pendingActivityAfterWrite;
	 
	 //Poly-morphic method call is needed so we can store the 
	 //next activity to be done while supporting non-blocking writes
	 //To minimize the cost however, no interface is used.
	 private ActivityAfterWrite AFTER_WRITE_DO_NOTHING;
	 private ActivityAfterWrite AFTER_WRITE_DO_DISCONNECT;
	 private ActivityAfterWrite AFTER_WRITE_CONTINUE_REPLAY;
	 private ActivityAfterWrite AFTER_WRITE_SET_DUP_BIT;
	        
     private int port;         

     //TODO: we must finish full publish and subcribe before teh 17th of aug.

	 //TLS     socket 8883
	 //non-tls socket 1883 
	 private InetSocketAddress addr;
	 
	 
	 //required to know when a ping must be sent to keep alive the connection
	 private long lastTimestamp; 
	 private int unconfirmedPings;   
	 
	  final int getIdMessageIdx = 0; 
	  final int genIdMessageSize; 
	  private final int maxAckMessageSize;   
	  
	 private int outstandingUnconfirmedMessages = 0;
	 
	 static Logger log = LoggerFactory.getLogger(ConnectionStage.class);
	 	 
	 // must be divisable by 4 and >=4 to evenly fit all the packets expected
	 // the biggest message is also 4 in length so this buffer will support
	 // parsing of many packets at once as long as its greater than 4.
	 private final static int INPUT_BUFFER_SIZE = 128;
     private static final byte DUP_BIT = 8;
     private final long timeLimitMS;
      
     
     PacketIdReleaseManager packetIdRelease;
     
     //debugging techniuqe that we should seriously consider moving into the framework its self.
     private int continueReason = 0;
     
	 static {
		 assert(INPUT_BUFFER_SIZE>=4) : "Must be >= 4";
		 assert(0==(INPUT_BUFFER_SIZE&0x3)) : "Must be divisable by 4";
	 }
	 
	//startup server
	//      java -Djavax.net.ssl.keyStore=mySrvKeystore -Djavax.net.ssl.keyStorePassword=123456 ServerApp
	//      
			//startup client 
	//      java -Djavax.net.ssl.trustStore=mySrvKeystore -Djavax.net.ssl.trustStorePassword=123456 ClientApp
	//      
			
	//      //debug
	//      -Djava.protocol.handler.pkgs=com.sun.net.ssl.internal.www.protocol -Djavax.net.debug=ssl
	 
	//TCP/IP port 1883 is reserved with IANA for use with MQTT. 
	//TCP/IP port 8883 is also registered, for using MQTT over SSL.
	 
	protected ConnectionStage(GraphManager graphManager, RingBuffer apiIn, 
			                                             RingBuffer apiOut, RingBuffer idGenOut, String rate, 
			                                             int inFlightLimit, int ttlSec, boolean secure, int port) {
		super(graphManager, 
				new RingBuffer[]{apiIn},
				new RingBuffer[]{apiOut,idGenOut});

		this.inFlightLimit = inFlightLimit;
		this.timeLimitMS = 1000 * ttlSec; //TODO: check spec this should send ping before this limit?
		
		this.port = port;
		
		this.apiIn = apiIn;
		this.apiOut = apiOut;
		this.idGenOut = idGenOut;   //use low level api, only 1 message type
				
		this.genIdMessageSize = RingBuffer.from(idGenOut).fragDataSize[getIdMessageIdx];
		this.maxAckMessageSize = computeMaxAckMessageSize(apiOut);
		
		//must keep re-setting this value
		RingBuffer.batchAllReleases(apiIn);
		
		GraphManager.addAnnotation(graphManager, GraphManager.SCHEDULE_RATE, rate, this);
		
	}
	
	
	private int computeMaxAckMessageSize(RingBuffer pipe) {
	    
	    int[] lookup = RingBuffer.from(pipe).fragDataSize;
	    int x = Math.max(lookup[ConOutConst.MSG_CON_OUT_PUB_REC], lookup[ConOutConst.MSG_CON_OUT_PUB_ACK]);
	    return Math.max(x, lookup[ConOutConst.MSG_CON_OUT_CONNACK_OK]);
    }



    @Override
	public void startup() {
        
        pendingWriteBuffers = new ByteBuffer[3];
        
        buildNewConnection(); 
		
		DISCONNECT_MESSAGE = ByteBuffer.allocate(2);
		DISCONNECT_MESSAGE.put((byte) 0xE0);
		DISCONNECT_MESSAGE.put((byte) 0x00);
		
		PING_MESSAGE = ByteBuffer.allocate(2);
		PING_MESSAGE.put((byte) 0xC0); //type/reserved 1100 0000
		PING_MESSAGE.put((byte) 0x00); //remaining length zero
		
		CON_ACK_MSG = new int[]{
                ConOutConst.MSG_CON_OUT_CONNACK_OK,
				ConOutConst.MSG_CON_OUT_CONNACK_PROTO,
				ConOutConst.MSG_CON_OUT_CONNACK_ID,
				ConOutConst.MSG_CON_OUT_CONNACK_SERVER,
				ConOutConst.MSG_CON_OUT_CONNACK_USER,
				ConOutConst.MSG_CON_OUT_CONNACK_AUTH 
				}; 
		
		
		//Connect message can be no bigger than the incoming pipe that holds it however, we could possibly make this a little smaller.
		CONNECT_MESSAGE = ByteBuffer.allocate(apiIn.sizeOfUntructuredLayoutRingBuffer);
		
		//input data can not be any bigger than the output pipe where messages will be sent back to the the caller, we could make this smaller
		inputSocketBuffer = ByteBuffer.allocate(apiOut.sizeOfUntructuredLayoutRingBuffer);
		
				
	     AFTER_WRITE_DO_NOTHING = new ActivityAfterWrite();
	     AFTER_WRITE_DO_DISCONNECT = new ActivityAfterWrite(){
	         public void doIt() {
	             try {
	                 channel.close();
	                 state = STATE_DISCONNECTED;
	                 clearTimestamp();
	             } catch (IOException e) {
	                 log.debug("Exception when disconnecting",e);
	             }
	             //restore the default behavior
	             pendingActivityAfterWrite = AFTER_WRITE_DO_NOTHING;
	         }
	     };
	     AFTER_WRITE_CONTINUE_REPLAY = new ActivityAfterWrite() {
	         public void doIt() {
	             //all the bytes were written
	             while (RingBuffer.isReplaying(apiIn) && RingReader.tryReadFragment(apiIn)) {
	                 
	                 int msgIdx = RingReader.getMsgIdx(apiIn);
	                 if (ConInConst.MSG_CON_IN_PUBLISH == msgIdx) { 
	                     int qos = RingReader.readInt(apiIn, ConInConst.CON_IN_PUBLISH_FIELD_QOS);
	                     if (qos>0) {//upon ack this qos is changed to <0 to mark that it has been sent.
	                         
	                        pendingWriteBuffers[0] = RingReader.wrappedUnstructuredLayoutBufferA(apiIn, ConInConst.CON_IN_PUBLISH_FIELD_PACKETDATA);
                            pendingWriteBuffers[1] = RingReader.wrappedUnstructuredLayoutBufferB(apiIn, ConInConst.CON_IN_PUBLISH_FIELD_PACKETDATA);
                            
                            assert(pendingWriteBuffers[0].remaining()>0): "The packed data must be found in the buffer";
                            //return ensures that we will come back in to run the next one after the above is written
                            return;                            
	                     }	                     
	                 } else if (ConInConst.MSG_CON_IN_PUB_REL == msgIdx) {
	                     if (RingReader.readInt(apiIn, ConInConst.CON_IN_PUB_REL_FIELD_PACKETID)>0) {
	                         
                            pendingWriteBuffers[0] = RingReader.wrappedUnstructuredLayoutBufferA(apiIn, ConInConst.CON_IN_PUB_REL_FIELD_PACKETDATA);
	                        pendingWriteBuffers[1] = RingReader.wrappedUnstructuredLayoutBufferB(apiIn, ConInConst.CON_IN_PUB_REL_FIELD_PACKETDATA);
	                          
	                        assert(pendingWriteBuffers[0].remaining()>0): "The packed data must be found in the buffer";
	                        //return ensures that we will come back in to run the next one after the above is written
	                        return;                            
	                         
	                     }	                     
	                     
	                 }
	             } 
	             RingBuffer.cancelReplay(apiIn);                 
	             pendingActivityAfterWrite = AFTER_WRITE_DO_NOTHING;	                 
	             	    
	             
	         }
	     };
	     AFTER_WRITE_SET_DUP_BIT = new ActivityAfterWrite() {
	         public void doIt() {
	             setDupBitOn();
                 pendingActivityAfterWrite = AFTER_WRITE_DO_NOTHING;
	         }
	     };	     
	     pendingActivityAfterWrite = AFTER_WRITE_DO_NOTHING;
		
	     packetIdRelease = new PacketIdReleaseManager();
	     
	}

    private boolean resendUnconfirmedMessages(long now) {
        
        //we must always enter replay mode when we use AFTER_WRITE_CONTINUE_REPLAY
        RingBuffer.replayUnReleased(apiIn);
        pendingActivityAfterWrite = AFTER_WRITE_CONTINUE_REPLAY;
        //do what can be done now, we may get lucky so we do not have to process this later
        pendingActivityAfterWrite.doIt();
        //if the state was changed back to do nothing then everything was replayed or there was nothing to be replayed.
        return AFTER_WRITE_DO_NOTHING == pendingActivityAfterWrite;
        
    }

	@Override
	public void shutdown() {
		
	}
	
	
	@Override
	public void run() {

	   // System.err.println("last continue reason "+continueReason);

    	    long now = System.currentTimeMillis();
    	    
    	    //must keep re-setting this value, must not let it release on its own, TODO: need a more elegant solution.
            RingBuffer.batchAllReleases(apiIn);
            
    		//65536/8 is 1<<13 8k bytes for perfect hash
    		            
    		//read input from socket and if data is found
    		if (STATE_CONNECTING==state || //connection in progress, now waiting for the ack 
    		    STATE_CONNECTED==state) { //connection established now reading
    			
    			if (!channel.isConnected()) {    			    
    			    state = STATE_CONNECTING;
    				if (!connect(now)) {
    				    continueReason = 1;
    				    return;
    				}
    			}
    	
    			if (channel.isOpen()) {
    			    if (hasPendingWrites()) {
    			        if (!nonBlockingByteBufferWrite(now)) {
    			            continueReason = 2;
    			            return;//do not continue because we have pending writes which must be done first.
    			        }			        
    			    }
    			    
    			    //there are no more pending writes at this point.
    			     
    			    //////////
    			    //Ping generation logic
    			    /////////
    		        if (timeStampTooOld(now)) {
    		            //try this first to avoid sending a ping
    		            if (!resendUnconfirmedMessages(now)) {
    		                continueReason = 3;
    		                return;//unable to send anything now, try again later.
    		            }
    		            //no messages to re send so ping must be sent
    		            if (timeStampTooOld(now)) {
    		                PING_MESSAGE.flip();
                            
    		                pendingWriteBuffers[0] = PING_MESSAGE;
                            
                            if (!nonBlockingByteBufferWrite(now)) {
                                continueReason = 4;
                                return;//try again later, can't send ping now.
                            }
    		            }		            
    		        }
    		        ////////
    		        //End of Ping logic
    		        ////////
    			    
    			    
    				try {		
    				    boolean hasPreviousData = inputSocketBuffer.position()>0;
    					while ( channel.read(inputSocketBuffer) > 0 || hasPreviousData) { //TODO: copy to HZ impl
    						//we found some new data what to do with it
    					    hasPreviousData = false;//any further passes will require the new reading of data.
    					    //must confirm there is room in case it is needed.
    					    if (!roomToLowLevelWrite(idGenOut, genIdMessageSize))  {
    					        continueReason = 5;
    					        return;//can not risk needing to release a packetId and not having the room so must wait.
    					    }					    
    					    if (!RingWriter.hasRoomForFragmentOfSize(apiOut, maxAckMessageSize)) {
    					        continueReason = 6;
    					        return;//can not risk needing to notify the caller and not having room to do so.
    					    }
    					    					    
    						assert(inputSocketBuffer.position()>0) : "If count was positive we should have had a value here in the buffer";
    						inputSocketBuffer.flip(); //start reading from zero
    						if (!parseData(now)) {
    							//disconnected so start over
    						    inputSocketBuffer.clear();
    						    continueReason = 7;
    							return;
    						}
    										
    						//copy the last 1 or 2 byte back down to bottom of buffer to add on for next time.
    	                    //sets up the position for writing again and sets limit to capacity.
    						unflip(inputSocketBuffer);    						
    					}
    					//if this returns 0 then there was nothing to read and nothing to do, only works in non blocking mode.
    							    					
    				} catch (IOException e) {
    					log.error("Unable to parse data",e);
    					 continueReason = 8;
    					return;
    				}
    			}
    			
    		}
    
            //only if there are NO unconfirmed messages outstanding.
            assert(outstandingUnconfirmedMessages>=0);
            if (0==outstandingUnconfirmedMessages && !hasPendingWrites()) {;
                RingReader.releaseReadLock(apiIn);
                RingBuffer.releaseAllBatchedReads(apiIn);
            }
            
    		//must be in connected or disconnected state before reading a fragment
    		if (notPendingConnect() && !hasPendingWrites() && RingReader.tryReadFragment(apiIn)) {
    			
    		    int msgIdx = RingReader.getMsgIdx(apiIn);
    		    
    	//	    System.err.println(apiIn.sizeOfUntructuredLayoutRingBuffer+"  "+apiIn+"  "+(RingBuffer.bytesWorkingHeadPosition(apiIn)-RingBuffer.bytesTailPosition(apiIn)));
    	//	    System.err.println("ByteBase:"+RingBuffer.bytesReadBase(apiIn)+" for "+ClientFromFactory.connectionInFROM.fieldNameScript[msgIdx]+"   "+ RingBuffer.bytesWorkingTailPosition(apiIn));
    		   
    		    
    			log.error("now reading message {}",ClientFromFactory.connectionInFROM.fieldNameScript[msgIdx]);
    			
    			switch (msgIdx) {
    				case ConInConst.MSG_CON_IN_CONNECT:	
        					//set value now so that no more fragments are read before the ack of the connect is recieved.
        					state = STATE_CONNECTING;
        					log.debug("sending a new connect request to server");
    
        					//only create new host iff it does not match the old value
        					if (null==addr || !RingReader.isEqual(apiIn, ConInConst.CON_IN_CONNECT_FIELD_URL, addr.getHostString())) {
        					    //this is only for a new connection as defined from the api
        					    
        					    commonBuilder.setLength(0);        					            					    
        					    String host = RingReader.readASCII(apiIn, ConInConst.CON_IN_CONNECT_FIELD_URL, commonBuilder).toString();
        					    
                                InetSocketAddress tempAddr = null;
                                try {
                                    tempAddr = new InetSocketAddressImmutable(host, port);
                                } catch (Throwable t) {
                                    log.error("Reconnecting but new new host was unknown.",t);
                                }
                                if (null==tempAddr || tempAddr.isUnresolved()) {
                                    if (null==addr) {
                                        throw new RuntimeException("Unable to resolve host: "+host);
                                    }
                                    log.error("Using last known host {}, Unable to resolve new host {}",addr.getHostName(),host);
                                } else {
                                    //safe to assign
                                    addr = tempAddr;
                                }
        					    //the above replacement may cause some garbage however none will be created upon connect and disconnect.
        					    //TOOD:D if it should become important however even this garbage can be eliminated.
        					}
        						    					
        					
        					//must hold the connection message in this byte buffer so we can use it any time we need to re-connect.
        					CONNECT_MESSAGE.clear();
        					RingReader.readBytes(apiIn, ConInConst.CON_IN_CONNECT_FIELD_PACKETDATA, CONNECT_MESSAGE);								
        										
        					if (!connect(now)) {
        					    continueReason = 9;
        					   // RingReader.releaseReadLock(apiIn);
        					    return;
        					}
        										
    					break;
    				case ConInConst.MSG_CON_IN_DISCONNECT:
    				    //System.out.println("dis mssg");
    				    
        					assert(STATE_CONNECTING!=state);								
        					if (!channel.isOpen()) {//unable to disconnect because it has already been done
        						state = STATE_DISCONNECTED;	
        						continueReason = 10;
        						System.out.println("unable to dis");
        						//RingReader.releaseReadLock(apiIn);
        						return;				
        					}										
        					if (STATE_CONNECTED == state && channel.isOpen()) {					    			
        							DISCONNECT_MESSAGE.flip();
        							
        							//TODO: AAA next urgent part to fix.
        							//NOTE: connect disconnect works perfect for qos0 so the problem must be cutting off the ack and the re-send on re-connect!!
        							
        							
        							//System.out.println("sent disconnect ");
        							if (hasPendingWrites()) {
        							    System.err.println("********************** unclean shutdown");
        							    new Exception().printStackTrace();
        							    System.exit(-1);
        							}
        							
        							pendingWriteBuffers[0] = DISCONNECT_MESSAGE;//send disconnect  0xE0 0x00
        							
        							pendingActivityAfterWrite=AFTER_WRITE_DO_DISCONNECT;
        							nonBlockingByteBufferWrite(now);
        							
        					} else {
        						log.error("warning something happended and disconnect found state to be :"+state);
        						state = STATE_DISCONNECTED;
        						clearTimestamp();
        					}
        					//RingReader.releaseReadLock(apiIn);
    					break;
    				case ConInConst.MSG_CON_IN_PUBLISH:
        					assert(STATE_CONNECTING!=state);    
        					
        					if (state==STATE_DISCONNECTED) {
        					    continueReason = 11;
        					    //TODO: error, called publish before connect. what about forced disconnects should that be soft or hard???
        					     //send error back to API.
        						return;
        					}
			
        					outstandingUnconfirmedMessages += RingReader.readInt(apiIn, ConInConst.CON_IN_PUBLISH_FIELD_QOS);
                            pendingWriteBuffers[0] = RingReader.wrappedUnstructuredLayoutBufferA(apiIn, ConInConst.CON_IN_PUBLISH_FIELD_PACKETDATA);
                            pendingWriteBuffers[1] = RingReader.wrappedUnstructuredLayoutBufferB(apiIn, ConInConst.CON_IN_PUBLISH_FIELD_PACKETDATA);
                            
                            pendingActivityAfterWrite = AFTER_WRITE_SET_DUP_BIT;
                            assert(pendingWriteBuffers[0].remaining()>0): "The packed data must be found in the buffer"; //note that we added 2 for each qos of 2
        					
                            nonBlockingByteBufferWrite(now);                           
    					break;    						
    			}    			
    		
    			
    			//only if there are NO unconfirmed messages outstanding.
    			assert(outstandingUnconfirmedMessages>=0);
    			if (0==outstandingUnconfirmedMessages && !hasPendingWrites()) {
    			    RingReader.releaseReadLock(apiIn);
    			    RingBuffer.releaseAllBatchedReads(apiIn);
    			}  			
    			
    			
    		} else {
    			if (prev!=state) {
    			    //may be here when waiting for the broker to startup.
    				log.debug("STUCK with state:{} inputRing: {}",state,apiIn);
    				prev=state;
    			}
    		}
    		continueReason = 0;

	}

	private void dropConnection(Exception reason) {
	        try {
	            log.warn("Deconnect initiated due to exception:",reason);
	            channel.close();
	            state = STATE_CONNECTING;//rolled back to the connecting state to re-connect
	        } catch (IOException e) {
	            log.debug("Exception when disconnecting",e);
	        }
	}
	
	private void dropConnection(String reason) {
	    try {
	        System.out.println("FAIL:"+reason);
	        log.warn("Deconnect initiated due to:",reason);
            channel.close();
            state = STATE_CONNECTING;//rolled back to the connecting state to re-connect
        } catch (IOException e) {
            log.debug("Exception when disconnecting",e);
        }
	}

    private void setDupBitOn() {
        //set the dup bit in case this gets sent again.
        RingReader.readBytesBackingArray(apiIn, ConInConst.CON_IN_PUBLISH_FIELD_PACKETDATA)
                 [RingReader.readBytesPosition(apiIn, ConInConst.CON_IN_PUBLISH_FIELD_PACKETDATA) &
                  RingReader.readBytesMask(apiIn, ConInConst.CON_IN_PUBLISH_FIELD_PACKETDATA)] |= DUP_BIT;
    }


    private boolean hasPendingWrites() {
        int x = pendingWriteBuffers.length;
        boolean result = false;
        while (--x >= 0) {
            result |= ((null!=pendingWriteBuffers[x])&&(pendingWriteBuffers[x].hasRemaining()));
        }
        return result;
    }


    /**
     * Returns true if all the data was written, false otherwise.
     * Continue to call this method until true is returned.
     * 
     * @param now
     * @return
     */
    private boolean nonBlockingByteBufferWrite(long now) {
                
        int i = 0;
        int limit = pendingWriteBuffers.length;
        while (i<limit) {
            if (null != pendingWriteBuffers[i]) {
                try{
                    if (channel.write(pendingWriteBuffers[i])>0) {
                        touchTimestamp(now);                  
                    }
                    if (0 == pendingWriteBuffers[i].remaining()) {
                        pendingWriteBuffers[i] = null;
                    } else {
                        //finish later
                        return false;
                    }
                } catch (Exception e) {
                    if (!(e instanceof NotYetConnectedException) && !(e instanceof IOException)) {
                        dropConnection(e);
                    }
                    return false;
                }
                
            }
            i++;
        }
        //At this point all the buffers have been set to null
        pendingActivityAfterWrite.doIt();
        return AFTER_WRITE_DO_NOTHING == pendingActivityAfterWrite; //true only if everthing was complete
        
    }



	private boolean notPendingConnect() {
		return STATE_CONNECTING != state;
	}



	private void unflip(ByteBuffer inputSocketBuffer) {
		int t = inputSocketBuffer.position();
		int l = inputSocketBuffer.limit();
		inputSocketBuffer.position(0);
		while (t<l) {
			inputSocketBuffer.put(inputSocketBuffer.get(t++));	                    	
		}
		inputSocketBuffer.limit(inputSocketBuffer.capacity());
	}

	
	//PINGREQ - generate send
	//0xC0  type/reserved
	//0x00 remaining length 0

	
	//TODO: must add a check because we may not get all the needed bytes and need to continue late when the rest of the bytes  are ready.
	
	private boolean parseData(long now) {
	    //using this local value because we may not have all the needed bytes and can not move the postion until we know for sure.
	    int position = inputSocketBuffer.position();
	    int limit = inputSocketBuffer.limit();
		//we only expect 4 different packet types so this makes a nice conditional tree
	    if (limit-position < 2) {
	        return true;//try again later;
	    }
		final int packetType = 0xFF&inputSocketBuffer.get(position++);						
		final int length = 0xFF&inputSocketBuffer.get(position++);
		if (0 == (0xAF & packetType)) { 
			//1010 1111 mask for PUBACK 0100 0000 or PUBREC 0101 0000
			//second byte must always be 2 (the number of remaining bytes in the packet)
			if ((2 != length) || (0 == (0x40 & packetType) ) ) {
				dropConnection("Packet assumed to be PUBACK but it was malformed.");
				return false;
			}
			
		    if (limit-position < 2) {
		            return true;//try again later;
		    }
			final int msb = 0xFF&inputSocketBuffer.get(position++);						
			final int lsb = 0xFF&inputSocketBuffer.get(position++);
			//This is needed for both QoS 1 and 2
			int packetId = (msb << 16) | (0xFF & lsb);
					
			//NOTE: PUBACK does not need any further work, we have already released the message
			//        PUBACK - ack from our publish of a QOS 1 message
			//        0x40 type/reserved    0100 0000
			//        0x02 remaining length
			//        MSB PacketID high
			//        LSB PacketID low
										
			
			if (0!=(0x10&packetType)) {								
				//NOTE: In addition to release PubRec must send back PUBCOMP
				//        PUBREC - ack from our publish of a QOS 2 message
				//        0x50 type/reserved    0101 0000
				//        0x02 remaining length
				//        MSB PacketID high
				//        LSB PacketID low
				
                boolean ok = RingWriter.tryWriteFragment(apiOut, ConOutConst.MSG_CON_OUT_PUB_REC);
                assert(ok) : "Internal error, expected there to be room for this write";
                RingWriter.writeInt(apiOut, ConOutConst.CON_OUT_PUB_REC_FIELD_PACKETID, packetId);                
                PacketIdReleaseManager.releaseMessage(packetIdRelease, this, packetId,2);
                
			} else {

			    boolean ok = RingWriter.tryWriteFragment(apiOut, ConOutConst.MSG_CON_OUT_PUB_ACK);
			    assert(ok) : "Internal error, expected there to be room for this write";
			    RingWriter.writeInt(apiOut, ConOutConst.CON_OUT_PUB_ACK_FIELD_PACKETID, packetId);		    
			    PacketIdReleaseManager.releaseMessage(packetIdRelease, this, packetId,1);			    
			}
			RingWriter.publishWrites(apiOut);					
			
		} else {		    
			
			//did not pass mask so this is CONNACK 0010 0000 or PINGRESP 1101 0000
			if (0==(0x80 & packetType)) { //top bit 1000 0000 mask to check for zero	
								
				if (0==(0x10 & packetType)) {
					log.debug("got ack from server testing");
					
					//CONNACK - ack from our request to connect
					// 0x20 type/reserved   0010 0000
					// 0x02 remaining length
					// 0x01 reserved with low session present bit
					// 0x?? connection return code 0 ok, 1 bad proto, 2, bad id 3 no server 4 bad userpass 5 not auth  6-255 reserved
					if ((2!=length) || (0x20 != packetType)) {
					    dropConnection("Packet assumed to be CONNACK but it was malformed.");
						return false;
					}
					
		            if (limit-position < 2) {
	                    return true;//try again later;
	                }		
					final int sessionPresent       = 0xFF&inputSocketBuffer.get(position++);	 //TODO: Why do we want this flag?					
					final int connectionReturnCode = 0xFF&inputSocketBuffer.get(position++);
					
					
					if (0==connectionReturnCode) {
						state = STATE_CONNECTED; //up and ready
					} else {
						state = STATE_DISCONNECTED;
					}
					log.debug("got ack from server connect state :{}",state);
					
				    boolean ok = RingWriter.tryWriteFragment(apiOut, CON_ACK_MSG[connectionReturnCode]); //This tells the caller we are disconnected or not
				    assert(ok) : "Internal error, expected there to be room for this write";
					RingWriter.publishWrites(apiOut);
									
					//Upon reconnection must always send unconfirmed messages
					resendUnconfirmedMessages(now);
			
				} else {

					//PUBCOMP  PARSE
					//0x70  type/reerved 0111 0000
					//0x02  remaining length
					// MSB PacketID high
					// LSB PacketID low
					
					if ((2!=length) || (0x70 != packetType)) {
					    dropConnection("Packet assumed to be PUBCOMP but it was malformed.");
						return false;
					}
					
			        if (limit-position < 2) {
		                    return true;//try again later;
		            }
					final int msb = 0xFF&inputSocketBuffer.get(position++);						
					final int lsb = 0xFF&inputSocketBuffer.get(position++);
					int packetId = (msb << 16) | (0xFF & lsb);
					
					//release the pubRel to prevent it from getting sent
				    PacketIdReleaseManager.releaseMessage(packetIdRelease, this, packetId,3);
				}
								
				
				
			} else {
				//PINGRESP - ack from our ping request
				//0xD0  type//reserved  1101 0000
				//0x00  remaining length 0
				if ((0!=length) || (0xD0 != packetType)) {
				    dropConnection("Packet assumed to be PINGRESP but it was malformed. len:"+length+" type:"+Integer.toHexString(packetType)); 
				    //TODO: all errors must capture data.
					return false;
				}
				processPingResponse();
			}
			
		}
		
		//upon successful parse always update time-stamp, this include conAck 
		touchTimestamp(now);
		inputSocketBuffer.position(position);
		return true;
	}


	private void touchTimestamp(long now) {
	    lastTimestamp = now;
    }

	private void clearTimestamp() {
	    lastTimestamp = 0;
	}

	private boolean timeStampTooOld(long now) {
	    
	    return (now-lastTimestamp)>timeLimitMS;
	}

    private void processPingResponse() {
        unconfirmedPings--;
	}
    

	private boolean connect(long now) {

		//Note this connection message can also be kicked off because the expected state is connected and the connection was lost.
		//     create socket and connect when we get the connect message		
		try {	
		    
		    if (!channel.isOpen()) {
		        //once a connection is closed it can not be re-opened so we have no choice but create a new connection.
		        //NOTE: this is a concern because we now have garbage to be collected.  TODO: X, review what can be done to make this garbage free?
		        buildNewConnection();
		    }
		    
			if (channel.isConnectionPending() || !channel.connect(addr)) {
			    if (!channel.finishConnect()) {
			        return false;
			    }
			}
			
			if (hasPendingWrites() && pendingWriteBuffers[0] != CONNECT_MESSAGE) {
			    //move them all down.
			    int x = pendingWriteBuffers.length-1;
			    assert(null==pendingWriteBuffers[x]);
			    while (--x >= 1) {
			        pendingWriteBuffers[x] = pendingWriteBuffers[x-1]; //TODO: This is sending too soon? we have not gotten ack back for connect.
			    }
			}
			
			CONNECT_MESSAGE.flip();						
			pendingWriteBuffers[0] = CONNECT_MESSAGE;
			return nonBlockingByteBufferWrite(now);

		} catch (Throwable t) {
		   // t.printStackTrace();
		    //this is not unreasonable if we are waiting for the broker to be started.
			log.warn("Unable to connect to {}",addr.toString(), t);
			System.exit(-1);
			buildNewConnection(); //rebuild-connection to start fresh.
			return false;
		}		
	}


    private void buildNewConnection() {
        try {
            channel = (SocketChannel) SelectorProvider.provider().openSocketChannel().configureBlocking(false);
            assert(!channel.isBlocking()) : "Blocking must be turned off for all socket connections";   
        } catch (IOException e) {
            throw new RuntimeException("CHECK NETWORK CONNECTION, New non blocking SocketChannel not supported on this platform",e);
        }
    }
}
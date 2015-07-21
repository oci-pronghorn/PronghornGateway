package com.ociweb.gateway.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.ring.RingReader;
import com.ociweb.pronghorn.ring.RingWriter;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class ConnectionStage extends PronghornStage {
	
	 private final RingBuffer apiIn;
	 private final RingBuffer timeIn;
	 private final RingBuffer apiOut;
	 private final RingBuffer timeOut;
	 private final RingBuffer idGenOut;
	 private SSLSocketFactory sslSocketFactory;
	 
	 private final int inFlightLimit;
	 private SocketChannel channel;
	 private StringBuilder commonBuilder = new StringBuilder();
	 private String host;	
	 private byte state; //0 disconnected
	 private ByteBuffer inputSocketBuffer;
	 private ByteBuffer DISCONNECT_MESSAGE;
	 private ByteBuffer CONNECT_MESSAGE;
	 	 
	 private int[] CON_ACK_MSG;
	 private int prev = -100;
	 
	 private static Logger log = LoggerFactory.getLogger(ConnectionStage.class);
	 
	 // must be divisable by 4 and >=4 to evenly fit all the packets expected
	 // the biggest message is also 4 in length so this buffer will support
	 // parsing of many packets at once as long as its greater than 4.
	 private final static int INPUT_BUFFER_SIZE = 128;	 
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
	 

	protected ConnectionStage(GraphManager graphManager, RingBuffer apiIn,  RingBuffer timeIn, 
			                                             RingBuffer apiOut, RingBuffer timeOut, RingBuffer idGenOut) {
		super(graphManager, 
				new RingBuffer[]{apiIn,timeIn},
				new RingBuffer[]{apiOut,timeOut,idGenOut});

		this.apiIn = apiIn;
		this.timeIn = timeIn; //use low level api 3 types but no fields
		this.apiOut = apiOut;
		this.timeOut = timeOut;    //use low level api only 1 message type
		this.idGenOut = idGenOut;   //use low level api, only 1 message type
		
		this.inFlightLimit = 10;//TODO: Needs to be configured 
		
	}

	
	
	@Override
	public void startup() {
		
		DISCONNECT_MESSAGE = ByteBuffer.allocate(2);
		DISCONNECT_MESSAGE.put((byte) 0xE0);
		DISCONNECT_MESSAGE.put((byte) 0x00);
		
		CON_ACK_MSG = new int[]{
                ConOutConst.MSG_CON_OUT_CONNACK_OK,
				ConOutConst.MSG_CON_OUT_CONNACK_PROTO,
				ConOutConst.MSG_CON_OUT_CONNACK_ID,
				ConOutConst.MSG_CON_OUT_CONNACK_SERVER,
				ConOutConst.MSG_CON_OUT_CONNACK_USER,
				ConOutConst.MSG_CON_OUT_CONNACK_AUTH 
				}; 
		
		CONNECT_MESSAGE = ByteBuffer.allocate(256);//TODO: AAA, no idea what size to make this.
		
		sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
		inputSocketBuffer = ByteBuffer.allocate(256);//TODO: how big are the acks we need room for?
	}



	@Override
	public void shutdown() {
		
	}
	
	
	@Override
	public void run() {
		
		//65536/8 is 1<<13 8k bytes for perfect hash
		
		//read input from socket and if data is found
		if (1==state || //connection in progress, now waiting for the ack 
			2==state) { //connection established now reading
			
			if (!channel.isOpen()) {
				connect();
			}
	
			if (channel.isOpen()) {
				try {					
					int count;					
					while ( (count = channel.read(inputSocketBuffer)  ) > 0 ) {
						//we found some new data what to do with it
																		
						assert(inputSocketBuffer.position()>0) : "If count was positive we should have had a value here in the buffer";
						inputSocketBuffer.flip(); //start reading from zero
												
						if (!parseData()) {
							//parse found an error and dropped the connection
							return;
						}
										
						//copy the last 1 or 2 byte back down to bottom of buffer to add on for next time.
	                    //sets up the position for writing again and sets limit to capacity.
						unflip(inputSocketBuffer);
						
					}					
					
					//if this returns 0 then there was nothing to read and nothign to do, only works in non blocking mode.
							
					
				} catch (IOException e) {
					log.error("Unable to parse data",e);
					return;
				}
			}
			
		}

		//must be in connected or disconnected state before reading a fragment
		if (notPendingConnect() &&
			RingReader.tryReadFragment(apiIn)) {
			
			int msgIdx = RingReader.getMsgIdx(apiIn);
			log.error("now reading message "+ClientFromFactory.connectionInFROM.fieldNameScript[msgIdx]);
			
			switch (msgIdx) {
			
				case ConInConst.MSG_CON_IN_CONNECT:	
					//set value now so that no more fragments are read before the ack of the connect is recieved.
					state = 1; //in the connection process
					log.error("sending a new connect request to server, set state: "+state);
							
					
					//TODO: A, need new method
					//if (!
					//RingReader.isEqual(apiIn, ConInConst.CON_IN_CONNECT_FIELD_URL,host);
										
					//this is only for a new connection as defined from the api
					commonBuilder.setLength(0);
					host = RingReader.readASCII(apiIn, ConInConst.CON_IN_CONNECT_FIELD_URL, commonBuilder).toString();										
					
					//must hold the connection message in this byte buffer so we can use it any time we need to re-connect.
					CONNECT_MESSAGE.clear();
					RingReader.readBytes(apiIn, ConInConst.CON_IN_CONNECT_FIELD_PACKETDATA, CONNECT_MESSAGE);								
										
					connect();
										
					break;
				case ConInConst.MSG_CON_IN_DISCONNECT:
					assert(1!=state);
					
					log.error("disconnect started");
					
					if (!channel.isOpen()) {
						//unable to disconnect because it has already been done
						state = 0;						
						return;				
					}
										
					if (2 == state && channel.isOpen()) {
						try {							
							DISCONNECT_MESSAGE.flip();
							
							while (DISCONNECT_MESSAGE.hasRemaining()) { //TODO: how can this be async?
								channel.write(DISCONNECT_MESSAGE );	//send disconnect  0xE0 0x00
							}
														
							channel.close();
						} catch (IOException e) {
							log.error("Unable to process disconnect from API",e);
						}
					} else {
						log.error("warning something happended and disconnect found state to be :"+state);
					}
					state = 0; //TODO: once defined these states need to be static constants
					log.error("disconnect finished state: "+state);
					
					break;
				case ConInConst.MSG_CON_IN_PUBLISH:
					assert(1!=state);
					
					if (state==0) {
						//TODO: error, called publish before connect.
						
						return;
					}

					if (!channel.isOpen()) {						
						connect();						
						if (!channel.isOpen()) {
							
							
							//TODO: AAA, we have already taken the message so we need a flag to re-enter to this point
							return;//try again later, unable to connect right now
							
						}
						
					}
					
					int qos = RingReader.readInt(apiIn, ConInConst.CON_IN_PUBLISH_FIELD_QOS);
					
					if (0==qos) {
						//simple case, just send the data
						log.error("sending QOS 0 to server");

						ByteBuffer buffer1 = RingReader.wrappedBuffer1(apiIn, ConInConst.CON_IN_PUBLISH_FIELD_PACKETDATA);
						assert(buffer1.remaining()>0): "The packed data must be found in the buffer";
						
						while (buffer1.hasRemaining()) { //TODO: how can this be async?
							try {
								channel.write(buffer1);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}	//send disconnect  0xE0 0x00
						}
						//
						ByteBuffer buffer2 = RingReader.wrappedBuffer2(apiIn, ConInConst.CON_IN_PUBLISH_FIELD_PACKETDATA);
						while (buffer2.hasRemaining()) { //TODO: how can this be async?
							try {
								channel.write(buffer2);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}	//send disconnect  0xE0 0x00
						}
						
					} else {
						//send and wait for ack so do not release.
						
						
					}
					
					
					//publish message to connection from the input queue
					//grab full block across fields,, may need to support constants later
					//qos 0 just do it.
					//qos 1 do it but do not rlease on ring
					//    when  ack comes must release ring.
					//    release all blocks that we can otherwise hold.
					
					
					break;
						
			
			}
			
			RingReader.releaseReadLock(apiIn);
		} else {
			if (prev!=state) {
				log.error("STUCK with "+state+ "   "+apiIn);
				prev=state;
			}
		}
		
	}



	private boolean notPendingConnect() {
		return 1!=state;
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

	
	//PINTREQ - generate send
	//0xC0  type/reserved
	//0x00 remaining length 0

	private boolean parseData() {
		//we only expect 4 different packet types so this makes a nice conditional tree
		final int packetType = inputSocketBuffer.get();						
		final int length = inputSocketBuffer.get(); //TODO: what if we have no more to read here??
		if (0 == (0xAF & packetType)) { 
			//1010 1111 mask for PUBACK 0100 0000 or PUBREC 0101 0000
			//second byte must always be 2 (the number of remaining bytes in the packet)
			if ((2 != length) || (0 == (0x40 & packetType) ) ) {
				dropConnection();
				return false;
			}
			
			final int msb = inputSocketBuffer.get();						
			final int lsb = inputSocketBuffer.get();
			//This is needed for both QoS 1 and 2
			releaseMessage((msb << 16) | (0xFF & lsb));
			
		
			//NOTE: PUBACK does not need any further work
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
				
				
				//TODO: send PUBREL now because we have the PUBREC
				//0x62  type/reserved   0110 0010
				//0x02  remaining length
				//MSB PacketID high
				//LSB PacketID low
				
				
			
				
			}

			
				
			
			
		} else {		    

			
			//did not pass mask so this is CONNACK 0010 0000 or PINGRESP 1101 0000
			if (0==(0x80 & packetType)) { //top bit 1000 0000 mask to check for zero	
				
				
				if (0==(0x10 & packetType)) {
					log.error("got ack from server testing");
					
					//CONNACK - ack from our request to connect
					// 0x20 type/reserved   0010 0000
					// 0x02 remaining length
					// 0x01 reserved with low session present bit
					// 0x?? connection return code 0 ok, 1 bad proto, 2, bad id 3 no server 4 bad userpass 5 not auth  6-255 reserved
					if ((2!=length) || (0x20 != packetType)) {
						dropConnection();
						return false;
					}
					
										
					final int sessionPresent       = inputSocketBuffer.get();						
					final int connectionReturnCode = inputSocketBuffer.get();
					
					
					if (0==connectionReturnCode) {
						state = 2; //up and ready
					} else {
						state = 0;						
					}
					log.error("got ack from server connect state :"+state);
					
					//TODO: B refactor to no block this call!!!
				    RingWriter.blockWriteFragment(apiOut, CON_ACK_MSG[connectionReturnCode]);
					RingWriter.publishWrites(apiOut);
			
				} else {

					//PUBCOMP  PARSE
					//0x70  type/reerved 0111 0000
					//0x02  remaining length
					// MSB PacketID high
					// LSB PacketID low
					
					if ((2!=length) || (0x70 != packetType)) {
						dropConnection();
						return false;
					}
					
					final int msb = inputSocketBuffer.get();						
					final int lsb = inputSocketBuffer.get();
					
					//TODO: do the last step for QoS2
					
					
				}
								
				
				
			} else {
				//PINGRESP - ack from our ping request
				//0xD0  type//reserved  1101 0000
				//0x00  remaining length 0
				if ((0!=length) || (0xD0 != packetType)) {
					dropConnection();
					return false;
				}
				processPingResponse();
			}
			
		}
		return true;
	}

//PINGREQ (two bytes)
	//0xC0  type/reserved 1100 0000
	//0x00   remaining length

	private void processPingResponse() {
		
		//TODO: clear the flag that was set when we sent the ping
		
		
	}

	private void releaseMessage(int packetId) {
		// TODO Auto-generated method stub
		
	}
	
	private void resendUnconfirmedMessages() {
		// TODO Auto-generated method stub
		
	}

	private void dropConnection() {
		if (channel.isOpen()) {
			try {
				channel.close();
			} catch (IOException e) {
				log.error("Unable to close", e);
			}
		}
		//state is not changed so we can reconnect until the APIStage instance requests a disconnect.
	}


	private boolean connect() {
		//TLS     socket 8883
		//non-tls socket 1883
		
		int port = 1883;//TODO: A, need a way to use both sockets as needed
		//TODO: B research how to support both TLS and non-TLS			
		
		//Note this connection message can also be kicked off because the expected state is connected and the connection was lost.
		//    create socket and connect when we get the connect message		
		try {

			SocketAddress addr = new InetSocketAddress(host, port); //TODO:AA, must move object create. and replace the holding of host, not needed.
			SocketChannel localChannel = SocketChannel.open();									
			channel = (SocketChannel)localChannel.configureBlocking(false); 
						
			
			assert(!channel.isBlocking()) : "Blocking must be turned off for all socket connections";
			
			channel.connect(addr); //TODO: do this earlier ?
			
			
			while (!channel.finishConnect()) { // TODO: make non blocking				
			}

			
			CONNECT_MESSAGE.flip();
						
			
			while (CONNECT_MESSAGE.hasRemaining()) {
				//TODO: make non blocking
				channel.write(CONNECT_MESSAGE);
			}
				


		} catch (Throwable t) {
			log.error("Unable to connect", t);
			return false;
		}
		resendUnconfirmedMessages();
		return true;
	}



}

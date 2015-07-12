package com.ociweb.gateway.client;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.ring.RingReader;
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
		if (state==2) {
			if (!channel.isOpen()) {
				
				connect();
				
				if (!channel.isOpen()) {
					return;//try again later, unable to connect right now					
				}
				
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
												
						//TODO: do we unflip the remaining data?
						
					}
					//if this returns 0 then there was nothing to read and nothign to do, only works in non blocking mode.
					
							  
										
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				
			}
	
			//channel.
			//TODO: A, need to check if we want to use selectors or a lower level approach
			
			
		}
		
		//PINTREQ - geneate send
		//0xC0  type/reserved
		//0x00 remaining length 0
		
		
		//PUBCOMP - generate send
		//0x70 type/reserved
		//0x02 remaining length
		//MSB PacketID high
		//LSB PacketID low
		
		
		if (RingReader.tryReadFragment(apiIn)) {
			switch (RingReader.getMsgIdx(apiIn)) {
			
				case ConInConst.MSG_CONNECT:			
					
					
					
					//TODO: A, need new method
					//if (!RingReader.isEqual(apiIn, ConInConst.CON_IN_CONNECT_FIELD_URL,host));
										
					//this is only for a new connection as defined from the api
					commonBuilder.setLength(0);
					host = RingReader.readASCII(apiIn, ConInConst.CON_IN_CONNECT_FIELD_URL, commonBuilder).toString();										
					state = 1; //in the connection process
					connect();
					state = 2; //up and ready
					
					break;
				case ConInConst.MSG_DISCONNECT:
					if (state !=0 && channel.isOpen()) {
						try {
							channel.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					state = 0; //TODO: once defined these states need to be static constants
					
					
					break;
				case ConInConst.MSG_PUBLISH:
					
					if (state!=2) {
						//TODO: A, caller error, connection should have been sent first
						
					}
					if (!channel.isOpen()) {
						//TODO: A, network error expected this to be open already
						connect();
						
						if (!channel.isOpen()) {
							return;//try again later
							
						}
						
					}
					
					int qos = RingReader.readInt(apiIn, ConInConst.CON_IN_PUBLISH_FIELD_QOS);
					
					
					
					
					//publish message to connection from the input queue
					//grab full block across fields,, may need to support constants later
					//qos 0 just do it.
					//qos 1 do it but do not rlease on ring
					//    when  ack comes must release ring.
					//    release all blocks that we can otherwise hold.
					
					
					break;
						
			
			}
		}
		
	}

	//TODO: send disconnect  0xE0 0x00
	//TLS     socket 8883
	//non-tls socket 1883


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
				
				
				//TODO: send PUBREL
				//0x62  type/reserved   0110 0010
				//0x02  remaining length
				//MSB PacketID high
				//LSB PacketID low
				
				//PUBCOMP  PARSE
				//0x70  type/reerved 0111 0000
				//0x02  remaining length
				// MSB PacketID high
				// LSB PacketID low
			
				
			}
			
			
		} else {
		    
			//did not pass mask so this is CONNACK 0010 0000 or PINGRESP 1101 0000
			if (0==(0x80 & packetType)) {								
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
				
				//TODO: turn on the connection or report issue from message.
				
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



	private void dropConnection() {
		// TODO Auto-generated method stub
		
	}



	private void releaseMessage(int packetId) {
		// TODO Auto-generated method stub
		
	}



	private void connect() {
		int port = 1830;//TODO: AA, check this 
		
		//Note this connection message can also be kicked off because the expected state is connected and the connection was lost.
		//    create socket and connect when we get the connect message		
		try {
			SocketFactory sslsocketfactory = SocketFactory.getDefault();
			SSLSocket sslsocket = (SSLSocket) sslsocketfactory .createSocket(host, port);
			channel = (SocketChannel)sslsocket.getChannel().configureBlocking(false); 
			assert(!channel.isBlocking()) : "Blocking must be turned off for all socket connections";
			
			
		} catch (UnknownHostException e) {
			// TODO: A, must determine what should be done with errors
			e.printStackTrace();
		} catch (IOException e) {
			// TODO: A, must determine what should be done with errors
			e.printStackTrace();
		}
		
		///Other connect activities
		//TODO: A, replay the unconfirmed messages still held in the queue.
	}

}

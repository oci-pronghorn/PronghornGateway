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
	 
	//startup server
	//      java -Djavax.net.ssl.keyStore=mySrvKeystore -Djavax.net.ssl.keyStorePassword=123456 ServerApp
	//      
			//startup client 
	//      java -Djavax.net.ssl.trustStore=mySrvKeystore -Djavax.net.ssl.trustStorePassword=123456 ClientApp
	//      
			
	//      //debug
	//      -Djava.protocol.handler.pkgs=com.sun.net.ssl.internal.www.protocol -Djavax.net.debug=ssl
	 

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
				//TODO: A, network error expected this to be open already
				connect();
				
				if (!channel.isOpen()) {
					return;//try again later
					
				}
				
				//can not use selectors becauase
				//   1. they create a lot of garbage
				//   2. they get in the way of my own business specific scheduling.
				
				try {
				  					
					int count;					
					while ( (count =  channel.read(inputSocketBuffer)) > 0 ) {
						
						//TODO: A, parse it  
						//TODO: A, if not all read keep buffer and try again later for the rest.
						//TODO: A, upon any error in bytes disconnect
						//TODO: A,  if ack then release published messages from queue.
						//          NOTE: all QOS of zero will need tobe skipped so we could use qos of -1 and -2 to indicate ack.
						//TODO: A, send ack up to api
						
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

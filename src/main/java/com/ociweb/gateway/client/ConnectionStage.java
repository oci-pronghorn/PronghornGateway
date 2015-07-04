package com.ociweb.gateway.client;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

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
	 
	 private final int[] seen;
	 private int seenCount;

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
		this.seen = new int[inFlightLimit];
		
	}

	
	
	@Override
	public void startup() {
		
		sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
		
		
		//startup server
//         java -Djavax.net.ssl.keyStore=mySrvKeystore -Djavax.net.ssl.keyStorePassword=123456 ServerApp
//         
		//startup client 
//         java -Djavax.net.ssl.trustStore=mySrvKeystore -Djavax.net.ssl.trustStorePassword=123456 ClientApp
//         
		
//         //debug
//         -Djava.protocol.handler.pkgs=com.sun.net.ssl.internal.www.protocol -Djavax.net.debug=ssl
		
	}



	@Override
	public void shutdown() {
		
	}


	
	
	@Override
	public void run() {
		
		
		//65536/8 is 1<<13 8k bytes for perfect hash
		
		//read input from socket and if data is found
		//parse it  
		//send it up to api
		// if ack then release publish.
		
		if (RingReader.tryReadFragment(apiIn)) {
			switch (RingReader.getMsgIdx(apiIn)) {
			
				case ConInConst.MSG_CONNECT:
			
					//Use message size to derive release bounds etc
					//allocate one array of packetIds to remember which have come back.
					//2 bytes for each would fit 4K in 8K block so at that point the bit mask would be better for up to 64K flight
					//if no swap over then 64K in flight would be 128K of memory.
										
					
					//Keep array the size of in flight, which can not be changed at runtime.
					// probability says the one we expect is the one that will arrive
					// so liniarly check each in probability order.
					//very little storage for small in flight values
					//all 
					
					//Algo:
					//  if ack is on end of queue then release message
					//         else add value to seen list
					//  upon release
					//     check seen list for match 
					//     if match found release that one
					//     set value to -1 but move count if at the end 
					
					//upon "release" must store an int and a long for release later.
					
					
					SocketFactory sslsocketfactory = SocketFactory.getDefault();
				//    create socket and connect when we get the connect message		
					SSLSocket sslsocket;
					try {
						sslsocket = (SSLSocket) sslsocketfactory .createSocket("localhost", 9999);
						SelectableChannel channel = sslsocket.getChannel().configureBlocking(false);
						//is this peformant?
					//	channel.
						
						
						
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					      
					
					
					break;
				case ConInConst.MSG_DISCONNECT:
					
					
					break;
				case ConInConst.MSG_PUBLISH:
					
					
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

}

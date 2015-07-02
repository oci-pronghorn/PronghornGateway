package com.ociweb.gateway.client;

import javax.net.ssl.SSLSocketFactory;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class ConnectionStage extends PronghornStage {
	
	 private final RingBuffer apiIn;
	 private final RingBuffer timeIn;
	 private final RingBuffer apiOut;
	 private final RingBuffer timeOut;
	 private final RingBuffer idGenOut;
	 private SSLSocketFactory sslSocketFactory;
	 

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
		
		
		
//    create socket and connect when we get the connect message		
//      SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket("localhost", 9999);
//      sslsocket.getChannel();
//      
		
	}

}

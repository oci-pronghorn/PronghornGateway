package com.ociweb.gateway.client;

import java.nio.ByteBuffer;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class APIStage extends PronghornStage {

	private final RingBuffer idGenIn;
	private final RingBuffer toConnection;
	private final RingBuffer fromConnection;
	 	
	protected APIStage(GraphManager graphManager, RingBuffer idGenIn, RingBuffer conIn, RingBuffer conOut) {
		super(graphManager, new RingBuffer[]{idGenIn,conIn}, conOut);
	
		this.idGenIn = idGenIn;
		this.toConnection = conIn;
		this.fromConnection = conOut;
	}
	
	

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		
		//read idGen with low level, only 1 message type
		//write con In with high level there are many message types and fields
		//read cont out with high level there are many message types and fields
		

	}

	public void connect(CharSequence url, boolean someFlags) {
		//TODO: needs implementation
		
	}
	
	public void disconnect() {
		//TODO: needs implementation
	}
	
	public void publish(CharSequence topic, int QualityOfService, CharSequence payload) {
		//TODO: needs implementation
	}
	
	public void publish(CharSequence topic, int QualityOfService, ByteBuffer payload) {
		//TODO: needs implementation
	}
	
	public void publish(CharSequence topic, int QualityOfService, byte[] payload, int payloadOffset, int payloadLength) {
		//TODO: needs implementation
	}
	
//    public void addPublishAckListener(Object someListener) {
//    	//   Called on PubRec and PubAck passes ID
//    	//TODO: needs implementation
//    }
//    
//    public void addSubcriptionListener(CharSequence topic, int QualityOfService, Object someListener) {
//    	
//    	//TODO: needs implementation
//    	
//    }

}

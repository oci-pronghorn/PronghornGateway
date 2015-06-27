package com.ociweb.gateway.client;

import java.nio.ByteBuffer;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class APIStage extends PronghornStage {

	private final RingBuffer toConnection; //directly populated by external method calls, eg unknown thread
	private final RingBuffer idGenIn; //used by same external thread
	private final RingBuffer fromConnection;
	 	

	private final ConOutConst conOutConst;
	private final ConInConst conInConst;

  	
	protected APIStage(GraphManager graphManager, RingBuffer idGenIn, RingBuffer conIn, RingBuffer conOut) {
		super(graphManager, new RingBuffer[]{idGenIn,conIn}, conOut);
	
		this.idGenIn = idGenIn;
		this.toConnection = conIn;
		this.fromConnection = conOut;
		
        this.conOutConst = new ConOutConst(RingBuffer.from(toConnection));
        this.conInConst = new ConInConst(RingBuffer.from(fromConnection)); 		
        		      
	  	
	}
	
	

	@Override
	public void run() {
		// TODO read input queues and notify listeners of new data.
		//what if listeners block?
		
		//read cont out with high level there are many message types and fields
		
		//upon qos 2 ack will need to send response to connection
        // do we need a second queue for this ack and another for the new requests, yes to avoid lock.
		
	}

	//  public void addPublishAckListener(Object someListener) {
	//	//   Called on PubRec and PubAck passes ID
	//	//TODO: needs implementation
	//}
	//
	//public void addSubcriptionListener(CharSequence topic, int QualityOfService, Object someListener) {
	//	
	//	//TODO: needs implementation
	//	
	//}
	
	
	public void connect(CharSequence url, boolean someFlags) {
		
	    
		
		//TODO:  form connect message and put it on the toConnectionQueue
		// may want to synchronize these methods to protect against bad usages
		
	}
	
	public void disconnect() {
		//TODO:  form connect message and put it on the toConnectionQueue
		// may want to synchronize these methods to protect against bad usages
				
	}
	
	public void publish(CharSequence topic, int QualityOfService, CharSequence payload) {
		//TODO: form publish message and put on to connection queue
		// may want to synchronize these methods to protect against bad usages
		//read idGen with low level, only 1 message type
		//write con In with high level there are many message types and fields
	}
	
	public void publish(CharSequence topic, int QualityOfService, ByteBuffer payload) {
		//TODO: form publish message and put on to connection queue
		// may want to synchronize these methods to protect against bad usages
		//read idGen with low level, only 1 message type
		//write con In with high level there are many message types and fields
	}
	
	public void publish(CharSequence topic, int QualityOfService, byte[] payload, int payloadOffset, int payloadLength) {
		//TODO: form publish message and put on to connection queue
		// may want to synchronize these methods to protect against bad usages
		//read idGen with low level, only 1 message type
		//write con In with high level there are many message types and fields
	}
	


}

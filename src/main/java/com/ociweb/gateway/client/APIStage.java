package com.ociweb.gateway.client;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.ring.RingReader;
import com.ociweb.pronghorn.ring.RingWriter;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class APIStage extends PronghornStage {

	private final RingBuffer fromCon; //directly populated by external method calls, eg unknown thread
	private final RingBuffer idGenIn; //used by same external thread
	private final RingBuffer toCon;
	 	
	private int packetId = -1;
	private int packetIdLimit = -1;
	
	
	private Settings settings = new Settings(); //TODO: passed in on construction?
	
  	/**
  	 * This first implementation is kept simple until we get a working project.
  	 * 
  	 * The API may need some adjustments based on use cases.
  	 * 
  	 */
	
	private final int sizeOfPacketIdFragment;
	private static final int theOneMsg = 0;// there is only 1 message supported by this stage
	
	
	protected APIStage(GraphManager graphManager, RingBuffer idGenIn, RingBuffer fromC, RingBuffer toC) {
		super(graphManager, new RingBuffer[]{idGenIn,fromC}, toC);
	
		this.idGenIn = idGenIn;
		this.fromCon = fromC;
		this.toCon = toC;
			  	
        this.sizeOfPacketIdFragment = RingBuffer.from(idGenIn).fragDataSize[theOneMsg];
        
        //add one more ring buffer so apps can write directly to it since this stage needs to copy from something.
        //this makes testing much easier, it makes integration tighter
        //it may add a copy?
        
        
	}
	
	

	@Override
	public void run() {
		
		//loop is for subscribers?
		while (RingReader.tryReadFragment(fromCon)) {			
			RingReader.releaseReadLock(fromCon);		
		}
		businessLogic();
	}
	
	
	public void businessLogic() {
		//call the protected methods on this single thread
		
		
		
	}
	
	
	//caller must pre-encode these fields so they can be re-used for mutiple calls?
	//but this connect would be called rarely?
	
	//TODO: these methods may be extracted because they need not be part of an actor, 
	//NOTE: these methods are not thread safe and are intended for sequential use.
	
	protected boolean requestConnect(CharSequence url, int conFlags, byte[] willTopic, byte[] willMessageBytes, byte[] username, byte[] passwordBytes) {

		if (RingWriter.tryWriteFragment(toCon, ConInConst.MSG_CON_IN_CONNECT)) {
			RingWriter.writeASCII(toCon, ConInConst.CON_IN_CONNECT_FIELD_URL, url);
			
			final int bytePos = RingBuffer.bytesWorkingHeadPosition(toCon);
			byte[] byteBuffer = RingBuffer.byteBuffer(toCon);
			int byteMask = RingBuffer.byteMask(toCon);
						
			int len = MQTTEncoder.buildConnectPacket(bytePos, byteBuffer, byteMask, settings.ttlSec, conFlags, 
					                                 settings.clientId, 0 , settings.clientId.length, 0xFFFF,
					                                 willTopic, 0 , willTopic.length, 0xFFFF,
					                                 willMessageBytes, 0, willMessageBytes.length, 0xFFFF,
					                                 username, 0, username.length, 0xFFFF,
					                                 passwordBytes, 0, passwordBytes.length, 0xFFFF);
			assert(len>0);
			RingWriter.writeSpecialBytesPosAndLen(toCon, ConInConst.CON_IN_CONNECT_FIELD_PACKETDATA, len, bytePos);
					
			RingWriter.publishWrites(toCon);
			return true;
		} else {
			return false;
		}

	}

	protected boolean requestDisconnect() {
		
		if (RingWriter.tryWriteFragment(toCon, ConInConst.MSG_CON_IN_DISCONNECT)) {
			RingWriter.publishWrites(toCon);
			return true;
		} else {
			return false;
		}		
				
	}


	//TODO: B, delay generative testing for this component because it may turn out to not be an actor.
	
	
	protected long requestPublish(byte[] topic, int topicIdx, int topicLength, int topicMask, 
			                   int QualityOfService, int retain, 
			                   byte[] payload, int payloadIdx, int payloadLength, int payloadMask) {
				
		if (packetId >= packetIdLimit) {
			//get next range
			if (RingBuffer.contentToLowLevelRead(idGenIn, sizeOfPacketIdFragment)) {				
				loadNextPacketIdRange();				
			} else {
				return -1;
			}	
		}
		////
		
		if (RingWriter.tryWriteFragment(toCon, ConInConst.MSG_CON_IN_PUBLISH)) {
						
			RingWriter.writeInt(toCon, ConInConst.CON_IN_PUBLISH_FIELD_QOS, QualityOfService);
			RingWriter.writeInt(toCon, ConInConst.CON_IN_PUBLISH_FIELD_PACKETID, packetId++);
						
			final int bytePos = RingBuffer.bytesWorkingHeadPosition(toCon);
			byte[] byteBuffer = RingBuffer.byteBuffer(toCon);
			int byteMask = RingBuffer.byteMask(toCon);
			
			int len = MQTTEncoder.buildPublishPacket(bytePos, byteBuffer, byteMask, QualityOfService, retain, 
					                topic, topicIdx, topicLength, topicMask, 
					                payload, payloadIdx, payloadLength, payloadMask);
			RingWriter.writeSpecialBytesPosAndLen(toCon, ConInConst.CON_IN_PUBLISH_FIELD_PACKETDATA, len, bytePos);
				
			RingWriter.publishWrites(toCon);
		} else {
			return -1;
		}
				
		return RingBuffer.workingHeadPosition(toCon);
	}


	private void loadNextPacketIdRange() {
		int msgIdx = RingBuffer.takeMsgIdx(idGenIn);
		assert(theOneMsg == msgIdx);
		
		int range = RingBuffer.takeValue(idGenIn);
		packetId = 0xFFFF&range;
		packetIdLimit = 0xFFFF&(range>>16); 
						
		RingBuffer.releaseReads(idGenIn);
		RingBuffer.confirmLowLevelRead(idGenIn, sizeOfPacketIdFragment);
	}
	
	protected long requestReleasedPosition() {
		return RingBuffer.tailPosition(toCon);
	}
	
}

package com.ociweb.gateway.client;

import java.nio.ByteBuffer;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.ring.RingReader;
import com.ociweb.pronghorn.ring.RingWriter;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class APIStage extends PronghornStage {

	private final RingBuffer fromCon; //directly populated by external method calls, eg unknown thread
	private final RingBuffer idGenIn; //used by same external thread
	private final RingBuffer toCon;
	 	
	private final ConOutConst fromConnectionConst;
	private final ConInConst toConnectionConst;

	private int packetId = -1;
	private int packetIdLimit = -1;
	
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
		
        this.fromConnectionConst = new ConOutConst(RingBuffer.from(fromCon));
        this.toConnectionConst = new ConInConst(RingBuffer.from(toCon)); 		  
	  	
        this.sizeOfPacketIdFragment = RingBuffer.from(idGenIn).fragDataSize[theOneMsg];
        
	}
	
	

	@Override
	public void run() {
		
		//loop is for subscribers?
		while (RingReader.tryReadFragment(fromCon)) {			
			RingReader.releaseReadLock(fromCon);		
		}
		
	}

	
	public boolean requestConnect(CharSequence url, boolean someFlags) {

		if (RingWriter.tryWriteFragment(toCon, toConnectionConst.MSG_CON_IN_CONNECT)) {
			RingWriter.writeASCII(toCon, toConnectionConst.CON_IN_CONNECT_FIELD_URL, url);
			
			ByteBuffer src = null; //TODO: build up message based on spec.
			int len = 0;
			RingWriter.writeBytes(toCon, toConnectionConst.CON_IN_CONNECT_FIELD_PACKETDATA, src, len);
			
			
			RingWriter.publishWrites(toCon);
			return true;
		} else {
			return false;
		}

	}



	public boolean requestDisconnect() {
		
		if (RingWriter.tryWriteFragment(toCon, toConnectionConst.MSG_CON_IN_DISCONNECT)) {
			RingWriter.publishWrites(toCon);
			return true;
		} else {
			return false;
		}		
				
	}


	
	public long requestPublish(CharSequence topic, int QualityOfService, CharSequence payload) {
				
		if (packetId >= packetIdLimit) {
			//get next range
			if (RingBuffer.contentToLowLevelRead(idGenIn, sizeOfPacketIdFragment)) {				
				loadNextPacketIdRange();				
			} else {
				return -1;
			}	
		}
		////
		
		if (RingWriter.tryWriteFragment(toCon, toConnectionConst.MSG_CON_IN_PUBLISH)) {
			
			
			RingWriter.writeInt(toCon, toConnectionConst.CON_IN_PUBLISH_FIELD_QOS, QualityOfService);
			RingWriter.writeInt(toCon, toConnectionConst.CON_IN_PUBLISH_FIELD_PACKETID, packetId++);
			
			ByteBuffer src = null; //TODO: build up message based on spec.
			int len = 0;
			RingWriter.writeBytes(toCon, toConnectionConst.CON_IN_PUBLISH_FIELD_PACKETDATA, src, len);
			
			RingWriter.publishWrites(toCon);
		} else {
			return -1;
		}
				
		return RingBuffer.workingHeadPosition(toCon);
	}



	private void loadNextPacketIdRange() {
		int msgIdx = RingBuffer.takeMsgIdx(idGenIn);
		assert(theOneMsg == msgIdx);
		
		int xx = RingBuffer.takeValue(idGenIn);
		packetId = 0xFFFF&xx;
		packetIdLimit = 0xFFFF&(xx>>16); 
						
		RingBuffer.releaseReads(idGenIn);
		RingBuffer.confirmLowLevelRead(idGenIn, sizeOfPacketIdFragment);
	}
	
	public long requestReleasedPosition() {
		return RingBuffer.tailPosition(toCon);
	}
	
	public long publish(CharSequence topic, int QualityOfService, ByteBuffer payload) {
		if (packetId >= packetIdLimit) {
			//get next range
			if (RingBuffer.contentToLowLevelRead(idGenIn, sizeOfPacketIdFragment)) {				
				loadNextPacketIdRange();				
			} else {
				return -1;
			}	
		}
		////
		
		if (RingWriter.tryWriteFragment(toCon, toConnectionConst.MSG_CON_IN_PUBLISH)) {
			
			
			RingWriter.writeInt(toCon, toConnectionConst.CON_IN_PUBLISH_FIELD_QOS, QualityOfService);
			RingWriter.writeInt(toCon, toConnectionConst.CON_IN_PUBLISH_FIELD_PACKETID, packetId++);
			
			ByteBuffer src = null; //TODO: build up message based on spec.
			int len = 0;
			RingWriter.writeBytes(toCon, toConnectionConst.CON_IN_PUBLISH_FIELD_PACKETDATA, src, len);
			
			RingWriter.publishWrites(toCon);
		} else {
			return -1;
		}
				
		return RingBuffer.workingHeadPosition(toCon);
	}
	
	public long publish(CharSequence topic, int QualityOfService, byte[] payload, int payloadOffset, int payloadLength) {
		if (packetId >= packetIdLimit) {
			//get next range
			if (RingBuffer.contentToLowLevelRead(idGenIn, sizeOfPacketIdFragment)) {				
				loadNextPacketIdRange();				
			} else {
				return -1;
			}	
		}
		////
		
		if (RingWriter.tryWriteFragment(toCon, toConnectionConst.MSG_CON_IN_PUBLISH)) {
			
			
			RingWriter.writeInt(toCon, toConnectionConst.CON_IN_PUBLISH_FIELD_QOS, QualityOfService);
			RingWriter.writeInt(toCon, toConnectionConst.CON_IN_PUBLISH_FIELD_PACKETID, packetId++);
			
			ByteBuffer src = null; //TODO: build up message based on spec.
			int len = 0;
			RingWriter.writeBytes(toCon, toConnectionConst.CON_IN_PUBLISH_FIELD_PACKETDATA, src, len);
			
			RingWriter.publishWrites(toCon);
		} else {
			return -1;
		}
				
		return RingBuffer.workingHeadPosition(toCon);
	}
	


}

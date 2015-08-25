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
	 	
	private int nextFreePacketId = -1;
	private int nextFreePacketIdLimit = -1;
	private final int ttlSec;
	
	public final byte[] clientId = "somename for this client in UTF8".getBytes(); //same for entire run

	/**
  	 * This first implementation is kept simple until we get a working project.
  	 * 
  	 * The API may need some adjustments based on use cases.
  	 * 
  	 */
	
	private final int sizeOfPacketIdFragment;
	private static final int theOneMsg = 0;// there is only 1 message supported by this stage
	
	
	protected APIStage(GraphManager graphManager, RingBuffer idGenIn, RingBuffer fromC, RingBuffer toC, int ttlSec) {
		super(graphManager, new RingBuffer[]{idGenIn,fromC}, toC);
	
		this.idGenIn = idGenIn;
		this.fromCon = fromC;
		this.toCon = toC;
		
		this.ttlSec = ttlSec;
			  	
        this.sizeOfPacketIdFragment = RingBuffer.from(idGenIn).fragDataSize[theOneMsg];
        
        //add one more ring buffer so apps can write directly to it since this stage needs to copy from something.
        //this makes testing much easier, it makes integration tighter
        //it may add a copy?
        
        //must be set so this stage will get shut down and ignore the fact that is has un-consumed messages coming in 
        GraphManager.addAnnotation(graphManager,GraphManager.PRODUCER, GraphManager.PRODUCER, this);
        
	}
	
	

	@Override
	public void run() {
		
		while (RingReader.tryReadFragment(fromCon)) {	
			int msgIdx = RingReader.getMsgIdx(fromCon);
			int packetId;
			switch(msgIdx) {
				case ConOutConst.MSG_CON_OUT_CONNACK_OK:
					newConnection();
				break;	
				case ConOutConst.MSG_CON_OUT_CONNACK_ID:
				case ConOutConst.MSG_CON_OUT_CONNACK_AUTH:
				case ConOutConst.MSG_CON_OUT_CONNACK_PROTO:
				case ConOutConst.MSG_CON_OUT_CONNACK_SERVER:
				case ConOutConst.MSG_CON_OUT_CONNACK_USER:
					newConnectionError(msgIdx);
				break;	
				case ConOutConst.MSG_CON_OUT_PUB_REL:
				    packetId = RingReader.readInt(fromCon, ConOutConst.CON_OUT_PUB_REL_FIELD_PACKETID);
				    //subscriber logic
				    //TODO: now send pubcomp message to be sent 
				    
				    
				break;
				case ConOutConst.MSG_CON_OUT_PUB_ACK:
				    packetId = RingReader.readInt(fromCon, ConOutConst.CON_OUT_PUB_ACK_FIELD_PACKETID);
				    //System.out.println("ack packet "+packetId+" "+fromCon);
				    ackReceived1(packetId);
				    
				break;    
				case ConOutConst.MSG_CON_OUT_PUB_REC:
				    packetId = RingReader.readInt(fromCon, ConOutConst.CON_OUT_PUB_REC_FIELD_PACKETID);
				    
				    ackReceived2(packetId);
				    
				    while (!RingWriter.tryWriteFragment(toCon, ConInConst.MSG_CON_IN_PUB_REL)) {	//TODO: rewrite as non-blocking. do not read unless we have this output room!			        
				    }
				    
				    RingWriter.writeInt(toCon,  ConInConst.CON_IN_PUB_REL_FIELD_PACKETID, packetId);
				   
		            final int bytePos = RingBuffer.bytesWorkingHeadPosition(toCon);
		            byte[] byteBuffer = RingBuffer.byteBuffer(toCon);
		            int byteMask = RingBuffer.byteMask(toCon);
		            
				    int len = MQTTEncoder.buildPubRelPacket(bytePos, byteBuffer, byteMask, packetId);
	                             
		            RingWriter.writeSpecialBytesPosAndLen(toCon, ConInConst.CON_IN_PUB_REL_FIELD_PACKETDATA, len, bytePos);
		            
		            RingWriter.publishWrites(toCon);
				    
				break;
				default:
					
			}
			RingReader.releaseReadLock(fromCon);
			//System.out.println("                "+fromCon);
		}
		businessLogic();
	}
	
	public void newConnection() {
		
	}
	
	
    protected void ackReceived1(int packetId) {
        ackReceived(packetId,1);
    }
    
    protected void ackReceived2(int packetId) {
        ackReceived(packetId,2);
    }    
	
    protected void ackReceived(int packetId, int qos) {
	    
	}
	
	public void newConnectionError(int err) {		
	}	
	
	public void businessLogic() {
	}
	
	
	//caller must pre-encode these fields so they can be re-used for mutiple calls?
	//but this connect would be called rarely?
	
	//TODO: these methods may be extracted because they need not be part of an actor, 
	//NOTE: these methods are not thread safe and are intended for sequential use.
	
	protected boolean requestConnect(CharSequence url, int conFlags, byte[] willTopic, int willTopicIdx, int willTopicLength, int willTopicMask,  
	                                  byte[] willMessageBytes, int willMessageBytesIdx, int willMessageBytesLength, int willMessageBytesMask,
	                                  byte[] username, byte[] passwordBytes) {

		if (RingWriter.tryWriteFragment(toCon, ConInConst.MSG_CON_IN_CONNECT)) {
									
			RingWriter.writeASCII(toCon, ConInConst.CON_IN_CONNECT_FIELD_URL, url);
			
			//this is the high level API however we are writing bytes to to the end of the unstructured buffer.
			final int bytePos = RingBuffer.bytesWorkingHeadPosition(toCon);
			byte[] byteBuffer = RingBuffer.byteBuffer(toCon);
			int byteMask = RingBuffer.byteMask(toCon);
						
			int len = MQTTEncoder.buildConnectPacket(bytePos, byteBuffer, byteMask, ttlSec, conFlags, 
					                                 clientId, 0 , clientId.length, 0xFFFF,
					                                 willTopic, willTopicIdx , willTopicLength, willTopicMask,
					                                 willMessageBytes, willMessageBytesIdx, willMessageBytesLength, willMessageBytesMask,
					                                 username, 0, username.length, 0xFFFF, //TODO: add rest of fields
					                                 passwordBytes, 0, passwordBytes.length, 0xFFFF);//TODO: add rest of fields
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
		    
		    System.out.println("requext disconnect");
		    
			RingWriter.publishWrites(toCon);
			return true;
		} else {
			return false;
		}		
				
	}

	protected int requestPublish(byte[] topic, int topicIdx, int topicLength, int topicMask, 
			                   int qualityOfService, int retain, 
			                   byte[] payload, int payloadIdx, int payloadLength, int payloadMask) {
				
		if (nextFreePacketId >= nextFreePacketIdLimit) {
			//get next range
			if (RingBuffer.contentToLowLevelRead(idGenIn, sizeOfPacketIdFragment)) {				
				loadNextPacketIdRange();				
			} else {
				return -1;
			}	
		}
		////
		
		if (RingWriter.tryWriteFragment(toCon, ConInConst.MSG_CON_IN_PUBLISH)) {
						
			RingWriter.writeInt(toCon, ConInConst.CON_IN_PUBLISH_FIELD_QOS, qualityOfService);
			
			int localPacketId = (0==qualityOfService) ? -1 : nextFreePacketId++;
			
			RingWriter.writeInt(toCon, ConInConst.CON_IN_PUBLISH_FIELD_PACKETID, localPacketId);
						
			final int bytePos = RingBuffer.bytesWorkingHeadPosition(toCon);
			byte[] byteBuffer = RingBuffer.byteBuffer(toCon);
			int byteMask = RingBuffer.byteMask(toCon);
			
			int len = MQTTEncoder.buildPublishPacket(bytePos, byteBuffer, byteMask, qualityOfService, retain, 
					                topic, topicIdx, topicLength, topicMask, 
					                payload, payloadIdx, payloadLength, payloadMask, localPacketId);
			RingWriter.writeSpecialBytesPosAndLen(toCon, ConInConst.CON_IN_PUBLISH_FIELD_PACKETDATA, len, bytePos);
				
			RingWriter.publishWrites(toCon);
						
			return localPacketId<0 ? 0 : localPacketId;//TODO: we have no id for qos 0 this is dirty.
		} else {
			return -1;
		}
				
	}


	private void loadNextPacketIdRange() {
		int msgIdx = RingBuffer.takeMsgIdx(idGenIn);
		assert(theOneMsg == msgIdx);
		
		int range = RingBuffer.takeValue(idGenIn);
		nextFreePacketId = 0xFFFF&range;
		nextFreePacketIdLimit = 0xFFFF&(range>>16); 
						
		RingBuffer.releaseReads(idGenIn);
		RingBuffer.confirmLowLevelRead(idGenIn, sizeOfPacketIdFragment);
	}
	
}

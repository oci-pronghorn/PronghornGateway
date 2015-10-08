package com.ociweb.gateway.client;

import java.util.Arrays;

import com.ociweb.pronghorn.pipe.Pipe;
import com.ociweb.pronghorn.pipe.PipeReader;
import com.ociweb.pronghorn.pipe.PipeWriter;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class APIStage extends PronghornStage {

	private final Pipe fromCon; //directly populated by external method calls, eg unknown thread
	private final Pipe idGenIn; //used by same external thread
	private final Pipe toCon;
	 	
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
	private final int sizeOfPubRel;
	private static final int theOneMsg = 0;// there is only 1 message supported by this stage
	
	
	protected APIStage(GraphManager graphManager, Pipe idGenIn, Pipe fromC, Pipe toC, int ttlSec) {
		super(graphManager, new Pipe[]{idGenIn,fromC}, toC);
	
		this.idGenIn = idGenIn;
		this.fromCon = fromC;
		this.toCon = toC;
		
		this.ttlSec = ttlSec;
			  	
        this.sizeOfPacketIdFragment = Pipe.from(idGenIn).fragDataSize[theOneMsg];
        this.sizeOfPubRel = Pipe.from(toCon).fragDataSize[ConInConst.MSG_CON_IN_PUB_REL];
        
        //add one more ring buffer so apps can write directly to it since this stage needs to copy from something.
        //this makes testing much easier, it makes integration tighter
        //it may add a copy?
        
        //must be set so this stage will get shut down and ignore the fact that is has un-consumed messages coming in 
        GraphManager.addNota(graphManager,GraphManager.PRODUCER, GraphManager.PRODUCER, this);
        
	}
	
	

	@Override
	public void run() {
		 
	   
		while ( PipeWriter.hasRoomForFragmentOfSize(toCon, sizeOfPubRel) && PipeReader.tryReadFragment(fromCon)) {	
			int msgIdx = PipeReader.getMsgIdx(fromCon);
			
			System.out.println("got message: "+msgIdx);
			
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
				    packetId = PipeReader.readInt(fromCon, ConOutConst.CON_OUT_PUB_REL_FIELD_PACKETID);
				    //subscriber logic
				    //TODO: now send pubcomp message to be sent 
				    
				    
				break;
				case ConOutConst.MSG_CON_OUT_PUB_ACK:
				    packetId = PipeReader.readInt(fromCon, ConOutConst.CON_OUT_PUB_ACK_FIELD_PACKETID);
				    //System.out.println("ack packet "+packetId+" "+fromCon);
				    ackReceived1(packetId);
				    
				break;    
				case ConOutConst.MSG_CON_OUT_PUB_REC:
				    System.out.println("ZZZZZZZZZZZZZZZZZZZZZZZZZZZzz  Got PubRec fromserver ");
				    
				    packetId = PipeReader.readInt(fromCon, ConOutConst.CON_OUT_PUB_REC_FIELD_PACKETID);
				    System.out.println("for packet:"+packetId);
				    
				    ackReceived2(packetId);
				    
				    if (!PipeWriter.tryWriteFragment(toCon, ConInConst.MSG_CON_IN_PUB_REL)) {
				       throw new UnsupportedOperationException("Expected room in pipe due to the hasRoomForFragmentOfSize check."); 
				    }
				    
				    PipeWriter.writeInt(toCon,  ConInConst.CON_IN_PUB_REL_FIELD_PACKETID, packetId);
				   
		            final int bytePos = Pipe.bytesWorkingHeadPosition(toCon);
		            byte[] byteBuffer = Pipe.byteBuffer(toCon);
		            int byteMask = Pipe.byteMask(toCon);
		            
				    int len = MQTTEncoder.buildPubRelPacket(bytePos, byteBuffer, byteMask, packetId);
	                             
		            PipeWriter.writeSpecialBytesPosAndLen(toCon, ConInConst.CON_IN_PUB_REL_FIELD_PACKETDATA, len, bytePos);
		            
		            PipeWriter.publishWrites(toCon);
				    
				break;
				default:
				    throw new UnsupportedOperationException("Unknown Mesage: "+msgIdx);
					
			}
			PipeReader.releaseReadLock(fromCon);
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

		if (PipeWriter.tryWriteFragment(toCon, ConInConst.MSG_CON_IN_CONNECT)) {
			
			PipeWriter.writeASCII(toCon, ConInConst.CON_IN_CONNECT_FIELD_URL, url);
						
			//this is the high level API however we are writing bytes to to the end of the unstructured buffer.
			final int bytePos = Pipe.bytesWorkingHeadPosition(toCon);
			byte[] byteBuffer = Pipe.byteBuffer(toCon);
			int byteMask = Pipe.byteMask(toCon);
						
			int len = MQTTEncoder.buildConnectPacket(bytePos, byteBuffer, byteMask, ttlSec, conFlags, 
					                                 clientId, 0 , clientId.length, 0xFFFF,
					                                 willTopic, willTopicIdx , willTopicLength, willTopicMask,
					                                 willMessageBytes, willMessageBytesIdx, willMessageBytesLength, willMessageBytesMask,
					                                 username, 0, username.length, 0xFFFF, //TODO: add rest of fields
					                                 passwordBytes, 0, passwordBytes.length, 0xFFFF);//TODO: add rest of fields
			assert(len>0);
			PipeWriter.writeSpecialBytesPosAndLen(toCon, ConInConst.CON_IN_CONNECT_FIELD_PACKETDATA, len, bytePos);
			
			PipeWriter.publishWrites(toCon);
			return true;
		} else {
			return false;
		}

	}

	protected boolean requestDisconnect() {
		
	//    System.err.println("AAA :"+RingBuffer.bytesWriteBase(toCon));
	    
		if (PipeWriter.tryWriteFragment(toCon, ConInConst.MSG_CON_IN_DISCONNECT)) {
		//    System.err.println("BBB :"+RingBuffer.bytesWriteBase(toCon));
			PipeWriter.publishWrites(toCon);
	//		 System.err.println("CCCC :"+RingBuffer.bytesWriteBase(toCon));
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
			if (Pipe.hasContentToRead(idGenIn, sizeOfPacketIdFragment)) {				
				loadNextPacketIdRange();				
			} else {
				return -1;
			}	
		}
		////
		
		if (PipeWriter.tryWriteFragment(toCon, ConInConst.MSG_CON_IN_PUBLISH)) {
						
		    
			PipeWriter.writeInt(toCon, ConInConst.CON_IN_PUBLISH_FIELD_QOS, qualityOfService);
			
			int localPacketId = (0==qualityOfService) ? -1 : nextFreePacketId++;
						
			PipeWriter.writeInt(toCon, ConInConst.CON_IN_PUBLISH_FIELD_PACKETID, localPacketId);
						
			final int bytePos = Pipe.bytesWorkingHeadPosition(toCon);
			byte[] byteBuffer = Pipe.byteBuffer(toCon);
			int byteMask = Pipe.byteMask(toCon);
			
			int len = MQTTEncoder.buildPublishPacket(bytePos, byteBuffer, byteMask, qualityOfService, retain, 
					                topic, topicIdx, topicLength, topicMask, 
					                payload, payloadIdx, payloadLength, payloadMask, localPacketId);
			PipeWriter.writeSpecialBytesPosAndLen(toCon, ConInConst.CON_IN_PUBLISH_FIELD_PACKETDATA, len, bytePos);
				
			PipeWriter.publishWrites(toCon);

			return localPacketId<0 ? 0 : localPacketId;//TODO: we have no id for qos 0 this is dirty.
		} else {
			return -1;
		}
				
	}


	private void loadNextPacketIdRange() {
		int msgIdx = Pipe.takeMsgIdx(idGenIn);
		assert(theOneMsg == msgIdx);
		
		int range = Pipe.takeValue(idGenIn);
		nextFreePacketId = 0xFFFF&range;
		nextFreePacketIdLimit = 0xFFFF&(range>>16); 
						
		Pipe.releaseReads(idGenIn);
		Pipe.confirmLowLevelRead(idGenIn, sizeOfPacketIdFragment);
	}
	
}

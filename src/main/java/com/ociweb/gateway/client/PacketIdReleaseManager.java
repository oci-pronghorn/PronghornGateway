package com.ociweb.gateway.client;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.ring.RingReader;

public class PacketIdReleaseManager {
    private int firstConsumedPacketId;
    private int lastConsumedPacketId;

    public PacketIdReleaseManager() {
        this.firstConsumedPacketId = -1;
        this.lastConsumedPacketId = -1;
    }

    private static void releasePacketId(PacketIdReleaseManager instance, ConnectionStage connectionStage, int packetId) {
        
        //most common expected case first
        if (packetId == instance.lastConsumedPacketId + 1) {
            instance.lastConsumedPacketId = packetId;
        } else if (packetId == instance.firstConsumedPacketId - 1) {
            instance.firstConsumedPacketId = packetId;
        } else {
            //push the old range back to idGen
            int result = (instance.firstConsumedPacketId<<16) | (instance.lastConsumedPacketId+1);
            //before parse of incoming message we have already checked that there is room on the outgoing queue
            RingBuffer.addMsgIdx(connectionStage.idGenOut, connectionStage.getIdMessageIdx);   
            RingBuffer.addIntValue(result, connectionStage.idGenOut);
            RingBuffer.publishWrites(connectionStage.idGenOut);  
            RingBuffer.confirmLowLevelWrite(connectionStage.idGenOut, connectionStage.genIdMessageSize);
            //now use packetIda as the beginning of a new group
            instance.firstConsumedPacketId = instance.lastConsumedPacketId = packetId;
        }
    }

    public static void releaseMessage(PacketIdReleaseManager instance, ConnectionStage connectionStage, int packetId, int originalQoS) {	    
    
            RingBuffer.replayUnReleased(connectionStage.apiIn);
            
            boolean releaseEndFound = false;
            boolean replaying = true;
            while (replaying && RingReader.tryReadFragment(connectionStage.apiIn)) {
                //always checks one more, the current one which is not technically part of the replay.
                replaying = RingBuffer.isReplaying(connectionStage.apiIn);	            
                int msgIdx = RingReader.getMsgIdx(connectionStage.apiIn);
                //based on type 
                if (originalQoS < 3 && ConInConst.MSG_CON_IN_PUBLISH == msgIdx) {                    
                    releaseEndFound = releasePublishMessage(instance, connectionStage, packetId, originalQoS, releaseEndFound);
                } else if (ConInConst.MSG_CON_IN_PUB_REL == msgIdx) {                    
                    releasePubRelMessage(instance, connectionStage, packetId);
                } else {
                    System.err.println("unkown "+msgIdx);
                }	            	            
                if (!releaseEndFound) {
                    //release everything up to this point, only done while
                    //the end is not found because we can only release the contiguous 
                    //messages until we reach the fist unconfirmed message.
                    RingBuffer.releaseReadLock(connectionStage.apiIn);
                    //System.err.println("              pipe "+apiIn);
                }
            }
            RingBuffer.cancelReplay(connectionStage.apiIn);
            RingBuffer.releaseAllBatchedReads(connectionStage.apiIn);	        	    		
    }

    private static void releasePubRelMessage(PacketIdReleaseManager instance, ConnectionStage connectionStage,
            int packetId) {
        int msgPacketId = RingReader.readInt(connectionStage.apiIn, ConInConst.CON_IN_PUB_REL_FIELD_PACKETID);
        if (packetId == msgPacketId) {
        
            System.err.println("release for packet pubRel "+packetId);
            
            //we found the pubRel now clear it by setting the packet id negative
            RingReader.readIntSecure(connectionStage.apiIn, ConInConst.CON_IN_PUB_REL_FIELD_PACKETID,-msgPacketId);
            PacketIdReleaseManager.releasePacketId(instance, connectionStage, packetId);//This is the end of the QoS2 publish
        }
    }

    private static boolean releasePublishMessage(PacketIdReleaseManager instance, ConnectionStage connectionStage,
            int packetId, int originalQoS, boolean releaseEndFound) {
        int msgPacketId = RingReader.readInt(connectionStage.apiIn, ConInConst.CON_IN_PUBLISH_FIELD_PACKETID);
        if (packetId == msgPacketId) {	                    
            //we found it, now clear the QoS and confirm that it was valid
            int qos = RingReader.readIntSecure(connectionStage.apiIn, ConInConst.CON_IN_PUBLISH_FIELD_QOS,-originalQoS);
            if (1==qos) {
                PacketIdReleaseManager.releasePacketId(instance, connectionStage, packetId);//This is the end of the QoS1 publish	                        
            } else if (qos<=0) {
                //this conditional is checked last because it is not expected to be frequent
                ConnectionStage.log.warn("reduntant ack");
            }
        } else {
            int qos = RingReader.readInt(connectionStage.apiIn, ConInConst.CON_IN_PUBLISH_FIELD_QOS);
            releaseEndFound |= (qos>0);//skip over and release all qos zeros this only stops on un-confirmed 1 and 2	                
        }
        return releaseEndFound;
    }
}
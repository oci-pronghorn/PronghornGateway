package com.ociweb.gateway.client;

import java.util.Arrays;

import com.ociweb.pronghorn.pipe.Pipe;

public class MQTTEncoder {

	//connect flags
	//bit 7 user name
	//bit 6 pass
	//bit 5 will retain
	//bit 4 will qos
	//bit 3 will qos
	//bit 2 will flag
	//bit 1 clean session
	//bit 0 reserved zero
	public static final int CONNECT_FLAG_RESERVED_0      = 1;
	public static final int CONNECT_FLAG_CLEAN_SESSION_1 = 2;
	public static final int CONNECT_FLAG_WILL_FLAG_2     = 4;
	public static final int CONNECT_FLAG_WILL_QOS_3      = 8;
	public static final int CONNECT_FLAG_WILL_QOS_4      = 16;
	public static final int CONNECT_FLAG_WILL_RETAIN_5   = 32;
	public static final int CONNECT_FLAG_PASSWORD_6      = 64;
	public static final int CONNECT_FLAG_USERNAME_7      = 128;
	
	
	static int buildConnectPacket(int bytePos, byte[] byteBuffer, int byteMask, int ttlSec, int conFlags, 
			                      byte[] clientId, int clientIdIdx, int clientIdLength, int clientIdMask,
			                      byte[] willTopic, int willTopicIdx, int willTopicLength, int willTopicMask,
			                      byte[] willMessage, int willMessageIdx, int willMessageLength, int willMessageMask,
			                      byte[] user, int userIdx, int userLength, int userMask,
			                      byte[] pass, int passIdx, int passLength, int passMask) {
		final int firstPos = bytePos;
		
		//The Remaining Length is the number of bytes remaining within the current packet, including data in the
		//variable header and the payload. The Remaining Length does not include the bytes used to encode the
		//Remaining Length.
		int length = 6+1+1+2;//fixed portion from protoName level flags and keep alive
		
		length += (2+clientId.length);//encoded clientId
		
		
		
		if (0!=(CONNECT_FLAG_WILL_FLAG_2&conFlags)) {
			length += (2+willTopic.length);
			length += (2+willMessage.length);
		}
		
		if (0!=(CONNECT_FLAG_USERNAME_7&conFlags)) {
			length += (2+user.length);
		}
		
		if (0!=(CONNECT_FLAG_PASSWORD_6&conFlags)) {
			length += (2+pass.length);
		}
		assert(length>0) : "Code error above this point, length must always be positive";
		if (length>(1<<28)) {
			//TODO: A, text fields are too large where do we report this error
			
		}
				
		bytePos = appendFixedHeader(bytePos, byteMask, byteBuffer, 0x10, length); //const and remaining length, 2  bytes
				
		//variable header
		bytePos = appendFixedProtoName(bytePos, byteMask, byteBuffer); //const 6 bytes
		bytePos = appendByte(bytePos, byteMask, byteBuffer, 4); //const 1 byte for version		
		bytePos = appendByte(bytePos, byteMask, byteBuffer, conFlags); //8 bits or togehter, if clientId zero length must set clear
		bytePos = appendShort(bytePos, byteMask, byteBuffer, ttlSec); //seconds < 16 bits
		
		
		//payload
		bytePos = appendShort(bytePos, byteMask, byteBuffer, clientIdLength);
		bytePos = appendBytes(bytePos, byteMask, byteBuffer, clientId, clientIdIdx, clientIdLength, clientIdMask);
		
		
		if (0!=(CONNECT_FLAG_WILL_FLAG_2&conFlags)) {
			
			bytePos = appendShort(bytePos, byteMask, byteBuffer, willTopicLength);
			bytePos = appendBytes(bytePos, byteMask, byteBuffer, willTopic, willTopicIdx, willTopicLength, willTopicMask);
			bytePos = appendShort(bytePos, byteMask, byteBuffer, willMessageLength);
			bytePos = appendBytes(bytePos, byteMask, byteBuffer, willMessage, willMessageIdx, willMessageLength, willMessageMask);
			
		}
		
		if (0!=(CONNECT_FLAG_USERNAME_7&conFlags)) {
			bytePos = appendShort(bytePos, byteMask, byteBuffer, userLength);
			bytePos = appendBytes(bytePos, byteMask, byteBuffer, user, userIdx, userLength, userMask);	//if user flag on	
		}
		
		if (0!=(CONNECT_FLAG_PASSWORD_6&conFlags)) {
			bytePos = appendShort(bytePos, byteMask, byteBuffer, passLength);
			bytePos = appendBytes(bytePos, byteMask, byteBuffer, pass, passIdx, passLength, passMask); //if pass flag on
		}
		
		//total length is needed to close out this var length field in the queue
		return bytePos-firstPos;
	}
	
	
	private static int appendFixedHeader(int bytePos, int byteMask, byte[] byteBuffer, int leadingByte, int length) {
		
		byteBuffer[byteMask&bytePos++] = (byte)(0xFF&leadingByte);
		return encodeVarLength(byteBuffer,bytePos,byteMask,length);
		
	}
	
	static int encodeVarLength(byte[] target, int idx,  int mask, int x) {		
		byte encodedByte = (byte)(x & 0x7F);
		x = x >> 7;
		while (x>0) {
			target[mask&idx++]=(byte)(0x80 | encodedByte);			
			encodedByte = (byte)(x & 0x7F);
			 x = x >> 7;
		}
		target[mask&idx++]=(byte)(0xFF & encodedByte);	
		return idx;
	}
	
	
	
	private static int appendFixedProtoName(int bytePos, int byteMask, byte[] byteBuffer) {
		//NOTE: this is hardcoded from 3.1.1 spec and may not be compatible with 3.1
		byteBuffer[byteMask&bytePos++] = 0; //MSB
		byteBuffer[byteMask&bytePos++] = 4; //LSB
		byteBuffer[byteMask&bytePos++] = 'M';
		byteBuffer[byteMask&bytePos++] = 'Q';
		byteBuffer[byteMask&bytePos++] = 'T';
		byteBuffer[byteMask&bytePos++] = 'T';
		
		return bytePos;
	}

	private static int appendByte(int bytePos, int byteMask, byte[] byteBuffer, int value) {
		byteBuffer[byteMask&bytePos++] = (byte)value;
		return bytePos;
	}

	private static int appendShort(int bytePos, int byteMask, byte[] byteBuffer, int value) {
		byteBuffer[byteMask & bytePos++] = (byte)(0xFF&(value>>8));
		byteBuffer[byteMask & bytePos++] = (byte)(0xFF&value);
		return bytePos;
	}

	private static int appendBytes(int targetPos, int targetMask, byte[] targetBuffer, byte[] srcBuffer, int srcIdx, int srcLength, int srcMask) {
		Pipe.copyBytesFromToRing(srcBuffer, srcIdx, srcMask, targetBuffer, targetPos, targetMask, srcLength);
		return targetPos+srcLength;
	}

		
	
    //TODO: B, Break up the packet into to splittable parts so we can use constants that need not be copied  through the ring buffer
	
	public static int buildPublishPacket(int bytePos, byte[] byteBuffer, int byteMask, int qos, int retain, 
			                             byte[] topic, int topicIdx, int topicLength, int topicMask,
			                             byte[] payload, int payloadIdx, int payloadLength, int payloadMask,  int packetId) {
		
		final int firstPos = bytePos;
		
		int length = topicLength + 2 + payloadLength + (packetId>=0 ? 2 : 0); //replace with  (0x2&(~packetId)>>30) //TOOD: B, or od this external and break method up.
		
		assert(length<(topicMask+payloadMask)) : "Length is far too large and can not be right"; //TODO: C, be sure server side checks this and rejects bad values.
				
		final int pubHead = 0x30 | (0x6&(qos<<1)) | 1&retain; //bit 3 dup is zero which is modified later
		bytePos = appendFixedHeader(bytePos, byteMask, byteBuffer, pubHead, length); //const and remaining length, 2  bytes
		assert(topicLength>0);
		
		//variable header
		bytePos = appendShort(bytePos, byteMask, byteBuffer, topicLength);
		bytePos = appendBytes(bytePos, byteMask, byteBuffer, topic, topicIdx, topicLength, topicMask);
		
		if (packetId>=0) {//TODO: B, we have 3 conditionals around this packet id change on QOS, must remove.\
		 //   byte a = (byte)(0xFF&(packetId>>8));
		//    byte b = (byte)(0xFF&packetId);
		    bytePos = appendShort(bytePos, byteMask, byteBuffer, packetId);

	//	    System.out.println("APPENDED packet id to "+packetId+"  "+byteBuffer[byteMask&(bytePos-2)]+"  "+byteBuffer[byteMask&(bytePos-1)]+"   "+a+"  "+b);
		    
		}
		
		
		//payload - note it does not record the length first, its just the remaining space
		bytePos = appendBytes(bytePos, byteMask, byteBuffer, payload, payloadIdx, payloadLength, payloadMask);
		
		
		
		//todo: BUILD THIS DEBUG MESSAGE INTO rINGbUFFER.
//		System.out.println("BUILT:"+Arrays.toString(Arrays.copyOfRange(byteBuffer, firstPos, bytePos)));
		//total length is needed to close out this var length field in the queue
		return bytePos-firstPos;
	}


    public static int buildPubRelPacket(int bytePos, byte[] byteBuffer, int byteMask, int packetId) {
        
        //0x62  type/reserved   0110 0010
        //0x02  remaining length
        //MSB PacketID high
        //LSB PacketID low

        byteBuffer[byteMask&bytePos++] = (byte)0x62;
        byteBuffer[byteMask&bytePos++] = (byte)0x02;
        appendShort(bytePos, byteMask, byteBuffer, packetId);
        return 4;
    }


}

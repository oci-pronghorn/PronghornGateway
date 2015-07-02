package com.ociweb.gateway.client;

import com.ociweb.pronghorn.ring.RingBuffer;

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
			                      byte[] clientId, int clientIdIdx, int clientIdLength,
			                      byte[] willTopic, int willTopicIdx, int willTopicLength,
			                      byte[] willMessage, int willMessageIdx, int willMessageLength,
			                      byte[] user, int userIdx, int userLength,
			                      byte[] pass, int passIdx, int passLength) {
		final int firstPos = bytePos;
		
		//The Remaining Length is the number of bytes remaining within the current packet, including data in the
		//variable header and the payload. The Remaining Length does not include the bytes used to encode the
		//Remaining Length.
		int length = 6+1+1+2;//fixed portion from protoName level flags and keep alive
		length += clientId.length;//encoded clientId
		
		if (0!=(CONNECT_FLAG_WILL_FLAG_2&conFlags)) {
			length += willTopic.length;
			length += willMessage.length;
		}
		
		if (0!=(CONNECT_FLAG_USERNAME_7&conFlags)) {
			length += user.length;
		}
		
		if (0!=(CONNECT_FLAG_PASSWORD_6&conFlags)) {
			length += pass.length;
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
		bytePos = appendBytes(bytePos, byteMask, byteBuffer, clientId, clientIdIdx, clientIdLength, 0xFFFF);
		if (0!=(CONNECT_FLAG_WILL_FLAG_2&conFlags)) {
			
			bytePos = appendShort(bytePos, byteMask, byteBuffer, willTopicLength);
			bytePos = appendBytes(bytePos, byteMask, byteBuffer, willTopic, willTopicIdx, willTopicLength, 0xFFFF);
			bytePos = appendShort(bytePos, byteMask, byteBuffer, willMessageLength);
			bytePos = appendBytes(bytePos, byteMask, byteBuffer, willMessage, willMessageIdx, willMessageLength, 0xFFFF);
			
		}
		
		if (0!=(CONNECT_FLAG_USERNAME_7&conFlags)) {
			bytePos = appendShort(bytePos, byteMask, byteBuffer, userLength);
			bytePos = appendBytes(bytePos, byteMask, byteBuffer, user, userIdx, userLength, 0xFFFF);	//if user flag on	
		}
		
		if (0!=(CONNECT_FLAG_PASSWORD_6&conFlags)) {
			bytePos = appendShort(bytePos, byteMask, byteBuffer, passLength);
			bytePos = appendBytes(bytePos, byteMask, byteBuffer, pass, passIdx, passLength, 0xFFFF); //if pass flag on
		}
		
		//total length is needed to close out this var length field in the queue
		return firstPos-bytePos;
	}
	
	
	private static int appendFixedHeader(int bytePos, int byteMask, byte[] byteBuffer, int leadingByte, int length) {
		
		byteBuffer[bytePos++] = (byte)(0xFF&leadingByte);
		return encodeVarLength(byteBuffer,bytePos,byteMask,length);
		
	}
	
	static int encodeVarLength(byte[] target, int idx,  int mask, int x) {		
		byte encodedByte = (byte)(x & 0x7F);
		x = x >> 7;
		while (x>0) {
			target[mask&idx++]=(byte)(128 | encodedByte);			
			encodedByte = (byte)(x & 0x7F);
			 x = x >> 7;
		}
		target[mask&idx++]=(byte)(0xFF&encodedByte);	
		return idx;
	}
	
	
	
	private static int appendFixedProtoName(int bytePos, int byteMask, byte[] byteBuffer) {
		//NOTE: this is hardcoded from 3.1.1 spec and may not be compatible with 3.1
		byteBuffer[bytePos++] = 0; //MSB
		byteBuffer[bytePos++] = 4; //LSB
		byteBuffer[bytePos++] = 'M';
		byteBuffer[bytePos++] = 'Q';
		byteBuffer[bytePos++] = 'T';
		byteBuffer[bytePos++] = 'T';
		
		return bytePos;
	}

	private static int appendByte(int bytePos, int byteMask, byte[] byteBuffer, int value) {
		byteBuffer[bytePos++] = (byte)value;
		return bytePos;
	}

	private static int appendShort(int bytePos, int byteMask, byte[] byteBuffer, int value) {
		byteBuffer[bytePos++] = (byte)(0xFF&(value>>8));
		byteBuffer[bytePos++] = (byte)(0xFF&value);
		return bytePos;
	}

	private static int appendBytes(int targetPos, int targetMask, byte[] targetBuffer, byte[] srcBuffer, int srcIdx, int srcLength, int srcMask) {
		RingBuffer.copyBytesFromToRing(srcBuffer, srcIdx, srcMask, targetBuffer, targetPos, targetMask, srcLength);
		return targetPos+srcLength;
	}


	//TODO: get jFAST utf8 encode method for use here.
	
	public static void buildUTF8(CharSequence charSeq) {
		
		int target = 0;
		byte[] targetBuf = new byte[10];
		int targetMask = 0xFFFFFFFF;
		
		int charCount = 10;
		int charOffset = 0;
		
	    int c = 0;
	    while (c < charCount) {
	    	target = RingBuffer.encodeSingleChar((int) charSeq.charAt(charOffset+c++), targetBuf, targetMask, target);
	    }
	}
	
	

	public static int buildPublishPacket(int bytePos, byte[] byteBuffer, int byteMask, int qos, int retain, 
			                             byte[] topic, int topicIdx, int topicLength,
			                             byte[] payload, int payloadIdx, int payloadLength) {
		
		final int firstPos = bytePos;
		
		int length = topic.length+2+payload.length;
		
		final int pubHead = 0x30 | (0x3&(qos<<1)) | 1&retain; //dup is zero that is modified later
		bytePos = appendFixedHeader(bytePos, byteMask, byteBuffer, pubHead, length); //const and remaining length, 2  bytes
		
		//variable header
		bytePos = appendShort(bytePos, byteMask, byteBuffer, topicLength);
		bytePos = appendBytes(bytePos, byteMask, byteBuffer, topic, topicIdx, topicLength, 0xFFFF);
		int packetId = -1;
		bytePos = appendShort(bytePos, byteMask, byteBuffer, packetId);
		//payload - note it does not record the length first, its just the remaining space
		bytePos = appendBytes(bytePos, byteMask, byteBuffer, payload, payloadIdx, payloadLength, 0xFFFF);
		
		//total length is needed to close out this var length field in the queue
		return firstPos-bytePos;
	}


}

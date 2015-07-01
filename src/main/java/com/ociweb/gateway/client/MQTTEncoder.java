package com.ociweb.gateway.client;

import java.nio.ByteBuffer;

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
	
	
	static int buildConnectPacket(int bytePos, byte[] byteBuffer, int byteMask, int ttlSec, int conFlags, byte[] clientId, byte[] willTopic, byte[] willMessage, byte[] user, byte[] pass) {
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
		
		bytePos = appendFixedHeader(bytePos, byteMask, byteBuffer, length); //const and remaining length, 2  bytes
		
		//variable header
		bytePos = appendFixedProtoName(bytePos, byteMask, byteBuffer); //const 6 bytes
		bytePos = appendFixedProtoLevel(bytePos, byteMask, byteBuffer); //const 1 byte for version		
		bytePos = appendFixedConFlags(bytePos, byteMask, byteBuffer, conFlags); //8 bits or togehter, if clientId zero length must set clear
		bytePos = appendFixedKeepAlive(bytePos, byteMask, byteBuffer, ttlSec); //seconds < 16 bits
		
		//payload
		bytePos = appendClientId(bytePos, byteMask, byteBuffer, clientId);
		if (0!=(CONNECT_FLAG_WILL_FLAG_2&conFlags)) {
			bytePos = appendWillTopicMsg(bytePos, byteMask, byteBuffer, willTopic, willMessage); //if will flag on
		}
		
		if (0!=(CONNECT_FLAG_USERNAME_7&conFlags)) {
			bytePos = appendUser(bytePos, byteMask, byteBuffer, user);	//if user flag on	
		}
		
		if (0!=(CONNECT_FLAG_PASSWORD_6&conFlags)) {
			bytePos = appendPass(bytePos, byteMask, byteBuffer, pass); //if pass flag on
		}
		
		//total length is needed to close out this var length field in the queue
		return firstPos-bytePos;
	}
	
	
	private static int appendFixedHeader(int bytePos, int byteMask, byte[] byteBuffer, int length) {
		
		byteBuffer[bytePos++] = 0x10;
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

	private static int appendFixedProtoLevel(int bytePos, int byteMask, byte[] byteBuffer) {
		byteBuffer[bytePos++] = (byte)4;
		return bytePos;
	}

	private static int appendFixedConFlags(int bytePos, int byteMask, byte[] byteBuffer, int conFlags) {
		byteBuffer[bytePos++] = (byte)conFlags;
		return bytePos;
	}

	private static int appendFixedKeepAlive(int bytePos, int byteMask, byte[] byteBuffer, int ttlSec) {
		byteBuffer[bytePos++] = (byte)(0xFF&(ttlSec>>8));
		byteBuffer[bytePos++] = (byte)(0xFF&ttlSec);
		return bytePos;
	}

	private static int appendClientId(int bytePos, int byteMask, byte[] byteBuffer, byte[] clientId) {
		RingBuffer.copyBytesFromToRing(clientId, 0, 0xFFFF, byteBuffer, bytePos, byteMask, clientId.length);
		return bytePos+clientId.length;
	}

	private static int appendWillTopicMsg(int bytePos, int byteMask, byte[] byteBuffer, byte[] willTopic, byte[] willMessage) {
		RingBuffer.copyBytesFromToRing(willTopic, 0, 0xFFFF, byteBuffer, bytePos, byteMask, willTopic.length);
		RingBuffer.copyBytesFromToRing(willMessage, 0, 0xFFFF, byteBuffer, bytePos+willTopic.length, byteMask, willMessage.length);
		return bytePos+willTopic.length+willMessage.length;
	}

	private static int appendUser(int bytePos, int byteMask, byte[] byteBuffer, byte[] user) {
		RingBuffer.copyBytesFromToRing(user, 0, 0xFFFF, byteBuffer, bytePos, byteMask, user.length);
		return bytePos+user.length;
	}

	private static int appendPass(int bytePos, int byteMask, byte[] byteBuffer, byte[] pass) {
		RingBuffer.copyBytesFromToRing(pass, 0, 0xFFFF, byteBuffer, bytePos, byteMask, pass.length);
		return bytePos+pass.length;
	}



	public static int buildPublishPacket(int bytePos, byte[] byteBuffer, int byteMask, CharSequence topic,
			CharSequence payload) {
		// TODO Auto-generated method stub
		return 0;
	}


	public static int buildPublishPacket(int bytePos, byte[] byteBuffer, int byteMask, CharSequence topic,
			ByteBuffer payload) { 
		// TODO Auto-generated method stub
		return 0;
	}


	public static int buildPublishPacket(int bytePos, byte[] byteBuffer, int byteMask, CharSequence topic,
			byte[] payload, int payloadOffset, int payloadLength) {
		// TODO Auto-generated method stub
		return 0;
	}

  //TODO: C, could add method to use InputStream for the payload

}

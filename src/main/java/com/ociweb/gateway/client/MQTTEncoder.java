package com.ociweb.gateway.client;

import java.nio.ByteBuffer;

public class MQTTEncoder {

	static int buildConnectPacket(int bytePos, byte[] byteBuffer, int byteMask, int ttlSec, int conFlags, byte[] clientId, byte[] willTopic, byte[] willMessage, byte[] user, byte[] pass) {
		final int firstPos = bytePos;
		bytePos = appendFixedHeader(bytePos, byteMask, byteBuffer); //const and remaining length, 2  bytes
		
		//variable header
		bytePos = appendFixedProtoName(bytePos, byteMask, byteBuffer); //const 6 bytes
		bytePos = appendFixedProtoLevel(bytePos, byteMask, byteBuffer); //const 1 byte for version		
		bytePos = appendFixedConFlags(bytePos, byteMask, byteBuffer, conFlags); //8 bits or togehter, if clientId zero length must set clear
		bytePos = appendFixedKeepAlive(bytePos, byteMask, byteBuffer, ttlSec); //seconds < 16 bits
		
		//payload
		bytePos = appendClientId(bytePos, byteMask, byteBuffer, clientId);
		bytePos = appendWillTopicMsg(bytePos, byteMask, byteBuffer, willTopic, willMessage); //if will flag on
		bytePos = appendUser(bytePos, byteMask, byteBuffer, user);	//if user flag on		
		bytePos = appendPass(bytePos, byteMask, byteBuffer, pass); //if pass flag on
		int len = firstPos-bytePos;
		appendFixedHeaderLengthCount(bytePos, byteMask, byteBuffer, len);
		return len;
	}
	
	
	private static int appendFixedHeader(int bytePos, int byteMask, byte[] byteBuffer) {
		// TODO Auto-generated method stub
		return 0;
	}

	private static int appendFixedProtoName(int bytePos, int byteMask, byte[] byteBuffer) {
		// TODO Auto-generated method stub
		return 0;
	}

	private static int appendFixedProtoLevel(int bytePos, int byteMask, byte[] byteBuffer) {
		// TODO Auto-generated method stub
		return 0;
	}

	private static int appendFixedConFlags(int bytePos, int byteMask, byte[] byteBuffer, int conFlags) {
		// TODO Auto-generated method stub
		return 0;
	}

	private static int appendFixedKeepAlive(int bytePos, int byteMask, byte[] byteBuffer, int ttlSec) {
		// TODO Auto-generated method stub
		return 0;
	}

	private static int appendClientId(int bytePos, int byteMask, byte[] byteBuffer, byte[] clientId) {
		// TODO Auto-generated method stub
		return 0;
	}

	private static int appendWillTopicMsg(int bytePos, int byteMask, byte[] byteBuffer, byte[] willTopic, byte[] willMessage) {
		// TODO Auto-generated method stub
		return 0;
	}

	private static int appendUser(int bytePos, int byteMask, byte[] byteBuffer, byte[] user) {
		// TODO Auto-generated method stub
		return 0;
	}

	private static int appendPass(int bytePos, int byteMask, byte[] byteBuffer, byte[] pass) {
		// TODO Auto-generated method stub
		return 0;
	}

	private static void appendFixedHeaderLengthCount(int bytePos, int byteMask, byte[] byteBuffer, int i) {
		// TODO Auto-generated method stub
		
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

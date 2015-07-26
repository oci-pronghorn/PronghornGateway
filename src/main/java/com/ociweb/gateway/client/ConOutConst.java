package com.ociweb.gateway.client;

public class ConOutConst {

	public static final int MSG_CON_OUT_PUB_ACK=0;
	public static final int MSG_CON_OUT_PUB_REC=3;
	public static final int MSG_CON_OUT_MESSAGE=18;	
	public static final int MSG_CON_OUT_CONNACK_OK=6;
	public static final int MSG_CON_OUT_CONNACK_PROTO=8;
	public static final int MSG_CON_OUT_CONNACK_ID=10;
	public static final int MSG_CON_OUT_CONNACK_SERVER=12;
	public static final int MSG_CON_OUT_CONNACK_USER=14;
	public static final int MSG_CON_OUT_CONNACK_AUTH=16;
	public static final int MSG_CON_OUT_PUB_REL=24;
		
	public static final int CON_OUT_PUB_ACK_FIELD_PACKETID=1;	
	public static final int CON_OUT_PUB_REC_FIELD_PACKETID=1;
	public static final int CON_OUT_PUB_REL_FIELD_PACKETID=1;
	
	public static final int CON_OUT_MESSAGE_FIELD_QOS=1;
	public static final int CON_OUT_MESSAGE_FIELD_PACKETID=2;
	public static final int CON_OUT_MESSAGE_FIELD_TOPIC=67108867;
	public static final int CON_OUT_MESSAGE_FIELD_PAYLOAD=67108869;

	
}

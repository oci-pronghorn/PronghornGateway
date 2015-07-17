package com.ociweb.gateway.client;

import static com.ociweb.pronghorn.ring.FieldReferenceOffsetManager.lookupFieldLocator;
import static com.ociweb.pronghorn.ring.FieldReferenceOffsetManager.lookupTemplateLocator;

import com.ociweb.pronghorn.ring.FieldReferenceOffsetManager;

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
	
	//public static final int MSG_CON_OUT_CONN_ACK;//TODO: AA, must send back to let caller know the state.
	
	public static final int CON_OUT_PUB_ACK_FIELD_PACKETDATA;
	public static final int CON_OUT_PUB_REC_FIELD_PACKETDATA;	
	public static final int CON_OUT_MESSAGE_FIELD_QOS;
	public static final int CON_OUT_MESSAGE_FIELD_PACKETID;
	public static final int CON_OUT_MESSAGE_FIELD_TOPIC;
	public static final int CON_OUT_MESSAGE_FIELD_PAYLOAD;
	
  	static {
	  	
  		FieldReferenceOffsetManager fromOutCon = ClientFromFactory.connectionOutFROM;
  			  	
		CON_OUT_PUB_ACK_FIELD_PACKETDATA = lookupFieldLocator("PacketId", MSG_CON_OUT_PUB_ACK, fromOutCon);
	  	CON_OUT_PUB_REC_FIELD_PACKETDATA = lookupFieldLocator("PacketId", MSG_CON_OUT_PUB_REC, fromOutCon);
	  	CON_OUT_MESSAGE_FIELD_QOS      = lookupFieldLocator("QOS", MSG_CON_OUT_MESSAGE, fromOutCon);
	  	CON_OUT_MESSAGE_FIELD_PACKETID = lookupFieldLocator("PacketId", MSG_CON_OUT_MESSAGE, fromOutCon);
	  	CON_OUT_MESSAGE_FIELD_TOPIC    = lookupFieldLocator("Topic", MSG_CON_OUT_MESSAGE, fromOutCon);
	  	CON_OUT_MESSAGE_FIELD_PAYLOAD  = lookupFieldLocator("Payload", MSG_CON_OUT_MESSAGE, fromOutCon);
	  			
  	}  	
	
}

package com.ociweb.gateway.client;

import static com.ociweb.pronghorn.ring.FieldReferenceOffsetManager.lookupFieldLocator;
import static com.ociweb.pronghorn.ring.FieldReferenceOffsetManager.lookupTemplateLocator;

import com.ociweb.pronghorn.ring.FieldReferenceOffsetManager;

public class ConOutConst {

	public final int MSG_CON_OUT_PUB_ACK;
	public final int MSG_CON_OUT_PUB_REC;
	public final int MSG_CON_OUT_MESSAGE;
	
	public final int CON_OUT_PUB_ACK_FIELD_PACKETDATA;
	public final int CON_OUT_PUB_REC_FIELD_PACKETDATA;	
	public final int CON_OUT_MESSAGE_FIELD_QOS;
	public final int CON_OUT_MESSAGE_FIELD_PACKETID;
	public final int CON_OUT_MESSAGE_FIELD_TOPIC;
	public final int CON_OUT_MESSAGE_FIELD_PAYLOAD;
	
  	public ConOutConst(FieldReferenceOffsetManager fromOutCon) {
	  	
		MSG_CON_OUT_PUB_ACK = lookupTemplateLocator("PubAck",fromOutCon);  
		MSG_CON_OUT_PUB_REC = lookupTemplateLocator("PubRec",fromOutCon);  
		MSG_CON_OUT_MESSAGE = lookupTemplateLocator("Message",fromOutCon);
	  	
		CON_OUT_PUB_ACK_FIELD_PACKETDATA = lookupFieldLocator("PacketId", MSG_CON_OUT_PUB_ACK, fromOutCon);
	  	CON_OUT_PUB_REC_FIELD_PACKETDATA = lookupFieldLocator("PacketId", MSG_CON_OUT_PUB_REC, fromOutCon);
	  	CON_OUT_MESSAGE_FIELD_QOS      = lookupFieldLocator("QOS", MSG_CON_OUT_MESSAGE, fromOutCon);
	  	CON_OUT_MESSAGE_FIELD_PACKETID = lookupFieldLocator("PacketId", MSG_CON_OUT_MESSAGE, fromOutCon);
	  	CON_OUT_MESSAGE_FIELD_TOPIC    = lookupFieldLocator("Topic", MSG_CON_OUT_MESSAGE, fromOutCon);
	  	CON_OUT_MESSAGE_FIELD_PAYLOAD  = lookupFieldLocator("Payload", MSG_CON_OUT_MESSAGE, fromOutCon);
	  			
  	}  	
	
}

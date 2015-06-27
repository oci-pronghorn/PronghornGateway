package com.ociweb.gateway.client;

import static com.ociweb.pronghorn.ring.FieldReferenceOffsetManager.lookupFieldLocator;
import static com.ociweb.pronghorn.ring.FieldReferenceOffsetManager.lookupTemplateLocator;

import com.ociweb.pronghorn.ring.FieldReferenceOffsetManager;

public class ConInConst {

	
	public final int MSG_CON_IN_PUBLISH;
	public final int MSG_CON_IN_CONNECT;
	public final int MSG_CON_IN_DISCONNECT;
	public final int MSG_CON_IN_SUBSCRIBE;  
	public final int MSG_CON_IN_UNSUBSCRIBE;
	public final int MSG_CON_IN_PUB_ACK;
	public final int MSG_CON_IN_PUB_REC;
	public final int MSG_CON_IN_PUB_COMP;  
	
	public final int CON_IN_PUBLISH_FIELD_QOS;
	public final int CON_IN_PUBLISH_FIELD_PACKETID;
	public final int CON_IN_PUBLISH_FIELD_PACKETDATA;  	
	public final int CON_IN_CONNECT_FIELD_URL;
	public final int CON_IN_CONNECT_FIELD_PACKETDATA;		  	
	public final int CON_IN_SUBSCRIBE_FIELD_PACKETDATA;
	public final int CON_IN_UNSUBSCRIBE_FIELD_PACKETDATA;
	public final int CON_IN_PUB_ACK_FIELD_PACKETDATA;
	public final int CON_IN_PUB_REC_FIELD_PACKETDATA;
	public final int CON_IN_PUB_COMP_FIELD_PACKETDATA;
	
	public ConInConst(FieldReferenceOffsetManager fromToCon) {
		
		MSG_CON_IN_PUBLISH     = lookupTemplateLocator("Publish",fromToCon);  
		MSG_CON_IN_CONNECT     = lookupTemplateLocator("Connect",fromToCon);  
		MSG_CON_IN_DISCONNECT  = lookupTemplateLocator("Disconnect",fromToCon);  
		MSG_CON_IN_SUBSCRIBE   = lookupTemplateLocator("Subscribe",fromToCon);  
		MSG_CON_IN_UNSUBSCRIBE = lookupTemplateLocator("UnSubscribe",fromToCon);  
		MSG_CON_IN_PUB_ACK     = lookupTemplateLocator("PubAck",fromToCon);  
		MSG_CON_IN_PUB_REC     = lookupTemplateLocator("PubRec",fromToCon);  
		MSG_CON_IN_PUB_COMP    = lookupTemplateLocator("PubComp",fromToCon);  
		
	  	CON_IN_PUBLISH_FIELD_QOS        = lookupFieldLocator("QOS", MSG_CON_IN_PUBLISH, fromToCon);
	  	CON_IN_PUBLISH_FIELD_PACKETID   = lookupFieldLocator("PacketId", MSG_CON_IN_PUBLISH, fromToCon);
	  	CON_IN_PUBLISH_FIELD_PACKETDATA = lookupFieldLocator("PacketData", MSG_CON_IN_PUBLISH, fromToCon);		
		CON_IN_CONNECT_FIELD_URL        = lookupFieldLocator("URL", MSG_CON_IN_CONNECT, fromToCon);
	  	CON_IN_CONNECT_FIELD_PACKETDATA = lookupFieldLocator("PacketData", MSG_CON_IN_CONNECT, fromToCon);		
	  	CON_IN_SUBSCRIBE_FIELD_PACKETDATA = lookupFieldLocator("PacketData", MSG_CON_IN_SUBSCRIBE, fromToCon);	  	
	  	CON_IN_UNSUBSCRIBE_FIELD_PACKETDATA = lookupFieldLocator("PacketData", MSG_CON_IN_UNSUBSCRIBE, fromToCon);		
	  	CON_IN_PUB_ACK_FIELD_PACKETDATA = lookupFieldLocator("PacketId", MSG_CON_IN_PUB_ACK, fromToCon);
	  	CON_IN_PUB_REC_FIELD_PACKETDATA = lookupFieldLocator("PacketId", MSG_CON_IN_PUB_REC, fromToCon);
	  	CON_IN_PUB_COMP_FIELD_PACKETDATA = lookupFieldLocator("PacketId", MSG_CON_IN_PUB_COMP, fromToCon);
	  		  	
		
	}
	
	
	
}

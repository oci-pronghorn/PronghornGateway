package com.ociweb.gateway.client;

import static com.ociweb.pronghorn.ring.FieldReferenceOffsetManager.lookupFieldLocator;
import static com.ociweb.pronghorn.ring.FieldReferenceOffsetManager.lookupTemplateLocator;

import com.ociweb.pronghorn.ring.FieldReferenceOffsetManager;

public class ConInConst {
	
    //confirm in unit test that this constant matches
    //TODO: AAAA, new unit tests needed,  ConInConst.MSG_CON_IN_CONNECT:
	final static int MSG_CONNECT = 0;
	final static int MSG_DISCONNECT = 1;
	final static int MSG_PUBLISH = 2;
	
		
	public static final int MSG_CON_IN_PUBLISH;
	public static final int MSG_CON_IN_CONNECT;
	public static final int MSG_CON_IN_DISCONNECT;
	public static final int MSG_CON_IN_SUBSCRIBE;  
	public static final int MSG_CON_IN_UNSUBSCRIBE;
	public static final int MSG_CON_IN_PUB_ACK;
	public static final int MSG_CON_IN_PUB_REC;
	public static final int MSG_CON_IN_PUB_COMP;  
	
	
	public static final int CON_IN_PUBLISH_FIELD_QOS;
	public static final int CON_IN_PUBLISH_FIELD_PACKETID;
	public static final int CON_IN_PUBLISH_FIELD_PACKETDATA;  	
	public static final int CON_IN_CONNECT_FIELD_URL;
	public static final int CON_IN_CONNECT_FIELD_PACKETDATA;		  	
	public static final int CON_IN_SUBSCRIBE_FIELD_PACKETDATA;
	public static final int CON_IN_UNSUBSCRIBE_FIELD_PACKETDATA;
	public static final int CON_IN_PUB_ACK_FIELD_PACKETDATA;
	public static final int CON_IN_PUB_REC_FIELD_PACKETDATA;
	public static final int CON_IN_PUB_COMP_FIELD_PACKETDATA;
	
	static {
		
		FieldReferenceOffsetManager fromToCon = ClientFromFactory.connectionInFROM;
		
		//TODO: move this block to test to confirm the constants.
		MSG_CON_IN_PUBLISH     = lookupTemplateLocator("Publish",fromToCon);  
		MSG_CON_IN_CONNECT     = lookupTemplateLocator("Connect",fromToCon);  
		MSG_CON_IN_DISCONNECT  = lookupTemplateLocator("Disconnect",fromToCon);  //Should there be packet data? its only 2 bytes 0xE0, 0x00
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

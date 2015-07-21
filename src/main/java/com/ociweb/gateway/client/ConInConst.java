package com.ociweb.gateway.client;

import static com.ociweb.pronghorn.ring.FieldReferenceOffsetManager.lookupFieldLocator;
import static com.ociweb.pronghorn.ring.FieldReferenceOffsetManager.lookupTemplateLocator;

import com.ociweb.pronghorn.ring.FieldReferenceOffsetManager;

public class ConInConst {
	
		
	public static final int MSG_CON_IN_PUBLISH = 0;
	public static final int MSG_CON_IN_CONNECT = 5;
	public static final int MSG_CON_IN_DISCONNECT = 9;
	public static final int MSG_CON_IN_SUBSCRIBE = 11;  
	public static final int MSG_CON_IN_UNSUBSCRIBE = 14;
	public static final int MSG_CON_IN_PUB_ACK = 17;
	public static final int MSG_CON_IN_PUB_REC = 20;
	public static final int MSG_CON_IN_PUB_COMP = 23;  
	
	
	public static final int CON_IN_PUBLISH_FIELD_QOS; //TODO: B, for smaller code change these to constants as well
	public static final int CON_IN_PUBLISH_FIELD_PACKETID;
	public static final int CON_IN_PUBLISH_FIELD_PACKETDATA;  //TODO: B, break this up into different blocks so we can use pronouns	
	public static final int CON_IN_CONNECT_FIELD_URL;
	public static final int CON_IN_CONNECT_FIELD_PACKETDATA;		  	
	public static final int CON_IN_SUBSCRIBE_FIELD_PACKETDATA;
	public static final int CON_IN_UNSUBSCRIBE_FIELD_PACKETDATA;
	public static final int CON_IN_PUB_ACK_FIELD_PACKETDATA;
	public static final int CON_IN_PUB_REC_FIELD_PACKETDATA;
	public static final int CON_IN_PUB_COMP_FIELD_PACKETDATA;
	
	static {
		
		FieldReferenceOffsetManager fromToCon = ClientFromFactory.connectionInFROM;
		
		
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

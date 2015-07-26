package com.ociweb.gateway.client;

public class ConInConst {
			
	public static final int MSG_CON_IN_PUBLISH = 0;
	public static final int MSG_CON_IN_CONNECT = 5;
	public static final int MSG_CON_IN_DISCONNECT = 9;
	public static final int MSG_CON_IN_SUBSCRIBE = 15;  
	public static final int MSG_CON_IN_UNSUBSCRIBE = 18;
	public static final int MSG_CON_IN_PUB_ACK = 21;
	public static final int MSG_CON_IN_PUB_REC = 25;
	public static final int MSG_CON_IN_PUB_COMP = 29;  
	public static final int MSG_CON_IN_PUB_REL = 11; 
	
	
	public static final int CON_IN_PUBLISH_FIELD_QOS=1;
	public static final int CON_IN_PUBLISH_FIELD_PACKETID=2;
	public static final int CON_IN_PUBLISH_FIELD_PACKETDATA=117440515;
	
	public static final int CON_IN_CONNECT_FIELD_URL=67108865;
	public static final int CON_IN_CONNECT_FIELD_PACKETDATA=117440515;		  	
	
	public static final int CON_IN_SUBSCRIBE_FIELD_PACKETDATA=117440513;
	
	public static final int CON_IN_UNSUBSCRIBE_FIELD_PACKETDATA=117440513;
	
	public static final int CON_IN_PUB_ACK_FIELD_PACKETDATA=117440514;
	public static final int CON_IN_PUB_ACK_FIELD_PACKETID=1;
	
	public static final int CON_IN_PUB_REC_FIELD_PACKETDATA=117440514;
	public static final int CON_IN_PUB_REC_FIELD_PACKETID=1;
	
	public static final int CON_IN_PUB_COMP_FIELD_PACKETDATA=117440514;
	public static final int CON_IN_PUB_COMP_FIELD_PACKETID=1;
	
	public static final int CON_IN_PUB_REL_FIELD_PACKETDATA=117440514;
	public static final int CON_IN_PUB_REL_FIELD_PACKETID=1;

		
}

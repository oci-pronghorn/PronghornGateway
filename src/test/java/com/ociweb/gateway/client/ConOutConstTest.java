package com.ociweb.gateway.client;

import static com.ociweb.pronghorn.pipe.FieldReferenceOffsetManager.lookupFieldLocator;
import static com.ociweb.pronghorn.pipe.FieldReferenceOffsetManager.lookupTemplateLocator;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ConOutConstTest {

	private final int MSG_CON_OUT_PUB_ACK        = lookupTemplateLocator("PubAck",ClientFromFactory.connectionOutFROM);  
	private final int MSG_CON_OUT_PUB_REC        = lookupTemplateLocator("PubRec",ClientFromFactory.connectionOutFROM); 
	private final int MSG_CON_OUT_PUB_REL        = lookupTemplateLocator("PubRel",ClientFromFactory.connectionOutFROM); 
    
	private final int MSG_CON_OUT_MESSAGE        = lookupTemplateLocator("Message",ClientFromFactory.connectionOutFROM);
	
	private final int MSG_CON_OUT_CONNACK_OK     = lookupTemplateLocator("ConnAckOK",ClientFromFactory.connectionOutFROM);
	private final int MSG_CON_OUT_CONNACK_PROTO  = lookupTemplateLocator("ConnAckProto",ClientFromFactory.connectionOutFROM);
	private final int MSG_CON_OUT_CONNACK_ID     = lookupTemplateLocator("ConnAckId",ClientFromFactory.connectionOutFROM);
	private final int MSG_CON_OUT_CONNACK_SERVER = lookupTemplateLocator("ConnAckServer",ClientFromFactory.connectionOutFROM);
	private final int MSG_CON_OUT_CONNACK_USER   = lookupTemplateLocator("ConnAckUser",ClientFromFactory.connectionOutFROM);
	private final int MSG_CON_OUT_CONNACK_AUTH   = lookupTemplateLocator("ConnAckAuth",ClientFromFactory.connectionOutFROM);
		
	private final int CON_OUT_PUB_ACK_FIELD_PACKETID = lookupFieldLocator("PacketId", MSG_CON_OUT_PUB_ACK, ClientFromFactory.connectionOutFROM);       
	private final int CON_OUT_PUB_REC_FIELD_PACKETID = lookupFieldLocator("PacketId", MSG_CON_OUT_PUB_REC, ClientFromFactory.connectionOutFROM);       
	private final int CON_OUT_PUB_REL_FIELD_PACKETID = lookupFieldLocator("PacketId", MSG_CON_OUT_PUB_REL, ClientFromFactory.connectionOutFROM);
    
	private final int CON_OUT_MESSAGE_FIELD_QOS      = lookupFieldLocator("QOS", MSG_CON_OUT_MESSAGE, ClientFromFactory.connectionOutFROM);
	private final int CON_OUT_MESSAGE_FIELD_PACKETID = lookupFieldLocator("PacketId", MSG_CON_OUT_MESSAGE, ClientFromFactory.connectionOutFROM);
	private final int CON_OUT_MESSAGE_FIELD_TOPIC    = lookupFieldLocator("Topic", MSG_CON_OUT_MESSAGE, ClientFromFactory.connectionOutFROM);
	private final int CON_OUT_MESSAGE_FIELD_PAYLOAD  = lookupFieldLocator("Payload", MSG_CON_OUT_MESSAGE, ClientFromFactory.connectionOutFROM);
	
	
	@Test
	public void testExpectedMsgIds() {
		
		assertEquals(MSG_CON_OUT_PUB_ACK, ConOutConst.MSG_CON_OUT_PUB_ACK);
		assertEquals(MSG_CON_OUT_PUB_REC, ConOutConst.MSG_CON_OUT_PUB_REC);
		assertEquals(MSG_CON_OUT_PUB_REL, ConOutConst.MSG_CON_OUT_PUB_REL);
        		
		assertEquals(MSG_CON_OUT_MESSAGE, ConOutConst.MSG_CON_OUT_MESSAGE);
		
		assertEquals(MSG_CON_OUT_CONNACK_OK, ConOutConst.MSG_CON_OUT_CONNACK_OK);
		assertEquals(MSG_CON_OUT_CONNACK_PROTO, ConOutConst.MSG_CON_OUT_CONNACK_PROTO);
		assertEquals(MSG_CON_OUT_CONNACK_ID, ConOutConst.MSG_CON_OUT_CONNACK_ID);
		assertEquals(MSG_CON_OUT_CONNACK_SERVER, ConOutConst.MSG_CON_OUT_CONNACK_SERVER);
		assertEquals(MSG_CON_OUT_CONNACK_USER, ConOutConst.MSG_CON_OUT_CONNACK_USER);
		assertEquals(MSG_CON_OUT_CONNACK_AUTH, ConOutConst.MSG_CON_OUT_CONNACK_AUTH);
				
	}
	
	@Test
	public void testExpectedMsgFieldLocIdx() {
	    
	    assertEquals(CON_OUT_PUB_ACK_FIELD_PACKETID, ConOutConst.CON_OUT_PUB_ACK_FIELD_PACKETID);
	    assertEquals(CON_OUT_PUB_REC_FIELD_PACKETID, ConOutConst.CON_OUT_PUB_REC_FIELD_PACKETID);
	    assertEquals(CON_OUT_PUB_REL_FIELD_PACKETID, ConOutConst.CON_OUT_PUB_REL_FIELD_PACKETID);
	    
	    assertEquals(CON_OUT_MESSAGE_FIELD_QOS, ConOutConst.CON_OUT_MESSAGE_FIELD_QOS);
	    assertEquals(CON_OUT_MESSAGE_FIELD_PACKETID, ConOutConst.CON_OUT_MESSAGE_FIELD_PACKETID);
	    assertEquals(CON_OUT_MESSAGE_FIELD_TOPIC, ConOutConst.CON_OUT_MESSAGE_FIELD_TOPIC);
	    assertEquals(CON_OUT_MESSAGE_FIELD_PAYLOAD, ConOutConst.CON_OUT_MESSAGE_FIELD_PAYLOAD);
	    
	}
	
	
	
}

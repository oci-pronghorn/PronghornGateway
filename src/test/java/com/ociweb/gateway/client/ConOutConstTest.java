package com.ociweb.gateway.client;

import static com.ociweb.pronghorn.ring.FieldReferenceOffsetManager.lookupTemplateLocator;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ConOutConstTest {

	private final int MSG_CON_OUT_PUB_ACK        = lookupTemplateLocator("PubAck",ClientFromFactory.connectionOutFROM);  
	private final int MSG_CON_OUT_PUB_REC        = lookupTemplateLocator("PubRec",ClientFromFactory.connectionOutFROM);  
	private final int MSG_CON_OUT_MESSAGE        = lookupTemplateLocator("Message",ClientFromFactory.connectionOutFROM);
	
	private final int MSG_CON_OUT_CONNACK_OK     = lookupTemplateLocator("ConnAckOK",ClientFromFactory.connectionOutFROM);
	private final int MSG_CON_OUT_CONNACK_PROTO  = lookupTemplateLocator("ConnAckProto",ClientFromFactory.connectionOutFROM);
	private final int MSG_CON_OUT_CONNACK_ID     = lookupTemplateLocator("ConnAckId",ClientFromFactory.connectionOutFROM);
	private final int MSG_CON_OUT_CONNACK_SERVER = lookupTemplateLocator("ConnAckServer",ClientFromFactory.connectionOutFROM);
	private final int MSG_CON_OUT_CONNACK_USER   = lookupTemplateLocator("ConnAckUser",ClientFromFactory.connectionOutFROM);
	private final int MSG_CON_OUT_CONNACK_AUTH   = lookupTemplateLocator("ConnAckAuth",ClientFromFactory.connectionOutFROM);
	
	
	@Test
	public void testExpectedMsgIds() { //
		
		assertEquals(MSG_CON_OUT_PUB_ACK, ConOutConst.MSG_CON_OUT_PUB_ACK);
		assertEquals(MSG_CON_OUT_PUB_REC, ConOutConst.MSG_CON_OUT_PUB_REC);
		assertEquals(MSG_CON_OUT_MESSAGE, ConOutConst.MSG_CON_OUT_MESSAGE);
		
		assertEquals(MSG_CON_OUT_CONNACK_OK, ConOutConst.MSG_CON_OUT_CONNACK_OK);
		assertEquals(MSG_CON_OUT_CONNACK_PROTO, ConOutConst.MSG_CON_OUT_CONNACK_PROTO);
		assertEquals(MSG_CON_OUT_CONNACK_ID, ConOutConst.MSG_CON_OUT_CONNACK_ID);
		assertEquals(MSG_CON_OUT_CONNACK_SERVER, ConOutConst.MSG_CON_OUT_CONNACK_SERVER);
		assertEquals(MSG_CON_OUT_CONNACK_USER, ConOutConst.MSG_CON_OUT_CONNACK_USER);
		assertEquals(MSG_CON_OUT_CONNACK_AUTH, ConOutConst.MSG_CON_OUT_CONNACK_AUTH);
		
		
		
	}
	
	
	
}

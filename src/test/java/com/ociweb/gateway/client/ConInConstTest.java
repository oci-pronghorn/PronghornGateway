package com.ociweb.gateway.client;

import static com.ociweb.pronghorn.ring.FieldReferenceOffsetManager.lookupTemplateLocator;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ConInConstTest {

	private final int MSG_CON_IN_PUBLISH = lookupTemplateLocator("Publish",ClientFromFactory.connectionInFROM);  
	private final int MSG_CON_IN_CONNECT = lookupTemplateLocator("Connect",ClientFromFactory.connectionInFROM);  
	private final int MSG_CON_IN_DISCONNECT = lookupTemplateLocator("Disconnect",ClientFromFactory.connectionInFROM);  //Should there be packet data? its only 2 bytes 0xE0, 0x00
	private final int MSG_CON_IN_SUBSCRIBE = lookupTemplateLocator("Subscribe",ClientFromFactory.connectionInFROM);  
	private final int MSG_CON_IN_UNSUBSCRIBE = lookupTemplateLocator("UnSubscribe",ClientFromFactory.connectionInFROM);  
	private final int MSG_CON_IN_PUB_ACK = lookupTemplateLocator("PubAck",ClientFromFactory.connectionInFROM);  
	private final int MSG_CON_IN_PUB_REC = lookupTemplateLocator("PubRec",ClientFromFactory.connectionInFROM);  
	private final int MSG_CON_IN_PUB_COMP = lookupTemplateLocator("PubComp",ClientFromFactory.connectionInFROM);  
	
	@Test
	public void testExpectedMsgIds() {
	
		assertEquals(MSG_CON_IN_PUBLISH, ConInConst.MSG_CON_IN_PUBLISH);
		assertEquals(MSG_CON_IN_CONNECT, ConInConst.MSG_CON_IN_CONNECT);
		assertEquals(MSG_CON_IN_DISCONNECT, ConInConst.MSG_CON_IN_DISCONNECT);
		assertEquals(MSG_CON_IN_SUBSCRIBE, ConInConst.MSG_CON_IN_SUBSCRIBE);
		assertEquals(MSG_CON_IN_UNSUBSCRIBE, ConInConst.MSG_CON_IN_UNSUBSCRIBE);
		assertEquals(MSG_CON_IN_PUB_ACK, ConInConst.MSG_CON_IN_PUB_ACK);
		assertEquals(MSG_CON_IN_PUB_REC, ConInConst.MSG_CON_IN_PUB_REC);
		assertEquals(MSG_CON_IN_PUB_COMP, ConInConst.MSG_CON_IN_PUB_COMP);
	}
	
	
}

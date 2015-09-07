package com.ociweb.gateway.client;

import static com.ociweb.pronghorn.pipe.FieldReferenceOffsetManager.lookupFieldLocator;
import static com.ociweb.pronghorn.pipe.FieldReferenceOffsetManager.lookupTemplateLocator;
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
	private final int MSG_CON_IN_PUB_REL = lookupTemplateLocator("PubRel",ClientFromFactory.connectionInFROM); 
	
	private final int CON_IN_PUBLISH_FIELD_QOS        = lookupFieldLocator("QOS", MSG_CON_IN_PUBLISH, ClientFromFactory.connectionInFROM);
	private final int CON_IN_PUBLISH_FIELD_PACKETID   = lookupFieldLocator("PacketId", MSG_CON_IN_PUBLISH, ClientFromFactory.connectionInFROM);
	private final int CON_IN_PUBLISH_FIELD_PACKETDATA = lookupFieldLocator("PacketData", MSG_CON_IN_PUBLISH, ClientFromFactory.connectionInFROM);     
	private final int CON_IN_CONNECT_FIELD_URL        = lookupFieldLocator("URL", MSG_CON_IN_CONNECT, ClientFromFactory.connectionInFROM);
	private final int CON_IN_CONNECT_FIELD_PACKETDATA = lookupFieldLocator("PacketData", MSG_CON_IN_CONNECT, ClientFromFactory.connectionInFROM); 
	private final int CON_IN_SUBSCRIBE_FIELD_PACKETDATA = lookupFieldLocator("PacketData", MSG_CON_IN_SUBSCRIBE, ClientFromFactory.connectionInFROM); 
	private final int CON_IN_UNSUBSCRIBE_FIELD_PACKETDATA = lookupFieldLocator("PacketData", MSG_CON_IN_UNSUBSCRIBE, ClientFromFactory.connectionInFROM);
	private final int CON_IN_PUB_ACK_FIELD_PACKETDATA = lookupFieldLocator("PacketData", MSG_CON_IN_PUB_ACK, ClientFromFactory.connectionInFROM);
	private final int CON_IN_PUB_ACK_FIELD_PACKETID = lookupFieldLocator("PacketId", MSG_CON_IN_PUB_ACK, ClientFromFactory.connectionInFROM);
	private final int CON_IN_PUB_REC_FIELD_PACKETDATA = lookupFieldLocator("PacketData", MSG_CON_IN_PUB_REC, ClientFromFactory.connectionInFROM);
	private final int CON_IN_PUB_REC_FIELD_PACKETID = lookupFieldLocator("PacketId", MSG_CON_IN_PUB_REC, ClientFromFactory.connectionInFROM);
	private final int CON_IN_PUB_COMP_FIELD_PACKETDATA = lookupFieldLocator("PacketData", MSG_CON_IN_PUB_COMP, ClientFromFactory.connectionInFROM);
	private final int CON_IN_PUB_COMP_FIELD_PACKETID = lookupFieldLocator("PacketId", MSG_CON_IN_PUB_COMP, ClientFromFactory.connectionInFROM);
	private final int CON_IN_PUB_REL_FIELD_PACKETDATA = lookupFieldLocator("PacketData", MSG_CON_IN_PUB_REL, ClientFromFactory.connectionInFROM);
	private final int CON_IN_PUB_REL_FIELD_PACKETID = lookupFieldLocator("PacketId", MSG_CON_IN_PUB_REL, ClientFromFactory.connectionInFROM);
	
	
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
		assertEquals(MSG_CON_IN_PUB_REL, ConInConst.MSG_CON_IN_PUB_REL);
		
	}
	
   @Test
   public void testExpectedMsgFieldLocIds() {
       
       assertEquals(CON_IN_PUBLISH_FIELD_QOS, ConInConst.CON_IN_PUBLISH_FIELD_QOS);
       assertEquals(CON_IN_PUBLISH_FIELD_PACKETID, ConInConst.CON_IN_PUBLISH_FIELD_PACKETID);
       assertEquals(CON_IN_PUBLISH_FIELD_PACKETDATA, ConInConst.CON_IN_PUBLISH_FIELD_PACKETDATA);
       assertEquals(CON_IN_CONNECT_FIELD_URL, ConInConst.CON_IN_CONNECT_FIELD_URL);
       assertEquals(CON_IN_CONNECT_FIELD_PACKETDATA, ConInConst.CON_IN_CONNECT_FIELD_PACKETDATA);
       assertEquals(CON_IN_SUBSCRIBE_FIELD_PACKETDATA, ConInConst.CON_IN_SUBSCRIBE_FIELD_PACKETDATA);
       assertEquals(CON_IN_UNSUBSCRIBE_FIELD_PACKETDATA, ConInConst.CON_IN_UNSUBSCRIBE_FIELD_PACKETDATA);
       assertEquals(CON_IN_PUB_ACK_FIELD_PACKETDATA, ConInConst.CON_IN_PUB_ACK_FIELD_PACKETDATA);
       assertEquals(CON_IN_PUB_ACK_FIELD_PACKETID, ConInConst.CON_IN_PUB_ACK_FIELD_PACKETID);
       assertEquals(CON_IN_PUB_REC_FIELD_PACKETDATA, ConInConst.CON_IN_PUB_REC_FIELD_PACKETDATA);
       assertEquals(CON_IN_PUB_REC_FIELD_PACKETID, ConInConst.CON_IN_PUB_REC_FIELD_PACKETID);
       assertEquals(CON_IN_PUB_COMP_FIELD_PACKETDATA, ConInConst.CON_IN_PUB_COMP_FIELD_PACKETDATA);
       assertEquals(CON_IN_PUB_COMP_FIELD_PACKETID, ConInConst.CON_IN_PUB_COMP_FIELD_PACKETID);
       assertEquals(CON_IN_PUB_REL_FIELD_PACKETDATA, ConInConst.CON_IN_PUB_REL_FIELD_PACKETDATA);
       assertEquals(CON_IN_PUB_REL_FIELD_PACKETID, ConInConst.CON_IN_PUB_REL_FIELD_PACKETID);
             
       
   }
	
}

package com.ociweb.gateway.client;

import com.ociweb.pronghorn.ring.FieldReferenceOffsetManager;

public class ClientFromFactory {

	public final static FieldReferenceOffsetManager connectionInFROM = new FieldReferenceOffsetManager(
		    new int[]{0xc0400004,0x80000000,0x80000001,0xb8000000,0xc0200004,0xc0400003,0xa0000001,0xb8000000,0xc0200003,0xc0400001,0xc0200001,0xc0400002,0xb8000000,0xc0200002,0xc0400002,0xb8000000,0xc0200002,0xc0400002,0x80000001,0xc0200002,0xc0400002,0x80000001,0xc0200002,0xc0400002,0x80000001,0xc0200002},
		    (short)0,
		    new String[]{"Publish","QOS","PacketId","PacketData",null,"Connect","URL","PacketData",null,"Disconnect",null,"Subscribe","PacketData",null,"UnSubscribe","PacketData",null,"PubAck","PacketId",null,"PubRec","PacketId",null,"PubComp","PacketId",null},
		    new long[]{1, 100, 200, 300, 0, 2, 400, 300, 0, 5, 0, 3, 300, 0, 4, 300, 0, 6, 200, 0, 7, 200, 0, 8, 200, 0},
		    new String[]{"global",null,null,null,null,"global",null,null,null,"global",null,"global",null,null,"global",null,null,"global",null,null,"global",null,null,"global",null,null},
		    "connectionIn.xml");
	
	public final static FieldReferenceOffsetManager connectionOutFROM = new FieldReferenceOffsetManager(
		    new int[]{0xc0400002,0x80000000,0xc0200002,0xc0400002,0x80000000,0xc0200002,0xc0400005,0x80000001,0x80000000,0xa0000000,0xa0000001,0xc0200005},
		    (short)0,
		    new String[]{"PubAck","PacketId",null,"PubRec","PacketId",null,"Message","QOS","PacketId","Topic","Payload",null},
		    new long[]{6, 200, 0, 7, 200, 0, 9, 100, 200, 400, 500, 0},
		    new String[]{"global",null,null,"global",null,null,"global",null,null,null,null,null},
		    "connectionOut.xml");
	
	
}
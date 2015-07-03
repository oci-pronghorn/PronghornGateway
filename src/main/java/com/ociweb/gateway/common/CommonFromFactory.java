package com.ociweb.gateway.common;

import com.ociweb.pronghorn.ring.FieldReferenceOffsetManager;

public class CommonFromFactory {

	//NOTE: this example class will be generated in the future, use this example as the template
	
	public final static FieldReferenceOffsetManager idRangesFROM = new FieldReferenceOffsetManager(
		    new int[]{0xc0400002,0x80000000,0xc0200002},
		    (short)0,
		    new String[]{"IdRange","Range",null},
		    new long[]{1, 100, 0},
		    new String[]{"global",null,null},
		    "idRanges.xml");
	
	public final static FieldReferenceOffsetManager timeControlFROM = new FieldReferenceOffsetManager(
		    new int[]{0xc0400001,0xc0200001,0xc0400001,0xc0200001,0xc0400001,0xc0200001},
		    (short)0,
		    new String[]{"Start",null,"Stop",null,"Reset",null},
		    new long[]{1, 0, 2, 0, 3, 0},
		    new String[]{"global",null,"global",null,"global",null},
		    "timeControl.xml");
	
	public final static FieldReferenceOffsetManager timeTriggerFROM = new FieldReferenceOffsetManager(
		    new int[]{0xc0400001,0xc0200001},
		    (short)0,
		    new String[]{"Trigger",null},
		    new long[]{1, 0},
		    new String[]{"global",null},
		    "timeTrigger.xml");

	public static FieldReferenceOffsetManager monitorFROM = new FieldReferenceOffsetManager(
		    new int[]{0xc1400006,0x90800000,0x90800001,0x90800002,0x80000000,0x80200001,0xc1200006,0xc1400008,0x90800000,0x90800001,0x90800002,0x80000000,0x80200001,0x80800002,0x80200003,0xc1200008},
		    (short)0,
		    new String[]{"RingStatSample","MS","Head","Tail","TemplateId","BufferSize",null,"RingStatEnhancedSample","MS","Head","Tail","TemplateId","BufferSize","StackDepth","Latency",null},
		    new long[]{1, 1, 2, 3, 4, 5, 0, 2, 1, 2, 3, 4, 5, 21, 22, 0},
		    new String[]{"global",null,null,null,null,null,null,"global",null,null,null,null,null,null,null,null},
		    "ringMonitor.xml");

}

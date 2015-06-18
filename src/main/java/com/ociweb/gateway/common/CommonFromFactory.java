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

}

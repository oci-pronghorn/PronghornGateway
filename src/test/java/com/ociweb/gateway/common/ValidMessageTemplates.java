package com.ociweb.gateway.common;

import org.junit.Test;

import com.ociweb.pronghorn.ring.FieldReferenceOffsetManager;

public class ValidMessageTemplates {

	@Test
	public void validateIdRangesTemplate() {
		
		String templateFile = "/com/ociweb/gateway/common/idRanges.xml";
		String varName = "idRangesFROM";				
		FieldReferenceOffsetManager encodedFrom = CommonFromFactory.idRangesFROM;
		
		TestUtil.testForMatchingFROMs(templateFile, varName, encodedFrom);
				
	}

	@Test
	public void validateTimeControlTemplate() {
		String templateFile = "/com/ociweb/gateway/common/timeControl.xml";
		String varName = "timeControlFROM";				
		FieldReferenceOffsetManager encodedFrom = CommonFromFactory.timeControlFROM;
		
		TestUtil.testForMatchingFROMs(templateFile, varName, encodedFrom);
	}
	
	@Test
	public void validateTimeTriggerTemplate() {
		String templateFile = "/com/ociweb/gateway/common/timeTrigger.xml";
		String varName = "timeTriggerFROM";				
		FieldReferenceOffsetManager encodedFrom = CommonFromFactory.timeTriggerFROM;
		
		TestUtil.testForMatchingFROMs(templateFile, varName, encodedFrom);
	}

}

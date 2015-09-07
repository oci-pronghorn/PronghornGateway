package com.ociweb.gateway.common;

import org.junit.Test;

import com.ociweb.pronghorn.pipe.FieldReferenceOffsetManager;
import com.ociweb.pronghorn.stage.monitor.MonitorFROM;

public class ValidMessageTemplates {

	@Test
	public void validateIdRangesTemplate() {
		
		String templateFile = "/com/ociweb/gateway/common/idRanges.xml";
		String varName = "idRangesFROM";				
		FieldReferenceOffsetManager encodedFrom = CommonFromFactory.idRangesFROM;
		
		TestUtil.testForMatchingFROMs(templateFile, varName, encodedFrom);
				
	}
	
	@Test
	public void validateMonitorTemplate() {
		String templateFile = "/ringMonitor.xml";
		String varName = "monitorFROM";				
		FieldReferenceOffsetManager encodedFrom = CommonFromFactory.monitorFROM;
		
		TestUtil.testForMatchingFROMs(templateFile, varName, encodedFrom);
					
	}
	
	
}

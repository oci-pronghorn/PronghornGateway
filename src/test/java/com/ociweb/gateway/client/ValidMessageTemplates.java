package com.ociweb.gateway.client;

import org.junit.Test;

import com.ociweb.gateway.client.ClientFromFactory;
import com.ociweb.gateway.common.TestUtil;
import com.ociweb.pronghorn.ring.FieldReferenceOffsetManager;

public class ValidMessageTemplates {

	
	@Test
	public void validateConnectionIn() {
		
		String templateFile = "/com/ociweb/gateway/client/connectionIn.xml";
		String varName = "connectionInFROM";				
		FieldReferenceOffsetManager encodedFrom = ClientFromFactory.connectionInFROM;
		
		TestUtil.testForMatchingFROMs(templateFile, varName, encodedFrom);
				
	}
	
	@Test
	public void validateConnectionOut() {
		
		String templateFile = "/com/ociweb/gateway/client/connectionOut.xml";
		String varName = "connectionOutFROM";				
		FieldReferenceOffsetManager encodedFrom = ClientFromFactory.connectionOutFROM;
		
		TestUtil.testForMatchingFROMs(templateFile, varName, encodedFrom);
				
	}
	
}

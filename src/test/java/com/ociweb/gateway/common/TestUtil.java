package com.ociweb.gateway.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.ociweb.pronghorn.ring.FieldReferenceOffsetManager;
import com.ociweb.pronghorn.ring.schema.loader.TemplateHandler;

public class TestUtil {

	public static void testForMatchingFROMs(String templateFile, String varName, FieldReferenceOffsetManager encodedFrom) {
		try {
			FieldReferenceOffsetManager expectedFrom = TemplateHandler.loadFrom(templateFile);
			assertNotNull(expectedFrom);				
			
			if (!expectedFrom.equals(encodedFrom)) {
				System.err.println("Encoded source:"+expectedFrom);
				System.err.println("Template file:"+encodedFrom);	
				
				assertEquals("Scripts same length",expectedFrom.tokens.length,expectedFrom.fieldNameScript.length);
				if (null!=encodedFrom) {
					assertEquals("FieldNameScript",expectedFrom.fieldNameScript.length,encodedFrom.fieldNameScript.length);
					assertEquals("FieldIdScript",expectedFrom.fieldIdScript.length,encodedFrom.fieldIdScript.length);
					assertEquals("DictionaryNameScript",expectedFrom.dictionaryNameScript.length,encodedFrom.dictionaryNameScript.length);
				}
				
				StringBuilder target = new StringBuilder();
				TemplateHandler.buildFROMConstructionSource(target, expectedFrom, varName, templateFile.substring(1+templateFile.lastIndexOf('/') ));												
				System.err.println(target);
				
				fail(varName+" template does not match template file "+templateFile);
			}			
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}

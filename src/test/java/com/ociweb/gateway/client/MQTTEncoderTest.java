package com.ociweb.gateway.client;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class MQTTEncoderTest {

	@Test
	public void testEncodeVarLength() {
		//this is a very simple generative test because it just uses sequential numbers
		
		byte[] targetSpace = new byte[4];//MQTT 3.1.1 section 2.2.3, this is only supported for up to 4 bytes
		int i = 0;
		
		while (i<(1<<25)) {
			//NOTE: this test only covers the generated values not their position wrapping an array.
			Arrays.fill(targetSpace, (byte)0);
			
			MQTTEncoder.encodeVarLength(targetSpace, 0, 0xFF, i);
			
			validateMatch(targetSpace, 0, i);
			
			
			i++;
		}
		
		System.out.println("tested up to 0x"+Integer.toHexString(i));
		
	}

	//This method taken from MQTT 3.1.1 section 2.2.3
	//	Non normative comment
	//	281
	//	282 The algorithm for encoding a non negative integer (X) into the variable length encoding scheme is as follows:
	//	283	do
	//	284    encodedByte = X MOD 128
	//	285    X = X DIV 128
	//	286 // if there are more data to encode, set the top bit of this byte
	//	287 if ( X > 0 )
	//	288	    encodedByte = encodedByte OR 128
	//	289	endif
	//	290      'output' encodedByte
	//	291	while ( X > 0 )
	//	292
	//	293
	//	294	Where MOD is the modulo operator (% in C), DIV is integer division (/ in C), and OR is bit-wise or	(| in C).
	//
	private void validateMatch(byte[] targetSpace, int idx, int x) {
		do {
			int encodedByte = x % 128;
			x = x / 128;
			if (x>0) {
				encodedByte |= 128;
			}
			if ((0xFF&targetSpace[idx++]) != encodedByte) {
				Assert.fail("Expected "+encodedByte+" but found "+targetSpace[idx-1]+" at "+(idx-1)+" for value "+x);
			}
		} while (x>0);
	}
	
	
}

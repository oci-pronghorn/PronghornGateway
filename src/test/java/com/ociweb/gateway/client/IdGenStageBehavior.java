package com.ociweb.gateway.client;

import static com.ociweb.pronghorn.ring.RingBuffer.addMsgIdx;
import static com.ociweb.pronghorn.ring.RingBuffer.contentToLowLevelRead;
import static com.ociweb.pronghorn.ring.RingBuffer.publishWrites;
import static com.ociweb.pronghorn.ring.RingBuffer.roomToLowLevelWrite;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.gateway.common.GGSGenerator;
import com.ociweb.gateway.common.GVSValidator;
import com.ociweb.gateway.common.IdGenStage;
import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class IdGenStageBehavior{

	private static final Logger log = LoggerFactory.getLogger(IdGenStageBehavior.class);
	
	private static void debug(String template, int range) {
		//Method prevents garbage creation when debug mode is not enabled
		if (log.isDebugEnabled()) {
			log.debug(template,IdGenStage.rangeToString(range));
		}
	}
	
	public static GGSGenerator generator(final long testDuration) {
		return new GGSGenerator() {
			
			private final int length = 1<<16;
			private final int mask = length-1;
			
			private final int[] values = new int[length]; //maximum of 65536 id values
			private long head = 0;
			private long tail = 0;
			private long stopTime = System.currentTimeMillis()+testDuration;
			
			@Override
			public boolean generate(GraphManager graphManager, RingBuffer[] inputs, RingBuffer[] outputs, Random random) {
				final int theOneMessage = 0;
				int sizeOfFragment = RingBuffer.from(inputs[0]).fragDataSize[theOneMessage];
				
				
				//TODO: drop some as they are sent
				
				assert(1==outputs.length) : "IdGen can only support a single queue of release ranges";
				
				
				RingBuffer outputRing = outputs[0];
				while ( tail<head && roomToLowLevelWrite(outputRing, sizeOfFragment)) {
				    
					addMsgIdx(outputRing, theOneMessage);	
					
					//assemble a single run
					long t = tail;
					{
						int last =  -1;
						int value = values[mask&(int)tail];
						int limit = Integer.MAX_VALUE;//IdGenStage.MAX_BLOCK_SIZE; TODO: use random  number here to mix up the return blocks
						do {
							last = value;
							value = values[mask&(int)++t];
						} while (1+last==value && t<head && --limit>=0);

					}
					//publish tail to t exclusive	
					int value = values[mask&(int)tail];
					long run = t-tail;
					int range = (0xFFFF & value) | ( (0xFFFF & (int)(value+run))<<16);
			
					debug("Generator produce range {}",range);
										
					tail = t;
					
					RingBuffer.addIntValue(range, outputRing);
					
					publishWrites(outputRing);
					RingBuffer.confirmLowLevelWrite(outputRing, sizeOfFragment);
				}
				
				
				
				//get new ranges
				int i = inputs.length;
				while (--i>=0) {
					RingBuffer inputRing = inputs[i];
					while (contentToLowLevelRead(inputRing, sizeOfFragment) && ((tail+65534)-head)>IdGenStage.MAX_BLOCK_SIZE ) {
						int msgIdx = RingBuffer.takeMsgIdx(inputRing);
						assert(theOneMessage == msgIdx);
						
						int range = RingBuffer.takeValue(inputRing);
						
						debug("Generator consume range {}",range);
										
						
						int idx = range & 0xFFFF;
						
						int limit = (range >> 16) & 0xFFFF;
						
						int count = limit-idx;
						if (count>IdGenStage.MAX_BLOCK_SIZE) {
							System.err.println("TOO many in one call "+count); //TODO: move test to validation
							return false;
						}
												
						while(idx<limit) {//room is guaranteed by head tail check in while definition
							
							
							values[mask & (int)head++] = idx++;
						}						
					///	System.err.println("set value "+(0xFFFF&(head-1))+" "+(head-1)+" value "+(idx-1));
						
						RingBuffer.readBytesAndreleaseReadLock(inputRing);
						RingBuffer.confirmLowLevelRead(inputRing, sizeOfFragment);
					}
				}
				
				//TODO: mix up the ranges 
				
								
	
				
				return System.currentTimeMillis()<stopTime;
				
			}
		};
	}

	public static GVSValidator validator() {
			return new GVSValidator() {
	
				final byte[] testSpace = new byte[1 << 16];// 0 to 65535
				final int theOneMessage = 0;
				long total=0;
				final String[] label = new String[]{"Tested","Generator"};
				
				@Override
				public boolean validate(GraphManager graphManager, RingBuffer[] inputs, RingBuffer[] outputs) {
					int sizeOfFragment = RingBuffer.from(inputs[0]).fragDataSize[theOneMessage];
					assert(inputs.length == outputs.length);
					assert(2 == outputs.length);
	
					byte j = 2; // 0 we expect to find 0 and change to 1, 1 we
								// expect to find 1 and change to zero.
					
					//TODO: remove loop
					while (--j >= 0) {
	
						
						final byte expected = j;
						final byte toggle = (byte) ((expected + 1) & 1);
						
						boolean contentToLowLevelRead = contentToLowLevelRead(inputs[expected], sizeOfFragment);
						boolean roomToLowLevelWrite = roomToLowLevelWrite(outputs[toggle], sizeOfFragment);
											
						if (contentToLowLevelRead && roomToLowLevelWrite) {
	
							int msgIdx = RingBuffer.takeMsgIdx(inputs[expected]);
							if (theOneMessage != msgIdx) {
								System.err.println(label[expected]+" bad message found "+msgIdx);
								return false;
							};
	
							int range = RingBuffer.takeValue(inputs[expected]);
	
							
							
							int idx = range & 0xFFFF; // TODO: helper method to get
												      // the shorts would be nice
													  // here
							int limit = (range >> 16) & 0xFFFF;
							
							debug("Validation "+label[expected]+" to "+label[toggle]+" range {} ",range);
							
							
							int count = limit-idx;
							if (count<1) {
								System.err.println(label[expected]+" message must contain at least 1 value in the range, found "+count+" in value "+Integer.toHexString(range));
								return false;
							}
												
							total += (long)count;
							// test for the expected values
							int failureIdx = -1;
							boolean exit = false;
							while (idx < limit) {
								if (expected != testSpace[idx]) {
									if (failureIdx < 0) {
										failureIdx = idx;
										exit = true;
									}								
								} else {
									if (failureIdx>=0) {								
										System.err.println("Validator found "+label[expected]+" toggle error at "+failureIdx+" up to "+idx+" expected "+expected+" released up to "+limit);
										failureIdx = -1;
									}								
								}
								
								testSpace[idx++] = toggle;
							}
							if (failureIdx>=0) {								
								System.err.println("Validator found "+label[expected]+" toggle error at "+failureIdx+" up to "+limit+" expected "+expected+" released up to "+limit);
							    exit = true;
							}
							if (exit) {
								return false;
							}
							
	
							addMsgIdx(outputs[toggle], msgIdx);
							RingBuffer.addIntValue(range, outputs[toggle]);
							publishWrites(outputs[toggle]);
							RingBuffer.confirmLowLevelWrite(outputs[toggle], sizeOfFragment);
	
							RingBuffer.readBytesAndreleaseReadLock(inputs[expected]);
							RingBuffer.confirmLowLevelRead(inputs[expected], sizeOfFragment);
						}
					}
					return true;
				}
	
				@Override
				public String status() {
					return "Total Values Checked:"+Long.toString(total>>1);
				}
			};
		}

}

package com.ociweb.gateway.client;

import static com.ociweb.pronghorn.ring.RingBuffer.addMsgIdx;
import static com.ociweb.pronghorn.ring.RingBuffer.contentToLowLevelRead;
import static com.ociweb.pronghorn.ring.RingBuffer.publishWrites;
import static com.ociweb.pronghorn.ring.RingBuffer.roomToLowLevelWrite;

import java.util.Random;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.gateway.common.GGSGenerator;
import com.ociweb.gateway.common.GVSValidator;
import com.ociweb.gateway.common.IdGenStage;
import com.ociweb.gateway.common.TestFailureDetails;
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
				int maxReturnBlockSize = 1+random.nextInt(2*IdGenStage.MAX_BLOCK_SIZE);// 1 up to 2x the block size used for reserving ranges
				

				//mix up the ranges to simulate late returning values in fllight
				int maxMix = 0;//70; //max in flight
				int oneFailureIn = 10000; //NOTE: as this ratio gets worse the speed drops off
				//NOTE: this does not test any "lost" ids, TODO: C, this would be a good test to add later.
				
				long mixStart = Math.max(tail, head-maxMix);
				long k = head-mixStart;
				//do the shuffle.
				while (--k>=1) {
					if (0==random.nextInt(oneFailureIn)) {
						//mix forward to values can move a long way
						int temp = values[mask&(int)(head-(k-1))];
						values[mask&(int)(head-(k-1))] = values[mask&(int)(head-k)];
						values[mask&(int)(head-k)] = temp;
					}					
				}
									
				assert(1==outputs.length) : "IdGen can only support a single queue of release ranges";
				
				
				RingBuffer outputRing = outputs[0];
				while ( tail<head && roomToLowLevelWrite(outputRing, sizeOfFragment)) {
				    
					addMsgIdx(outputRing, theOneMessage);	
					
					//assemble a single run
					long t = tail;
					{
						int last =  -1;
						int value = values[mask&(int)tail];
						int limit = maxReturnBlockSize;
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
							System.err.println("TOO many in one call "+count); //TODO: AAA,  move test to validation
							return false;
						}
												
						while(idx<limit) {//room is guaranteed by head tail check in while definition														
							values[mask & (int)head++] = idx++;
						}						
						
						RingBuffer.releaseReads(inputRing);
						RingBuffer.confirmLowLevelRead(inputRing, sizeOfFragment);
					}
				}
	
				
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
				public TestFailureDetails validate(GraphManager graphManager, RingBuffer[] inputs, RingBuffer[] outputs) {
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
								return new TestFailureDetails("Corrupt Feed");
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
								
								return new TestFailureDetails("Missing Data");
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
								System.err.println("Validator found "+label[expected]+" toggle error at "+failureIdx+" up to "+limit+" expected "+expected);
								
							    exit = true;
							}
							if (exit) {
								
								//Thinking:
								///   if validate from Tested does not match expected state then...
								//      Tested sent bad range or internal map is wrong
								//      Internal map can not be wrong because it matches the data passed thru from the generator and previously checked
								// SO... the new range from either tested or generated is always suspect for the error.
								
								//TODO: TestFailureDetails must take the needed seeds to repo the problem, this will not work however until deterministic scheduler is complete.
								return new TestFailureDetails("Bad Match");
								
							}
							
	
							addMsgIdx(outputs[toggle], msgIdx);
							RingBuffer.addIntValue(range, outputs[toggle]);
							publishWrites(outputs[toggle]);
							RingBuffer.confirmLowLevelWrite(outputs[toggle], sizeOfFragment);
	
							RingBuffer.releaseReads(inputs[expected]);
							RingBuffer.confirmLowLevelRead(inputs[expected], sizeOfFragment);
						}
					}
					return null;
				}
	
				@Override
				public String status() {
					return "Total Values Checked:"+Long.toString(total>>1);
				}
			};
		}

}
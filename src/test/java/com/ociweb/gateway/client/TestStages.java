package com.ociweb.gateway.client;

import static com.ociweb.pronghorn.ring.RingBuffer.addMsgIdx;
import static com.ociweb.pronghorn.ring.RingBuffer.contentToLowLevelRead;
import static com.ociweb.pronghorn.ring.RingBuffer.publishWrites;
import static com.ociweb.pronghorn.ring.RingBuffer.roomToLowLevelWrite;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.gateway.common.GGSGenerator;
import com.ociweb.gateway.common.GVSValidator;
import com.ociweb.gateway.common.GeneralGeneratorStage;
import com.ociweb.gateway.common.GeneralValidationStage;
import com.ociweb.gateway.common.IdGenStage;
import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.ring.RingBufferConfig;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.monitor.MonitorConsoleStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;
import com.ociweb.pronghorn.stage.scheduling.StageScheduler;
import com.ociweb.pronghorn.stage.scheduling.ThreadPerStageScheduler;

public class TestStages {
	
	private static final Logger log = LoggerFactory.getLogger(TestStages.class);
	
	
	/*
	 * Testing notes:
	 * 
	 * All the tests for this project have been dynamically generated so more can be tested the longer they run.
	 * There are 3 major tests types in use.
	 * 
	 * 1. Fuzz test
	 *    Generator builds random messages with random content.
	 *    Ensure all outgoing messages are valid as defined by schema.
	 *    No Business checks
	 *    More invalid business messages are sent than valid
	 *    Ensure no messages cause crash or hang
	 *    Crash is defined as an unexpected exception.
	 *    Hang is defined as a blocking call to run that does not return
	 *    
	 *2. Expected use test
	 *   Generator builds random valid business messages
	 *   Ensure un-expected business messages do not stop processing with chrash or hang.
	 *   More valid bussiness message are sent than invalid
	 *   Ensure output messages follow business expectations
	 * 
	 *3. Identical behavior test
	 *   When refactoring its helpful to have a baseline implementation to ensure no behavior has changed.
	 *   Test data is produced from the same Fuzz and Expected generators above except messages are given to both new and old implementations.
	 *   Both implementations must produce the exact same results. 
	 * 
	 */
	
	
	@Ignore
	public void testStages() throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException {
		GraphManager gm = new GraphManager();		
		ClientAPIFactory.clientAPI(gm);
		
		//we do not know which id will be given to which stage so walk them all and do the right test for each
		int stageId = PronghornStage.totalStages();
		while (--stageId>=0) {			
			PronghornStage stage = GraphManager.getStage(gm, stageId);
			if (null!=stage) {
				//filter out any stages dedicated to monitoring
				if (null ==	GraphManager.getAnnotation(gm, stage, GraphManager.MONITOR, null)) {
					//build test instance, must be different instance than one found in the graph.
					
					//TODO: filter out the producers here to do different tests
					
					
					int inputs = GraphManager.getInputPipeCount(gm, stage);
					//need array of RingBufferConfig objects.
					RingBufferConfig[] inputConfigs = new RingBufferConfig[inputs];
					int i = inputs;
					while (--i>=0) {
						inputConfigs[i]=GraphManager.getInputPipe(gm, stage, i).config();
					}
					
					int outputs = GraphManager.getOutputPipeCount(gm, stage.stageId);
					RingBufferConfig[] outputConfigs = new RingBufferConfig[outputs];
					i = outputs;
					while (--i>=0) {
						outputConfigs[i]=GraphManager.getOutputPipe(gm, stage, i).config();
					}
									
					testSingleStage(stage.getClass(),inputConfigs,outputConfigs);
									
				}
			}
		}
		
	}
	
	
	//TODO: each test must start at known clean state an not go far from there to ensure repro-script is very short.
	
	@SuppressWarnings("unused")
	private void testSingleStage(Class targetStage, RingBufferConfig[] inputConfigs, RingBufferConfig[] outputConfigs) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException {
		//NOTE: it will be a while before this is all sorted out 
		//TODO: As a short term solution testing will be data driven with scripts and expect results
		//      This will teach us what we need to know to build the generative examples.
		
		//Generative producers are an interesting corner case, how do know what to expect.
		
		//Need generator for each input 
		//     May define in XML template file and pick up
		//     May code specific generator for each template?
		//Need validator for each input
		//     This is not the existing identical check validator 
		//     Validation must know the rules and apply them from  XML or hard coded.
		
		//

		//TODO: this conditional will be removed once we have general solutions for all the stages.
		if (IdGenStage.class == targetStage) {
			
			final long testDuration = 10000;
			
			log.info("begin testing {}",targetStage);
			
			GraphManager gm = new GraphManager();
			
			//to randomize from seed
			int generatorSeed = 42;
			int schedulerSeed = 123;
			Random random = new Random(generatorSeed);
			
			//NOTE: Use RingBufferConfig queue size so we only test for the case that is built and deployed.
			
			RingBuffer testedToValidate = new RingBuffer(outputConfigs[0]);
			RingBuffer validateToGenerate = new RingBuffer(outputConfigs[0]);
			
			RingBuffer validateToTested = new RingBuffer(inputConfigs[0]);			
			RingBuffer generateToValidate = new RingBuffer(inputConfigs[0]);
			
			//TODO: once complete determine how we will do this with multiple queues.
			Constructor constructor = targetStage.getConstructor(gm.getClass(), validateToTested.getClass(), testedToValidate.getClass());

			//all target test stages are market as producer for the duration of this test run
			GraphManager.addAnnotation(gm, GraphManager.PRODUCER, GraphManager.PRODUCER, (PronghornStage)constructor.newInstance(gm, validateToTested, testedToValidate));
									
			//TODO: how is one of these defined and generated? Can this be built by proxy?
			
			//validation shuts down when the producers on both end have already shut down.
			new GeneralValidationStage(gm, new RingBuffer[]{testedToValidate, generateToValidate},
					                       new RingBuffer[]{validateToTested, validateToGenerate},
					                       genIdValidator());

			//generator is always a producer and must be marked as such.
			GraphManager.addAnnotation(gm, GraphManager.PRODUCER, GraphManager.PRODUCER,
					                  new GeneralGeneratorStage(gm,
					                      new RingBuffer[]{validateToGenerate},
                    					  new RingBuffer[]{generateToValidate},
                    					  random, genIdGenerator(testDuration)));
			
			 MonitorConsoleStage.attach(gm); 
			 
			//TODO: create new single threaded deterministic scheduler.
			StageScheduler scheduler = new ThreadPerStageScheduler(gm);
			
			scheduler.startup();
			final long shutdownWindow = 1000;//No shutdown should every take longer than this.
			scheduler.awaitTermination(testDuration+shutdownWindow, TimeUnit.MILLISECONDS);
			
//			System.err.println(testedToValidate);
//			System.err.println(validateToTested);
//			System.err.println(validateToGenerate);
//			System.err.println(generateToValidate);
			
			
			
		}
		
		
		
	}


	private GGSGenerator genIdGenerator(final long testDuration) {
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
						int last = -1;
						int value = 0;
						int limit = Integer.MAX_VALUE;//IdGenStage.MAX_BLOCK_SIZE; TODO: use random  number here to mix up the return blocks
						while (--limit>=0 &&  t<head && ( (1+last == (value = values[mask&(int)t++])) | (-1==last) ) ) {
							last = value;						
						}
					}
					//publish tail to t exclusive	
					int value = values[mask&(int)tail];					
					int range = (0xFFFF & value) | ( (0xFFFF & (int)(value+t-tail))<<16);
			
					log.info("Generator produce range "+IdGenStage.rangeToString(range));
										
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
						
						log.info("Generator consume range "+IdGenStage.rangeToString(range));
										
						
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
						
						RingBuffer.readBytesAndreleaseReadLock(inputRing);
						RingBuffer.confirmLowLevelRead(inputRing, sizeOfFragment);
					}
				}
				
				//TODO: mix up the ranges 
				
								

				
				return System.currentTimeMillis()<stopTime;
				
			}
		};
	}


	private GVSValidator genIdValidator() {
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

						log.info("Validation {} to {} range {}",label[expected],label[toggle],IdGenStage.rangeToString(range));
						
						
						int idx = range & 0xFFFF; // TODO: helper method to get
											      // the shorts would be nice
												  // here
						int limit = (range >> 16) & 0xFFFF;
						
						int count = limit-idx;
						if (count<1) {
							System.err.println(label[expected]+" message must contain at least 1 value in the range, found "+count+" in value "+Integer.toHexString(range));
							return false;
						}
											
						

						total += (long)count;
						// test for the expected values
						while (idx < limit) {
							if (expected != testSpace[idx]) {
								System.err.println("Validator found "+label[expected]+" toggle error at "+idx+" expected "+expected);
								return false;
							}
							
							testSpace[idx++] = toggle;
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

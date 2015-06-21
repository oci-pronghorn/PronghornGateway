package com.ociweb.gateway.client;

import static com.ociweb.pronghorn.ring.RingBuffer.addMsgIdx;
import static com.ociweb.pronghorn.ring.RingBuffer.contentToLowLevelRead;
import static com.ociweb.pronghorn.ring.RingBuffer.publishWrites;
import static com.ociweb.pronghorn.ring.RingBuffer.roomToLowLevelWrite;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

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
	
	
	@Test
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
	private void testSingleStage(Class targetStage, RingBufferConfig[] inputs, RingBufferConfig[] outputs) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException {
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
			
			System.err.println(targetStage.getSimpleName()+" "+inputs.length+" "+outputs.length);
			
			GraphManager gm = new GraphManager();
			
			//to randomize from seed
			int generatorSeed = 42;
			int schedulerSeed = 123;
			Random random = new Random(generatorSeed);
			
			//NOTE: Use RingBufferConfig queue size so we only test for the case that is built and deployed.
			
			RingBuffer testedToValidate = new RingBuffer(outputs[0]);
			RingBuffer validateToTested = new RingBuffer(inputs[0]);			
			
			RingBuffer validateToGenerate = new RingBuffer(outputs[0]);
			RingBuffer generateToValidate = new RingBuffer(inputs[0]);
			
			//TODO: once complete determine how we will do this with multiple queues.
			Constructor constructor = targetStage.getConstructor(gm.getClass(), validateToTested.getClass(), testedToValidate.getClass());

			PronghornStage toTest = (PronghornStage)constructor.newInstance(gm, validateToTested, testedToValidate);
			
			//TODO: how do we know that this is a producer? Must get from graph? without producer flag it will not shut down.
			GraphManager.addAnnotation(gm, GraphManager.PRODUCER, GraphManager.PRODUCER, toTest);
						
			
			//TODO: how is one of these defined and generated? Can this be built by proxy?
			
			//validation shuts down when the producers on both end have already shut down.
			new GeneralValidationStage(gm, new RingBuffer[]{testedToValidate, generateToValidate},
					                       new RingBuffer[]{validateToTested, validateToGenerate},
					                       genIdValidator());
			
			//generator is always a producer and must be marked as such.
			GraphManager.addAnnotation(gm, GraphManager.PRODUCER, GraphManager.PRODUCER,new GeneralGeneratorStage(gm, new RingBuffer[]{validateToGenerate},
                    					  new RingBuffer[]{generateToValidate},
                    					  random, genIdGenerator()));
			
			 MonitorConsoleStage.attach(gm); 
			 
			//TODO: create new single threaded deterministic scheduler.
			StageScheduler scheduler = new ThreadPerStageScheduler(gm);
			
			scheduler.startup();

			Thread.sleep(10000);
			
			scheduler.shutdown();
			
			scheduler.awaitTermination(3000, TimeUnit.MILLISECONDS);
			
			System.err.println(testedToValidate);
			System.err.println(validateToTested);
			System.err.println(validateToGenerate);
			System.err.println(generateToValidate);
			
			
			
		}
		
		
		
	}


	private GGSGenerator genIdGenerator() {
		return new GGSGenerator() {
			
			private final int length = 1<<16;
			private final int mask = length-1;
			
			private final int[] values = new int[length]; //maximum of 65536 id values
			private long head = 0;
			private long tail = 0;
			
			@Override
			public boolean generate(GraphManager graphManager, RingBuffer[] inputs, RingBuffer[] outputs, Random random) {
				final int theOneMessage = 0;
				int sizeOfFragment = RingBuffer.from(inputs[0]).fragDataSize[theOneMessage];
				
				
				
				
				//TODO: drop some as they are sent
				assert(1==outputs.length) : "IdGen can only support a single queue of release ranges";
				
				
				RingBuffer outputRing = outputs[0];
				while (roomToLowLevelWrite(outputRing, sizeOfFragment) ) {
				    
					addMsgIdx(outputRing, theOneMessage);	
					
					//assemble a single run
					int last = -1;
					int value = 0;
					long t = tail;
					while (t<head & ((-1==last) || (last+1==value) ) ) {
						value = values[mask&(int)t++];
						last = value;
					}
					
					//publish tail to t exclusive					
					int range = (0xFFFF & values[mask&(int)tail]) | (0xFFFF & (values[mask&(int)t]>>16) );
					tail = t;
					RingBuffer.addIntValue(range, outputRing);
					
					publishWrites(outputRing);	
					RingBuffer.confirmLowLevelWrite(outputRing, sizeOfFragment);
				}
				
				
				
				//gather all the released ranges
				int i = inputs.length;
				while (--i>=0) {
					RingBuffer inputRing = inputs[i];
					while (contentToLowLevelRead(inputRing, sizeOfFragment)) {
						int msgIdx = RingBuffer.takeMsgIdx(inputRing);
						assert(theOneMessage == msgIdx);
						
						int value = RingBuffer.takeValue(inputRing);
						int idx = value & 0xFFFF;
						int limit = (value >> 16) & 0xFFFF;
						
						while(idx<limit) {
							values[mask & (int)head++] = idx++;
							//TODO: confirm that we are not over flowing
						}						
						
						RingBuffer.readBytesAndreleaseReadLock(inputRing);
						RingBuffer.confirmLowLevelRead(inputRing, sizeOfFragment);
					}
				}
				
				//TODO: mix up the ranges 
				
								

				
				return true;
				
			}
		};
	}


	private GVSValidator genIdValidator() {
		return new GVSValidator() {

			final byte[] testSpace = new byte[1 << 16];// 0 to 65535
			final int theOneMessage = 0;
			long total=0;

			@Override
			public boolean validate(GraphManager graphManager, RingBuffer[] inputs, RingBuffer[] outputs) {
				int sizeOfFragment = RingBuffer.from(inputs[0]).fragDataSize[theOneMessage];
				assert(inputs.length == outputs.length);
				assert(2 == outputs.length);

				byte i = 2; // 0 we expect to find 0 and change to 1, 1 we
							// expect to find 1 and change to zero.
				while (--i >= 0) {

					if (contentToLowLevelRead(inputs[i], sizeOfFragment)
							&& roomToLowLevelWrite(outputs[i], sizeOfFragment)) {

						int msgIdx = RingBuffer.takeMsgIdx(inputs[i]);
						assert(theOneMessage == msgIdx);
						addMsgIdx(outputs[i], msgIdx);

						int value = RingBuffer.takeValue(inputs[i]);

						int idx = value & 0xFFFF; // TODO: helper method to get
													// the shorts would be nice
													// here
						int limit = (value >> 16) & 0xFFFF;

						final byte expected = i;
						final byte toggle = (byte) ((expected + 1) & 1);

						total += (long)(limit-idx);
						// test for the expected values
						while (idx < limit) {
							if (expected != testSpace[idx]) {
							//	System.err.println(i+" found error XXX at "+idx+" expected "+expected);
								
								// TODO: log this as an error
								return false;
							}
//							else {
//								if (idx<100) {
//									System.err.println(i+" ok "+idx+" set "+toggle);
//								}
//							}
							
							
							testSpace[idx++] = toggle;
						}

						RingBuffer.addIntValue(value, outputs[i]);
						publishWrites(outputs[i]);
						RingBuffer.confirmLowLevelWrite(outputs[i], sizeOfFragment);

						RingBuffer.readBytesAndreleaseReadLock(inputs[i]);
						RingBuffer.confirmLowLevelRead(inputs[i], sizeOfFragment);
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

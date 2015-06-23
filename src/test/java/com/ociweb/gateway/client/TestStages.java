package com.ociweb.gateway.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.gateway.common.FuzzGeneratorStage;
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
	private static final long SHUTDOWN_WINDOW = 500;//No shutdown should every take longer than this.
	
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
		long testDuration = 1000; //keep short for now to save limited time on build server

		//TODO: this conditional will be removed once we have general solutions for all the stages.
		if (IdGenStage.class == targetStage) {
			
			GVSValidator validator = IdGenStageBehavior.validator();
			GGSGenerator generator = IdGenStageBehavior.generator(testDuration);

			//to randomize from seed
			int generatorSeed = 42;
			Random random = new Random(generatorSeed);
						
			runExpectedUseTest(targetStage, inputConfigs, outputConfigs, testDuration, validator, generator, random);			
			runFuzzTest(targetStage, inputConfigs, outputConfigs, testDuration, random);
		}
		
	}


	private void runFuzzTest(Class targetStage, RingBufferConfig[] inputConfigs, RingBufferConfig[] outputConfigs, long testDuration, Random random) {
		log.info("begin 'fuzz' testing {}",targetStage);
		
		GraphManager gm = new GraphManager();
		
		RingBuffer[] inputRings = buildRings(inputConfigs);	
		RingBuffer[] outputRings = buildRings(outputConfigs);	
		
		int i = outputRings.length;
		while (--i>=0) {
			new FuzzGeneratorStage(gm, random, testDuration, inputRings[i]);
		}
		
		//TODO: test must check for hang
		//TODO: test much check for throw
		//TODO: test much check output rings for valid messages if any
		
		
		
	}


	/**
	 * General method for running each "expected use" tests
	 * 
	 * Specific details of each tested stage must be passed in.
	 * 
	 */
	private void runExpectedUseTest(Class targetStage, RingBufferConfig[] inputConfigs,
			RingBufferConfig[] outputConfigs, final long testDuration, GVSValidator validator, GGSGenerator generator,
			Random random) throws NoSuchMethodException, InstantiationException, IllegalAccessException,
					InvocationTargetException {
		
		log.info("begin 'expected use' testing {}",targetStage);
		
		GraphManager gm = new GraphManager();
		
		//NOTE: Uses RingBufferConfig queue size so we only test for the case that is built and deployed.		
		RingBuffer[] testedToValidate = buildRings(outputConfigs);
		RingBuffer[] validateToGenerate = buildRings(outputConfigs);		
		RingBuffer[] validateToTested = buildRings(inputConfigs);			
		RingBuffer[] generateToValidate = buildRings(inputConfigs);
		
		RingBuffer[] validationInputs = joinArrays(testedToValidate, generateToValidate);
		RingBuffer[] validationOutputs = joinArrays(validateToTested, validateToGenerate);
		
		RingBuffer[] generatorInputs = validateToGenerate;
		RingBuffer[] generatorOutputs = generateToValidate;
		
		//TODO: once complete determine how we will do this with multiple queues.
		Constructor constructor = targetStage.getConstructor(gm.getClass(), validateToTested.getClass(), testedToValidate.getClass());

		//all target test stages are market as producer for the duration of this test run
		GraphManager.addAnnotation(gm, GraphManager.PRODUCER, GraphManager.PRODUCER, (PronghornStage)constructor.newInstance(gm, validateToTested, testedToValidate));
								
		//validation shuts down when the producers on both end have already shut down.
		GeneralValidationStage valdiationStage = new GeneralValidationStage(gm, validationInputs, validationOutputs, validator);

		//generator is always a producer and must be marked as such.			
		GraphManager.addAnnotation(gm, GraphManager.PRODUCER, GraphManager.PRODUCER,
				                  new GeneralGeneratorStage(gm, generatorInputs, generatorOutputs, random, generator));
		
		if (log.isDebugEnabled()) {
			MonitorConsoleStage.attach(gm);
		}
		 
		//TODO: create new single threaded deterministic scheduler.
		StageScheduler scheduler = new ThreadPerStageScheduler(gm);
		
		scheduler.startup();		
		
		if (!scheduler.awaitTermination(testDuration+SHUTDOWN_WINDOW, TimeUnit.MILLISECONDS) || valdiationStage.foundError()) {
			for (RingBuffer ring: testedToValidate) {
				log.info("{}->Valdate {}",targetStage,ring);
			}
			for (RingBuffer ring: validateToTested) {
				log.info("Validate->{} {}",targetStage,ring);
			}
			for (RingBuffer ring: validateToGenerate) {
				log.info("Validate->Generate {}",targetStage,ring);
			}
			for (RingBuffer ring: generateToValidate) {
				log.info("Generate->Validate {}",targetStage,ring);
			}			
		}
	}


	private RingBuffer[] buildRings(RingBufferConfig[] configs) {
		int i = configs.length;
		RingBuffer[] result = new RingBuffer[i];
		while (--i>=0) {
			result[i] = new RingBuffer(configs[i]);
		}		
		return result;
	}
	
	private RingBuffer[] joinArrays(RingBuffer[] a, RingBuffer[] b) {
		int len = a.length+b.length;
		RingBuffer[] result = new RingBuffer[len];
		int i = b.length;
		while (--i>=0) {
			result[--len] = b[i];
		}
		i = a.length;
		while (--i>=0) {
			result[--len] = a[i];
		}	
		return result;
	}
	
	
}

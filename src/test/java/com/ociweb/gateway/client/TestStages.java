package com.ociweb.gateway.client;

import org.junit.Test;

import com.ociweb.pronghorn.ring.RingBufferConfig;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class TestStages {

	
	
	@Test
	public void testStages() {
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
	
	
	private void testSingleStage(Class targetStage, RingBufferConfig[] inputs, RingBufferConfig[] outputs) {
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
		
		System.err.println(targetStage.getSimpleName()+" "+inputs.length+" "+outputs.length);
		//
		
		
		
	}
	
	
}

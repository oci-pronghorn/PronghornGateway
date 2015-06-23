package com.ociweb.gateway.client;

import static org.junit.Assert.*;

import org.junit.Test;

import com.ociweb.gateway.common.IdGenStage;
import com.ociweb.gateway.common.TimeKeeperStage;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class ValidGraphConstruction {

	
	@Test
	public void graphTest() {
		//This test may not be needed in the future with the graph if generated directly from the DOT file.
		
		GraphManager gm = new GraphManager();		
		ClientAPIFactory.clientAPI(gm);
		
		assertEquals("API and IdGen should be the only producers",2, gm.countStagesWithAnnotationKey(gm, GraphManager.PRODUCER));
		
		//we do not know which id will be given to which stage so walk them all and do the right test for each
		int stageId = PronghornStage.totalStages();
		while (--stageId>=0) {			
			PronghornStage stage = GraphManager.getStage(gm, stageId);
			//filter out any stages dedicated to monitoring
			if (null ==	GraphManager.getAnnotation(gm, stage, GraphManager.MONITOR, null)) {
				if (stage instanceof ConnectionStage) {
					
					assertEquals("from API to Connection ",APIStage.class, GraphManager.getRingProducer(gm, GraphManager.getInputPipe(gm, stage, 1).ringId).getClass());
					assertEquals("from TimeKeeper to Connection ",TimeKeeperStage.class, GraphManager.getRingProducer(gm, GraphManager.getInputPipe(gm, stage, 2).ringId).getClass());
					
					assertEquals("from Connection to API ",APIStage.class, GraphManager.getRingConsumer(gm, GraphManager.getOutputPipe(gm, stage, 1).ringId).getClass());
					assertEquals("from Connection to TimeKeeper ",TimeKeeperStage.class, GraphManager.getRingConsumer(gm, GraphManager.getOutputPipe(gm, stage, 2).ringId).getClass());
					assertEquals("from Connection to IdGen ",IdGenStage.class, GraphManager.getRingConsumer(gm, GraphManager.getOutputPipe(gm, stage, 3).ringId).getClass());
															
				} else if (stage instanceof TimeKeeperStage) {
					
					assertEquals("from TimeKeeper to Connection ",ConnectionStage.class, GraphManager.getRingConsumer(gm, GraphManager.getOutputPipe(gm, stage, 1).ringId).getClass());
					assertEquals("from Connection to TimeKeeper ",ConnectionStage.class, GraphManager.getRingProducer(gm, GraphManager.getInputPipe(gm, stage, 1).ringId).getClass());
					
				} else if (stage instanceof APIStage) {
					
					assertEquals("from IdGen to API ",IdGenStage.class, GraphManager.getRingProducer(gm, GraphManager.getInputPipe(gm, stage, 1).ringId).getClass());
					assertEquals("from Connection to API ",ConnectionStage.class, GraphManager.getRingProducer(gm, GraphManager.getInputPipe(gm, stage, 2).ringId).getClass());
					assertEquals("from API to Connection ",ConnectionStage.class, GraphManager.getRingConsumer(gm, GraphManager.getOutputPipe(gm, stage, 1).ringId).getClass());
					
				} else if (stage instanceof IdGenStage) {
					
					assertEquals("from Connection to IdGen ",ConnectionStage.class, GraphManager.getRingProducer(gm, GraphManager.getInputPipe(gm, stage, 1).ringId).getClass());
					assertEquals("from IdGen to API ",APIStage.class, GraphManager.getRingConsumer(gm, GraphManager.getOutputPipe(gm, stage, 1).ringId).getClass());
					
				} else {
					fail("Unknown stage :"+stage.getClass().getSimpleName());
				}
				
			}
			
			
			
		}
		
		
	}
	
}
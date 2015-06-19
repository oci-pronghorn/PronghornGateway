package com.ociweb.gateway.client;

import com.ociweb.gateway.common.CommonFromFactory;
import com.ociweb.gateway.common.IdGenStage;
import com.ociweb.gateway.common.TimeKeeperStage;
import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.ring.RingBufferConfig;
import com.ociweb.pronghorn.stage.monitor.MonitorConsoleStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;
import com.ociweb.pronghorn.stage.scheduling.StageScheduler;
import com.ociweb.pronghorn.stage.scheduling.ThreadPerStageScheduler;

public class ClientAPIFactory {

	public static APIStage clientAPI() {
		return clientAPI(new GraphManager());
	}
	
	public static APIStage clientAPI(GraphManager gm) {
		int queuedIds = 5;
		int queuedTimeControl = 2;
		int queuedTimeTrigger = 2;
		int queuedConIn = 5;
		int queuedConOut = 5;
		int maxTopicOrPayload = 256;
		
		APIStage result = buildInstance(gm, 
				            queuedIds, queuedTimeControl, queuedTimeTrigger, queuedConIn,
				            queuedConOut, maxTopicOrPayload);
		
		//TODO: B, must add way to pass in different schedulers and shut them down.
		StageScheduler scheduler = new ThreadPerStageScheduler(gm);
		scheduler.startup();
		scheduler.shutdown();
		return result;
	}

	//NOTE: this will be generated from the DOT file in future projects, this is here as an example.
	private static APIStage buildInstance(GraphManager gm, int queuedIds, int queuedTimeControl, int queuedTimeTrigger, int queuedConIn, int queuedConOut, int maxTopicOrPayload) {
		RingBufferConfig idGenConfig = new RingBufferConfig(CommonFromFactory.idRangesFROM, queuedIds, 0);
		RingBufferConfig timeControlConfig = new RingBufferConfig(CommonFromFactory.timeControlFROM, queuedTimeControl, 0);
		RingBufferConfig timeTriggerConfig = new RingBufferConfig(CommonFromFactory.timeTriggerFROM, queuedTimeTrigger, 0);			
		RingBufferConfig connectionInConfig = new RingBufferConfig(ClientFromFactory.connectionInFROM, queuedConIn, maxTopicOrPayload);
		RingBufferConfig connectionOutConfig = new RingBufferConfig(ClientFromFactory.connectionOutFROM, queuedConOut, maxTopicOrPayload);
						
		RingBuffer unusedIds = new RingBuffer(idGenConfig);
		RingBuffer releasedIds = new RingBuffer(idGenConfig);
		RingBuffer timeControl = new RingBuffer(timeControlConfig);
		RingBuffer timeTrigger = new RingBuffer(timeTriggerConfig);
		RingBuffer connectionIn = new RingBuffer(connectionInConfig);
		RingBuffer connectionOut = new RingBuffer(connectionOutConfig);
				

		//these instances are all held by the graph which is passed in	
		GraphManager.addAnnotation(gm, GraphManager.PRODUCER, GraphManager.PRODUCER, new IdGenStage(gm, releasedIds, unusedIds));
		APIStage apiStage = new APIStage(gm, unusedIds, connectionOut, connectionIn);
		GraphManager.addAnnotation(gm, GraphManager.PRODUCER, GraphManager.PRODUCER, apiStage);
		
		new TimeKeeperStage(gm, timeControl, timeTrigger);
		new ConnectionStage(gm, connectionIn, timeTrigger, connectionOut, timeControl, releasedIds);
		
		//TODO: B, replace with JMX version before release.
	    MonitorConsoleStage.attach(gm);
		return apiStage;
	}

	
	
}

package com.ociweb.gateway.client;

import com.ociweb.gateway.common.CommonFromFactory;
import com.ociweb.gateway.common.IdGenStage;
import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.ring.RingBufferConfig;
import com.ociweb.pronghorn.stage.monitor.MonitorConsoleStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class ClientAPIFactory {

	public static APIStage clientAPI(APIStageFactory factory) {
		return clientAPI(factory, new GraphManager());
	}
	
	public static APIStage clientAPI(APIStageFactory factory, GraphManager gm) {
		int queuedIds = 3;
		int queuedTimeControl = 2;
		int queuedTimeTrigger = 2;
		int queuedConIn = 2;
		int queuedConOut = 2;
		int maxTopicOrPayload = 16;
		
		return buildInstance(factory, gm, 
				            queuedIds, queuedTimeControl, queuedTimeTrigger, queuedConIn,
				            queuedConOut, maxTopicOrPayload);
	}

	//NOTE: this will be generated from the DOT file in future projects, this is here as an example.
	private static APIStage buildInstance(APIStageFactory factory, GraphManager gm, int queuedIds, int queuedTimeControl, int queuedTimeTrigger, int queuedConIn, int queuedConOut, int maxTopicOrPayload) {
		RingBufferConfig idGenConfig = new RingBufferConfig(CommonFromFactory.idRangesFROM, queuedIds, 0);		
		RingBufferConfig connectionInConfig = new RingBufferConfig(ClientFromFactory.connectionInFROM, queuedConIn, maxTopicOrPayload);
		RingBufferConfig connectionOutConfig = new RingBufferConfig(ClientFromFactory.connectionOutFROM, queuedConOut, maxTopicOrPayload);
						
		RingBuffer unusedIds = new RingBuffer(idGenConfig);
		RingBuffer releasedIds = new RingBuffer(idGenConfig);
		RingBuffer connectionIn = new RingBuffer(connectionInConfig);
		RingBuffer connectionOut = new RingBuffer(connectionOutConfig);
				

		//these instances are all held by the graph which is passed in	
		GraphManager.addAnnotation(gm, GraphManager.PRODUCER, GraphManager.PRODUCER, new IdGenStage(gm, releasedIds, unusedIds));
		
		APIStage apiStage = factory.newInstance(gm, unusedIds, connectionOut, connectionIn);
		
		GraphManager.addAnnotation(gm, GraphManager.PRODUCER, GraphManager.PRODUCER, apiStage);
		
		new ConnectionStage(gm, connectionIn, connectionOut, releasedIds);
		
		//TODO: B, replace with JMX version before release.
		
		
		//enable monitoring if we have 64mb of memory, //TODO: AAA, do not turn on takes all the memory.
		if (Runtime.getRuntime().freeMemory()>(64<<20)) {
			Integer defaultMonitorRate = Integer.valueOf(50000000);
			RingBufferConfig defaultMonitorRingConfig = new RingBufferConfig(CommonFromFactory.monitorFROM, 5, 0);
		    MonitorConsoleStage.attach(gm,defaultMonitorRate,defaultMonitorRingConfig); 
		}
		return apiStage;
	}

	
	
}

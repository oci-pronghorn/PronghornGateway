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
		int queuedConIn = 4; //TODO: move to factory.
		int queuedConOut = 4;
		
		return buildInstance(factory, gm, 
				            queuedIds, queuedConIn, queuedConOut);
	}

	//NOTE: this will be generated from the DOT file in future projects, this is here as an example.
	private static APIStage buildInstance(APIStageFactory factory, GraphManager gm, int queuedIds, int queuedConIn, int queuedConOut) {
		
	    String rate = factory.getRate();
	    RingBufferConfig idGenConfig = new RingBufferConfig(CommonFromFactory.idRangesFROM, queuedIds, 0);		
		RingBufferConfig connectionInConfig = new RingBufferConfig(ClientFromFactory.connectionInFROM, queuedConIn, factory.getMaxTopicOrPayload());
		RingBufferConfig connectionOutConfig = new RingBufferConfig(ClientFromFactory.connectionOutFROM, queuedConOut, factory.getMaxTopicOrPayload());
						
		RingBuffer unusedIds = new RingBuffer(idGenConfig);
		RingBuffer releasedIds = new RingBuffer(idGenConfig);
		RingBuffer connectionIn = new RingBuffer(connectionInConfig);
		RingBuffer connectionOut = new RingBuffer(connectionOutConfig);
				

		//these instances are all held by the graph which is passed in	
		GraphManager.addAnnotation(gm, GraphManager.PRODUCER, GraphManager.PRODUCER, new IdGenStage(gm, releasedIds, unusedIds, rate));
		APIStage apiStage = factory.newInstance(gm, unusedIds, connectionOut, connectionIn);
		GraphManager.addAnnotation(gm, GraphManager.PRODUCER, GraphManager.PRODUCER, apiStage);
		new ConnectionStage(gm, connectionIn, connectionOut, releasedIds, rate.length()>2 ? rate.substring(0, rate.length()-2) : "0", 
		                    factory.getInFlightLimit(), factory.getTTLSec(), factory.isSecured(), factory.getPort());
		
		
		//enable monitoring if we have 64mb of memory, //TODO: AAA, do not turn on takes all the memory.
		if (factory.isDebug() && Runtime.getRuntime().freeMemory()>(64<<20)) {
		    
		    //TODO: add telnet server support so we can control and monitor the server remotely
		    //      once complete build http server to serve angularJS pages for a better experiance.
		    
		    //TODO: B, replace with JMX version before release.
			Integer defaultMonitorRate = Integer.valueOf(50000000);
			RingBufferConfig defaultMonitorRingConfig = new RingBufferConfig(CommonFromFactory.monitorFROM, 5, 0);
		    MonitorConsoleStage.attach(gm,defaultMonitorRate,defaultMonitorRingConfig); 
		}
		return apiStage;
	}

	
	
}

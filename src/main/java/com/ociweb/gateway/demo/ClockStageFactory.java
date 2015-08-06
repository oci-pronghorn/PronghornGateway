package com.ociweb.gateway.demo;

import com.ociweb.gateway.client.APIStage;
import com.ociweb.gateway.client.APIStageFactory;
import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class ClockStageFactory extends APIStageFactory {

	public static ClockStageFactory instance = new ClockStageFactory(); //DemoStageFactory.instance
	
	private ClockStageFactory(){
		
	}
	
	@Override
	public APIStage newInstance(GraphManager gm, RingBuffer unusedIds, RingBuffer connectionOut, RingBuffer connectionIn) {
		return new ClockStage(gm,unusedIds,connectionOut,connectionIn);
	}

}

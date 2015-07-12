package com.ociweb.gateway.client;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class DemoStageFactory extends APIStageFactory {

	public static DemoStageFactory instance = new DemoStageFactory(); //DemoStageFactory.instance
	
	private DemoStageFactory(){
		
	}
	
	@Override
	public APIStage newInstance(GraphManager gm, RingBuffer unusedIds, RingBuffer connectionOut, RingBuffer connectionIn) {
		return new DemoStage(gm,unusedIds,connectionOut,connectionIn);
	}

}

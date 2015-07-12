package com.ociweb.gateway.client;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class DemoStage extends APIStage {

	public DemoStage(GraphManager graphManager, RingBuffer idGenIn, RingBuffer fromC, RingBuffer toC) {
		super(graphManager, idGenIn, fromC, toC);
	}

}

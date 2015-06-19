package com.ociweb.gateway.client;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class APIStage extends PronghornStage {

	protected APIStage(GraphManager graphManager, RingBuffer idGenIn, RingBuffer conIn, RingBuffer conOut) {
		super(graphManager, new RingBuffer[]{idGenIn,conIn}, conOut);
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

}

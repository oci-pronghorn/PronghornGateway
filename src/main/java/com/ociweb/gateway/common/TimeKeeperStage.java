package com.ociweb.gateway.common;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class TimeKeeperStage extends PronghornStage {

	public TimeKeeperStage(GraphManager graphManager, RingBuffer input, RingBuffer output) {
		super(graphManager, input, output);
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		//Must be scheduled frequently enough to do its job.

	}

}

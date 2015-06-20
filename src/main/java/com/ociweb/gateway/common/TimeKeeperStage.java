package com.ociweb.gateway.common;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class TimeKeeperStage extends PronghornStage {

	private boolean clockIsRunning;
	private long lastTimeInMS;
	private long timeout;

	private final RingBuffer input;
	private final RingBuffer output;
	
	public TimeKeeperStage(GraphManager graphManager, RingBuffer input, RingBuffer output) {
		super(graphManager, input, output);
		this.input = input;
		this.output = output;
		timeout = 1000;//TODO: where does this number come from?
	}

	@Override
	public void startup() {
	}

	@Override
	public void shutdown() {
	}
	
	//Must be scheduled frequently enough to do its job.
	@Override
	public void run() {
		//write with low level
		//read 3 types with low level because they have no fields and are all the same length
		
		
		long now = System.currentTimeMillis();
		
		if (clockIsRunning) {
			long timeSinceLastCheck = now-lastTimeInMS;
			if (timeSinceLastCheck > timeout) {
				//send signal to do something.
				
			}
			
			//read all messages, should not find "start clock"
			
			//Rule for all actors: Log failure and continue working
			//stopping is not an option, logging however may do extra processing to get message history.
			
			
		} else {
			//ignore all messages except "start clock" message
			
		}
		
		
		lastTimeInMS = now;

	}

}

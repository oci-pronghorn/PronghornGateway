package com.ociweb.gateway.demo;

import com.ociweb.gateway.client.APIStage;
import com.ociweb.gateway.client.APIStageFactory;
import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class ClockStageFactory extends APIStageFactory {

	private final String rate;
	private final boolean debug;
	private final int inFlightLimit;
	private final int ttlSec;
	
	public ClockStageFactory(String rate, boolean debug, int inFlightLimit, int ttlSec){
	    assert(rate.length()>0);
	    assert(Long.parseLong(rate)>=0);
		this.rate = rate;
		this.debug = debug;
		this.inFlightLimit = inFlightLimit;
		this.ttlSec = ttlSec;
	}
	
	@Override
	public APIStage newInstance(GraphManager gm, RingBuffer unusedIds, RingBuffer connectionOut, RingBuffer connectionIn) {
		return new ClockStage(gm,unusedIds,connectionOut,connectionIn, ttlSec);
	}
	
	public String getRate() {
	    return rate;
	}
	
	public boolean isDebug() {
	    return debug;
	}

    public int getInFlightLimit() {        
        return inFlightLimit;
    }
    
    public int getTTLSec() {
        return ttlSec;
    }
    
    public boolean isSecured() {
        return false;
    }

    public int getPort() {
        return 1883;
    }
}

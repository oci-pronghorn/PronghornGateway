package com.ociweb.gateway.client;

import com.ociweb.pronghorn.pipe.Pipe;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public abstract class APIStageFactory {

	public abstract APIStage newInstance(GraphManager gm, Pipe unusedIds, Pipe connectionOut, Pipe connectionIn);

    public String getRate() {
        return "0";
    }
    
    public boolean isDebug() {
        return false;
    }

    public int getInFlightLimit() {        
        return 0;
    }

    public int getTTLSec() {
        return 60;
    }

    public int getMaxTopicPlusPayload() {
        return 64; //maximum length in bytes of the payload or utf8 encoded topic
    }

    public boolean isSecured() {
        return false;
    }

    public int getPort() {
        return 1883;
    }

    public int getQueuedIdsMaxCount() {
       return 3;
    }

    public int getQueuedConInMaxCount() {
        return 3;
    }

    public int getQueuedConOutMaxCount() {
       return 20;
    }
}

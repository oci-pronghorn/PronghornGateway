package com.ociweb.gateway.common;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public interface GVSValidator {

	boolean validate(GraphManager graphManager, RingBuffer[] inputs, RingBuffer[] outputs);

	String status();

}

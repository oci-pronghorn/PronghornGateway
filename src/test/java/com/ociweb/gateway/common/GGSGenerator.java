package com.ociweb.gateway.common;

import java.util.Random;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public interface GGSGenerator {

	boolean generate(GraphManager graphManager, RingBuffer[] inputs, RingBuffer[] outputs, Random random);

}

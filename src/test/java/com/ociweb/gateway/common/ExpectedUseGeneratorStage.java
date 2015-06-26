package com.ociweb.gateway.common;

import java.util.Random;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class ExpectedUseGeneratorStage extends PronghornStage {

	private final Random random;
	private final GraphManager graphManager;
	private final RingBuffer[] inputs;
	private final RingBuffer[] outputs;
	private final GGSGenerator generator;
	
	public ExpectedUseGeneratorStage(GraphManager graphManager, RingBuffer[] inputs, RingBuffer[] outputs, Random random, GGSGenerator generator) {
		super(graphManager, inputs, outputs);
		this.graphManager = graphManager;
		this.inputs = inputs;
		this.outputs = outputs;
		this.random = random;		
		this.generator = generator;
	}

	@Override
	public void run() {
		
		if (!generator.generate(graphManager,inputs,outputs,random)) {
			//force hard shut down of stage under test
			GraphManager.terminateInputStages(graphManager);
			//force hard shut down of this stage
			GraphManager.setStateToShutdown(graphManager, stageId);
		}
		
	}

}
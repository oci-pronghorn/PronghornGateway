package com.ociweb.gateway.common;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class GeneralValidationStage extends PronghornStage{

	private final RingBuffer[] inputs;
	private final RingBuffer[] outputs;
	private final GraphManager graphManager;
	private final GVSValidator validator;
	
	public GeneralValidationStage(GraphManager graphManager, RingBuffer[] inputs, RingBuffer[] outputs, GVSValidator validator) {
		super(graphManager, inputs, outputs);
		this.inputs = inputs;
		this.outputs = outputs;
		this.graphManager = graphManager;
		this.validator = validator;
	}

	@Override
	public void run() {		
		if (!validator.validate(graphManager, inputs, outputs)) {
			//force hard shut down of stage under test and generator
			GraphManager.terminateInputStages(graphManager);
			//force hard shut down of this stage
			GraphManager.setStateToShutdown(graphManager, stageId);
		}
	}
	
	@Override
	public void shutdown() {
		System.out.println("shutdown validator :"+validator.status());
	}

}

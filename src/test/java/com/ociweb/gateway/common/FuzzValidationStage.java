package com.ociweb.gateway.common;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.ring.stream.StreamingReadVisitor;
import com.ociweb.pronghorn.ring.stream.StreamingReadVisitorToJSON;
import com.ociweb.pronghorn.ring.stream.StreamingVisitorReader;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class FuzzValidationStage extends PronghornStage{

	private final StreamingVisitorReader reader;
	private boolean foundError = false;
	
	
	public FuzzValidationStage(GraphManager graphManager, RingBuffer input) {
		super(graphManager, input, NONE);

		StreamingReadVisitor visitor = new StreamingReadVisitorToJSON(System.out);
		
        reader = new StreamingVisitorReader(input, visitor);//, new StreamingReadVisitorDebugDelegate(visitor) );
		
	}

    @Override
    public void startup() {
    	reader.startup();
    }
    
    @Override
    public void run() {
    	reader.run();
    	
    	//TODO: C, need to set foundError for bad data on output ring.
    	
    }
    
    @Override
    public void shutdown() {
    	reader.shutdown();
    }

	public boolean foundError() {
		return foundError;
	}

}

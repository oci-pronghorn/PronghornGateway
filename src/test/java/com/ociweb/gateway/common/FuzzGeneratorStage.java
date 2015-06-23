package com.ociweb.gateway.common;

import java.util.Random;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.ring.stream.StreamingVisitorWriter;
import com.ociweb.pronghorn.ring.stream.StreamingWriteVisitor;
import com.ociweb.pronghorn.ring.stream.StreamingWriteVisitorGenerator;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class FuzzGeneratorStage extends PronghornStage{

    private final StreamingVisitorWriter writer;
    private final long duration;
    private long timeLimit;
    
    public FuzzGeneratorStage(GraphManager gm, Random random, long duration, RingBuffer output) {
        super(gm, NONE, output);
        
        this.duration = duration;
        StreamingWriteVisitor visitor = new StreamingWriteVisitorGenerator(RingBuffer.from(output), random, 
                                           output.maxAvgVarLen>>3,  //room for UTF8 
                                           output.maxAvgVarLen>>1); //just use half       
        this.writer = new StreamingVisitorWriter(output, visitor  );
        
    }

    
    @Override
    public void startup() {
    	timeLimit = System.currentTimeMillis()+duration;
        writer.startup();
    }
    
    @Override
    public void run() {
        if (System.currentTimeMillis()<timeLimit) {
            writer.run();
        } else {
            requestShutdown();
        }
    }
    
    @Override
    public void shutdown() {
        writer.shutdown();
    }
}

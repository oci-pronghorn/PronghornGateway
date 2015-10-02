package com.ociweb.gateway.demo;

import com.ociweb.gateway.client.APIStage;
import com.ociweb.pronghorn.pipe.Pipe;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class ClockStage extends APIStage {
    
    private byte[] NONE;
    private byte[] workspace;
    
    private int conFlags;
    private int willTopicIdx;
    private int willTopicLength;
    private int willMessageBytesIdx;
    private int willMessageBytesLength;
    private byte[] username;
    private byte[] passwordBytes;
    
    private int topicIdx;
    private int topicLength;
    private int qualityOfService;
    private int retain;
    private int payloadIdx;
    private int payloadLength;
    
    boolean connected;
    private final long rate;
    
    public ClockStage(GraphManager graphManager, Pipe idGenIn, Pipe fromC, Pipe toC, int ttlSec, String rate) {
		super(graphManager, idGenIn, fromC, toC, ttlSec);
		this.rate = Long.parseLong(rate);
		GraphManager.addNota(graphManager, GraphManager.SCHEDULE_RATE, rate, this);
				
	}

    private final static int WORK_BITS = 10;
    private final static int WORK_SIZE = 1<<WORK_BITS;
    private final static int WORK_MASK = WORK_SIZE-1;
    private final static String TIME_TOPIC = "root/heartbeat/time";
    private final static String EXIT_TOPIC = "root/heartbeat/exit";
    private final static String EXIT_PAYLOAD = "bye";
    
    private long seqCount;
    
	@Override
    public void startup() {
	    
	    System.out.println();        
	    System.out.print("sending to topic ");
	    System.out.print(TIME_TOPIC);
	    System.out.println(" payload of 9 bytes  [byte seq][long time in ms][long rate in ms] most significant byte first.");
        System.out.println();
        System.out.print("last will is set to ");
        System.out.print(EXIT_TOPIC);
        System.out.print(" ");
        System.out.println(EXIT_PAYLOAD);	    
	    
	    NONE = new byte[0];
	    workspace = new byte[WORK_SIZE];
	    
	    username = NONE;
	    passwordBytes = NONE;
	    conFlags = 0; //MQTTEncoder.CONNECT_FLAG_CLEAN_SESSION_1; //Not set so the session is remembered if we disconnect
	    
	    //begin population of the working array
	    
	    int runningPos = 0;
	    
	    topicIdx = runningPos;
	    topicLength = Pipe.convertToUTF8(TIME_TOPIC, 0, TIME_TOPIC.length(), workspace, runningPos, WORK_MASK);
	    runningPos += topicLength;	    
	    assert(runningPos < WORK_SIZE);
	    
	    willTopicIdx = runningPos;
	    willTopicLength = Pipe.convertToUTF8(EXIT_TOPIC, 0, EXIT_TOPIC.length(), workspace, runningPos, WORK_MASK);
	    runningPos += willTopicLength;      
	    assert(runningPos < WORK_SIZE);
	    
	    willMessageBytesIdx = runningPos;
        willMessageBytesLength = Pipe.convertToUTF8(EXIT_PAYLOAD, 0, EXIT_PAYLOAD.length(), workspace, runningPos, WORK_MASK);
        runningPos += willMessageBytesLength;      
        assert(runningPos < WORK_SIZE);
        
        payloadIdx = runningPos;
        payloadLength = 1+8+8;        
        
        int tmp = runningPos+9;//skip over the dynamic fields and add this constant
        workspace[WORK_MASK&tmp++] = (byte) (rate >> 56);
        workspace[WORK_MASK&tmp++] = (byte) (rate >> 48);
        workspace[WORK_MASK&tmp++] = (byte) (rate >> 40);
        workspace[WORK_MASK&tmp++] = (byte) (rate >> 32);
        
        workspace[WORK_MASK&tmp++] = (byte) (rate >> 24);
        workspace[WORK_MASK&tmp++] = (byte) (rate >> 16);
        workspace[WORK_MASK&tmp++] = (byte) (rate >> 8);
        workspace[WORK_MASK&tmp++] = (byte) (rate >> 0); 
                
        runningPos += payloadLength;      
        assert(runningPos < WORK_SIZE);
        
        //end population of the working array        
        
        qualityOfService = Integer.parseInt(ClockApp.qos);
        retain = 0;//do not keep the clock but always wait for the next tick        
        
	    connected = requestConnect(ClockApp.destinationIp, conFlags, 
	                workspace, willTopicIdx, willTopicLength, WORK_MASK, 
	                workspace, willMessageBytesIdx, willMessageBytesLength, WORK_MASK, 
	                                username, passwordBytes);
    }
  
	boolean readyForNewMessage = true;

    @Override
    public void businessLogic() {
        
        if (!connected) {
            if (! (connected = requestConnect(ClockApp.destinationIp, conFlags, workspace, willTopicIdx, willTopicLength, WORK_MASK,  workspace, willMessageBytesIdx, willMessageBytesLength, WORK_MASK, username, passwordBytes))) {
                return;
            }
        }
              
        if (readyForNewMessage) {
            buildNewMessage(); 
        }
        
        long pubPos = requestPublish(workspace, topicIdx, topicLength, WORK_MASK, qualityOfService, retain, workspace, payloadIdx, payloadLength, WORK_MASK);
        readyForNewMessage = pubPos>=0;
        
        
        //TEST TODO: when we disconnect every time it becomes corrupt and hangs.        
        
        while (!requestDisconnect()) {
            //TODO: modify shutdown so we can pass the boolean back to the scheduler.
        }
        connected = false;
        
	}


    private void buildNewMessage() {
        int tmp = payloadIdx;

        seqCount++;
        workspace[WORK_MASK&tmp++] = (byte) seqCount;
        
        long now = System.currentTimeMillis();
        workspace[WORK_MASK&tmp++] = (byte) (now >> 56);
        workspace[WORK_MASK&tmp++] = (byte) (now >> 48);
        workspace[WORK_MASK&tmp++] = (byte) (now >> 40);
        workspace[WORK_MASK&tmp++] = (byte) (now >> 32);
        
        workspace[WORK_MASK&tmp++] = (byte) (now >> 24);
        workspace[WORK_MASK&tmp++] = (byte) (now >> 16);
        workspace[WORK_MASK&tmp++] = (byte) (now >> 8);
        workspace[WORK_MASK&tmp++] = (byte) (now >> 0);
    }


    @Override
    public void shutdown() {
       while (!requestDisconnect()) {
           //TODO: modify shutdown so we can pass the boolean back to the scheduler.
       }
    }


    @Override
    protected void ackReceived(int packetId, int qos) {
        //NOTE: if we would like to confirm delivery it is done here.        
    }



    public void printExitStatus() {
        System.out.print("Total time values sent :");
        System.out.println(seqCount);
    }
    
    
    
	
}

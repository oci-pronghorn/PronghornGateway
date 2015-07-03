package com.ociweb.gateway.broker;

import java.util.concurrent.TimeUnit;

import com.ociweb.gateway.client.APIStage;
import com.ociweb.gateway.client.ClientAPIFactory;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;
import com.ociweb.pronghorn.stage.scheduling.StageScheduler;
import com.ociweb.pronghorn.stage.scheduling.ThreadPerStageScheduler;

public class PronghornGateway {

	public static void main(String[] args) {
		
		GraphManager gm = new GraphManager();
		APIStage stage = ClientAPIFactory.clientAPI(gm);
		
		StageScheduler scheduler = new ThreadPerStageScheduler(gm);
		scheduler.startup();
		scheduler.shutdown();
		scheduler.awaitTermination(10, TimeUnit.MILLISECONDS);
		
		// TODO Auto-generated method stub
		System.out.println("Placeholder until this class is finished");
	}

}

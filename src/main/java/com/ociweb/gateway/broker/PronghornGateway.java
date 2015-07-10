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
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//zulu 8
		//20.9,  total heap MB 1.5  .5 free
		
		//oracle 7
		//20     total heap MB 6.8   4.7 free
		//oracle 8
		//23.6   total heap MB 1.5   .2 free 
		
		scheduler.shutdown();
		
		System.err.println("total:"+ Runtime.getRuntime().totalMemory());
		System.err.println("max:"+ Runtime.getRuntime().maxMemory());
		System.err.println("free:"+ Runtime.getRuntime().freeMemory());
		
		try {
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		scheduler.awaitTermination(10, TimeUnit.MILLISECONDS);
		
		// TODO Auto-generated method stub
		System.out.println("Placeholder until this class is finished");
	}

}

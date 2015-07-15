package com.ociweb.gateway.client;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.eclipse.moquette.server.Server;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;
import com.ociweb.pronghorn.stage.scheduling.StageScheduler;
import com.ociweb.pronghorn.stage.scheduling.ThreadPerStageScheduler;

public class IntegrationTest {

	public class IntegrationTestConnectDisconnector extends APIStage {

		private int iterations;
		
		public IntegrationTestConnectDisconnector(GraphManager gm, RingBuffer unusedIds, RingBuffer connectionOut,
				RingBuffer connectionIn, int iterations) {
			super(gm,unusedIds,connectionOut,connectionIn);
			this.iterations = iterations;
		}

		@Override
		public void businessLogic() {
			
			if (--iterations>=0) {
				CharSequence url = "127.0.0.1";
				int conFlags = MQTTEncoder.CONNECT_FLAG_CLEAN_SESSION_1;
				
				byte[] empty = new byte[0];
				
				byte[] willTopic = empty;
				byte[] willMessageBytes = empty;
				byte[] username = empty;
				byte[] passwordBytes = empty;
				
				requestConnect(url, conFlags, willTopic, willMessageBytes, username, passwordBytes);
							
				requestDisconnect();
							
			} else {
				requestShutdown();
			}
			
		}		
		
		
	}


	@BeforeClass
	public static void setup() {
		try {
			startBroker();
			startSubsriber();
		} catch (Exception e) {
			
			e.printStackTrace();
			fail();
		}
	}
	
	
	
	private static void startSubsriber() throws MqttException {
		MqttClient client = new MqttClient("tcp://localhost:1883", "TestClient", new MemoryPersistence());
		
		MqttCallback callback = new MqttCallback() {

			@Override
			public void connectionLost(Throwable cause) {
				cause.printStackTrace();
				fail();
			}

			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				
				//TODO: write to ring so the values can be checked in the test code.
				
//				int metaMask = ((QOS_MASK & message.getQos()) << QOS_SHIFT) |
//					       ((message.isRetained() ? 1 : 0) << RET_SHIFT) |
//					       ((message.isDuplicate() ? 1 : 0) << DUP_SHIFT);
//			
//			byte[] payload = message.getPayload();
//			
//			
//			RingWriter.blockWriteFragment(outputRing, MSG_MQTT);
//			
//			RingWriter.writeASCII(outputRing, FIELD_TOPIC, topic);
//			RingWriter.writeBytes(outputRing, FIELD_PAYLOAD, payload);
//			RingWriter.writeInt(outputRing, FIELD_META_MASK, metaMask);
//			
//			RingWriter.publishWrites(outputRing);
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
				System.err.println("should not be called for subscriber");
				System.err.println(token);
				fail();
				
			}};
		client.setCallback(callback);
		
		client.connect();
		client.subscribe("#", 2);
	}


    @Test		
	public void testConnectDisconnect() {
		//for this test we will use a known working broker and known working subscriber (both from eclipse)
		//we will connect, publish and disconnect with the pronghorn code and confirm the expected values in the subscriber.
		
    	final int testSize = 5;
    	
		GraphManager gm = new GraphManager();
		APIStageFactory factory = new APIStageFactory() {

			@Override
			public APIStage newInstance(GraphManager gm, RingBuffer unusedIds, RingBuffer connectionOut, RingBuffer connectionIn) {
				return new IntegrationTestConnectDisconnector(gm, unusedIds, connectionOut, connectionIn, testSize);
			}
			
		};
		ClientAPIFactory.clientAPI(factory ,gm); //TODO:: Make own stage and in run measure to send value.
		
	    StageScheduler scheduler = new ThreadPerStageScheduler(gm);
        scheduler.startup();
        scheduler.awaitTermination(10, TimeUnit.SECONDS);
		
        //TODO: need to confirm count of connections.
       
       
		
		
	}
	
	
	//then test publish QoS 0
	
	//then test publish QoS 1
	
	//then test publish QoS 2
	
	
	
	
	private static void startBroker() throws IOException {
		
        final Server server = new Server();
        
        String configPath = System.getProperty("moquette.path", null);
        server.startServer(new File(configPath, "config/moquette.conf"));
                
        //Bind  a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.stopServer();
            }
        });
        
	}
	
	
}

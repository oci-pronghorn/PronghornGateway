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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;
import com.ociweb.pronghorn.stage.scheduling.StageScheduler;
import com.ociweb.pronghorn.stage.scheduling.ThreadPerStageScheduler;

public class IntegrationTest {


    private static Server server;
    private static Logger log = LoggerFactory.getLogger(IntegrationTest.class);
	
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
	
	@AfterClass
	public static void shutdown() {
		server.stopServer();
		server = null;
	}
	
	public class IntegrationTestQOS0Publish extends APIStage {
		
		private int toSend;
		private final int iterations;
		private int connectionsCounted;
		private boolean cleanShutdown = false;
		
		private final String topic = "root\\box\\color";		
		private final int valuesBits = 8;
		private final int valuesMask = (1<<valuesBits)-1;
		private final byte[] values = new byte[valuesMask+1];
		
		private final int topicPos = 0;
		private final int topicLen = MQTTEncoder.convertToUTF8(topic,0,topic.length(),
				                                 values, topicPos, valuesMask);
		
		private final int payloadPos = 0 + topicLen;			
		private final int payloadLen = 32;
		
		public IntegrationTestQOS0Publish(GraphManager gm, RingBuffer unusedIds, RingBuffer connectionOut, RingBuffer connectionIn, int iterations) {
			super(gm,unusedIds,connectionOut,connectionIn);
			this.toSend = iterations;
			this.iterations =iterations;
			
			int i = payloadLen;
			while (--i>=0) {
				values[payloadPos+i] = (byte)i;
			}
			
		}

		@Override
		public void businessLogic() {
			
			if (--toSend>=0) {
				System.err.println("sent connect, disconect "+toSend);
				CharSequence url = "127.0.0.1";
				int conFlags = MQTTEncoder.CONNECT_FLAG_CLEAN_SESSION_1;
				
				byte[] empty = new byte[0];
				
				byte[] willTopic = empty;
				byte[] willMessageBytes = empty;
				byte[] username = empty;
				byte[] passwordBytes = empty;
				
				while (!requestConnect(url, conFlags, willTopic, willMessageBytes, username, passwordBytes)) {
					
				}
						
				final int qualityOfService = 0;
								
				int retain = 0;
							
				long pos;
				while (-1==(pos = requestPublish(values, topicPos, topicLen, valuesMask, qualityOfService, retain, values, payloadPos, payloadLen, valuesMask))) {
				}
				
				while (!requestDisconnect()) {
					
				};
							
			} else {				
				if (connectionsCounted>=iterations) {
					cleanShutdown = true;
					requestShutdown();		
					log.error("shutdown of test was requested.");
				}
			}
			
		}
	}
	
	
	public class IntegrationTestConnectDisconnector extends APIStage {

		private int toSend;
		private final int iterations;
		private int connectionsCounted;
		private boolean cleanShutdown = false;
		
		public IntegrationTestConnectDisconnector(GraphManager gm, RingBuffer unusedIds, RingBuffer connectionOut,
				RingBuffer connectionIn, int iterations) {
			super(gm,unusedIds,connectionOut,connectionIn);
			this.toSend = iterations;
			this.iterations =iterations;
		}

		
		@Override
		public void businessLogic() {
			
			if (--toSend>=0) {
				System.err.println("sent connect, disconect "+toSend);
				CharSequence url = "127.0.0.1";
				int conFlags = MQTTEncoder.CONNECT_FLAG_CLEAN_SESSION_1;
				
				byte[] empty = new byte[0];
				
				byte[] willTopic = empty;
				byte[] willMessageBytes = empty;
				byte[] username = empty;
				byte[] passwordBytes = empty;
				
				while (!requestConnect(url, conFlags, willTopic, willMessageBytes, username, passwordBytes)) {
					
				}
							
				while (!requestDisconnect()) {
					
				};
							
			} else {				
				if (connectionsCounted>=iterations) {
					cleanShutdown = true;
					requestShutdown();		
					log.error("shutdown of test was requested.");
				}
			}
			
		}

		@Override
		public void newConnection() {
			connectionsCounted++;
		}

		@Override
		public void newConnectionError(int err) {
			fail("Unable to connect due to error code:"+err);
		}

		@Override
		public void shutdown() {
			if (connectionsCounted<iterations) {
				fail("expected "+iterations+" counnetions but found "+connectionsCounted);
			}
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
        scheduler.awaitTermination(3, TimeUnit.SECONDS);		
	}
	
    @Test		
	public void testQoS0() {
		//for this test we will use a known working broker and known working subscriber (both from eclipse)
		//we will connect, publish and disconnect with the pronghorn code and confirm the expected values in the subscriber.
		
    	final int testSize = 5;
    	
		GraphManager gm = new GraphManager();
		APIStageFactory factory = new APIStageFactory() {

			@Override
			public APIStage newInstance(GraphManager gm, RingBuffer unusedIds, RingBuffer connectionOut, RingBuffer connectionIn) {
				return new IntegrationTestQOS0Publish(gm, unusedIds, connectionOut, connectionIn, testSize);
			}
			
		};
		ClientAPIFactory.clientAPI(factory ,gm); //TODO:: Make own stage and in run measure to send value.
		
	    StageScheduler scheduler = new ThreadPerStageScheduler(gm);
        scheduler.startup();
        scheduler.awaitTermination(3, TimeUnit.SECONDS);		
	}
	    
    
    
	
	//then test publish QoS 0
	
	//then test publish QoS 1
	
	//then test publish QoS 2
	
	
	
	private static void startBroker() throws IOException {
		
        server = new Server();
        
        String configPath = System.getProperty("moquette.path", null);
        server.startServer(new File(configPath, "config/moquette.conf"));
                
        //Bind  a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
            	if (null!=server) {
            		server.stopServer();
            	}
            }
        });
        
	}
	
	
}

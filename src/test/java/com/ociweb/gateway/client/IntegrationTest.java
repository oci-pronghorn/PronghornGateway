package com.ociweb.gateway.client;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.pronghorn.pipe.Pipe;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;
import com.ociweb.pronghorn.stage.scheduling.StageScheduler;
import com.ociweb.pronghorn.stage.scheduling.ThreadPerStageScheduler;

public class IntegrationTest {


    private static Server server;
    private static MqttClient client;
    
    private static Logger log = LoggerFactory.getLogger(IntegrationTest.class);
	
	private final static String qos0TestTopic = "root/qos0test/box/color";	
	private final static int qos0ConnectionIterations = 3;
	private final static int qos0Messages = 25;
	private final static int qos0TestPayloadLength = 32;
	
    private final static String qos1TestTopic = "root/qos1test/box/color";  
	private final static int qos1ConnectionIterations = 1; //TODO: replay on disconnect is broken, must fix
	private final static int qos1Messages = 15;
	private final static int qos1TestPayloadLength = 32;
	
    private final static String qos2TestTopic = "root/qos2test/box/color";  
    private final static int qos2ConnectionIterations = 1;
    private final static int qos2Messages = 21;
    private final static int qos2TestPayloadLength = 32;
	
	private static int qos0TestTotalCount = 0;
	private static int qos1TestTotalCount = 0;
	private static int qos1TestTotalAckCount = 0;
	private static int qos2TestTotalCount = 0;
	private static int qos2TestTotalAckCount = 0;
    
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

		try {
			client.disconnect();
			client.close();
		} catch (MqttException e) {
		    //we want to disconnect so if its already done this is not a problem
		    if (!e.getMessage().contains("Client is disconnected")) {
		        fail(e.getMessage());
		    }
		}
		server.stopServer();
		server = null;
	}
	
	
    public class IntegrationTestQOS2Publish extends APIStage {
        
        private int iterationCount;
        private int messageCount;
        private byte connectState;//-1 need contect, 0 do nothing, 1 need disconnect
        private final byte NEED_CONNECT = -1;
        private final byte NEED_NOTHING = 0;
        private final byte NEED_DISCONNECT = 1;
                             
        
        private final String topic = qos2TestTopic;
        
        private final int valuesBits = 8;
        private final int valuesMask = (1<<valuesBits)-1;
        private final byte[] values = new byte[valuesMask+1];
        
        private final int topicPos = 0;
        private final int topicLen = Pipe.convertToUTF8(topic,0,topic.length(),
                                                 values, topicPos, valuesMask);
        
        private final int payloadPos = 0 + topicLen;            
        private final int payloadLen = qos1TestPayloadLength;
        private final byte[] empty = new byte[0];
        private final Pipe connectionIn;
        
        private final int messages;
        
        public IntegrationTestQOS2Publish(GraphManager gm, Pipe unusedIds, Pipe connectionOut, Pipe connectionIn, int iterations, int messages) {
            super(gm,unusedIds,connectionOut,connectionIn,60);
            this.iterationCount = iterations;
            this.messageCount = messages;
            this.connectState = NEED_CONNECT;
            this.messages = messages;
            
            this.connectionIn = connectionIn;
            
            int i = payloadLen;
            while (--i>=0) {
                values[payloadPos+i] = (byte)i;
            }
            
        }

        @Override
        public void businessLogic() {
            CharSequence url = "127.0.0.1";
            int conFlags = 0;//MQTTEncoder.CONNECT_FLAG_CLEAN_SESSION_1; //do not clean session                              
            byte[] willTopic = empty;
            byte[] willMessageBytes = empty;
            byte[] username = empty;
            byte[] passwordBytes = empty;
            final int qualityOfService = 2;                      
            int retain = 0;
            
            //NOTE: this is not a spinning while any write that is not successful will return and try again later.
            //      but as long as every write succeeds we will stay here.
            while (iterationCount>=0) {
                
                if (NEED_DISCONNECT == connectState) {
                    if (requestDisconnect()) {
                        connectState = NEED_CONNECT;
                    } else {
                        return;//try again later
                    }
                }
                 
                if (NEED_CONNECT == connectState) {
                    if (requestConnect(url, conFlags, willTopic,0,0,0, willMessageBytes,0,0,0, username, passwordBytes)) {                  
                        connectState = NEED_NOTHING;
                        messageCount = messages;
                    } else {
                        return;//try again later
                    }
                }
                 
                if (messageCount>0) {
                    if (-1!=(requestPublish(values, topicPos, topicLen, valuesMask, qualityOfService, retain, values, payloadPos, payloadLen, valuesMask))) {
                        if (--messageCount<=0) {
                            if (1==iterationCount) {
                                //On the last trip we must stay connected so that all the Acks will be recieved
                                connectState = NEED_NOTHING; //now done waiting for all the acks.
                                return;
                            }
                            if (requestDisconnect()) {                  
                                connectState = NEED_CONNECT;
                            } else {
                                connectState = NEED_DISCONNECT;
                                return;//try again later
                            }
                            
                        }
                    } else {
                        return;//try again later
                    }
                } else {
                    iterationCount--;                      
                }
             } 
        }
        
        @Override
        protected void ackReceived2(int packetId) {
            
            assertEquals(0, Pipe.getPublishBatchSize(connectionIn));
            
            if (++qos2TestTotalAckCount==qos2Messages*qos2ConnectionIterations) {
                requestDisconnect();
                //only shutdown after we get the right count of acks 
                requestShutdown();               
            }
        }
        
        
    }

	   public class IntegrationTestQOS1Publish extends APIStage {
           
           private int iterationCount;
           private int messageCount;
           private byte connectState;//-1 need contect, 0 do nothing, 1 need disconnect
           private final byte NEED_CONNECT = -1;
           private final byte NEED_NOTHING = 0;
           private final byte NEED_DISCONNECT = 1;
                                
           private final String topic = qos1TestTopic;
           
           private final int valuesBits = 8;
           private final int valuesMask = (1<<valuesBits)-1;
           private final byte[] values = new byte[valuesMask+1];
           
           private final int topicPos = 0;
           private final int topicLen = Pipe.convertToUTF8(topic,0,topic.length(),
                                                    values, topicPos, valuesMask);
           
           private final int payloadPos = 0 + topicLen;            
           private final int payloadLen = qos1TestPayloadLength;
           
           private final int messages;
           private final byte[] empty = new byte[0];
           
           private final Pipe connectionIn;
           
           public IntegrationTestQOS1Publish(GraphManager gm, Pipe unusedIds, Pipe connectionOut, Pipe connectionIn, int iterations, int messages) {
               super(gm,unusedIds,connectionOut,connectionIn,60);
               
               this.iterationCount = iterations;
               this.messageCount = messages;
               this.connectState = NEED_CONNECT;
               
               this.messages = messages;
               this.connectionIn = connectionIn;
               
               int i = payloadLen;
               while (--i>=0) {
                   values[payloadPos+i] = (byte)i;
               }
               
           }

           @Override
           public void businessLogic() {
               CharSequence url = "127.0.0.1";
               int conFlags = 0;//MQTTEncoder.CONNECT_FLAG_CLEAN_SESSION_1; //do not clean session                              
               byte[] willTopic = empty;
               byte[] willMessageBytes = empty;
               byte[] username = empty;
               byte[] passwordBytes = empty;
               final int qualityOfService = 1;                      
               int retain = 0;
               
               //NOTE: this is not a spinning while any write that is not successful will return and try again later.
               //      but as long as every write succeeds we will stay here.
               while (iterationCount>=0) {
                   
                  if (NEED_DISCONNECT == connectState) {
                      if (requestDisconnect()) {
                          connectState = NEED_CONNECT;
                      } else {
                          return;//try again later
                      }
                  }
                   
                  if (NEED_CONNECT == connectState) {
                      if (requestConnect(url, conFlags, willTopic,0,0,0, willMessageBytes,0,0,0, username, passwordBytes)) {                  
                          connectState = NEED_NOTHING;
                          messageCount = messages;
                      } else {
                          return;//try again later
                      }
                  }
                   
                  if (messageCount>0) {
                      if (-1!=(requestPublish(values, topicPos, topicLen, valuesMask, qualityOfService, retain, values, payloadPos, payloadLen, valuesMask))) {
                          if (--messageCount<=0) {
                              if (1==iterationCount) {
                                  //On the last trip we must stay connected so that all the Acks will be recieved
                                  connectState = NEED_NOTHING; //now done waiting for all the acks.
                                  return;
                              }
                              if (requestDisconnect()) {                  
                                  connectState = NEED_CONNECT;
                              } else {
                                  connectState = NEED_DISCONNECT;
                                  return;//try again later
                              }
                              
                          }
                      } else {
                          return;//try again later
                      }
                  } else {
                      iterationCount--;                      
                  }
               } 
           }
           
           protected void ackReceived1(int packetId) {
               
               assertEquals(0, Pipe.getPublishBatchSize(connectionIn));
               
               if (++qos1TestTotalAckCount==qos1Messages*qos1ConnectionIterations) {
                   requestDisconnect();
                   //only shutdown after we get the right count of acks 
                   requestShutdown();               
               }
           }
           
       }
	
	public class IntegrationTestQOS0Publish extends APIStage {
		
		private int toSend;
		private final int iterations;
		private int connectionsCounted;
		
		private final String topic = qos0TestTopic;
		
		private final int valuesBits = 8;
		private final int valuesMask = (1<<valuesBits)-1;
		private final byte[] values = new byte[valuesMask+1];
		
		private final int topicPos = 0;
		private final int topicLen = Pipe.convertToUTF8(topic,0,topic.length(),
				                                 values, topicPos, valuesMask);
		
		private final int payloadPos = 0 + topicLen;			
		private final int payloadLen = qos0TestPayloadLength;
		
		private final int messages;
				
		private Pipe connectionOut;
		
		public IntegrationTestQOS0Publish(GraphManager gm, Pipe unusedIds, Pipe connectionOut, Pipe connectionIn, int iterations, int messages) {
			super(gm,unusedIds,connectionOut,connectionIn,60);
			this.toSend = iterations;
			this.iterations =iterations;
			this.messages = messages;
			this.connectionOut = connectionOut;
			int i = payloadLen;
			while (--i>=0) {
				values[payloadPos+i] = (byte)i;
			}
			
		}

		@Override
		public void businessLogic() {
			
			if (--toSend>=0) {
				
				CharSequence url = "127.0.0.1";
				int conFlags = 0;//MQTTEncoder.CONNECT_FLAG_CLEAN_SESSION_1; //do not clean session
				
				byte[] empty = new byte[0];
				
				byte[] willTopic = empty;
				byte[] willMessageBytes = empty;
				byte[] username = empty;
				byte[] passwordBytes = empty;
				
				/////////////////DO NOT COPY THIS APPROACH, DO NOT USE WHILE LOOPS LIKE THIS/////////////
				//NOTE: we can only use this lazy approach because there are no Acks back from the server.
				//      if we got messages back they would back up the incoming pipe.  When it gets backed up
				//      then our outgoing pipe will get backed up and we will hang right here in one of these while loops.
				/////////////////////////////////////////////////////////////////////////////////////////
				
				while (!requestConnect(url, conFlags, willTopic,0,0,0, willMessageBytes,0,0,0, username, passwordBytes)) {					
				}
						
				final int qualityOfService = 0;
								
				int retain = 0;
							
				int count = messages;
				while (--count>=0) {
					while (-1==requestPublish(values, topicPos, topicLen, valuesMask, qualityOfService, retain, values, payloadPos, payloadLen, valuesMask)) {
					}
				}
				
				
				while (!requestDisconnect()) {					
				}

							
			} else {				
				requestShutdown();		
				log.trace("shutdown of test was requested.");
			}
			
		}
		
		
		
	}
	
	
	public class IntegrationTestConnectDisconnector extends APIStage {

		private int toSend;
		private final int iterations;
		private int connectionsCounted;
		
		public IntegrationTestConnectDisconnector(GraphManager gm, Pipe unusedIds, Pipe connectionOut,
				Pipe connectionIn, int iterations) {
			super(gm,unusedIds,connectionOut,connectionIn,60);
			this.toSend = iterations;
			this.iterations =iterations;
		}

		
		@Override
		public void businessLogic() {
			
			if (--toSend>=0) {
				CharSequence url = "127.0.0.1";
				int conFlags = MQTTEncoder.CONNECT_FLAG_CLEAN_SESSION_1;
				
				byte[] empty = new byte[0];
				
				byte[] willTopic = empty;
				byte[] willMessageBytes = empty;
				byte[] username = empty;
				byte[] passwordBytes = empty;
				
				while (!requestConnect(url, conFlags, willTopic,0,0,0, willMessageBytes,0,0,0, username, passwordBytes)) {
					
				}
							
				while (!requestDisconnect()) {
					
				};
							
			} else {				
				if (connectionsCounted>=iterations) {
					requestShutdown();		
					log.trace("shutdown of test was requested.");
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
		client = new MqttClient("tcp://localhost:1883", "TestClient", new MemoryPersistence());
		
		MqttCallback callback = new MqttCallback() {

			@Override
			public void connectionLost(Throwable cause) {
				fail(cause.getMessage());
			}

			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
			    
			    System.out.println("***************  subscriber recieved message on topic "+topic+" for QOS "+message.getQos());
			    
				if (0==message.getQos() && topic.equals(qos0TestTopic)) {
				    if (++qos0TestTotalCount>(qos0ConnectionIterations*qos0Messages) ) {
				        fail("too many messages");
				    }
					//the QoS 0 test will send 
					byte[] payload = message.getPayload();
					int i = qos0TestPayloadLength;
					while (--i>=0) {
					    if (payload[i]!=i) {
					        fail("failue in payload data for QOS "+message.getQos());
					    }
					}
					//System.out.println("OK XXXX");
				} else if (1==message.getQos() && topic.equals(qos1TestTopic)) {
                    if (++qos1TestTotalCount>(qos1ConnectionIterations*qos1Messages) ) {
                        fail("too many messages");
                    }
                    //the QoS 0 test will send 
                    byte[] payload = message.getPayload();
                    int i = qos1TestPayloadLength;
                    while (--i>=0) {
                        if (payload[i]!=i) {
                            fail("failue in payload data for QOS "+message.getQos());
                        }
                    }
                } else if (2==message.getQos() && topic.equals(qos2TestTopic)) {
                    if (++qos2TestTotalCount>(qos2ConnectionIterations*qos2Messages) ) {
                        fail("too many messages");
                    }
                    //the QoS 0 test will send 
                    byte[] payload = message.getPayload();
                    int i = qos2TestPayloadLength;
                    while (--i>=0) {
                        if (payload[i]!=i) {
                            fail("failue in payload data for QOS "+message.getQos());
                        }
                    }
                } else {					
					fail("unknown message QOS "+message.getQos()+" topic "+topic);
				}
				
		
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
			public APIStage newInstance(GraphManager gm, Pipe unusedIds, Pipe connectionOut, Pipe connectionIn) {
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
        qos0TestTotalCount = 0;
		GraphManager gm = new GraphManager();
		APIStageFactory factory = new APIStageFactory() {
			@Override
			public APIStage newInstance(GraphManager gm, Pipe unusedIds, Pipe connectionOut, Pipe connectionIn) {
				return new IntegrationTestQOS0Publish(gm, unusedIds, connectionOut, connectionIn, qos0ConnectionIterations, qos0Messages);
			}		
		};
		ClientAPIFactory.clientAPI(factory ,gm); //TODO:: Make own stage and in run measure to send value.
		
	    StageScheduler scheduler = new ThreadPerStageScheduler(gm);
        scheduler.startup();
        
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        scheduler.awaitTermination(2, TimeUnit.SECONDS);		
        
        assertEquals(qos0Messages*qos0ConnectionIterations, qos0TestTotalCount);
	}
	    
    @Test     
    public void testQoS1() {
        //for this test we will use a known working broker and known working subscriber (both from eclipse)
        //we will connect, publish and disconnect with the pronghorn code and confirm the expected values in the subscriber.
       
        qos1TestTotalCount = 0;
        qos1TestTotalAckCount = 0;
        
        GraphManager gm = new GraphManager();
        APIStageFactory factory = new APIStageFactory() {
            @Override
            public APIStage newInstance(GraphManager gm, Pipe unusedIds, Pipe connectionOut, Pipe connectionIn) {
                return new IntegrationTestQOS1Publish(gm, unusedIds, connectionOut, connectionIn, qos1ConnectionIterations, qos1Messages);
            }           
        };
        ClientAPIFactory.clientAPI(factory ,gm); //TODO:: Make own stage and in run measure to send value.
        
        StageScheduler scheduler = new ThreadPerStageScheduler(gm);
        scheduler.startup();

        scheduler.awaitTermination(4, TimeUnit.SECONDS);
        
        assertEquals("Sent",qos1Messages*qos1ConnectionIterations, qos1TestTotalCount);
        assertEquals("Acked",qos1Messages*qos1ConnectionIterations, qos1TestTotalAckCount);
    }
    
    
    @Test       
    public void testQoS2() {
        //for this test we will use a known working broker and known working subscriber (both from eclipse)
        //we will connect, publish and disconnect with the pronghorn code and confirm the expected values in the subscriber.
        
        qos2TestTotalCount = 0;
        qos2TestTotalAckCount = 0;
        
        GraphManager gm = new GraphManager();
        APIStageFactory factory = new APIStageFactory() {
            @Override
            public APIStage newInstance(GraphManager gm, Pipe unusedIds, Pipe connectionOut, Pipe connectionIn) {
                return new IntegrationTestQOS2Publish(gm, unusedIds, connectionOut, connectionIn, qos2ConnectionIterations, qos2Messages);
            }           
        };
        ClientAPIFactory.clientAPI(factory ,gm); //TODO:: Make own stage and in run measure to send value.
        
        StageScheduler scheduler = new ThreadPerStageScheduler(gm);
        scheduler.startup();
        scheduler.awaitTermination(3, TimeUnit.SECONDS);     
        
        assertEquals("Acked",qos2Messages*qos2ConnectionIterations, qos2TestTotalAckCount);
        assertEquals("Sent",qos2Messages*qos2ConnectionIterations, qos2TestTotalCount);
        
    }
	
	
	
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

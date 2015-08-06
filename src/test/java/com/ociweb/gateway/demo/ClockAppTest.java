package com.ociweb.gateway.demo;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

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

public class ClockAppTest {
    
    private static Server server;
    private static MqttClient client;
    
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
        
    @Ignore //Neeed to make the test stop Test
    public void runClockAppTest() {
        //default configuration test
        ClockApp.main(new String[0]);
        
        
        
        
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
    
    private static void startSubsriber() throws MqttException {
        client = new MqttClient("tcp://localhost:1883", "TestClient", new MemoryPersistence());
        
        MqttCallback callback = new MqttCallback() {

            @Override
            public void connectionLost(Throwable cause) {
                fail(cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                
                byte seqByte = message.getPayload()[0];
                System.err.println(message.getQos()+"  "+((int)0xFF&seqByte));
                
                
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                System.err.println("should not be called for subscriber");
                System.err.println(token);
                fail();
                
            }};
            
        client.setCallback(callback);
        
        client.connect();
        client.subscribe("#", 0);
    }
    
    
}

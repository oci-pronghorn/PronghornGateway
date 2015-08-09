package com.ociweb.gateway.demo;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Date;

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
        
    //TODO: if the client starts first it must resend - if broker is down.
    @Test
    public void runClockAppTest() {
        final int TEST_DURATION = 4000; //MS        
        
        PrintStream tempStorageOfOutputStream = System.out;
        PrintStream tempStorageOfErrorStream = System.err;
        InputStream tempStorageOfInputStream = System.in;
        
        //capture all this output so we can test for the expected value
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream(); //TODO: need to protect against external users at the same time.
        System.setOut(new PrintStream(myOut));
        
        final ByteArrayOutputStream myErr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(myErr));
                
        new Thread(new Runnable() {
            @Override
            public void run() {
                //default configuration test
                ClockApp.main(new String[0]);               
            }            
        }).start();
        
        
        try {
            Thread.sleep(TEST_DURATION);
            
            System.setIn(new ByteArrayInputStream(new byte[]{13}) );
            
            
                        
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        
        System.setOut(tempStorageOfOutputStream);
        System.setOut(tempStorageOfErrorStream);
        System.setIn(tempStorageOfInputStream);
        
        System.out.println(new String(myOut.toByteArray()));
        
        //TODO: need to count and confirm the data in myOut is what we expected.
        
        
        
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
                
                byte[] data = message.getPayload();
                int seqByte = 0xFF&data[0];
                
                int qos = message.getQos();
                
                long now = 0;
                
                int idx = 0;
                while (++idx<9) {
                    now = (now<<8)|(0xFF&data[idx]);
                }                
                System.out.println(seqByte+"  "+new Date(now)+"  "+(now%1000));
                
                long rate = 0;
                idx=8;
                while (++idx<17) {
                    rate = (rate<<8)|(0xFF&data[idx]);
                }             

                System.out.println("rate: "+rate+" QOS "+qos);
                                
                
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
    
    
}

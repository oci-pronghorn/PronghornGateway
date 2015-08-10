package com.ociweb.gateway.demo;

public class RunServer {

    public static void main(String[] args) {
        ClockAppTest.setup();
        int duration = 10*60*60*1000; //run server for 10 hours then shut down.
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
           
        }
        ClockAppTest.shutdown();
    }

}

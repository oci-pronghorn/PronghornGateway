package com.ociweb.gateway.demo;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.gateway.client.ClientAPIFactory;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;
import com.ociweb.pronghorn.stage.scheduling.StageScheduler;
import com.ociweb.pronghorn.stage.scheduling.ThreadPerStageScheduler;

public class ClockApp {

    private final static GraphManager gm = new GraphManager();
    private Logger log = LoggerFactory.getLogger(ClockApp.class);
    
    static String destinationIp;
    static String qos;
    static String rate;
    
    ClockStage stage;
    
    private ClockApp(String[] args) {
        System.out.println("Clock App"); //TODO: report who we are talking to.
        destinationIp = getOptArg("-destination","-d", args, "127.0.0.1");
        qos =           getOptArg("-qos","-q", args, "0");
        rate =          getOptArg("-rate","-r", args, "100"); //in ms        

        stage = (ClockStage)ClientAPIFactory.clientAPI(ClockStageFactory.instance, gm);
    }
    
    private void run() {
        
        
        StageScheduler scheduler = new ThreadPerStageScheduler(gm);
        scheduler.startup();
        
        Scanner scan = new Scanner(System.in);
        System.out.println("press enter to exit");
        scan.hasNextLine();//blocks until enter 
       
        
        System.out.println("exiting...");
        
        scheduler.shutdown();
        scheduler.awaitTermination(100, TimeUnit.MILLISECONDS);
        
        stage.printExitStatus();
        
        
    }
    
    public static void main(String[] args) {        
        new ClockApp(args).run();
    }

    private static String getOptArg(String longName, String shortName, String[] args, String defaultValue) {
        
        String prev = null;
        for (String token : args) {
            if (longName.equals(prev) || shortName.equals(prev)) {
                if (token == null || token.trim().length() == 0 || token.startsWith("-")) {
                    return defaultValue;
                }
                return reportChoice(longName, shortName, token.trim());
            }
            prev = token;
        }
        return reportChoice(longName, shortName, defaultValue);
    }
    
    private static String reportChoice(final String longName, final String shortName, final String value) {
        System.out.print(longName);
        System.out.print(" ");
        System.out.print(shortName);
        System.out.print(" ");
        System.out.println(value);
        return value;
    }
    
}

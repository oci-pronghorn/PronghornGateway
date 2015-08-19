package com.ociweb.gateway.client;

import java.net.InetSocketAddress;

public class InetSocketAddressImmutable extends InetSocketAddress {

    private String value; 
            
    public InetSocketAddressImmutable(String hostname, int port) {
        super(hostname, port);
    }
    
    public void reset() {
        value = null;
    }
    
    @Override
    public String toString() {
        if (null == value) {
            value = super.toString();
        }
        return value;
    }

}

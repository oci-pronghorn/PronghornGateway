package com.ociweb.gateway.client;

public class Settings {

	public static final int IN_FLIGHT = 10;
    public static final long TIME_LIMIT_MS = 1000;
	public final int ttlSec = 10;//TOOD: must never be zero.                     //same for entire run
	public final byte[] clientId = "somename for this client in UTF8".getBytes(); //same for entire run
	
}

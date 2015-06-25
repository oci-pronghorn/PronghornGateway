package com.ociweb.gateway.common;

public class TestFailureDetails {

	private final String note;
	
	public TestFailureDetails(String note) {
		this.note = note;
	}
	
	public String toString() {
		return note;
	}
	
}

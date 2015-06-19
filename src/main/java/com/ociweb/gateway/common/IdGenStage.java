package com.ociweb.gateway.common;

import com.ociweb.pronghorn.ring.RingBuffer;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

/**
 * Generate ranges of packet Ids for use in distributed communication. Once communications
 * are complete the Id is returned so it can be used again later for a different transaction.
 * 
 * Ids are short values. They are stored in a single int with the low position holding the start
 * and the high value holding the exclusive stop.
 * 
 * @author Nathan Tippy
 *
 */
public class IdGenStage extends PronghornStage {

	public final int MAX_BLOCK_SIZE = 1024;
	private final int MAX_CONSUMED_BLOCKS = 128;
	private final int STOP_CODE = 0xFFFFFFFF;
	
	private int[] consumedRanges;
	private int totalRanges = 0;
		
	public IdGenStage(GraphManager graphManager, RingBuffer input, RingBuffer output) {
		super(graphManager, input, output);
	}
	
	public IdGenStage(GraphManager graphManager, RingBuffer[] inputs, RingBuffer[] outputs) {
		super(graphManager, inputs, outputs);
	}

	@Override
	public void startup() {
		consumedRanges = new int[MAX_CONSUMED_BLOCKS];
	}

	@Override
	public void run() {
		//try to keep output rings full
		//there is only one message type so the low level API is ideal here
		
		//Read all the releases and put them back first
		//Reserve new messages until 
		//          1. all output queues are full or
		//          2. no more space to allocate
		
		
		//TODO: needs non blocking code
		
		
		
	}
	
	public void releaseRange(int range) {
		
		//find spot to insert this range
		//may need to reduce size of block or delete block
		//may span multiple blocks all to be deleted.
		
		
	}
 	
	
	
	/**
	 * Find the biggest range then reserves and returns a block no bigger than MAX_BLOCK_SIZE
	 * 
	 * @return
	 */
	private int reserveNewRange() {
		//all ranges are kept in order 
		
		int max = 0;
		int maxStart = 0;
		int idx = 0;
		
		//walk list of all reserved ranges and find the biggest gap between them
		int lastBegin = 65535;
		int i = totalRanges;
		while (--i>=0) {
			int j = consumedRanges[i];
			int begin = j&0xFFFF;
			int end   = (j>>16)&0xFFFF;
			
			int len = lastBegin-end;
			if (len>max) {
				max = len;
				maxStart = end;
				idx=i+1;
			}
			
			lastBegin = begin;
		}
		//last range that starts at 0 and goes up to first reserved range
		int len = lastBegin-0;
		if (len>max) {
			max = len;
			maxStart = 0;
			idx=0;
		}
		//check that we have not run out of values
		if (0==max) {
			//return stop code
			return STOP_CODE;//try again after some ranges are released
		}		
		
		//we now know where the largest range is and where it starts.
		
		int newReservation = maxStart | ((maxStart+Math.min(max, MAX_BLOCK_SIZE))<<16);
		
		//insert new reservation
		if (idx<totalRanges) {
			System.arraycopy(consumedRanges, idx, consumedRanges, idx+1, totalRanges-idx);
		}		
		totalRanges++;
		return consumedRanges[idx]=newReservation;
	}
	

}

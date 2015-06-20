package com.ociweb.gateway.common;

import static com.ociweb.pronghorn.ring.RingBuffer.addMsgIdx;
import static com.ociweb.pronghorn.ring.RingBuffer.publishWrites;
import static com.ociweb.pronghorn.ring.RingBuffer.roomToLowLevelWrite;

import java.util.Arrays;

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
	
	private final RingBuffer[] inputs;
	private final RingBuffer[] outputs;	
		
	public IdGenStage(GraphManager graphManager, RingBuffer input, RingBuffer output) {
		super(graphManager, input, output);
		this.inputs = new RingBuffer[]{input};
		this.outputs = new RingBuffer[]{output};
	}
	
	public IdGenStage(GraphManager graphManager, RingBuffer[] inputs, RingBuffer[] outputs) {
		super(graphManager, inputs, outputs);
		this.inputs = inputs;
		this.outputs = outputs;
	}

	@Override
	public void startup() {
		consumedRanges = new int[MAX_CONSUMED_BLOCKS];
	}

	@Override
	public void run() {
		//try to keep output rings full
		//there is only one message type so the low level API is ideal here
		
		//pull in all the relapsed blocks
		int i = inputs.length;
		while (--i>=0) {
			RingBuffer inputRing = inputs[i];
			int sizeOfFragment = 123;//????
			while (RingBuffer.contentToLowLevelRead(inputRing, sizeOfFragment)) {
//				int msgIdx = RingBuffer.takeMsgIdx(inputRing);
//				
//				
//				RingBuffer.readBytesAndreleaseReadLock(inputRing);
//				
//				//low level API can write multiple message and messages with multiple fragments but it 
//				//becomes more difficult. (That is what the high level API is more commonly used for)
//				//In this example we are writing 1 message that is made up of 1 fragment
//				RingBuffer.confirmLowLevelRead(inputRing, FROM.fragDataSize[msgIdx]);
			}
			
			
		}
		
		//populate all the output Rings if possible
		int j = inputs.length;
		while (--j>=0) {
			RingBuffer outputRing = outputs[j];
			int sizeOfFragment = 123;//????
			while (roomToLowLevelWrite(outputRing, sizeOfFragment) ) {
			    
				//addMsgIdx(outputRing, fragToWrite);		
				//publishWrites(outputRing);
				
			}			
			
			
		}
		
		
		//Read all the releases and put them back first
		//Reserve new messages until 
		//          1. all output queues are full or
		//          2. no more space to allocate
		
		
		//TODO: needs non blocking code
		
	}
	
	public void releaseRange(int range) {
		
		int idx = Arrays.binarySearch(consumedRanges, range);		
		if (idx<0) {
			int insertAt = -(idx+1);
			if (insertAt==totalRanges) {
				//skip because there is nothing to delete
			} else {
			
				//this number comes before that value at this position
				//must erase up to end however for bad messages end may be before start.
				//if end is greater than end must continue and delete block
				
				int releaseEnd = 0xFFFF&(range>>16);
				int rowBegin =0;
				int rows = 0;
				do {
					rowBegin= 0xFFFF&consumedRanges[++rows+insertAt];				
				} while(releaseEnd>rowBegin && (rows+insertAt<totalRanges));
				//must delete all rows but the last one may be split
				
				int lastRow = rows+insertAt-1;
				int lastEnd = (0xFFFF&(consumedRanges[lastRow]>>16));
				if (releaseEnd<lastEnd) {
					//this is a partial row so modify rather than clear
					consumedRanges[lastRow] = (0xFFFF&releaseEnd) | (lastEnd<<16);
					rows--;//this row is done
				}
				//all remaining rows are deleted
				if (rows>0) {
					int toCopy = -rows + totalRanges - idx;
					System.arraycopy(consumedRanges, idx+rows, consumedRanges, idx, toCopy);			
					totalRanges-=rows;
				}
			}
		} else {
			//exact match so just delete this row
			int toCopy = -1 + totalRanges - idx;
			System.arraycopy(consumedRanges, idx+1, consumedRanges, idx, toCopy);			
			totalRanges--;
		}	
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

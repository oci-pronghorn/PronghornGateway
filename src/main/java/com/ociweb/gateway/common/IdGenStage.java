package com.ociweb.gateway.common;

import static com.ociweb.pronghorn.ring.RingBuffer.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger log = LoggerFactory.getLogger(IdGenStage.class);
	
	public static final int MAX_BLOCK_SIZE = 2048;	
	private static final int MAX_CONSUMED_BLOCKS = 128;
	private static final int MAX_CONSUMED_BLOCKS_LIMIT = MAX_CONSUMED_BLOCKS-1;
	private static final int STOP_CODE = 0xFFFFFFFF;
	
	private int[] consumedRanges;
	private int totalRanges = 0;
	
	private final RingBuffer[] inputs;
	private final RingBuffer[] outputs;	
	private final int sizeOfFragment;
	private static final int theOneMsg = 0;// there is only 1 message supported by this stage
		
	public IdGenStage(GraphManager graphManager, RingBuffer input, RingBuffer output) {
		super(graphManager, input, output);
		this.inputs = new RingBuffer[]{input};
		this.outputs = new RingBuffer[]{output};
		assert(RingBuffer.from(input).equals(RingBuffer.from(output))) : "Both must have same message types ";	
		this.sizeOfFragment = RingBuffer.from(input).fragDataSize[theOneMsg];
		
		
	}
	
	public IdGenStage(GraphManager graphManager, RingBuffer[] inputs, RingBuffer[] outputs) {
		super(graphManager, inputs, outputs);
		this.inputs = inputs;
		this.outputs = outputs;
		//TODO: add assert to confirm that all the inputs and outputs have the same eq from.
		this.sizeOfFragment = RingBuffer.from(inputs[0]).fragDataSize[theOneMsg];
	}

	@Override
	public void startup() {
		consumedRanges = new int[MAX_CONSUMED_BLOCKS];
	}

	@Override
	public void run() {
		
		//pull in all the released ranges first so we have them to give back out again.
		int i = inputs.length;
		while (--i>=0) {
			RingBuffer inputRing = inputs[i];
			while (contentToLowLevelRead(inputRing, sizeOfFragment) && 
				   totalRanges<MAX_CONSUMED_BLOCKS_LIMIT) // only release blocks if we have room, just in case one splits, back-pressure 
			{
				int msgIdx = RingBuffer.takeMsgIdx(inputRing);
				assert(theOneMsg == msgIdx);
				
				releaseRange(RingBuffer.takeValue(inputRing));
				
				//TODO: do we send the reset request here or earler or later?
				
				RingBuffer.releaseReads(inputRing);
				RingBuffer.confirmLowLevelRead(inputRing, sizeOfFragment);
			}
		}
		
		//push out all the new ranges for use as long as we have values and the queues have room
		int j = outputs.length;
		while (--j>=0) {
			RingBuffer outputRing = outputs[j];
			while (roomToLowLevelWrite(outputRing, sizeOfFragment) ) {
			    				
				int range = reserveNewRange();
				if (STOP_CODE == range || 0==range) {
					return;//no more values to give out
				}
				
				addMsgIdx(outputRing, theOneMsg);	
				RingBuffer.addIntValue(range, outputRing);
				publishWrites(outputRing);	
				RingBuffer.confirmLowLevelWrite(outputRing, sizeOfFragment);
			}
		}
	
	}
	
	//TODO: this is a problem because it creates garbage when not logging
	public static String rangeToString(int range) {
		return Integer.toString(0xFFFF&range)+"->"+Integer.toString(0xFFFF&(range>>16));
	}
	
	private void debug(String template, int range) {
		//Method prevents garbage creation when debug mode is not enabled
		if (log.isDebugEnabled()) {
			log.debug(template,IdGenStage.rangeToString(range));
		}
	}
	
	public void releaseRange(int range) {
		
		int insertAt = findIndex(consumedRanges, 0, totalRanges, range);		
				
		if (range != consumedRanges[insertAt]) {
			int releaseEnd = 0xFFFF&(range>>16);
			int releaseBegin = 0xFFFF&range;
			debug("IdGen release range {}",range);
			
			// if begin is < last cosumedRange end then its in the middle of that row need special logic
			if (insertAt>0) {
				int lastRangeEnd = 0xFFFF&(consumedRanges[insertAt-1]>>16);
				if (releaseBegin <  lastRangeEnd  ) {
													
					if (releaseEnd < lastRangeEnd) {
						
						//cuts range in two parts, will need to add row to table
						consumedRanges[insertAt-1] = (0xFFFF&consumedRanges[insertAt-1]) | ((0xFFFF&releaseBegin)<<16);
						int toCopy = totalRanges - insertAt;					
						System.arraycopy(consumedRanges, insertAt, consumedRanges, insertAt+1, toCopy);						
						consumedRanges[insertAt] = (0xFFFF&releaseEnd) | (lastRangeEnd<<16);
						totalRanges++;
						return; // do not continue

					} else {
						consumedRanges[insertAt-1] = (0xFFFF&consumedRanges[insertAt-1]) | ((0xFFFF&releaseBegin)<<16);
						//end is >= to last end so this is a tail trim to the existing row							
						if (releaseEnd == lastRangeEnd) {
							return; //no need to continue
						}
					}					
				}
			}
			
			///
			
			//count up how many full rows must be cleared, result is in rows
			int rows = 0;
			if (insertAt<totalRanges) {
				int rowBegin =0;
				do {
					rowBegin = 0xFFFF&consumedRanges[++rows+insertAt];				
				} while(releaseEnd>rowBegin && (rows+insertAt<totalRanges));
			}
			
			//this last row may be split and needs adjustment			
			int lastRow = rows+insertAt-1;		
			if (totalRanges>0) {
				int lastEnd = (0xFFFF&(consumedRanges[lastRow]>>16));
				if (releaseEnd<lastEnd) {
					//this is a partial row so modify rather than clear
					consumedRanges[lastRow] = (0xFFFF&releaseEnd) | (lastEnd<<16);
					rows--;//this row is done
				}
			}
			
			//all full rows are deleted
			if (rows>0) {
				int toCopy = -rows + totalRanges - insertAt;					
				System.arraycopy(consumedRanges, insertAt+rows, consumedRanges, insertAt, toCopy);			
				totalRanges-=rows;
			}

		} else {
			
			debug("IdGen release range exact match {}",range);
			//exact match so just delete this row
			int toCopy = -1 + totalRanges - insertAt;
			if (toCopy>0) {
				System.arraycopy(consumedRanges, insertAt+1, consumedRanges, insertAt, toCopy);		
			}
			totalRanges--;
		}
	}
 	
	
	
	private int findIndex(int[] consumedRanges, int from, int to, int target) {
		int i = from;
		int targetStart = 0xFFFF&target;
		//while target start is before each range start keep walking
		while (i<to &&  (0xFFFF&consumedRanges[i])<targetStart) {
			i++;
		}
		return i;
	}

	/**
	 * Find the biggest range then reserves and returns a block no bigger than MAX_BLOCK_SIZE
	 * 
	 * @return
	 */
	private int reserveNewRange() {
		//if there are no selected ranges then just add the first one
		if (0 == totalRanges) {
			totalRanges = 1;
			return consumedRanges[0]= 0 | ((MAX_BLOCK_SIZE)<<16);
		}
		
		//if have existing ranges so append onto the end of one of the existing ones.
		
		//all ranges are kept in order 		
		int max = 0;
		int maxStart = 0;
		int idx = 0;
		
		//walk list of all reserved ranges and find the biggest found after each
		int lastBegin = 65534; //need room to use 65535 as the exclude end
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
				if (max >= MAX_BLOCK_SIZE) {
     				return reserveRange(maxStart, idx, MAX_BLOCK_SIZE);
				}
			}			
			lastBegin = begin;
		}
		
		//check that we have not run out of values
		if (0==max || totalRanges==consumedRanges.length) {
			//return stop code
			return STOP_CODE;//try again after some ranges are released
		}		
		
		//we now know where the largest range is and where it starts.		
		return reserveRange(maxStart, idx, Math.min(max, MAX_BLOCK_SIZE));
	}

	private int reserveRange(int rangeStart, int idx, int rangeCount) {
		assert(idx>0);
		//appends on to previous internal row
		
		int rangeEnd = rangeStart+rangeCount;
	    if (idx<totalRanges && (consumedRanges[idx]&0xFFFF)==rangeEnd) {
	    	//combine these two because reservation connects them together	    	
	    	rangeEnd = (consumedRanges[idx]>>16)&0xFFFF;
	    	System.arraycopy(consumedRanges, idx+1, consumedRanges, idx, totalRanges-idx);
	    	totalRanges--;
	    }
		
		consumedRanges[idx-1]= (consumedRanges[idx-1]&0xFFFF) | (rangeEnd<<16);
		
		//send back value that is just the new range
		int newReservation = rangeStart | ((rangeStart+rangeCount)<<16);
		debug("IdGen reserve range {}",newReservation);
		return newReservation;
		
	}	

}

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
	
	public static final int MAX_BLOCK_SIZE = 1024;
	
	private static final int MAX_CONSUMED_BLOCKS = 2+((1<<16)/MAX_BLOCK_SIZE);
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
			while (contentToLowLevelRead(inputRing, sizeOfFragment)) {
				int msgIdx = RingBuffer.takeMsgIdx(inputRing);
				assert(theOneMsg == msgIdx);
				
				releaseRange(RingBuffer.takeValue(inputRing));
				
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
		
		
		//TODO: AAAAAA, rlease can split a row and need to make a new one.
		
		if ((0xFFFF&range) != (0xFFFF&consumedRanges[insertAt]) && totalRanges>0 && insertAt<totalRanges) {
			log.warn("**************** FEATURE NOT YET IMPLEMENTED");
			
			int j = 0;
			while (j<totalRanges) {
				log.warn(j+"   "+rangeToString(consumedRanges[j]));
				j++;
			}
		    log.warn("insert at "+insertAt+" value "+rangeToString(range));
			
			System.exit(-1);
		}
		
		
		
		if (range != consumedRanges[insertAt]) {

			debug("IdGen release range {}",range);
			
			//this number comes before that value at this position
			//must erase up to end however for bad messages end may be before start.
			//if end is greater than end must continue and delete block
			int releaseEnd = 0xFFFF&(range>>16);
			int rows = 0;
			if (insertAt<totalRanges) {
				int rowBegin =0;
				do {
					rowBegin= 0xFFFF&consumedRanges[++rows+insertAt];				
				} while(releaseEnd>rowBegin && (rows+insertAt<totalRanges));
			}
			//must delete all rows but the last one may be split
			
			int lastRow = rows+insertAt-1;		
			if (totalRanges>0) {
				int lastEnd = (0xFFFF&(consumedRanges[lastRow]>>16));
				if (releaseEnd<lastEnd) {
					//this is a partial row so modify rather than clear
					consumedRanges[lastRow] = (0xFFFF&releaseEnd) | (lastEnd<<16);
					rows--;//this row is done
				}
			}
			
			//all remaining rows are deleted
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
		while (i<to &&  (0xFFFF&consumedRanges[i])<targetStart) {
			i++;
		}
		if (i>0 && (0xFFFF&consumedRanges[i])>targetStart) {
			if ((0xFFFF&(consumedRanges[i]>>16) )>targetStart) {
				return i-1;
			}
		}
		return i;
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
				if (max>=MAX_BLOCK_SIZE) {
     				return reserveRange(maxStart, idx, MAX_BLOCK_SIZE);
				}
			}
			
			lastBegin = begin;
		}

		//last range that starts at 0 and goes up to first reserved range
		int len = lastBegin-0;
		if (len>max) {
			//TODO: these are supposted to be in unsigned order however if high bit gets set its a negative!!
			//System.err.println("last len:"+ Arrays.toString(consumedRanges));
			
			max = len;
			maxStart = 0;
			idx=0;
		}
		//check that we have not run out of values
		if (0==max || totalRanges==consumedRanges.length) {
			//return stop code
			return STOP_CODE;//try again after some ranges are released
		}		
		
		//we now know where the largest range is and where it starts.
		
		int rangeCount = Math.min(max, MAX_BLOCK_SIZE);
		return reserveRange(maxStart, idx, rangeCount);
	}

	private int reserveRange(int maxStart, int idx, int rangeCount) {
		int newReservation = maxStart | ((maxStart+rangeCount)<<16);

		debug("IdGen reserve range {}",newReservation);
		
		
		//insert new reservation
		if (idx<totalRanges) {
			System.arraycopy(consumedRanges, idx, consumedRanges, idx+1, totalRanges-idx);
		}		
		totalRanges++;
		return consumedRanges[idx]=newReservation;
	}
	

}

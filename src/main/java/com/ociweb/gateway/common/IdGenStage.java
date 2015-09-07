package com.ociweb.gateway.common;

import static com.ociweb.pronghorn.pipe.Pipe.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.gateway.demo.ClockApp;
import com.ociweb.pronghorn.pipe.Pipe;
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
	
	public static final int MAX_BLOCK_SIZE = 4096;
	private static final int MAX_CONSUMED_BLOCKS = 128;//128*4 == 512 bytes where 8k could do map, must be <= 1024
	private static final int MAX_CONSUMED_BLOCKS_LIMIT = MAX_CONSUMED_BLOCKS-1;
	private static final int STOP_CODE = 0xFFFFFFFF;
	
	private int[] consumedRanges;
	private int totalRanges = 0;
	
	private final Pipe[] inputs;
	private final Pipe[] outputs;	
	private final int sizeOfFragment;
	private static final int theOneMsg = 0;// there is only 1 message supported by this stage
		
	public IdGenStage(GraphManager graphManager, Pipe input, Pipe output, String rate) {
		super(graphManager, input, output);
		this.inputs = new Pipe[]{input};
		this.outputs = new Pipe[]{output};
		assert(Pipe.from(input).equals(Pipe.from(output))) : "Both must have same message types ";	
		this.sizeOfFragment = Pipe.from(input).fragDataSize[theOneMsg];
		
		GraphManager.addAnnotation(graphManager, GraphManager.SCHEDULE_RATE, rate, this);
		
		//must be set so this stage will get shut down and ignore the fact that is has un-consumed messages coming in 
        GraphManager.addAnnotation(graphManager,GraphManager.PRODUCER, GraphManager.PRODUCER, this);
	}
	
	public IdGenStage(GraphManager graphManager, Pipe[] inputs, Pipe[] outputs) {
		super(graphManager, inputs, outputs);
		this.inputs = inputs;
		this.outputs = outputs;
		//TODO: add assert to confirm that all the inputs and outputs have the same eq from.
		this.sizeOfFragment = Pipe.from(inputs[0]).fragDataSize[theOneMsg];
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
			Pipe inputRing = inputs[i];
			while (contentToLowLevelRead(inputRing, sizeOfFragment) && 
				   totalRanges<MAX_CONSUMED_BLOCKS_LIMIT) // only release blocks if we have room, just in case one splits, back-pressure 
			{
				int msgIdx = Pipe.takeMsgIdx(inputRing);
				assert(theOneMsg == msgIdx);
				
				releaseRange(Pipe.takeValue(inputRing));
				
				//TODO: do we send the reset request here or earler or later?
				
				Pipe.releaseReads(inputRing);
				Pipe.confirmLowLevelRead(inputRing, sizeOfFragment);
			}
		}
		
		//push out all the new ranges for use as long as we have values and the queues have room
		int j = outputs.length;
		while (--j>=0) {
			Pipe outputRing = outputs[j];
			while (roomToLowLevelWrite(outputRing, sizeOfFragment) ) {
			    				
				int range = reserveNewRange();
				if (STOP_CODE == range || 0==range) {
					return;//no more values to give out
				} 
				
				addMsgIdx(outputRing, theOneMsg);	
				Pipe.addIntValue(range, outputRing);
				publishWrites(outputRing);	
				Pipe.confirmLowLevelWrite(outputRing, sizeOfFragment);
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
	
	public void releaseRange(final int range) {
		
		int insertAt = findIndex(consumedRanges, 0, totalRanges, range);		
				
		if (range != consumedRanges[insertAt]) {
			int releaseEnd = 0xFFFF&(range>>16);
			int releaseBegin = 0xFFFF&range;
			if (releaseBegin>=releaseEnd) {
				//this is an invalid message and treated as a no-op
				//this must always be done to support generative testing
				//TODO: B, if desired this can be logged
				log.debug("skipped release for {}",rangeToString(range));
				return;
			}
			
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
						//subtract one from release end because that value is exclusive of release
						consumedRanges[insertAt] = (0xFFFF&(releaseEnd-1)) | (lastRangeEnd<<16);
						totalRanges++;
						assert(validateInternalTable()) : "Added at "+insertAt + " from release "+rangeToString(range);
						return; // do not continue

					} else {
						consumedRanges[insertAt-1] = (0xFFFF&consumedRanges[insertAt-1]) | ((0xFFFF&releaseBegin)<<16);
						//end is >= to last end so this is a tail trim to the existing row							
						if (releaseEnd == lastRangeEnd) {
							assert(validateInternalTable());
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
				//assert(releaseEnd>(consumedRanges[lastRow]&0xFFFF)); //TODO: B, do we need this assert?
				int lastEnd = (0xFFFF&(consumedRanges[lastRow]>>16));
				if (releaseEnd<lastEnd) {
					//this is a partial row so modify rather than clear
					consumedRanges[lastRow] = (0xFFFF&releaseEnd) | (lastEnd<<16);
					rows--;//this row is done
				} else {
					//releaseEnd is > this row end
					//confirm its < next row begin
					assert(lastRow+1>=totalRanges || releaseEnd<=(consumedRanges[lastRow+1]&0xFFFF));
										
				}
			}
			
			//all full rows are deleted
			if (rows>0) {
				assert(isInRange(insertAt, rows, releaseBegin, releaseEnd));
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
		assert(validateInternalTable());
	}
 	
	
	
	private boolean isInRange(int insertAt, int rows, int releaseBegin, int releaseEnd) {
		int i = rows;
		while (--i>=0) {
			int range = consumedRanges[insertAt+i];
			int begin = range&0xFFFF;
			int end = (range>>16)&0xFFFF;
			if (begin<releaseBegin || end<releaseBegin) {
				return false;
			}
			if (end>releaseEnd || begin> releaseEnd) {
				return false;
			}
		}
		return true;
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
		if (0==max) {//return stop code
			return STOP_CODE;//try again after some ranges are released
		}		
		
		//we now know where the largest range is and where it starts.		
		return reserveRange(maxStart, idx, Math.min(max, MAX_BLOCK_SIZE));
	}

	private void dumpInternalTable() {
		int j = totalRanges;
		while (--j>=0) {
			System.err.println(j+" reserved "+rangeToString(consumedRanges[j]));
		}
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
				
		assert(validateInternalTable());
		return newReservation;
		
	}

	private boolean validateInternalTable() {
		int last = 65536;
		int j = totalRanges;
		while (--j>=0) {
			int start = 0xFFFF&consumedRanges[j];
			if (start>last) {
				dumpInternalTable();				
				return false;
			} else {
				last = start;
			}			
		}
		return true;
	}	

}

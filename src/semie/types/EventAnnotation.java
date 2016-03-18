package semie.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import semie.util.Indexer;

/**
 * Event annotation gives role annotation for an event span.
 * @author luwei
 * @version 1.0
 */

public class EventAnnotation implements Serializable{

	private static final long serialVersionUID = -1324830761475831207L;
	
	/**
	 * mapping from interval to role
	 */
	private HashMap<Integer, Role> _intv2role;
	
	/**
	 * intervals, sorted
	 */
	private int[][] _sortedIntervals;
	
	public EventAnnotation(){
		this._intv2role = new HashMap<Integer, Role>();
	}
	
	/**
	 * Get all the intervals, unsorted
	 * @return the list of all intervals
	 */
	public ArrayList<Integer> getAllIntervals(){
		ArrayList<Integer> results = new ArrayList<Integer>();
		Iterator<Integer> keys = this._intv2role.keySet().iterator();
		while(keys.hasNext())
			results.add(keys.next());
		return results;
	}
	
	/**
	 * Get the list of sorted intervals
	 * @return sorted intervals
	 */
	public int[][] getSortedIntervals(){
		if(this._sortedIntervals!=null)
			return this._sortedIntervals;
		
		ArrayList<Integer> sortedIntervals = new ArrayList<Integer>();
		Iterator<Integer> intervals = this._intv2role.keySet().iterator();
		while(intervals.hasNext()){
			int interval = intervals.next();
			int index = Collections.binarySearch(sortedIntervals, interval);
			if(index>=0)
				throw new IllegalStateException("Interval "+interval+" appeared multiple times in EventAnnotation?!");
			else 
				sortedIntervals.add(-1-index, interval);
		}
		
		int[][] results = new int[sortedIntervals.size()][];
		int oldEIndex = -1;
		for(int i = 0; i<results.length; i++){
			int interval = sortedIntervals.get(i);
			int[] indices = Indexer.decode(interval);
			//contains sth the current model could not handle...
			if(indices[0] < oldEIndex){
				System.err.println("warning: contains some annotation the current model could not handle...");
				results[i] = new int[0];
			} else {
				results[i] = indices;
				oldEIndex = indices[1];
			}
		}
		this._sortedIntervals = results;
		return results;
	}
	
	/**
	 * Annotate an interval with a role
	 * @param bIndex the start index of the interval
	 * @param eIndex the end index of the interval
	 * @param role the role to be annotated
	 */
	public void annotateInterval(int bIndex, int eIndex, Role role){
		int key = Indexer.encode(bIndex, eIndex);
		this._intv2role.put(key, role);
	}
	
	/**
	 * Get the role for a given interval
	 * @param bIndex the start index of the interval
	 * @param eIndex the end index of the interval
	 * @return the role of the interval
	 */
	public Role getRole(int bIndex, int eIndex){
		int key = Indexer.encode(bIndex, eIndex);
		return this._intv2role.get(key);
	}
	
	/**
	 * Compute the statistics which can be used for computation of the PRF scores
	 * @param prediction the predicted annotation
	 * @return the statistics
	 */
	public double[] computePRFStatistics(EventAnnotation prediction){
		
		HashMap<Integer, Role> gold = this._intv2role;
		HashMap<Integer, Role> pred = prediction._intv2role;
		
		double predicted_predictions = pred.size();
		double expected_predictions = gold.size();
		double correct_predictions = 0;
		
		Iterator<Integer> intervals_gold = gold.keySet().iterator();
		while(intervals_gold.hasNext()){
			int interval_gold = intervals_gold.next();
			Role arg_gold = gold.get(interval_gold);
			if(pred.containsKey(interval_gold))
				if(arg_gold.equals(pred.get(interval_gold)))
					correct_predictions ++;
		}
		
		return new double[]{correct_predictions, expected_predictions, predicted_predictions};
	}
	
	/**
	 * View the event annotation with its corresponding event span
	 * @param span the event span
	 */
	public void viewIt(EventSpan span){
		StringBuilder sb = new StringBuilder();
		int[][] intervals = this.getSortedIntervals();
		
		for(int i = 0; i<intervals.length; i++){
			int[] interval = intervals[i];
			if(interval.length==0)
				continue;
			
			int bIndex = interval[0];
			int eIndex = interval[1];
			sb.append(this.getRole(bIndex, eIndex));
			sb.append("\t["+bIndex+","+eIndex+")\t");
			for(int k = bIndex; k<eIndex; k++){
				sb.append(' ');
				sb.append(span.getWords()[k].getWord());
			}
			sb.append('\n');
		}
		
		System.out.println(sb.toString());
	}

	/**
	 * View the event annotation with its corresponding event span
	 * @param span the event span
	 */
	public String viewSpan(EventSpan span){
		StringBuilder sb = new StringBuilder();
		int[][] intervals = this.getSortedIntervals();
		
		for(int i = 0; i<intervals.length; i++){
			int[] interval = intervals[i];
			if(interval.length==0)
				continue;
			
			int bIndex = interval[0];
			int eIndex = interval[1];
			sb.append(this.getRole(bIndex, eIndex));
			sb.append("\t["+bIndex+","+eIndex+")\t");
			for(int k = bIndex; k<eIndex; k++){
				sb.append(' ');
				sb.append(span.getWords()[k].getWord());
			}
			sb.append('\n');
		}
		
		return sb.toString();
	}
	
}

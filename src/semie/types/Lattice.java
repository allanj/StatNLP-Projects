package semie.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import semie.util.Indexer;

/**
 * The lattice is used for specifying the boundaries of the mentions 
 * as well as the relative positions between the mentions.
 * @author luwei
 * @version 1.0
 */

public class Lattice implements Serializable{
	
	private static final long serialVersionUID = 129302727631689514L;
	
	//don't do contrastive estimation.
	public static boolean _contrastiveEstimation = false;
	
	private EventSpan _span;
	private int _spanlength;
	
	private HashMap<Integer, ArrayList<Type>> _intv2types;
	private HashMap<Integer, Phrase> _intv2phrases;
	private HashMap<Integer, ArrayList<Phrase>> _intv2phraess_contrastive;//ids of the additional phrases for contrastive estimation purpose.
	
	private HashMap<Integer, ArrayList<Integer>> _nodeBIndex2EIndex;
	private HashMap<Integer, ArrayList<Integer>> _nodeEIndex2BIndex;
	private HashMap<Integer, ArrayList<Integer>> _pathBIndex2EIndex;
	private HashMap<Integer, ArrayList<Integer>> _pathEIndex2BIndex;
	
	public Lattice(EventSpan span){
		this._span = span;
		this._spanlength = span.length();
		this._intv2types = new HashMap<Integer, ArrayList<Type>>();
		this._intv2phraess_contrastive = new HashMap<Integer, ArrayList<Phrase>>();
		this._nodeBIndex2EIndex = new HashMap<Integer, ArrayList<Integer>>();
		this._pathBIndex2EIndex = new HashMap<Integer, ArrayList<Integer>>();
		this._nodeEIndex2BIndex = new HashMap<Integer, ArrayList<Integer>>();
		this._pathEIndex2BIndex = new HashMap<Integer, ArrayList<Integer>>();
	}

	/**
	 * Count the number of intervals
	 * @return the number of intervals
	 */
	public int countIntervals(){
		return this._intv2types.size()-2;
	}
	
	/**
	 * Get all the intervals
	 * @return the list of all intervals
	 */
	public ArrayList<Integer> getAllIntervals(){
		ArrayList<Integer> results = new ArrayList<Integer>();
		Iterator<Integer> intervals = this._intv2types.keySet().iterator();
		while(intervals.hasNext())
			results.add(intervals.next());
		return results;
	}
	
	/**
	 * Get all possible start indices of those nodes whose end index is given
	 * @param nodeEIndex the end index of any possible node
	 * @return the list of possible start indices
	 */
	public ArrayList<Integer> getNodeBIndexGivenEIndex(int nodeEIndex){
		return this._nodeEIndex2BIndex.get(nodeEIndex);
	}

	/**
	 * Get all possible start indices of those paths whose end index is given
	 * @param pathEIndex the end index of any possible path
	 * @return the list of possible start indices
	 */
	public ArrayList<Integer> getPathBIndexGivenEIndex(int pathEIndex){
		return this._pathEIndex2BIndex.get(pathEIndex);
	}
	
	/**
	 * Get all possible end indices of those nodes whose start index is given
	 * @param nodeBIndex the start index of any possible node
	 * @return the list of possible end indices
	 */
	public ArrayList<Integer> getNodeEIndexGivenBIndex(int nodeBIndex){
		return this._nodeBIndex2EIndex.get(nodeBIndex);
	}

	/**
	 * Get all possible end indices of those paths whose start index is given
	 * @param pathBIndex the start index of any possible path
	 * @return the list of possible end indices
	 */
	public ArrayList<Integer> getPathEIndexGivenBIndex(int pathBIndex){
		return this._pathBIndex2EIndex.get(pathBIndex);
	}
	
	/**
	 * Label an interval with a type
	 * @param bIndex the start index of the interval
	 * @param eIndex the end index of the interval
	 * @param type the type to be labeled to the interval
	 */
	public void label(int bIndex, int eIndex, Type type){
		
		if(bIndex > this._spanlength)
			throw new IllegalArgumentException("bIndex "+bIndex+" is greater than "+this._spanlength);
		if(bIndex == eIndex)
			throw new IllegalArgumentException("bIndex "+bIndex+" is equal to eIndex "+eIndex);
		
		int key = Indexer.encode(bIndex, eIndex);
		if(!this._intv2types.containsKey(key)){
			this._intv2types.put(key, new ArrayList<Type>());
			
			if(!this._nodeBIndex2EIndex.containsKey(bIndex))
				this._nodeBIndex2EIndex.put(bIndex, new ArrayList<Integer>());
			
			ArrayList<Integer> eIndices = this._nodeBIndex2EIndex.get(bIndex);
			eIndices.add(eIndex);
			
			if(!this._nodeEIndex2BIndex.containsKey(eIndex))
				this._nodeEIndex2BIndex.put(eIndex, new ArrayList<Integer>());
			
			ArrayList<Integer> bIndices = this._nodeEIndex2BIndex.get(eIndex);
			bIndices.add(bIndex);
		}
		
		ArrayList<Type> types = this._intv2types.get(key);
		if(!types.contains(type))
			types.add(type);
		
	}
	
	/**
	 * Performs post-processing
	 * Note: this method needs to be called after all 'label' calls are done.
	 */
	public void label_postprocess(){
		
		//for BEGIN
		this.label(-1, 0, Type._BEGIN_TYPE);
		//for END
		this.label(this._spanlength, this._spanlength+1, Type._END_TYPE);
		
		Iterator<Integer> bIndices;
		
		ArrayList<Integer> all_bIndices = new ArrayList<Integer>();
		
		bIndices = this._nodeBIndex2EIndex.keySet().iterator();
		while(bIndices.hasNext()){
			int bIndex = bIndices.next();
			//note: inefficient implementation... 
			if(!all_bIndices.contains(bIndex))
				all_bIndices.add(bIndex);
		}
		Collections.sort(all_bIndices);
		
		for(int k =0; k<all_bIndices.size(); k++){
			int bIndex = all_bIndices.get(k);
			if(bIndex> this._spanlength)
				throw new IllegalStateException("Error:"+bIndex+">"+this._spanlength);
		}
		
		ArrayList<Integer> all_eIndices = new ArrayList<Integer>();
		
		bIndices = this._nodeBIndex2EIndex.keySet().iterator();
		while(bIndices.hasNext()){
			int bIndex = bIndices.next();
			ArrayList<Integer> eIndices = this._nodeBIndex2EIndex.get(bIndex);
			for(int i = 0; i<eIndices.size(); i++){
				int eIndex = eIndices.get(i);
				//note: inefficient implementation...
				if(!all_eIndices.contains(eIndex))
					all_eIndices.add(eIndex);
			}
		}
		Collections.sort(all_eIndices);
		
		//note: inefficient implementation...
		for(int i = 0; i<all_eIndices.size(); i++){
			int eIndex = all_eIndices.get(i);
			for(int j = 0; j<all_bIndices.size(); j++){
				int bIndex = all_bIndices.get(j);
				if(bIndex >= eIndex){
					ArrayList<Integer> bs = new ArrayList<Integer>();
					for(int k = j; k<all_bIndices.size(); k++)
						bs.add(all_bIndices.get(k));
					this._pathBIndex2EIndex.put(eIndex, bs);
					break;
				}
			}
		}
		
		for(int i = all_bIndices.size()-1; i>=0; i--){
			int bIndex = all_bIndices.get(i);
			for(int j = all_eIndices.size()-1; j>=0; j--){
				int eIndex = all_eIndices.get(j);
				if(eIndex <= bIndex){
					ArrayList<Integer> bs = new ArrayList<Integer>();
					for(int k = j; k>=0; k--)
						bs.add(all_eIndices.get(k));
					this._pathEIndex2BIndex.put(bIndex, bs);
					break;
				}
			}
		}
	}
	
	/**
	 * Map the phrases to intervals
	 * @param manager the manager
	 */
	public void map_phrases(Manager manager){
		
		this._intv2phrases = new HashMap<Integer, Phrase>();
		this._intv2phraess_contrastive = new HashMap<Integer, ArrayList<Phrase>>();
		
		ArrayList<Phrase> phrases = new ArrayList<Phrase>();
		
		Iterator<Integer> nodeBIndices;
		
		//below for original lattice...
		nodeBIndices = this._nodeBIndex2EIndex.keySet().iterator();
		while(nodeBIndices.hasNext()){
			int bIndex = nodeBIndices.next();
			ArrayList<Integer> nodeEIndices = this._nodeBIndex2EIndex.get(bIndex);
			for(int i = 0; i < nodeEIndices.size(); i++){
				int eIndex = nodeEIndices.get(i);
				Phrase phrase = manager.toPhrase(this._span, bIndex, eIndex);
				int interval = Indexer.encode(bIndex, eIndex);
				this._intv2phrases.put(interval, phrase);
				if(!phrase.equals(Phrase._BEG_PHRASE) && !phrase.equals(Phrase._END_PHRASE))
					phrases.add(phrase);
			}
		}
		
		//below for contrastive estimation...
		if(_contrastiveEstimation){

			nodeBIndices = this._nodeBIndex2EIndex.keySet().iterator();
			while(nodeBIndices.hasNext()){
				int bIndex = nodeBIndices.next();
				ArrayList<Integer> nodeEIndices = this._nodeBIndex2EIndex.get(bIndex);
				for(int i = 0; i < nodeEIndices.size(); i++){
					int eIndex = nodeEIndices.get(i);
					if(bIndex == -1 && eIndex == 0){
						int interval = Indexer.encode(bIndex, eIndex);
						if(!this._intv2phraess_contrastive.containsKey(interval))
							this._intv2phraess_contrastive.put(interval, new ArrayList<Phrase>());
						
						ArrayList<Phrase> ids = this._intv2phraess_contrastive.get(interval);
						ids.add(Phrase._BEG_PHRASE);
					} else if(bIndex == this._spanlength && eIndex == this._spanlength + 1){
						int interval = Indexer.encode(bIndex, eIndex);
						if(!this._intv2phraess_contrastive.containsKey(interval))
							this._intv2phraess_contrastive.put(interval, new ArrayList<Phrase>());
						
						ArrayList<Phrase> ids = this._intv2phraess_contrastive.get(interval);
						ids.add(Phrase._END_PHRASE);
					} else {
						for(int k = 0; k<phrases.size(); k++){
							Phrase phrase = phrases.get(k);
							if(phrase.getBIndex() >= eIndex || phrase.getEIndex() <= bIndex || (phrase.getBIndex() == bIndex && phrase.getEIndex() == eIndex)){
								int interval = Indexer.encode(bIndex, eIndex);
								if(!this._intv2phraess_contrastive.containsKey(interval))
									this._intv2phraess_contrastive.put(interval, new ArrayList<Phrase>());
								
								ArrayList<Phrase> ids = this._intv2phraess_contrastive.get(interval);
								ids.add(phrase);
							}
						}
					}
				}
			}
			
		}
			
		
	}
	
	/**
	 * Get the phrase from a given interval
	 * @param bIndex the start index of the interval
	 * @param eIndex the end index of the interval
	 * @return the phrase associated with the interval
	 */
	public Phrase getPhrase(int bIndex, int eIndex){
		if(bIndex == -1 && eIndex == 0)
			return Phrase._BEG_PHRASE;
		if(bIndex == this._spanlength && eIndex == this._spanlength+1)
			return Phrase._END_PHRASE;
		
		int key = Indexer.encode(bIndex, eIndex);
		if(this._intv2phrases.containsKey(key))
			return this._intv2phrases.get(key);
		
		throw new IllegalStateException("Should not call this method!"+bIndex+","+eIndex);
	}
	
	/**
	 * Get the phrases used for contrastive estimation
	 * @param bIndex the start index of the interval
	 * @param eIndex the end index of the interval
	 * @return the list of phrases associated with the interval
	 */
	public ArrayList<Phrase> getPhrases_contrastive(int bIndex, int eIndex){
		
		if(!_contrastiveEstimation)
		{
			ArrayList<Phrase> results = new ArrayList<Phrase>();
			results.add(this.getPhrase(bIndex, eIndex));
			return results;
		}
		
		int key = Indexer.encode(bIndex, eIndex);
		if(this._intv2phraess_contrastive.containsKey(key))
			return this._intv2phraess_contrastive.get(key);
		
		throw new IllegalStateException("The lattice for contrastive estimation is not constructed.");
	}
	
	/**
	 * Get the types labeled for an interval
	 * @param bIndex the start index of the interval
	 * @param eIndex the end index of the interval
	 * @return the list of types associated with the interval
	 */
	public ArrayList<Type> getTypes(int bIndex, int eIndex){
		int key = Indexer.encode(bIndex, eIndex);
		if(this._intv2types.containsKey(key))
			return this._intv2types.get(key);
		return null;//if the interval does not correspond to any type
	}
	
}

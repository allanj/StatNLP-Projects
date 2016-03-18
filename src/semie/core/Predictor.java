package semie.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import semie.pref.PreferenceManager;
import semie.types.FeatureManager;
import semie.types.Phrase;
import semie.types.Role;
import semie.types.Event;
import semie.types.EventSpan;
import semie.types.Lattice;
import semie.types.Manager;
import semie.types.RoleSpan;
import semie.types.RolePair;
import semie.types.Type;

/**
 * predictor, predicts the top-k outputs.
 * @author luwei
 * @version 1.0
 */

public class Predictor {
	
	protected Manager _manager;
	protected FeatureManager _fm;
	protected PreferenceManager _pm;
	protected EventSpan _span;
	protected Lattice _lattice;
	
	protected int _topK;
	protected int _beamsize = 1000;
	
	protected HashMap<Event, HashMap<Long, ArrayList<Role>>> _acceptableArgsMapPerEvent = new HashMap<Event, HashMap<Long, ArrayList<Role>>>();
	protected HashMap<Event, ArrayList<RoleSpan>> _predictions = new HashMap<Event, ArrayList<RoleSpan>>();
	
	protected HashMap<RolePair, ArrayList<RoleSpan>> _remainingSpansMap = new HashMap<RolePair, ArrayList<RoleSpan>>();
	
	protected boolean _constrained = false;
	
	public Predictor(EventSpan span, Manager manager, FeatureManager fm, PreferenceManager pm, int topK, boolean isConstrained){
		
		this._span = span;
		this._lattice = span.getLattice();
		this._manager = manager;
		this._fm = fm;
		this._pm = pm;
		this._topK = topK;
		if(this._topK == -1)
			this._topK = Integer.MAX_VALUE;
		this._constrained = isConstrained;
		if(this._pm == null || this._pm.getNumPreferences()==0)
			this._constrained = false;
		
	}
	
	public void decode(Event event, boolean considerPreferences){
		
		//decode from the left to the right...
		ArrayList<RolePair> remainingSpansList = new ArrayList<RolePair>();
		
		RoleSpan beginSpan = new RoleSpan(-1, 0, this._manager.toBeginRole(event), 1.0, new ArrayList<ArrayList<Integer>>());
		
		this.decode(event, beginSpan, beginSpan, remainingSpansList, considerPreferences);
		
		while(!remainingSpansList.isEmpty()){
			RolePair key = remainingSpansList.get(0);
			remainingSpansList.remove(0);
			ArrayList<RoleSpan> spans = this._remainingSpansMap.get(key);
			for(int i = 0; i<spans.size(); i++){
				RoleSpan curr = spans.get(i);
				RoleSpan prev = curr.getPrev();
				this.decode(event, prev, curr, remainingSpansList, considerPreferences);
			}
		}
		
	}
	
	protected void decode(Event event, RoleSpan span_A, RoleSpan span_B, ArrayList<RolePair> remainingSpansList, boolean considerPreferences){

		Role A = span_A.getRole();
		Role B = span_B.getRole();
		
		int bIndex = span_B.getEIndex();
		ArrayList<Integer> cIndices = this._lattice.getPathEIndexGivenBIndex(bIndex);
		
		double prevScore = span_B.getScore();
		
		for(int cIndex : cIndices){
			ArrayList<Integer> eIndices = this._lattice.getNodeEIndexGivenBIndex(cIndex);
			for(int eIndex : eIndices){
				ArrayList<Role> args = this.getAcceptableArguments(event, cIndex, eIndex);
				for(Role C : args){
//					System.err.println("+a");
					// A [..] B [bIndex .. cIndex) C [eIndex ..
					
					ArrayList<ArrayList<Integer>> features = new ArrayList<ArrayList<Integer>>();
					
					double subScore = 0;
					ArrayList<Type> types = this._lattice.getTypes(cIndex, eIndex);
					Phrase phrase = this._lattice.getPhrase(cIndex, eIndex);
					Type[] acceptableTypes = C.getTypes();
					for(Type type : types){
						for(Type type2 : acceptableTypes){
							if(type.equals(type2)){
								subScore += this.computeNodeScore(C, type, phrase, this._span, cIndex, eIndex, features, considerPreferences);
							}
						}
					}
					
					//quickly stop if the score is already zero.
					if(subScore <= 0){
						continue;
					}
					
					Phrase phrase_path = this._manager.toPhrase(this._span, bIndex, cIndex); //this._lattice.getPhrase(bIndex, cIndex);
					
					double currScore = this.computePathScore(new Role[]{A, B, C}, phrase_path, features, considerPreferences) * subScore;
					double score = prevScore * currScore;
					
					//quickly stop if the score is already zero.
					if(score <= 0){
						continue;
					}
					
					RoleSpan span_C = new RoleSpan(cIndex, eIndex, C, score, features);
					span_C.setPrev(span_B);
					
					if(!C.equals(this._manager.toEndRole(event))){
//						System.err.println("+b");
						
						RolePair tuple = new RolePair(span_B.getRole(), span_C.getRole(), span_C.getEIndex());
						
						if(!this._remainingSpansMap.containsKey(tuple)){
							this._remainingSpansMap.put(tuple, new ArrayList<RoleSpan>());
						}
						ArrayList<RoleSpan> spans = this._remainingSpansMap.get(tuple);
						int index = Collections.binarySearch(spans, span_C);
						if(index < 0){
							index = -1-index;
						}
						spans.add(index, span_C);
						if(this._constrained){
//							System.err.println("here.");
							if(spans.size()> this._topK*this._beamsize){
								spans.remove(0);
							}
						} else {
							if(spans.size()> this._topK){
								spans.remove(0);
							}
						}
						int index1 = Collections.binarySearch(remainingSpansList, tuple);
						if(index1 < 0){
							remainingSpansList.add(-1-index1, tuple);
						}
					}
					
					if(C.equals(this._manager.toEndRole(event))){
						this.putPrediction(event, span_C);
					}
				}
			}
		}
		
	}
	
	protected boolean putPrediction(Event event, RoleSpan span){
		
		if(!this._predictions.containsKey(event))
			this._predictions.put(event, new ArrayList<RoleSpan>());
		
		ArrayList<RoleSpan> oldSpans = this._predictions.get(event);
		int index = Collections.binarySearch(oldSpans, span);
		if(index<0)
			index = -1 - index;
		
		oldSpans.add(index, span);
		if(oldSpans.size() > this._topK*this._beamsize)
			oldSpans.remove(0);
		
		return true;
	}
	
	public RoleSpan getSingleBestPreferredPrediction(Event event){
		
		ArrayList<RoleSpan> rolespans = this._predictions.get(event);
		double prefscore_max = Double.NEGATIVE_INFINITY;
		RoleSpan rolespan_max = null;
		for(int i = 0; i<rolespans.size(); i++){
			RoleSpan rolespan = rolespans.get(i);
			double prefscore = this._pm.computePreferenceScore(rolespan, this._span);
			if(prefscore > prefscore_max){
				prefscore_max = prefscore;
				rolespan_max = rolespan;
			}
		}
		
		return rolespan_max;
	}
	
	public ArrayList<RoleSpan> getPredictions(Event event, int topK){

		ArrayList<RoleSpan> results = new ArrayList<RoleSpan>();
		ArrayList<PreferredRoleSpan> pspans = new ArrayList<PreferredRoleSpan>();
		
		ArrayList<RoleSpan> rolespans = this._predictions.get(event);
//		System.err.println("x:"+rolespans.size());
		for(int i = 0; i<rolespans.size(); i++){
			RoleSpan rolespan = rolespans.get(i);
//			double prefscore = this._pm.computePreferenceScore(rolespan, this._eventspan);
			double prefscore = rolespan.getScore();
			PreferredRoleSpan pspan = new PreferredRoleSpan(rolespan, prefscore);
			pspans.add(pspan);
		}
		Collections.sort(pspans);
		for(int i = pspans.size()-1; i>= Math.max(pspans.size()-topK, 0); i--){
//			System.err.println("score="+pspans.get(i).debug_getPrefScore()+"\t"+this._manager.viewRoleSpan(pspans.get(i).getRoleSpan(), this._eventspan));
			results.add(pspans.get(i).getRoleSpan());
		}
//		System.exit(1);
		
		return results;
	}
	
	public ArrayList<RoleSpan> getPreferredPredictions(Event event, int topK){

		ArrayList<RoleSpan> results = new ArrayList<RoleSpan>();
		ArrayList<PreferredRoleSpan> pspans = new ArrayList<PreferredRoleSpan>();
		
		ArrayList<RoleSpan> rolespans = this._predictions.get(event);
//		System.err.println("x:"+rolespans.size());
		for(int i = 0; i<rolespans.size(); i++){
			RoleSpan rolespan = rolespans.get(i);
			double prefscore = this._pm.computePreferenceScore(rolespan, this._span);
			PreferredRoleSpan pspan = new PreferredRoleSpan(rolespan, prefscore);
			pspans.add(pspan);
		}
		Collections.sort(pspans);
		for(int i = pspans.size()-1; i>= Math.max(pspans.size()-topK, 0); i--){
//			System.err.println("score="+pspans.get(i).debug_getPrefScore()+"\t"+this._manager.viewRoleSpan(pspans.get(i).getRoleSpan(), this._eventspan));
			results.add(pspans.get(i).getRoleSpan());
		}
//		System.exit(1);
		
		return results;
	}
	
	protected class PreferredRoleSpan implements Comparable<PreferredRoleSpan>{
		protected RoleSpan _rolespan;
		protected double _prefScore;
		public PreferredRoleSpan(RoleSpan rolespan, double prefScore){
			this._rolespan = rolespan;
			this._prefScore = prefScore;
		}
		public RoleSpan getRoleSpan(){
			return this._rolespan;
		}
		public int compareTo(PreferredRoleSpan span){
			return this._prefScore > span._prefScore ? +1 : this._prefScore < span._prefScore ? -1 : this._rolespan.compareTo(span._rolespan);
		}
	}
	
	public ArrayList<RoleSpan> getPredictions(Event event){
		ArrayList<RoleSpan> spans = this._predictions.get(event);
		while(spans.size()>this._topK){
			spans.remove(0);
		}
		return spans;
	}
	
	public ArrayList<Role> getAcceptableArguments(Event event, int bIndex, int eIndex){
		long key = bIndex*1000+eIndex;
		
		if(!this._acceptableArgsMapPerEvent.containsKey(event)){
			this._acceptableArgsMapPerEvent.put(event, new HashMap<Long, ArrayList<Role>>());
		}
		
		if(this._acceptableArgsMapPerEvent.get(event).containsKey(key)){
			return this._acceptableArgsMapPerEvent.get(event).get(key);
		}
		
		ArrayList<Role> results = new ArrayList<Role>();
		
		if(bIndex == -1 && eIndex ==0){
			results.add(this._manager.toBeginRole(event));
		} else if(bIndex == this._span.getWords().length && eIndex == this._span.getWords().length+1){
			results.add(this._manager.toEndRole(event));
		} else {
			ArrayList<Type> types = this._lattice.getTypes(bIndex, eIndex);
			Role[] arguments = event.getRoles();
			for(Role arg : arguments){
				boolean acceptable = false;
				Type[] acceptableTypes = arg.getTypes();
				for(Type type: types){
					for(Type acceptableType : acceptableTypes){
						if(acceptableType.equals(type)){
							acceptable = true;
							break;
						}
					}
					if(acceptable){
						break;
					}
				}
				if(acceptable){
					results.add(arg);
				}
			}
		}
		
		this._acceptableArgsMapPerEvent.get(event).put(key, results);
		
		return results;
	}
	
	protected double computeNodeScore(Role A, Type type, Phrase phrase, EventSpan eventspanId, int bIndex, int eIndex, ArrayList<ArrayList<Integer>> allfeatures, boolean considerPreferences){
		ArrayList<Integer> features = this._fm.extractFeatures(A, type, phrase, eventspanId, bIndex, eIndex, false);
		allfeatures.add(features);
		return this._fm.computeScore(features, considerPreferences);
	}
	
	protected double computePathScore(Role[] args, Phrase phrase, ArrayList<ArrayList<Integer>> allfeatures, boolean considerPreferences){
		ArrayList<Integer> features = this._fm.extractFeatures(args, phrase, false);
		allfeatures.add(features);
		return this._fm.computeScore(features, considerPreferences);
	}
	
}

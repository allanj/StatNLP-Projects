package semie.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import semie.types.FeatureManager;
import semie.types.Phrase;
import semie.types.Role;
import semie.types.Event;
import semie.types.EventSpan;
import semie.types.Lattice;
import semie.types.Manager;
import semie.types.RolePair;
import semie.types.Type;
import semie.util.Indexer;

/**
 * unconstrained learner
 * @author luwei
 * @version 1.0
 */

public class UnconstrainedLearner extends Learner{
	
	protected Lattice _lattice;
	
	protected HashMap<Event, Double> _beta_L = new HashMap<Event, Double>();
	protected HashMap<Event, Double> _beta_G = new HashMap<Event, Double>();
	protected HashMap<Event, Double> _alpha_L = new HashMap<Event, Double>();
	protected HashMap<Event, Double> _alpha_G = new HashMap<Event, Double>();
	
	private HashMap<Event, HashMap<RolePair, Double>> _insideprob_L;
	private HashMap<Event, HashMap<RolePair, Double>> _insideprob_G;
	private HashMap<Event, HashMap<RolePair, Double>> _outsideprob_L;
	private HashMap<Event, HashMap<RolePair, Double>> _outsideprob_G;
	
	private HashMap<Event, HashMap<Integer, ArrayList<Role>>> _acceptableRolesMap = new HashMap<Event, HashMap<Integer, ArrayList<Role>>>();
	
	public UnconstrainedLearner(Manager manager, FeatureManager fm, EventSpan span, Event event, double weight){
		super(manager, fm, span, event, weight);
		this._lattice = span.getLattice();
		this._insideprob_L = new HashMap<Event, HashMap<RolePair, Double>>();
		this._outsideprob_L = new HashMap<Event, HashMap<RolePair, Double>>();
		this._insideprob_G = new HashMap<Event, HashMap<RolePair, Double>>();
		this._outsideprob_G = new HashMap<Event, HashMap<RolePair, Double>>();
		this._fm.resetFeatureExtractorCache();
	}
	
	/**
	 * Performing forward-backward styled algorithms and compute expected counts
	 */
	public double[] computeStatistics(){
		this.computeL();
		this.computeG();
		return new double[]{this._beta_L.get(this._event), this._beta_G.get(this._event)};
	}
	
	/**
	 * Computation for the numerator
	 */
	protected void computeL(){

		boolean contrastive = false;
		boolean considerPreferences = true;
		
		this.computeForward(this._event, contrastive, considerPreferences);
		this.computeBackward(this._event, contrastive, considerPreferences);
		
		//debugging purpose
		if(Math.abs(this._beta_L.get(this._event) - this._alpha_L.get(this._event))/this._alpha_L.get(this._event)>1e-12){
			System.err.println("Error:");
			System.err.println("beta : "+this._beta_L.get(this._event));
			System.err.println("alfa : "+this._alpha_L.get(this._event));
			System.err.println(Manager.viewEventSpan(_span));
//			this._lattice.visualize(_manager, _span);
			System.exit(1);
		}
		
		this.updateParam(this._event, this._beta_L.get(this._event), contrastive, considerPreferences);
		
	}
	
	/**
	 * Computation for the denominator
	 */
	protected void computeG(){
		
		boolean contrastive = true;
		boolean considerPreferences = false;
		
		this.computeForward(this._event, contrastive, considerPreferences);
		this.computeBackward(this._event, contrastive, considerPreferences);
		
		//debugging purpose
		if(Math.abs(this._beta_G.get(this._event) - this._alpha_G.get(this._event))/this._alpha_G.get(this._event)>1e-12){
			System.err.println("Error:");
			System.err.println(this._beta_G.get(this._event));
			System.err.println(this._alpha_G.get(this._event));
			System.err.println(Manager.viewEventSpan(_span));
//			this._lattice.visualize(_manager, _span);
			System.exit(1);
		}
		
		this.updateParam(this._event, this._beta_G.get(this._event), contrastive, considerPreferences);
		
	}

	private void computeForward(Event event, boolean contrastive, boolean considerPreferences){
		
		ArrayList<RolePair> tuples = new ArrayList<RolePair>();
		
		this.computeForward_Node(event, this._manager.toBeginRole(event), this._manager.toBeginRole(event), 0, tuples, contrastive, considerPreferences);
		
		while(!tuples.isEmpty()){
			RolePair tuple = tuples.remove(0);
			this.computeForward_Node(event, tuple.first(), tuple.second(), tuple.value(), tuples, contrastive, considerPreferences);
		}
		
	}
	
	private void computeForward_Node(Event event, Role A, Role B, int bIndex, ArrayList<RolePair> tuples, boolean contrastive, boolean considerPreferences){
		
		ArrayList<Integer> cIndices = this._lattice.getPathEIndexGivenBIndex(bIndex);
		
		for(int cIndex : cIndices){
			ArrayList<Integer> eIndices = this._lattice.getNodeEIndexGivenBIndex(cIndex);
			for(int eIndex : eIndices){
				ArrayList<Role> roles = this.getAcceptableRoles(event, cIndex, eIndex);
				for(Role C : roles){
					this.computeForward_Path(event, A, B, C, bIndex, cIndex, eIndex, contrastive, considerPreferences);
					
					if(!C.equals(this._manager.toEndRole(event))){
						RolePair tuple = new RolePair(B, C, eIndex);
						int idx = Collections.binarySearch(tuples, tuple);
						if(idx < 0)
							tuples.add(-1 - idx, tuple);
					}
				}
			}
		}
		
	}
	
	private void computeForward_Path(Event event, Role A, Role B, Role C, int bIndex, int cIndex, int eIndex, boolean isDenom, boolean considerPreferences){

		double prevScore = this.getInsideScore(event, A, B, bIndex, isDenom);
		
		double subScore = 0;
		
		if(isDenom){
			ArrayList<Phrase> phrases = this._lattice.getPhrases_contrastive(cIndex, eIndex);
			
			ArrayList<Type> types = this._lattice.getTypes(cIndex, eIndex);
			Type[] acceptableTypes = C.getTypes();
			for(Type type : types)
				for(Type type2 : acceptableTypes)
					if(type == type2)
						for(Phrase phrase : phrases)
							subScore += this.computeNodeScore(C, type, phrase, cIndex, eIndex, considerPreferences);
		} else {
			Phrase phrase = this._lattice.getPhrase(cIndex, eIndex);
			
			ArrayList<Type> types = this._lattice.getTypes(cIndex, eIndex);
			Type[] acceptableTypes = C.getTypes();
			for(Type type : types)
				for(Type type2 : acceptableTypes)
					if(type == type2)
						subScore += this.computeNodeScore(C, type, phrase, cIndex, eIndex, considerPreferences);
		}
		
		Phrase phrase_path = this._manager.toPhrase(this._span, bIndex, cIndex);
		
		double currScore = this.computePathScore(new Role[]{A, B, C}, phrase_path, considerPreferences) * subScore;
		double score = prevScore * currScore;
		this.putInsideScore(event, B, C, eIndex, score, isDenom);
		if(C == this._manager.toEndRole(event))
			this.putInsideScore(event, score, isDenom);
	}
	
	private void computeBackward(Event event, boolean isDenom, boolean considerPreferences){
		
		//computing the outside probabilities
		ArrayList<RolePair> tuples = new ArrayList<RolePair>();
		
		Role[] allargs = event.getRoles();
		for(Role arg : allargs){
			Role B = arg;
			
			if(this.getInsideScore(event, B, this._manager.toEndRole(event), this._span.getWords().length+1, isDenom)>0)
				this.computeBackward_Node(event, B, this._manager.toEndRole(event), this._span.getWords().length+1, tuples, isDenom, considerPreferences);
		}
		//consider the head...
		{
			Role B = this._manager.toBeginRole(event);
			if(this.getInsideScore(event, B, this._manager.toEndRole(event), this._span.getWords().length+1, isDenom)>0)
				this.computeBackward_Node(event, B, this._manager.toEndRole(event), this._span.getWords().length+1, tuples, isDenom, considerPreferences);
		}
		
		while(!tuples.isEmpty()){
			RolePair tuple = tuples.remove(tuples.size()-1);
			this.computeBackward_Node(event, tuple.first(), tuple.second(), tuple.value(), tuples, isDenom, considerPreferences);
		}
		
	}
	
	private void computeBackward_Node(Event event, Role B, Role C, int eIndex, ArrayList<RolePair> tuples, boolean contrastive, boolean considerPreferences){

		Role[] allargs = event.getRoles();
		ArrayList<Integer> cIndices = this._lattice.getNodeBIndexGivenEIndex(eIndex);
		
		for(int cIndex : cIndices){
			ArrayList<Role> args = this.getAcceptableRoles(event, cIndex, eIndex);
			if(args.contains(C)){
				//it's valid.
				ArrayList<Integer> bIndices = this._lattice.getPathBIndexGivenEIndex(cIndex);
				for(int bIndex : bIndices){
					for(Role A : allargs){
						if(this.getInsideScore(event, A, B, bIndex, contrastive)>0){
							this.computeBackward_Path(event, A, B, C, bIndex, cIndex, eIndex, contrastive, considerPreferences);
							if(!B.equals(this._manager.toBeginRole(event))){
								RolePair tuple = new RolePair(A, B, bIndex);
								int idx = Collections.binarySearch(tuples, tuple);
								if(idx < 0)
									tuples.add(- 1 - idx, tuple);
							}
						}
					}
					//consider the head.
					{
						Role A = this._manager.toBeginRole(event);
						if(this.getInsideScore(event, A, B, bIndex, contrastive)>0){
							this.computeBackward_Path(event, A, B, C, bIndex, cIndex, eIndex, contrastive, considerPreferences);
							if(!B.equals(this._manager.toBeginRole(event))){
								RolePair tuple = new RolePair(A, B, bIndex);
								int idx = Collections.binarySearch(tuples, tuple);
								if(idx < 0)
									tuples.add(- 1 - idx, tuple);
							}
						}
					}
				}
			}
		}
		
	}
	
	private void computeBackward_Path(Event event, Role A, Role B, Role C, int bIndex, int cIndex, int eIndex, boolean isDenom, boolean considerPreferences){
		
		double nextScore = this.getOutsideScore(event, B, C, eIndex, isDenom);
		
		double subScore = 0;
		
		if(isDenom){
			ArrayList<Phrase> phrases = this._lattice.getPhrases_contrastive(cIndex, eIndex);
			
			ArrayList<Type> types = this._lattice.getTypes(cIndex, eIndex);
			Type[] acceptableTypes = C.getTypes();
			for(Type type : types)
				for(Type type2 : acceptableTypes)
					if(type == type2)
						for(Phrase phrase: phrases)
							subScore += this.computeNodeScore(C, type, phrase, cIndex, eIndex, considerPreferences);
		} else {
			Phrase phrase = this._lattice.getPhrase(cIndex, eIndex);
			
			ArrayList<Type> types = this._lattice.getTypes(cIndex, eIndex);
			Type[] acceptableTypes = C.getTypes();
			for(Type type : types)
				for(Type type2 : acceptableTypes)
					if(type == type2)
						subScore += this.computeNodeScore(C, type, phrase, cIndex, eIndex, considerPreferences);
		}
		
		Phrase phrase_path = this._manager.toPhrase(this._span, bIndex, cIndex);
		
		double currScore = this.computePathScore(new Role[]{A, B, C}, phrase_path, considerPreferences) * subScore;
		double score = nextScore * currScore;
		
		this.putOutsideScore(event, A, B, bIndex, score, isDenom);
		if(bIndex==0 && A == this._manager.toBeginRole(event))
			this.putOutsideScore(event, score, isDenom);
		
	}
	
	private void updateParam(Event event, double norm, boolean isDenom, boolean considerPreferences){
		
		ArrayList<RolePair> tuples = new ArrayList<RolePair>();
		
		Role[] allargs = event.getRoles();
		for(Role B : allargs)
			if(this.getInsideScore(event, B, this._manager.toEndRole(event), this._span.getWords().length+1, isDenom)>0)
				this.updateParam_Node(event, B, this._manager.toEndRole(event), this._span.getWords().length+1, tuples, norm, isDenom, considerPreferences);
		//consider the head...
		{
			Role B = this._manager.toBeginRole(event);
			if(this.getInsideScore(event, B, this._manager.toEndRole(event), this._span.getWords().length+1, isDenom)>0)
				this.updateParam_Node(event, B, this._manager.toEndRole(event), this._span.getWords().length+1, tuples, norm, isDenom, considerPreferences);
		}
		
		while(!tuples.isEmpty()){
			RolePair tuple = tuples.remove(tuples.size()-1);
			this.updateParam_Node(event, tuple.first(), tuple.second(), tuple.value(), tuples, norm, isDenom, considerPreferences);
		}
	}
	
	private void updateParam_Node(Event event, Role B, Role C, int eIndex, ArrayList<RolePair> tuples, double norm, boolean isDenom, boolean considerPreferences){

		Role[] allargs = event.getRoles();
		ArrayList<Integer> cIndices = this._lattice.getNodeBIndexGivenEIndex(eIndex);
		
		for(int cIndex : cIndices){
			ArrayList<Role> args = this.getAcceptableRoles(event, cIndex, eIndex);
			if(args.contains(C)){
				//it's valid.
				ArrayList<Integer> bIndices = this._lattice.getPathBIndexGivenEIndex(cIndex);
				for(int bIndex : bIndices){
					for(Role A : allargs){
						if(this.getInsideScore(event, A, B, bIndex, isDenom)>0){
							this.updateParam_Path(event, A, B, C, bIndex, cIndex, eIndex, norm, isDenom, considerPreferences);
							if(B!=(this._manager.toBeginRole(event))){
								RolePair tuple = new RolePair(A, B, bIndex);
								int idx = Collections.binarySearch(tuples, tuple);
								if(idx < 0)
									tuples.add(- 1 - idx, tuple);
							}
						}
					}
					//consider the head.
					{
						Role A = this._manager.toBeginRole(event);
						if(this.getInsideScore(event, A, B, bIndex, isDenom)>0){
							this.updateParam_Path(event, A, B, C, bIndex, cIndex, eIndex, norm, isDenom, considerPreferences);
							if(B!=(this._manager.toBeginRole(event))){
								RolePair tuple = new RolePair(A, B, bIndex);
								int idx = Collections.binarySearch(tuples, tuple);
								if(idx < 0)
									tuples.add(- 1 - idx, tuple);
							}
						}
					}
				}
			}
		}
		
	}
	
	private void updateParam_Path(Event event, Role A, Role B, Role C, int bIndex, int cIndex, int eIndex, double norm, boolean isDenom, boolean considerPreferences){

		// A [..] B [bIndex .. cIndex) C [eIndex ..
		
		double nextScore = this.getOutsideScore(event, B, C, eIndex, isDenom);
		
		Phrase phrase_path = this._manager.toPhrase(this._span, bIndex, cIndex);
		
		double p_prob = this.getInsideScore(event, A, B, bIndex, isDenom)
		* this.computePathScore(new Role[] {A, B, C}, phrase_path, considerPreferences) 
		* nextScore / norm;
		
		double subScore = 0;
		
		ArrayList<Type> types = this._lattice.getTypes(cIndex, eIndex);
		Type[] acceptableTypes = C.getTypes();

		for(Type type : types){
			for(Type type2 : acceptableTypes){
				if(type == type2){
					if(isDenom){
						ArrayList<Phrase> phrases = this._lattice.getPhrases_contrastive(cIndex, eIndex);
						for(Phrase phrase: phrases){
							double val = this.computeNodeScore(C, type, phrase, cIndex, eIndex, considerPreferences);
							subScore += val;
							this.putCount(C, type, phrase, cIndex, eIndex, val * p_prob, isDenom);
						}
					} else {
						Phrase phrase = this._lattice.getPhrase(cIndex, eIndex);
						double val = this.computeNodeScore(C, type, phrase, cIndex, eIndex, considerPreferences);
						subScore += val;
						this.putCount(C, type, phrase, cIndex, eIndex, val * p_prob, isDenom);
					}
				}
			}
		}
		
		double currScore = this.computePathScore(new Role[]{A, B, C}, phrase_path, considerPreferences) * subScore;
		double score = nextScore * currScore;
		this.putCount(new Role[]{A, B, C}, phrase_path, score * this.getInsideScore(event, A, B, bIndex, isDenom) / norm, isDenom);
		
	}
	
	private double computeNodeScore(Role role, Type type, Phrase phrase, int bIndex, int eIndex, boolean considerPreferences){
		return this._fm.computeScore(this._fm.extractFeatures(role, type, phrase, this._span, bIndex, eIndex, true), considerPreferences);
	}
	
	private double computePathScore(Role[] roles, Phrase phrase, boolean considerPreferences){
		return this._fm.computeScore(this._fm.extractFeatures(roles, phrase, true), considerPreferences);
	}
	
	private void putCount(Role[] roles, Phrase phrase, double count, boolean isDenom){
		ArrayList<Integer> features = this._fm.extractFeatures(roles, phrase, false);
		for(int feature : features){
			if(isDenom)
				this._fm.addFeatureCount_G(feature, count*this._weight);
			else
				this._fm.addFeatureCount_L(feature, count*this._weight);
		}
	}
	
	private void putCount(Role arg, Type type, Phrase phrase, int bIndex, int eIndex, double count, boolean isDenom){
		ArrayList<Integer> features = this._fm.extractFeatures(arg, type, phrase, this._span, bIndex, eIndex, false);
		for(int feature : features){
			if(isDenom)
				this._fm.addFeatureCount_G(feature, count*this._weight);
			else
				this._fm.addFeatureCount_L(feature, count*this._weight);
		}
	}
	
	private void putInsideScore(Event event, double score, boolean isDenom){
		HashMap<Event, Double> map = isDenom ? this._beta_G : this._beta_L;
		double oldScore = 0;
		if(map.containsKey(event))
			oldScore = map.get(event);
		map.put(event, oldScore + score);
	}
	
	private void putOutsideScore(Event event, double score, boolean isDenom){
		HashMap<Event, Double> map = isDenom ? this._alpha_G : this._alpha_L;
		double oldScore = 0;
		if(map.containsKey(event))
			oldScore = map.get(event);
		map.put(event, oldScore + score);
	}
	
	private void putInsideScore(Event event, Role role1, Role role2, int eIndex, double score, boolean isDenom){
		HashMap<Event, HashMap<RolePair, Double>> map = isDenom ? _insideprob_G : _insideprob_L;
		
		if(!map.containsKey(event))
			map.put(event, new HashMap<RolePair, Double>());
		
		RolePair tuple = new RolePair(role1, role2, eIndex);
		double oldScore = 0;
		if(map.get(event).containsKey(tuple))
			oldScore = map.get(event).get(tuple);
		
		map.get(event).put(tuple, oldScore + score);
	}
	
	private double getInsideScore(Event event, Role role1, Role role2, int eIndex, boolean isDenom){
		HashMap<Event, HashMap<RolePair, Double>> map = isDenom ? _insideprob_G : _insideprob_L;
		
		if(role1 == this._manager.toBeginRole(event) && role2.equals(this._manager.toBeginRole(event)) && eIndex == 0)
			return 1.0;
		
		RolePair tuple = new RolePair(role1, role2, eIndex);
		if(map.get(event).containsKey(tuple))
			return map.get(event).get(tuple);
		
		return 0.0;
	}
	
	private void putOutsideScore(Event event, Role role1, Role role2, int eIndex, double score, boolean isDenom){
		
		HashMap<Event, HashMap<RolePair, Double>> map = isDenom ? _outsideprob_G : _outsideprob_L;
		
		if(!map.containsKey(event))
			map.put(event, new HashMap<RolePair, Double>());
		
		RolePair tuple = new RolePair(role1, role2, eIndex);
		double oldScore = 0;
		if(map.get(event).containsKey(tuple))
			oldScore = map.get(event).get(tuple);
		
		map.get(event).put(tuple, oldScore + score);
	}
	
	private double getOutsideScore(Event event, Role role1, Role role2, int eIndex, boolean isDenom){
		
		HashMap<Event, HashMap<RolePair, Double>> map = isDenom ? _outsideprob_G : _outsideprob_L;
		
		if(role2 == this._manager.toEndRole(event) && eIndex == this._span.getWords().length+1)
			return 1.0;
		
		RolePair tuple = new RolePair(role1, role2, eIndex);
		if(map.get(event).containsKey(tuple))
			return map.get(event).get(tuple);
		
		return 0.0;
		
	}
	
	private ArrayList<Role> getAcceptableRoles(Event event, int bIndex, int eIndex){
		
		if(!this._acceptableRolesMap.containsKey(event))
			this._acceptableRolesMap.put(event, new HashMap<Integer, ArrayList<Role>>());
		
		int key = Indexer.encode(bIndex, eIndex);
		
		if(this._acceptableRolesMap.get(event).containsKey(key))
			return this._acceptableRolesMap.get(event).get(key);
		
		ArrayList<Role> results = new ArrayList<Role>();
		
		if(bIndex == -1 && eIndex == 0)
			results.add(this._manager.toBeginRole(event));
		else if(bIndex == this._span.getWords().length && eIndex == this._span.getWords().length+1)
			results.add(this._manager.toEndRole(event));
		else {
			ArrayList<Type> types = this._lattice.getTypes(bIndex, eIndex);
			Role[] roles = event.getRoles();
			for(Role role : roles){
				boolean acceptable = false;
				Type[] acceptableTypes = role.getTypes();
				for(Type type: types){
					for(Type acceptableType : acceptableTypes)
						if(acceptableType == type){
							acceptable = true;
							break;
						}
					if(acceptable) break;
				}
				if(acceptable) results.add(role);
			}
		}
		
		this._acceptableRolesMap.get(event).put(key, results);
		
		return results;
		
	}
	
}




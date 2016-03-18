package semie.core;

import java.util.ArrayList;
import java.util.HashMap;

import semie.types.Event;
import semie.types.EventAnnotation;
import semie.types.EventSpan;
import semie.types.FeatureManager;
import semie.types.Manager;
import semie.types.Phrase;
import semie.types.Role;
import semie.types.RoleSpan;
import semie.types.Type;

/**
 * supervised learner
 * @author luwei
 * @version 1.0
 */

public class SupervisedLearner extends ConstrainedLearner{
	
	private EventAnnotation _annotation;
	private int[][] _guide_intervals;
	private int _annotationIndex;
	private HashMap<Event, RoleSpan> _predictions = new HashMap<Event, RoleSpan>();
	
	public SupervisedLearner(Manager manager, FeatureManager fm, EventSpan span, Event event, double weight) {
		super(manager, fm, span, event, weight, null);
		this._annotation = span.getGoldAnnotation();
		this._guide_intervals = this._annotation.getSortedIntervals();
		this._fm.resetFeatureExtractorCache();
	}
	
	protected void computeL(){
		boolean considerPreferences = true;
		this._annotationIndex = 0;
		
		this.guided_decode(considerPreferences);
		RoleSpan prediction = this._predictions.get(this._event);

		RoleSpan curr = prediction;
		while(curr!=null){
			ArrayList<ArrayList<Integer>> allfeatures = curr.getFeatures();
			for(ArrayList<Integer> features : allfeatures)
				for(int feature : features)
					this._fm.addFeatureCount_L(feature, 1.0*this._weight);
			curr = curr.getPrev();
		}
		
		double totalScore = prediction.getScore();
		
		this._beta_L.put(this._event, totalScore);

	}
	
	protected void computeG(){
		super.computeG();
	}
	
	private void guided_decode(boolean considerPreferences){
		RoleSpan beginSpan = new RoleSpan(-1, 0, this._manager.toBeginRole(this._event), 1.0, new ArrayList<ArrayList<Integer>>());
		this.guided_decode(beginSpan, beginSpan, considerPreferences);
	}
	
	private void guided_decode(RoleSpan span_A, RoleSpan span_B, boolean considerPreferences){
		
		Role A = span_A.getRole();
		Role B = span_B.getRole();
		
		int[] interval;
		int bIndex, cIndex, eIndex;
		Role C;
		
		bIndex = span_B.getEIndex();
		
		if(this._annotationIndex!=this._guide_intervals.length){
			while(true){
				interval = this._guide_intervals[this._annotationIndex++];
				if(interval.length>0){
					cIndex = interval[0];
					eIndex = interval[1];
					C = this._annotation.getRole(cIndex, eIndex);
					
					if(eIndex>this._span.getWords().length)
						throw new IllegalStateException("Check eIndex="+eIndex);
					if(cIndex>=this._span.getWords().length)
						throw new IllegalStateException("Check cIndex="+cIndex);
					
					break;
				}
				if(this._annotationIndex==this._guide_intervals.length){
					cIndex = this._span.getWords().length;
					eIndex = cIndex+1;
					C = this._manager.toEndRole(this._event);
					
					break;
				}
			}
		} else {
			cIndex = this._span.getWords().length;
			eIndex = cIndex+1;
			C = this._manager.toEndRole(this._event);
		}
		
		double prevScore = span_B.getScore();
		
		// A [..] B [bIndex .. cIndex) C [eIndex ..
		
		ArrayList<ArrayList<Integer>> features = new ArrayList<ArrayList<Integer>>();
		
		double subScore = 0;
		ArrayList<Type> types = this._lattice.getTypes(cIndex, eIndex);
		Phrase phrase = this._lattice.getPhrase(cIndex, eIndex);
		Type[] acceptableTypes = C.getTypes();
		for(Type type : types)
			for(Type type2 : acceptableTypes)
				if(type == type2)
					subScore += this.computeNodeScore(C, type, phrase, this._span, cIndex, eIndex, features, considerPreferences);
		
		Phrase phrase_path = this._manager.toPhrase(this._span, bIndex, cIndex);
		
		double currScore = this.computePathScore(new Role[]{A, B, C}, phrase_path, features, considerPreferences) * subScore;
		double score = prevScore * currScore;
		
		RoleSpan span_C = new RoleSpan(cIndex, eIndex, C, score, features);
		span_C.setPrev(span_B);
		
		if(C != this._manager.toEndRole(this._event))
			this.guided_decode(span_B, span_C, considerPreferences);
		else
			this.putPrediction(this._event, span_C);
		
	}

	private boolean putPrediction(Event event, RoleSpan span){
		
		if(this._predictions.containsKey(event))
			throw new RuntimeException("Multiple supervision!?");
		else
			this._predictions.put(event, span);
		return true;
	}
	
	private double computeNodeScore(Role A, Type type, Phrase phrase, EventSpan eventspanId, int bIndex, int eIndex, ArrayList<ArrayList<Integer>> allfeatures, boolean considerPreferences){
		ArrayList<Integer> features = this._fm.extractFeatures(A, type, phrase, eventspanId, bIndex, eIndex, false);
		allfeatures.add(features);
		return this._fm.computeScore(features, considerPreferences);
	}
	
	private double computePathScore(Role[] args, Phrase phrase, ArrayList<ArrayList<Integer>> allfeatures, boolean considerPreferences){
		ArrayList<Integer> features = this._fm.extractFeatures(args, phrase, false);
		allfeatures.add(features);
		return this._fm.computeScore(features, considerPreferences);
	}
	
}

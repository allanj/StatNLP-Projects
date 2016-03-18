package semie.core;


import java.util.ArrayList;

import semie.pref.PreferenceManager;
import semie.types.Event;
import semie.types.EventSpan;
import semie.types.FeatureManager;
import semie.types.Manager;
import semie.types.RoleSpan;

/**
 * K-best hard constrained learner.
 * @author luwei
 * @version 1.0
 */

public class KBestHardConstrainedLearner extends ConstrainedLearner{
	
	private int _topK;
	
	public KBestHardConstrainedLearner(Manager manager, FeatureManager fm, EventSpan span, Event event, double weight, int topK, PreferenceManager cm) {
		super(manager, fm, span, event, weight, cm);
		this._topK = topK;
		this.init();
	}
	
	//features needs to be extracted, this is done by calling the unconstrained learner, but set the counts to 0.0
	//note: not the case for now, since features are extracted at the beginning of the Trainer 
	private void init(){
		this._fm.resetFeatureExtractorCache();
	}
	
	protected void computeL(){
		
		boolean considerPreferences = true;
		
		Predictor predictor = new Predictor(this._span, this._manager, this._fm, this._cm, this._topK, true);
		predictor.decode(this._event, considerPreferences);
		ArrayList<RoleSpan> predictions = predictor.getPreferredPredictions(this._event, this._topK);
		
		double totalScore = 0;
		for(RoleSpan prediction : predictions){
			double score = prediction.getScore();
			totalScore += score;
		}
		
		//put feature counts...
		for(RoleSpan prediction : predictions){
			double count = prediction.getScore()/totalScore;
			RoleSpan curr = prediction;
			while(curr!=null){
				ArrayList<ArrayList<Integer>> allfeatures = curr.getFeatures();
				for(ArrayList<Integer> features: allfeatures){
					for(int feature : features){
						this._fm.addFeatureCount_L(feature, count * this._weight);
					}
				}
				curr = curr.getPrev();
			}
		}
		
		this._beta_L.put(this._event, totalScore);
	}

}

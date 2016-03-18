package semie.core;


import java.util.ArrayList;

import semie.pref.PreferenceManager;
import semie.types.Event;
import semie.types.EventSpan;
import semie.types.FeatureManager;
import semie.types.Manager;
import semie.types.RoleSpan;

/**
 * k-best soft constrained learner.
 * @author luwei
 * @version 1.0
 */

public class KBestSoftConstrainedLearner extends ConstrainedLearner{
	
	private int _topK;
	private double _alpha = 0.01;// how much do we want to relax our constraints.
	private boolean _considerPreference = true;
	private ArrayList<RoleSpan> _predictions;
	
	public KBestSoftConstrainedLearner(Manager manager, FeatureManager fm, EventSpan span, Event event, double weight, int topK, PreferenceManager cm, double alpha) {
		super(manager, fm, span, event, weight, cm);
		this._topK = topK;
		this._alpha = alpha;
		if(this._alpha <0 || this._alpha >1)
			throw new IllegalStateException("Please set alpha to: [0,1]");
		//features needs to be extracted, this is done by calling the unconstrained learner, but set the weights to 0.0
		//note: not the case for now, since features are extracted at the beginning of the Trainer 
		this._fm.resetFeatureExtractorCache();
	}
	
	public void TurnOnPreference(){
		this._considerPreference = true;
	}
	
	public void TurnOffPreference(){
		this._considerPreference = false;
	}
	
	public ArrayList<RoleSpan> getPredictions(){
		return this._predictions;
	}

	protected void computeL(){
		
		boolean considerPreferences = true;
		
		double unconstrained_beta = 0;
		if(this._alpha > 0){
			UnconstrainedLearner learner = new UnconstrainedLearner(this._manager, _fm, this._span, this._event, this._weight * this._alpha);
			learner.computeL();
			unconstrained_beta = learner._beta_L.get(this._event);
		}
		
		double totalScore = unconstrained_beta * this._alpha;
		
		if(this._alpha<1){
			Predictor predictor = new Predictor(this._span, this._manager, _fm, this._cm, this._topK, true);
			predictor.decode(this._event, considerPreferences);
			
			{
				ArrayList<RoleSpan> predictions;
				if(this._considerPreference)
					predictions = predictor.getPreferredPredictions(this._event, this._topK);
				else
					predictions = predictor.getPredictions(this._event, this._topK);
				this._predictions = predictions;
				
				boolean uniform = false;
				
				if(!uniform){
					//this case will occur when the constraints are so strong that no prediction is available..
					if(predictions != null){
						for(RoleSpan prediction : predictions)
							totalScore += prediction.getScore() * (1-this._alpha);
						
						//put feature counts
						for(RoleSpan prediction : predictions){
							double count = prediction.getScore()* (1-this._alpha)/totalScore;
							RoleSpan curr = prediction;
							while(curr!=null){
								ArrayList<ArrayList<Integer>> allfeatures = curr.getFeatures();
								for(ArrayList<Integer> features: allfeatures)
									for(int feature : features)
										this._fm.addFeatureCount_L(feature, count*this._weight);
								curr = curr.getPrev();
							}
						}
					}
				} else {
					//this case will occur when the constraints are so strong that no prediction is available..
					if(predictions != null){
						for(@SuppressWarnings("unused") RoleSpan prediction : predictions)
							totalScore += 1.0 * (1-this._alpha);
						
						//put feature counts
						for(RoleSpan prediction : predictions){
							double count = 1.0 * (1-this._alpha)/totalScore;
							RoleSpan curr = prediction;
							while(curr!=null){
								ArrayList<ArrayList<Integer>> allfeatures = curr.getFeatures();
								for(ArrayList<Integer> features: allfeatures)
									for(int feature : features)
										this._fm.addFeatureCount_L(feature, count*this._weight);
								curr = curr.getPrev();
							}
						}
					}
				}
			}
			
		}

		this._beta_L.put(this._event, totalScore);
		
	}
}
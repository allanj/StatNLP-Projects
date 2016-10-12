package com.statnlp.projects.nndcrf.linear_pos;

import java.util.ArrayList;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;


public class POSInstance extends Instance {


	private static final long serialVersionUID = 1851514046050983662L;
	protected Sentence sentence;
	protected ArrayList<String> tags;
	protected ArrayList<String> predictons;
	
	
	protected double predictionScore;
	
	public POSInstance(int instanceId, double weight, Sentence sent) {
		super(instanceId, weight);
		this.sentence = sent;
	}
	

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return this.sentence.length();
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public POSInstance duplicate() {
		POSInstance inst = new POSInstance(this._instanceId, this._weight,this.sentence);
		if(tags!=null)
			inst.tags = (ArrayList<String>)tags.clone();
		else inst.tags = null;
		if(predictons!=null)
			inst.predictons =(ArrayList<String>)predictons.clone();
		else inst.predictons = null;
		return inst;
	}

	@Override
	public void removeOutput() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removePrediction() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Sentence getInput() {
		return this.sentence;
	}

	@Override
	public ArrayList<String> getOutput() {
		// TODO Auto-generated method stub
		return this.tags;
	}

	@Override
	public ArrayList<String> getPrediction() {
		// TODO Auto-generated method stub
		return this.predictons;
	}

	@Override
	public boolean hasOutput() {
		if(tags!=null) return true;
		else return false;
	}

	@Override
	public boolean hasPrediction() {
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setPrediction(Object o) {
		this.predictons = (ArrayList<String>)o;
	}
	

	public void setPredictionScore(double score){this.predictionScore = score;}
	public double getPredictionScore(){return this.predictionScore;}
	
}

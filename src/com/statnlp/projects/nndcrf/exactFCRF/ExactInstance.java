package com.statnlp.projects.nndcrf.exactFCRF;

import java.util.ArrayList;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;


public class ExactInstance extends Instance {


	private static final long serialVersionUID = 1851514046050983662L;
	protected Sentence sentence;
	protected ArrayList<String> predicton; //chunk tag
	protected ArrayList<String> output; //chunk tag
	
	public ExactInstance(int instanceId, double weight, Sentence sent) {
		super(instanceId, weight);
		this.sentence = sent;
	}

	@Override
	public int size() {
		return this.sentence.length();
	}

	@SuppressWarnings("unchecked")
	@Override
	public ExactInstance duplicate() {
		ExactInstance inst = new ExactInstance(this._instanceId, this._weight,this.sentence);
		if(output!=null)
			inst.output = (ArrayList<String>)output.clone();
		else inst.output = null;
		if(predicton!=null)
			inst.predicton =(ArrayList<String>)predicton.clone();
		else inst.predicton = null;
		return inst;
	}

	@Override
	public void removeOutput() {
	}

	@Override
	public void removePrediction() {
	}

	@Override
	public Sentence getInput() {
		return this.sentence;
	}

	@Override
	public ArrayList<String> getOutput() {
		return this.output;
	}

	@Override
	public ArrayList<String> getPrediction() {
		return this.predicton;
	}

	@Override
	public boolean hasOutput() {
		if(output!=null) return true;
		else return false;
	}

	@Override
	public boolean hasPrediction() {
		if(output!=null) return true;
		else return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setPrediction(Object o) {
		this.predicton = (ArrayList<String>)o;
	}

}

package com.statnlp.example.treecrf;

import java.util.ArrayList;

import com.statnlp.commons.types.Instance;

import edu.stanford.nlp.trees.Tree;


public class TCRFInstance extends Instance {

	protected ArrayList<String> words;
	protected Tree tags;
	protected Tree predictions;
	
	public TCRFInstance(int instanceId, double weight) {
		super(instanceId, weight);
		// TODO Auto-generated constructor stub
	}

	@Override
	public int size() {
		return this.words.size();
	}

	@SuppressWarnings("unchecked")
	@Override
	public TCRFInstance duplicate() {
		TCRFInstance tcrfInstance = new TCRFInstance(this._instanceId, this._weight);
		if(words==null)
			tcrfInstance.words = null;
		else tcrfInstance.words = (ArrayList<String>)words.clone();
		if(tags==null)
			tcrfInstance.tags = null;
		else tcrfInstance.tags = tags.deepCopy();
		
		if(predictions==null)
			tcrfInstance.predictions = null;
		else tcrfInstance.predictions = predictions.deepCopy();
		return tcrfInstance;
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
	public ArrayList<String> getInput() {
		// TODO Auto-generated method stub
		return this.words;
	}

	@Override
	public Tree getOutput() {
		// TODO Auto-generated method stub
		return this.tags;
	}

	@Override
	public Tree getPrediction() {
		// TODO Auto-generated method stub
		return this.predictions;
	}

	@Override
	public boolean hasOutput() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasPrediction() {
		// TODO Auto-generated method stub
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setPrediction(Object o) {
		// TODO Auto-generated method stub
		this.predictions = (Tree)o;
	}

}

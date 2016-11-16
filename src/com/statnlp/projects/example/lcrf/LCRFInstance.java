package com.statnlp.projects.example.lcrf;

import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.io.Label;
import com.statnlp.commons.types.Instance;

@SuppressWarnings("serial")
public class LCRFInstance extends Instance {

	protected ArrayList<String> words;
	protected ArrayList<Label> tags;
	protected ArrayList<Label> predictons;
	
	public LCRFInstance(int instanceId, double weight) {
		super(instanceId, weight);
		// TODO Auto-generated constructor stub
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return this.words.size();
	}

	@SuppressWarnings("unchecked")
	@Override
	public LCRFInstance duplicate() {
		LCRFInstance lcrfInstance = new LCRFInstance(this._instanceId, this._weight);
		if(words==null)
			lcrfInstance.words = null;
		else lcrfInstance.words = (ArrayList<String>)words.clone();
		if(tags!=null)
			lcrfInstance.tags = (ArrayList<Label>)tags.clone();
		else lcrfInstance.tags = null;
		if(predictons!=null)
			lcrfInstance.predictons =(ArrayList<Label>)predictons.clone();
		else lcrfInstance.predictons = null;
		return lcrfInstance;
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
	public List<String> getInput() {
		
		return this.words;
	}

	@Override
	public List<Label> getOutput() {
		// TODO Auto-generated method stub
		return this.tags;
	}

	@Override
	public List<Label> getPrediction() {
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
		// TODO Auto-generated method stub
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setPrediction(Object o) {
		this.predictons = (ArrayList<Label>)o;
	}

}

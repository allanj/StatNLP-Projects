package com.statnlp.projects.entity.dcrf;

import java.util.ArrayList;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;


public class DCRFInstance extends Instance {


	private static final long serialVersionUID = 1851514046050983662L;
	protected Sentence sentence;
	protected ArrayList<String> entities;
	protected ArrayList<String> tags;
	protected ArrayList<String> predictons;
	protected ArrayList<String> entityPredictons;
	protected ArrayList<String> tagPredictons;
	
	public DCRFInstance(int instanceId, double weight, Sentence sent) {
		super(instanceId, weight);
		this.sentence = sent;
		// TODO Auto-generated constructor stub
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return this.sentence.length();
	}

	@Override
	public DCRFInstance duplicate() {
		DCRFInstance inst = new DCRFInstance(this._instanceId, this._weight,this.sentence);
		if(entities!=null)
			inst.entities = (ArrayList<String>)entities.clone();
		else inst.entities = null;
		
		if(tags!=null)
			inst.tags = (ArrayList<String>)tags.clone();
		else inst.tags = null;
		
		if(predictons!=null)
			inst.predictons =(ArrayList<String>)predictons.clone();
		else inst.predictons = null;
		if(entityPredictons!=null)
			inst.entityPredictons =(ArrayList<String>)entityPredictons.clone();
		else inst.entityPredictons = null;
		if(tagPredictons!=null)
			inst.tagPredictons =(ArrayList<String>)tagPredictons.clone();
		else inst.tagPredictons = null;
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
		return this.entities;
	}

	@Override
	public ArrayList<String> getPrediction() {
		// TODO Auto-generated method stub
		return this.predictons;
	}

	@Override
	public boolean hasOutput() {
		if(entities!=null) return true;
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
		this.predictons = (ArrayList<String>)o;
	}
	
	public void setEntityPrediction(Object o) {
		this.entityPredictons = (ArrayList<String>)o;
	}
	
	public ArrayList<String> getEntityPrediction() {
		return this.entityPredictons;
	}
	public ArrayList<String> getTagPrediction() {
		return this.tagPredictons;
	}
	
	public void setTagPrediction(Object o) {
		this.tagPredictons = (ArrayList<String>)o;
	}

}

package com.statnlp.projects.nndcrf.factorialCRFs;

import java.util.ArrayList;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;


public class TFInstance extends Instance {


	private static final long serialVersionUID = 1851514046050983662L;
	protected Sentence sentence;
	protected ArrayList<String> entities;
	protected ArrayList<String> predictons;
	protected ArrayList<String> entityPredictons;
	protected ArrayList<String> tagPredictons;
	
	public TFInstance(int instanceId, double weight, Sentence sent) {
		super(instanceId, weight);
		this.sentence = sent;
	}

	@Override
	public int size() {
		return this.sentence.length();
	}

	@SuppressWarnings("unchecked")
	@Override
	public TFInstance duplicate() {
		TFInstance inst = new TFInstance(this._instanceId, this._weight,this.sentence);
		if(entities!=null)
			inst.entities = (ArrayList<String>)entities.clone();
		else inst.entities = null;
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
		return this.entities;
	}

	@Override
	public ArrayList<String> getPrediction() {
		return this.predictons;
	}

	@Override
	public boolean hasOutput() {
		if(entities!=null) return true;
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

	public ArrayList<String> getEntityPredictons() {
		return entityPredictons;
	}

	public void setEntityPredictons(ArrayList<String> entityPredictons) {
		this.entityPredictons = entityPredictons;
	}

	public ArrayList<String> getTagPredictons() {
		return tagPredictons;
	}

	public void setTagPredictons(ArrayList<String> tagPredictons) {
		this.tagPredictons = tagPredictons;
	}
	
	public void setEntities(ArrayList<String> entities) {
		this.entities = entities;
	}
	

}

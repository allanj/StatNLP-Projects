package com.statnlp.dp.model.bruteforce;

import java.util.ArrayList;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;


public class BFInstance extends Instance {


	private static final long serialVersionUID = 1851514046050983662L;
	protected Sentence sentence;
	protected ArrayList<String> entities;
	protected ArrayList<String> predEntities;
	protected int[] predHeads;
	
	public BFInstance(int instanceId, double weight, Sentence sent) {
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
	public BFInstance duplicate() {
		BFInstance inst = new BFInstance(this._instanceId, this._weight,this.sentence);
		if(entities!=null)
			inst.entities = (ArrayList<String>)entities.clone();
		else inst.entities = null;
		if(predEntities!=null)
			inst.predEntities =(ArrayList<String>)predEntities.clone();
		else inst.predEntities = null;
		if(predHeads!=null)
			inst.predHeads = predHeads.clone();
		else inst.predHeads = null;
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

	public ArrayList<String> getPredEntities() {
		// TODO Auto-generated method stub
		return this.predEntities;
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

	public void setPredEntities(Object o) {
		this.predEntities = (ArrayList<String>)o;
	}

	/**
	 * This two prediction method is unused here
	 */
	@Override
	public Object getPrediction() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPrediction(Object o) {
		throw new RuntimeException("This method is not implementend. ");
	}

	public int[] getPredHeads() {
		return predHeads;
	}

	public void setPredHeads(int[] predHeads) {
		this.predHeads = predHeads;
	}
	
	

}

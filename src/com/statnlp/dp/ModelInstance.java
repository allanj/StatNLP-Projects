package com.statnlp.dp;

import java.util.ArrayList;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.UnnamedDependency;

public abstract class ModelInstance extends Instance {

	
	private static final long serialVersionUID = -2151506120384617544L;
	
	
	protected Sentence sentence;
	protected Tree output; //output is the span tree;
	protected Tree prediction; //prediction is also the dependency spanning tree
	protected String[] predEntities; //0 index is rootl
	
	public ModelInstance(int instanceId, double weight) {
		super(instanceId, weight);
	}

	public ModelInstance(int instanceId, double weight, Sentence sentence) {
		super(instanceId, weight);
		this.sentence = sentence;
	}
	

	@Override
	public int size() {
		//Return the length of the input sentence
		return this.sentence.length();
	}

	@Override
	public abstract ModelInstance duplicate();

	@Override
	public void removeOutput() {
		this.output = null;
	}

	@Override
	public void removePrediction() {
		this.prediction = null;

	}

	@Override
	public Sentence getInput() {
		return this.sentence;
	}

	@Override
	public Tree getOutput() {
		return this.output;
	}

	@Override
	public Tree getPrediction() {
		return this.prediction;
	}

	@Override
	public boolean hasOutput() {
		return output!=null;
	}

	@Override
	public boolean hasPrediction() {
		return prediction!=null;
	}

	@Override
	public void setPrediction(Object o) {
		Tree pred = (Tree)o;
		this.prediction = pred;

	}
	
	public String[] getPredEntities() {
		return predEntities;
	}

	public void setPredEntities(String[] predEntities) {
		this.predEntities = predEntities;
	}

	
	public abstract ArrayList<UnnamedDependency> toDependencies(Tree root);
	
	public abstract String[] toEntities(Tree spanRoot);

}

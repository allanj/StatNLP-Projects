package com.statnlp.dp.model.var;

import java.util.ArrayList;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;

import edu.stanford.nlp.trees.UnnamedDependency;

public class VarInstance extends Instance{

	/**
	 * All the input and output the index 0 is the root, not the actual starting point
	 */
	protected Sentence sent;
	protected int[] depOutput;
	protected int[] depPrediction;
	protected String[] enOutput;
	protected String[] enPrediction;
	
	protected ArrayList<UnnamedDependency> dependencies; //only for the output
	
	
	private static final long serialVersionUID = 2175742491946682505L;
	
	public VarInstance(int instanceId, double weight) {
		super(instanceId, weight);
	}
	
	public VarInstance(int instanceId, double weight, Sentence sent) {
		super(instanceId, weight);
		this.sent = sent;
	}
	
	public VarInstance(int instanceId, double weight, Sentence sent, ArrayList<UnnamedDependency> dependencies) {
		super(instanceId, weight);
		this.sent = sent;
		this.dependencies = dependencies;
	}
	
	public VarInstance(int instanceId, double weight, Sentence sent, ArrayList<UnnamedDependency> dependencies, int[] depOutput) {
		super(instanceId, weight);
		this.sent = sent;
		this.dependencies = dependencies;
		this.depOutput = depOutput;
		for(int i=0;i<this.size();i++){
			enOutput[i] = this.sent.get(i).getEntity();
		}
	}


	@SuppressWarnings("unchecked")
	@Override
	public VarInstance duplicate() {
		VarInstance di = new VarInstance(this._instanceId, this._weight,this.sent);
		if(depOutput==null)
			di.depOutput = null;
		else di.depOutput = depOutput.clone();
		
		if(enOutput==null)
			di.enOutput = null;
		else di.enOutput = enOutput.clone();
		
		if(depPrediction==null)
			di.depPrediction = null;
		else di.depPrediction = depPrediction.clone();
		
		if(enPrediction==null)
			di.enPrediction = null;
		else di.enPrediction = enPrediction.clone();
		
		if(dependencies==null) di.dependencies = null;
		else di.dependencies = (ArrayList<UnnamedDependency>) dependencies.clone();
		
		return di;
	}


	@Override
	public int size() {
		return this.sent.length();
	}

	@Override
	public void removeOutput() {
		this.depOutput = null;
		this.enOutput = null;
	}

	@Override
	public void removePrediction() {
		this.depPrediction = null;
		this.enPrediction = null;
	}

	@Override
	public Sentence getInput() {
		return this.sent;
	}

	@Override
	public Object getOutput() {
		throw new RuntimeException("The output is dependency and entity. pls use getDepOutput or getEntityOutput");
	}
	
	public int[] getDepOutput() {
		return this.depOutput;
	}
	
	public String[] getEntityOutput() {
		return this.enOutput;
	}

	@Override
	@Deprecated
	public Object getPrediction() {
		throw new RuntimeException("The prediction is dependency and entity. pls use getDepPrediction or getEntitPrediction");
	}
	
	public int[] getDepPrediction() {
		return this.depPrediction;
	}
	
	public String[] getEntityPrediction() {
		return this.enPrediction;
	}

	@Override
	public boolean hasOutput() {
		return this.depOutput!=null && this.enOutput!=null;
	}

	@Override
	public boolean hasPrediction() {
		return this.depPrediction!=null && this.enPrediction!=null;
	}

	@Override
	@Deprecated
	public void setPrediction(Object o) {
		throw new RuntimeException("The prediction is dependency and entity. pls use setDepPrediction or setEntitPrediction");
	}

	public void setDepPrediction(int[] predictions) {
		this.depPrediction = predictions;
	}
	
	public void setEntityPrediction(String[] entities) {
		this.enPrediction = entities;
	}
	
	
}

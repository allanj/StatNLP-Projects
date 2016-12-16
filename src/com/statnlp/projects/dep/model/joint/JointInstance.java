package com.statnlp.projects.dep.model.joint;

import java.util.ArrayList;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;


public class JointInstance extends Instance {

	/**
	 * Input is a sequence of words and tags. index 0 is root
	 */
	protected Sentence input;
	
	/**
	 * Output is span with head index and also the entity label.
	 */
	protected ArrayList<Span> output; 
	
	protected ArrayList<Span> prediction;
	
	public JointInstance(int instanceId, double weight, Sentence input) {
		super(instanceId, weight);
		this.input = input;
	}

	private static final long serialVersionUID = 3868561354729955153L;

	@Override
	public int size() {
		return this.input.length();
	}

	@SuppressWarnings("unchecked")
	@Override
	public JointInstance duplicate() {
		JointInstance inst = new JointInstance(this._instanceId, this._weight, this.input);
		if (output==null)
			inst.output = null;
		else inst.output = (ArrayList<Span>)output.clone();
		if(prediction==null)
			inst.prediction = null;
		else inst.prediction = (ArrayList<Span>)prediction.clone();
		return inst;
	}

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
		return this.input;
	}

	@Override
	public ArrayList<Span> getOutput() {
		return this.output;
	}

	@Override
	public ArrayList<Span> getPrediction() {
		return this.prediction;
	}

	@Override
	public boolean hasOutput() {
		return this.output != null;
	}

	@Override
	public boolean hasPrediction() {
		return this.prediction != null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setPrediction(Object o) {
		this.prediction = (ArrayList<Span>)o;
	}

}

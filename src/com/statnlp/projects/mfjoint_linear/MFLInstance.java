package com.statnlp.projects.mfjoint_linear;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;

public class MFLInstance extends Instance {

	private static final long serialVersionUID = 7472469003829845696L;

	protected Sentence input;
	protected MFLPair output;
	protected MFLPair prediction;
	
	
	public MFLInstance(int instanceId, double weight) {
		super(instanceId, weight);
	}
	
	public MFLInstance(int instanceId, double weight, Sentence sentence) {
		super(instanceId, weight);
		this.input = sentence;
	}

	@Override
	public int size() {
		return this.input.length();
	}

	@Override
	public Instance duplicate() {
		MFLInstance inst = new MFLInstance(this._instanceId, this._weight, this.input);
		if (output == null)
			inst.output = null;
		else inst.output = output.duplicate();
		if (prediction == null)
			inst.prediction = null;
		else inst.prediction = prediction.duplicate();
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
	public MFLPair getOutput() {
		return this.output;
	}

	@Override
	public MFLPair getPrediction() {
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

	@Override
	public void setPrediction(Object o) {
		MFLPair predict = (MFLPair)o;
		this.prediction = predict;
	}

}

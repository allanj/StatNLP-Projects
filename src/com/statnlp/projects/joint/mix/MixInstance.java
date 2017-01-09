package com.statnlp.projects.joint.mix;

import java.util.List;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;

public class MixInstance extends Instance {


	private static final long serialVersionUID = 8954850327772241324L;

	protected Sentence input;
	protected MixPair output;
	protected MixPair prediction;
	
	
	public MixInstance(int instanceId, double weight) {
		super(instanceId, weight);
	}
	
	public MixInstance(int instanceId, double weight, Sentence sentence) {
		super(instanceId, weight);
		this.input = sentence;
	}

	@Override
	public int size() {
		return this.input.length();
	}

	@Override
	public Instance duplicate() {
		MixInstance inst = new MixInstance(this._instanceId, this._weight, this.input);
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
	public MixPair getOutput() {
		return this.output;
	}

	@Override
	public MixPair getPrediction() {
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
		MixPair predict = (MixPair)o;
		this.prediction = predict;
	}

	
	/**
	 * provide the method for span to string array.
	 * @param ss
	 * @return
	 */
	public String[] toEntities(List<MixSpan> ss){
		String[] res = new String[this.input.length()];
		for(MixSpan s: ss){
			for(int i= s.start; i<= s.end; i++){
				if (s.label.form.equals("O")) {
					res[i] = s.label.form;
				} else {
					res[i] = i == s.start? "B-"+s.label.form : "I-"+s.label.form;
				}
			}
		}
		return res;
	}


}

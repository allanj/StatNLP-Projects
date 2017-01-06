package com.statnlp.projects.mfjoint;

import java.util.List;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;

public class MFJInstance extends Instance {

	private static final long serialVersionUID = 7472469003829845696L;

	protected Sentence input;
	protected MFJPair output;
	protected MFJPair prediction;
	
	
	public MFJInstance(int instanceId, double weight) {
		super(instanceId, weight);
	}
	
	public MFJInstance(int instanceId, double weight, Sentence sentence) {
		super(instanceId, weight);
		this.input = sentence;
	}

	@Override
	public int size() {
		return this.input.length();
	}

	@Override
	public Instance duplicate() {
		MFJInstance inst = new MFJInstance(this._instanceId, this._weight, this.input);
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
	public MFJPair getOutput() {
		return this.output;
	}

	@Override
	public MFJPair getPrediction() {
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
		MFJPair predict = (MFJPair)o;
		this.prediction = predict;
	}

	
	/**
	 * provide the method for span to string array.
	 * @param ss
	 * @return
	 */
	public String[] toEntities(List<MFJSpan> ss){
		String[] res = new String[this.input.length()];
		for(MFJSpan s: ss){
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

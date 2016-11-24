package com.statnlp.projects.dep.model.segdep;

import java.util.ArrayList;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;


public class SDInstance extends Instance {

	/**
	 * Input is a sequence of words and tags. index 0 is root
	 */
	protected Sentence input;

	/**
	 * Output is the head index. stored according to the index of segments
	 */
	protected int[] output;

	protected int[] prediction;

	/**
	 * The segment list store the span
	 */
	protected ArrayList<Span> segments;
	
	public SDInstance(int instanceId, double weight, Sentence input) {
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
	public SDInstance duplicate() {
		SDInstance inst = new SDInstance(this._instanceId, this._weight, this.input);
		if (output == null)
			inst.output = null;
		else
			inst.output = output.clone();
		if (prediction == null)
			inst.prediction = null;
		else
			inst.prediction = prediction.clone();
		if (segments == null)
			inst.segments = null;
		else
			inst.segments = (ArrayList<Span>) segments.clone();
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
	public int[] getOutput() {
		return this.output;
	}

	@Override
	public int[] getPrediction() {
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
		this.prediction = (int[])o;
	}

}

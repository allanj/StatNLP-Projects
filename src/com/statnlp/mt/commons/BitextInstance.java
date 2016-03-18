package com.statnlp.mt.commons;

import com.statnlp.commons.types.InputToken;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.OutputToken;

public class BitextInstance extends Instance{
	
	private static final long serialVersionUID = -6157677899857676503L;
	
	private InputToken[] _src;
	private OutputToken[] _tgt;
	private OutputToken[] _prediction;
	
	public BitextInstance(int instanceId, double weight, InputToken[] src, OutputToken[] tgt) {
		super(instanceId, weight);
		this._src = src;
		this._tgt = tgt;
	}
	
	@Override
	public int size() {
		return this._src.length;
	}
	
	@Override
	public Instance duplicate() {
		return new BitextInstance(this._instanceId, this._weight, this._src, this._tgt);
	}
	
	@Override
	public void removeOutput() {
		this._tgt = null;
	}
	
	@Override
	public void removePrediction() {
		this._prediction = null;
	}
	
	@Override
	public InputToken[] getInput() {
		return this._src;
	}
	
	@Override
	public OutputToken[] getOutput() {
		return this._tgt;
	}
	
	@Override
	public OutputToken[] getPrediction() {
		return this._prediction;
	}
	
	@Override
	public boolean hasOutput() {
		return this._tgt != null;
	}
	
	@Override
	public boolean hasPrediction() {
		return this._prediction != null;
	}
	
	@Override
	public void setPrediction(Object o) {
		this._prediction = (OutputToken[]) o;
	}
	
}
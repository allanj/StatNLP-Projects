package com.statnlp.topic.commons;

import com.statnlp.commons.types.InputToken;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.OutputToken;

public class TopicInstance extends Instance{
	
	private static final long serialVersionUID = -6157677899857676503L;
	
	private InputToken[] _src;
	
	public TopicInstance(int instanceId, double weight, InputToken[] src) {
		super(instanceId, weight);
		this._src = src;
	}
	
	@Override
	public int size() {
		return this._src.length;
	}
	
	@Override
	public Instance duplicate() {
		return new TopicInstance(this._instanceId, this._weight, this._src);
	}
	
	@Override
	public void removeOutput() {
		//do nothing...
	}
	
	@Override
	public void removePrediction() {
		//do nothing...
	}
	
	@Override
	public InputToken[] getInput() {
		return this._src;
	}
	
	@Override
	public OutputToken[] getOutput() {
		return null;
	}
	
	@Override
	public OutputToken[] getPrediction() {
		return null;
	}
	
	@Override
	public boolean hasOutput() {
		return false;
	}
	
	@Override
	public boolean hasPrediction() {
		return false;
	}
	
	@Override
	public void setPrediction(Object o) {
		//do nothing...
	}
	
}
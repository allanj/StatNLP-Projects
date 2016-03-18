package com.statnlp.model.hybridtree;

import com.statnlp.commons.types.Instance;

public class HTInstance extends Instance {

	public HTInstance(int instanceId, double weight) {
		super(instanceId, weight);
		// TODO Auto-generated constructor stub
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Instance duplicate() {
		// TODO Auto-generated method stub
		return null;
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
	public Object getInput() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getOutput() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getPrediction() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasOutput() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasPrediction() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setPrediction(Object o) {
		// TODO Auto-generated method stub

	}

}

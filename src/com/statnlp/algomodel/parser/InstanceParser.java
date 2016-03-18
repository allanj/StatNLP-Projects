package com.statnlp.algomodel.parser;

import java.io.FileNotFoundException;

import com.statnlp.algomodel.AlgoModel;
import com.statnlp.hybridnetworks.FeatureManager;

/**
 * This InstanceParser class is used to parse training data and arguments and build instance and parameters for network compiler and feature manager
 * Users can override the BuildInstances(Instance[] instances) function to customize its own method for a specific algorithm
 * @author Li Hao
 *
 */
public abstract class InstanceParser {
	
	protected String[] args;
	protected AlgoModel algomodel;
	

	public InstanceParser(AlgoModel algomodel) {
		this.algomodel = algomodel;
	}
	
	
	public abstract void BuildInstances() throws FileNotFoundException;
	
	
	public void setParameters(String key, Object value)
	{
		this.algomodel.setParameters(key, value);
	}
	
	public Object getParameters(String key)
	{
		return this.algomodel.getParameters(key);
	}
	
	
	public FeatureManager getFeatureManager()
	{
		return this.algomodel.getFeatureManager();
	}
	
	
	

}

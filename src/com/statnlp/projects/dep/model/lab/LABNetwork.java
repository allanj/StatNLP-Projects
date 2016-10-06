package com.statnlp.projects.dep.model.lab;

import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.projects.dep.DependencyNetwork;

public class LABNetwork extends DependencyNetwork {

	
	private static final long serialVersionUID = 3850194972536404963L;
	
	public LABNetwork(){
		
	}
	
	public LABNetwork(int networkId, LABInstance inst, LocalNetworkParam param) {
		super(networkId, inst, param);
		// TODO Auto-generated constructor stub
	}

	public LABNetwork(int networkId, LABInstance inst, long[] nodes, int[][][] children, LocalNetworkParam param, int numNodes) {
		super(networkId, inst, nodes, children, param,numNodes);
	}
	
	public void remove(int k){
		
	}
	
	public boolean isRemoved(int k){
		return false;
	}
	
	//not implement the total LossUpTo yet

}

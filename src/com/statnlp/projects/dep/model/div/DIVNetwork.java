package com.statnlp.projects.dep.model.div;

import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.projects.dep.DependencyNetwork;

public class DIVNetwork extends DependencyNetwork {

	
	private static final long serialVersionUID = 991556477287748391L;
	
	
	public DIVNetwork() {
		// TODO Auto-generated constructor stub
	}

	public DIVNetwork(int networkId, DIVInstance inst, LocalNetworkParam param) {
		super(networkId, inst, param);
		// TODO Auto-generated constructor stub
	}

	public DIVNetwork(int networkId, DIVInstance inst, long[] nodes, int[][][] children, LocalNetworkParam param, int numNodes) {
		super(networkId, inst, nodes, children, param,numNodes);
	}
	
	public void remove(int k){
		
	}
	
	public boolean isRemoved(int k){
		return false;
	}
	

}

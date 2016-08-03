package com.statnlp.dp.model.hp2d;

import com.statnlp.dp.DependencyNetwork;
import com.statnlp.hybridnetworks.LocalNetworkParam;

public class H2DNetwork extends DependencyNetwork {

	
	private static final long serialVersionUID = 991556477287748391L;
	
	
	public H2DNetwork() {
		// TODO Auto-generated constructor stub
	}

	public H2DNetwork(int networkId, H2DInstance inst, LocalNetworkParam param) {
		super(networkId, inst, param);
		// TODO Auto-generated constructor stub
	}

	public H2DNetwork(int networkId, H2DInstance inst, long[] nodes, int[][][] children, LocalNetworkParam param, int numNodes) {
		super(networkId, inst, nodes, children, param,numNodes);
	}
	
	public void remove(int k){
		
	}
	
	public boolean isRemoved(int k){
		return false;
	}
	
	

}

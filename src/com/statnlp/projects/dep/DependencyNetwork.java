package com.statnlp.projects.dep;

import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.TableLookupNetwork;

public class DependencyNetwork extends TableLookupNetwork {

	
	private static final long serialVersionUID = 991556477287748391L;
	int _numNodes = -1;
	
	public DependencyNetwork() {
		// TODO Auto-generated constructor stub
	}

	public DependencyNetwork(int networkId, DependInstance inst, LocalNetworkParam param) {
		super(networkId, inst, param);
		// TODO Auto-generated constructor stub
	}

	public DependencyNetwork(int networkId, DependInstance inst, long[] nodes, int[][][] children, LocalNetworkParam param, int numNodes) {
		super(networkId, inst, nodes, children, param);
		this._numNodes = numNodes;
	}
	
	public int countNodes(){
		if(this._numNodes==-1)
			return super.countNodes();
		else return this._numNodes;
	}
	
	
	public void remove(int k){
		
	}
	
	public boolean isRemoved(int k){
		return false;
	}

}

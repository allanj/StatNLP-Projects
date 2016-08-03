package com.statnlp.dp.model.var;

import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.TableLookupNetwork;

public class TreeNetwork extends TableLookupNetwork {

	
	private static final long serialVersionUID = 991556477287748391L;
	int _numNodes = -1;
	
	public TreeNetwork() {
	}

	public TreeNetwork(int networkId, VarInstance inst, LocalNetworkParam param) {
		super(networkId, inst, param);
	}

	public TreeNetwork(int networkId, VarInstance inst, long[] nodes, int[][][] children, LocalNetworkParam param, int numNodes) {
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

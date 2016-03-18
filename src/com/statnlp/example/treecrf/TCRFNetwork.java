package com.statnlp.example.treecrf;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.TableLookupNetwork;

public class TCRFNetwork extends TableLookupNetwork {

	int _numNodes = -1;
	
	public TCRFNetwork() {
		// TODO Auto-generated constructor stub
	}

	public TCRFNetwork(int networkId, Instance inst, LocalNetworkParam param) {
		super(networkId, inst, param);
		// TODO Auto-generated constructor stub
	}

	public TCRFNetwork(int networkId, TCRFInstance inst, long[] nodes, int[][][] children, LocalNetworkParam param, int numNodes) {
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

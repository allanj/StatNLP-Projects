package com.statnlp.entity.dcrf;

import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.TableLookupNetwork;

public class DCRFNetwork extends TableLookupNetwork{

	int _numNodes = -1;
	
	public DCRFNetwork(){
		
	}
	
	public DCRFNetwork(int networkId, DCRFInstance inst, LocalNetworkParam param){
		super(networkId, inst, param);
	}
	
	public DCRFNetwork(int networkId, DCRFInstance inst, long[] node, int[][][] children, LocalNetworkParam param, int numNodes){
		super(networkId, inst,node, children, param);
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

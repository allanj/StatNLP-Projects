package com.statnlp.example.lcrf;

import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.TableLookupNetwork;

public class LCRFNetwork extends TableLookupNetwork{

	int _numNodes = -1;
	
	public LCRFNetwork(){
		
	}
	
	public LCRFNetwork(int networkId, LCRFInstance inst, LocalNetworkParam param){
		super(networkId, inst, param);
	}
	
	public LCRFNetwork(int networkId, LCRFInstance inst, long[] node, int[][][] children, LocalNetworkParam param, int numNodes){
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

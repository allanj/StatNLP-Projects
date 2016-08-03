package com.statnlp.entity.lcr2d;

import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.TableLookupNetwork;

public class E2DNetwork extends TableLookupNetwork{

	private static final long serialVersionUID = 301603038046736006L;
	
	int _numNodes = -1;
	
	public E2DNetwork(){
		
	}
	
	public E2DNetwork(int networkId, E2DInstance inst, LocalNetworkParam param){
		super(networkId, inst, param);
	}
	
	public E2DNetwork(int networkId, E2DInstance inst, long[] node, int[][][] children, LocalNetworkParam param, int numNodes){
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

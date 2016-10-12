package com.statnlp.projects.nndcrf.linear_chunk;

import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.TableLookupNetwork;

public class ChunkNetwork extends TableLookupNetwork{

	private static final long serialVersionUID = -1921705711609292845L;
	int _numNodes = -1;
	
	public ChunkNetwork(){
		
	}
	
	public ChunkNetwork(int networkId, ChunkInstance inst, LocalNetworkParam param, NetworkCompiler compiler){
		super(networkId, inst, param, compiler);
	}
	
	public ChunkNetwork(int networkId, ChunkInstance inst, long[] node, int[][][] children, LocalNetworkParam param, int numNodes, NetworkCompiler compiler){
		super(networkId, inst,node, children, param, compiler);
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

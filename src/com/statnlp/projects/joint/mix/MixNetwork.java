package com.statnlp.projects.joint.mix;

import java.util.Arrays;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.TableLookupNetwork;

public class MixNetwork extends TableLookupNetwork {

	private static final long serialVersionUID = -8553828001450750973L;
	protected int _numNodes = -1;
	protected int structure; 
	
	public MixNetwork () {
		
	}
	
	public MixNetwork(int networkId, Instance inst, LocalNetworkParam param, NetworkCompiler compiler) {
		super(networkId, inst, param);
	}

	public MixNetwork(int networkId, Instance inst, long[] nodes, int[][][] children, LocalNetworkParam param, int numNodes, NetworkCompiler compiler) {
		super(networkId, inst, nodes, children, param, compiler);
		this._numNodes = numNodes;
		this.isVisible = new boolean[nodes.length];
		Arrays.fill(isVisible, true);
	}
	
	public int countNodes(){
		if(this._numNodes==-1)
			return super.countNodes();
		else return this._numNodes;
	}
	
	
	public void remove(int k){
		this.isVisible[k] = false;
		if (this._inside != null){
			this._inside[k] = Double.NEGATIVE_INFINITY;
		}
		if (this._outside != null){
			this._outside[k] = Double.NEGATIVE_INFINITY;
		}
	}
	
	public boolean isRemoved(int k){
		return !this.isVisible[k];
	}
	

}

package com.statnlp.projects.dep.model.bruteforce;

import java.util.Arrays;

import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.TableLookupNetwork;
import com.statnlp.projects.dep.model.bruteforce.BFNetworkCompiler.NODE_TYPES;

public class BFNetwork extends TableLookupNetwork{

	private static final long serialVersionUID = -4941893181603696413L;
	int _numNodes = -1;
	
	boolean[] _is_removed;
	
	public BFNetwork(){
	}
	
	public BFNetwork(int networkId, BFInstance inst, LocalNetworkParam param){
		super(networkId, inst, param);
	}
	
	public BFNetwork(int networkId, BFInstance inst, long[] node, int[][][] children, LocalNetworkParam param, int numNodes){
		super(networkId, inst,node, children, param);
		this._numNodes = numNodes;
		_is_removed = new boolean[this._numNodes];
	}
	
	public int countNodes(){
		if(this._numNodes==-1)
			return super.countNodes();
		else return this._numNodes;
	}
	
	public void remove(int k){
		this._is_removed[k] = true;
		if (this._inside!=null){
			this._inside[k] = Double.NEGATIVE_INFINITY;
		}
		if (this._outside!=null){
			this._outside[k] = Double.NEGATIVE_INFINITY;
		}
	}
	
	public boolean isRemoved(int k){
		return _is_removed[k];
	}

	public void removeSomeNodes(){
		for(int k=0; k<countNodes();k++){
			int[] nodeArr = this.getNodeArray(k);
			if(nodeArr[4]==NODE_TYPES.depInLinear.ordinal() && nodeArr[1]>(this.getInstance().size()-1))
				remove(k);
		}
	}
	
	public void iniRemoveArr(){
		_is_removed = new boolean[countNodes()];
	}
}

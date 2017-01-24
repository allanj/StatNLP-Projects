package com.statnlp.projects.nndcrf.factorialCRFs;

import java.util.Arrays;

import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.TableLookupNetwork;
import com.statnlp.projects.nndcrf.factorialCRFs.FCRFNetworkCompiler.NODE_TYPES;

public class FCRFNetwork extends TableLookupNetwork{

	private static final long serialVersionUID = -5035676335489326537L;

	int _numNodes = -1;
	
	int structure; 
	
	public FCRFNetwork(){
		
	}
	
	public FCRFNetwork(int networkId, FCRFInstance inst, LocalNetworkParam param){
		super(networkId, inst, param);
	}
	
	public FCRFNetwork(int networkId, FCRFInstance inst, long[] nodes, int[][][] children, LocalNetworkParam param, int numNodes){
		super(networkId, inst,nodes, children, param);
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
	
	public void recover(int k){
		this.isVisible[k] = true;
	}
	
	
	/**
	 * 0 is the entity chain
	 * 1 is the PoS chain
	 */
	public void enableKthStructure(int kthStructure){
		if (kthStructure == 0) {
			// enable the chunking structure
			for (int i = 0; i < this.countNodes(); i++) {
				int[] node_k = this.getNodeArray(i);
				if (node_k[2] == NODE_TYPES.ENODE.ordinal() || node_k[2] == NODE_TYPES.LEAF.ordinal()
						|| node_k[2] == NODE_TYPES.ROOT.ordinal())
					recover(i);
				else remove(i);
			}
		} else if (kthStructure == 1) {
			// enable POS tagging structure
			for (int i = 0; i < this.countNodes(); i++) {
				int[] node_k = this.getNodeArray(i);
				if (node_k[2] == NODE_TYPES.TNODE.ordinal() || node_k[2] == NODE_TYPES.LEAF.ordinal()
						|| node_k[2] == NODE_TYPES.ROOT.ordinal())
					recover(i);
				else remove(i);
			}
		} else {
			throw new RuntimeException("removing unknown structures");
		}
	}
}

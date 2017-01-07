package com.statnlp.projects.mfjoint_linear;

import java.util.Arrays;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.TableLookupNetwork;
import com.statnlp.projects.mfjoint_linear.MFLConfig.STRUCT;
import com.statnlp.projects.mfjoint_linear.MFLNetworkCompiler.NodeType;

public class MFLNetwork extends TableLookupNetwork {

	private static final long serialVersionUID = 7222829854325317528L;
	protected int _numNodes = -1;
	protected int structure; 
	
	public MFLNetwork () {
		
	}
	
	public MFLNetwork(int networkId, Instance inst, LocalNetworkParam param, NetworkCompiler compiler) {
		super(networkId, inst, param);
	}

	public MFLNetwork(int networkId, Instance inst, long[] nodes, int[][][] children, LocalNetworkParam param, int numNodes, NetworkCompiler compiler) {
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
	
	public void recover(int k){
		this.isVisible[k] = true;
	}
	
	/**
	 * 0 is the entity chain
	 * 1 is the Dependency parsing tree
	 */
	public void enableKthStructure(int kthStructure){
		if (kthStructure == STRUCT.SEMI.ordinal()) {
			// enable the semiCRF chain structure
			for (int i = 0; i < this.countNodes(); i++) {
				int[] node_k = this.getNodeArray(i);
				if (node_k[4] == NodeType.ENTITY.ordinal() || node_k[4] == NodeType.ROOT.ordinal())
					recover(i);
				else remove(i);
			}
		} else if (kthStructure == STRUCT.TREE.ordinal()) {
			// enable the dependency structure
			for (int i = 0; i < this.countNodes(); i++) {
				int[] node_k = this.getNodeArray(i);
				if (node_k[4] == NodeType.DEP.ordinal() || node_k[4] == NodeType.ROOT.ordinal())
					recover(i);
				else remove(i);
			}
		} else {
			throw new RuntimeException("removing unknown structures");
		}
	}
	
}

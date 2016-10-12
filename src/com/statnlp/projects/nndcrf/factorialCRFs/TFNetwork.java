package com.statnlp.projects.nndcrf.factorialCRFs;

import java.util.Arrays;
import java.util.HashMap;

import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.hybridnetworks.TableLookupNetwork;
import com.statnlp.projects.nndcrf.factorialCRFs.TFNetworkCompiler.NODE_TYPES;

public class TFNetwork extends TableLookupNetwork{

	private static final long serialVersionUID = -5035676335489326537L;

	int _numNodes = -1;
	
	int structure; 
	
	public TFNetwork(){
		
	}
	
	public TFNetwork(int networkId, TFInstance inst, LocalNetworkParam param){
		super(networkId, inst, param);
	}
	
	public TFNetwork(int networkId, TFInstance inst, long[] nodes, int[][][] children, LocalNetworkParam param, int numNodes){
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
	public void removeKthStructure(int kthStructure){
		if(kthStructure==0){
			//remove the NE structure
			for(int i=0;i<this.countNodes();i++){
				int[] node_k = this.getNodeArray(i);
				if(node_k[1] == NODE_TYPES.ENODE.ordinal() || node_k[1] == NODE_TYPES.TAG_IN_E.ordinal() || node_k[1] == NODE_TYPES.ENODE_HYP.ordinal()) 
					remove(i);
				else recover(i);
			}
		}else if(kthStructure==1){
			//remove tag structure
			for(int i=0;i<this.countNodes();i++){
				int[] node_k = this.getNodeArray(i);
				if(node_k[1] == NODE_TYPES.E_IN_TAG.ordinal() || node_k[1] == NODE_TYPES.TNODE.ordinal() || node_k[1] == NODE_TYPES.TNODE_HYP.ordinal()) 
					remove(i);
				else recover(i);
			}
		}else{
			throw new RuntimeException("removing unknown structures");
		}
		
	}
	
	public void saveKthStructureScore(int kthStructure){
		if(kthStructure==0){
			//saving the score of NE chain
			this.newMarginals = new HashMap<Integer, Double>();
			for(int k=0;k<this.countNodes();k++){
				int[] node_k = this.getNodeArray(k);
				if(node_k[1] == NODE_TYPES.ENODE.ordinal()){
					double score = 0.0;
					if(this._outside!=null && this._inside!=null){
						score = this._outside[k]+this._inside[k]-this.getInside();
						if(score>0.0000001) {
							throw new RuntimeException("The marginal probability cannot be larger than 1.0");
						}
						int[] corrNode = node_k.clone();
						corrNode[1] = NODE_TYPES.E_IN_TAG.ordinal();
						int corrIdx = Arrays.binarySearch(this.getAllNodes(), NetworkIDMapper.toHybridNodeID(corrNode));
						if(corrIdx<0){
							throw new RuntimeException("saving NE score: do not have this node?");
						}
						newMarginals.put(corrIdx, score);
					}
				}
			}
		}else if(kthStructure==1){
			//saving the score of pos chain
			this.newMarginals = new HashMap<Integer, Double>();
			for(int k=0;k<this.countNodes();k++){
				int[] node_k = this.getNodeArray(k);
				if(node_k[1] == NODE_TYPES.TNODE.ordinal()){
					double score = 0.0;
					if(this._outside!=null && this._inside!=null){
						score = this._outside[k]+this._inside[k]-this.getInside();
						if(score>0.0000001) {
							throw new RuntimeException("The marginal probability cannot be larger than 1.0");
						}
						int[] corrNode = node_k.clone();
						corrNode[1] = NODE_TYPES.TAG_IN_E.ordinal();
						int corrIdx = Arrays.binarySearch(this.getAllNodes(), NetworkIDMapper.toHybridNodeID(corrNode));
						if(corrIdx<0){
							throw new RuntimeException("saving POS score: do not have this node?");
						}
						newMarginals.put(corrIdx, score);
					}
				}
			}
		}else{
			throw new RuntimeException("Saving unknown structure score?");
		}
	}
	
}

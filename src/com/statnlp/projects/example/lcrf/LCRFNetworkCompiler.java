package com.statnlp.projects.example.lcrf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.statnlp.commons.io.Label;
import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class LCRFNetworkCompiler extends NetworkCompiler{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1674061394842216446L;

	public enum NODE_TYPES {LEAF,NODE,ROOT};
	public List<Label> _labels;
	public int _size;
	public LCRFNetwork genericUnlabeledNetwork;
	
	public LCRFNetworkCompiler(List<Label> _labels){
		this._labels = _labels;
		this._size = 100;
		this.compileUnlabeledInstancesGeneric();
	}
	
	@Override
	public LCRFNetwork compile(int networkId, Instance inst, LocalNetworkParam param) {
		// TODO Auto-generated method stub
		LCRFInstance lcrfInstance = (LCRFInstance)inst;
		if(lcrfInstance.isLabeled())
			return compileLabeledInstances(networkId, lcrfInstance, param);
		else
			return compileUnlabeledInstances(networkId, lcrfInstance, param);
	}
	
	public long toNode_leaf(){
		int[] arr = new int[]{0,0,0,0,NODE_TYPES.LEAF.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	public long toNode(int pos, int tag_id){
		int[] arr = new int[]{pos+1,tag_id,0,0,NODE_TYPES.NODE.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	public long toNode_root(int size){
		int[] arr = new int[]{size+1,this._labels.size(),0,0,NODE_TYPES.ROOT.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}

	@Override
	public LCRFInstance decompile(Network network) {
		LCRFNetwork lcrfNetwork = (LCRFNetwork)network;
		LCRFInstance lcrfInstance = (LCRFInstance)lcrfNetwork.getInstance();
		LCRFInstance result = lcrfInstance.duplicate();
		ArrayList<Label> prediction = new ArrayList<Label>();
		
		
		long root = toNode_root(lcrfInstance.size());
		int rootIdx = Arrays.binarySearch(lcrfNetwork.getAllNodes(),root);
		
		for(int i=0;i<lcrfInstance.size();i++){
			int child_k = lcrfNetwork.getMaxPath(rootIdx)[0];
			long child = lcrfNetwork.getNode(child_k);
			rootIdx = child_k;
			int tagID = NetworkIDMapper.toHybridNodeArray(child)[1];
			prediction.add(0, _labels.get(tagID));
		}
		
		
		result.setPrediction(prediction);
		
		return result;
	}

	public LCRFNetwork compileLabeledInstances(int networkId, LCRFInstance inst, LocalNetworkParam param){
		LCRFNetwork lcrfNetwork = new LCRFNetwork(networkId, inst,param);
		long leaf = toNode_leaf();
		long[] children = new long[]{leaf};
		lcrfNetwork.addNode(leaf);
		for(int i=0;i<inst.size();i++){
			long node = toNode(i,inst.getOutput().get(i).getID());
			lcrfNetwork.addNode(node);
			long[] currentNodes = new long[]{node};
			lcrfNetwork.addEdge(node, children);
			children = currentNodes;
		}
		long root = toNode_root(inst.size());
		lcrfNetwork.addNode(root);
		lcrfNetwork.addEdge(root, children);
		
		lcrfNetwork.finalizeNetwork();
		return lcrfNetwork;
	}
	
	public LCRFNetwork compileUnlabeledInstances(int networkId, LCRFInstance inst, LocalNetworkParam param){
		
		long[] allNodes = genericUnlabeledNetwork.getAllNodes();
		long root = toNode_root(inst.size());
		int rootIdx = Arrays.binarySearch(allNodes, root);
		LCRFNetwork lcrfNetwork = new LCRFNetwork(networkId, inst,allNodes,genericUnlabeledNetwork.getAllChildren() , param, rootIdx+1);
		return lcrfNetwork;
	}
	
	public void compileUnlabeledInstancesGeneric(){
		LCRFNetwork lcrfNetwork = new LCRFNetwork();
		
		long leaf = toNode_leaf();
		long[] children = new long[]{leaf};
		lcrfNetwork.addNode(leaf);
		for(int i=0;i<_size;i++){
			long[] currentNodes = new long[_labels.size()];
			for(int l=0;l<_labels.size();l++){
				long node = toNode(i,_labels.get(l).getID());
				currentNodes[l] = node;
				lcrfNetwork.addNode(node);
				for(long child: children)
					lcrfNetwork.addEdge(node, new long[]{child});
			}
			long root = toNode_root(i+1);
			lcrfNetwork.addNode(root);
			for(long child:currentNodes)
				lcrfNetwork.addEdge(root, new long[]{child});
			children = currentNodes;
			
		}
		lcrfNetwork.finalizeNetwork();
		genericUnlabeledNetwork =  lcrfNetwork;
	}
	
	
	
}

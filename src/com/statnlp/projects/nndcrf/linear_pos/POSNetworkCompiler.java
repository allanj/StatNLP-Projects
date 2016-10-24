package com.statnlp.projects.nndcrf.linear_pos;

import java.util.ArrayList;
import java.util.Arrays;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class POSNetworkCompiler extends NetworkCompiler{

	private static final long serialVersionUID = -2388666010977956073L;

	public enum NODE_TYPES {LEAF,NODE,ROOT};
	public int _size;
	public POSNetwork genericUnlabeledNetwork;
	
	public POSNetworkCompiler(){
		this._size = 150;
		this.compileUnlabeledInstancesGeneric();
	}
	
	@Override
	public POSNetwork compile(int networkId, Instance inst, LocalNetworkParam param) {
		// TODO Auto-generated method stub
		POSInstance lcrfInstance = (POSInstance)inst;
		if(lcrfInstance.isLabeled())
			return compileLabeledInstances(networkId, lcrfInstance, param);
		else
			return compileUnlabeledInstances(networkId, lcrfInstance, param);
	}
	
	public long toNode_leaf(){
		int[] arr = new int[]{0, POS.get("START").getId(), 0,0,NODE_TYPES.LEAF.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	public long toNode(int pos, int tag_id){
		int[] arr = new int[]{pos+1,tag_id,0,0,NODE_TYPES.NODE.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	public long toNode_root(int size){
		int[] arr = new int[]{size, POS.get("END").getId(),0,0,NODE_TYPES.ROOT.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}

	@Override
	public POSInstance decompile(Network network) {
		POSNetwork lcrfNetwork = (POSNetwork)network;
		POSInstance lcrfInstance = (POSInstance)lcrfNetwork.getInstance();
		POSInstance result = lcrfInstance.duplicate();
		ArrayList<String> prediction = new ArrayList<String>();
		
		
		long root = toNode_root(lcrfInstance.size());
		int rootIdx = Arrays.binarySearch(lcrfNetwork.getAllNodes(),root);
		for(int i=0;i<lcrfInstance.size();i++){
			int child_k = lcrfNetwork.getMaxPath(rootIdx)[0];
			long child = lcrfNetwork.getNode(child_k);
			rootIdx = child_k;
			int tagID = NetworkIDMapper.toHybridNodeArray(child)[1];
			String resPOS = POS.get(tagID).getForm();
			prediction.add(0, resPOS);
		}
		
		result.setPrediction(prediction);
		result.setPredictionScore(lcrfNetwork.getMax());
		return result;
	}
	

	public POSNetwork compileLabeledInstances(int networkId, POSInstance inst, LocalNetworkParam param){
		POSNetwork lcrfNetwork = new POSNetwork(networkId, inst,param, this);
		long leaf = toNode_leaf();
		long[] children = new long[]{leaf};
		lcrfNetwork.addNode(leaf);
		for(int i=0;i<inst.size();i++){
			long node = toNode(i, POS.get(inst.getOutput().get(i)).getId());
			lcrfNetwork.addNode(node);
			long[] currentNodes = new long[]{node};
			lcrfNetwork.addEdge(node, children);
			children = currentNodes;
		}
		long root = toNode_root(inst.size());
		lcrfNetwork.addNode(root);
		lcrfNetwork.addEdge(root, children);
		lcrfNetwork.finalizeNetwork();
//		System.err.println(inst.getOutput().toString());
		if(!genericUnlabeledNetwork.contains(lcrfNetwork))
			System.err.println("not contains");
		return lcrfNetwork;
	}
	
	public POSNetwork compileUnlabeledInstances(int networkId, POSInstance inst, LocalNetworkParam param){
		
		long[] allNodes = genericUnlabeledNetwork.getAllNodes();
		long root = toNode_root(inst.size());
		int rootIdx = Arrays.binarySearch(allNodes, root);
		POSNetwork lcrfNetwork = new POSNetwork(networkId, inst,allNodes,genericUnlabeledNetwork.getAllChildren() , param, rootIdx+1, this);
		return lcrfNetwork;
	}
	
	public void compileUnlabeledInstancesGeneric(){
		POSNetwork lcrfNetwork = new POSNetwork();
		
		long leaf = toNode_leaf();
		long[] children = new long[]{leaf};
		lcrfNetwork.addNode(leaf);
		for(int i=0;i<_size;i++){
			long[] currentNodes = new long[POS.POSLabels.size()-2];
			for(int l=0;l<POS.POSLabels.size();l++){
				if(l==POS.get("START").getId()) continue;
				if(l==POS.get("END").getId()) continue;
				long node = toNode(i,l);
				for(long child: children){
					if(child==-1) continue;
					if(lcrfNetwork.contains(child)){
						lcrfNetwork.addNode(node);
						lcrfNetwork.addEdge(node, new long[]{child});
					}
				}
				if(lcrfNetwork.contains(node))
					currentNodes[l] = node;
				else currentNodes[l] = -1;
			}
			long root = toNode_root(i+1);
			lcrfNetwork.addNode(root);
			for(long child:currentNodes){
				if(child==-1) continue;
				lcrfNetwork.addEdge(root, new long[]{child});
			}
				
			children = currentNodes;
			
		}
		lcrfNetwork.finalizeNetwork();
		genericUnlabeledNetwork =  lcrfNetwork;
	}
	
	public double costAt(Network network, int parent_k, int[] child_k){
		return 0;
	}
	
}

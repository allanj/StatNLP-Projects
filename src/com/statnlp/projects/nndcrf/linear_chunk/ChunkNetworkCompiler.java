package com.statnlp.projects.nndcrf.linear_chunk;

import java.util.ArrayList;
import java.util.Arrays;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class ChunkNetworkCompiler extends NetworkCompiler{

	private static final long serialVersionUID = -2388666010977956073L;

	public enum NODE_TYPES {LEAF,NODE,ROOT};
	public int _size;
	public ChunkNetwork genericUnlabeledNetwork;
	private boolean IOBESencoding;
	
	public ChunkNetworkCompiler(boolean iobesencoding){
		this._size = 150;
		this.compileUnlabeledInstancesGeneric();
		this.IOBESencoding = iobesencoding;
	}
	
	@Override
	public ChunkNetwork compile(int networkId, Instance inst, LocalNetworkParam param) {
		// TODO Auto-generated method stub
		ChunkInstance lcrfInstance = (ChunkInstance)inst;
		if(lcrfInstance.isLabeled())
			return compileLabeledInstances(networkId, lcrfInstance, param);
		else
			return compileUnlabeledInstances(networkId, lcrfInstance, param);
	}
	
	public long toNode_leaf(){
		int[] arr = new int[]{0,Chunk.get("O").getId(), 0,0,NODE_TYPES.LEAF.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	public long toNode(int pos, int tag_id){
		int[] arr = new int[]{pos+1,tag_id,0,0,NODE_TYPES.NODE.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	public long toNode_root(int size){
		int[] arr = new int[]{size, Chunk.ChunkLabels.size(),0,0,NODE_TYPES.ROOT.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}

	@Override
	public ChunkInstance decompile(Network network) {
		ChunkNetwork lcrfNetwork = (ChunkNetwork)network;
		ChunkInstance lcrfInstance = (ChunkInstance)lcrfNetwork.getInstance();
		ChunkInstance result = lcrfInstance.duplicate();
		ArrayList<String> prediction = new ArrayList<String>();
		
		
		long root = toNode_root(lcrfInstance.size());
		int rootIdx = Arrays.binarySearch(lcrfNetwork.getAllNodes(),root);
		//System.err.println(rootIdx+" final score:"+network.getMax(rootIdx));
		for(int i=0;i<lcrfInstance.size();i++){
			int child_k = lcrfNetwork.getMaxPath(rootIdx)[0];
			long child = lcrfNetwork.getNode(child_k);
			rootIdx = child_k;
			int tagID = NetworkIDMapper.toHybridNodeArray(child)[1];
			String resEntity = Chunk.get(tagID).getForm();
			//assume it's the IOBES encoding.
			if(resEntity.startsWith("S")) resEntity = "B"+resEntity.substring(1);
			if(resEntity.startsWith("E")) resEntity = "I"+resEntity.substring(1);
			prediction.add(0, resEntity);
		}
		
		result.setPrediction(prediction);
		result.setPredictionScore(lcrfNetwork.getMax());
		return result;
	}
	

	public ChunkNetwork compileLabeledInstances(int networkId, ChunkInstance inst, LocalNetworkParam param){
		ChunkNetwork lcrfNetwork = new ChunkNetwork(networkId, inst,param, this);
		long leaf = toNode_leaf();
		long[] children = new long[]{leaf};
		lcrfNetwork.addNode(leaf);
		for(int i=0;i<inst.size();i++){
			long node = toNode(i, Chunk.get(inst.getOutput().get(i)).getId());
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
	
	public ChunkNetwork compileUnlabeledInstances(int networkId, ChunkInstance inst, LocalNetworkParam param){
		
		long[] allNodes = genericUnlabeledNetwork.getAllNodes();
		long root = toNode_root(inst.size());
		int rootIdx = Arrays.binarySearch(allNodes, root);
		ChunkNetwork lcrfNetwork = new ChunkNetwork(networkId, inst,allNodes,genericUnlabeledNetwork.getAllChildren() , param, rootIdx+1, this);
		return lcrfNetwork;
	}
	
	public void compileUnlabeledInstancesGeneric(){
		ChunkNetwork lcrfNetwork = new ChunkNetwork();
		
		long leaf = toNode_leaf();
		long[] children = new long[]{leaf};
		lcrfNetwork.addNode(leaf);
		for(int i=0;i<_size;i++){
			long[] currentNodes = new long[Chunk.ChunkLabels.size()];
			for(int l=0;l<Chunk.ChunkLabels.size();l++){
				long node = toNode(i,l);
				String currEntity = Chunk.get(l).getForm();
				for(long child: children){
					if(child==-1) continue;
					int[] childArr = NetworkIDMapper.toHybridNodeArray(child);
					String childEntity = Chunk.get(childArr[1]).getForm();
					if(childEntity.startsWith("I")){
						if(currEntity.startsWith("I") && !childEntity.substring(1).equals(currEntity.substring(1))) continue;
						if(currEntity.startsWith("E") && !childEntity.substring(1).equals(currEntity.substring(1))) continue;
						if(IOBESencoding && (currEntity.startsWith("O") || currEntity.startsWith("B") || currEntity.startsWith("S")  ) ) continue;
					}else if(childEntity.equals("O")){
						if(currEntity.startsWith("I") || currEntity.startsWith("E")) continue;
						
					}else if(childEntity.startsWith("B")){
						if(currEntity.startsWith("I")  && !childEntity.substring(1).equals(currEntity.substring(1)) ) continue;
						if(currEntity.startsWith("E")  && !childEntity.substring(1).equals(currEntity.substring(1)) ) continue;
						if(IOBESencoding && ( currEntity.equals("O") || currEntity.equals("B") || currEntity.equals("S") )  ) continue;
						
					}else if(childEntity.startsWith("E")){
						if(currEntity.startsWith("I") || currEntity.startsWith("E")) continue;
						
					}else if(childEntity.startsWith("S")){
						if(currEntity.startsWith("I") || currEntity.startsWith("E")) continue;
					}else{
						throw new RuntimeException("Unknown type "+childEntity+" in network compilation");
					}
					
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

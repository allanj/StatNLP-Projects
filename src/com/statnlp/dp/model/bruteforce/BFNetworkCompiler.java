package com.statnlp.dp.model.bruteforce;

import java.util.ArrayList;
import java.util.Arrays;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class BFNetworkCompiler extends NetworkCompiler{

	private static final long serialVersionUID = -2388666010977956073L;

	public enum NODE_TYPES {LEAF, depInLinear ,NODE,ROOT};
	public int _size = 128;
	public BFNetwork genericUnlabeledNetwork;
	
	public BFNetworkCompiler(){
		this.compileUnlabeledInstancesGeneric();
	}
	
	@Override
	public BFNetwork compile(int networkId, Instance inst, LocalNetworkParam param) {
		// TODO Auto-generated method stub
		BFInstance lcrfInstance = (BFInstance)inst;
		if(lcrfInstance.isLabeled())
			return compileLabeledInstances(networkId, lcrfInstance, param);
		else
			return compileUnlabeledInstances(networkId, lcrfInstance, param);
	}
	
	public long toNode_leaf(){
		int[] arr = new int[]{0,Entity.ENTS.size(),0,0,NODE_TYPES.LEAF.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	public long toNode_linear(int pos, int tag_id){
		int[] arr = new int[]{pos, tag_id,0,0,NODE_TYPES.NODE.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	} 
	
	private long toNodeDepInLinear(int currIndex, int headIndex){
		return NetworkIDMapper.toHybridNodeID(new int[]{currIndex, headIndex, 0,0, NODE_TYPES.depInLinear.ordinal()});
	}
	
	public long toNode_root(int size){
		int[] arr = new int[]{size-1, Entity.ENTS.size()+this._size,0,0,NODE_TYPES.ROOT.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}

	@Override
	public BFInstance decompile(Network network) {
		BFNetwork lcrfNetwork = (BFNetwork)network;
		BFInstance lcrfInstance = (BFInstance)lcrfNetwork.getInstance();
		BFInstance result = lcrfInstance.duplicate();
		
		String[] leaves = new String[lcrfInstance.size()];
		int[] heads = new int[lcrfInstance.size()];
		long root = toNode_root(lcrfInstance.size());
		int rootIdx = Arrays.binarySearch(lcrfNetwork.getAllNodes(),root);
		//System.err.println(rootIdx+" final score:"+network.getMax(rootIdx));
		
		//up to the leave, that's why to lcrfInstance.size -1 
		for(int i=0;i<lcrfInstance.size()-1;i++){
			int child_k = lcrfNetwork.getMaxPath(rootIdx)[0];
			long child = lcrfNetwork.getNode(child_k);
			int[] child_1_arr = NetworkIDMapper.toHybridNodeArray(child);
			int pos = child_1_arr[0];
			int tagID = child_1_arr[1];
			//System.err.println(Arrays.toString(child_1_arr));
			leaves[pos] = Entity.ENTS_INDEX.get(tagID).getForm();
			
			int child_2  = lcrfNetwork.getMaxPath(rootIdx)[1];
			long child2 = lcrfNetwork.getNode(child_2);
			int headIndex = NetworkIDMapper.toHybridNodeArray(child2)[1];
			heads[pos] = headIndex;
					
			rootIdx = child_k;
		}
		heads[0] = -1;
		leaves[0] = "O";
		ArrayList<String> res = new ArrayList<String>();
		String prev = "O";
		res.add("O");
		for(int i=1;i< leaves.length;i++){
			res.add(leaves[i]);
//			String current = leaves[i];
//			if(current.equals(prev)){
//				if(prev.equals("O"))
//					res.add("O");
//				else 
//					res.add("I-"+current);
//			}else{
//				if(current.equals("O"))
//					res.add("O");
//				else res.add("B-"+current);
//			}
//			prev = current;
		}
		result.setPredEntities(res);
		result.setPredHeads(heads);
		return result;
	}
	

	public BFNetwork compileLabeledInstances(int networkId, BFInstance inst, LocalNetworkParam param){
		BFNetwork lcrfNetwork = new BFNetwork(networkId, inst,param);
		
		long leaf = toNode_leaf();
		long[] children = new long[]{leaf};
		lcrfNetwork.addNode(leaf);
		BFInstance bfInst = (BFInstance)lcrfNetwork.getInstance();
		for(int i=1;i<lcrfNetwork.getInstance().size();i++){
			
			String entity = bfInst.getInput().get(i).getEntity();
			//if(entity.length()>1) entity = entity.substring(2);
			long node = toNode_linear(i, Entity.get(entity).getId());
			if(i==bfInst.getInput().get(i).getHeadIndex()){
				throw new RuntimeException(" current index and the head index cannot be the same");
			}
			long depInLinear = toNodeDepInLinear(i, bfInst.getInput().get(i).getHeadIndex());
			lcrfNetwork.addNode(node);
			lcrfNetwork.addNode(depInLinear);
			long[] currentNodes = new long[]{node, depInLinear};
			lcrfNetwork.addEdge(node, children);
			children = currentNodes;
		}
		long root = toNode_root(bfInst.size());
		lcrfNetwork.addNode(root);
		lcrfNetwork.addEdge(root, children);
		
		lcrfNetwork.finalizeNetwork();
		lcrfNetwork.iniRemoveArr();
//		if(!genericUnlabeledNetwork.contains(lcrfNetwork)){
//			System.err.println("wrong");
//		}
		return lcrfNetwork;
	}
	
	public BFNetwork compileUnlabeledInstances(int networkId, BFInstance inst, LocalNetworkParam param){
		
		long[] allNodes = genericUnlabeledNetwork.getAllNodes();
		long root = toNode_root(inst.size());
		int rootIdx = Arrays.binarySearch(allNodes, root);
		BFNetwork lcrfNetwork = new BFNetwork(networkId, inst,allNodes,genericUnlabeledNetwork.getAllChildren() , param, rootIdx+1);
		return lcrfNetwork;
	}
	
	public void compileUnlabeledInstancesGeneric(){
		BFNetwork lcrfNetwork = new BFNetwork();
		
		long linearLeaf = toNode_leaf();
		long[] children = new long[]{linearLeaf};
		lcrfNetwork.addNode(linearLeaf);
		long[] currentNodes = new long[Entity.ENTS.size()];
		
		for(int i=1;i<_size;i++){
			currentNodes = new long[Entity.ENTS.size()];
			for(int l=0;l<Entity.ENTS.size();l++){
				long node = toNode_linear(i, l);
				currentNodes[l] = node;
				lcrfNetwork.addNode(node);
				String paEntity = Entity.get(l).getForm();
				
				
				for(long child: children){
					if(child==-1) continue;
					if(i==1) 
						lcrfNetwork.addEdge(node, new long[]{child});
					else{
						int[] childArr = NetworkIDMapper.toHybridNodeArray(child);
						String childEntity = Entity.get(childArr[1]).getForm();
						if(childEntity.startsWith("B") && paEntity.startsWith("I") && !childEntity.substring(2).equals(paEntity.substring(2))) continue;
						if(childEntity.startsWith("I-") && paEntity.startsWith("I-") && !childEntity.substring(2).equals(paEntity.substring(2))) continue;
						//if(entities[childArr[1]].startsWith("I-") && entities[l].startsWith("B-") && entities[childArr[1]].substring(2).equals(entities[l].substring(2))) continue;
						if(childEntity.equals("O") && paEntity.startsWith("I-")) continue;
						
						for(int headIdx = 0; headIdx < this._size;headIdx++){
							if(headIdx==(i-1)) continue;
							long depInLinear = toNodeDepInLinear(i-1, headIdx);
							lcrfNetwork.addNode(depInLinear);
							lcrfNetwork.addEdge(node, new long[]{child, depInLinear});
						}
					}
					
				}
				
			}
			
			long root = toNode_root(i+1);
			lcrfNetwork.addNode(root);
			for(long child:currentNodes){
				if(child==-1) continue;
				for(int headIdx = 0; headIdx < this._size;headIdx++){
					if(headIdx==i) continue;
					long depInLinear = toNodeDepInLinear(i, headIdx);
					lcrfNetwork.addNode(depInLinear);
					lcrfNetwork.addEdge(root, new long[]{child, depInLinear});
				}
			}
			children = currentNodes;
			
		}
		lcrfNetwork.finalizeNetwork();
		genericUnlabeledNetwork =  lcrfNetwork;
		System.err.println("total number of nodes:"+genericUnlabeledNetwork.getAllNodes().length);
	}
	
	
	
}

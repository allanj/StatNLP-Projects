package com.statnlp.projects.nndcrf.factorialCRFs;

import java.util.ArrayList;
import java.util.Arrays;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class TFNetworkCompiler extends NetworkCompiler{

	private static final long serialVersionUID = -2388666010977956073L;

	public enum NODE_TYPES {LEAF, 
		ENODE,   //node type in NE-chain 
		TNODE,   //node type in PoS-chain
		ROOT};
	public int _size;
	public TFNetwork genericUnlabeledNetwork;
	
	public TFNetworkCompiler(){
		this._size = 2;
		this.compileUnlabeledInstancesGeneric();
	}
	
	@Override
	public TFNetwork compile(int networkId, Instance inst, LocalNetworkParam param) {
		// TODO Auto-generated method stub
		TFInstance lcrfInstance = (TFInstance)inst;
		if(lcrfInstance.isLabeled())
			return compileLabeledInstances(networkId, lcrfInstance, param);
		else
			return compileUnlabeledInstances(networkId, lcrfInstance, param);
	}
	
	public long toNode_leaf(){
		int[] arr = new int[]{0, NODE_TYPES.LEAF.ordinal() ,Entity.ENTS.size()+Tag.TAGS.size(),0,0};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	public long toNode_t(int pos, int tag_id){
		int[] arr = new int[]{pos+1, NODE_TYPES.TNODE.ordinal(), tag_id,0,0};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	public long toNode_e(int pos, int entity_id){
		int[] arr = new int[]{pos+1, NODE_TYPES.ENODE.ordinal(), entity_id,0,0};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	public long toNode_root(int size){
		int[] arr = new int[]{size+1,NODE_TYPES.ROOT.ordinal(),Entity.ENTS.size()+Tag.TAGS.size(),0,0};
		return NetworkIDMapper.toHybridNodeID(arr);
	}

	@Override
	public TFInstance decompile(Network network) {
		//remember to disable some nodes first
		int structure = network.getStructure();
		if(structure==0){
			return decompileNE(network);
		}else{
			return decompilePOS(network);
		}
		//System.err.println(rootIdx+" final score:"+network.getMax(rootIdx));
	}
	
	public TFInstance decompileNE(Network network){
		TFNetwork lcrfNetwork = (TFNetwork)network;
		TFInstance lcrfInstance = (TFInstance)lcrfNetwork.getInstance();
		TFInstance result = lcrfInstance.duplicate();
		ArrayList<String> nePred = new ArrayList<String>();
		long root = toNode_root(lcrfInstance.size());
//		System.out.println(lcrfInstance.size()+" "+ Arrays.toString(NetworkIDMapper.toHybridNodeArray(root)));
		int rootIdx = Arrays.binarySearch(lcrfNetwork.getAllNodes(),root);
		
		for(int i=0;i<lcrfInstance.size();i++){
			int child_k = lcrfNetwork.getMaxPath(rootIdx)[0];
			long child = lcrfNetwork.getNode(child_k);
			rootIdx = child_k;
			int[] childArr = NetworkIDMapper.toHybridNodeArray(child);
			int eId = childArr[2];
//			System.err.println(eId + " node type:"+NODE_TYPES.values()[childArr[1]]);
			nePred.add(0, Entity.get(eId).getForm());
		}
		result.setEntityPredictons(nePred);
		lcrfInstance.setEntityPredictons(nePred);
		return result;
	}
	
	public TFInstance decompilePOS(Network network){
		TFNetwork lcrfNetwork = (TFNetwork)network;
		TFInstance lcrfInstance = (TFInstance)lcrfNetwork.getInstance();
		TFInstance result = lcrfInstance.duplicate();
		ArrayList<String> posPrediction = new ArrayList<String>();
		long root = toNode_root(lcrfInstance.size());
		int rootIdx = Arrays.binarySearch(lcrfNetwork.getAllNodes(),root);
		
		for(int i=0;i<lcrfInstance.size();i++){
			int child_k = lcrfNetwork.getMaxPath(rootIdx)[0];
			long child = lcrfNetwork.getNode(child_k);
			rootIdx = child_k;
			int[] childArr = NetworkIDMapper.toHybridNodeArray(child);
			int tId = childArr[2];
			posPrediction.add(0, Tag.get(tId).getForm());
		}
		result.setTagPredictons(posPrediction);
		return result;
	}

	public TFNetwork compileLabeledInstances(int networkId, TFInstance inst, LocalNetworkParam param){
		TFNetwork lcrfNetwork = new TFNetwork(networkId, inst,param);
		long leaf = toNode_leaf();
		lcrfNetwork.addNode(leaf);
		long[] e_children = new long[]{leaf};
		long[] t_children = new long[]{leaf};
		
		for(int i=0;i<inst.size();i++){
			
			int entityId = Entity.ENTS.get(inst.getOutput().get(i)).getId();
			int posId = Tag.TAGS.get(inst.getInput().get(i).getTag()).getId();
			long e_node = toNode_e(i,entityId);
			long tag_node = toNode_t(i,posId);
			lcrfNetwork.addNode(e_node);
			lcrfNetwork.addNode(tag_node);
			
			long[] current_e_Nodes = new long[]{e_node};
			long[] current_t_Nodes = new long[]{tag_node};
			lcrfNetwork.addEdge(e_node, e_children);
			lcrfNetwork.addEdge(tag_node, t_children);
			
			e_children = current_e_Nodes;
			t_children = current_t_Nodes;
		}
		long root = toNode_root(inst.size());
		lcrfNetwork.addNode(root);
		lcrfNetwork.addEdge(root, e_children);
		lcrfNetwork.addEdge(root, t_children);
		lcrfNetwork.finalizeNetwork();
		
		if(!genericUnlabeledNetwork.contains(lcrfNetwork)){
			System.err.println("wrong");
		}
		return lcrfNetwork;
	}
	
	public TFNetwork compileUnlabeledInstances(int networkId, TFInstance inst, LocalNetworkParam param){
		
		long[] allNodes = genericUnlabeledNetwork.getAllNodes();
		long root = toNode_root(inst.size());
		int rootIdx = Arrays.binarySearch(allNodes, root);
		TFNetwork lcrfNetwork = new TFNetwork(networkId, inst,allNodes,genericUnlabeledNetwork.getAllChildren() , param, rootIdx+1);
		return lcrfNetwork;
	}
	
	public void compileUnlabeledInstancesGeneric(){
		TFNetwork lcrfNetwork = new TFNetwork();
		long leaf = toNode_leaf();
		lcrfNetwork.addNode(leaf);
		for(int i=0;i<_size;i++){
			long root = toNode_root(i+1);
			lcrfNetwork.addNode(root);
			for(int l=0;l<Entity.ENTS.size();l++){
				//if(i==0 && Entity.ENTS_INDEX.get(l).getForm().startsWith("I")){ continue;}
				long enode = toNode_e(i, l);
//				String currentEntity = Entity.ENTS_INDEX.get(l).getForm();
				if(i==0){
					lcrfNetwork.addNode(enode);
					lcrfNetwork.addEdge(enode, new long[]{leaf});
				}else{
					for(int prevL=0; prevL<Entity.ENTS.size(); prevL++){
//						String prevEntity = Entity.ENTS_INDEX.get(prevL).getForm();
						//if(prevEntity.equals("O") && currentEntity.startsWith("I")) continue;
						//some constraints?
						long prev_e_node = toNode_e(i-1, prevL);
						if(lcrfNetwork.contains(prev_e_node)){
							lcrfNetwork.addNode(enode);
							lcrfNetwork.addEdge(enode, new long[]{prev_e_node});
						}
					}
				}
				if(lcrfNetwork.contains(enode)){
					lcrfNetwork.addEdge(root, new long[]{enode});
				}
			}
			
			for(int t=0;t<Tag.TAGS.size();t++){
				long tnode = toNode_t(i, t);
				if(i==0){
					lcrfNetwork.addNode(tnode);
					lcrfNetwork.addEdge(tnode, new long[]{leaf});
				}else{
					for(int prevT=0;prevT<Tag.TAGS.size();prevT++){
						long prev_t_node = toNode_t(i-1, prevT);
						if(lcrfNetwork.contains(prev_t_node)){
							lcrfNetwork.addNode(tnode); 
							lcrfNetwork.addEdge(tnode, new long[]{prev_t_node});
						}
					}
				}
				if(lcrfNetwork.contains(tnode)){
					lcrfNetwork.addEdge(root, new long[]{tnode});
				}
				
			}
		}
		lcrfNetwork.finalizeNetwork();
		genericUnlabeledNetwork =  lcrfNetwork;
		System.err.println(genericUnlabeledNetwork.getAllNodes().length+" nodes..");
	}


	
	
}

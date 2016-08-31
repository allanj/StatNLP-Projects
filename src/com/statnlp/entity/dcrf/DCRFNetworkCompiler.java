package com.statnlp.entity.dcrf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class DCRFNetworkCompiler extends NetworkCompiler{

	private static final long serialVersionUID = -2388666010977956073L;

	public enum NODE_TYPES {entLEAF, tagLEAF, tagNODE, entNODE,ROOT};
	public int _size;
	public DCRFNetwork genericUnlabeledNetwork;
	public DCRFViewer viewer;
	
	public DCRFNetworkCompiler(DCRFViewer viewer){
		this._size = 150;
		this.compileUnlabeledInstancesGeneric();
//		this.debugUnlabeledInstancesGeneric();
		//viewer.visualizeNetwork(genericUnlabeledNetwork, null, "unlabel network");
		this.viewer = viewer;
	}
	
	@Override
	public DCRFNetwork compile(int networkId, Instance inst, LocalNetworkParam param) {
		// TODO Auto-generated method stub
		DCRFInstance lcrfInstance = (DCRFInstance)inst;
		if(lcrfInstance.isLabeled())
			return compileLabeledInstances(networkId, lcrfInstance, param);
		else
			return compileUnlabeledInstances(networkId, lcrfInstance, param);
	}
	
	public long toNode_e_leaf(){
		int[] arr = new int[]{0,0,0,0,NODE_TYPES.entLEAF.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	public long toNode_t_leaf(){
		int[] arr = new int[]{0,0,0,0,NODE_TYPES.tagLEAF.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	public long toNode_e(int pos, int tag_id){
		int[] arr = new int[]{pos+1,tag_id,0,0,NODE_TYPES.entNODE.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	public long toNode_t(int pos, int tag_id){
		int[] arr = new int[]{pos+1,tag_id,0,0,NODE_TYPES.tagNODE.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	public long toNode_root(int size){
		int[] arr = new int[]{size+1, 0, 0, 0, NODE_TYPES.ROOT.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}

	@Override
	public DCRFInstance decompile(Network network) {
		return decompileByMarginal(network);
	}

	public DCRFInstance decompileByMarginal(Network network){
		DCRFNetwork lcrfNetwork = (DCRFNetwork)network;
		DCRFInstance lcrfInstance = (DCRFInstance)lcrfNetwork.getInstance();
		DCRFInstance result = lcrfInstance.duplicate();
		ArrayList<String> entityPrediction = new ArrayList<String>();
		ArrayList<String> tagPrediction = new ArrayList<String>();
		for(int i=0;i<lcrfInstance.size();i++){
			int maxEId = -1;
			double maxMarginal = Double.NEGATIVE_INFINITY;
			for(int e=0;e<DEntity.ENTS.size();e++){
				long node = toNode_e(i, e);
				int idx = Arrays.binarySearch(lcrfNetwork.getAllNodes(), node);
				if(idx>=0){
					double marginal = network.getMarginal(idx);
//					if(i==11){
//						System.err.println(lcrfInstance.getInput().get(i).getName()+","+Entity.ENTS_INDEX.get(e).getForm()+", marginal:"+marginal);
//					}
					if(marginal> maxMarginal){
						maxEId = e;
						maxMarginal = marginal;
					}
				}
				
			}
			entityPrediction.add(DEntity.ENTS_INDEX.get(maxEId).getForm());
			
			int maxTagId = -1;
			maxMarginal = Double.NEGATIVE_INFINITY;
			for(int t=0;t<Tag.TAGS.size();t++){
				long node = toNode_t(i, t);
				int idx = Arrays.binarySearch(lcrfNetwork.getAllNodes(), node);
				if(idx>=0){
					double marginal = network.getMarginal(idx);
//					if(i==11){
//						System.err.println(lcrfInstance.getInput().get(i).getName()+","+Tag.TAGS_INDEX.get(t).getForm()+", marginal:"+marginal);
//					}
					if(marginal> maxMarginal){
						maxTagId = t;
						maxMarginal = marginal;
					}
				}
			}
			tagPrediction.add(Tag.TAGS_INDEX.get(maxTagId).getForm());
		}
		result.setEntityPrediction(entityPrediction);
		result.setTagPrediction(tagPrediction);
		return result;
	}
	
	public DCRFNetwork compileLabeledInstances(int networkId, DCRFInstance inst, LocalNetworkParam param){
		DCRFNetwork lcrfNetwork = new DCRFNetwork(networkId, inst,param);
		long eleaf = toNode_e_leaf();
		long tleaf = toNode_t_leaf();
		long[] children = new long[]{eleaf, tleaf};
		lcrfNetwork.addNode(eleaf);
		lcrfNetwork.addNode(tleaf);
		for(int i=0;i<inst.size();i++){
			
			long enode = toNode_e(i, DEntity.ENTS.get(inst.getOutput().get(i)).getId());
			long tnode = toNode_t(i, Tag.TAGS.get(inst.getInput().get(i).getTag()).getId());
			lcrfNetwork.addNode(enode);
			lcrfNetwork.addNode(tnode);
			long[] currentNodes = new long[]{enode, tnode};
			lcrfNetwork.addEdge(enode, children);
			lcrfNetwork.addEdge(tnode, children);
			children = currentNodes;
		}
		long root = toNode_root(inst.size());
		lcrfNetwork.addNode(root);
		lcrfNetwork.addEdge(root, children);
		
		lcrfNetwork.finalizeNetwork();
		
//		if(!genericUnlabeledNetwork.contains(lcrfNetwork)){
//			System.err.println("wrong");
//		}
		//viewer.visualizeNetwork(lcrfNetwork, null, "label network");
		return lcrfNetwork;
	}
	
	public DCRFNetwork compileUnlabeledInstances(int networkId, DCRFInstance inst, LocalNetworkParam param){
		
		long[] allNodes = genericUnlabeledNetwork.getAllNodes();
		long root = toNode_root(inst.size());
		int rootIdx = Arrays.binarySearch(allNodes, root);
		DCRFNetwork lcrfNetwork = new DCRFNetwork(networkId, inst,allNodes,genericUnlabeledNetwork.getAllChildren() , param, rootIdx+1);
		return lcrfNetwork;
	}
	
	public void compileUnlabeledInstancesGeneric(){
		DCRFNetwork lcrfNetwork = new DCRFNetwork();
		long eleaf = toNode_e_leaf();
		long tleaf = toNode_t_leaf();
		long[] leaves = new long[]{eleaf, tleaf};
		lcrfNetwork.addNode(tleaf);
		lcrfNetwork.addNode(eleaf);
		for(int i=0;i<_size;i++){
			for(int l=0;l< DEntity.ENTS.size();l++){
				if(i==0 && DEntity.ENTS_INDEX.get(l).getForm().startsWith("I")) continue;
				String currentEntity = DEntity.ENTS_INDEX.get(l).getForm();
				long enode = toNode_e(i,l);
				if(i==0){
					lcrfNetwork.addNode(enode);
					lcrfNetwork.addEdge(enode, leaves);
				}else{
					for(int prevL=0; prevL<DEntity.ENTS.size(); prevL++){
						String prevEntity = DEntity.ENTS_INDEX.get(prevL).getForm();
						//if(entities[childArr[1]].startsWith("I-") && entities[l].startsWith("B-") && entities[childArr[1]].substring(2).equals(entities[l].substring(2))) continue;
						if(prevEntity.equals("O") && currentEntity.startsWith("I")) continue;
						long prev_e_node = toNode_e(i-1, prevL);
						for(int prevT=0;prevT<Tag.TAGS.size();prevT++){
							long prev_t_node = toNode_t(i-1, prevT);
							if(lcrfNetwork.contains(prev_e_node) && lcrfNetwork.contains(prev_t_node)){
								lcrfNetwork.addNode(enode);
								lcrfNetwork.addEdge(enode, new long[]{prev_e_node, prev_t_node});
								
							}
						}
					}
				}
			}
			for(int t=0;t<Tag.TAGS.size();t++){
				long tnode = toNode_t(i,t);
				if(i==0){
					lcrfNetwork.addNode(tnode);
					lcrfNetwork.addEdge(tnode, leaves);
				}else{
					for(int prevL=0; prevL<DEntity.ENTS.size(); prevL++){
						for(int prevT=0;prevT<Tag.TAGS.size();prevT++){
							long prev_e_node = toNode_e(i-1, prevL);
							long prev_t_node = toNode_t(i-1, prevT);
							if(lcrfNetwork.contains(prev_e_node) && lcrfNetwork.contains(prev_t_node)){
								lcrfNetwork.addNode(tnode);
								lcrfNetwork.addEdge(tnode, new long[]{prev_e_node, prev_t_node});
								
							}
						}
					}
				}
			}
			
			long root = toNode_root(i+1);
			lcrfNetwork.addNode(root);
			for(int prevL=0; prevL<DEntity.ENTS.size(); prevL++){
				for(int prevT=0;prevT<Tag.TAGS.size();prevT++){
					long prev_e_node = toNode_e(i, prevL);
					long prev_t_node = toNode_t(i, prevT);
					if(lcrfNetwork.contains(prev_e_node) && lcrfNetwork.contains(prev_t_node)){
						lcrfNetwork.addEdge(root, new long[]{prev_e_node, prev_t_node});
						
					}
				}
			}
			
		}
		lcrfNetwork.finalizeNetwork();
		genericUnlabeledNetwork =  lcrfNetwork;
	}
	
	
	public void debugUnlabeledInstancesGeneric(){
		DCRFNetwork lcrfNetwork = new DCRFNetwork();
		long eleaf = toNode_e_leaf();
		long tleaf = toNode_t_leaf();
		long[] leaves = new long[]{eleaf, tleaf};
		lcrfNetwork.addNode(tleaf);
		lcrfNetwork.addNode(eleaf);
		String[] es = new String[]{"B", "O","B","I","O",
				"O","O","O","O","B",
				"I","I","O","B","I",
				"O","B","O","O","O",
				"B","B","O","O","O",
				"O","B","I","I"};
		for(int i=0;i<_size;i++){
			for(int l=0;l< DEntity.ENTS.size();l++){
				if(i==0 && DEntity.ENTS_INDEX.get(l).getForm().startsWith("I")) continue;
				String currentEntity = DEntity.ENTS_INDEX.get(l).getForm();
				if(!currentEntity.equals(es[i])) continue;
				long enode = toNode_e(i,l);
				if(i==0){
					lcrfNetwork.addNode(enode);
					lcrfNetwork.addEdge(enode, leaves);
				}else{
					for(int prevL=0; prevL<DEntity.ENTS.size(); prevL++){
						String prevEntity = DEntity.ENTS_INDEX.get(prevL).getForm();
						//if(entities[childArr[1]].startsWith("I-") && entities[l].startsWith("B-") && entities[childArr[1]].substring(2).equals(entities[l].substring(2))) continue;
						if(prevEntity.equals("O") && currentEntity.startsWith("I")) continue;
						long prev_e_node = toNode_e(i-1, prevL);
						for(int prevT=0;prevT<Tag.TAGS.size();prevT++){
							long prev_t_node = toNode_t(i-1, prevT);
							if(lcrfNetwork.contains(prev_e_node) && lcrfNetwork.contains(prev_t_node)){
								lcrfNetwork.addNode(enode);
								lcrfNetwork.addEdge(enode, new long[]{prev_e_node, prev_t_node});
								
							}
						}
					}
				}
			}
			for(int t=0;t<Tag.TAGS.size();t++){
				if(!Tag.TAGS_INDEX.get(t).getForm().equals("NN")) continue;
				long tnode = toNode_t(i,t);
				if(i==0){
					lcrfNetwork.addNode(tnode);
					lcrfNetwork.addEdge(tnode, leaves);
				}else{
					for(int prevL=0; prevL<DEntity.ENTS.size(); prevL++){
						for(int prevT=0;prevT<Tag.TAGS.size();prevT++){
							long prev_e_node = toNode_e(i-1, prevL);
							long prev_t_node = toNode_t(i-1, prevT);
							if(lcrfNetwork.contains(prev_e_node) && lcrfNetwork.contains(prev_t_node)){
								lcrfNetwork.addNode(tnode);
								lcrfNetwork.addEdge(tnode, new long[]{prev_e_node, prev_t_node});
								
							}
						}
					}
				}
			}
			
			long root = toNode_root(i+1);
			lcrfNetwork.addNode(root);
			for(int prevL=0; prevL<DEntity.ENTS.size(); prevL++){
				for(int prevT=0;prevT<Tag.TAGS.size();prevT++){
					long prev_e_node = toNode_e(i, prevL);
					long prev_t_node = toNode_t(i, prevT);
					if(lcrfNetwork.contains(prev_e_node) && lcrfNetwork.contains(prev_t_node)){
						lcrfNetwork.addEdge(root, new long[]{prev_e_node, prev_t_node});
						
					}
				}
			}
			
		}
		lcrfNetwork.finalizeNetwork();
		genericUnlabeledNetwork =  lcrfNetwork;
	}
	
	
}

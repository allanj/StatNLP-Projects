package com.statnlp.dp.model.var;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkIDMapper;

/**
 * Double check this one, since the actual starting point is index 1
 * @author allanjie
 *
 */
public class LinearCompiler{

	private static final long serialVersionUID = -2388666010977956073L;

	public enum NODE_TYPES {LEAF,NODE,ROOT};
	public int _size;
	public LinearNetwork genericUnlabeledNetwork;
	private HashMap<String, Integer> entityMap;
	private String[] entities;
	private EntityViewer eViewer;
	
	public LinearCompiler(HashMap<String, Integer> entityMap,String[] entities,EntityViewer eViewer){
		this.entityMap = entityMap;
		this.entities = entities;
		this._size = 150;

		this.eViewer = eViewer;
		this.compileUnlabeledInstancesGeneric();
		this.eViewer.nothing();
	}
	
	public LinearNetwork compile(int networkId, Instance inst, LocalNetworkParam param) {
		// TODO Auto-generated method stub
		VarInstance lcrfInstance = (VarInstance)inst;
		if(lcrfInstance.isLabeled())
			return compileLabeledInstances(networkId, lcrfInstance, param);
		else
			return compileUnlabeledInstances(networkId, lcrfInstance, param);
	}
	
	public long toNode_leaf(){
		int[] arr = new int[]{0,0,0,0,NODE_TYPES.LEAF.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	public long toNode(int pos, int tag_id, int direction){
		int[] arr = new int[]{pos,direction,tag_id,0,NODE_TYPES.NODE.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	public long toNode_root(int size){
		int[] arr = new int[]{size+1,2,entityMap.get("O"),0,NODE_TYPES.ROOT.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}

	public LinearNetwork compileLabeledInstances(int networkId, VarInstance inst, LocalNetworkParam param){
		LinearNetwork lcrfNetwork = new LinearNetwork(networkId, inst,param);
		long leaf = toNode_leaf();
		long[] children = new long[]{leaf};
		lcrfNetwork.addNode(leaf);
		String prevEntity = "O";
		for(int i=1;i<inst.size();i++){
			
			String output = inst.getEntityOutput()[i].length()>2? inst.getEntityOutput()[i].substring(2) : inst.getEntityOutput()[i];
			String after = i>=inst.size()-1? null: inst.getEntityOutput()[i+1].length()>2? inst.getEntityOutput()[i+1].substring(2) : inst.getEntityOutput()[i+1] ;
			long nodeL = -1;
			long nodeR = -1;
			if(output.equals(prevEntity) ){
				nodeL = toNode(i,entityMap.get(output),0);
				if(output.equals("O"))
					nodeR = toNode(i,entityMap.get(output),1);
				else if(i<inst.size()-1 && after.equals(output))
					nodeR = toNode(i,entityMap.get(output),1);
				else nodeR = toNode(i,entityMap.get("O"),1);
			}else{
				nodeL = toNode(i,entityMap.get(output),0);
				nodeR = toNode(i,entityMap.get(output),1);
				if(!output.equals("O")){
					nodeL = toNode(i,entityMap.get("O"),0);
					nodeR = toNode(i,entityMap.get(output),1);
				}
			}
			nodeL = toNode(i,entityMap.get(output),0);
			nodeR = toNode(i,entityMap.get(output),1);
			lcrfNetwork.addNode(nodeL);
			lcrfNetwork.addNode(nodeR);
			lcrfNetwork.addEdge(nodeR, new long[]{nodeL});
			long[] currentNodes = new long[]{nodeR};
			lcrfNetwork.addEdge(nodeL, children);
			children = currentNodes;
			prevEntity = entities[NetworkIDMapper.toHybridNodeArray(nodeR)[2]];
		}
		long root = toNode_root(inst.size());
		lcrfNetwork.addNode(root);
		lcrfNetwork.addEdge(root, children);
		
		lcrfNetwork.finalizeNetwork();
		//eViewer.visualizeNetwork(lcrfNetwork, null, "Labeled Network");
		return lcrfNetwork;
	}
	
	public LinearNetwork compileUnlabeledInstances(int networkId, VarInstance inst, LocalNetworkParam param){
		
		long[] allNodes = genericUnlabeledNetwork.getAllNodes();
		long root = toNode_root(inst.size());
		int rootIdx = Arrays.binarySearch(allNodes, root);
		LinearNetwork lcrfNetwork = new LinearNetwork(networkId, inst,allNodes,genericUnlabeledNetwork.getAllChildren() , param, rootIdx+1);
		return lcrfNetwork;
	}
	
	public void compileUnlabeledInstancesGeneric(){
		LinearNetwork lcrfNetwork = new LinearNetwork();
		
		long leaf = toNode_leaf();
		long[] children = new long[]{leaf};
		lcrfNetwork.addNode(leaf);
		for(int i=1;i<_size;i++){
			long[] currentNodes = new long[entities.length];
			
			//different positions
			for(int l=0;l<entities.length;l++){
				//if(i==0 && !entities[l].equals("O")) continue;
				long node = toNode(i, l, 0);
				lcrfNetwork.addNode(node);
				for(long child: children){
					if(child==-1) continue;
					lcrfNetwork.addEdge(node, new long[]{child});
				}
			}
			
			//same position
			for(int r=0;r<entities.length;r++){
				long nodeR = toNode(i, r, 1);
				for(int l=0;l<entities.length;l++){
					long nodeL = toNode(i, l, 0);
					if(!entities[r].equals(entities[l]) ) continue;
					//if(!entities[r].equals(entities[l]) && !entities[l].equals("O") && !entities[r].equals("O")) continue;
					if(lcrfNetwork.contains(nodeL)){
						lcrfNetwork.addNode(nodeR);
						currentNodes[r] = nodeR;
						lcrfNetwork.addEdge(nodeR, new long[]{nodeL});
					}
				}
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
		//eViewer.visualizeNetwork(lcrfNetwork, null, "UnLabeled Network");
		genericUnlabeledNetwork =  lcrfNetwork;
		
	}
	

	public void decompile(Network network, VarInstance result) {
		
		LinearNetwork lcrfNetwork = (LinearNetwork)network;
		VarInstance lcrfInstance = (VarInstance)lcrfNetwork.getInstance();
//		VarInstance result = lcrfInstance.duplicate();
		
		ArrayList<String> prediction = new ArrayList<String>();
		
		
		long root = toNode_root(lcrfInstance.size());
		int rootIdx = Arrays.binarySearch(lcrfNetwork.getAllNodes(),root);
		//System.err.println(rootIdx+" final score:"+network.getMax(rootIdx));
		for(int i=0;i<lcrfInstance.size()*2;i++){
			int child_k = lcrfNetwork.getMaxPath(rootIdx)[0];
			long child = lcrfNetwork.getNode(child_k);
			rootIdx = child_k;
			int tagID = NetworkIDMapper.toHybridNodeArray(child)[2];
			prediction.add(0, entities[tagID]);
		}
//		System.err.println(prediction.toString());
		ArrayList<String> res = new ArrayList<String>();
		String prev = "O";
		boolean diff = false;
		for(int i=0;i<prediction.size();i = i+2){
			String current = null;
			if(prediction.get(i).equals(prediction.get(i+1)))
				current = prediction.get(i);
			else{
				//System.err.println("Two directions are not the same");
				if(prediction.get(i).equals("O")) current = prediction.get(i+1);
				else{
					//System.err.println("Error");
					diff = true;
					Random rand = new Random(1000);
					current = prediction.get(rand.nextInt(2));
				}
			}
			if(current.equals(prev)){
				if(prev.equals("O"))
					res.add("O");
				else 
					res.add("I-"+current);
			}else{
				if(current.equals("O"))
					res.add("O");
				else res.add("B-"+current);
			}
			prev = current;
		}
		if(diff){
			System.err.println("repeated");
		}
		String[] resArr = new String[res.size()];
		result.setEntityPrediction(res.toArray(resArr));
	}

}

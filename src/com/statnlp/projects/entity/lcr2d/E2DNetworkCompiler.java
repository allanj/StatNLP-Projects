package com.statnlp.projects.entity.lcr2d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkIDMapper;

import edu.stanford.nlp.ling.tokensregex.types.Expressions.AndExpression;

public class E2DNetworkCompiler extends NetworkCompiler{

	private static final long serialVersionUID = -2388666010977956073L;

	public enum NODE_TYPES {LEAF,NODE,ROOT};
	public int _size;
	public E2DNetwork genericUnlabeledNetwork;
	private HashMap<String, Integer> entityMap;
	private String[] entities;
	private EntityViewer eViewer;
	
	public E2DNetworkCompiler(HashMap<String, Integer> entityMap,String[] entities,EntityViewer eViewer){
		this.entityMap = entityMap;
		this.entities = entities;
		this._size = 150;

		this.eViewer = eViewer;
		this.compileUnlabeledInstancesGeneric();
	}
	
	@Override
	public E2DNetwork compile(int networkId, Instance inst, LocalNetworkParam param) {
		// TODO Auto-generated method stub
		E2DInstance lcrfInstance = (E2DInstance)inst;
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
		int[] arr = new int[]{pos+1,direction,tag_id,0,NODE_TYPES.NODE.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	public long toNode_root(int size){
		int[] arr = new int[]{size+1,2,entityMap.get("O"),0,NODE_TYPES.ROOT.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}

	public E2DNetwork compileLabeledInstances(int networkId, E2DInstance inst, LocalNetworkParam param){
		E2DNetwork lcrfNetwork = new E2DNetwork(networkId, inst,param);
		long leaf = toNode_leaf();
		long[] children = new long[]{leaf};
		lcrfNetwork.addNode(leaf);
		String prevEntity = "O";
		for(int i=0;i<inst.size();i++){
			
			String output = inst.getOutput().get(i).length()>2?inst.getOutput().get(i).substring(2):inst.getOutput().get(i) ;
			String after = i>=inst.size()-1? null: inst.getOutput().get(i+1).length()>2?inst.getOutput().get(i+1).substring(2):inst.getOutput().get(i+1) ;
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
//			nodeL = toNode(i,entityMap.get(output),0);
//			nodeR = toNode(i,entityMap.get(output),1);
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
	
	public E2DNetwork compileUnlabeledInstances(int networkId, E2DInstance inst, LocalNetworkParam param){
		
		long[] allNodes = genericUnlabeledNetwork.getAllNodes();
		long root = toNode_root(inst.size());
		int rootIdx = Arrays.binarySearch(allNodes, root);
		E2DNetwork lcrfNetwork = new E2DNetwork(networkId, inst,allNodes,genericUnlabeledNetwork.getAllChildren() , param, rootIdx+1);
		return lcrfNetwork;
	}
	
	public void compileUnlabeledInstancesGeneric(){
		E2DNetwork lcrfNetwork = new E2DNetwork();
		
		long leaf = toNode_leaf();
		long[] children = new long[]{leaf};
		lcrfNetwork.addNode(leaf);
		for(int i=0;i<_size;i++){
			long[] currentNodes = new long[entities.length];
			
			//different positions
			for(int l=0;l<entities.length;l++){
				//this if for  O,person (left,right)
				if(i==0 && !entities[l].equals("O")) continue;
				long node = toNode(i, l, 0);
				lcrfNetwork.addNode(node);
				for(long child: children){
					if(child==-1) continue;
					int[] childArr = NetworkIDMapper.toHybridNodeArray(child);
					String prevR = entities[childArr[2]];
					//this if for  O,person (left,right)
					if(!prevR.equals(entities[l]) && !entities[l].equals("O")) continue;
					lcrfNetwork.addEdge(node, new long[]{child});
				}
			}
			
			//same position
			for(int r=0;r<entities.length;r++){
				long nodeR = toNode(i, r, 1);
				for(int l=0;l<entities.length;l++){
					long nodeL = toNode(i, l, 0);
					//if(!entities[r].equals(entities[l]) ) continue;
					
					//this if for  O,person (left,right)
					if(!entities[r].equals(entities[l]) && !entities[l].equals("O") && !entities[r].equals("O")) continue;
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
	

	@Override
	public E2DInstance decompile(Network network) {
		if(NetworkConfig.MAX_MARGINAL_DECODING){
			return maxMarginalDecompile(network);
		}
		E2DNetwork lcrfNetwork = (E2DNetwork)network;
		E2DInstance lcrfInstance = (E2DInstance)lcrfNetwork.getInstance();
		E2DInstance result = lcrfInstance.duplicate();
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
				if(prediction.get(i).equals("O") || prediction.get(i+1).equals("O")) current = prediction.get(i).equals("O")? prediction.get(i+1):prediction.get(i);
				else{
					System.err.println("Error: "+prediction.get(i)+" and "+prediction.get(i+1));
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
		result.setPrediction(res);
		return result;
	}

	
	public E2DInstance maxMarginalDecompile(Network network){
		E2DNetwork lcrfNetwork = (E2DNetwork)network;
		E2DInstance lcrfInstance = (E2DInstance)lcrfNetwork.getInstance();
		E2DInstance result = lcrfInstance.duplicate();
		ArrayList<String> prediction = new ArrayList<String>();
		
		//System.err.println(rootIdx+" final score:"+network.getMax(rootIdx));
		for(int i=0;i<lcrfInstance.size()*2;i++){
			double max = Double.NEGATIVE_INFINITY;
			int bestLabel = -1;
			for(int l=0;l<entities.length;l++){
				int pos = -1;
				if(i%2==1) pos = (i-1)/2;
				else pos = i/2;
				long node = toNode(pos, l, i%2);
				int nodeIdx = Arrays.binarySearch(lcrfNetwork.getAllNodes(), node);
				if(nodeIdx<0) continue;
				double marginal = lcrfNetwork.getMarginal(nodeIdx);
				if(marginal>max){
					max = marginal;
					bestLabel = l;
				}
			}
			if(bestLabel==-1)
				System.err.println("index:"+i+" sentence len:"+lcrfInstance.size());
			prediction.add(entities[bestLabel]);
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
		result.setPrediction(res);
		return result;
	}
	
}

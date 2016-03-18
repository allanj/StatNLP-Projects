package com.statnlp.example.treecrf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.statnlp.commons.crf.Label;
import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkIDMapper;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;

public class TCRFNetworkCompiler extends NetworkCompiler {

	public enum NODE_TYPES {NODE};
	public List<Label> allLabels;
	public int _size;
	public int root_label_idx;
	public TCRFNetwork genericUnlabeledNetwork;
	public Map<Integer, ArrayList<int[]>> nonTerminalRules;
	public Set<Integer> preTerminals;
	
	public TCRFNetworkCompiler(List<Label> allLabels){
		this.allLabels = allLabels;
		this._size = 50; //??? what does this mean
		
	}
	
	public TCRFNetworkCompiler(List<Label> allLabels, int root_label_idx, Map<Integer, ArrayList<int[]>> nonTerminalRules, Set<Integer> preTerminals){
		this.allLabels = allLabels;
		this._size = 65; 
		this.root_label_idx = root_label_idx;
		this.nonTerminalRules = nonTerminalRules;
		this.preTerminals = preTerminals;
		compileUnlabeledInstanceGeneric();
		
	}
	
	public TCRFNetworkCompiler() {
		// TODO Auto-generated constructor stub
	}

	
//	public long toNode_leaf(int index,int tag_id){
//		int[] arr = new int[]{0,index,tag_id,0,NODE_TYPES.LEAF.ordinal()};
//		return NetworkIDMapper.toHybridNodeID(arr);
//	}
	
	public long toNode(int spanLength, int index, int tag_id){
		int[] arr = new int[]{spanLength,index,tag_id,0,NODE_TYPES.NODE.ordinal()};
		//System.err.println(spanLength+","+index+","+tag_id);
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	/*
	public long toNode_root(int spanLength){
		int[] arr = new int[]{spanLength,0,this.allLabels.size(),0,NODE_TYPE.ROOT.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	*/
	
	@Override
	public Network compile(int networkId, Instance inst, LocalNetworkParam param) {
		TCRFInstance tcrfInst = (TCRFInstance)inst;
//		System.out.println(tcrfInst.getInstanceId()+","+tcrfInst.isLabeled());
		//return compileLabeledInstances(networkId, tcrfInst, param);
		if(tcrfInst.isLabeled())
			return compileLabeledInstances(networkId, tcrfInst, param);
		else{
			return compileUnLabeledInstance(networkId, tcrfInst, param);
		}
	}

	private void findAll(Tree prediction, TCRFNetwork tcrfNetwork, long node, int nodeIdx, boolean isRoot, int left, int right){
		
//		System.err.print("node arr:"+Arrays.toString(NetworkIDMapper.toHybridNodeArray(node)));
//		System.err.println(" left :"+left+" right:"+right);
		int node_tag_id = NetworkIDMapper.toHybridNodeArray(node)[2];
		CoreLabel cl = new CoreLabel();
		cl.setTag(this.allLabels.get(node_tag_id).getTag());
		cl.setValue(this.allLabels.get(node_tag_id).getTag());
		cl.setIndex(node_tag_id);
		prediction.setLabel(cl);
//		System.err.println("Node tag:"+cl.tag());
//		CoreLabel cl2 = (CoreLabel)(prediction.label());
//		System.out.println(cl2.tag());
		if(left==right){
			//must be leaf then do nothing.
			return;
		}else{
//			System.out.println("# of all nodes:"+tcrfNetwork.getAllNodes().length);
//			System.out.println("Node index:"+nodeIdx+"  "+Arrays.toString(tcrfNetwork.getMaxPath(nodeIdx)));
			int child_1_idx = tcrfNetwork.getMaxPath(nodeIdx)[0];
			int child_2_idx = tcrfNetwork.getMaxPath(nodeIdx)[1];
			long child_1 = tcrfNetwork.getNode(child_1_idx);
			long child_2 = tcrfNetwork.getNode(child_2_idx);
			int[] child_1_arr = NetworkIDMapper.toHybridNodeArray(child_1);
			int[] child_2_arr = NetworkIDMapper.toHybridNodeArray(child_2);
//			System.err.println("child 1 arr:"+Arrays.toString(child_1_arr));
//			System.err.println("child 2 arr:"+Arrays.toString(child_2_arr));
			int child_1_tag_id = child_1_arr[2];
			int child_2_tag_id = child_2_arr[2];
			CoreLabel cl_1 = new CoreLabel();
			cl_1.setTag(this.allLabels.get(child_1_tag_id).getTag());
			cl_1.setValue(this.allLabels.get(child_1_tag_id).getTag());
			cl_1.setIndex(child_1_tag_id);
			CoreLabel cl_2 = new CoreLabel();
			cl_2.setTag(this.allLabels.get(child_2_tag_id).getTag());
			cl_2.setValue(this.allLabels.get(child_2_tag_id).getTag());
			cl_2.setIndex(child_2_tag_id);
			LabeledScoredTreeNode childNode_1 = new LabeledScoredTreeNode(cl_1);
			LabeledScoredTreeNode childNode_2 = new LabeledScoredTreeNode(cl_2);
			prediction.addChild(childNode_1);
			prediction.addChild(childNode_2);
			findAll(prediction.getChild(0), tcrfNetwork, child_1,child_1_idx,false,child_1_arr[1],child_1_arr[1]+child_1_arr[0]);
			findAll(prediction.getChild(1), tcrfNetwork, child_2,child_2_idx,false,child_2_arr[1],child_2_arr[1]+child_2_arr[0]);
		}
		
		
		
	}
	
	@Override
	public TCRFInstance decompile(Network network) {
		TCRFNetwork tcrfNetwork = (TCRFNetwork)network;
		TCRFInstance tcrfInstance = (TCRFInstance)tcrfNetwork.getInstance();
//		System.out.println("Instance id:"+tcrfInstance.getInstanceId()); //id:214 have some problem
//		if(tcrfInstance.getInstanceId()==214) System.out.println(tcrfInstance.getOutput().pennString());
		TCRFInstance result = tcrfInstance.duplicate();
		Tree prediction = new LabeledScoredTreeNode();
		long root = tcrfNetwork.getRoot();
//		System.err.println("The root:"+Arrays.toString(NetworkIDMapper.toHybridNodeArray(root)));
		//long root = toNode(tcrfInstance.size()-1, 0, this.allLabels.size()-1);
		int rootIdx = Arrays.binarySearch(tcrfNetwork.getAllNodes(), root);
		findAll(prediction, tcrfNetwork,root, rootIdx,true,0, tcrfInstance.size()-1);
		prediction.setSpans();
//		System.err.println(prediction.pennString());
		result.setPrediction(prediction);
		return result;
	}
	
	public void addAll(TCRFNetwork tcrfNetwork, Tree t){
		CoreLabel cl = (CoreLabel)(t.label());
		long parent = toNode(t.getSpan().getTarget()-t.getSpan().getSource(), t.getSpan().getSource(), cl.index());
		if(!t.isLeaf()){
			List<Tree> children = t.getChildrenAsList();
			if(t.isPhrasal()){
				Tree child1 = children.get(0);
				CoreLabel cl_1 = (CoreLabel)(child1.label());
				Tree child2 = children.get(1);
				CoreLabel cl_2 = (CoreLabel)(child2.label());
				long child1Node = toNode(child1.getSpan().getTarget()-child1.getSpan().getSource(), child1.getSpan().getSource(),cl_1.index());;
				long child2Node = toNode(child2.getSpan().getTarget()-child2.getSpan().getSource(), child2.getSpan().getSource(),cl_2.index());
				tcrfNetwork.addNode(child1Node);
				tcrfNetwork.addNode(child2Node);
				tcrfNetwork.addEdge(parent, new long[]{child1Node,child2Node});
				if(!child1.isPreTerminal()) addAll(tcrfNetwork,child1);
				if(!child2.isPreTerminal()) addAll(tcrfNetwork,child2);
			}
			
		}
	}
	
	public TCRFNetwork compileLabeledInstances(int networkId, TCRFInstance inst, LocalNetworkParam param){
		TCRFNetwork tcrfNetwork = new TCRFNetwork(networkId, inst, param);
		Tree output = inst.getOutput();
		output.setSpans();
		CoreLabel cl = (CoreLabel)(output.label());
		long parent = toNode(output.getSpan().getTarget()-output.getSpan().getSource(), output.getSpan().getSource(), cl.index());
		tcrfNetwork.addNode(parent);
		addAll(tcrfNetwork,output);
		tcrfNetwork.finalizeNetwork();
		return tcrfNetwork;
		
	}
	
	public void addUnlabeledAll(TCRFNetwork tcrfNetwork, long parent){
		int[] parentArr = NetworkIDMapper.toHybridNodeArray(parent);
//		System.out.println("parent node now: "+Arrays.toString(parentArr));
		int curSpanLen = parentArr[0];
		for(int nxtSpanLen=0;nxtSpanLen<curSpanLen;nxtSpanLen++){
			for(int l=0;l<this.allLabels.size();l++){
				long child1 = toNode(nxtSpanLen, parentArr[1],this.allLabels.get(l).getID());
				if(!tcrfNetwork.contains(child1)) tcrfNetwork.addNode(child1);
//				System.out.println("  child1 node now: "+Arrays.toString(NetworkIDMapper.toHybridNodeArray(child1)));
				for(int l_1=0;l_1<this.allLabels.size();l_1++){
					long child2 = toNode(curSpanLen-nxtSpanLen-1, parentArr[1]+nxtSpanLen+1,this.allLabels.get(l_1).getID());
					if(!tcrfNetwork.contains(child2)) tcrfNetwork.addNode(child2);
					
//					System.out.println("  child2 node now: "+Arrays.toString(NetworkIDMapper.toHybridNodeArray(child2)));
					long[] children = new long[]{child1,child2}; 
					if(!tcrfNetwork.containsEdge(parent, children)) tcrfNetwork.addEdge(parent, children);
					if(curSpanLen-nxtSpanLen-1>0) addUnlabeledAll(tcrfNetwork,child2);
				}
				if(nxtSpanLen>0) addUnlabeledAll(tcrfNetwork,child1);
			}
		}
	}
	
	public TCRFNetwork compileUnLabeledInstance(int networkId, TCRFInstance inst, LocalNetworkParam param){
		long[] allNodes = genericUnlabeledNetwork.getAllNodes();
		long root = toNode(inst.size()-1, 0, this.allLabels.get(root_label_idx).getID());
		int rootIdx = Arrays.binarySearch(allNodes, root);
		TCRFNetwork tcrfNetwork = new TCRFNetwork(networkId, inst, allNodes, genericUnlabeledNetwork.getAllChildren(),param,rootIdx+1);
		return tcrfNetwork;
		
	}
	
	public void compileUnlabeledInstanceGeneric(){
		TCRFNetwork tcrfNetwork= new TCRFNetwork();
//		long root = toNode(this._size-1, 0, this.allLabels.get(root_label_idx).getID());
//		tcrfNetwork.addNode(root);
//		addUnlabeledAll(tcrfNetwork, root);
//		for(int pos = 0;pos<this._size;pos++){
//			Iterator<Integer> iter = preTerminals.iterator();
//			while(iter.hasNext()){
//				long leaf = toNode(0,pos,iter.next());
//				tcrfNetwork.addNode(leaf);
//			}
//		}
		for(int span=2;span<=this._size;span++){
			for(int L=0; L<=this._size-span;L++){
				int R = L+span-1;
				for(int M=L; M<=R-1;M++){
					Iterator<Integer> iter = nonTerminalRules.keySet().iterator();
					while(iter.hasNext()){
						int parent_tag = iter.next();
						long parent = toNode(R-L,L,parent_tag);
						tcrfNetwork.addNode(parent);
						ArrayList<int[]> children_tag = nonTerminalRules.get(parent_tag);
//						if(R==1 && L==0 ){
//							System.err.println("[DEBUG]: children_tag length: "+ children_tag.size()+" parent_tag: "+parent_tag);
//							System.err.println("[DEBUG]: children_tag: "+ children_tag.get(0)[0]+","+children_tag.get(0)[1]);
//						}
						for(int[] childs: children_tag){
//							System.err.println("[DEBUG] out parent: "+Arrays.toString(NetworkIDMapper.toHybridNodeArray(parent)));
							long child_1 = toNode(M-L, L, childs[0]);
							tcrfNetwork.addNode(child_1);
							long child_2 = toNode(R-M-1, M+1, childs[1]);
							tcrfNetwork.addNode(child_2);

//							System.err.println("[DEBUG] out chilren_1: "+Arrays.toString(NetworkIDMapper.toHybridNodeArray(child_1)));
//							System.err.println("[DEBUG] out chilren_2: "+Arrays.toString(NetworkIDMapper.toHybridNodeArray(child_2)));
							tcrfNetwork.addEdge(parent, new long[]{child_1,child_2});
						}
					}
				}
			}
		}
		tcrfNetwork.finalizeNetwork();
//		System.err.println("[DEBUG] num all nodes:"+tcrfNetwork.getAllNodes().length);
		long root = tcrfNetwork.getRoot();
//		System.err.println("The root:"+Arrays.toString(NetworkIDMapper.toHybridNodeArray(root)));
		int rootIdx = Arrays.binarySearch(tcrfNetwork.getAllNodes(), root);
//		System.err.println("[DEBUG] root index:"+rootIdx);
//		System.err.println("[DEBUG] node index with childs: "+Arrays.toString(tcrfNetwork.getChildren(rootIdx)[0]));
		genericUnlabeledNetwork = tcrfNetwork;
		
	}
}

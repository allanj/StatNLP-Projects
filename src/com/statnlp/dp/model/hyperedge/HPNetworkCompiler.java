package com.statnlp.dp.model.hyperedge;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.DependInstance;
import com.statnlp.dp.DependencyNetwork;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.ui.visualize.VisualizationViewerEngine;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;

public class HPNetworkCompiler extends NetworkCompiler {

	private static final long serialVersionUID = -5080640847287255079L;

	private long[] _nodes;
	private final int maxSentLen = 100;
	private int[][][] _children;
	private HashMap<String, Integer> typeMap;
	private String[] types;
	private enum NODE {normal};
	private VisualizationViewerEngine viewer;
	
	/**
	 * Compiler constructor
	 * @param typeMap: typeMap from DPConfig
	 */
	public HPNetworkCompiler(HashMap<String, Integer> typeMap, VisualizationViewerEngine viewer) {
		// rightIndex, rightIndex-leftIndex, completeness, direction, entity type, node type
		int[] capacity = new  int[]{1000,1000,2,3,15,2};
		NetworkIDMapper.setCapacity(capacity);
		this.typeMap = typeMap;
		this.types = new String[typeMap.size()]; 
		Iterator<String> iter = typeMap.keySet().iterator();
		while(iter.hasNext()){
			String entity = iter.next();
			types[typeMap.get(entity)] = entity;
		}
		this.viewer = viewer;
	}

	@Override
	public Network compile(int networkId, Instance inst, LocalNetworkParam param) {
		DependInstance di = (DependInstance)inst;
		if(di.isLabeled()){
			//System.err.println("[Info] Compiling Labeled Network...");
			return this.compileLabledInstance(networkId, di, param);
		}else{
			//System.err.println("[Info] Compiling Unlabeled Network...");
			return this.compileUnLabledInstance(networkId, di, param);
		}
	}

	public DependencyNetwork compileLabledInstance(int networkId, DependInstance inst, LocalNetworkParam param){
		DependencyNetwork network = new DependencyNetwork(networkId,inst,param);
		Sentence sent = inst.getInput();
		Tree tree = inst.getOutput();
		return this.compile(network, sent, tree);
	}
	
	private DependencyNetwork compile(DependencyNetwork network, Sentence sent, Tree output){
		output.setSpans();
		long rootNode = this.toNode_generalRoot(sent.length());
		network.addNode(rootNode);
		addToNetwork(network,output);
		network.finalizeNetwork();
		//viewer.visualizeNetwork(network, null, "HP labeled network");
		return network;
	}
	
	private void addToNetwork(DependencyNetwork network, Tree parent){
		if(parent.isLeaf()) return; //means headindex==modifier index
		CoreLabel cl = (CoreLabel)(parent.label());
		String[] info = cl.value().split(",");
		int pa_leftIndex = Integer.valueOf(info[0]);
		int pa_rightIndex = Integer.valueOf(info[1]);
		int pa_direction = Integer.valueOf(info[2]);
		int pa_completeness = Integer.valueOf(info[3]);
		String pa_type = info[4];
		if(pa_leftIndex==pa_rightIndex) return; //means the span width now is 1, already enough
		long parentNode = toNode(pa_leftIndex, pa_rightIndex, pa_direction, pa_completeness,pa_type);
		Tree[] children = parent.children();
		if(children.length!=2 && !pa_type.equals("root")){
			throw new RuntimeException("The children length should be 2 in the labeled tree.");
		}	
		
		long[] childrenArr = new long[2];
		
		for(int c=0;c<children.length;c++){
			CoreLabel childLabel = (CoreLabel)(children[c].label());
			String[] childInfo = childLabel.value().split(",");
			int child_leftIndex = Integer.valueOf(childInfo[0]);
			int child_rightIndex = Integer.valueOf(childInfo[1]);
			int child_direction = Integer.valueOf(childInfo[2]);
			int child_completeness = Integer.valueOf(childInfo[3]);
			String c1type = childInfo[4];
			childrenArr[c] = toNode(child_leftIndex, child_rightIndex, child_direction, child_completeness,c1type);
			network.addNode(childrenArr[c]);
		}
		if(children.length==1) {
			network.addEdge(parentNode, new long[]{childrenArr[0]});
			addToNetwork(network, children[0]);
		}else {
			network.addEdge(parentNode, new long[]{childrenArr[0],childrenArr[1]});
			addToNetwork(network, children[0]);
			addToNetwork(network, children[1]);
		}
		
		
			
	}
	
	public DependencyNetwork compileUnLabledInstance(int networkId, DependInstance inst, LocalNetworkParam param){
		if(this._nodes==null){
			this.compileUnlabeled();
		}
		long root = this.toNode_generalRoot(inst.getInput().length());
		int rootIdx = Arrays.binarySearch(this._nodes, root);
		DependencyNetwork network = new DependencyNetwork(networkId,inst,this._nodes,this._children, param,rootIdx+1 );
		return network;
	}
	
	public synchronized void compileUnlabeled(){
		if(this._nodes!=null){
			return;
		}
		DependencyNetwork network = new DependencyNetwork();
		long rootE = this.toNode(0, 0, 1, 1, "ONE");
		network.addNode(rootE);
		
		for(int rightIndex = 1; rightIndex<=this.maxSentLen-1;rightIndex++){
			//eIndex: 1,2,3,4,5,..n
			for(String e: types){
				if(e.equals("OE")) continue;
				long wordLeftNodeE = this.toNode(rightIndex, rightIndex, 0, 1, e);
				long wordRightNodeE = this.toNode(rightIndex, rightIndex, 1, 1, e);
				network.addNode(wordLeftNodeE);
				network.addNode(wordRightNodeE);
			}

			
			for(int L=1;L<=rightIndex;L++){
				//L:(1),(1,2),(1,2,3),(1,2,3,4),(1,2,3,4,5),...(1..n)
				//bIndex:(0),(1,0),(2,1,0),(3,2,1,0),(4,3,2,1,0),...(n-1..0)
				//span: {(0,1)},{(1,2),(0,2)}
				int leftIndex = rightIndex - L;
				//span:[bIndex, rightIndex]
				
				for(int complete=0;complete<=1;complete++){
					for(int direction=0;direction<=1;direction++){
						if(leftIndex==0 && direction==0) continue;
						if(complete==0){
							//incomplete span decompose to two complete spans
							
							for(int m=leftIndex;m<rightIndex;m++){
								for(int t=0;t<types.length;t++){
									long parentE = this.toNode(leftIndex, rightIndex, direction, complete, types[t]);
									for(int t1=0;t1<types.length;t1++){
										long child_1 = this.toNode(leftIndex, m, 1, 1, types[t1]);
										for(int t2=0;t2<types.length;t2++){
											long child_2 = this.toNode(m+1, rightIndex, 0, 1, types[t2]);
											if(checkRule(types[t], types[t1],types[t2]) && network.contains(child_1) && network.contains(child_2)){
												network.addNode(parentE);
												network.addEdge(parentE, new long[]{child_1,child_2});
											}
										}
									}
								}
							}
						}
						
						if(complete==1 && direction==0){
							for(int m=leftIndex;m<rightIndex;m++){
								for(int t=0;t<types.length;t++){
									long parentE = this.toNode(leftIndex, rightIndex, direction, complete, types[t]);
									
									for(int t1=0;t1<types.length;t1++){
										long child_1 = this.toNode(leftIndex, m, 0, 1,types[t1]);
										for(int t2=0;t2<types.length;t2++){
											long child_2 = this.toNode(m, rightIndex, 0, 0, types[t2]);
											if(checkRule(types[t], types[t1],types[t2]) && network.contains(child_1) && network.contains(child_2)){
												network.addNode(parentE);
												network.addEdge(parentE, new long[]{child_1,child_2});
											}
										}
									}
									
								}
							}
						}
						
						if(complete==1 && direction==1){
							
							for(int m=leftIndex+1;m<=rightIndex;m++){
								for(int t=0;t<types.length;t++){
									long parentE  = this.toNode(leftIndex, rightIndex, direction, complete, types[t]);
									
									for(int t1=0;t1<types.length;t1++){
										long child_1 = this.toNode(leftIndex, m, 1, 0,types[t1]);
										for(int t2=0;t2<types.length;t2++){
											long child_2 = this.toNode(m, rightIndex, 1, 1,types[t2]);
											if(checkRule(types[t], types[t1],types[t2]) && network.contains(child_1) && network.contains(child_2)){
												network.addNode(parentE);
												network.addEdge(parentE, new long[]{child_1,child_2});
											}
										}
									}
								}
							}
							
							
							if(leftIndex==0 && rightIndex>leftIndex){
								long subRoot = this.toNode(leftIndex, rightIndex, direction, complete, "root");
								for(int t=0;t<types.length;t++){
									long parentE  = this.toNode(leftIndex, rightIndex, direction, complete, types[t]);
									if(network.contains(parentE)){
										network.addNode(subRoot);
										network.addEdge(subRoot, new long[]{parentE});
									}
								}
							}
						}
						
					}
				}
			}
		}
		
		network.finalizeNetwork();
		this._nodes = network.getAllNodes();
		this._children = network.getAllChildren();
		
		//printNodes(this._nodes);
		System.err.println(network.countNodes()+" nodes..");
		//viewer.visualizeNetwork(network, null, "unLabeled Model");
		//printNetwork(network, null);
		//System.exit(0);
	}
	
	
	private boolean checkRule(String parent, String child1, String child2){
		if(!parent.equals("OE")){
			if(child1.equals(parent) && child2.equals(parent)) return true;
			else return false;
		}else{
			if(child1.equals(child2) && !child1.equals("OE")) return false;
			else return true;
		}
	}
	
	@Override
	public Instance decompile(Network network) {
		DependencyNetwork dependNetwork = (DependencyNetwork)network;
		DependInstance inst = (DependInstance)(dependNetwork.getInstance());
		inst = inst.duplicate();
		if(dependNetwork.getMax()==Double.NEGATIVE_INFINITY) return inst;
		//viewer.visualizeNetwork(dependNetwork, null, "Testing Labeled Model:"+network.getInstance().getInstanceId());
		Tree forest = this.toTree(dependNetwork,inst);
//		printNetwork(dependNetwork, (Sentence)dependNetwork.getInstance().getInput());
//		System.err.println("[Result] "+forest.toString());
		inst.setPrediction(forest);
		return inst;
	}
	
	private Tree toTree(DependencyNetwork network,DependInstance inst){
		
		Tree root = new LabeledScoredTreeNode();
		CoreLabel rootLabel = new CoreLabel();
		rootLabel.setValue("0,"+(inst.getInput().length()-1)+",1,1,root");
		root.setLabel(rootLabel);
		this.toTreeHelper(network, network.countNodes()-1, root);
		//System.err.println(root.pennString());
		return root;
	}
	
	private void toTreeHelper(DependencyNetwork network, int node_k, Tree parentNode){
		int[] children_k = network.getMaxPath(node_k);
//		System.err.println("node_k:"+node_k);
//		System.err.println("Parent Node:"+parentNode.toString());
//		System.err.println("Children length:"+children_k.length);
		for(int k=0;k<children_k.length;k++){
			long child = network.getNode(children_k[k]);
			int[] ids_child = NetworkIDMapper.toHybridNodeArray(child);
			Tree childNode = new LabeledScoredTreeNode();
			CoreLabel childLabel = new CoreLabel();
			int leftIndex = ids_child[0]-ids_child[1];
			StringBuilder sb = new StringBuilder();
			sb.append(leftIndex);  sb.append(",");
			sb.append(ids_child[0]);  sb.append(",");
			sb.append(ids_child[3]); sb.append(",");
			sb.append(ids_child[2]); sb.append(",");
			String type = null;
			if(ids_child[4]==types.length)
				type = "root";
			else{
				type = types[ids_child[4]];
			}
			sb.append(type);
			childLabel.setValue(sb.toString());
			childNode.setLabel(childLabel);
			parentNode.addChild(childNode);
			this.toTreeHelper(network, children_k[k], childNode);
		}
		
	}
	
	
	public void printNetwork(DependencyNetwork network, Sentence sent){
		int len = -1;
		if(sent==null) len = this.maxSentLen;
		else len = sent.length();
		long root = this.toNode_generalRoot(len);
		long[] nodes = network.getAllNodes();
		System.err.println("Number of nodes: "+nodes.length);
		int rootIndex = Arrays.binarySearch(nodes, root);
		int level = 0;
		printAll(rootIndex,network,level);
		
	}
	
	private void printAll(int index, DependencyNetwork network, int level){
		long current = network.getNode(index);
		for(int i=0;i<level;i++)
			System.err.print("\t");
		int[] arr = NetworkIDMapper.toHybridNodeArray(current);
		int leftIndex = arr[0]-arr[1];
		String direction = arr[3]==0? "left":"right";
		String complete = arr[2]==0? "incomplete":"complete";
		String type = null;
		
		if(arr[4]==types.length)
			type = "root";
		else{
			type = types[arr[4]];
		}
		System.err.println("current node: "+leftIndex+","+arr[0]+","+direction+","+complete+","+type+" , at index:"+index);
		int[][] children = network.getChildren(index);
		for(int i=0;i<children.length;i++){
			int[] twochilds = children[i];
			for(int j=0;j<twochilds.length;j++)
				printAll(twochilds[j],network,level+1);
		}
	}
	
	@SuppressWarnings("unused")
	private void printNodes(long[] nodes){
		System.err.println("Print all the nodes:...");
		for(long node: nodes){
			int[] arr = NetworkIDMapper.toHybridNodeArray(node);
			int leftIndex = arr[0]-arr[1];
			String direction = arr[3]==0? "left":"right";
			String complete = arr[2]==0? "incomplete":"complete";
			String type = types[arr[4]];
			System.err.println("current node: "+leftIndex+","+arr[0]+","+direction+","+complete+","+type);
		}
	}
	
	//Node composition
	//Span Len (eIndex-bIndex), eIndex, direction(0--left,1--right), complete (0--incomplete,1), node Type
	public long toNode_generalRoot(int sentLen){
		int sentence_len = sentLen;
		//Largest span and the node id is sentence len, because the id 0 to sentence len-1, EMPTY is the general type
		return NetworkIDMapper.toHybridNodeID(new int[]{sentence_len-1, sentence_len-1,1,1,typeMap.size(),NODE.normal.ordinal()});
	}
	
	public long toNode(int leftIndex, int rightIndex, int direction, int complete, String type){
		if(!typeMap.containsKey(type) && !type.equals("root")){
			System.err.println("The type is:"+type);
		}
		if(type.equals("root"))
			return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex,rightIndex-leftIndex,complete, direction, typeMap.size(),NODE.normal.ordinal()});
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex,rightIndex-leftIndex,complete, direction, typeMap.get(type),NODE.normal.ordinal()});
	}
	

}

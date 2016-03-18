package com.statnlp.dp.model;

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

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;

public class ADPNetworkCompiler extends NetworkCompiler {

	private static final long serialVersionUID = -5080640847287255079L;

	private long[] _nodes;
	private final int maxSentLen = 100;
	private final int enLen = 1000; //length restriction
	private int[][][] _children;
	private HashMap<String, Integer> typeMap;
	private String[] types;
	
	public ADPNetworkCompiler(HashMap<String, Integer> typeMap) {
		
		// rightIndex, rightIndex-leftIndex, completeness, direction, entity type, node type
		int[] capacity = new  int[]{1000,1000,2,3,10,2};
		NetworkIDMapper.setCapacity(capacity);
		this.typeMap = typeMap;
		this.types = new String[typeMap.size()]; 
		Iterator<String> iter = typeMap.keySet().iterator();
		while(iter.hasNext()){
			String entity = iter.next();
			types[typeMap.get(entity)] = entity;
		}
	}

	@Override
	public Network compile(int networkId, Instance inst, LocalNetworkParam param) {
		DependInstance di = (DependInstance)inst;
		if(di.isLabeled()){
			return this.compileLabledInstance(networkId, di, param);
		}else{
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
		long rootNode = this.toNode_root(sent.length());
		network.addNode(rootNode);
		addToNetwork(network,output);
		network.finalizeNetwork();
//		printNetwork(network,sent);
//		System.exit(0);
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
		String type = info[4];
		if(pa_leftIndex==pa_rightIndex && !type.equals("EMPTY")) return; //means the span width now is 1, already enough
		long parentNode = toNode(pa_leftIndex, pa_rightIndex, pa_direction, pa_completeness,type);
		Tree[] children = parent.children();
		if(children.length!=2 && children.length!=1){
			throw new RuntimeException("The children length should be 2 in the labeled tree.");
		}
		if(children.length==1 && pa_rightIndex-pa_leftIndex+1>enLen){
			children = children[0].children();
			if(children.length==1) throw new RuntimeException("Cannot have only one son after one son");
		}
			
		CoreLabel childLabel_1 = (CoreLabel)(children[0].label());
		String[] childInfo_1 = childLabel_1.value().split(",");
		int child_leftIndex_1 = Integer.valueOf(childInfo_1[0]);
		int child_rightIndex_1 = Integer.valueOf(childInfo_1[1]);
		int child_direction_1 = Integer.valueOf(childInfo_1[2]);
		int child_completeness_1 = Integer.valueOf(childInfo_1[3]);
		String c1type = childInfo_1[4];
		long childNode_1 = toNode(child_leftIndex_1, child_rightIndex_1, child_direction_1, child_completeness_1,c1type);
		network.addNode(childNode_1);
		if(children.length==1){
			network.addEdge(parentNode,new long[]{childNode_1});
			addToNetwork(network, children[0]);
		}
		else if(children.length==2){
			CoreLabel childLabel_2 = (CoreLabel)(children[1].label());
			String[] childInfo_2 = childLabel_2.value().split(",");
			int child_leftIndex_2 = Integer.valueOf(childInfo_2[0]);
			int child_rightIndex_2 = Integer.valueOf(childInfo_2[1]);
			int child_direction_2 = Integer.valueOf(childInfo_2[2]);
			int child_completeness_2 = Integer.valueOf(childInfo_2[3]);
			String c2type = childInfo_2[4];
			long childNode_2 = toNode(child_leftIndex_2, child_rightIndex_2, child_direction_2, child_completeness_2,c2type);
			network.addNode(childNode_2);
			network.addEdge(parentNode, new long[]{childNode_1,childNode_2});
			addToNetwork(network, children[0]);
			addToNetwork(network, children[1]);
		}		
	}
	
	public DependencyNetwork compileUnLabledInstance(int networkId, DependInstance inst, LocalNetworkParam param){
		if(this._nodes==null){
			this.compileUnlabeled();
		}
		
//		System.err.println("[Info] Compile Unlabeled instance, length: "+inst.getInput().length());
		long root = this.toNode(0, inst.getInput().length()-1, 1, 1,"EMPTY");
		int rootIdx = Arrays.binarySearch(this._nodes, root);
//		System.err.println("Root: "+ Arrays.toString(NetworkIDMapper.toHybridNodeArray(root)));
//		System.err.println("Number of nodes under this root Index: "+ (rootIdx+1));

		DependencyNetwork network = new DependencyNetwork(networkId,inst,this._nodes,this._children, param,rootIdx+1 );
//		ADPNetwork network = debug(networkId,inst,param);
		
		return network;
	}
	
	public DependencyNetwork debug(int networkId, DependInstance inst, LocalNetworkParam param){
		DependencyNetwork network = new DependencyNetwork();
		long rootNode = this.toNode(0, 3, 1, 1,"EMPTY");
		long rootE = this.toNodeE(rootNode, "O");
		network.addNode(rootNode);
		network.addNode(rootE);
		network.addEdge(rootNode, new long[]{rootE});
		long rootchild1 = this.toNode(0, 2, 1, 0,"EMPTY");
		long rootchild1E = this.toNodeE(rootchild1, "O");
		long rootchild2 = this.toNode(2, 3, 1, 1,"EMPTY"); // love singapore complete
		long rootchild2E = this.toNodeE(rootchild2, "O");
		network.addNode(rootchild1);
		network.addNode(rootchild2);
		network.addEdge(rootE, new long[]{rootchild1, rootchild2});
		network.addNode(rootchild1E); network.addNode(rootchild2E);
		network.addEdge(rootchild1, new long[]{rootchild1E});
		network.addEdge(rootchild2, new long[]{rootchild2E});
		
		long leaf1 = this.toNode(0, 0, 1, 1,"EMPTY");
		long leaf1E = this.toNodeE(leaf1,"O");
		long leaf2 = this.toNode(1, 2, 0, 1,"EMPTY"); // I love complete general
		long leaf2E = this.toNodeE(leaf2,"O");   // I love complete O
		network.addNode(leaf1);
		network.addNode(leaf2);
		network.addNode(leaf1E); network.addNode(leaf2E);
		network.addEdge(leaf1, new long[]{leaf1E});
		network.addEdge(leaf2, new long[]{leaf2E});
		network.addEdge(rootchild1E, new long[]{leaf1, leaf2});
		

		long leaf11 = this.toNode(1, 1, 0, 1,"EMPTY");  //I
		long leaf11E = this.toNodeE(leaf11,"person");  //I entity
		long leaf12 = this.toNode(1, 2, 0, 0,"EMPTY");  //I love
		long leaf12E = this.toNodeE(leaf12, "O"); // I love incomplte entity
		network.addNode(leaf11);
		network.addNode(leaf12);
		network.addNode(leaf11E);
		network.addNode(leaf12E);
		network.addEdge(leaf11, new long[]{leaf11E});
		network.addEdge(leaf12, new long[]{leaf12E});
		network.addEdge(leaf2E, new long[]{leaf11, leaf12});
		
		long leaf_I_r = this.toNode(1, 1, 1, 1,"EMPTY");  //I
		long leaf_I_rE = this.toNodeE(leaf_I_r,"person");  //I
		network.addNode(leaf_I_r);
		network.addNode(leaf_I_rE);
		network.addEdge(leaf_I_r, new long[]{leaf_I_rE});
		long leaf_love_l = this.toNode(2, 2, 0, 1, "EMPTY");
		long leaf_love_lE = this.toNodeE(leaf_love_l, "O");
		network.addNode(leaf_love_l);
		network.addNode(leaf_love_lE);
		network.addEdge(leaf_love_l, new long[]{leaf_love_lE});
		network.addEdge(leaf12E, new long[]{leaf_I_r,leaf_love_l});
		
		long love_sing_incom = this.toNode(2, 3, 1, 0, "EMPTY");
		long leaf_sing_r = this.toNode(3, 3, 1, 1, "EMPTY");
		network.addEdge(rootchild2E, new long[]{love_sing_incom,leaf_sing_r});
		long love_sing_incomE = this.toNodeE(love_sing_incom, "O");
		long leaf_sing_rE = this.toNodeE(leaf_sing_r, "location");
		network.addNode(love_sing_incomE); network.addNode(leaf_sing_rE);
		network.addEdge(love_sing_incom, new long[]{love_sing_incomE});
		network.addEdge(leaf_sing_r, new long[]{leaf_sing_rE});
		
		long leaf_love_r = this.toNode(2, 2, 1, 1, "EMPTY");
		long leaf_sing_l = this.toNode(3, 3, 0, 1, "EMPTY");
		network.addNode(leaf_love_r); network.addNode(leaf_sing_l);
		network.addEdge(love_sing_incomE, new long[]{leaf_love_r, leaf_sing_l});
		long leaf_love_rE = this.toNodeE(leaf_love_r, "O");
		long leaf_sing_lE = this.toNodeE(leaf_sing_l, "location");
		network.addNode(leaf_love_rE); network.addNode(leaf_sing_lE);
		network.addEdge(leaf_love_r, new long[]{leaf_love_rE});
		network.addEdge(leaf_sing_l, new long[]{leaf_sing_lE});
		
		network.finalizeNetwork();
		this._nodes = network.getAllNodes();
		this._children = network.getAllChildren();
		System.err.println("Debug mode:"+network.countNodes()+" nodes..");
		long root = this.toNode(0, 3, 1, 1,"EMPTY");
		int rootIdx = Arrays.binarySearch(this._nodes, root);
		DependencyNetwork mynetwork = new DependencyNetwork(networkId, inst, this._nodes, this._children, param, rootIdx+1);
		printNetwork(network, null);
		return mynetwork;
	}
	
	public synchronized void compileUnlabeled(){
		if(this._nodes!=null){
			return;
		}
		DependencyNetwork network = new DependencyNetwork();
		//add the root word and other nodes
		//all are complete nodes.
		long rootWordNode = this.toNode(0, 0, 1, 1,"EMPTY");
		long rootE = this.toNodeE(rootWordNode, "O");
		network.addNode(rootWordNode);
		network.addNode(rootE);
		network.addEdge(rootWordNode, new long[]{rootE});
		boolean noE = false;
		
		for(int rightIndex = 1; rightIndex<=this.maxSentLen-1;rightIndex++){
			//eIndex: 1,2,3,4,5,..n
			
			long wordLeftNode = this.toNode(rightIndex, rightIndex, 0, 1,"EMPTY");
			long wordRightNode = this.toNode(rightIndex, rightIndex, 1, 1,"EMPTY");
			network.addNode(wordLeftNode);
			network.addNode(wordRightNode);
			for(String e: types){
				if(e.equals("EMPTY")) continue;
				long wordLeftNodeE = this.toNodeE(wordLeftNode, e);
				long wordRightNodeE = this.toNodeE(wordRightNode, e);
				network.addNode(wordLeftNodeE);
				network.addNode(wordRightNodeE);
				network.addEdge(wordLeftNode, new long[]{wordLeftNodeE});
				network.addEdge(wordRightNode, new long[]{wordRightNodeE});
			}
			
			for(int L=1;L<=rightIndex;L++){
				//L:(1),(1,2),(1,2,3),(1,2,3,4),(1,2,3,4,5),...(1..n)
				//bIndex:(0),(1,0),(2,1,0),(3,2,1,0),(4,3,2,1,0),...(n-1..0)
				//span: {(0,1)},{(1,2),(0,2)}
				int leftIndex = rightIndex - L;
				//span:[bIndex, rightIndex]
				if(rightIndex-leftIndex+1>enLen) noE = true;
				else noE = false;
				
				for(int complete=0;complete<=1;complete++){
					for(int direction=0;direction<=1;direction++){
						boolean addedPa = false;
						boolean addedPaeLink = false;
						if(leftIndex==0 && direction==0) continue;
						if(complete==0){
							//incomplete span decompose to two complete spans
							
							for(int m=leftIndex;m<rightIndex;m++){
								long child_1 = this.toNode(leftIndex, m, 1, 1, "EMPTY");
								long child_2 = this.toNode(m+1, rightIndex, 0, 1, "EMPTY");
								long parent = this.toNode(leftIndex, rightIndex, direction, complete, "EMPTY");
								if(network.contains(child_1) && network.contains(child_2)){
									if(!addedPa) {
										network.addNode(parent);
										addedPa = true;
									}
									if(noE){
										network.addEdge(parent, new long[]{child_1,child_2});
									}else{
										if(!addedPaeLink){
											for(int t=0;t<types.length;t++){
												if(types[t].equals("EMPTY")) continue;
												long parentE = this.toNode(leftIndex, rightIndex, direction, complete,types[t]);
												network.addNode(parentE);
												network.addEdge(parent, new long[]{parentE});
											}
											addedPaeLink = true;
										}
										for(int t=0;t<types.length;t++){
											if(types[t].equals("EMPTY")) continue;
											long parentE = this.toNode(leftIndex, rightIndex, direction, complete,types[t]);
											network.addEdge(parentE, new long[]{child_1,child_2});
												
										}
									}
								}
							}
						}
						
						if(complete==1 && direction==0){
							for(int m=leftIndex;m<rightIndex;m++){
								long child_1 = this.toNode(leftIndex, m, 0, 1,"EMPTY");
								long child_2 = this.toNode(m, rightIndex, 0, 0, "EMPTY");
								long parent = this.toNode(leftIndex, rightIndex, direction, complete, "EMPTY");
								if(network.contains(child_1) && network.contains(child_2)){
									if(!addedPa) {
										network.addNode(parent);
										addedPa = true;
									}
									if(noE){
										network.addEdge(parent, new long[]{child_1,child_2});
									}else{
										if(!addedPaeLink){
											for(int t=0;t<types.length;t++){
												if(types[t].equals("EMPTY")) continue;
												long parentE = this.toNode(leftIndex, rightIndex, direction, complete,types[t]);
												network.addNode(parentE);
												network.addEdge(parent, new long[]{parentE});
											}
											addedPaeLink = true;
										}
										for(int t=0;t<types.length;t++){
											if(types[t].equals("EMPTY") ) continue;
											long parentE = this.toNode(leftIndex, rightIndex, direction, complete,types[t]);
											network.addEdge(parentE, new long[]{child_1,child_2});
										}
									}
								}
								
							}
						}
						
						if(complete==1 && direction==1){
							
							for(int m=leftIndex+1;m<=rightIndex;m++){
								long child_1 = this.toNode(leftIndex, m, 1, 0,"EMPTY");
								long child_2 = this.toNode(m, rightIndex, 1, 1,"EMPTY");
								long parent = this.toNode(leftIndex, rightIndex, direction, complete, "EMPTY");
								
	
								if(network.contains(child_1) && network.contains(child_2)){
									if(!addedPa) {
										network.addNode(parent);
										addedPa = true;
									}
									if(noE){
										network.addEdge(parent, new long[]{child_1,child_2});
									}else{
										if(!addedPaeLink){
											for(int t=0;t<types.length;t++){
												if(types[t].equals("EMPTY")) continue;
												long parentE  = this.toNode(leftIndex, rightIndex, direction, complete, types[t]);
												network.addNode(parentE);
												network.addEdge(parent, new long[]{parentE});
											}
											addedPaeLink = true;
										}
										for(int t=0;t<types.length;t++){
											if(types[t].equals("EMPTY")) continue;
											long parentE  = this.toNode(leftIndex, rightIndex, direction, complete, types[t]);
											network.addEdge(parentE, new long[]{child_1,child_2});
										}
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
		//printNetwork(network, null);
	}
	
	
	@Override
	public Instance decompile(Network network) {
		DependencyNetwork dependNetwork = (DependencyNetwork)network;
		DependInstance inst = (DependInstance)(dependNetwork.getInstance());
		inst = inst.duplicate();
		if(dependNetwork.getMax()==Double.NEGATIVE_INFINITY) return inst;
		Tree forest = this.toTree(dependNetwork,inst);
//		System.err.println("[Result] "+forest.toString());
		inst.setPrediction(forest);
		return inst;
	}
	
	private Tree toTree(DependencyNetwork network,DependInstance inst){
		
		Tree root = new LabeledScoredTreeNode();
		CoreLabel rootLabel = new CoreLabel();
		rootLabel.setValue("0,"+(inst.getInput().length()-1)+",1,1,EMPTY");
		root.setLabel(rootLabel);
		this.toTreeHelper(network, network.countNodes()-1, root);
//		System.err.println(root.pennString());
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
			sb.append(types[ids_child[4]]);
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
		long root = this.toNode_root(len);
		long[] nodes = network.getAllNodes();
		System.err.println("Number of nodes: "+nodes.length);
		int rootIndex = Arrays.binarySearch(nodes, root);
		System.err.println("root node: "+Arrays.toString(NetworkIDMapper.toHybridNodeArray(root))+" , at index:"+rootIndex);
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
		String type = types[arr[4]];
		System.err.println("current node: "+leftIndex+","+arr[0]+","+direction+","+complete+","+type+" , at index:"+index);
		int[][] children = network.getChildren(index);
		for(int i=0;i<children.length;i++){
			int[] twochilds = children[i];
			for(int j=0;j<twochilds.length;j++)
				printAll(twochilds[j],network,level+1);
		}
	}
	
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
	public long toNode_root(int sentLen){
		int sentence_len = sentLen;
		//Largest span and the node id is sentence len, because the id 0 to sentence len-1, EMPTY is the general type
		return NetworkIDMapper.toHybridNodeID(new int[]{sentence_len-1, sentence_len-1,1,1,typeMap.get("EMPTY"),Network.NODE_TYPE.max.ordinal()});
	}
	
	public long toNode(int leftIndex, int rightIndex, int direction, int complete, String type){
		if(!typeMap.containsKey(type)){
			System.err.println("The type is:"+type);
		}
		//if direction is 2, that must be the leaf nodes
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex,rightIndex-leftIndex,complete, direction, typeMap.get(type),Network.NODE_TYPE.max.ordinal()});
	}
	
	public long toNodeE(long node, String type){
		int[] arr = NetworkIDMapper.toHybridNodeArray(node);
		return NetworkIDMapper.toHybridNodeID(new int[]{arr[0], arr[1], arr[2],arr[3], typeMap.get(type), arr[5]});
	}

}

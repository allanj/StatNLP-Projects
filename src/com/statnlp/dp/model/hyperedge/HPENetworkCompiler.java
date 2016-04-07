package com.statnlp.dp.model.hyperedge;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.DependInstance;
import com.statnlp.dp.DependencyNetwork;
import com.statnlp.dp.utils.DPConfig;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkException;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.ui.visualize.VisualizationViewerEngine;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;

public class HPENetworkCompiler extends NetworkCompiler {

	private static final long serialVersionUID = -5080640847287255079L;

	private long[] _nodes;
	private final int maxSentLen = 128;
	private int[][][] _children;
	private HashMap<String, Integer> typeMap;
	private String[] types;
	private enum NODE {normal};
	private  VisualizationViewerEngine viewer;
	public static String OE = DPConfig.OE;
	public static String ONE = DPConfig.ONE;
	
	private static boolean DEBUG = false;
	
	/**
	 * Compiler constructor
	 * @param typeMap: typeMap from DPConfig
	 */
	public HPENetworkCompiler(HashMap<String, Integer> typeMap, VisualizationViewerEngine viewer) {
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
		this.compile(network, sent, tree);
		if(DEBUG){
			DependencyNetwork unlabeled = compileUnLabledInstance(networkId, inst, param);
			if(!unlabeled.contains(network)){
				System.err.println(sent.toString());
				throw new NetworkException("Labeled network is not contained in the unlabeled version");
			}
		}
		return network;
	}
	
	private void compile(DependencyNetwork network, Sentence sent, Tree output){
		output.setSpans();
		long rootNode = this.toNode_generalRoot(sent.length());
		network.addNode(rootNode);
		CoreLabel cl = (CoreLabel)(output.label());
		String[] info = cl.value().split(",");
		int pa_leftIndex = Integer.valueOf(info[0]);
		int pa_rightIndex = Integer.valueOf(info[1]);
		int pa_direction = Integer.valueOf(info[2]);
		int pa_completeness = Integer.valueOf(info[3]);
		String pa_type = info[4];
		long treeRootNode = toNode(pa_leftIndex, pa_rightIndex, pa_direction, pa_completeness,pa_type);
		network.addNode(treeRootNode);
		network.addEdge(rootNode, new long[]{treeRootNode});
		addToNetwork(network,output);
		network.finalizeNetwork();
		//viewer.visualizeNetwork(network, null, "Labeled Network");
		//System.err.println(network);
		//System.err.println(output.pennString());
		
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
		if(children.length!=2){
			throw new RuntimeException("The children length must be 2 in the labeled hyper edge model tree.");
		}
		CoreLabel childLabel_1 = (CoreLabel)(children[0].label());
		String[] childInfo_1 = childLabel_1.value().split(",");
		int child_leftIndex_1 = Integer.valueOf(childInfo_1[0]);
		int child_rightIndex_1 = Integer.valueOf(childInfo_1[1]);
		int child_direction_1 = Integer.valueOf(childInfo_1[2]);
		int child_completeness_1 = Integer.valueOf(childInfo_1[3]);
		String c1type = childInfo_1[4];
		long childNode_1 = toNode(child_leftIndex_1, child_rightIndex_1, child_direction_1, child_completeness_1,c1type);
		
		CoreLabel childLabel_2 = (CoreLabel)(children[1].label());
		String[] childInfo_2 = childLabel_2.value().split(",");
		int child_leftIndex_2 = Integer.valueOf(childInfo_2[0]);
		int child_rightIndex_2 = Integer.valueOf(childInfo_2[1]);
		int child_direction_2 = Integer.valueOf(childInfo_2[2]);
		int child_completeness_2 = Integer.valueOf(childInfo_2[3]);
		String c2type = childInfo_2[4];
		long childNode_2 = toNode(child_leftIndex_2, child_rightIndex_2, child_direction_2, child_completeness_2,c2type);
		
		network.addNode(childNode_1);
		network.addNode(childNode_2);
		network.addEdge(parentNode, new long[]{childNode_1,childNode_2});
		addToNetwork(network, children[0]);
		addToNetwork(network, children[1]);
	}
	
	public DependencyNetwork compileUnLabledInstance(int networkId, DependInstance inst, LocalNetworkParam param){
		if(this._nodes==null){
			this.compileUnlabeled();
		}
		long root = this.toNode_generalRoot(inst.getInput().length());
		int rootIdx = Arrays.binarySearch(this._nodes, root);
		DependencyNetwork network = new DependencyNetwork(networkId,inst,this._nodes,this._children, param,rootIdx+1 );
		//viewer.visualizeNetwork(network, null, "UnLabeled Network");
		//System.err.println("[Info] Compile Unlabeled instance, length: "+inst.getInput().length());
		//System.err.println("My root:"+Arrays.toString(NetworkIDMapper.toHybridNodeArray(root)));
		//System.err.println("root index:"+rootIdx);
		//System.err.println(Arrays.toString(NetworkIDMapper.toHybridNodeArray(this._nodes[this._nodes.length-5])));
		//System.err.println("Root: "+ Arrays.toString(NetworkIDMapper.toHybridNodeArray(root)));
		//System.err.println("Number of nodes under this root Index: "+ (rootIdx+1));
		network.contains(network);
		return network;
	}
	
	public synchronized void compileUnlabeled(){
		if(this._nodes!=null){
			return;
		}
		DependencyNetwork network = new DependencyNetwork();
		//add the root word and other nodes
		//all are complete nodes.
		long rootE = this.toNode(0, 0, 1, 1, ONE);
		network.addNode(rootE);
		
		for(int rightIndex = 1; rightIndex<=this.maxSentLen-1;rightIndex++){
			//eIndex: 1,2,3,4,5,..n
			for(String e: types){
				if(e.equals(OE)) continue;
				long wordRightNodeE = this.toNode(rightIndex, rightIndex, 1, 1, e); //the leaf node entity, right direction.
				network.addNode(wordRightNodeE);
				long wordLeftNodeE = -1;
				if(!(rightIndex==1 && !e.equals(ONE))){
					wordLeftNodeE = this.toNode(rightIndex, rightIndex, 0, 1, e);
					network.addNode(wordLeftNodeE);
				}
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
									long parent = this.toNode(leftIndex, rightIndex, direction, complete, types[t]);
									long leftChild = -1;
									long rightChild = -1;
									if(isEntity(types[t]) || types[t].equals(ONE)){
										leftChild = this.toNode(leftIndex, m, 1, 1, types[t]);
										rightChild = this.toNode(m+1, rightIndex, 0, 1,types[t]);
										if(network.contains(leftChild) && network.contains(rightChild)){
											network.addNode(parent);
											network.addEdge(parent, new long[]{leftChild, rightChild});
										}
									}else{ // must be OE
										for(int t1=0;t1<types.length;t1++){
											for(int t2=0;t2<types.length;t2++){
												if(isEntity(types[t2])) continue;
												if(isEntity(types[t1]) &&  (leftIndex!=m) ) continue;
												if(types[t1].equals(ONE) && types[t1].equals(types[t2])) continue;
												leftChild = this.toNode(leftIndex, m, 1, 1, types[t1]);
												rightChild = this.toNode(m+1, rightIndex, 0, 1,types[t2]);
												if(network.contains(leftChild) && network.contains(rightChild)){
													network.addNode(parent);
													network.addEdge(parent, new long[]{leftChild, rightChild});
												}
											}
										}
									}
									
									
								}
							}
						}
						
						if(complete==1 && direction==0){
							for(int m=leftIndex;m<rightIndex;m++){
								for(int t=0;t<types.length;t++){
									long parent = this.toNode(leftIndex, rightIndex, direction, complete, types[t]);
									long leftChild = -1;
									long rightChild = -1;
									if(isEntity(types[t]) || types[t].equals(ONE)){
										leftChild = this.toNode(leftIndex, m, 0, 1, types[t]);
										rightChild = this.toNode(m, rightIndex, 0, 0, types[t]);
										if(network.contains(leftChild) && network.contains(rightChild)){
											network.addNode(parent);
											network.addEdge(parent, new long[]{leftChild, rightChild});
										}
									}else{ // must be OE
										for(int t1=0;t1<types.length;t1++){
											if(isEntity(types[t1])) continue;
											for(int t2=0;t2<types.length;t2++){
												if(types[t1].equals(ONE) && types[t1].equals(types[t2])) continue;
												leftChild = this.toNode(leftIndex, m, 0, 1, types[t1]);
												rightChild = this.toNode(m, rightIndex, 0, 0, types[t2]);
												if(network.contains(leftChild) && network.contains(rightChild)){
													network.addNode(parent);
													network.addEdge(parent, new long[]{leftChild, rightChild});
												}
											}
										}
									}
								}
							}
						}
						
						if(complete==1 && direction==1){
							boolean[] rootAdded = new boolean[types.length];
							for(int x=0;x<rootAdded.length;x++) rootAdded[x]=false;
							for(int m=leftIndex+1;m<=rightIndex;m++){
								for(int t=0;t<types.length;t++){
									long parent  = this.toNode(leftIndex, rightIndex, direction, complete, types[t]);
									long leftChild = -1;
									long rightChild = -1;
									if(isEntity(types[t]) || types[t].equals(ONE)){
										leftChild = this.toNode(leftIndex, m, 1, 0, types[t]);
										rightChild = this.toNode(m, rightIndex, 1, 1, types[t]);
										if(network.contains(leftChild) && network.contains(rightChild)){
											network.addNode(parent);
											network.addEdge(parent, new long[]{leftChild, rightChild});
										}
									}else{
										for(int t1=0;t1<types.length;t1++){
											for(int t2=0;t2<types.length;t2++){
												if(types[t1].equals(OE) && isEntity(types[t2]) && m!=rightIndex) continue;
												if(isEntity(types[t1]) && isEntity(types[t2])) continue;
												if(types[t1].equals(ONE) && types[t1].equals(types[t2])) continue;
												leftChild = this.toNode(leftIndex, m, 1, 0, types[t1]);
												rightChild = this.toNode(m, rightIndex, 1, 1, types[t2]);
												if(network.contains(leftChild) && network.contains(rightChild)){
													network.addNode(parent);
													network.addEdge(parent, new long[]{leftChild, rightChild});
												}
											}
										}
									}
									if(!rootAdded[t] && network.contains(parent) && leftIndex==0 && rightIndex>leftIndex){
										long subRoot = this.toNode(leftIndex, rightIndex, direction, complete, "null");
										network.addNode(subRoot);
										network.addEdge(subRoot, new long[]{parent});
										rootAdded[t] = true;
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
	
	
	@Override
	public Instance decompile(Network network) {
		DependencyNetwork dependNetwork = (DependencyNetwork)network;
		DependInstance inst = (DependInstance)(dependNetwork.getInstance());
		inst = inst.duplicate();
		if(dependNetwork.getMax()==Double.NEGATIVE_INFINITY) return inst;
		//viewer.visualizeNetwork(dependNetwork, null, "Testing Labeled Model:"+network.getInstance().getInstanceId());
		Tree forest = this.toTree(dependNetwork,inst);
//		printNetwork(dependNetwork, (Sentence)dependNetwork.getInstance().getInput());
//		System.err.println("[Result] "+forest.pennString());
		inst.setPrediction(forest);
		return inst;
	}
	
	private Tree toTree(DependencyNetwork network,DependInstance inst){
		
		Tree root = new LabeledScoredTreeNode();
		CoreLabel rootLabel = new CoreLabel();
		rootLabel.setValue("0,"+(inst.getInput().length()-1)+",1,1,null");
		root.setLabel(rootLabel);
		this.toTreeHelper(network, network.countNodes()-1, root);
		return root.getChild(0);
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
			//ids_child: rightIndex, rightIndex-leftIndex, completeness, direction.
			sb.append(leftIndex);  sb.append(",");
			sb.append(ids_child[0]);  sb.append(",");
			sb.append(ids_child[3]); sb.append(",");
			sb.append(ids_child[2]); sb.append(",");
			String type = null;
			if(ids_child[4]==types.length)
				type = "null";
			else type=types[ids_child[4]];
			sb.append(type);
//			if(leftIndex==ids_child[0] && !type.startsWith(PARENT_IS)){
//				Sentence sentence = (Sentence)network.getInstance().getInput();
//				System.err.println(sentence.get(leftIndex).getName()+" left:"+leftIndex+", right:"+ids_child[0]+", cmp:"+ids_child[3]+", dir:"+ids_child[2]+", "+type);
//			}
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
//		System.err.println("root node: "+Arrays.toString(NetworkIDMapper.toHybridNodeArray(root))+" , at index:"+rootIndex);
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
			type = "null";
		else type=types[arr[4]];
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
		if(!typeMap.containsKey(type) && !type.equals("null")){
			System.err.println("The type is:"+type);
		}
		if(type.equals("null"))
			return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex,rightIndex-leftIndex,complete, direction, typeMap.size(),NODE.normal.ordinal()});
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex,rightIndex-leftIndex,complete, direction, typeMap.get(type),NODE.normal.ordinal()});
	}
	
	private boolean isEntity(String type){
		return !type.equals(OE) &&!type.equals(ONE);
	}

}

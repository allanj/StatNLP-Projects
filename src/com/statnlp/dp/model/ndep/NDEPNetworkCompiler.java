package com.statnlp.dp.model.ndep;

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
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.ui.visualize.VisualizationViewerEngine;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;

public class NDEPNetworkCompiler extends NetworkCompiler {

	private static final long serialVersionUID = -5080640847287255079L;

	private long[] _nodes;
	private final int maxSentLen = 100;
	private int[][][] _children;
	private HashMap<String, Integer> typeMap;
	private String[] types;
	private enum NODE {normal};
	private  VisualizationViewerEngine viewer;
	public static String PARENT_IS = DPConfig.PARENT_IS;
	public static String OE = DPConfig.OE;
	public static String ONE = DPConfig.ONE;
	
	
	/**
	 * Compiler constructor
	 * @param typeMap: typeMap from DPConfig which also include those type with "pae"
	 */
	public NDEPNetworkCompiler(HashMap<String, Integer> typeMap, VisualizationViewerEngine viewer) {
		// rightIndex, rightIndex-leftIndex, completeness, direction, entity type, node type
		int[] capacity = new  int[]{1000,1000,2,3,15,2};
		NetworkIDMapper.setCapacity(capacity);
		this.typeMap = typeMap;
		this.types = new String[typeMap.size()/2]; 
		Iterator<String> iter = typeMap.keySet().iterator();
		while(iter.hasNext()){
			String entity = iter.next();
			if(!entity.startsWith(PARENT_IS))
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
		//viewer.visualizeNetwork(network, null, "Labeled Network");
		//System.err.println(network);
		//System.err.println(output.pennString());
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
		if(pa_leftIndex==pa_rightIndex && !pa_type.startsWith(PARENT_IS)) return; //means the span width now is 1, already enough
		if(!pa_type.equals(ONE) && !pa_type.equals(OE)) return; //means this span is already entity, then return.
		long parentNode = toNode(pa_leftIndex, pa_rightIndex, pa_direction, pa_completeness,pa_type);
		Tree[] children = parent.children();
		if(children.length!=2 && children.length!=1){
			throw new RuntimeException("The children length should be 2 in the labeled tree.");
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
		long root = this.toNode_generalRoot(inst.getInput().length());
		int rootIdx = Arrays.binarySearch(this._nodes, root);
		DependencyNetwork network = new DependencyNetwork(networkId,inst,this._nodes,this._children, param,rootIdx+1 );
		//viewer.visualizeNetwork(network, null, "UnLabeled Network");
		//System.err.println("[Info] Compile Unlabeled instance, length: "+inst.getInput().length());
		//System.err.println("My root:"+Arrays.toString(NetworkIDMapper.toHybridNodeArray(root)));
		//System.err.println("root index:"+rootIdx);
		//5964
		//System.err.println(Arrays.toString(NetworkIDMapper.toHybridNodeArray(this._nodes[this._nodes.length-5])));
		//System.err.println("Root: "+ Arrays.toString(NetworkIDMapper.toHybridNodeArray(root)));
		//System.err.println("Number of nodes under this root Index: "+ (rootIdx+1));
		return network;
	}
	
	public synchronized void compileUnlabeled(){
		if(this._nodes!=null){
			return;
		}
		DependencyNetwork network = new DependencyNetwork();
		//add the root word and other nodes
		//all are complete nodes.
		long rootWordNode1 = this.toNode(0, 0, 1, 1,PARENT_IS+ONE);
		long rootWordNode2 = this.toNode(0, 0, 1, 1,PARENT_IS+OE);
		long rootE = this.toNode(0, 0, 1, 1, ONE);
		network.addNode(rootWordNode1);
		network.addNode(rootWordNode2);
		network.addNode(rootE);
		network.addEdge(rootWordNode1, new long[]{rootE});
		network.addEdge(rootWordNode2, new long[]{rootE});
		
		for(int rightIndex = 1; rightIndex<=this.maxSentLen-1;rightIndex++){
			//eIndex: 1,2,3,4,5,..n
			for(String e: types){
				if(e.equals(OE)) continue;
				long wordLeftNodeE = this.toNode(rightIndex, rightIndex, 0, 1, e);
				long wordRightNodeE = this.toNode(rightIndex, rightIndex, 1, 1, e);
				network.addNode(wordLeftNodeE);
				network.addNode(wordRightNodeE);
				for(String pae:types){
					if(!e.equals(ONE) && pae.equals(ONE)) continue;
					if(!pae.equals(ONE) && !pae.equals(OE) && !e.equals(pae)) continue;
					
					long wordRightNode = this.toNode(rightIndex, rightIndex, 1, 1,PARENT_IS+pae);
					network.addNode(wordRightNode);
					network.addEdge(wordRightNode, new long[]{wordRightNodeE});
					if(pae.equals(OE) && !e.equals(ONE)) continue;
					long wordLeftNode = this.toNode(rightIndex, rightIndex, 0, 1,PARENT_IS+pae);
					network.addNode(wordLeftNode);
					network.addEdge(wordLeftNode, new long[]{wordLeftNodeE});
					
					
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
						boolean addedPaeLink = false;
						boolean haveAdded = false;
						if(leftIndex==0 && direction==0) continue;
						if(complete==0){
							//incomplete span decompose to two complete spans
							
							for(int m=leftIndex;m<rightIndex;m++){
								for(int t=0;t<types.length;t++){
									long parentE = this.toNode(leftIndex, rightIndex, direction, complete, types[t]);
									long child_1 = this.toNode(leftIndex, m, 1, 1, PARENT_IS+types[t]);
									long child_2 = this.toNode(m+1, rightIndex, 0, 1, PARENT_IS+types[t]);
									if(network.contains(child_1) && network.contains(child_2)){
										network.addNode(parentE);
										network.addEdge(parentE, new long[]{child_1,child_2});
									}
									if(!addedPaeLink){
										for(String pae:types){
											if(types[t].equals(OE) && !pae.equals(OE)) continue;
											if(!types[t].equals(ONE) && !types[t].equals(OE) && pae.equals(ONE)) continue;
											if(!pae.equals(ONE) && !pae.equals(OE) && !types[t].equals(pae)) continue;
											long parent = this.toNode(leftIndex, rightIndex, direction, complete, PARENT_IS+pae);
											if(network.contains(parentE)){
												network.addNode(parent);
												network.addEdge(parent, new long[]{parentE});
												haveAdded = true;
											}
										}
									}
								}
								if(haveAdded) addedPaeLink = true;
							}
						}
						
						if(complete==1 && direction==0){
							for(int m=leftIndex;m<rightIndex;m++){
								for(int t=0;t<types.length;t++){
									long parentE = this.toNode(leftIndex, rightIndex, direction, complete, types[t]);
									long child_1 = this.toNode(leftIndex, m, 0, 1, PARENT_IS+types[t]);
									long child_2 = this.toNode(m, rightIndex, 0, 0, PARENT_IS+types[t]);
									if(network.contains(child_1) && network.contains(child_2)){
										network.addNode(parentE);
										network.addEdge(parentE, new long[]{child_1,child_2});
									}
									if(!addedPaeLink){
										for(String pae:types){
											if(types[t].equals(OE) && !pae.equals(OE)) continue;
											if(!types[t].equals(ONE) && !types[t].equals(OE) && pae.equals(ONE)) continue;
											if(!pae.equals(ONE) && !pae.equals(OE) && !types[t].equals(pae)) continue;
											if(pae.equals(OE) && !types[t].equals(OE) && !types[t].equals(ONE)) continue;
											long parent = this.toNode(leftIndex, rightIndex, direction, complete, PARENT_IS+pae);
											if(network.contains(parentE)){
												network.addNode(parent);
												network.addEdge(parent, new long[]{parentE});
												haveAdded = true;
											}
										}
									}
								}
								if(haveAdded) addedPaeLink = true;
							}
						}
						
						if(complete==1 && direction==1){
							
							for(int m=leftIndex+1;m<=rightIndex;m++){
								for(int t=0;t<types.length;t++){
									long parentE  = this.toNode(leftIndex, rightIndex, direction, complete, types[t]);
									long child_1 = this.toNode(leftIndex, m, 1, 0,PARENT_IS+ types[t]);
									long child_2 = this.toNode(m, rightIndex, 1, 1,PARENT_IS+ types[t]);
									if(network.contains(child_1) && network.contains(child_2)){
										network.addNode(parentE);
										network.addEdge(parentE, new long[]{child_1,child_2});
									}
									if(!addedPaeLink){
										if(leftIndex==0 && rightIndex>leftIndex){
											long subRoot = this.toNode(leftIndex, rightIndex, direction, complete, PARENT_IS+"null");
											if(network.contains(parentE)){
												network.addNode(subRoot);
												network.addEdge(subRoot, new long[]{parentE});
											}
										}
										//since complete span from span>=2, it should always same as the parent.
										for(String pae:types){
											if(types[t].equals(OE) && !pae.equals(OE)) continue;
											if(!types[t].equals(ONE) && !types[t].equals(OE) && pae.equals(ONE)) continue;
											if(!pae.equals(ONE) && !pae.equals(OE) && !types[t].equals(pae)) continue;
											if(pae.equals(OE) && !types[t].equals(OE) && !types[t].equals(ONE)) continue;
											long parent = this.toNode(leftIndex, rightIndex, direction, complete, PARENT_IS+pae);
											if(network.contains(parentE)){
												network.addNode(parent);
												network.addEdge(parent, new long[]{parentE});
												haveAdded = true;
											}
										}
									}
								}
								if(haveAdded) addedPaeLink = true;
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
		//viewer.visualizeNetwork(network, null, "Labeled Model");
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
		rootLabel.setValue("0,"+(inst.getInput().length()-1)+",1,1,"+PARENT_IS+"null");
		root.setLabel(rootLabel);
		this.toTreeHelper(network, network.countNodes()-1, root);
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
			//ids_child: rightIndex, rightIndex-leftIndex, completeness, direction.
			sb.append(leftIndex);  sb.append(",");
			sb.append(ids_child[0]);  sb.append(",");
			sb.append(ids_child[3]); sb.append(",");
			sb.append(ids_child[2]); sb.append(",");
			String type = null;
			if(ids_child[4]==2*types.length)
				type = PARENT_IS+"null";
			else if(ids_child[4]>types.length-1){
				type = PARENT_IS+types[ids_child[4]-types.length];
			}else{
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
		
		if(arr[4]==2*types.length)
			type = "pae:null";
		else if(arr[4]>types.length-1){
			type = "pae:"+types[arr[4]-types.length];
		}else{
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
		if(!typeMap.containsKey(type) && !type.equals(PARENT_IS+"null")){
			System.err.println("The type is:"+type);
		}
		if(type.equals(PARENT_IS+"null"))
			return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex,rightIndex-leftIndex,complete, direction, typeMap.size(),NODE.normal.ordinal()});
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex,rightIndex-leftIndex,complete, direction, typeMap.get(type),NODE.normal.ordinal()});
	}
	

}

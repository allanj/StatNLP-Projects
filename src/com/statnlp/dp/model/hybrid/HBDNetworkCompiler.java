package com.statnlp.dp.model.hybrid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
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

public class HBDNetworkCompiler extends NetworkCompiler {

	private static final long serialVersionUID = -5080640847287255079L;

	private long[] _nodes;
	private final int maxSentLen = 128;
	private int[][][] _children;
	private HashMap<String, Integer> typeMap;
	private String[] types;
	private enum NODE {normal};
	private  VisualizationViewerEngine viewer;
	public static String PARENT_IS = DPConfig.PARENT_IS;
	public static String OE = DPConfig.OE;
	public static String O_TYPE = DPConfig.O_TYPE;
	public static String ONE = DPConfig.ONE;
	public static String E_B_PREFIX = DPConfig.E_B_PREFIX;
	public static String E_I_PREFIX = DPConfig.E_I_PREFIX;
	
	private static boolean DEBUG = false;
	
	/**
	 * Compiler constructor
	 * @param typeMap: typeMap from DPConfig which also include those type with "pae"
	 */
	public HBDNetworkCompiler(HashMap<String, Integer> typeMap, VisualizationViewerEngine viewer) {
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
		HBDInstance di = (HBDInstance)inst;
		if(di.isLabeled()){
			//System.err.println("[Info] Compiling Labeled Network...");
			return this.compileLabledInstance(networkId, di, param);
		}else{
			//System.err.println("[Info] Compiling Unlabeled Network...");
			return this.compileUnLabledInstance(networkId, di, param);
		}
	}

	public HBDNetwork compileLabledInstance(int networkId, HBDInstance inst, LocalNetworkParam param){
		HBDNetwork network = new HBDNetwork(networkId,inst,param);
		Sentence sent = inst.getInput();
		Tree tree = inst.getOutput();
		this.compile(network, sent, tree);
		if(DEBUG){
			HBDNetwork unlabeled = compileUnLabledInstance(networkId, inst, param);
			if(!unlabeled.contains(network)){
				System.err.println(sent.toString());
				throw new NetworkException("Labeled network is not contained in the unlabeled version");
			}
		}
		return network;
	}
	
	private void compile(HBDNetwork network, Sentence sent, Tree output){
		output.setSpans();
		long rootNode = this.toNode_generalRoot(sent.length());
		network.addNode(rootNode);
		addToNetwork(network,output);
		HBDInstance inst = (HBDInstance)network.getInstance();
		String[][] leavesType = inst.getLeavesType();
		long child = this.toNode(0, 0, 1, 1, ONE);
		for(int pos=1; pos<sent.length(); pos++){
			for(int direction=0;direction<=1;direction++){
				long parent = this.toNode(pos, pos, direction, 1, leavesType[pos][direction]);
				network.addEdge(parent, new long[]{child});
				child = parent;
			}
		}
		long lastWord = this.toNode(sent.length()-1, sent.length()-1, 1, 1, typeMap.size());
		network.addNode(lastWord);
		network.addEdge(lastWord, new long[]{child});
		network.finalizeNetwork();
		//viewer.visualizeNetwork(network, null, "Labeled Network");
		//System.err.println(network);
		//System.err.println(output.pennString());
		
	}
	
	private void addToNetwork(HBDNetwork network, Tree parent){
		if(parent.isLeaf()) return; //means headindex==modifier index
		CoreLabel cl = (CoreLabel)(parent.label());
		String[] info = cl.value().split(",");
		int pa_leftIndex = Integer.valueOf(info[0]);
		int pa_rightIndex = Integer.valueOf(info[1]);
		int pa_direction = Integer.valueOf(info[2]);
		int pa_completeness = Integer.valueOf(info[3]);
		String pa_type = info[4];
		if(pa_leftIndex==pa_rightIndex && !pa_type.startsWith(PARENT_IS)) return; //means the span width now is 1, already enough
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
	
	public HBDNetwork compileUnLabledInstance(int networkId, HBDInstance inst, LocalNetworkParam param){
		if(this._nodes==null){
			this.compileUnlabeled();
		}
		long root = this.toNode_generalRoot(inst.getInput().length());
		int rootIdx = Arrays.binarySearch(this._nodes, root);
		HBDNetwork network = new HBDNetwork(networkId,inst,this._nodes,this._children, param,rootIdx+1 );
		//viewer.visualizeNetwork(network, null, "UnLabeled Network");
		//System.err.println("[Info] Compile Unlabeled instance, length: "+inst.getInput().length());
		//System.err.println("My root:"+Arrays.toString(NetworkIDMapper.toHybridNodeArray(root)));
		//System.err.println("root index:"+rootIdx);
		//System.err.println(Arrays.toString(NetworkIDMapper.toHybridNodeArray(this._nodes[this._nodes.length-5])));
		//System.err.println("Root: "+ Arrays.toString(NetworkIDMapper.toHybridNodeArray(root)));
		//System.err.println("Number of nodes under this root Index: "+ (rootIdx+1));
		//System.err.println(network.toString());
		return network;
	}
	
	public synchronized void compileUnlabeled(){
		if(this._nodes!=null){
			return;
		}
		HBDNetwork network = new HBDNetwork();
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
		ArrayList<Long> children = new ArrayList<Long>();
		children.add(rootE);
		
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
				for(String pae:types){
					if(!e.equals(ONE) && pae.equals(ONE)) continue;
					if(!pae.equals(ONE) && !pae.equals(OE) && !e.equals(pae)) continue;
					
					long wordRightNode = this.toNode(rightIndex, rightIndex, 1, 1,PARENT_IS+pae);
					network.addNode(wordRightNode);
					network.addEdge(wordRightNode, new long[]{wordRightNodeE});
					//Should use the first one. can yield the best result
					//if(wordLeftNodeE!=-1 && !(pae.equals(OE) && isEntity(e)) ){
					boolean exp= wordLeftNodeE!=-1 && !(pae.equals(OE) && isEntity(e));
//					if(DPConfig.readWeight) exp = wordLeftNodeE!=-1;
//					else exp = wordLeftNodeE!=-1 && !(pae.equals(OE) && isEntity(e));
					if(exp){
						long wordLeftNode = this.toNode(rightIndex, rightIndex, 0, 1,PARENT_IS+pae);
						network.addNode(wordLeftNode);
						network.addEdge(wordLeftNode, new long[]{wordLeftNodeE});
					}
					
				}
			}
			
			//for linear chain structure at the end.
			ArrayList<Long> updateChildren = new ArrayList<Long>();
			for(String le: types){
				//add the adjacent word links first.
				long parent = this.toNode(rightIndex, rightIndex, 0, 1, le);
				if(!network.contains(parent)) continue;
				boolean added = false;
				int typeIdx = typeMap.get(le);
				for(long child: children){
					int[] childArr = NetworkIDMapper.toHybridNodeArray(child);
					if(isEntity(le) && typeIdx!=childArr[4]) continue;
					network.addEdge(parent, new long[]{child});
					added=true;
				}
				if(added) updateChildren.add(parent);
			}
			children = updateChildren;
			updateChildren = new ArrayList<Long>();
			for(String re: types){
				//add the same word link
				long parent = this.toNode(rightIndex, rightIndex, 1, 1, re);
				if(!network.contains(parent)) continue;
				boolean added = false;
				for(long child: children){
					int[] childArr = NetworkIDMapper.toHybridNodeArray(child);
					String ce = types[childArr[4]];
					if(isEntity(re) && isEntity(ce) && !re.equals(ce)) continue;
					network.addEdge(parent, new long[]{child});
					added=true;
				}
				if(added) updateChildren.add(parent);
			}
			children = updateChildren;
			
			long lastWordRoot = this.toNode(rightIndex, rightIndex, 1, 1, typeMap.size());
			network.addNode(lastWordRoot);
			for(long child:children){
				network.addEdge(lastWordRoot, new long[]{child});
			}

			
			for(int L=1;L<=rightIndex;L++){
				//L:(1),(1,2),(1,2,3),(1,2,3,4),(1,2,3,4,5),...(1..n)
				//bIndex:(0),(1,0),(2,1,0),(3,2,1,0),(4,3,2,1,0),...(n-1..0)
				//span: {(0,1)},{(1,2),(0,2)}
				int leftIndex = rightIndex - L;
				//span:[bIndex, rightIndex]
				
				for(int complete=0;complete<=1;complete++){
					for(int direction=0;direction<=1;direction++){
						boolean[] addedPaeLink = new boolean[types.length];
						for(int x=0;x<addedPaeLink.length;x++) addedPaeLink[x] = false;
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
									if(!addedPaeLink[t]){
										for(String pae:types){
											if(types[t].equals(OE) && !pae.equals(OE)) continue;
											if(!types[t].equals(ONE) && !types[t].equals(OE) && pae.equals(ONE)) continue;
											if(!pae.equals(ONE) && !pae.equals(OE) && !types[t].equals(pae)) continue;
											long parent = this.toNode(leftIndex, rightIndex, direction, complete, PARENT_IS+pae);
											if(network.contains(parentE)){
												network.addNode(parent);
												network.addEdge(parent, new long[]{parentE});
												addedPaeLink[t] = true;
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
									long child_1 = this.toNode(leftIndex, m, 0, 1, PARENT_IS+types[t]);
									long child_2 = this.toNode(m, rightIndex, 0, 0, PARENT_IS+types[t]);
									if(network.contains(child_1) && network.contains(child_2)){
										network.addNode(parentE);
										network.addEdge(parentE, new long[]{child_1,child_2});
									}
									if(!addedPaeLink[t]){
										for(String pae:types){
											if(types[t].equals(OE) && !pae.equals(OE)) continue;
											if(!types[t].equals(ONE) && !types[t].equals(OE) && pae.equals(ONE)) continue;
											if(!pae.equals(ONE) && !pae.equals(OE) && !types[t].equals(pae)) continue;
											if(pae.equals(OE) && !types[t].equals(OE) && !types[t].equals(ONE)) continue;
											long parent = this.toNode(leftIndex, rightIndex, direction, complete, PARENT_IS+pae);
											if(network.contains(parentE)){
												network.addNode(parent);
												network.addEdge(parent, new long[]{parentE});
												addedPaeLink[t] = true;
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
									long child_1 = this.toNode(leftIndex, m, 1, 0,PARENT_IS+ types[t]);
									long child_2 = this.toNode(m, rightIndex, 1, 1,PARENT_IS+ types[t]);
									if(network.contains(child_1) && network.contains(child_2)){
										network.addNode(parentE);
										network.addEdge(parentE, new long[]{child_1,child_2});
									}
									if(!addedPaeLink[t]){
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
												addedPaeLink[t] = true;
											}
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
		//viewer.visualizeNetwork(network, null, "UnLabeled Model");
		//printNetwork(network, null);
		//System.exit(0);
	}
	
	
	@Override
	public Instance decompile(Network network) {
		HBDNetwork dependNetwork = (HBDNetwork)network;
		HBDInstance inst = (HBDInstance)(dependNetwork.getInstance());
		inst = inst.duplicate();
		if(dependNetwork.getMax()==Double.NEGATIVE_INFINITY) return inst;
		//viewer.visualizeNetwork(dependNetwork, null, "Testing Labeled Model:"+network.getInstance().getInstanceId());
		Tree forest = this.toTree(dependNetwork,inst);
//		printNetwork(dependNetwork, (Sentence)dependNetwork.getInstance().getInput());
		if(DEBUG) System.err.println("[Result] "+forest.pennString());
		inst.setPrediction(forest);
		inst.setLinearPrediction(toLinear(dependNetwork, inst));
		return inst;
	}
	
	private Tree toTree(HBDNetwork network,HBDInstance inst){
		
		Tree root = new LabeledScoredTreeNode();
		CoreLabel rootLabel = new CoreLabel();
		rootLabel.setValue("0,"+(inst.getInput().length()-1)+",1,1,"+PARENT_IS+"null");
		root.setLabel(rootLabel);
		this.toTreeHelper(network, network.countNodes()-1, root);
		return root;
	}
	
	private void toTreeHelper(HBDNetwork network, int node_k, Tree parentNode){
		int[] children_k = network.getMaxPath(node_k);
//		System.err.println("node_k:"+node_k+" score:"+network.getMax(node_k));
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
	
	private String[] toLinear(HBDNetwork network, HBDInstance inst){
		Sentence sent = inst.getInput();
		String[][] leavesType = new String[sent.length()][2];
		long lastWordRoot = this.toNode(sent.length()-1, sent.length()-1, 1, 1, typeMap.size());
		long[] nodes = network.getAllNodes();
		int lastIdx = Arrays.binarySearch(nodes, lastWordRoot);
		for(int i=sent.length()-1;i>0;i--){
			for(int times = 0;times<=1;times++){
				int[] children_k = network.getMaxPath(lastIdx);
				long child = network.getNode(children_k[0]);
				int[] carr = NetworkIDMapper.toHybridNodeArray(child);
				int direction = carr[3];
				if(types[carr[4]].equals(ONE))
					leavesType[i][direction] = O_TYPE;
				else
					leavesType[i][direction] = types[carr[4]];
				lastIdx = children_k[0];
			}
		}
		String[] es = new String[sent.length()];
		String[] res = new String[sent.length()];
		es[0] = O_TYPE;
		res[0] = O_TYPE;
		for(int i=1;i<leavesType.length;i++){
			if(leavesType[i][0].equals(leavesType[i][1]))
				es[i] = leavesType[i][0];
			else{
				es[i] = leavesType[i][0].equals(O_TYPE)? leavesType[i][1]:leavesType[i][0];
			}
			if(!es[i].equals(es[i-1])){
				if(!es[i].equals(O_TYPE))
					res[i] = E_B_PREFIX+es[i];
				else res[i] = es[i];
			}else{
				if(es[i].equals(O_TYPE))
					res[i] = es[i];
				else res[i] = E_I_PREFIX+es[i];
			}
		}
		return res;
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
	
	public long toNode(int leftIndex, int rightIndex, int direction, int complete, int typeIdx){
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex,rightIndex-leftIndex,complete, direction, typeIdx,NODE.normal.ordinal()});
	}
	
	private boolean isEntity(String type){
		return !type.equals(OE) &&!type.equals(ONE);
	}

}

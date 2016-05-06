package com.statnlp.dp.model.labelleddp;

import java.util.Arrays;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkIDMapper;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;

public class LDPNetworkCompiler extends NetworkCompiler {

	private static final long serialVersionUID = -5080640847287255079L;

	private long[] _nodes;
	private int maxSentLen = 128;
	private int[][][] _children;
	private String rootDepLabel = LDPConfig.rootDepLabel;
	
	public LDPNetworkCompiler() {
		
	}

	@Override
	public Network compile(int networkId, Instance inst, LocalNetworkParam param) {
		NetworkIDMapper.setCapacity(new int[]{1000,1000,1000,1000,1000,1000});
		LDPInstance di = (LDPInstance)inst;
		if(di.isLabeled()){
			return this.compileLabledInstance(networkId, di, param);
		}else{
			return this.compileUnLabledInstance(networkId, di, param);
		}
	}

	public LDPNetwork compileLabledInstance(int networkId, LDPInstance inst, LocalNetworkParam param){
		LDPNetwork network = new LDPNetwork(networkId,inst,param);
		Sentence sent = inst.getInput();
		Tree tree = inst.getOutput();
		return this.compile(network, sent, tree);
	}
	
	private LDPNetwork compile(LDPNetwork network, Sentence sent, Tree output){
		output.setSpans();
		long rootNode = this.toNode_root(sent.length());
		network.addNode(rootNode);
		
		addToNetwork(network,output);
		network.finalizeNetwork();

		return network;
	}

	
	
	private void addToNetwork(LDPNetwork network, Tree parent){
		if(parent.isLeaf()) return; //means headindex==modifier index
		CoreLabel cl = (CoreLabel)(parent.label());
		String[] info = cl.value().split(",");
		int pa_leftIndex = Integer.valueOf(info[0]);
		int pa_rightIndex = Integer.valueOf(info[1]);
		int pa_direction = Integer.valueOf(info[2]);
		int pa_completeness = Integer.valueOf(info[3]);
		String label = info[4];
		if(pa_leftIndex==pa_rightIndex) return; //means the span width now is 1, already enough
		long parentNode = toNode(pa_leftIndex, pa_rightIndex, pa_direction, pa_completeness,label);
		Tree[] children = parent.children();
		if(children.length!=2){
			throw new RuntimeException("The children length should be 2 in the labeled tree.");
		}
		CoreLabel childLabel_1 = (CoreLabel)(children[0].label());
		String[] childInfo_1 = childLabel_1.value().split(",");
		int child_leftIndex_1 = Integer.valueOf(childInfo_1[0]);
		int child_rightIndex_1 = Integer.valueOf(childInfo_1[1]);
		int child_direction_1 = Integer.valueOf(childInfo_1[2]);
		int child_completeness_1 = Integer.valueOf(childInfo_1[3]);
		String child_label_1 = childInfo_1[4];
		long childNode_1 = toNode(child_leftIndex_1, child_rightIndex_1, child_direction_1, child_completeness_1,child_label_1);
		network.addNode(childNode_1);
		
		CoreLabel childLabel_2 = (CoreLabel)(children[1].label());
		String[] childInfo_2 = childLabel_2.value().split(",");
		int child_leftIndex_2 = Integer.valueOf(childInfo_2[0]);
		int child_rightIndex_2 = Integer.valueOf(childInfo_2[1]);
		int child_direction_2 = Integer.valueOf(childInfo_2[2]);
		int child_completeness_2 = Integer.valueOf(childInfo_2[3]);
		String child_label_2 = childInfo_2[4];
//		System.err.println("child 2 Info:"+Arrays.toString(childInfo_2));
		long childNode_2 = toNode(child_leftIndex_2, child_rightIndex_2, child_direction_2, child_completeness_2,child_label_2);
		network.addNode(childNode_2);
//		System.err.println("network node child 2:"+Arrays.toString(NetworkIDMapper.toHybridNodeArray(childNode_2)));
		network.addEdge(parentNode, new long[]{childNode_1,childNode_2});
		addToNetwork(network, children[0]);
		addToNetwork(network, children[1]);
		
	}
	
	public LDPNetwork compileUnLabledInstance(int networkId, LDPInstance inst, LocalNetworkParam param){
		if(this._nodes==null){
			this.compileUnlabeled();
		}
//		System.err.println("[Info] Compile Unlabeled instance, length: "+inst.getInput().length());
		long root = this.toNode_root(inst.getInput().length());
		int rootIdx = Arrays.binarySearch(this._nodes, root);
//		System.err.println("Number of nodes under this root Index: "+ (rootIdx+1));

		LDPNetwork network = new LDPNetwork(networkId,inst,this._nodes,this._children, param,rootIdx+1 );
		
		return network;
	}
	
	
	public synchronized void compileUnlabeled(){
		if(this._nodes!=null){
			return;
		}
		LDPNetwork network = new LDPNetwork();
		//add the root word and other nodes
		//all are complete nodes.
		long rootWordNode = this.toNode(0, 0, 1, 1, DepLabel.LABELS.size());
		network.addNode(rootWordNode);
		
		for(int rightIndex = 1; rightIndex<=this.maxSentLen-1;rightIndex++){
			//eIndex: 1,2,3,4,5,..n
			
			long wordLeftNode = this.toNode(rightIndex, rightIndex, 0, 1, DepLabel.LABELS.size());
			long wordRightNode = this.toNode(rightIndex, rightIndex, 1, 1, DepLabel.LABELS.size());
			network.addNode(wordLeftNode);
			network.addNode(wordRightNode);
			
			for(int L=1;L<=rightIndex;L++){
				//L:(1),(1,2),(1,2,3),(1,2,3,4),(1,2,3,4,5),...(1..n)
				//bIndex:(0),(1,0),(2,1,0),(3,2,1,0),(4,3,2,1,0),...(n-1..0)
				//span: {(0,1)},{(1,2),(0,2)}
				int leftIndex = rightIndex - L;
				//span:[bIndex, rightIndex]
				
				for(int complete=0;complete<=1;complete++){
					for(int direction=0;direction<=1;direction++){
						
//						System.err.println("[Info] Network Parent: "+Arrays.toString(NetworkIDMapper.toHybridNodeArray(parent)));
						if(leftIndex==0 && direction==0) continue;
						if(complete==0){
							for(int m=leftIndex;m<rightIndex;m++){
								long child_1 = this.toNode(leftIndex, m, 1, 1, DepLabel.LABELS.size());
//								System.err.println("[Info] Network child 1: "+Arrays.toString(NetworkIDMapper.toHybridNodeArray(child_1)));
								long child_2 = this.toNode(m+1, rightIndex, 0, 1, DepLabel.LABELS.size());
//								System.err.println("[Info] Network child 2: "+Arrays.toString(NetworkIDMapper.toHybridNodeArray(child_2)));
								if(network.contains(child_1) && network.contains(child_2)){
									if(leftIndex==0 && direction==1){
										long parent = this.toNode(leftIndex, rightIndex, direction, complete, DepLabel.LABELS.get(rootDepLabel).getForm());
										network.addNode(parent);
										network.addEdge(parent, new long[]{child_1,child_2});
									}else{
										for(int l=0;l<DepLabel.LABELS.size();l++){
											long parent = this.toNode(leftIndex, rightIndex, direction, complete,l);
											network.addNode(parent);
											network.addEdge(parent, new long[]{child_1,child_2});
										}
									}
								}
								
							}
						}
						
						if(complete==1 && direction==0){
							long parent = this.toNode(leftIndex, rightIndex, direction, complete,  DepLabel.LABELS.size());
							for(int m=leftIndex;m<rightIndex;m++){
								long child_1 = this.toNode(leftIndex, m, 0, 1, DepLabel.LABELS.size());
								
								for(int l=0;l<DepLabel.LABELS.size();l++){
									long child_2 = this.toNode(m, rightIndex, 0, 0, l);
									if(network.contains(child_1) && network.contains(child_2)){
										network.addNode(parent);
										network.addEdge(parent, new long[]{child_1,child_2});
									}
								}
								
							}
						}
						
						if(complete==1 && direction==1){
							long parent = this.toNode(leftIndex, rightIndex, direction, complete,  DepLabel.LABELS.size());
							for(int m=leftIndex+1;m<=rightIndex;m++){
								long child_2 = this.toNode(m, rightIndex, 1, 1, DepLabel.LABELS.size());
								for(int l=0;l<DepLabel.LABELS.size();l++){
									long child_1 = this.toNode(leftIndex, m, 1, 0,l);
									if(network.contains(child_1) && network.contains(child_2)){
										network.addNode(parent);
										network.addEdge(parent, new long[]{child_1,child_2});
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
		
		System.err.println(network.countNodes()+" nodes..");
//		printNetwork(network, null);
	}
	
	
	@Override
	public Instance decompile(Network network) {
		LDPNetwork dependNetwork = (LDPNetwork)network;
		LDPInstance inst = (LDPInstance)(dependNetwork.getInstance());
		inst = inst.duplicate();
		if(dependNetwork.getMax()==Double.NEGATIVE_INFINITY) return inst;
		Tree forest = this.toTree(dependNetwork,inst);
//		System.err.println("[Result] "+forest.toString());
		inst.setPrediction(forest);
		return inst;
	}
	
	private Tree toTree(LDPNetwork network,LDPInstance inst){
		
		Tree root = new LabeledScoredTreeNode();
		CoreLabel rootLabel = new CoreLabel();
		rootLabel.setValue("0,"+(inst.getInput().length()-1)+",1,1");
		root.setLabel(rootLabel);
		this.toTreeHelper(network, network.countNodes()-1, root);
		return root;
	}
	
	private void toTreeHelper(LDPNetwork network, int node_k, Tree parentNode){
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
			sb.append(ids_child[2]);
			childLabel.setValue(sb.toString());
			childNode.setLabel(childLabel);
			parentNode.addChild(childNode);
			this.toTreeHelper(network, children_k[k], childNode);
		}
		
	}
	
	
	public void printNetwork(LDPNetwork network, Sentence sent){
//		DependInstance inst = (DependInstance)network.getInstance();
		int len = -1;
		if(sent==null) len = this.maxSentLen;
		else len = sent.length();
		long root = this.toNode_root(len);
		long[] nodes = network.getAllNodes();
		System.err.println("Number of nodes: "+nodes.length);
		int rootIndex = Arrays.binarySearch(nodes, root);
		System.err.println("root node: "+Arrays.toString(NetworkIDMapper.toHybridNodeArray(root))+" , at index:"+rootIndex);
		int level = 0;
//		System.err.println("node at index 3:"+Arrays.toString(NetworkIDMapper.toHybridNodeArray(network.getNode(3))));
		printAll(rootIndex,network,level);
	}
	
	private void printAll(int index, LDPNetwork network, int level){
		long current = network.getNode(index);
		for(int i=0;i<level;i++)
			System.err.print("\t");
		int[] arr = NetworkIDMapper.toHybridNodeArray(current);
		int leftIndex = arr[0]-arr[1];
		String direction = arr[3]==0? "left":"right";
		String complete = arr[2]==0? "incomplete":"complete";
		System.err.println("current node: "+leftIndex+","+arr[0]+","+direction+","+complete+" , at index:"+index);
		int[][] children = network.getChildren(index);
		for(int i=0;i<children.length;i++){
			int[] twochilds = children[i];
			for(int j=0;j<twochilds.length;j++)
				printAll(twochilds[j],network,level+1);
		}
	}
	
	//Node composition
	//Span Len (eIndex-bIndex), eIndex, direction(0--left,1--right), complete (0--incomplete,1), node Type
	public long toNode_root(int sentLen){
		int sentence_len = sentLen;
		//Largest span and the node id is sentence len, because the id 0 to sentence len-1
		return NetworkIDMapper.toHybridNodeID(new int[]{sentence_len-1, sentence_len-1,1,1,DepLabel.LABELS.size(),  Network.NODE_TYPE.max.ordinal()});
	}
	
	public long toNode(int leftIndex, int rightIndex, int direction, int complete, String label){
		//dfn has the id the sentence.
		//could be same span but not same word id.
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex,rightIndex-leftIndex,complete, direction, DepLabel.get(label).getId() ,Network.NODE_TYPE.max.ordinal()});
	}
	
	public long toNode(int leftIndex, int rightIndex, int direction, int complete, int label_id){
		//dfn has the id the sentence.
		//could be same span but not same word id.
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex,rightIndex-leftIndex,complete, direction, label_id ,Network.NODE_TYPE.max.ordinal()});
	}

}

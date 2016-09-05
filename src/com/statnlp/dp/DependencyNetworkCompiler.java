package com.statnlp.dp;

import java.util.Arrays;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.commons.DepLabel;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkIDMapper;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;

public class DependencyNetworkCompiler extends NetworkCompiler {

	private static final long serialVersionUID = -5080640847287255079L;

	private long[] _nodes;
	private int maxSentLen = 143;
	private int[][][] _children;
	private boolean labeledDep;
	
	public DependencyNetworkCompiler(boolean labeledDep) {
		this.labeledDep = labeledDep;
		NetworkIDMapper.setCapacity(new int[]{500, 500, 5, 5, 100, 10});
	}

	@Override
	public Network compile(int networkId, Instance inst, LocalNetworkParam param) {
		DependInstance di = (DependInstance)inst;
		if(di.isLabeled()){
//			System.err.println("[Info] Compiling labeled data....");
//			System.err.println("[info] Instance:"+di.getInput().toString());
			return this.compileLabledInstance(networkId, di, param);
		}else{
//			System.err.println("[Info] Compiling Unlabeled data....");
//			System.err.println("[info] Instance:"+di.getInput().toString());
			return this.compileUnLabledInstance(networkId, di, param);
//			return this.debug(networkId, di, param);
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
//		CoreLabel core_label = (CoreLabel)(output.label());
//		String[] info = core_label.value().split(",");
//		int direction = Integer.valueOf(info[0]);
//		int completeness = Integer.valueOf(info[1]);
		long rootNode = this.toNode_root(sent.length());
//		IntPair span_pair = output.getSpan();
//		long rootNode2 = this.toNode(span_pair.getSource(), span_pair.getTarget(), 1, 1);
		//should be same as the root node I created.
		network.addNode(rootNode);
		
		addToNetwork(network,output);
		network.finalizeNetwork();
//		printNetwork(network,sent);
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
//		System.err.println("Parent Info:"+Arrays.toString(info));
//		if(pa_leftIndex==pa_rightIndex-1) return; //means the span width now is 2, already enough
		if(pa_leftIndex==pa_rightIndex) return; //means the span width now is 1, already enough

		Sentence sent = ((DependInstance)network.getInstance()).getInput();
		int pa_depLabelId = -1;
		if(!labeledDep || pa_completeness==1) pa_depLabelId = DepLabel.LABELS.size();
		else{
			String depLabel = pa_direction==1? sent.get(pa_rightIndex).getDepLabel():sent.get(pa_leftIndex).getDepLabel();
			if(!DepLabel.LABELS.containsKey(depLabel)) throw new RuntimeException("not contain this label");
			pa_depLabelId = DepLabel.get(depLabel).getId();
		}
		long parentNode = toNode(pa_leftIndex, pa_rightIndex, pa_direction, pa_completeness, pa_depLabelId);
		Tree[] children = parent.children();
		if(children.length!=2){
			throw new RuntimeException("The children length should be 2 in the labeled tree.");
		}
		CoreLabel childLabel_1 = (CoreLabel)(children[0].label());
		String[] childInfo_1 = childLabel_1.value().split(",");
//		System.err.println("child 1 Info:"+Arrays.toString(childInfo_1));
		int child_leftIndex_1 = Integer.valueOf(childInfo_1[0]);
		int child_rightIndex_1 = Integer.valueOf(childInfo_1[1]);
		int child_direction_1 = Integer.valueOf(childInfo_1[2]);
		int child_completeness_1 = Integer.valueOf(childInfo_1[3]);
		
		int c1_depLabelId = -1;
		if(!labeledDep || child_completeness_1==1) c1_depLabelId = DepLabel.LABELS.size();
		else{
			String depLabel = child_direction_1==1? sent.get(child_rightIndex_1).getDepLabel():sent.get(child_leftIndex_1).getDepLabel();
			if(!DepLabel.LABELS.containsKey(depLabel)) throw new RuntimeException("not contain this label");
			c1_depLabelId = DepLabel.get(depLabel).getId();
		}
		long childNode_1 = toNode(child_leftIndex_1, child_rightIndex_1, child_direction_1, child_completeness_1, c1_depLabelId);
//		System.err.println("network node child 1:"+Arrays.toString(NetworkIDMapper.toHybridNodeArray(childNode_1)));
		network.addNode(childNode_1);
		
		CoreLabel childLabel_2 = (CoreLabel)(children[1].label());
		String[] childInfo_2 = childLabel_2.value().split(",");
		int child_leftIndex_2 = Integer.valueOf(childInfo_2[0]);
		int child_rightIndex_2 = Integer.valueOf(childInfo_2[1]);
		int child_direction_2 = Integer.valueOf(childInfo_2[2]);
		int child_completeness_2 = Integer.valueOf(childInfo_2[3]);
//		System.err.println("child 2 Info:"+Arrays.toString(childInfo_2));
		int c2_depLabelId = -1;
		if(!labeledDep || child_completeness_2==1) c2_depLabelId = DepLabel.LABELS.size();
		else{
			String depLabel = child_direction_2==1? sent.get(child_rightIndex_2).getDepLabel():sent.get(child_leftIndex_2).getDepLabel();
			if(!DepLabel.LABELS.containsKey(depLabel)) throw new RuntimeException("not contain this label");
			c2_depLabelId = DepLabel.get(depLabel).getId();
		}
		long childNode_2 = toNode(child_leftIndex_2, child_rightIndex_2, child_direction_2, child_completeness_2,c2_depLabelId);
		network.addNode(childNode_2);
//		System.err.println("network node child 2:"+Arrays.toString(NetworkIDMapper.toHybridNodeArray(childNode_2)));
		network.addEdge(parentNode, new long[]{childNode_1,childNode_2});
		addToNetwork(network, children[0]);
		addToNetwork(network, children[1]);
		
	}
	
	public DependencyNetwork compileUnLabledInstance(int networkId, DependInstance inst, LocalNetworkParam param){
		if(this._nodes==null){
			this.compileUnlabeled();
		}
//		System.err.println("[Info] Compile Unlabeled instance, length: "+inst.getInput().length());
		long root = this.toNode(0, inst.getInput().length()-1, 1, 1, DepLabel.LABELS.size());
		int rootIdx = Arrays.binarySearch(this._nodes, root);
//		System.err.println("Number of nodes under this root Index: "+ (rootIdx+1));

		DependencyNetwork network = new DependencyNetwork(networkId,inst,this._nodes,this._children, param,rootIdx+1 );
		
		return network;
	}
	
	public DependencyNetwork debug(int networkId, DependInstance inst, LocalNetworkParam param){
		DependencyNetwork network = new DependencyNetwork();
		long rootNode = this.toNode(0, 3, 1, 1,1);
		long rootchild1 = this.toNode(0, 2, 1, 0,1);
		long rootchild2 = this.toNode(2, 3, 1, 1,1);
		long leaf1 = this.toNode(0, 0, 1, 1,1);
		long leaf2 = this.toNode(1, 2, 0, 1,1);
		network.addNode(rootNode);
		network.addNode(rootchild1);
		network.addNode(rootchild2);
		network.addEdge(rootNode, new long[]{rootchild1, rootchild2});
		network.addNode(leaf1);
		network.addNode(leaf2);
		network.addEdge(rootchild1, new long[]{leaf1,leaf2});
		long leaf11 = this.toNode(1, 1, 0, 1,1);
		long leaf12 = this.toNode(1, 2, 0, 0,1);
		network.addNode(leaf11);
		network.addNode(leaf12);
		network.addEdge(leaf2, new long[]{leaf11,leaf12});
		long leaf11r = this.toNode(1, 1, 1, 1,1);
		long leaf22l = this.toNode(2, 2, 0, 1,1);
		network.addNode(leaf11r);
		network.addNode(leaf22l);
		network.addEdge(leaf12, new long[]{leaf11r,leaf22l});
		
		long node23 = this.toNode(2, 3, 1, 0,1);
		long leaf33r = this.toNode(3, 3, 1, 1,1);
		network.addNode(node23); network.addNode(leaf33r);
		network.addEdge(rootchild2, new long[]{node23,leaf33r});
		
		long leaf22r = this.toNode(2, 2, 1, 1,1);
		long leaf33l = this.toNode(3, 3, 0, 1,1);
		network.addNode(leaf22r); network.addNode(leaf33l);
		network.addEdge(node23, new long[]{leaf22r,leaf33l});
		
		network.finalizeNetwork();
		this._nodes = network.getAllNodes();
		this._children = network.getAllChildren();
		System.err.println("Debug mode:"+network.countNodes()+" nodes..");
		long root = this.toNode(0, 3, 1, 1,1);
		int rootIdx = Arrays.binarySearch(this._nodes, root);
		DependencyNetwork mynetwork = new DependencyNetwork(networkId, inst, this._nodes, this._children, param, rootIdx+1);
		return mynetwork;
	}
	
	public synchronized void compileUnlabeled(){
		if(this._nodes!=null){
			return;
		}
		DependencyNetwork network = new DependencyNetwork();
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
						long parent = this.toNode(leftIndex, rightIndex, direction, complete, DepLabel.LABELS.size());
						if(leftIndex==0 && direction==0) continue;
						if(complete==0){
							for(int m=leftIndex;m<rightIndex;m++){
								long child_1 = this.toNode(leftIndex, m, 1, 1, DepLabel.LABELS.size());
//								System.err.println("[Info] Network child 1: "+Arrays.toString(NetworkIDMapper.toHybridNodeArray(child_1)));
								long child_2 = this.toNode(m+1, rightIndex, 0, 1, DepLabel.LABELS.size());
//								System.err.println("[Info] Network child 2: "+Arrays.toString(NetworkIDMapper.toHybridNodeArray(child_2)));
								if(network.contains(child_1) && network.contains(child_2)){
									if(labeledDep){
										if(leftIndex==0 && direction==1){
											parent = this.toNode(leftIndex, rightIndex, direction, complete, DepLabel.get(DepLabel.rootDepLabel).getId());
											network.addNode(parent);
											network.addEdge(parent, new long[]{child_1,child_2});
										}else{
											for(int l=0;l<DepLabel.LABELS.size();l++){
												if(DepLabel.get(l).getForm().equals(DepLabel.rootDepLabel)) continue;
												parent = this.toNode(leftIndex, rightIndex, direction, complete,l);
												network.addNode(parent);
												network.addEdge(parent, new long[]{child_1,child_2});
											}
										}
									}else{
										network.addNode(parent);
										network.addEdge(parent, new long[]{child_1,child_2});
									}
									
								}
								
							}
						}
						
						if(complete==1 && direction==0){
							for(int m=leftIndex;m<rightIndex;m++){
								long child_1 = this.toNode(leftIndex, m, 0, 1, DepLabel.LABELS.size());
								long child_2 = this.toNode(m, rightIndex, 0, 0, DepLabel.LABELS.size());
								if(labeledDep){
									for(int l=0;l<DepLabel.LABELS.size();l++){
										if(DepLabel.get(l).getForm().equals(DepLabel.rootDepLabel)) continue;
										child_2 = this.toNode(m, rightIndex, 0, 0, l);
										if(network.contains(child_1) && network.contains(child_2)){
											network.addNode(parent);
											network.addEdge(parent, new long[]{child_1,child_2});
										}
									}
								}else{
									if(network.contains(child_1) && network.contains(child_2)){
										network.addNode(parent);
										network.addEdge(parent, new long[]{child_1,child_2});
									}
								}
								
							}
						}
						
						if(complete==1 && direction==1){
							for(int m=leftIndex+1;m<=rightIndex;m++){
								long child_1 = this.toNode(leftIndex, m, 1, 0, DepLabel.LABELS.size());
								long child_2 = this.toNode(m, rightIndex, 1, 1, DepLabel.LABELS.size());
								if(labeledDep){
									for(int l=0;l<DepLabel.LABELS.size();l++){
										if(leftIndex==0 && !DepLabel.get(l).getForm().equals(DepLabel.rootDepLabel)) continue;
										if(DepLabel.get(l).getForm().equals(DepLabel.rootDepLabel) && leftIndex!=0) continue;
										child_1 = this.toNode(leftIndex, m, 1, 0,l);
										if(network.contains(child_1) && network.contains(child_2)){
											network.addNode(parent);
											network.addEdge(parent, new long[]{child_1,child_2});
										}
									}
								}else{
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
		rootLabel.setValue("0,"+(inst.getInput().length()-1)+",1,1,"+DepLabel.LABELS.size());
		root.setLabel(rootLabel);
		root.setScore(network.getMax());
		this.toTreeHelper(network, network.countNodes()-1, root);
		
		if(NetworkConfig._topKValue>1){
			Tree[] topK = new Tree[NetworkConfig._topKValue];
			double total = 0.0;
			int numK = 0;
			for(int kth=0;kth<NetworkConfig._topKValue;kth++){
				//System.err.println(kth+"-best:");
				Tree kthRoot = new LabeledScoredTreeNode();
				double score = network.getMaxTopK(network.countNodes()-1, kth);
				if(network.getMaxTopK(network.countNodes()-1, kth)== Double.NEGATIVE_INFINITY){
					break;
				}
				total += score;
				kthRoot.setScore(score);
				CoreLabel kthRootLabel = new CoreLabel();
				kthRootLabel.setValue("0,"+(inst.getInput().length()-1)+",1,1,"+DepLabel.LABELS.size());
				kthRoot.setLabel(kthRootLabel);
				this.toTreeHelperTopK(network, network.countNodes()-1, kthRoot, kth);
				topK[kth] = kthRoot;
				numK++;
//				System.err.println(kthRoot.pennString());
			}
			
			for(int kth=0;kth<numK;kth++){
				topK[kth].setScore(topK[kth].score()/total);
				//System.err.println(kth+"-best:"+topK[kth].score());
			}
			inst.setTopKPrediction(topK);
		}
		return root;
	}
	
	private void toTreeHelper(DependencyNetwork network, int node_k, Tree parentNode){
		int[] children_k = network.getMaxPath(node_k);
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
			sb.append(ids_child[4]);
			childLabel.setValue(sb.toString());
			childNode.setLabel(childLabel);
			parentNode.addChild(childNode);
			this.toTreeHelper(network, children_k[k], childNode);
		}
		
	}
	
	private void toTreeHelperTopK(DependencyNetwork network, int node_k, Tree parentNode, int kth){
		//System.err.println(node_k+" with "+(kth+1)+"th "+Arrays.toString(network.getNodeArray(node_k))+":"+network.getMaxTopK(node_k, kth));
		int[] children_k = network.getMaxTopKPath(node_k, kth);
		int[] children_k_bestlist = network.getMaxTopKBestListPath(node_k, kth);
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
			sb.append(ids_child[4]);
			childLabel.setValue(sb.toString());
			childNode.setLabel(childLabel);
			parentNode.addChild(childNode);
			this.toTreeHelperTopK(network, children_k[k], childNode, children_k_bestlist[k]);
		}
	}
	
	
	public void printNetwork(DependencyNetwork network, Sentence sent){
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
	
	private void printAll(int index, DependencyNetwork network, int level){
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
		return NetworkIDMapper.toHybridNodeID(new int[]{sentence_len-1, sentence_len-1,1,1,DepLabel.LABELS.size(),Network.NODE_TYPE.max.ordinal()});
	}
	
	public long toNode(int leftIndex, int rightIndex, int direction, int complete, int depLabelId){
		//dfn has the id the sentence.
		//could be same span but not same word id.
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex,rightIndex-leftIndex,complete, direction,depLabelId,Network.NODE_TYPE.max.ordinal()});
	}

}

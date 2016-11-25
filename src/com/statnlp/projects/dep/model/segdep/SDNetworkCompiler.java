package com.statnlp.projects.dep.model.segdep;

import java.util.Arrays;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkException;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.projects.dep.utils.DPConfig.COMP;
import com.statnlp.projects.dep.utils.DPConfig.DIR;


public class SDNetworkCompiler extends NetworkCompiler {

	private static final long serialVersionUID = -5080640847287255079L;

	private long[] _nodes;
	private final int maxSentLen = 128;
	private int[][][] _children;
	private enum NodeType {normal};
	
	private static boolean DEBUG = true;
	
	private int rightDir = DIR.right.ordinal();
	private int leftDir = DIR.left.ordinal();
	
	/**
	 * Compiler constructor
	 * @param typeMap: typeMap from DPConfig
	 */
	public SDNetworkCompiler() {
		// rightIndex, rightIndex-leftIndex, completeness, direction, entity type, node type
		int[] capacity = new  int[]{500, 500, 5, 5, 10};
		NetworkIDMapper.setCapacity(capacity);
	}
	
	//Node composition
	//rightIndex, (rightIndex-leftIndex), complete (0--incomplete,1), direction(0--left,1--right), leftSpanLen, leftType, rightSpanLen, rightType, node Type
	//sentLen include index 0. 
	public long toNode_root(int sentLen){
		int endIndex = sentLen -1;
		return NetworkIDMapper.toHybridNodeID(new int[]{endIndex, endIndex - 0, COMP.comp.ordinal(), DIR.right.ordinal(), NodeType.normal.ordinal()});
	}
	
	
	public long toNodeIncomp(int leftIndex, int rightIndex, int direction){
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex, rightIndex-leftIndex, COMP.incomp.ordinal(), direction, NodeType.normal.ordinal()});
	}
	
	public long toNodeComp(int leftIndex, int rightIndex, int direction){
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex, rightIndex-leftIndex, COMP.comp.ordinal(), direction, NodeType.normal.ordinal()});
	}
	
	@Override
	public Network compile(int networkId, Instance inst, LocalNetworkParam param) {
		SDInstance di = (SDInstance)inst;
		if(di.isLabeled()){
			//System.err.println("[Info] Compiling Labeled Network...");
			return this.compileLabledInstance(networkId, di, param);
		}else{
			//System.err.println("[Info] Compiling Unlabeled Network...");
			return this.compileUnLabledInstance(networkId, di, param);
		}
	}

	public SDNetwork compileLabledInstance(int networkId, SDInstance inst, LocalNetworkParam param){
		SDNetwork network = new SDNetwork(networkId,inst,param);
		Sentence sent = inst.getInput();
		int[] output = inst.getOutput();
		this.compileLabeled(network, sent, output);
		if(DEBUG){
			SDNetwork unlabeled = compileUnLabledInstance(networkId, inst, param);
			if(!unlabeled.contains(network)){
				System.err.println(sent.toString());
				throw new NetworkException("Labeled network is not contained in the unlabeled version");
			}
		}
		return network;
	}
	
	private void compileLabeled(SDNetwork network, Sentence sent, int[] output){
		long rootE = this.toNodeComp(0, 0, rightDir);
		network.addNode(rootE);
		SDInstance inst = (SDInstance)network.getInstance();
		for(int rightIndex = 1; rightIndex <= sent.length()-1; rightIndex++){
			//eIndex: 1,2,3,4,5,..n
			long wordLeftNode = this.toNodeComp(rightIndex, rightIndex, leftDir);
			long wordRightNode = this.toNodeComp(rightIndex, rightIndex, rightDir);
			network.addNode(wordLeftNode);
			network.addNode(wordRightNode);
			
			for(int L = 1;L <= rightIndex;L++){
				//L:(1),(1,2),(1,2,3),(1,2,3,4),(1,2,3,4,5),...(1..n)
				//bIndex:(0),(1,0),(2,1,0),(3,2,1,0),(4,3,2,1,0),...(n-1..0)
				//span: {(0,1)},{(1,2),(0,2)}
				int leftIndex = rightIndex - L;
				//span:[bIndex, rightIndex]
				for(int complete = 0; complete <= 1; complete++){
					for (int direction = 0; direction <= 1; direction++) {
						if (leftIndex == 0 && direction == 0) continue;
						if(leftIndex==0 && direction==0) continue;
						if(complete==0){
							long parent = this.toNodeIncomp(leftIndex, rightIndex, direction);
							if (direction == rightDir && inst.output[rightIndex] != leftIndex) continue;
							if (direction == leftDir && inst.output[leftIndex] != rightIndex) continue;
							for (int m = leftIndex; m < rightIndex; m++) {
								long child_1 = this.toNodeComp(leftIndex, m, rightDir);
								long child_2 = this.toNodeComp(m+1, rightIndex, leftDir);
								if (network.contains(child_1) && network.contains(child_2)) {
									network.addNode(parent);
									network.addEdge(parent, new long[]{child_1,child_2});
								}
							}
						}
						
						if (complete == COMP.comp.ordinal() && direction == leftDir) {
							long parent = this.toNodeComp(leftIndex, rightIndex, leftDir);
							for(int m=leftIndex;m<rightIndex;m++){
								long child_1 = this.toNodeComp(leftIndex, m, leftDir);
								long child_2 = this.toNodeIncomp(m, rightIndex, leftDir);
								if(network.contains(child_1) && network.contains(child_2)){
									network.addNode(parent);
									network.addEdge(parent, new long[]{child_1,child_2});
								}
							}
						}
						
						if (complete == COMP.comp.ordinal() && direction == rightDir) {
							long parent = this.toNodeComp(leftIndex, rightIndex, rightDir);
							for (int m = leftIndex + 1; m <= rightIndex; m++) {
								long child_1 = this.toNodeIncomp(leftIndex, m, rightDir);
								long child_2 = this.toNodeComp(m, rightIndex, rightDir);
								if (network.contains(child_1) && network.contains(child_2)) {
									network.addNode(parent);
									network.addEdge(parent, new long[] { child_1, child_2 });
								}
							}
						}
					}
				}
			}
		}
		network.finalizeNetwork();
		
	}
	
	public SDNetwork compileUnLabledInstance(int networkId, SDInstance inst, LocalNetworkParam param){
		if (this._nodes == null) {
			this.compileUnlabeled();
		}
		long root = this.toNode_root(inst.getInput().length());
		int rootIdx = Arrays.binarySearch(this._nodes, root);
		SDNetwork network = new SDNetwork(networkId, inst, this._nodes, this._children, param, rootIdx + 1);
		return network;
	}
	
	public synchronized void compileUnlabeled(){
		if(this._nodes!=null){
			return;
		}
		SDNetwork network = new SDNetwork();
		long rootE = this.toNodeComp(0, 0, rightDir);
		network.addNode(rootE);
		for(int rightIndex = 1; rightIndex <= maxSentLen - 1; rightIndex++){
			//eIndex: 1,2,3,4,5,..n
			long wordLeftNode = this.toNodeComp(rightIndex, rightIndex, leftDir);
			long wordRightNode = this.toNodeComp(rightIndex, rightIndex, rightDir);
			network.addNode(wordLeftNode);
			network.addNode(wordRightNode);
			
			for(int L = 1;L <= rightIndex;L++){
				//L:(1),(1,2),(1,2,3),(1,2,3,4),(1,2,3,4,5),...(1..n)
				//bIndex:(0),(1,0),(2,1,0),(3,2,1,0),(4,3,2,1,0),...(n-1..0)
				//span: {(0,1)},{(1,2),(0,2)}
				int leftIndex = rightIndex - L;
				//span:[bIndex, rightIndex]
				for(int complete = 0; complete <= 1; complete++){
					for (int direction = 0; direction <= 1; direction++) {
						if (leftIndex == 0 && direction == 0) continue;
						if(leftIndex==0 && direction==0) continue;
						if(complete==0){
							long parent = this.toNodeIncomp(leftIndex, rightIndex, direction);
							for (int m = leftIndex; m < rightIndex; m++) {
								long child_1 = this.toNodeComp(leftIndex, m, rightDir);
								long child_2 = this.toNodeComp(m+1, rightIndex, leftDir);
								if (network.contains(child_1) && network.contains(child_2)) {
									network.addNode(parent);
									network.addEdge(parent, new long[]{child_1,child_2});
								}
							}
						}
						
						if (complete == COMP.comp.ordinal() && direction == leftDir) {
							long parent = this.toNodeComp(leftIndex, rightIndex, leftDir);
							for(int m=leftIndex;m<rightIndex;m++){
								long child_1 = this.toNodeComp(leftIndex, m, leftDir);
								long child_2 = this.toNodeIncomp(m, rightIndex, leftDir);
								if(network.contains(child_1) && network.contains(child_2)){
									network.addNode(parent);
									network.addEdge(parent, new long[]{child_1,child_2});
								}
							}
						}
						
						if (complete == COMP.comp.ordinal() && direction == rightDir) {
							long parent = this.toNodeComp(leftIndex, rightIndex, rightDir);
							for (int m = leftIndex + 1; m <= rightIndex; m++) {
								long child_1 = this.toNodeIncomp(leftIndex, m, rightDir);
								long child_2 = this.toNodeComp(m, rightIndex, rightDir);
								if (network.contains(child_1) && network.contains(child_2)) {
									network.addNode(parent);
									network.addEdge(parent, new long[] { child_1, child_2 });
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
		SDNetwork hpeNetwork = (SDNetwork)network;
		SDInstance inst = (SDInstance)(hpeNetwork.getInstance());
		inst = inst.duplicate();
		if(hpeNetwork.getMax()==Double.NEGATIVE_INFINITY) return inst;
		int[] prediction = this.toOutput(hpeNetwork, inst);
		inst.setPrediction(prediction);
		return inst;
	}
	
	
	private int[] toOutput(SDNetwork network, SDInstance inst) {
		int[] prediction = new int[inst.size()];
		prediction[0] = -1;  //no head for the leftmost root node
		long root = this.toNode_root(inst.size());
		int rootIdx = Arrays.binarySearch(network.getAllNodes(), root);
		findBest(network, inst, rootIdx, prediction);
		return prediction;
	}
	
	private void findBest(SDNetwork network, SDInstance inst, int parent_k, int[] prediction) {
		int[] children_k = network.getMaxPath( parent_k);
		for (int child_k: children_k) {
			long node = network.getNode(child_k);
			int[] nodeArr = NetworkIDMapper.toHybridNodeArray(node);
			int rightIndex = nodeArr[0];
			int leftIndex = nodeArr[0] - nodeArr[1];
			int comp = nodeArr[2];
			int direction = nodeArr[3];
			if (comp == COMP.incomp.ordinal()) {
				if (direction == leftDir) {
					prediction[leftIndex] = rightIndex;
				} else {
					prediction[rightIndex] = leftIndex;
				}
			}
			findBest(network, inst, child_k, prediction);
 		}
	}
	 


}

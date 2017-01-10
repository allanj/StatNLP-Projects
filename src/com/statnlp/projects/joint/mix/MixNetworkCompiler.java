package com.statnlp.projects.joint.mix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.projects.joint.mix.MixConfig.COMP;
import com.statnlp.projects.joint.mix.MixConfig.DIR;

public class MixNetworkCompiler extends NetworkCompiler {

	private static final long serialVersionUID = 2601737587426539990L;

	private int maxSentLen = 128; //including the root node at 0 idx
	public int maxSegmentLength = 8;
	private long[] _nodes;
	private int[][][] _children;
	private boolean DEBUG = true;
	
	public enum NodeType {
		ENTITY_DEP,
		ENTITY_NONDEP,
		ENTITY,
		DEP,
		ROOT,
	}
	
	private int rightDir = DIR.right.ordinal();
	private int leftDir = DIR.left.ordinal();
	
	
	static {
//		/***for dependency parsing: nodeType, rightIdx, rightIdx-leftIdx, complete, direction, entityType
//		for semi model: nodeType, position, SemiLabelId, 0, 0, 0
//		for entity_dep node: nodeType, rightIdx, rightIdx-leftIdx, complete, direction, entityType ****/
//		NetworkIDMapper.setCapacity(new int[]{10, 200, 200, 5, 5, 5});
		
		/***for dependency parsing: nodeType, rightIdx, rightIdx-leftIdx, complete, direction, entityType
		for semi model: position, max, max, max, SemiLabelId, nodeType
		for entity_dep node: rightIdx, rightIdx-leftIdx, complete, direction, entityType, nodeType
		for dep node: rightIndex, rightIdx-leftIdx, complete, direction, labelId, nodeType****/
		NetworkIDMapper.setCapacity(new int[]{200, 200, 5, 5, 10, 5});
	}
	
	public MixNetworkCompiler(int maxSize, int maxSegmentLength){
		this.maxSentLen = maxSize;
		this.maxSegmentLength = maxSegmentLength;
		this.compileUnlabeledInstancesGeneric();
	}

	@Override
	public Network compile(int networkId, Instance inst, LocalNetworkParam param) {
		if (inst.isLabeled()) {
			return this.compileLabeledNetwork(networkId, inst, param);
		} else {
			return this.compileUnlabeledNetwork(networkId, inst, param);
		}
	}
	
	/**
	 * Obtain a root node, the sentence length should consider the root(pos=0) as well.
	 * @param len: 
	 * @return
	 */
	private long toNode_JointRoot(int len) {
		return NetworkIDMapper.toHybridNodeID(new int[]{len, 0, 0, 0, MixLabel.Labels.size(), NodeType.ROOT.ordinal()});
	}
	
	private long toNode_entityLeaf(){
		return NetworkIDMapper.toHybridNodeID(new int[]{0, 0, 0, 0, MixLabel.get("O").id, NodeType.ENTITY.ordinal()});
	}
	
	private long toNode_entity(int pos, int labelId){
		return NetworkIDMapper.toHybridNodeID(new int[]{pos, 199, 4, 4, labelId, NodeType.ENTITY.ordinal()});
	}
	
	private long toNode_entityRoot(int size){
		return NetworkIDMapper.toHybridNodeID(new int[]{size, 0, 0, 0, MixLabel.Labels.size(), NodeType.ENTITY.ordinal()});
	}
	
	private long toNode_entity_non_dep_comp(int leftIndex, int rightIndex, int direction, int labelId) {
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex, rightIndex-leftIndex, COMP.comp.ordinal(), direction, labelId, NodeType.ENTITY_NONDEP.ordinal()});
	}
	
	private long toNode_entity_non_dep_incomp(int leftIndex, int rightIndex, int direction, int labelId) {
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex, rightIndex-leftIndex, COMP.incomp.ordinal(), direction, labelId, NodeType.ENTITY_NONDEP.ordinal()});
	}
	
	private long toNode_entity_dep_comp(int leftIndex, int rightIndex, int direction, int labelId) {
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex, rightIndex-leftIndex, COMP.comp.ordinal(), direction, labelId, NodeType.ENTITY_DEP.ordinal()});
	}
	
	private long toNode_entity_dep_incomp(int leftIndex, int rightIndex, int direction, int labelId) {
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex, rightIndex-leftIndex, COMP.incomp.ordinal(), direction, labelId, NodeType.ENTITY_DEP.ordinal()});
	}
	
	private long toNode_DepRoot(int sentLen){
		int endIndex = sentLen - 1;
		return NetworkIDMapper.toHybridNodeID(new int[]{endIndex, endIndex - 0, COMP.comp.ordinal(), DIR.right.ordinal(), 0, NodeType.DEP.ordinal()});
	}
	
	private long toNodeIncomp(int leftIndex, int rightIndex, int direction, int labelId){
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex, rightIndex-leftIndex, COMP.incomp.ordinal(), direction, labelId, NodeType.DEP.ordinal()});
	}
	
	private long toNodeComp(int leftIndex, int rightIndex, int direction){
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex, rightIndex-leftIndex, COMP.comp.ordinal(), direction, 0, NodeType.DEP.ordinal()});
	}
	
	private Network compileLabeledNetwork(int networkId, Instance inst, LocalNetworkParam param) {
		MixNetwork network = new MixNetwork(networkId, inst, param, this);
		MixInstance mixInst = (MixInstance)inst;
		long jointRoot = this.toNode_JointRoot(inst.size());
		network.addNode(jointRoot);
		int size = mixInst.size();
		
		List<MixSpan> outputSpans = mixInst.getOutput().entities;
		int[] heads = mixInst.getOutput().heads;
		Collections.sort(outputSpans);
		long entityLeaf = this.toNode_entityLeaf();
		network.addNode(entityLeaf);
		long prevNode = entityLeaf;
		this.buildSemiDepSubNetwork(network, size, heads, mixInst.toEntities(outputSpans));
		for(MixSpan span: outputSpans){
			if (span.start == span.end && span.start == 0) continue;
			int labelId = span.label.id;
			long end = toNode_entity(span.end, labelId);
			network.addNode(end);
			if (labelId != MixLabel.get("O").id && span.start != span.end) {
				long entityNonDepNode = this.toNode_entity_non_dep_comp(span.start - 1, span.end, DIR.right.ordinal(), labelId);
				network.addEdge(end, new long[]{prevNode, entityNonDepNode});
			} else {
				network.addEdge(end, new long[]{prevNode});
			}
			prevNode = end;
		}
		long entityRoot = this.toNode_entityRoot(size);
		network.addNode(entityRoot);
		network.addEdge(entityRoot, new long[]{prevNode});
		this.buildDepNetwork(network, size, heads);
		long depRoot = this.toNode_DepRoot(size);
		network.addEdge(jointRoot, new long[]{entityRoot, depRoot});
		network.finalizeNetwork();
		if (DEBUG) {
//			System.err.println(inst.getInput().toString());
			MixNetwork generic = (MixNetwork) compileUnlabeledNetwork(networkId, inst, param);
			if (!generic.contains(network)) {
				System.err.println("wrong");
			}
		}
		
		return network;
	}
	
	private Network compileUnlabeledNetwork(int networkId, Instance inst, LocalNetworkParam param) {
		int size = inst.size();
		long root = toNode_JointRoot(size);
		int root_k = Arrays.binarySearch(this._nodes, root);
		int numNodes = root_k + 1;
		return new MixNetwork(networkId, inst, this._nodes, this._children, param, numNodes, this);
	}

	private void compileUnlabeledInstancesGeneric() {
		if (this._nodes != null) return;
		MixNetwork network = new MixNetwork();
		this.buildSemiDepSubNetwork(network, maxSentLen, null, null);
		this.buildDepNetwork(network, maxSentLen, null);
		long entityLeaf = this.toNode_entityLeaf();
		network.addNode(entityLeaf);
		List<Long> currNodes = new ArrayList<Long>();
		for(int pos = 1; pos < maxSentLen; pos++){
			for(int labelId = 0; labelId < MixLabel.Labels.size(); labelId++){
				long node = this.toNode_entity(pos, labelId);
				if(labelId != MixLabel.Labels.get("O").id){
					//is entity
					for(int prevPos = pos - 1; prevPos >= pos - maxSegmentLength && prevPos >= 1; prevPos--){
						for(int prevLabelId = 0; prevLabelId< MixLabel.Labels.size(); prevLabelId++){
							long prevBeginNode = this.toNode_entity(prevPos, prevLabelId);
							if(network.contains(prevBeginNode)){
								network.addNode(node);
								if(pos - prevPos > 1) {
									long entityNonDepNode = this.toNode_entity_non_dep_comp(prevPos, pos, DIR.right.ordinal(), labelId);
									network.addEdge(node, new long[]{prevBeginNode, entityNonDepNode});
								} else {
									network.addEdge(node, new long[]{prevBeginNode});
								}
							}
						}
					}
					if(pos >= 1){
						network.addNode(node);
						if (pos > 1) {
							long entityNonDepNode = this.toNode_entity_non_dep_comp(0, pos, DIR.right.ordinal(), labelId);
							network.addEdge(node, new long[]{entityLeaf, entityNonDepNode});
						} else {
							network.addEdge(node, new long[]{entityLeaf});
						}
					}
				}else{
					//O label should be with only length 1. actually does not really affect.
					int prevPos = pos - 1;
					if(prevPos >= 1){
						for(int prevLabelId = 0; prevLabelId < MixLabel.Labels.size(); prevLabelId++){
							long prevBeginNode = this.toNode_entity(prevPos, prevLabelId);
							if(network.contains(prevBeginNode)){
								network.addNode(node);
//								System.err.println("parent: " + pos + ", " + "O; child: "+prevPos+","+MixLabel.get(prevLabelId));
//								System.err.println("parent: " + Arrays.toString(NetworkIDMapper.toHybridNodeArray(node)) + ", " + " child: "+Arrays.toString(NetworkIDMapper.toHybridNodeArray(prevBeginNode)));
								network.addEdge(node, new long[]{prevBeginNode});
							}
						}
					}
					if(pos == 1){
						network.addNode(node);
						network.addEdge(node, new long[]{entityLeaf});
					}
				}
				currNodes.add(node);
			}
			
			long entityRoot = this.toNode_entityRoot(pos + 1);
			network.addNode(entityRoot);
			for(long currNode: currNodes){
				if(network.contains(currNode)){
					network.addEdge(entityRoot, new long[]{currNode});
				}	
			}
			long jointRoot = this.toNode_JointRoot(pos + 1);
			long depRoot = this.toNode_DepRoot(pos + 1);
			network.addNode(jointRoot);
			network.addEdge(jointRoot, new long[]{entityRoot, depRoot});
			currNodes = new ArrayList<Long>();
		}
		network.finalizeNetwork();
		this._nodes = network.getAllNodes();
		this._children = network.getAllChildren();
		System.err.println(this._nodes.length + " nodes..");
	}

	/**
	 * Building the depenedency network from start to end.
	 * @param network
	 * @param start
	 * @param end: exclusive
	 * @param heads
	 */
	private void buildDepNetwork(MixNetwork network, int maxLength, int[] heads) {
		long rootRight = this.toNodeComp(0, 0, rightDir);
		network.addNode(rootRight);
		long depRoot = this.toNode_DepRoot(maxLength);
		network.addNode(depRoot);
		for(int rightIndex = 1; rightIndex <= maxLength - 1; rightIndex++){
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
						if (complete == 0) {
							if (heads != null) {
								if (direction == rightDir && heads[rightIndex] != leftIndex) continue;
								if (direction == leftDir && heads[leftIndex] != rightIndex) continue;
							}
							for (int lab = 0; lab < MixLabel.Labels.size(); lab++) {
								long parent = this.toNodeIncomp(leftIndex, rightIndex, direction, lab);
								for (int m = leftIndex; m < rightIndex; m++) {
									long child_1 = this.toNodeComp(leftIndex, m, rightDir);
									long child_2 = this.toNodeComp(m + 1, rightIndex, leftDir);
									if (network.contains(child_1) && network.contains(child_2)) {
										network.addNode(parent);
										network.addEdge(parent, new long[]{child_1,child_2});
									}
								}
							}
						}
						
						if (complete == COMP.comp.ordinal() && direction == leftDir) {
							long parent = this.toNodeComp(leftIndex, rightIndex, leftDir);
							for (int m = leftIndex; m < rightIndex; m++) {
								long child_1 = this.toNodeComp(leftIndex, m, leftDir);
								for (int lab = 0; lab < MixLabel.Labels.size(); lab++) {
									long child_2 = this.toNodeIncomp(m, rightIndex, leftDir, lab);
									if(network.contains(child_1) && network.contains(child_2)){
										network.addNode(parent);
										network.addEdge(parent, new long[]{child_1,child_2});
									}
								}
							}
						}
						
						if (complete == COMP.comp.ordinal() && direction == rightDir) {
							long parent = this.toNodeComp(leftIndex, rightIndex, rightDir);
							for (int m = leftIndex + 1; m <= rightIndex; m++) {
								long child_2 = this.toNodeComp(m, rightIndex, rightDir);
								for (int lab = 0; lab < MixLabel.Labels.size(); lab++) {
									long child_1 = this.toNodeIncomp(leftIndex, m, rightDir, lab);
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
		}
	}
	
	private void buildSemiDepSubNetwork(MixNetwork network, int maxLength, int[] heads, String[] entities) {
		int oLabelId = MixLabel.get("O").id;
		for(int rightIndex = 1; rightIndex <= maxLength - 1; rightIndex++){
			//eIndex: 1,2,3,4,5,..n
			for (int lab = 0; lab < MixLabel.Labels.size(); lab++) {
				
				if (lab == oLabelId || (entities != null && entities[rightIndex].equals("O"))) continue;
				
				long wordLeftNode = this.toNode_entity_dep_comp(rightIndex, rightIndex, leftDir, lab);
				long wordRightNode = this.toNode_entity_dep_comp(rightIndex, rightIndex, rightDir, lab);
				network.addNode(wordLeftNode);
				network.addNode(wordRightNode);
				long dummyRootRight = this.toNode_entity_non_dep_comp(rightIndex - 1 , rightIndex - 1, rightDir, lab);
				
				if (entities != null && entities[rightIndex].startsWith("B") && entities[rightIndex].substring(2).equals(MixLabel.get(lab).form)) {
					if (rightIndex == maxLength - 1) continue;
					if (!entities[rightIndex + 1].startsWith("I")) continue;
				}
				network.addNode(dummyRootRight);
			}
			
			for(int L = 1;L <= rightIndex;L++){
				//L:(1),(1,2),(1,2,3),(1,2,3,4),(1,2,3,4,5),...(1..n)
				//bIndex:(0),(1,0),(2,1,0),(3,2,1,0),(4,3,2,1,0),...(n-1..0)
				//span: {(0,1)},{(1,2),(0,2)}
				int leftIndex = rightIndex - L;
				//span:[bIndex, rightIndex]
				for(int complete = 0; complete <= 1; complete++){
					for (int direction = 0; direction <= 1; direction++) {
						if (leftIndex == 0 && direction == 0) continue;
						if (complete == 0) {
							
							for (int lab = 0; lab < MixLabel.Labels.size(); lab++) {
								if (lab == oLabelId) continue;
								if (direction == DIR.right.ordinal()) {
									long dummyParent = this.toNode_entity_non_dep_incomp(leftIndex, rightIndex, direction, lab);
									for (int m = leftIndex; m < rightIndex; m++) {
										long dummyChild_1 = this.toNode_entity_non_dep_comp(leftIndex, m, rightDir, lab);
										long child_2 = this.toNode_entity_dep_comp(m + 1, rightIndex, leftDir, lab);
										if (network.contains(dummyChild_1) && network.contains(child_2)) {
											network.addNode(dummyParent);
											network.addEdge(dummyParent, new long[]{dummyChild_1,child_2});
										}
									}
								}
							}
							if (heads != null) {
								if (direction == rightDir && heads[rightIndex] != leftIndex) continue;
								if (direction == leftDir && heads[leftIndex] != rightIndex) continue;
							}
							for (int lab = 0; lab < MixLabel.Labels.size(); lab++) {
								if (lab == oLabelId) continue;
								long parent = this.toNode_entity_dep_incomp(leftIndex, rightIndex, direction, lab);
								for (int m = leftIndex; m < rightIndex; m++) {
									long child_1 = this.toNode_entity_dep_comp(leftIndex, m, rightDir, lab);
									long child_2 = this.toNode_entity_dep_comp(m + 1, rightIndex, leftDir, lab);
									if (network.contains(child_1) && network.contains(child_2)) {
										network.addNode(parent);
										network.addEdge(parent, new long[]{child_1,child_2});
									}
								}
							}
						}
						
						if (complete == COMP.comp.ordinal() && direction == leftDir) {
							for (int lab = 0; lab < MixLabel.Labels.size(); lab++) {
								if (lab == oLabelId) continue;
								long parent = this.toNode_entity_dep_comp(leftIndex, rightIndex, leftDir, lab);
								for (int m = leftIndex; m < rightIndex; m++) {
									long child_1 = this.toNode_entity_dep_comp(leftIndex, m, leftDir, lab);
									long child_2 = this.toNode_entity_dep_incomp(m, rightIndex, leftDir, lab);
									if(network.contains(child_1) && network.contains(child_2)){
										network.addNode(parent);
										network.addEdge(parent, new long[]{child_1,child_2});
									}
									
								}
							}
						}
						
						if (complete == COMP.comp.ordinal() && direction == rightDir) {
							for (int lab = 0; lab < MixLabel.Labels.size(); lab++) {
								if (lab == oLabelId) continue;
								long parent = this.toNode_entity_dep_comp(leftIndex, rightIndex, rightDir, lab);
								for (int m = leftIndex + 1; m <= rightIndex; m++) {
									long child_1 = this.toNode_entity_dep_incomp(leftIndex, m, rightDir, lab);
									long child_2 = this.toNode_entity_dep_comp(m, rightIndex, rightDir, lab);
									if (network.contains(child_1) && network.contains(child_2)) {
										network.addNode(parent);
										network.addEdge(parent, new long[] { child_1, child_2 });
									}
								}
								long dummyParent = this.toNode_entity_non_dep_comp(leftIndex, rightIndex, rightDir, lab);
								for (int m = leftIndex + 1; m <= rightIndex; m++) {
									long dummyChild_1 = this.toNode_entity_non_dep_incomp(leftIndex, m, rightDir, lab);
									long child_2 = this.toNode_entity_dep_comp(m, rightIndex, rightDir, lab);
									if (network.contains(dummyChild_1) && network.contains(child_2)) {
										network.addNode(dummyParent);
										network.addEdge(dummyParent, new long[] { dummyChild_1, child_2 });
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	public Instance decompile(Network network) {
		//decompile the NE first
		MixNetwork mixNetwork = (MixNetwork)network;
		MixInstance result = (MixInstance)mixNetwork.getInstance();
		ArrayList<MixSpan> predSpans = new ArrayList<MixSpan>();
		long entityRoot = this.toNode_entityRoot(result.size());
		int node_k = Arrays.binarySearch(mixNetwork.getAllNodes(), entityRoot);
		int[] preHeadPrediction = new int[result.size()];
		Arrays.fill(preHeadPrediction, -1);
		while(node_k > 0){
			int[] children_k = mixNetwork.getMaxPath(node_k);
			int[] child_arr = mixNetwork.getNodeArray(children_k[0]);
			int pos = child_arr[0];
			int nodeType = child_arr[5];
			if(pos == 0){
				break;
			}
			int labelId = child_arr[4];
			int end = pos;
			if (nodeType != NodeType.ROOT.ordinal()) {
				if(end != 1){
					int[] children_k1 = mixNetwork.getMaxPath(children_k[0]);
					int[] child_arr1 = mixNetwork.getNodeArray(children_k1[0]);
					int start = child_arr1[0] + 1;
					predSpans.add(new MixSpan(start, end, MixLabel.Label_Index.get(labelId)));
				}else{
					predSpans.add(new MixSpan(end, end, MixLabel.Label_Index.get(labelId)));
				}
			}
			if (children_k.length == 2) {
				findBest(mixNetwork, result, children_k[1], preHeadPrediction);
			}
			node_k = children_k[0];
		}
		Collections.sort(predSpans);
		if (!result.hasPrediction())
			result.setPrediction(new MixPair(null, predSpans));
		else {
			result.getPrediction().entities = predSpans;
		}
		//set back the entity predictions and then do max again.
		String[] entityArr = result.toEntities(predSpans);
		for (int k = 0; k < mixNetwork.countNodes(); k++) {
			int[] arr = mixNetwork.getNodeArray(k);
			int nodeType = arr[5];
			int comp = arr[2];
			int direction = arr[3];
			int rightIndex = arr[0];
			int leftIndex = arr[0] - arr[1];
			int labelId = arr[4];
			if (nodeType == NodeType.DEP.ordinal() && comp == COMP.incomp.ordinal() && rightIndex < result.size()) {
				int modifierIndex = direction == leftDir ? leftIndex : rightIndex;
				if (preHeadPrediction[modifierIndex] != -1) {
					if (labelId != MixLabel.get(entityArr[modifierIndex].substring(2)).id) {
						mixNetwork.remove(k);
					}
				} else {
					if (labelId != MixLabel.get("O").id) {
						mixNetwork.remove(k);
					}
				}
			}
		}
		mixNetwork.max();
		long depRoot = this.toNode_DepRoot(result.size());
		int depRootIdx = Arrays.binarySearch(mixNetwork.getAllNodes(), depRoot);
		findBest(mixNetwork, result, depRootIdx, preHeadPrediction);
		result.setPrediction(new MixPair(preHeadPrediction, predSpans));
		return result;
	}
	
	
	
	
	
	
	private void findBest(MixNetwork network, MixInstance inst, int parent_k, int[] prediction) {
		int[] children_k = network.getMaxPath( parent_k);
		for (int child_k: children_k) {
			long node = network.getNode(child_k);
			int[] nodeArr = NetworkIDMapper.toHybridNodeArray(node);
			int rightIndex = nodeArr[0];
			int leftIndex = nodeArr[0] - nodeArr[1];
			int comp = nodeArr[2];
			int direction = nodeArr[3];
			int nodeType = nodeArr[5];
			if (nodeType == NodeType.ENTITY_DEP.ordinal() && comp == COMP.incomp.ordinal()) {
				if (direction == leftDir) {
					prediction[leftIndex] = rightIndex;
				} else {
					prediction[rightIndex] = leftIndex;
				}
			}
			if (nodeType == NodeType.DEP.ordinal() && comp == COMP.incomp.ordinal()) {
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

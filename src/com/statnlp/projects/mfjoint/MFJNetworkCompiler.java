package com.statnlp.projects.mfjoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.projects.mfjoint.MFJConfig.COMP;
import com.statnlp.projects.mfjoint.MFJConfig.DIR;
import com.statnlp.projects.mfjoint.MFJConfig.MFJTASK;
import com.statnlp.projects.mfjoint.MFJConfig.STRUCT;

public class MFJNetworkCompiler extends NetworkCompiler {

	private static final long serialVersionUID = -938028087951999377L;

	private int maxSentLen = 150; //including the root node at 0 idx
	public int maxSegmentLength = 8;
	private long[] _nodes;
	private int[][][] _children;
	private MFJTASK task;
	private boolean DEBUG = false;
	
	public enum NodeType {
		DEP,
		ENTITY,
		ROOT,
	}
	
	private int rightDir = DIR.right.ordinal();
	private int leftDir = DIR.left.ordinal();
	
	
	static {
		/***for dependency parsing: rightIdx, rightIdx-leftIdx, complete, direction, nodeType
		for semiCRF: position, SemiLabelId, 0, 0, nodeType ****/
		NetworkIDMapper.setCapacity(new int[]{200, 200, 5, 5, 10});
	}
	
	public MFJNetworkCompiler(MFJTASK task, int maxSize, int maxSegmentLength){
		this.task = task;
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
		return NetworkIDMapper.toHybridNodeID(new int[]{len, 0, 0, 0, NodeType.ROOT.ordinal()});
	}
	
	private long toNode_entityLeaf(){
		return NetworkIDMapper.toHybridNodeID(new int[]{0, MFJLabel.get("O").id, 0, 0, NodeType.ENTITY.ordinal()});
	}
	
	private long toNode_entity(int pos, int labelId){
		return NetworkIDMapper.toHybridNodeID(new int[]{pos, labelId, 0, 0, NodeType.ENTITY.ordinal()});
	}
	
	private long toNode_DepRoot(int sentLen){
		int endIndex = sentLen - 1;
		return NetworkIDMapper.toHybridNodeID(new int[]{endIndex, endIndex - 0, COMP.comp.ordinal(), DIR.right.ordinal(), NodeType.DEP.ordinal()});
	}
	
	private long toNodeIncomp(int leftIndex, int rightIndex, int direction){
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex, rightIndex-leftIndex, COMP.incomp.ordinal(), direction, NodeType.DEP.ordinal()});
	}
	
	private long toNodeComp(int leftIndex, int rightIndex, int direction){
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex, rightIndex-leftIndex, COMP.comp.ordinal(), direction, NodeType.DEP.ordinal()});
	}
	
	private Network compileLabeledNetwork(int networkId, Instance inst, LocalNetworkParam param) {
		MFJNetwork network = new MFJNetwork(networkId, inst, param, this);
		MFJInstance mfjInst = (MFJInstance)inst;
		long jointRoot = this.toNode_JointRoot(inst.size());
		network.addNode(jointRoot);
		int size = mfjInst.size();
		
		if (task == MFJTASK.NER || task == MFJTASK.JOINT) {
			List<MFJSpan> outputSpans = mfjInst.getOutput().entities;
			Collections.sort(outputSpans);
			long entityLeaf = this.toNode_entityLeaf();
			network.addNode(entityLeaf);
			long prevNode = entityLeaf;
			for(MFJSpan span: outputSpans){
				if (span.start == span.end && span.start == 0) continue;
				int labelId = span.label.id;
				long end = toNode_entity(span.end, labelId);
				network.addNode(end);
				network.addEdge(end, new long[]{prevNode});
				prevNode = end;
			}
			network.addEdge(jointRoot, new long[]{prevNode});
		}
		
		if (task == MFJTASK.PARING || task == MFJTASK.JOINT) {
			int[] heads = mfjInst.getOutput().heads;
			this.buildDepNetwork(network, size, heads);
		}
		
		network.finalizeNetwork();
		if (DEBUG) {
			MFJNetwork generic = (MFJNetwork) compileUnlabeledNetwork(networkId, inst, param);
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
		return new MFJNetwork(networkId, inst, this._nodes, this._children, param, numNodes, this);
	}

	private void compileUnlabeledInstancesGeneric() {
		if (this._nodes != null) return;
		MFJNetwork network = new MFJNetwork();
		if (task == MFJTASK.NER || task == MFJTASK.JOINT) {
			long entityLeaf = this.toNode_entityLeaf();
			network.addNode(entityLeaf);
			List<Long> currNodes = new ArrayList<Long>();
			for(int pos = 1; pos < maxSentLen; pos++){
				for(int labelId = 0; labelId < MFJLabel.Labels.size(); labelId++){
					long node = this.toNode_entity(pos, labelId);
					if(labelId != MFJLabel.Labels.get("O").id){
						for(int prevPos = pos - 1; prevPos >= pos - maxSegmentLength && prevPos >= 1; prevPos--){
							for(int prevLabelId = 0; prevLabelId< MFJLabel.Labels.size(); prevLabelId++){
								long prevBeginNode = this.toNode_entity(prevPos, prevLabelId);
								if(network.contains(prevBeginNode)){
									network.addNode(node);
									network.addEdge(node, new long[]{prevBeginNode});
								}
							}
						}
						if(pos >= 1){
							network.addNode(node);
							network.addEdge(node, new long[]{entityLeaf});
						}
					}else{
						//O label should be with only length 1. actually does not really affect.
						int prevPos = pos - 1;
						if(prevPos >= 1){
							for(int prevLabelId = 0; prevLabelId < MFJLabel.Labels.size(); prevLabelId++){
								long prevBeginNode = this.toNode_entity(prevPos, prevLabelId);
								if(network.contains(prevBeginNode)){
									network.addNode(node);
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
				long jointRoot = this.toNode_JointRoot(pos + 1);
				network.addNode(jointRoot);
				for(long currNode: currNodes){
					if(network.contains(currNode)){
						network.addEdge(jointRoot, new long[]{currNode});
					}	
				}
				currNodes = new ArrayList<Long>();
			}
		}
		
		
		if (task == MFJTASK.PARING || task == MFJTASK.JOINT) {
			this.buildDepNetwork(network, maxSentLen, null);
		}
		
		network.finalizeNetwork();
		this._nodes = network.getAllNodes();
		this._children = network.getAllChildren();
	}

	
	private void buildDepNetwork(MFJNetwork network, int maxLength, int[] heads) {
		long rootRight = this.toNodeComp(0, 0, rightDir);
		network.addNode(rootRight);
		long depRoot = this.toNode_DepRoot(maxLength);
		network.addNode(depRoot);
		long jointRoot = this.toNode_JointRoot(maxLength);
		if (heads != null) {
			network.addNode(jointRoot);
			network.addEdge(jointRoot, new long[]{depRoot});
		}
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
							long parent = this.toNodeIncomp(leftIndex, rightIndex, direction);
							if (heads != null) {
								if (direction == rightDir && heads[rightIndex] != leftIndex) continue;
								if (direction == leftDir && heads[leftIndex] != rightIndex) continue;
							}
							for (int m = leftIndex; m < rightIndex; m++) {
								long child_1 = this.toNodeComp(leftIndex, m, rightDir);
								long child_2 = this.toNodeComp(m + 1, rightIndex, leftDir);
								if (network.contains(child_1) && network.contains(child_2)) {
									network.addNode(parent);
									network.addEdge(parent, new long[]{child_1,child_2});
								}
							}
						}
						
						if (complete == COMP.comp.ordinal() && direction == leftDir) {
							long parent = this.toNodeComp(leftIndex, rightIndex, leftDir);
							for (int m = leftIndex; m < rightIndex; m++) {
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
							if (heads == null && leftIndex == 0 && network.contains(parent)) {
								jointRoot = this.toNode_JointRoot(rightIndex + 1);
								network.addNode(jointRoot);
								network.addEdge(jointRoot, new long[]{parent});
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	public Instance decompile(Network network) {
		if (task == MFJTASK.NER)
			return this.decompileNE(network);
		else if (task == MFJTASK.PARING)
			return this.decompileDep(network);
		else if (task == MFJTASK.JOINT) {
			int struct = network.getStructure();
			if (struct == STRUCT.SEMI.ordinal()){
				return this.decompileNE(network);
			} else {
				return this.decompileDep(network);
			}
		} else {
			throw new RuntimeException("unknown task:"+task+".");
		}
	}
	
	private MFJInstance decompileNE(Network network) {
		MFJNetwork mfjNetwork = (MFJNetwork)network;
		MFJInstance result = (MFJInstance)mfjNetwork.getInstance();
		ArrayList<MFJSpan> predSpans = new ArrayList<MFJSpan>();
		int node_k = network.countNodes() - 1;
		while(node_k > 0){
			int[] children_k = network.getMaxPath(node_k);
			int[] child_arr = network.getNodeArray(children_k[0]);
			int pos = child_arr[0];
			
			int nodeType = child_arr[4];
			if(pos == 0){
				break;
			} 
			int labelId = child_arr[1];
			//System.err.println(pos+","+Label.LABELS_INDEX.get(labelId).getForm()+" ," + nodeType.toString());
			int end = pos;
			if (nodeType != NodeType.ROOT.ordinal()) {
				if(end != 1){
					int[] children_k1 = network.getMaxPath(children_k[0]);
					int[] child_arr1 = network.getNodeArray(children_k1[0]);
					int start = child_arr1[0] + 1;
					predSpans.add(new MFJSpan(start, end, MFJLabel.Label_Index.get(labelId)));
				}else{
					predSpans.add(new MFJSpan(end, end, MFJLabel.Label_Index.get(labelId)));
				}
			}
			node_k = children_k[0];
			
		}
		Collections.sort(predSpans);
		if (!result.hasPrediction())
			result.setPrediction(new MFJPair(null, predSpans));
		else {
			result.getPrediction().entities = predSpans;
		}
		return result;
	}
	
	private MFJInstance decompileDep(Network network) {
		MFJNetwork mfjNetwork = (MFJNetwork)network;
		MFJInstance mfjInst = (MFJInstance)network.getInstance();
		int[] prediction = this.toOutput(mfjNetwork, mfjInst);
		if (mfjInst.hasPrediction()) {
			mfjInst.getPrediction().heads = prediction;
		} else {
			mfjInst.setPrediction(new MFJPair(prediction, null));
		}
		return mfjInst;
	}
	
	private int[] toOutput(MFJNetwork network, MFJInstance inst) {
		int[] prediction = new int[inst.size()];
		prediction[0] = -1;  //no head for the leftmost root node
		long root = this.toNode_JointRoot(inst.size());
		int rootIdx = Arrays.binarySearch(network.getAllNodes(), root);
		findBest(network, inst, rootIdx, prediction);
		return prediction;
	}
	
	private void findBest(MFJNetwork network, MFJInstance inst, int parent_k, int[] prediction) {
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

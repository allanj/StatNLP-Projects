package com.statnlp.projects.dep.model.hyperedge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkException;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.projects.dep.utils.DPConfig.COMP;
import com.statnlp.projects.dep.utils.DPConfig.DIR;


public class HPENetworkCompiler extends NetworkCompiler {

	private static final long serialVersionUID = -5080640847287255079L;

	private long[] _nodes;
	private final int maxSentLen = 57;
	private final int maxEntityLen = 7;
	private int[][][] _children;
	public enum NodeType {entity, phrase, incomp_dup, normal};
	
	private static boolean DEBUG = true;
	
	private int rightDir = DIR.right.ordinal();
	private int leftDir = DIR.left.ordinal();
	private String OEntity = "O";
	private final int maxEntityIdx = 9; //including O
	
	/**
	 * Compiler constructor
	 * @param typeMap: typeMap from DPConfig
	 */
	public HPENetworkCompiler() {
		// rightIndex, rightIndex-leftIndex, completeness, direction, leftSpanLen, rightSpanLen, labelType=0, nodeType
		int[] capacity = new  int[]{145, 145, 2, 2, 9, 9, maxEntityIdx + 1, 5};
		// for entity node: rightIndex, rightIndex-leftIndex, 0, 0, 0, 0, labelId, nodeType 
		NetworkIDMapper.setCapacity(capacity);
	}
	
	@Override
	public Network compile(int networkId, Instance inst, LocalNetworkParam param) {
		HPEInstance di = (HPEInstance)inst;
		if(di.isLabeled()){
			return this.compileLabledInstance(networkId, di, param);
		}else{
			return this.compileUnLabledInstance(networkId, di, param);
		}
	}

	public HPENetwork compileLabledInstance(int networkId, HPEInstance inst, LocalNetworkParam param){
		HPENetwork network = new HPENetwork(networkId,inst,param);
		Sentence sent = inst.getInput();
		List<Span> output = inst.getOutput();
		this.compileLabeled(network, sent, output);
		if(DEBUG){
			HPENetwork unlabeled = compileUnLabledInstance(networkId, inst, param);
			System.err.println("unlabel instance contain? " + unlabeled.contains(network));
			if(!unlabeled.contains(network)){
				System.err.println(sent.toString());
				throw new NetworkException("Labeled network is not contained in the unlabeled version");
			}
		}
		return network;
	}
	
	private void compileLabeled(HPENetwork network, Sentence sent, List<Span> output){

		Map<Span, Span> outputMap = new HashMap<>();
		for(Span span: output) 
			outputMap.put(span, span);
		Map<Span, Span> labelFreeOutputMap = new HashMap<>();
		for(Span span: output){
			Span span_dup = new Span(span.start, span.end, null, span.headSpan);
			if (span_dup.headSpan != null)
				 span_dup.headSpan.label = null;
			labelFreeOutputMap.put(span_dup, span_dup);
		}
		
		//adding the entity node first:
		for (int leftPos = 1; leftPos < sent.length(); leftPos++) {
			for (int rightPos = leftPos; rightPos < sent.length(); rightPos++) {
				long phraseNode = this.toNodePhrase(leftPos, rightPos);
				for (int l = 0; l < Label.Labels.size(); l++) {
					if (l == Label.get(OEntity).id && leftPos != rightPos) continue;
					Span span = new Span(leftPos, rightPos, Label.get(l));
					if (outputMap.containsKey(span)) {
						network.addNode(phraseNode);
						long entityNode = this.toNodeEntity(leftPos, rightPos, l);
						network.addNode(entityNode);
						network.addEdge(phraseNode, new long[]{entityNode});
						break;
					}
				}
			}
		}
		
		long rootE = this.toNodeComp(0, 0, rightDir, 1);
		network.addNode(rootE);
		for(int rightIndex = 1; rightIndex <= sent.length()-1; rightIndex++){
			//eIndex: 1,2,3,4,5,..n
			for (int spanLen = 1; spanLen <= maxEntityLen && (rightIndex - spanLen + 1) > 0; spanLen++) {
				Span span = new Span(rightIndex - spanLen + 1, rightIndex, null);
				if (labelFreeOutputMap.containsKey(span)) {
					long wordRightNodeE = this.toNodeComp(rightIndex - spanLen + 1, rightIndex, rightDir, spanLen); 
					long wordLeftNodeE = this.toNodeComp(rightIndex - spanLen + 1, rightIndex, leftDir, spanLen);
					network.addNode(wordRightNodeE);
					network.addNode(wordLeftNodeE);
				}
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
						if (complete == COMP.incomp.ordinal()) {
							for (int lr = leftIndex; lr < rightIndex && (lr - leftIndex + 1) <= maxEntityLen; lr++) {
								int leftSpanLen = lr - leftIndex + 1;
								for (int rl = rightIndex; rl > lr && (rightIndex - rl + 1) <= maxEntityLen; rl--) {
									int rightSpanLen = rightIndex - rl + 1;
									long parent = this.toNodeIncomp(leftIndex, rightIndex, direction, leftSpanLen, rightSpanLen);
									Span leftSpan = new Span(leftIndex, lr);
									Span rightSpan = new Span(rl, rightIndex);
									boolean inOutput = false;
									if (direction == leftDir) {
										if (labelFreeOutputMap.containsKey(leftSpan) && labelFreeOutputMap.get(leftSpan).headSpan.equals(rightSpan)) {
											inOutput = true;
										}
									} else {
										if (labelFreeOutputMap.containsKey(rightSpan) && 
												labelFreeOutputMap.get(rightSpan).headSpan.
												equals(leftSpan) ) {
											inOutput = true;
										}
									}
									if (inOutput) {
										long parent_dup = this.toNodeIncompSub(parent);
										long phraseNode = direction == leftDir ?
												this.toNodePhrase(leftIndex, lr) : this.toNodePhrase(rl, rightIndex); 
										if (network.contains(phraseNode)) {
											for (int m = lr; m < rl; m++) {
												long leftChild = this.toNodeComp(leftIndex, m, rightDir, leftSpanLen);
												long rightChild = this.toNodeComp(m + 1, rightIndex, leftDir, rightSpanLen);
												if(network.contains(leftChild) && network.contains(rightChild)){
													network.addNode(parent_dup);
													network.addEdge(parent_dup, new long[]{leftChild, rightChild});
												}
											}
											if (network.contains(parent_dup)) {
												network.addNode(parent);
												network.addEdge(parent, new long[]{parent_dup, phraseNode});
											}
										}
									}
								}
							}
						}
						
						if(complete == COMP.comp.ordinal() && direction == leftDir){
							for (int rl = rightIndex; rl > leftIndex && (rightIndex - rl + 1) <= maxEntityLen; rl--) {
								int rightSpanLen = rightIndex - rl + 1;
								long parent = this.toNodeComp(leftIndex, rightIndex, leftDir, rightSpanLen);
								for (int ml = leftIndex; ml < rl; ml++) {
									for (int mr = ml; mr < rl && (mr - ml + 1) <= maxEntityLen; mr++) {
										int middleSpanLen = mr - ml + 1;
										long leftChild = this.toNodeComp(leftIndex, mr, leftDir, middleSpanLen);
										long rightChild = this.toNodeIncomp(ml, rightIndex, leftDir, middleSpanLen, rightSpanLen);
										if(network.contains(leftChild) && network.contains(rightChild)){
											network.addNode(parent);
											network.addEdge(parent, new long[]{leftChild, rightChild});
										}
									}
								}
							}
						}
						
						if(complete == COMP.comp.ordinal() && direction == rightDir){
							for (int lr = leftIndex; lr < rightIndex && (lr - leftIndex + 1) <= maxEntityLen; lr++) {
								int leftSpanLen = lr - leftIndex + 1;
								if (leftIndex == 0 && leftSpanLen != 1) continue;
								long parent = this.toNodeComp(leftIndex, rightIndex, rightDir, leftSpanLen);
								for (int ml = lr + 1; ml <= rightIndex; ml++) {
									for (int mr = ml; mr <= rightIndex && (mr - ml + 1) <= maxEntityLen; mr++) {
										int middleSpanLen = mr - ml + 1;
										long leftChild = this.toNodeIncomp(leftIndex, mr, rightDir, leftSpanLen, middleSpanLen);
										long rightChild = this.toNodeComp(ml, rightIndex, rightDir, middleSpanLen);
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
			}
		}
		
		
		
		network.finalizeNetwork();
		
	}
	
	public HPENetwork compileUnLabledInstance(int networkId, HPEInstance inst, LocalNetworkParam param){
		if (this._nodes == null) {
			this.compileUnlabeled();
		}
		long root = this.toNode_root(inst.getInput().length());
		int rootIdx = Arrays.binarySearch(this._nodes, root);
		HPENetwork network = new HPENetwork(networkId, inst, this._nodes, this._children, param, rootIdx + 1);
		return network;
	}
	
	public synchronized void compileUnlabeled(){
		if(this._nodes!=null){
			return;
		}
		HPENetwork network = new HPENetwork();
		for (int leftPos = 1; leftPos < this.maxSentLen; leftPos++) {
			for (int rightPos = leftPos; rightPos < this.maxSentLen; rightPos++) {
				long phraseNode = this.toNodePhrase(leftPos, rightPos);
				for (int l = 0; l < Label.Labels.size(); l++) {
					if (l == Label.get(OEntity).id && leftPos != rightPos) continue;
					network.addNode(phraseNode);
					long entityNode = this.toNodeEntity(leftPos, rightPos, l);
					network.addNode(entityNode);
					network.addEdge(phraseNode, new long[]{entityNode});
				}
			}
		}
		
		//add the root word and other nodes
		//all are complete nodes.
		long rootE = this.toNodeComp(0, 0, rightDir, 1);
		network.addNode(rootE);
		
		for(int rightIndex = 1; rightIndex <= this.maxSentLen-1; rightIndex++){
			//eIndex: 1,2,3,4,5,..n
			for (int spanLen = 1; spanLen <= maxEntityLen && (rightIndex - spanLen + 1) > 0; spanLen++) {
				long wordRightNodeE = this.toNodeComp(rightIndex - spanLen + 1, rightIndex, rightDir, spanLen); 
				long wordLeftNodeE = this.toNodeComp(rightIndex - spanLen + 1, rightIndex, leftDir, spanLen);
				network.addNode(wordRightNodeE);
				network.addNode(wordLeftNodeE);
			}
			
			for(int L = 1; L <= rightIndex;L++){
				//L:(1),(1,2),(1,2,3),(1,2,3,4),(1,2,3,4,5),...(1..n)
				//bIndex:(0),(1,0),(2,1,0),(3,2,1,0),(4,3,2,1,0),...(n-1..0)
				//span: {(0,1)},{(1,2),(0,2)}
				int leftIndex = rightIndex - L;
				//span:[bIndex, rightIndex]
				
				for(int complete = 0; complete <= 1; complete++){
					for(int direction=0;direction<=1;direction++){
						if (leftIndex == 0 && direction == 0) continue;
						if (complete == COMP.incomp.ordinal()) {
							for (int lr = leftIndex; lr < rightIndex && (lr - leftIndex + 1) <= maxEntityLen; lr++) {
								int leftSpanLen = lr - leftIndex + 1;
								for (int rl = rightIndex; rl > lr && (rightIndex - rl + 1) <= maxEntityLen; rl--) {
									int rightSpanLen = rightIndex - rl + 1;
									if (leftSpanLen > 4 && rightSpanLen > 4) continue;
									if (direction == DIR.right.ordinal() && rightSpanLen > 2 && leftSpanLen > 4) continue;
									if (direction == DIR.right.ordinal() && rightSpanLen > 1 && leftSpanLen > 6) continue;
									if (direction == DIR.right.ordinal() && rightSpanLen > 0 && leftSpanLen > 8) continue;
									if (direction == DIR.left.ordinal() && leftSpanLen > 2 && rightSpanLen > 4) continue;
									if (direction == DIR.left.ordinal() && leftSpanLen > 1 && rightSpanLen > 6) continue;
									if (direction == DIR.left.ordinal() && leftSpanLen > 0 && rightSpanLen > 8) continue;
									long parent = this.toNodeIncomp(leftIndex, rightIndex, direction, leftSpanLen, rightSpanLen);
									long parent_dup = this.toNodeIncompSub(parent);
									long phraseNode = direction == leftDir ?
											this.toNodePhrase(leftIndex, lr) : this.toNodePhrase(rl, rightIndex); 
									if (network.contains(phraseNode)) {
										for (int m = lr; m < rl; m++) {
											long leftChild = this.toNodeComp(leftIndex, m, rightDir, leftSpanLen);
											long rightChild = this.toNodeComp(m + 1, rightIndex, leftDir, rightSpanLen);
											if(network.contains(leftChild) && network.contains(rightChild)){
												network.addNode(parent_dup);
												network.addEdge(parent_dup, new long[]{leftChild, rightChild});
											}
										}
										if (network.contains(parent_dup)) {
											network.addNode(parent);
											network.addEdge(parent, new long[]{parent_dup, phraseNode});
										}
									}
								}
							}
						}
						
						if(complete == COMP.comp.ordinal() && direction == leftDir){
							for (int rl = rightIndex; rl > leftIndex && (rightIndex - rl + 1) <= maxEntityLen; rl--) {
								int rightSpanLen = rightIndex - rl + 1;
								long parent = this.toNodeComp(leftIndex, rightIndex, leftDir, rightSpanLen);
								for (int ml = leftIndex; ml < rl; ml++) {
									for (int mr = ml; mr < rl && (mr - ml + 1) <= maxEntityLen; mr++) {
										int middleSpanLen = mr - ml + 1;
										long leftChild = this.toNodeComp(leftIndex, mr, leftDir, middleSpanLen);
										long rightChild = this.toNodeIncomp(ml, rightIndex, leftDir, middleSpanLen, rightSpanLen);
										if(network.contains(leftChild) && network.contains(rightChild)){
											network.addNode(parent);
											network.addEdge(parent, new long[]{leftChild, rightChild});
										}
									}
								}
							}
						}
						
						if(complete == COMP.comp.ordinal() && direction == rightDir){
							
							for (int lr = leftIndex; lr < rightIndex && (lr - leftIndex + 1) <= maxEntityLen; lr++) {
								int leftSpanLen = lr - leftIndex + 1;
								if (leftIndex == 0 && leftSpanLen != 1) continue;
								long parent = this.toNodeComp(leftIndex, rightIndex, rightDir, leftSpanLen);
								for (int ml = lr + 1; ml <= rightIndex; ml++) {
									for (int mr = ml; mr <= rightIndex && (mr - ml + 1) <= maxEntityLen; mr++) {
										int middleSpanLen = mr - ml + 1;
										long leftChild = this.toNodeIncomp(leftIndex, mr, rightDir, leftSpanLen, middleSpanLen);
										long rightChild = this.toNodeComp(ml, rightIndex, rightDir, middleSpanLen);
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
		HPENetwork hpeNetwork = (HPENetwork)network;
		HPEInstance inst = (HPEInstance)(hpeNetwork.getInstance());
		inst = inst.duplicate();
		if(hpeNetwork.getMax()==Double.NEGATIVE_INFINITY) return inst;
		List<Span> prediction = this.toOutput(hpeNetwork, inst);
		inst.setPrediction(prediction);
		return inst;
	}
	
	
	private List<Span> toOutput(HPENetwork network, HPEInstance inst) {
		Map<Span, Span> predictionMap = new HashMap<>();
		long root = this.toNode_root(inst.size());
		int rootIdx = Arrays.binarySearch(network.getAllNodes(), root);
		findBest(network, inst, rootIdx, predictionMap);
		List<Span> prediction = new ArrayList<>();
		for (Span span : predictionMap.keySet()) {
			Span valSpan = predictionMap.get(span);
			prediction.add(valSpan);
		}
		Collections.sort(prediction);
		return prediction;
	}
	
	private void findBest(HPENetwork network, HPEInstance inst, int parent_k, Map<Span, Span> predictionMap) {
		int[] children_k = network.getMaxPath(parent_k);
		for (int child_k: children_k) {
			long node = network.getNode(child_k);
			int[] nodeArr = NetworkIDMapper.toHybridNodeArray(node);
			int rightIndex = nodeArr[0];
			int leftIndex = nodeArr[0] - nodeArr[1];
			int comp = nodeArr[2];
			int direction = nodeArr[3];
			int leftSpanLen = nodeArr[4];
			int rightSpanLen = nodeArr[5];
			int labelId = nodeArr[6];
			int nodeType = nodeArr[7];
			if (comp == COMP.incomp.ordinal() && nodeType == NodeType.normal.ordinal()) {
				Span span;
				if (direction == leftDir) {
					span = new Span(leftIndex, leftIndex + leftSpanLen - 1, null, new Span(rightIndex - rightSpanLen + 1, rightIndex));
				} else {
					span = new Span(rightIndex - rightSpanLen + 1, rightIndex, null, new Span(leftIndex, leftIndex + leftSpanLen - 1, null));
				}
				predictionMap.put(span, span);
			}
			if (nodeType == NodeType.entity.ordinal()) {
				Span span = new Span(leftIndex, rightIndex);
				Span valSpan = predictionMap.get(span);
				valSpan.label = Label.get(labelId);
			}
			findBest(network, inst, child_k, predictionMap);
 		}
	}
	
	
	//Node composition
	//rightIndex, (rightIndex-leftIndex), complete (0--incomplete,1), direction(0--left,1--right), leftSpanLen, leftType, rightSpanLen, rightType, node Type
	//sentLen include index 0. 
	public long toNode_root(int sentLen){
		int endIndex = sentLen -1;
		return NetworkIDMapper.toHybridNodeID(new int[]{endIndex, endIndex - 0, COMP.comp.ordinal(), DIR.right.ordinal(), 1, 0, maxEntityIdx, NodeType.normal.ordinal()});
	}
	
	public long toNodeIncomp(int leftIndex, int rightIndex, int direction, int leftSpanLen, int rightSpanlen){
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex, rightIndex-leftIndex, COMP.incomp.ordinal(), direction, leftSpanLen, rightSpanlen, maxEntityIdx, NodeType.normal.ordinal()});
	}
	
	public long toNodeComp(int leftIndex, int rightIndex, int direction, int typeSpanLen){
		int leftSpanLen = direction == DIR.right.ordinal()? typeSpanLen:0;
		int rightSpanlen = direction == DIR.right.ordinal()? 0:typeSpanLen;
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex, rightIndex-leftIndex, COMP.comp.ordinal(), direction, leftSpanLen, rightSpanlen, maxEntityIdx, NodeType.normal.ordinal()});
	}
	
	public long toNodeIncompSub(long incompNode) {
		int[] incompArr = NetworkIDMapper.toHybridNodeArray(incompNode);
		incompArr[incompArr.length - 1] = NodeType.incomp_dup.ordinal();
		return NetworkIDMapper.toHybridNodeID(incompArr);
	}
	
	public long toNodePhrase(int phraseLeft, int phraseRight) {
		return NetworkIDMapper.toHybridNodeID(new int[]{phraseRight, phraseRight - phraseLeft, 0, 0, 0, 0, maxEntityIdx, NodeType.phrase.ordinal()});
	}
	
	public long toNodeEntity(int phraseLeft, int phraseRight, String entity) {
		return NetworkIDMapper.toHybridNodeID(new int[]{phraseRight, phraseRight - phraseLeft, 0, 0, 0, 0, Label.get(entity).id, NodeType.entity.ordinal()});
	}
	
	public long toNodeEntity(int phraseLeft, int phraseRight, int entityId) {
		return NetworkIDMapper.toHybridNodeID(new int[]{phraseRight, phraseRight - phraseLeft, 0, 0, 0, 0, entityId, NodeType.entity.ordinal()});
	}


}

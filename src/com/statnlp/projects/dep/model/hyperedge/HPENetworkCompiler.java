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
import com.statnlp.projects.dep.utils.DPConfig;
import com.statnlp.projects.dep.utils.DPConfig.COMP;
import com.statnlp.projects.dep.utils.DPConfig.DIR;


public class HPENetworkCompiler extends NetworkCompiler {

	private static final long serialVersionUID = -5080640847287255079L;

	private long[] _nodes;
	private final int maxSentLen = 50;
	private final int maxEntityLen = 4;
	private int[][][] _children;
	private enum NodeType {normal};
	public static String EMPTY = DPConfig.EMPTY;
	
	private static boolean DEBUG = true;
	
	private int rightDir = DIR.right.ordinal();
	private int leftDir = DIR.left.ordinal();
	private String OEntity = "O";
	
	/**
	 * Compiler constructor
	 * @param typeMap: typeMap from DPConfig
	 */
	public HPENetworkCompiler() {
		// rightIndex, rightIndex-leftIndex, completeness, direction, entity type, node type
		int[] capacity = new  int[]{145, 145, 2, 2, 9, Label.Labels.size(), 9, Label.Labels.size(), 1};
		NetworkIDMapper.setCapacity(capacity);
	}
	
	//Node composition
	//rightIndex, (rightIndex-leftIndex), complete (0--incomplete,1), direction(0--left,1--right), leftSpanLen, leftType, rightSpanLen, rightType, node Type
	//sentLen include index 0. 
	public long toNode_root(int sentLen){
		int endIndex = sentLen -1;
		return NetworkIDMapper.toHybridNodeID(new int[]{endIndex, endIndex - 0, COMP.comp.ordinal(), DIR.right.ordinal(), 1, Label.get("O").id, 0, Label.get(EMPTY).id, NodeType.normal.ordinal()});
	}
	
	
	public long toNodeIncomp(int leftIndex, int rightIndex, int direction, String leftType, int leftSpanLen, String rightType, int rightSpanlen){
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex, rightIndex-leftIndex, COMP.incomp.ordinal(), direction, leftSpanLen, Label.get(leftType).id, rightSpanlen, Label.get(rightType).id, NodeType.normal.ordinal()});
	}
	
	public long toNodeComp(int leftIndex, int rightIndex, int direction, String type, int typeSpanLen){
		int leftSpanLen = direction == DIR.right.ordinal()? typeSpanLen:0;
		int rightSpanlen = direction == DIR.right.ordinal()? 0:typeSpanLen;
		String leftType = direction == DIR.right.ordinal()? type:EMPTY;
		String rightType = direction == DIR.right.ordinal()? EMPTY:type;
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex, rightIndex-leftIndex, COMP.comp.ordinal(), direction, leftSpanLen, Label.get(leftType).id, rightSpanlen, Label.get(rightType).id, NodeType.normal.ordinal()});
	}
	
	@Override
	public Network compile(int networkId, Instance inst, LocalNetworkParam param) {
		HPEInstance di = (HPEInstance)inst;
		if(di.isLabeled()){
			//System.err.println("[Info] Compiling Labeled Network...");
			return this.compileLabledInstance(networkId, di, param);
		}else{
			//System.err.println("[Info] Compiling Unlabeled Network...");
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
			if(!unlabeled.contains(network)){
				System.err.println(sent.toString());
				throw new NetworkException("Labeled network is not contained in the unlabeled version");
			}
		}
		return network;
	}
	
	private void compileLabeled(HPENetwork network, Sentence sent, List<Span> output){
		long rootE = this.toNodeComp(0, 0, rightDir, OEntity, 1);
		network.addNode(rootE);
		Map<Span, Span> outputMap = new HashMap<>();
		for(Span span: output) 
			outputMap.put(span, span);
		for(int rightIndex = 1; rightIndex <= sent.length()-1; rightIndex++){
			//eIndex: 1,2,3,4,5,..n
			for(String e: Label.Labels.keySet()){
				if(e.equals(EMPTY)) continue;
				for (int spanLen = 1; spanLen <= maxEntityLen && (rightIndex - spanLen + 1) > 0; spanLen++) {
					if (spanLen != 1 && e.equals(OEntity)) continue;
					Span span = new Span(rightIndex - spanLen + 1, rightIndex, Label.get(e));
					if (outputMap.containsKey(span)) {
						long wordRightNodeE = this.toNodeComp(rightIndex - spanLen + 1, rightIndex, rightDir, e, spanLen); 
						long wordLeftNodeE = this.toNodeComp(rightIndex - spanLen + 1, rightIndex, leftDir, e, spanLen);
						network.addNode(wordRightNodeE);
						network.addNode(wordLeftNodeE);
					}
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
									for (int lt = 0; lt < Label.Labels.size(); lt++) {
										if (lt == Label.get(EMPTY).id || (leftSpanLen != 1 && lt == Label.get(OEntity).id)) continue;
										for (int rt = 0; rt < Label.Labels.size(); rt++) {
											if (rt == Label.get(EMPTY).id || (rightSpanLen != 1 && rt == Label.get(OEntity).id)) continue;
											long parent = this.toNodeIncomp(leftIndex, rightIndex, direction, Label.get(lt).form, leftSpanLen, Label.get(rt).form, rightSpanLen);
											Span leftSpan = new Span(leftIndex, lr, Label.get(lt));
											Span rightSpan = new Span(rl, rightIndex, Label.get(rt));
											boolean inOutput = false;
											if (direction == leftDir) {
												if (outputMap.containsKey(leftSpan)) {
													int leftSpanHead = outputMap.get(leftSpan).headIndex;
													if (leftSpanHead >= rl || leftSpanHead <= rightIndex) inOutput = true;
												}
											} else {
												if (outputMap.containsKey(rightSpan)) {
													int rightSpanHead = outputMap.get(rightSpan).headIndex;
													if (rightSpanHead >= leftIndex || rightSpanHead <= lr) inOutput = true;
												}
											}
											if (inOutput) {
												for (int m = lr; m < rl; m++) {
													long leftChild = this.toNodeComp(leftIndex, m, rightDir, Label.get(lt).form, leftSpanLen);
//													if (leftIndex==0 && m==0) {
//														System.err.println("at root one.");
//													}
													long rightChild = this.toNodeComp(m + 1, rightIndex, leftDir, Label.get(rt).form, rightSpanLen);
													
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
						
						if(complete == COMP.comp.ordinal() && direction == leftDir){
							for (int rl = rightIndex; rl > leftIndex && (rightIndex - rl + 1) <= maxEntityLen; rl--) {
								int rightSpanLen = rightIndex - rl + 1;
								for (int rt = 0; rt < Label.Labels.size(); rt++) {
									if (rt == Label.get(EMPTY).id || (rightSpanLen != 1 && rt == Label.get(OEntity).id)) continue;
									long parent = this.toNodeComp(leftIndex, rightIndex, leftDir, Label.get(rt).form, rightSpanLen);
									for (int ml = leftIndex; ml < rl; ml++) {
										for (int mr = ml; mr < rl && (mr - ml + 1) <= maxEntityLen; mr++) {
											int middleSpanLen = mr - ml + 1;
											for (int mt = 0; mt < Label.Labels.size(); mt++) {
												if (mt == Label.get(EMPTY).id || (middleSpanLen != 1 && mt == Label.get(OEntity).id)) continue;
												long leftChild = this.toNodeComp(leftIndex, mr, leftDir, Label.get(mt).form, middleSpanLen);
												long rightChild = this.toNodeIncomp(ml, rightIndex, leftDir, Label.get(mt).form, middleSpanLen, Label.get(rt).form, rightSpanLen);
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
						
						if(complete == COMP.comp.ordinal() && direction == rightDir){
							
							for (int lr = leftIndex; lr < rightIndex && (lr - leftIndex + 1) <= maxEntityLen; lr++) {
								int leftSpanLen = lr - leftIndex + 1;
								if (leftIndex == 0 && leftSpanLen != 1) continue;
								for (int lt = 0; lt < Label.Labels.size(); lt++) {
									if (leftIndex == 0 && lt != Label.get(OEntity).id) continue;
									if (lt == Label.get(EMPTY).id || (leftSpanLen != 1 && lt == Label.get(OEntity).id)) continue;
									long parent = this.toNodeComp(leftIndex, rightIndex, rightDir, Label.get(lt).form, leftSpanLen);
									for (int ml = lr + 1; ml <= rightIndex; ml++) {
										for (int mr = ml; mr <= rightIndex && (mr - ml + 1) <= maxEntityLen; mr++) {
											int middleSpanLen = mr - ml + 1;
											for (int mt = 0; mt < Label.Labels.size(); mt++) {
												long leftChild = this.toNodeIncomp(leftIndex, mr, rightDir, Label.get(lt).form, leftSpanLen, Label.get(mt).form, middleSpanLen);
												long rightChild = this.toNodeComp(ml, rightIndex, rightDir, Label.get(mt).form, middleSpanLen);
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
		//add the root word and other nodes
		//all are complete nodes.
		long rootE = this.toNodeComp(0, 0, rightDir, OEntity, 1);
		network.addNode(rootE);
		
		for(int rightIndex = 1; rightIndex <= this.maxSentLen-1; rightIndex++){
			//eIndex: 1,2,3,4,5,..n
			for(String e: Label.Labels.keySet()){
				if(e.equals(EMPTY)) continue;
				for (int spanLen = 1; spanLen <= maxEntityLen && (rightIndex - spanLen + 1) > 0; spanLen++) {
					if (spanLen != 1 && e.equals(OEntity)) continue;
					long wordRightNodeE = this.toNodeComp(rightIndex - spanLen + 1, rightIndex, rightDir, e, spanLen); 
					long wordLeftNodeE = this.toNodeComp(rightIndex - spanLen + 1, rightIndex, leftDir, e, spanLen);
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
					for(int direction=0;direction<=1;direction++){
						if (leftIndex == 0 && direction == 0) continue;
						if (complete == COMP.incomp.ordinal()) {
							for (int lr = leftIndex; lr < rightIndex && (lr - leftIndex + 1) <= maxEntityLen; lr++) {
								int leftSpanLen = lr - leftIndex + 1;
								for (int rl = rightIndex; rl > lr && (rightIndex - rl + 1) <= maxEntityLen; rl--) {
									int rightSpanLen = rightIndex - rl + 1;
									for (int lt = 0; lt < Label.Labels.size(); lt++) {
										if (lt == Label.get(EMPTY).id || (leftSpanLen != 1 && lt == Label.get(OEntity).id)) continue;
										for (int rt = 0; rt < Label.Labels.size(); rt++) {
											if (rt == Label.get(EMPTY).id || (rightSpanLen != 1 && rt == Label.get(OEntity).id)) continue;
											long parent = this.toNodeIncomp(leftIndex, rightIndex, direction, Label.get(lt).form, leftSpanLen, Label.get(rt).form, rightSpanLen);
											for (int m = lr; m < rl; m++) {
												long leftChild = this.toNodeComp(leftIndex, m, rightDir, Label.get(lt).form, leftSpanLen);
												long rightChild = this.toNodeComp(m + 1, rightIndex, leftDir, Label.get(rt).form, rightSpanLen);
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
						
						if(complete == COMP.comp.ordinal() && direction == leftDir){
							for (int rl = rightIndex; rl > leftIndex && (rightIndex - rl + 1) <= maxEntityLen; rl--) {
								int rightSpanLen = rightIndex - rl + 1;
								for (int rt = 0; rt < Label.Labels.size(); rt++) {
									if (rt == Label.get(EMPTY).id || (rightSpanLen != 1 && rt == Label.get(OEntity).id)) continue;
									long parent = this.toNodeComp(leftIndex, rightIndex, leftDir, Label.get(rt).form, rightSpanLen);
									for (int ml = leftIndex; ml < rl; ml++) {
										for (int mr = ml; mr < rl && (mr - ml + 1) <= maxEntityLen; mr++) {
											int middleSpanLen = mr - ml + 1;
											for (int mt = 0; mt < Label.Labels.size(); mt++) {
												if (mt == Label.get(EMPTY).id || (middleSpanLen != 1 && mt == Label.get(OEntity).id)) continue;
												long leftChild = this.toNodeComp(leftIndex, mr, leftDir, Label.get(mt).form, middleSpanLen);
												long rightChild = this.toNodeIncomp(ml, rightIndex, leftDir, Label.get(mt).form, middleSpanLen, Label.get(rt).form, rightSpanLen);
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
						
						if(complete == COMP.comp.ordinal() && direction == rightDir){
							
							for (int lr = leftIndex; lr < rightIndex && (lr - leftIndex + 1) <= maxEntityLen; lr++) {
								int leftSpanLen = lr - leftIndex + 1;
								if (leftIndex == 0 && leftSpanLen != 1) continue;
								for (int lt = 0; lt < Label.Labels.size(); lt++) {
									if (leftIndex == 0 && lt != Label.get(OEntity).id) continue;
									if (lt == Label.get(EMPTY).id || (leftSpanLen != 1 && lt == Label.get(OEntity).id)) continue;
									long parent = this.toNodeComp(leftIndex, rightIndex, rightDir, Label.get(lt).form, leftSpanLen);
									for (int ml = lr + 1; ml <= rightIndex; ml++) {
										for (int mr = ml; mr <= rightIndex && (mr - ml + 1) <= maxEntityLen; mr++) {
											int middleSpanLen = mr - ml + 1;
											for (int mt = 0; mt < Label.Labels.size(); mt++) {
												long leftChild = this.toNodeIncomp(leftIndex, mr, rightDir, Label.get(lt).form, leftSpanLen, Label.get(mt).form, middleSpanLen);
												long rightChild = this.toNodeComp(ml, rightIndex, rightDir, Label.get(mt).form, middleSpanLen);
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
		List<Span> prediction = new ArrayList<>();
		long root = this.toNode_root(inst.size());
		int rootIdx = Arrays.binarySearch(network.getAllNodes(), root);
		findBest(network, inst, rootIdx, prediction);
		Collections.sort(prediction);
		return prediction;
	}
	
	private void findBest(HPENetwork network, HPEInstance inst, int parent_k, List<Span> prediction) {
		int[] children_k = network.getMaxPath(parent_k);
		for (int child_k: children_k) {
			long node = network.getNode(child_k);
			int[] nodeArr = NetworkIDMapper.toHybridNodeArray(node);
			int rightIndex = nodeArr[0];
			int leftIndex = nodeArr[0] - nodeArr[1];
			int comp = nodeArr[2];
			int direction = nodeArr[3];
			int leftSpanLen = nodeArr[4];
			int ltId = nodeArr[5];
			int rightSpanLen = nodeArr[6];
			int rtId = nodeArr[7];
			if (comp == COMP.incomp.ordinal()) {
				Span span;
				int headIdx;
				if (direction == leftDir) {
					//the rule is assigned to nearest member first.
					headIdx = rightIndex - rightSpanLen + 1;
					span = new Span(leftIndex, leftIndex + leftSpanLen - 1, Label.get(ltId), headIdx);
				} else {
					headIdx = leftIndex + leftSpanLen - 1;
					span = new Span(rightIndex - rightSpanLen + 1, rightIndex, Label.get(rtId));
				}
				prediction.add(span);
			}
			findBest(network, inst, child_k, prediction);
 		}
	}
	 


}

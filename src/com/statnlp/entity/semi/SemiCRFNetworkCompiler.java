package com.statnlp.entity.semi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkException;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class SemiCRFNetworkCompiler extends NetworkCompiler {
	
	private final static boolean DEBUG = true;
	
	private static final long serialVersionUID = 6585870230920484539L;
	public int maxSize = 128;
	public int maxSegmentLength = 8;
	public long[] allNodes;
	public int[][][] allChildren;
	private SemiViewer sViewer;
	
	public enum NodeType {
		LEAF,
		NORMAL,
		ROOT,
	}
	
	static {
		NetworkIDMapper.setCapacity(new int[]{10000, 10, 100});
	}

	public SemiCRFNetworkCompiler(int maxSize, int maxSegmentLength,SemiViewer sViewer) {
		this.maxSize = Math.max(maxSize, this.maxSize);
//		this.maxSize = 3;
		this.maxSegmentLength = Math.max(maxSegmentLength, this.maxSegmentLength);
//		this.maxSegmentLength = 2;
		System.out.println(String.format("Max size: %s, Max segment length: %s", maxSize, maxSegmentLength));
		System.out.println(Label.LABELS.toString());
		this.sViewer = sViewer;
		buildUnlabeled();
	}

	@Override
	public SemiCRFNetwork compile(int networkId, Instance inst, LocalNetworkParam param) {
		try{
			if(inst.isLabeled()){
				return compileLabeled(networkId, (SemiCRFInstance)inst, param);
			} else {
				return compileUnlabeled(networkId, (SemiCRFInstance)inst, param);
			}
		} catch (NetworkException e){
			System.out.println(inst);
			throw e;
		}
	}
	
	private SemiCRFNetwork compileLabeled(int networkId, SemiCRFInstance instance, LocalNetworkParam param){
		SemiCRFNetwork network = new SemiCRFNetwork(networkId, instance, param);
		
		int size = instance.size();
		List<Span> output = instance.getOutput();
		Collections.sort(output);
		
		long leaf = toNode_leaf();
		network.addNode(leaf);
		long prevNode = leaf;
		for(Span span: output){
			int labelId = span.label.id;
			long thisEnd =-1;
//			if(span.end!=span.start){
//				long end = toNode(span.end, labelId);
//				network.addNode(end);
//				network.addEdge(end, new long[]{prevNode});
//				thisEnd = end;
//			}else{
//				long begin = toNode(span.start, labelId);
//				network.addNode(begin);
//				network.addEdge(begin, new long[]{prevNode});
//				thisEnd = begin;
//			}
			long end = toNode(span.end, labelId);
			network.addNode(end);
			network.addEdge(end, new long[]{prevNode});
			thisEnd = end;
			
			
			prevNode = thisEnd;
		}
		long root = toNode_root(size-1);
		network.addNode(root);
		network.addEdge(root, new long[]{prevNode});
		
		network.finalizeNetwork();
		//sViewer.visualizeNetwork(network, null, "Labeled Network");
		if(DEBUG){
//			System.out.println(network);
//			System.err.println(instance.getInput().toString());
			SemiCRFNetwork unlabeled = compileUnlabeled(networkId, instance, param);
			//System.out.println("Contained: "+unlabeled.contains(network));
		}
		return network;
	}
	
	private SemiCRFNetwork compileUnlabeled(int networkId, SemiCRFInstance instance, LocalNetworkParam param){
		int size = instance.size();
		long root = toNode_root(size-1);
		int root_k = Arrays.binarySearch(allNodes, root);
		int numNodes = root_k + 1;
		return new SemiCRFNetwork(networkId, instance, allNodes, allChildren, param, numNodes);
	}
	
	private synchronized void buildUnlabeled(){
		SemiCRFNetwork network = new SemiCRFNetwork();
		
		long leaf = toNode_leaf();
		network.addNode(leaf);
		List<Long> prevNodes = new ArrayList<Long>();
		List<Long> currNodes = new ArrayList<Long>();
		prevNodes.add(leaf);
		for(int pos=0; pos<maxSize; pos++){
			for(int labelId=0; labelId<Label.LABELS.size(); labelId++){
				long node = toNode(pos, labelId);
				
				network.addNode(node);
				currNodes.add(node);
//				if(pos==0 && Label.LABELS_INDEX.get(labelId).getForm().equals("O")){
//					throw new RuntimeException("Test");
//				}
				for(int prevPos=pos-2; prevPos >= pos-maxSegmentLength && prevPos >= 0; prevPos--){
					for(int prevLabelId=0; prevLabelId<Label.LABELS.size(); prevLabelId++){
						long prevBeginNode = toNode(prevPos, prevLabelId);
						network.addEdge(node, new long[]{prevBeginNode});
					}
				}
				if(pos>0){
					network.addEdge(node, new long[]{toNode_leaf()});
				}
				
				for(long prevNode: prevNodes){
					network.addEdge(node, new long[]{prevNode});
				}
				
			}
			long root = toNode_root(pos);
			network.addNode(root);
			for(long currNode: currNodes){
				network.addEdge(root, new long[]{currNode});	
			}
			prevNodes = currNodes;
			currNodes = new ArrayList<Long>();
		}
		network.finalizeNetwork();
		//sViewer.visualizeNetwork(network, null, "UnLabeled Network");
		allNodes = network.getAllNodes();
		allChildren = network.getAllChildren();
	}
	
	private long toNode_leaf(){
		return toNode(0, Label.get("O").id, NodeType.LEAF);
	}
	
	private long toNode(int pos, int labelId){
		return toNode(pos, labelId, NodeType.NORMAL);
	}
	
	private long toNode_root(int pos){
		return toNode(pos, Label.LABELS.size(), NodeType.ROOT);
	}
	
	private long toNode(int pos, int labelId, NodeType type){
		return NetworkIDMapper.toHybridNodeID(new int[]{pos, type.ordinal(), labelId});
	}

	@Override
	public SemiCRFInstance decompile(Network net) {
		SemiCRFNetwork network = (SemiCRFNetwork)net;
		SemiCRFInstance result = (SemiCRFInstance)network.getInstance().duplicate();
		List<Span> prediction = new ArrayList<Span>();
		int node_k = network.countNodes()-1;
		while(node_k > 0){
			int[] children_k = network.getMaxPath(node_k);
			int[] child_arr = network.getNodeArray(children_k[0]);
			int pos = child_arr[0];
			
			NodeType nodeType = NodeType.values()[child_arr[1]];
			if(nodeType == NodeType.LEAF){
				break;
			} 
			int labelId = child_arr[2];
			//System.err.println(pos+","+Label.LABELS_INDEX.get(labelId).getForm()+" ," + nodeType.toString());
			int end = pos;
			if(end!=0){
				int[] children_k1 = network.getMaxPath(children_k[0]);
				int[] child_arr1 = network.getNodeArray(children_k1[0]);
				int start = child_arr1[0] + 1;
				if(child_arr1[1]==NodeType.LEAF.ordinal())
					start = child_arr1[0];
				prediction.add(new Span(start, end, Label.LABELS_INDEX.get(labelId)));
			}else{
				
				prediction.add(new Span(end, end, Label.LABELS_INDEX.get(labelId)));
			}
			node_k = children_k[0];
			
		}
		Collections.sort(prediction);
		result.setPrediction(prediction);
		return result;
	}

}

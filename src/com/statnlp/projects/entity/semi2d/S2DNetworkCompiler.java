package com.statnlp.projects.entity.semi2d;

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
import com.statnlp.projects.entity.semi.Label;
import com.statnlp.projects.entity.semi.SemiCRFInstance;
import com.statnlp.projects.entity.semi.SemiCRFNetwork;
import com.statnlp.projects.entity.semi.Span;

public class S2DNetworkCompiler extends NetworkCompiler {
	
	private final static boolean DEBUG = false;
	
	private static final long serialVersionUID = 6585870230920484539L;
	public int maxSize = 128;
	public int maxSegmentLength = 8;
	public long[] allNodes;
	public int[][][] allChildren;
	private S2DViewer sViewer;
	
	public enum NodeType {
		LEAF,
		NORMAL,
		ROOT,
	}
	
	static {
		NetworkIDMapper.setCapacity(new int[]{10000, 10,3, 100});
	}

	public S2DNetworkCompiler(int maxSize, int maxSegmentLength,S2DViewer sViewer) {
		this.maxSize = Math.max(maxSize, this.maxSize);
//		this.maxSize = 3;
		this.maxSegmentLength = Math.max(maxSegmentLength, this.maxSegmentLength);
//		this.maxSegmentLength = 2;
		System.out.println(String.format("Max size: %s, Max segment length: %s", maxSize, maxSegmentLength));
		System.out.println(Label.LABELS.toString());
		this.sViewer = sViewer;
		this.sViewer.nothing();
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
		SemiCRFNetwork network = new SemiCRFNetwork(networkId, instance, param, this);
		
		int size = instance.size();
		List<Span> output = instance.getOutput();
		Collections.sort(output);
		
		long leaf = toNode_leaf();
		network.addNode(leaf);
		long prevNode = leaf;
		for(Span span: output){
			int labelId = span.label.id;
			long left = toNode_left(span.end, labelId);
			long right = toNode_right(span.end, Label.get("O").id);
			if(span.start!=span.end){
				long prevLeft = toNode_left(span.start, Label.get("O").id);
				network.addNode(prevLeft);
				network.addEdge(prevLeft, new long[]{prevNode});
				network.addNode(left);
				network.addEdge(left, new long[]{prevLeft});
			}else{
				left = toNode_left(span.end, Label.get("O").id);
				right = toNode_right(span.end, labelId);
				network.addNode(left);
				network.addEdge(left, new long[]{prevNode});
			}
			network.addNode(right);
			network.addEdge(right, new long[]{left});
			
			long thisEnd = right;
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
		return new SemiCRFNetwork(networkId, instance, allNodes, allChildren, param, numNodes, this);
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
				long left = toNode_left(pos, labelId);
				network.addNode(left);
				
				for(long prevRightNode: prevNodes){
					//this one now is fully connected first, adjacent position
					network.addEdge(left, new long[]{prevRightNode});
				}
				for(int prevPos=pos-1; prevPos > pos-maxSegmentLength && prevPos >= 0; prevPos--){
					for(int prevLabelId=0; prevLabelId<Label.LABELS.size(); prevLabelId++){
						long prevBeginNode = toNode_left(prevPos, prevLabelId);
						network.addEdge(left, new long[]{prevBeginNode});
					}
				}
				currNodes.add(left);
				
			}
			prevNodes = currNodes;
			currNodes = new ArrayList<Long>();
			for(int labelId=0; labelId<Label.LABELS.size(); labelId++){
				long right = toNode_right(pos, labelId);
				network.addNode(right);
				for(long leftNode: prevNodes){
					network.addEdge(right, new long[]{leftNode});
				}
				currNodes.add(right);
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
		return toNode(0, Label.get("O").id, 2,NodeType.LEAF);
	}
	
	private long toNode_left(int pos, int labelId){
		return toNode(pos, labelId,0, NodeType.NORMAL);
	}
	
	private long toNode_right(int pos, int labelId){
		return toNode(pos, labelId,1, NodeType.NORMAL);
	}
	
	private long toNode_root(int pos){
		return toNode(pos, Label.LABELS.size(),2, NodeType.ROOT);
	}
	
	private long toNode(int pos, int labelId,int direction, NodeType type){
		return NetworkIDMapper.toHybridNodeID(new int[]{pos, type.ordinal(), direction, labelId});
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
			int labelId = child_arr[3];
			int direction = child_arr[2];
			if(direction==1){
				int[] children_k1 = network.getMaxPath(children_k[0]);
				int[] child_arr1 = network.getNodeArray(children_k1[0]);
				int childLabelId = child_arr1[3];
				if(Label.LABELS_INDEX.get(childLabelId).getForm().equals("O"))
					prediction.add(new Span(pos, pos, Label.LABELS_INDEX.get(labelId)));
			}else{
				int[] children_k1 = network.getMaxPath(children_k[0]);
				int[] child_arr1 = network.getNodeArray(children_k1[0]);
				int childPos = child_arr1[0];
				prediction.add(new Span(childPos, pos, Label.LABELS_INDEX.get(labelId)));
			}
			
			//System.err.println(pos+","+Label.LABELS_INDEX.get(labelId).getForm()+" ," + nodeType.toString());
			node_k = children_k[0];
			
		}
		Collections.sort(prediction);
		result.setPrediction(prediction);
		return result;
	}

}

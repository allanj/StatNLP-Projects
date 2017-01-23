package com.statnlp.projects.nndcrf.exactFCRF;

import java.util.ArrayList;
import java.util.Arrays;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class ExactNetworkCompiler extends NetworkCompiler{

	private static final long serialVersionUID = -2388666010977956073L;

	public enum NODE_TYPES {LEAF, 
		NODE,  
		ROOT};
	private int _size;
	private ExactNetwork genericUnlabeledNetwork;
	private boolean IOBESencoding; //useless if using tagging task only
	
	public ExactNetworkCompiler(boolean IOBESencoding){
		this(IOBESencoding, 150);
	}
	
	public ExactNetworkCompiler(boolean IOBESencoding, int maxSize){
		this._size = maxSize;
		this.IOBESencoding = IOBESencoding;
		NetworkIDMapper.setCapacity(new int[]{1000, 500, 10});
		this.compileUnlabeledInstancesGeneric();
	}
	
	@Override
	public ExactNetwork compile(int networkId, Instance inst, LocalNetworkParam param) {
		ExactInstance lcrfInstance = (ExactInstance)inst;
		if(lcrfInstance.isLabeled())
			return compileLabeledInstances(networkId, lcrfInstance, param);
		else
			return compileUnlabeledInstances(networkId, lcrfInstance, param);
	}
	
	public long toNode_leaf(){
		int[] arr = new int[]{0, ExactLabel.Labels.size(), NODE_TYPES.LEAF.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	/**
	 * to a node in tagging: no label for root node
	 * @param pos: the argument 0 indexed.
	 * @param tag_id
	 * @return
	 */
	public long toNode(int pos, int label_id){
		int[] arr = new int[]{pos + 1, label_id, NODE_TYPES.NODE.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	/**
	 * to a root node: no label for root node
	 * @param size: size of the sentence.
	 * @return
	 */
	public long toNode_root(int size){
		int[] arr = new int[]{size + 1, ExactLabel.Labels.size() ,NODE_TYPES.ROOT.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	

	/**
	 * Compile the labeled network. it could be independent task or joint task depending on task variable.
	 * @param networkId
	 * @param inst
	 * @param param
	 * @return
	 */
	public ExactNetwork compileLabeledInstances(int networkId, ExactInstance inst, LocalNetworkParam param) {
		ExactNetwork lcrfNetwork = new ExactNetwork(networkId, inst, param);
		long leaf = toNode_leaf();
		lcrfNetwork.addNode(leaf);
		long root = toNode_root(inst.size());
		lcrfNetwork.addNode(root);

		long[] children = new long[] { leaf };
		for (int i = 0; i < inst.size(); i++) {
			int labelId = ExactLabel.get(inst.getOutput().get(i)).getId();
			long node = toNode(i, labelId);
			lcrfNetwork.addNode(node);
			long[] currentNodes = new long[] { node };
			lcrfNetwork.addEdge(node, children);
			children = currentNodes;
		}
		lcrfNetwork.addEdge(root, children);
		lcrfNetwork.finalizeNetwork();

		if (!genericUnlabeledNetwork.contains(lcrfNetwork)) {
			System.err.println("wrong");
		}
		return lcrfNetwork;
	}
	

	public ExactNetwork compileUnlabeledInstances(int networkId, ExactInstance inst, LocalNetworkParam param) {

		long[] allNodes = genericUnlabeledNetwork.getAllNodes();
		long root = toNode_root(inst.size());
		int rootIdx = Arrays.binarySearch(allNodes, root);
		ExactNetwork lcrfNetwork = new ExactNetwork(networkId, inst, allNodes, genericUnlabeledNetwork.getAllChildren(),
				param, rootIdx + 1);
		return lcrfNetwork;
	}
	
	public void compileUnlabeledInstancesGeneric(){
		ExactNetwork lcrfNetwork = new ExactNetwork();
		long leaf = toNode_leaf();
		lcrfNetwork.addNode(leaf);
		long[] children = new long[]{leaf};
		for(int i = 0; i < _size; i++){
			long root = toNode_root(i+1); // the size should be i+1
			lcrfNetwork.addNode(root);
			long[] currentNodes = new long[ExactLabel.Labels.size()];
			for(int l = 0 ; l < ExactLabel.Labels.size(); l++){
				long node = toNode(i, l);
				String exactLabel = ExactLabel.Labels_Index.get(l).getForm();
				String[] vals = exactLabel.split(ExactConfig.EXACT_SEP);
				String currChunk = vals[0];
				for(long child: children){
					if(child==-1) continue;
					int[] childArr = NetworkIDMapper.toHybridNodeArray(child);
					String childLabel  = i==0? "O"+ExactConfig.EXACT_SEP+"STR":ExactLabel.Labels_Index.get(childArr[1]).getForm();
					String[] childVals = childLabel.split(ExactConfig.EXACT_SEP);
					String childChunk = childVals[0]; //i=0; child is the leaf
					if(childChunk.startsWith("I")){
						if(currChunk.startsWith("I") && !childChunk.substring(1).equals(currChunk.substring(1))) continue;
						if(currChunk.startsWith("E") && !childChunk.substring(1).equals(currChunk.substring(1))) continue;
						if(IOBESencoding && (currChunk.startsWith("O") || currChunk.startsWith("B") || currChunk.startsWith("S")  ) ) continue;
					}else if(childChunk.equals("O")){
						if(currChunk.startsWith("I") || currChunk.startsWith("E")) continue;
						
					}else if(childChunk.startsWith("B")){
						if(currChunk.startsWith("I")  && !childChunk.substring(1).equals(currChunk.substring(1)) ) continue;
						if(currChunk.startsWith("E")  && !childChunk.substring(1).equals(currChunk.substring(1)) ) continue;
						if(IOBESencoding && ( currChunk.startsWith("O") || currChunk.startsWith("B") || currChunk.startsWith("S") )  ) continue;
						
					}else if(childChunk.startsWith("E")){
						if(currChunk.startsWith("I") || currChunk.startsWith("E")) continue;
						
					}else if(childChunk.startsWith("S")){
						if(currChunk.startsWith("I") || currChunk.startsWith("E")) continue;
					}else{
						throw new RuntimeException("Unknown type "+childChunk+" in network compilation");
					}
					if(lcrfNetwork.contains(child)){
						lcrfNetwork.addNode(node);
						lcrfNetwork.addEdge(node, new long[]{child});
					}
				}
				if(lcrfNetwork.contains(node))
					currentNodes[l] = node;
				else currentNodes[l] = -1;
			}
			for(long child:currentNodes){
				if(child==-1) continue;
				int[] childArr = NetworkIDMapper.toHybridNodeArray(child);
				String childLabel  = i==0? "O"+ExactConfig.EXACT_SEP+"STR":ExactLabel.Labels_Index.get(childArr[1]).getForm();
				String[] childVals = childLabel.split(ExactConfig.EXACT_SEP);
				String childChunk = childVals[0]; //i=0; child is the leaf
				if (IOBESencoding && childChunk.startsWith("B")) continue;
				if (IOBESencoding && childChunk.startsWith("I")) continue;
				lcrfNetwork.addEdge(root, new long[]{child});
			}
			children = currentNodes;
			
		}
		lcrfNetwork.finalizeNetwork();
		genericUnlabeledNetwork =  lcrfNetwork;
		System.err.println(genericUnlabeledNetwork.getAllNodes().length+" nodes..");
	}

	@Override
	public ExactInstance decompile(Network network) {
		ExactNetwork lcrfNetwork = (ExactNetwork)network;
		ExactInstance lcrfInstance = (ExactInstance)lcrfNetwork.getInstance();
		ExactInstance result = lcrfInstance.duplicate();
		ArrayList<String> nePred = new ArrayList<String>();
		long root = toNode_root(lcrfInstance.size());
		int rootIdx = Arrays.binarySearch(lcrfNetwork.getAllNodes(),root);
		for(int i = 0; i < lcrfInstance.size(); i++){
			int child_k = lcrfNetwork.getMaxPath(rootIdx)[0];
			long child = lcrfNetwork.getNode(child_k);
			rootIdx = child_k;
			int[] childArr = NetworkIDMapper.toHybridNodeArray(child);
			int labelId = childArr[1];
			String label = ExactLabel.Labels_Index.get(labelId).getForm();
			//assume it's the IOBES encoding.
			String[] vals = label.split(ExactConfig.EXACT_SEP);
			String resChunk = vals[0];
			if(resChunk.startsWith("S")) resChunk = "B"+resChunk.substring(1);
			if(resChunk.startsWith("E")) resChunk = "I"+resChunk.substring(1);
			nePred.add(0, resChunk + ExactConfig.EXACT_SEP + vals[1]);
		}
		result.setPrediction(nePred);
		return result;
	}
	
}

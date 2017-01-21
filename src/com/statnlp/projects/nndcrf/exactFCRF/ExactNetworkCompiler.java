package com.statnlp.projects.nndcrf.exactFCRF;

import java.util.ArrayList;
import java.util.Arrays;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.projects.nndcrf.factorialCRFs.FCRFConfig.TASK;

public class ExactNetworkCompiler extends NetworkCompiler{

	private static final long serialVersionUID = -2388666010977956073L;

	public enum NODE_TYPES {LEAF, 
		ENODE,   //node type in NE-chain 
		TNODE,   //node type in PoS-chain
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
		NetworkIDMapper.setCapacity(new int[]{1000, 10, 500});
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
		int[] arr = new int[]{0, NODE_TYPES.LEAF.ordinal(), ChunkLabel.CHUNKS.size()+TagLabel.TAGS.size()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	/**
	 * to a node in tagging: no label for root node
	 * @param pos: the argument 0 indexed.
	 * @param tag_id
	 * @return
	 */
	public long toNode_t(int pos, int tag_id){
		int[] arr = new int[]{pos+1, NODE_TYPES.TNODE.ordinal(), tag_id};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	public long toNode_e(int pos, int chunk_id){
		int[] arr = new int[]{pos+1, NODE_TYPES.ENODE.ordinal(), chunk_id};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	/**
	 * to a root node: no label for root node
	 * @param size: size of the sentence.
	 * @return
	 */
	public long toNode_root(int size){
		int[] arr = new int[]{size+1,NODE_TYPES.ROOT.ordinal(),ChunkLabel.CHUNKS.size()+TagLabel.TAGS.size()};
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

		if (task == TASK.CHUNKING || task == TASK.JOINT) {
			// NE chain structure
			long[] e_children = new long[] { leaf };
			for (int i = 0; i < inst.size(); i++) {
				// output is actually the chunk.
				int chunkId = ChunkLabel.CHUNKS.get(inst.getOutput().get(i)).getId();
				long e_node = toNode_e(i, chunkId);
				lcrfNetwork.addNode(e_node);
				long[] current_e_Nodes = new long[] { e_node };
				lcrfNetwork.addEdge(e_node, e_children);
				e_children = current_e_Nodes;
			}
			lcrfNetwork.addEdge(root, e_children);
		}

		if (task == TASK.TAGGING || task == TASK.JOINT) {
			// tagging chain structure
			long[] t_children = new long[] { leaf };
			for (int i = 0; i < inst.size(); i++) {
				int posId = TagLabel.TAGS.get(inst.getInput().get(i).getTag()).getId();
				long tag_node = toNode_t(i, posId);
				lcrfNetwork.addNode(tag_node);
				long[] current_t_Nodes = new long[] { tag_node };
				lcrfNetwork.addEdge(tag_node, t_children);
				t_children = current_t_Nodes;
			}
			lcrfNetwork.addEdge(root, t_children);
		}
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
		long[] tag_children = new long[]{leaf};
		for(int i = 0; i < _size; i++){
			long root = toNode_root(i+1); // the size should be i+1
			lcrfNetwork.addNode(root);
			if(task==TASK.CHUNKING || task==TASK.JOINT){
				long[] currentNodes = new long[ChunkLabel.CHUNKS.size()];
				for(int e=0;e<ChunkLabel.CHUNKS.size();e++){
					long enode = toNode_e(i, e);
					String currChunk = ChunkLabel.CHUNKS_INDEX.get(e).getForm();
					for(long child: children){
						if(child==-1) continue;
						int[] childArr = NetworkIDMapper.toHybridNodeArray(child);
						String childChunk = i==0? "O":ChunkLabel.CHUNKS_INDEX.get(childArr[2]).getForm(); //i=0; child is the leaf
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
							lcrfNetwork.addNode(enode);
							lcrfNetwork.addEdge(enode, new long[]{child});
						}
					}
					if(lcrfNetwork.contains(enode))
						currentNodes[e] = enode;
					else currentNodes[e] = -1;
				}
				for(long child:currentNodes){
					if(child==-1) continue;
					int[] childArr = NetworkIDMapper.toHybridNodeArray(child);
					String childChunk = i==0? "O":ChunkLabel.CHUNKS_INDEX.get(childArr[2]).getForm(); //i=0; child is the leaf
					if (IOBESencoding && childChunk.startsWith("B")) continue;
					if (IOBESencoding && childChunk.startsWith("I")) continue;
					lcrfNetwork.addEdge(root, new long[]{child});
				}
				children = currentNodes;
			}
			
			
			if(task==TASK.TAGGING || task==TASK.JOINT){
				long[] tag_currentNodes = new long[TagLabel.TAGS.size()];
				for(int t=0;t<TagLabel.TAGS.size();t++){
					long tnode = toNode_t(i, t);
					for(long child: tag_children){
						if(child==-1) continue;
						if(lcrfNetwork.contains(child)){
							lcrfNetwork.addNode(tnode);
							lcrfNetwork.addEdge(tnode, new long[]{child});
						}
					}
					if(lcrfNetwork.contains(tnode))
						tag_currentNodes[t] = tnode;
					else tag_currentNodes[t] = -1;
				}
				for(long child:tag_currentNodes){
					if(child==-1) continue;
					lcrfNetwork.addEdge(root, new long[]{child});
				}
				tag_children = tag_currentNodes;
			}
			
			
		}
		lcrfNetwork.finalizeNetwork();
		genericUnlabeledNetwork =  lcrfNetwork;
		System.err.println(genericUnlabeledNetwork.getAllNodes().length+" nodes..");
	}

	@Override
	public ExactInstance decompile(Network network) {
		if(task==TASK.CHUNKING)
			return decompileNE(network);
		else if(task==TASK.TAGGING)
			return decompilePOS(network);
		else if(task==TASK.JOINT){
			//remember to disable some nodes first
			int structure = network.getStructure();
			if(structure==0){
				return decompileNE(network);
			}else{
				return decompilePOS(network);
			}
		}else
			throw new RuntimeException("unknown task:"+task+".");
	}
	
	public ExactInstance decompileNE(Network network){
		ExactNetwork lcrfNetwork = (ExactNetwork)network;
		ExactInstance lcrfInstance = (ExactInstance)lcrfNetwork.getInstance();
		ExactInstance result = lcrfInstance.duplicate();
		ArrayList<String> nePred = new ArrayList<String>();
		long root = toNode_root(lcrfInstance.size());
		int rootIdx = Arrays.binarySearch(lcrfNetwork.getAllNodes(),root);
		
		for(int i=0;i<lcrfInstance.size();i++){
			int child_k = lcrfNetwork.getMaxPath(rootIdx)[0];
			long child = lcrfNetwork.getNode(child_k);
			rootIdx = child_k;
			int[] childArr = NetworkIDMapper.toHybridNodeArray(child);
			int eId = childArr[2];
			String resChunk = ChunkLabel.CHUNKS_INDEX.get(eId).getForm();
			//assume it's the IOBES encoding.
			if(resChunk.startsWith("S")) resChunk = "B"+resChunk.substring(1);
			if(resChunk.startsWith("E")) resChunk = "I"+resChunk.substring(1);
			nePred.add(0, resChunk);
		}
		result.setChunkPredictons(nePred);
		lcrfInstance.setChunkPredictons(nePred);
		return result;
	}
	
	public ExactInstance decompilePOS(Network network){
		ExactNetwork lcrfNetwork = (ExactNetwork) network;
		ExactInstance lcrfInstance = (ExactInstance) lcrfNetwork.getInstance();
		ExactInstance result = lcrfInstance.duplicate();
		ArrayList<String> posPrediction = new ArrayList<String>();
		long root = toNode_root(lcrfInstance.size());
		int rootIdx = Arrays.binarySearch(lcrfNetwork.getAllNodes(), root);

		for (int i = 0; i < lcrfInstance.size(); i++) {
			int child_k = lcrfNetwork.getMaxPath(rootIdx)[0];
			long child = lcrfNetwork.getNode(child_k);
			rootIdx = child_k;
			int[] childArr = NetworkIDMapper.toHybridNodeArray(child);
			int tId = childArr[2];
			posPrediction.add(0, TagLabel.TAGS_INDEX.get(tId).getForm());
		}
		result.setTagPredictons(posPrediction);
		return result;
	}

	
	
}

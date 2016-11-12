package com.statnlp.projects.nndcrf.factorialCRFs;

import java.util.ArrayList;
import java.util.Arrays;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.projects.nndcrf.factorialCRFs.TFConfig.TASK;

public class TFNetworkCompiler extends NetworkCompiler{

	private static final long serialVersionUID = -2388666010977956073L;

	public enum NODE_TYPES {LEAF, 
		ENODE,   //node type in NE-chain 
		TNODE,   //node type in PoS-chain
		ROOT};
	private int _size;
	private TFNetwork genericUnlabeledNetwork;
	private TASK task;
	private boolean IOBESencoding; //useless if using tagging task only
	
	public TFNetworkCompiler(TASK task, boolean IOBESencoding){
		this(task, IOBESencoding, 150);
	}
	
	public TFNetworkCompiler(TASK task, boolean IOBESencoding, int maxSize){
		this.task = task;
		this._size = maxSize;
		this.IOBESencoding = IOBESencoding;
		NetworkIDMapper.setCapacity(new int[]{1000, 10, 500});
		this.compileUnlabeledInstancesGeneric();
	}
	
	@Override
	public TFNetwork compile(int networkId, Instance inst, LocalNetworkParam param) {
		TFInstance lcrfInstance = (TFInstance)inst;
		if(lcrfInstance.isLabeled())
			return compileLabeledInstances(networkId, lcrfInstance, param);
		else
			return compileUnlabeledInstances(networkId, lcrfInstance, param);
	}
	
	public long toNode_leaf(){
		int[] arr = new int[]{0, NODE_TYPES.LEAF.ordinal(), Entity.ENTS.size()+Tag.TAGS.size()};
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
	
	public long toNode_e(int pos, int entity_id){
		int[] arr = new int[]{pos+1, NODE_TYPES.ENODE.ordinal(), entity_id};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	/**
	 * to a root node: no label for root node
	 * @param size: size of the sentence.
	 * @return
	 */
	public long toNode_root(int size){
		int[] arr = new int[]{size+1,NODE_TYPES.ROOT.ordinal(),Entity.ENTS.size()+Tag.TAGS.size()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	

	/**
	 * Compile the labeled network. it could be independent task or joint task depending on task variable.
	 * @param networkId
	 * @param inst
	 * @param param
	 * @return
	 */
	public TFNetwork compileLabeledInstances(int networkId, TFInstance inst, LocalNetworkParam param) {
		TFNetwork lcrfNetwork = new TFNetwork(networkId, inst, param);
		long leaf = toNode_leaf();
		lcrfNetwork.addNode(leaf);
		long root = toNode_root(inst.size());
		lcrfNetwork.addNode(root);

		if (task == TASK.NER || task == TASK.JOINT) {
			// NE chain structure
			long[] e_children = new long[] { leaf };
			for (int i = 0; i < inst.size(); i++) {
				// output is actually the entity.
				int entityId = Entity.ENTS.get(inst.getOutput().get(i)).getId();
				long e_node = toNode_e(i, entityId);
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
				int posId = Tag.TAGS.get(inst.getInput().get(i).getTag()).getId();
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
	

	public TFNetwork compileUnlabeledInstances(int networkId, TFInstance inst, LocalNetworkParam param) {

		long[] allNodes = genericUnlabeledNetwork.getAllNodes();
		long root = toNode_root(inst.size());
		int rootIdx = Arrays.binarySearch(allNodes, root);
		TFNetwork lcrfNetwork = new TFNetwork(networkId, inst, allNodes, genericUnlabeledNetwork.getAllChildren(),
				param, rootIdx + 1);
		return lcrfNetwork;
	}
	
	public void compileUnlabeledInstancesGeneric(){
		TFNetwork lcrfNetwork = new TFNetwork();
		long leaf = toNode_leaf();
		lcrfNetwork.addNode(leaf);
		long[] children = new long[]{leaf};
		long[] tag_children = new long[]{leaf};
		for(int i = 0; i < _size; i++){
			long root = toNode_root(i+1); // the size should be i+1
			lcrfNetwork.addNode(root);
			if(task==TASK.NER || task==TASK.JOINT){
				long[] currentNodes = new long[Entity.ENTS.size()];
				for(int e=0;e<Entity.ENTS.size();e++){
					long enode = toNode_e(i, e);
					String currEntity = Entity.ENTS_INDEX.get(e).getForm();
					for(long child: children){
						if(child==-1) continue;
						int[] childArr = NetworkIDMapper.toHybridNodeArray(child);
						String childEntity = i==0? "O":Entity.ENTS_INDEX.get(childArr[2]).getForm(); //i=0; child is the leaf
						if(childEntity.startsWith("I")){
							if(currEntity.startsWith("I") && !childEntity.substring(1).equals(currEntity.substring(1))) continue;
							if(currEntity.startsWith("E") && !childEntity.substring(1).equals(currEntity.substring(1))) continue;
							if(IOBESencoding && (currEntity.startsWith("O") || currEntity.startsWith("B") || currEntity.startsWith("S")  ) ) continue;
						}else if(childEntity.equals("O")){
							if(currEntity.startsWith("I") || currEntity.startsWith("E")) continue;
							
						}else if(childEntity.startsWith("B")){
							if(currEntity.startsWith("I")  && !childEntity.substring(1).equals(currEntity.substring(1)) ) continue;
							if(currEntity.startsWith("E")  && !childEntity.substring(1).equals(currEntity.substring(1)) ) continue;
							if(IOBESencoding && ( currEntity.startsWith("O") || currEntity.startsWith("B") || currEntity.startsWith("S") )  ) continue;
							
						}else if(childEntity.startsWith("E")){
							if(currEntity.startsWith("I") || currEntity.startsWith("E")) continue;
							
						}else if(childEntity.startsWith("S")){
							if(currEntity.startsWith("I") || currEntity.startsWith("E")) continue;
						}else{
							throw new RuntimeException("Unknown type "+childEntity+" in network compilation");
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
					String childEntity = i==0? "O":Entity.ENTS_INDEX.get(childArr[2]).getForm(); //i=0; child is the leaf
					if (IOBESencoding && childEntity.startsWith("B")) continue;
					if (IOBESencoding && childEntity.startsWith("I")) continue;
					lcrfNetwork.addEdge(root, new long[]{child});
				}
				children = currentNodes;
			}
			
			
			if(task==TASK.TAGGING || task==TASK.JOINT){
				long[] tag_currentNodes = new long[Tag.TAGS.size()];
				for(int t=0;t<Tag.TAGS.size();t++){
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
	public TFInstance decompile(Network network) {
		if(task==TASK.NER)
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
	
	public TFInstance decompileNE(Network network){
		TFNetwork lcrfNetwork = (TFNetwork)network;
		TFInstance lcrfInstance = (TFInstance)lcrfNetwork.getInstance();
		TFInstance result = lcrfInstance.duplicate();
		ArrayList<String> nePred = new ArrayList<String>();
		long root = toNode_root(lcrfInstance.size());
		int rootIdx = Arrays.binarySearch(lcrfNetwork.getAllNodes(),root);
		
		for(int i=0;i<lcrfInstance.size();i++){
			int child_k = lcrfNetwork.getMaxPath(rootIdx)[0];
			long child = lcrfNetwork.getNode(child_k);
			rootIdx = child_k;
			int[] childArr = NetworkIDMapper.toHybridNodeArray(child);
			int eId = childArr[2];
			String resEntity = Entity.ENTS_INDEX.get(eId).getForm();
			//assume it's the IOBES encoding.
			if(resEntity.startsWith("S")) resEntity = "B"+resEntity.substring(1);
			if(resEntity.startsWith("E")) resEntity = "I"+resEntity.substring(1);
			nePred.add(0, resEntity);
		}
		result.setEntityPredictons(nePred);
		lcrfInstance.setEntityPredictons(nePred);
		return result;
	}
	
	public TFInstance decompilePOS(Network network){
		TFNetwork lcrfNetwork = (TFNetwork)network;
		TFInstance lcrfInstance = (TFInstance)lcrfNetwork.getInstance();
		TFInstance result = lcrfInstance.duplicate();
		ArrayList<String> posPrediction = new ArrayList<String>();
		long root = toNode_root(lcrfInstance.size());
		int rootIdx = Arrays.binarySearch(lcrfNetwork.getAllNodes(),root);
		
		for(int i=0;i<lcrfInstance.size();i++){
			int child_k = lcrfNetwork.getMaxPath(rootIdx)[0];
			long child = lcrfNetwork.getNode(child_k);
			rootIdx = child_k;
			int[] childArr = NetworkIDMapper.toHybridNodeArray(child);
			int tId = childArr[2];
			posPrediction.add(0, Tag.TAGS_INDEX.get(tId).getForm());
		}
		result.setTagPredictons(posPrediction);
		return result;
	}

	
	
}

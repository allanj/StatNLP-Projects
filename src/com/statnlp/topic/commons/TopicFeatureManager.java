package com.statnlp.topic.commons;

import com.statnlp.commons.types.InputToken;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.topic.commons.TopicNetworkCompiler.nodeType;

public class TopicFeatureManager extends FeatureManager{
	
	private static final long serialVersionUID = -8702911622042935389L;

	public TopicFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}

	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		
		TopicNetwork bn = (TopicNetwork)network;
		TopicInstance inst = (TopicInstance)bn.getInstance();
		InputToken[] words = inst.getInput();
		
		long node_parent = bn.getNode(parent_k);
		int[] ids_parent = NetworkIDMapper.toHybridNodeArray(node_parent);
		
		if(ids_parent[4] == nodeType.ROOT.ordinal()){
			return FeatureArray.EMPTY;
		}
		else if(ids_parent[4] == nodeType.WORD.ordinal()){
			InputToken word = words[ids_parent[2]];
			int fs [] = new int[children_k.length];
			int k = 0;
			for(int child_k : children_k){
				long node_child = bn.getNode(child_k);
				int[] ids_child = NetworkIDMapper.toHybridNodeArray(node_child);
				if(ids_child[4] == nodeType.TOPIC.ordinal()){
					String topic = "topic-"+ids_child[2];
					fs[k++] = this._param_g.toFeature("topic-word", topic, word.getName());
				} else {
					throw new RuntimeException("(0) The node type is "+ids_child[4]);
				}
			}
			return new FeatureArray(fs);
		}
		else if(ids_parent[4] == nodeType.TOPIC.ordinal()){
			String topic = "topic-"+ids_parent[2];
			int fs [] = new int[children_k.length];
			int k = 0;
			for(int child_k : children_k){
				long node_child = bn.getNode(child_k);
				int[] ids_child = NetworkIDMapper.toHybridNodeArray(node_child);
				if(ids_child[4] == nodeType.LEAF.ordinal()){
					int networkId = ids_child[2];
					fs[k++] = this._param_g.toFeature("doc-topic", "doc-"+networkId, topic);
				} else {
					throw new RuntimeException("(1) The node type is "+ids_child[4]);
				}
			}
			return new FeatureArray(fs);
		}
		else if(ids_parent[4] == nodeType.LEAF.ordinal()){
			return FeatureArray.EMPTY;
		}
		else {
			throw new RuntimeException("(2) The node type is "+ids_parent[4]);
		}
		
	}
	
}

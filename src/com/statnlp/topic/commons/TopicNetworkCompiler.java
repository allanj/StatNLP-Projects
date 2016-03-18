package com.statnlp.topic.commons;

import com.statnlp.commons.types.InputToken;
import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class TopicNetworkCompiler extends NetworkCompiler{
	
	private static final long serialVersionUID = 4344621495273642001L;
	
	public enum nodeType {LEAF, WORD, TOPIC, ROOT};
	
	private TopicToken[] _topics;
	
	public TopicNetworkCompiler(int numCepts) {
		this._topics = new TopicToken[numCepts];
		for(int k = 0; k<numCepts; k++)
			this._topics[k] = new TopicToken("<Cept:"+k+">");
	}
	
	@Override
	public TopicInstance decompile(Network network) {
		//TODO
		return null;
	}
	
	@Override
	public TopicNetwork compile(int networkId, Instance inst, LocalNetworkParam param) {
		
		if(inst.getWeight()<=0){
			TopicNetwork bn = new TopicNetwork();
			bn.finalizeNetwork();
			return bn;
		}
		if(this._topics.length==0){
			return this.compileLabeled(networkId, (TopicInstance)inst, param);
		} else {
			return this.compileLabeled_hidden(networkId, (TopicInstance)inst, param);
		}
	}

	private TopicNetwork compileLabeled_hidden(int networkId, TopicInstance inst, LocalNetworkParam param){
		
		TopicNetwork network = new TopicNetwork(networkId, inst, param);
		
		InputToken[] inputs = inst.getInput();
		
		long leaf = this.toNode_leaf(networkId);
		network.addNode(leaf);
		
		long[] nodes_topic = new long[this._topics.length];
		for(int pos_topic = 0; pos_topic<this._topics.length; pos_topic++){
			long node_topic = this.toNode_topic(pos_topic);
			network.addNode(node_topic);
			network.addEdge(node_topic, new long[]{leaf});
			nodes_topic[pos_topic] = node_topic;
		}
		
		long[] nodes_word = new long[inputs.length];
		for(int pos_word = 0; pos_word<inputs.length; pos_word++){
			long node_word = this.toNode_word(pos_word);
			network.addNode(node_word);
			for(long node_topic : nodes_topic)
				network.addEdge(node_word, new long[]{node_topic});
			nodes_word[pos_word] = node_word;
		}
		
		long root = this.toNode_root();
		network.addNode(root);
		network.addEdge(root, nodes_word);
		
		network.finalizeNetwork();
		
		return network;
		
	}
	
	private TopicNetwork compileLabeled(int networkId, TopicInstance inst, LocalNetworkParam param){
		
//		TopicNetwork network = new TopicNetwork(networkId, inst, param);
//		
//		InputToken[] inputs = inst.getInput();
//		OutputToken[] outputs = inst.getOutput();
//		
//		long leaf = this.toNode_leaf();
//		network.addNode(leaf);
//		
//		long[] nodes_src = new long[inputs.length];
//		for(int k = 0; k<inputs.length; k++){
//			long node_src = this.toNode_src(k);
//			network.addNode(node_src);
//			network.addEdge(node_src, new long[]{leaf});
//			nodes_src[k] = node_src;
//		}
//		
//		long[] nodes_tgt = new long[outputs.length];
//		for(int k = 0; k<outputs.length; k++){
//			long node_tgt = this.toNode_tgt(k);
//			network.addNode(node_tgt);
//			for(long node_src : nodes_src)
//				network.addEdge(node_tgt, new long[]{node_src});
//			nodes_tgt[k] = node_tgt;
//		}
//		
//		long root = this.toNode_root();
//		network.addNode(root);
//		network.addEdge(root, nodes_tgt);
//		
//		network.finalizeNetwork();
//		
//		return network;
		
		return null;
	}
	
	private long toNode_leaf(int network_id){
		return NetworkIDMapper.toHybridNodeID(new int[]{0, 0, network_id, 0, nodeType.LEAF.ordinal()});
	}
	
	private long toNode_topic(int pos_cept){
		return NetworkIDMapper.toHybridNodeID(new int[]{1, 0, pos_cept, 0, nodeType.TOPIC.ordinal()});
	}
	
	private long toNode_word(int pos_src){
		return NetworkIDMapper.toHybridNodeID(new int[]{2, 0, pos_src, 0, nodeType.WORD.ordinal()});
	}
	
	private long toNode_root(){
		return NetworkIDMapper.toHybridNodeID(new int[]{1000, 0, 1000, 0, nodeType.ROOT.ordinal()});
	}
	
}
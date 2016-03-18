package com.statnlp.mt.commons;

import com.statnlp.commons.types.InputToken;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.OutputToken;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class BitextNetworkCompiler extends NetworkCompiler{
	
	private static final long serialVersionUID = 4344621495273642001L;
	
	public enum nodeType {LEAF, SRC, HIDDEN, EXPLICIT, TGT, ROOT};
	
	private CeptToken[] _cepts;
	
	public BitextNetworkCompiler(int numCepts) {
		this._cepts = new CeptToken[numCepts];
		for(int k = 0; k<numCepts; k++)
			this._cepts[k] = new CeptToken("<Cept:"+k+">");
	}
	
	@Override
	public BitextInstance decompile(Network network) {
		//THERE'S NO NEED TO IMPLEMENT THIS METHOD.
		return null;
	}
	
	@Override
	public BitextNetwork compile(int networkId, Instance inst, LocalNetworkParam param) {
		if(inst.getWeight()<=0.0){
			BitextNetwork bn = new BitextNetwork();
			bn.finalizeNetwork();
			return bn;
		}
		if(this._cepts.length==0){
			return this.compileLabeled(networkId, (BitextInstance)inst, param);
		} else {
			return this.compileLabeled_hidden_v3(networkId, (BitextInstance)inst, param);
		}
	}
	
	//this version does not do anything. the simplest version
	@SuppressWarnings("unused")
	private BitextNetwork compileLabeled_hidden_v1(int networkId, BitextInstance inst, LocalNetworkParam param){
		
		BitextNetwork network = new BitextNetwork(networkId, inst, param);
		
		InputToken[] inputs = inst.getInput();
		OutputToken[] outputs = inst.getOutput();
		
		long leaf = this.toNode_leaf();
		network.addNode(leaf);
		
		long[] nodes_src = new long[inputs.length];
		for(int pos_src = 0; pos_src<inputs.length; pos_src++){
			long node_src = this.toNode_src(pos_src);
			network.addNode(node_src);
			network.addEdge(node_src, new long[]{leaf});
			nodes_src[pos_src] = node_src;
		}
		
		long[] nodes_cept = new long[this._cepts.length];
		for(int pos_cept = 0; pos_cept<this._cepts.length; pos_cept++){
			long node_cept = this.toNode_hidden(pos_cept);
			network.addNode(node_cept);
			for(long node_src : nodes_src)
				network.addEdge(node_cept, new long[]{node_src});
			nodes_cept[pos_cept] = node_cept;
		}
		
		long[] nodes_tgt = new long[outputs.length];
		for(int pos_tgt = 0; pos_tgt<outputs.length; pos_tgt++){
			long node_tgt = this.toNode_tgt(pos_tgt);
			network.addNode(node_tgt);
			for(long node_cept : nodes_cept)
				network.addEdge(node_tgt, new long[]{node_cept});
			nodes_tgt[pos_tgt] = node_tgt;
		}
		
		long root = this.toNode_root();
		network.addNode(root);
		network.addEdge(root, nodes_tgt);
		
		network.finalizeNetwork();
		
		return network;
		
	}
	
	//this version adds the NULL word to the source side...
	@SuppressWarnings("unused")
	private BitextNetwork compileLabeled_hidden_v2(int networkId, BitextInstance inst, LocalNetworkParam param){
		
		BitextNetwork network = new BitextNetwork(networkId, inst, param);
		
		InputToken[] inputs = inst.getInput();
		OutputToken[] outputs = inst.getOutput();
		
		long leaf = this.toNode_leaf();
		network.addNode(leaf);
		
		long[] nodes_src = new long[inputs.length+1];
		for(int pos_src = 0; pos_src<inputs.length; pos_src++){
			long node_src = this.toNode_src(pos_src);
			network.addNode(node_src);
			network.addEdge(node_src, new long[]{leaf});
			nodes_src[pos_src] = node_src;
		}
		
		{
			long node_src = this.toNode_src(inputs.length);
			network.addNode(node_src);
			network.addEdge(node_src, new long[]{leaf});
			nodes_src[inputs.length] = node_src;
		}
		
		long[] nodes_cept = new long[this._cepts.length];
		for(int pos_cept = 0; pos_cept<this._cepts.length; pos_cept++){
			long node_cept = this.toNode_hidden(pos_cept);
			network.addNode(node_cept);
			for(long node_src : nodes_src)
				network.addEdge(node_cept, new long[]{node_src});
			nodes_cept[pos_cept] = node_cept;
		}
		
		long[] nodes_tgt = new long[outputs.length];
		for(int pos_tgt = 0; pos_tgt<outputs.length; pos_tgt++){
			long node_tgt = this.toNode_tgt(pos_tgt);
			network.addNode(node_tgt);
			for(long node_cept : nodes_cept)
				network.addEdge(node_tgt, new long[]{node_cept});
			nodes_tgt[pos_tgt] = node_tgt;
		}
		
		long root = this.toNode_root();
		network.addNode(root);
		network.addEdge(root, nodes_tgt);
		
		network.finalizeNetwork();
		
		return network;
		
	}
	
	//this version adds the NULL word to the source side, and add the self nodes..
	private BitextNetwork compileLabeled_hidden_v3(int networkId, BitextInstance inst, LocalNetworkParam param){
		
		BitextNetwork network = new BitextNetwork(networkId, inst, param);
		
		InputToken[] inputs = inst.getInput();
		OutputToken[] outputs = inst.getOutput();
		
		long leaf = this.toNode_leaf();
		network.addNode(leaf);
		
		long[] nodes_src = new long[inputs.length+1];
		for(int pos_src = 0; pos_src<inputs.length; pos_src++){
			long node_src = this.toNode_src(pos_src);
			network.addNode(node_src);
			network.addEdge(node_src, new long[]{leaf});
			nodes_src[pos_src] = node_src;
		}
		
		{
			long node_src = this.toNode_src(inputs.length);
			network.addNode(node_src);
			network.addEdge(node_src, new long[]{leaf});
			nodes_src[inputs.length] = node_src;
		}
		
		long[] nodes_cept = new long[this._cepts.length+inputs.length+1];
		for(int pos_cept = 0; pos_cept<this._cepts.length; pos_cept++){
			long node_cept = this.toNode_hidden(pos_cept);
			network.addNode(node_cept);
			for(long node_src : nodes_src)
				network.addEdge(node_cept, new long[]{node_src});
			nodes_cept[pos_cept] = node_cept;
		}
		
		//now, add the self nodes..
		for(int pos_src = 0; pos_src<inputs.length; pos_src++){
			long node_explicit = this.toNode_explicit(pos_src);
			network.addNode(node_explicit);
			network.addEdge(node_explicit, new long[]{nodes_src[pos_src]});
			nodes_cept[this._cepts.length+pos_src] = node_explicit;
		}
		
		{
			long node_explicit = this.toNode_explicit(inputs.length);
			network.addNode(node_explicit);
			network.addEdge(node_explicit, new long[]{nodes_src[inputs.length]});
			nodes_cept[this._cepts.length+inputs.length] = node_explicit;
		}
		
		long[] nodes_tgt = new long[outputs.length];
		for(int pos_tgt = 0; pos_tgt<outputs.length; pos_tgt++){
			long node_tgt = this.toNode_tgt(pos_tgt);
			network.addNode(node_tgt);
			for(long node_cept : nodes_cept)
				network.addEdge(node_tgt, new long[]{node_cept});
			nodes_tgt[pos_tgt] = node_tgt;
		}
		
		long root = this.toNode_root();
		network.addNode(root);
		network.addEdge(root, nodes_tgt);
		
		network.finalizeNetwork();
		
		return network;
		
	}
	
	@SuppressWarnings("unused")
	private BitextNetwork compileLabeled_hidden_old(int networkId, BitextInstance inst, LocalNetworkParam param){
		
		BitextNetwork network = new BitextNetwork(networkId, inst, param);
		
		InputToken[] inputs = inst.getInput();
		OutputToken[] outputs = inst.getOutput();
		
		long leaf = this.toNode_leaf();
		network.addNode(leaf);
		
		long[] nodes_src = new long[inputs.length];
		for(int pos_src = 0; pos_src<inputs.length; pos_src++){
			long node_src = this.toNode_src(pos_src);
			network.addNode(node_src);
			network.addEdge(node_src, new long[]{leaf});
			nodes_src[pos_src] = node_src;
		}
		
		long[] nodes_cept = new long[this._cepts.length];
		for(int pos_cept = 0; pos_cept<this._cepts.length; pos_cept++){
			long node_cept = this.toNode_hidden(pos_cept);
			network.addNode(node_cept);
			for(long node_src : nodes_src)
				network.addEdge(node_cept, new long[]{node_src});
			nodes_cept[pos_cept] = node_cept;
		}
		
		long[] nodes_tgt = new long[outputs.length];
		for(int pos_tgt = 0; pos_tgt<outputs.length; pos_tgt++){
			long node_tgt = this.toNode_tgt(pos_tgt);
			network.addNode(node_tgt);
			for(long node_cept : nodes_cept)
				network.addEdge(node_tgt, new long[]{node_cept});
			nodes_tgt[pos_tgt] = node_tgt;
		}
		
		long root = this.toNode_root();
		network.addNode(root);
		network.addEdge(root, nodes_tgt);
		
		network.finalizeNetwork();
		
		return network;
		
	}
	
	private BitextNetwork compileLabeled(int networkId, BitextInstance inst, LocalNetworkParam param){
		
		BitextNetwork network = new BitextNetwork(networkId, inst, param);
		
		InputToken[] inputs = inst.getInput();
		OutputToken[] outputs = inst.getOutput();
		
		long leaf = this.toNode_leaf();
		network.addNode(leaf);
		
		long[] nodes_src = new long[inputs.length+1];
		for(int k = 0; k<inputs.length; k++){
			long node_src = this.toNode_src(k);
			network.addNode(node_src);
			network.addEdge(node_src, new long[]{leaf});
			nodes_src[k] = node_src;
		}
		
		{
			long node_src = this.toNode_src(inputs.length);
			network.addNode(node_src);
			network.addEdge(node_src, new long[]{leaf});
			nodes_src[inputs.length] = node_src;	
		}
		
		long[] nodes_tgt = new long[outputs.length];
		for(int k = 0; k<outputs.length; k++){
			long node_tgt = this.toNode_tgt(k);
			network.addNode(node_tgt);
			for(long node_src : nodes_src)
				network.addEdge(node_tgt, new long[]{node_src});
			nodes_tgt[k] = node_tgt;
		}
		
		long root = this.toNode_root();
		network.addNode(root);
		network.addEdge(root, nodes_tgt);
		
		network.finalizeNetwork();
		
		return network;
		
	}
	
	private long toNode_leaf(){
		return NetworkIDMapper.toHybridNodeID(new int[]{0, 0, 0, 0, nodeType.LEAF.ordinal()});
	}
	
	private long toNode_src(int pos_src){
		return NetworkIDMapper.toHybridNodeID(new int[]{1, 0, pos_src, 0, nodeType.SRC.ordinal()});
	}
	
	private long toNode_hidden(int pos_cept){
		return NetworkIDMapper.toHybridNodeID(new int[]{2, 0, pos_cept, 0, nodeType.HIDDEN.ordinal()});
	}
	
	private long toNode_explicit(int pos_src){
		return NetworkIDMapper.toHybridNodeID(new int[]{3, 0, pos_src, 0, nodeType.EXPLICIT.ordinal()});
	}
	
	private long toNode_tgt(int pos_tgt){
		return NetworkIDMapper.toHybridNodeID(new int[]{4, 0, pos_tgt, 0, nodeType.TGT.ordinal()});
	}
	
	private long toNode_root(){
		return NetworkIDMapper.toHybridNodeID(new int[]{1000, 0, 0, 0, nodeType.ROOT.ordinal()});
	}
	
}
/** Statistical Natural Language Processing System
    Copyright (C) 2014-2015  Lu, Wei

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * 
 */
package com.statnlp.cws;

import java.util.ArrayList;
import java.util.Arrays;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Segment;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkIDMapper;

/**
 * @author wei_lu
 *
 */
public class CWSNetworkCompiler extends NetworkCompiler{
	
	private static final long serialVersionUID = 6309385525943163056L;
	
	private int _tagGram;
	private ArrayList<CWSOutputTokenList> _lists;
	private CWSOutputTokenSet _set;
	
	public CWSNetworkCompiler(CWSOutputTokenSet set, ArrayList<CWSOutputTokenList> lists){
		this._set = set;
		this._tagGram = CWSNetworkConfig.TAG_GRAM;
		this._lists = lists;
		
		int[] capacity = new int[3+this._tagGram];
		capacity[0] = 500;
		capacity[1] = 10;
		capacity[2] = 10;
		for(int k = 3; k<capacity.length; k++){
			capacity[k] = 100;
		}
		NetworkIDMapper.setCapacity(capacity);
	}
	
	@Override
	public CWSNetwork compile(int networkId, Instance inst, LocalNetworkParam param) {
		
		CWSInstance cInst = (CWSInstance)inst;
		
		if(cInst.hasOutput()){
			return this.compile_labeled(networkId, cInst, param);
		} else {
			CWSNetwork network_unlabeled = this.compile_unlabeled(networkId, cInst, param);
//			CWSNetwork network_labeled = this.compile_labeled(networkId, cInst, param);
			
//			System.err.println(network_unlabeled.countNodes()+">>"+network_labeled.countNodes());
//			
//			if(!network_unlabeled.contains(network_labeled)){
//				System.err.println("oho...");
//				System.exit(1);
//			}
			
			return network_unlabeled;
		}
		
	}
	
	private CWSNetwork compile_labeled(int networkId, CWSInstance inst, LocalNetworkParam param){
		
		CWSNetwork network = new CWSNetwork(networkId, inst, param);
		
		CWSOutput output = inst.getOutput();
		ArrayList<Segment> segs = output.getSegments();
		
		ArrayList<CWSOutputToken> tags = new ArrayList<CWSOutputToken>();
		
		for(int k = 0; k<this._tagGram; k++){
			tags.add(CWSOutputToken._START);
		}
		
		long node_prev = -1;
		
		for(Segment seg : segs){
			int bIndex = seg.getBIndex();
			int eIndex = seg.getEIndex();
			
			CWSOutputToken tag = output.getOutputBySegment(seg);
			tags.add(tag);
			tags.remove(0);
			
			CWSOutputTokenList tagList = new CWSOutputTokenList(tags);
			long node = this.toNode(bIndex, eIndex, tagList);
			network.addNode(node);
			
			CWSOutputTokenList prefixList = tagList.getPrefix();
			long node_prefix = this.toNode_agg(bIndex, prefixList);
			if(bIndex==0){
				network.addNode(node_prefix);
			}
//			System.err.println(bIndex+"\t"+eIndex);
			network.addEdge(node, new long[]{node_prefix});
			
			CWSOutputTokenList suffixList = tagList.getSuffix();
			long node_suffix = this.toNode_agg(eIndex, suffixList);
			
			network.addNode(node_suffix);
			network.addEdge(node_suffix, new long[]{node});
			
			node_prev = node_suffix;
		}
		
		long node_root = this.toNode_root(inst.size());
		network.addNode(node_root);
		network.addEdge(node_root, new long[]{node_prev});
		
		network.finalizeNetwork();
		
		return network;
		
	}
	
	private CWSNetwork compile_unlabeled(int networkId, CWSInstance inst, LocalNetworkParam param){
		
		CWSNetwork network = new CWSNetwork(networkId, inst, param);
		
//		WordToken[] inputs = inst.getInput();
//		System.err.println(">>"+inst.getInput().length+"<<"+"\t"+inst.size());
		
		ArrayList<CWSOutputToken> tags = new ArrayList<CWSOutputToken>();
		
		for(int k = 0; k<this._tagGram; k++){
			tags.add(CWSOutputToken._START);
		}
		CWSOutputTokenList tagList0 = new CWSOutputTokenList(tags);
		CWSOutputTokenList prefixList0 = tagList0.getPrefix();
		long node_prefix0 = this.toNode_agg(0, prefixList0);
		network.addNode(node_prefix0);
		
		ArrayList<Long> lastNodes = new ArrayList<Long>();
		
		for(int eIndex = 1; eIndex<= inst.size(); eIndex++){
			
			for(int L = CWSNetworkConfig.MAX_WORD_LEN; L>=1; L--){
				int bIndex = eIndex - L;
				
				if(bIndex<0)
					continue;
				
				for(int k = 0; k<this._lists.size(); k++){
					CWSOutputTokenList tagList = this._lists.get(k);
					
//					System.err.println(bIndex+","+eIndex+"\t"+tagList);
					
					if(bIndex==0){
						if(!tagList.getPrefix().containsOnlyStartNodes()){
							continue;
//						} else {
//							System.err.println("OK");
						}
					}
					
					CWSOutputTokenList prefixList = tagList.getPrefix();
					long node_prefix = this.toNode_agg(bIndex, prefixList);
					
					if(network.contains(node_prefix)){
						
						long node = this.toNode(bIndex, eIndex, tagList);
						network.addNode(node);
						network.addEdge(node, new long[]{node_prefix});
						
						CWSOutputTokenList suffixList = tagList.getSuffix();
						long node_suffix = this.toNode_agg(eIndex, suffixList);
						
						if(!network.contains(node_suffix)){
							network.addNode(node_suffix);
						}
						
						network.addEdge(node_suffix, new long[]{node});
						if(eIndex == inst.size()){
							if(!lastNodes.contains(node_suffix)){
								lastNodes.add(node_suffix);
							}
						}
						
					}
					
				}
				
			}
			
		}
		
		long node_root = this.toNode_root(inst.size());
//		System.err.println(lastNodes.size());
//		System.exit(1);
		network.addNode(node_root);
		for(int k = 0; k<lastNodes.size(); k++){
			network.addEdge(node_root, new long[]{lastNodes.get(k)});
		}
		
		network.finalizeNetwork();
		
		return network;
		
	}
	
	@Override
	public CWSInstance decompile(Network network) {
		CWSNetwork cNetwork = (CWSNetwork)network;
		CWSInstance inst = (CWSInstance)cNetwork.getInstance();
		
		CWSOutput prediction = new CWSOutput();
		this.decompile_helper(cNetwork, prediction, cNetwork.countNodes()-1);
		inst.setPrediction(prediction);
		
		return inst;
	}
	
	private void decompile_helper(CWSNetwork network, CWSOutput output, int node_k){
		
		long node = network.getNode(node_k);
		int[] ids = NetworkIDMapper.toHybridNodeArray(node);
		if(ids[1]==0){
			int eIndex = ids[0];
			int bIndex = eIndex - ids[2];
			int tagId = ids[ids.length-1];
			CWSOutputToken tag = this._set.getOutputTokenById(tagId);
			output.addOutput(new Segment(bIndex, eIndex), tag);
		}
		
		int[] children_k = network.getMaxPath(node_k);
		
		for(int child_k : children_k){
			this.decompile_helper(network, output, child_k);
		}
		
	}
	
	private long toNode_root(int eIndex){
		int[] ids = new int[this._tagGram+3];
		ids[0] = eIndex;
		ids[1] = 2;
		return NetworkIDMapper.toHybridNodeID(ids);
	}

	//the last element in tags is the current tag.
	private long toNode_agg(int eIndex, CWSOutputTokenList tagList){
		int[] ids = new int[this._tagGram+3];
		ids[0] = eIndex;
		ids[1] = 1;
		ids[2] = 0;
		for(int k = 0; k<tagList.size(); k++){
			ids[k+3] = tagList.getTokens().get(k).getId();
		}
		ids[tagList.size()+2] = 0;
		return NetworkIDMapper.toHybridNodeID(ids);
	}
	
	//the last element in tags is the current tag.
	private long toNode(int bIndex, int eIndex, CWSOutputTokenList tagList){
		int[] ids = new int[this._tagGram+3];
		ids[0] = eIndex;
		ids[1] = 0;
		ids[2] = eIndex-bIndex;
		for(int k = 0; k<tagList.size(); k++){
			ids[k+3] = tagList.getTokens().get(k).getId();
		}
		long v = NetworkIDMapper.toHybridNodeID(ids);
//		if(bIndex==eIndex){
////			System.err.println(bIndex+"\t"+eIndex+"+++"+v+"||"+Arrays.toString(ids));
//			System.exit(1);
//		}
		return v;
	}
	
	
	
}

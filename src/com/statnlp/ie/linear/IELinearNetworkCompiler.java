/** Statistical Natural Language Processing System
    Copyright (C) 2014  Lu, Wei

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
package com.statnlp.ie.linear;

import java.util.ArrayList;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Segment;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.ie.types.LabeledTextSpan;
import com.statnlp.ie.types.Mention;
import com.statnlp.ie.types.SemanticTag;
import com.statnlp.ie.types.UnlabeledTextSpan;

public class IELinearNetworkCompiler extends NetworkCompiler{
	
	private static final long serialVersionUID = 3457392668746597522L;
	
	private int _N;
	private int _M; // number of possible tags at each position
	private SemanticTag[] _tags; //all possible tags, excluding start and end.
//	private boolean _isLabeled = false;
	private boolean _built = false;
	private long[] _nodes;
	private int[][][] _children;
	private int[] _roots;
	private boolean _debug = false;
	
	public IELinearNetworkCompiler(SemanticTag[] tags){
		this._tags = tags;
		this._N = IELinearConfig._MAX_SENT_LENGTH;
		this._M = this._tags.length;
	}
	
	@Override
	public IELinearNetwork compile(int networkId, Instance inst, LocalNetworkParam param) {
		
		if(inst.isLabeled()){
//			System.err.println("Ok..labeled..");
			IELinearNetwork network = this.build_labeled(networkId, (IELinearInstance)inst, param);
//			System.err.println("labeled.#nodes="+network.countNodes());
			
//			{
//				if(!this._built){
//					this.build_unlabeled();
//					this._built = true;
//				}
//				int numNodes = this.getRootPositionForLen(inst.size())+1;
//				IELinearNetwork network2 = new IELinearNetwork(networkId, (IELinearInstance)inst, this._nodes, this._children, param, numNodes);
//				System.err.println(network2.contains(network));
//				System.exit(1);
//			}
			
			return network;
		} else {
//			System.err.println("Ok..unlabeled");
			if(!this._built){
				this.build_unlabeled();
				this._built = true;
			}
			int numNodes = this.getRootPositionForLen(inst.size())+1;
			IELinearNetwork network = new IELinearNetwork(networkId, (IELinearInstance)inst, this._nodes, this._children, param, numNodes);
//			System.err.println("unlabeled.#nodes="+numNodes);
			return network;
		}
		
	}
	
	public IELinearNetwork build_labeled(int id, IELinearInstance inst, LocalNetworkParam param){
		
		IELinearNetwork network = new IELinearNetwork(id, inst, param);
		this.build_labeled(network, inst);
		network.finalizeNetwork();
//		int size = 1 + (this._M * 2 + 3) * inst.length();
//		System.err.println("There are "+network.countNodes()+" nodes. maximum: "+size);
		return network;
		
	}
	
//	public IELinearNetwork build_unlabeled(int id, IELinearInstance inst, LocalNetworkParam param){
//		this._N = inst.size();
//		IELinearNetwork network = new IELinearNetwork(id, inst, param);
//		this.build(network, -1);
//		network.finalizeNetwork();
//		return network;
//	}
	
	public void build_unlabeled(){
		IELinearNetwork network = new IELinearNetwork();
		this.build(network, -1);
		network.finalizeNetwork();
		this._nodes = network.getAllNodes();
		this._children = network.getAllChildren();
		int size = 1 + (this._M * 2 + 3) * this._N;
		if(size!=this._nodes.length){
			throw new RuntimeException("The size is "+this._nodes.length+" which is different from expected "+size);
		}
		this._roots = new int[this._N];
		for(int k = 1; k<=this._N; k++){
			this._roots[k-1] = 1 + (this._M * 2 + 3) * k - 1;
//			System.err.println(this._roots[k-1]+":"+NetworkIDMapper.viewHybridNode_ie(this._nodes[this._roots[k-1]], this._N, this._M));
		}
//		System.err.println("size="+this._nodes.length+"\t"+size);
//		System.err.println(":"+NetworkIDMapper.viewHybridNode_ie(this._nodes[this._nodes.length-1], this._N, this._M));
	}
	
	public int getRootPositionForLen(int len){
		return this._roots[len-1];
	}
	
	private SemanticTag[] getTags(int bIndex){
		return this._tags;
	}
	
	public void build_labeled(IELinearNetwork network, IELinearInstance inst){
		
		LabeledTextSpan span = inst.getOutput();
		
		boolean isLabeled = true;
		
		int N = span.length();
		
		ArrayList<Segment> segments = null;
		
		if(isLabeled){
			segments = span.getAllSegments();
		}
		
		long node_terminate = this.toNode_terminate();
		network.addNode(node_terminate);
		if(this._debug)
			System.err.println("TERMINATE:");
		
		for(int bIndex = -1; bIndex>= -N; bIndex--){
			long node_after_start = this.toNode_after_start(bIndex);
			long node_exact_start = this.toNode_exact_start(bIndex);
			network.addNode(node_after_start);
			network.addNode(node_exact_start);
			if(bIndex==-1){
				network.addEdge(node_after_start, new long[]{node_exact_start, node_terminate});
			} else {
				long node_after_start_next = this.toNode_after_start(bIndex+1);
				network.addEdge(node_after_start, new long[]{node_exact_start, node_after_start_next});
			}
//			if(bIndex==-N)
			{
				long node_root = this.toNode_root(bIndex);
				network.addNode(node_root);
				if(this._debug)
					System.err.println("ROOT:"+bIndex);
				network.addEdge(node_root, new long[]{node_after_start});
			}
			
			long[] nodes_exact_start_tag = new long[this._tags.length];
//			System.err.println(Arrays.toString(this._tags));
//			System.exit(1);
			for(int k = 0; k<this._tags.length; k++){
				SemanticTag tag = this._tags[k];
				long node_exact_start_tag = this.toNode_exact_start_tag(bIndex, tag);
				network.addNode(node_exact_start_tag);
				nodes_exact_start_tag[k] = node_exact_start_tag;
				
				if(isLabeled){
					boolean should_start = false;
					boolean should_terminate = false;
					boolean should_connect_next = false;
					
					for(Segment segment : segments){
						ArrayList<Mention> mentions = ((LabeledTextSpan)span).getLabels_all(segment.getBIndex(), segment.getEIndex());

						boolean found_tag = false;
						for(Mention mention: mentions){
							if(mention.getSemanticTag().equals(tag)){
								found_tag = true;
								break;
							}
						}
						if(!found_tag){
							continue;
						}
						
						if(segment.getBIndex()-N==bIndex){
							should_start = true;
						}
						if(segment.getBIndex()-N<=bIndex && segment.getEIndex()-N>bIndex+1){
							should_connect_next = true;
						}
						if(segment.getEIndex()-N-1==bIndex){
							should_terminate = true;
						}
					}
					
//					long node_incomplete_start_tag = this.toNode_incomplete_start_tag_numerator(bIndex, tag);
					long node_incomplete_start_tag = this.toNode_incomplete_start_tag(bIndex, tag);
					
					if(should_start){
//						System.err.println("started.."+(N+bIndex));
						network.addNode(node_incomplete_start_tag);
						network.addEdge(node_exact_start_tag, new long[]{node_incomplete_start_tag});
					} else {
						network.addEdge(node_exact_start_tag, new long[]{node_terminate});
					}
					
					if(should_terminate){
						network.addNode(node_incomplete_start_tag);
//						long node_incomplete_start_tag_next = this.toNode_incomplete_start_tag_numerator(bIndex+1, tag);
						long node_incomplete_start_tag_next = this.toNode_incomplete_start_tag(bIndex+1, tag);
						if(should_connect_next){
//							System.err.println("terminated+connected.."+(N+bIndex+1));
							network.addEdge(node_incomplete_start_tag, new long[]{node_terminate, node_incomplete_start_tag_next});
						} else {
//							System.err.println("terminated.."+(N+bIndex+1));
							network.addEdge(node_incomplete_start_tag, new long[]{node_terminate});
						}
					}
					else if(should_connect_next){
//						System.err.println("connected.."+(N+bIndex));
						network.addNode(node_incomplete_start_tag);
//						long node_incomplete_start_tag_next = this.toNode_incomplete_start_tag_numerator(bIndex+1, tag);
						long node_incomplete_start_tag_next = this.toNode_incomplete_start_tag(bIndex+1, tag);
						network.addEdge(node_incomplete_start_tag, new long[]{node_incomplete_start_tag_next});
					}
				} else {
					network.addEdge(node_exact_start_tag, new long[]{node_terminate});
					
					long node_incomplete_start_tag = this.toNode_incomplete_start_tag(bIndex, tag);
					network.addNode(node_incomplete_start_tag);
					network.addEdge(node_exact_start_tag, new long[]{node_incomplete_start_tag});
					
					network.addEdge(node_incomplete_start_tag, new long[]{node_terminate});
					
					if(bIndex<-1){
						long node_incomplete_start_tag_next = this.toNode_incomplete_start_tag(bIndex+1, tag);
						network.addEdge(node_incomplete_start_tag, new long[]{node_incomplete_start_tag_next});
						network.addEdge(node_incomplete_start_tag, new long[]{node_terminate, node_incomplete_start_tag_next});
					}
				}
			}
			network.addEdge(node_exact_start, nodes_exact_start_tag);
		}
		
	}
	
	public void build(IELinearNetwork network, int bIndex){
		
		long node_terminate = this.toNode_terminate();
		
		if(bIndex == -1){
			network.addNode(node_terminate);
			if(this._debug)
				System.err.println("TERMINATE:");
		}
		
		long node_after_start = this.toNode_after_start(bIndex);
		network.addNode(node_after_start);
		if(_debug){
			System.err.println("AFTER_START:"+bIndex);
		}
		
		long node_exact_start = this.toNode_exact_start(bIndex);
		network.addNode(node_exact_start);
		if(_debug){
			System.err.println("EXACT_START:"+bIndex);
		}
		
		if(bIndex == -1){
			network.addEdge(node_after_start, new long[]{node_exact_start, node_terminate});
		} else {
			long node_after_start_next = this.toNode_after_start(bIndex+1);
			network.addEdge(node_after_start, new long[]{node_exact_start, node_after_start_next});
		}
		//FINISH AS
		
		SemanticTag[] tags = this.getTags(bIndex);
		
		long[] nodes_exact_start_tags = new long[tags.length];
		
		for(int k = 0; k<tags.length; k++){
			SemanticTag tag = tags[k];
			
			long node_exact_start_tag = this.toNode_exact_start_tag(bIndex, tag);
			network.addNode(node_exact_start_tag);
			if(this._debug)
				System.err.println("EXACT_START_TAG:"+bIndex+"\t"+tag);
			
			nodes_exact_start_tags[k] = node_exact_start_tag;
			
			long node_incomplete_start_tag = this.toNode_incomplete_start_tag(bIndex, tag);
			network.addNode(node_incomplete_start_tag);
			if(this._debug)
				System.err.println("INCOMPLETE_START_TAG:"+bIndex+"\t"+tag);
			
			network.addEdge(node_exact_start_tag, new long[]{node_terminate});
			network.addEdge(node_exact_start_tag, new long[]{node_incomplete_start_tag});
			//FINISH EST
			
			network.addEdge(node_incomplete_start_tag, new long[]{node_terminate});
			
			if(bIndex<-1){
				long node_incomplete_start_tag_next = this.toNode_incomplete_start_tag(bIndex+1, tag);
				if(network.contains(node_incomplete_start_tag_next)){
					network.addEdge(node_incomplete_start_tag, new long[]{node_incomplete_start_tag_next});
					network.addEdge(node_incomplete_start_tag, new long[]{node_terminate, node_incomplete_start_tag_next});
				}
			}
			//FINISH IST
		}
		
//		System.err.println("size="+nodes_exact_start_tags.length);
		network.addEdge(node_exact_start, nodes_exact_start_tags);
		//FINISH ES
		
		long node_root = this.toNode_root(bIndex);
		network.addNode(node_root);
		if(this._debug){
			System.err.println("ROOT:"+bIndex);
		}
		network.addEdge(node_root, new long[]{node_after_start});
		//FINISH ROOT
		
		if(bIndex!=-this._N)
			this.build(network, bIndex-1);
		
	}
	
	@Override
	public IELinearInstance decompile(Network network) {
		
		IELinearNetwork iNetwork = (IELinearNetwork)network;
		IELinearInstance inst = (IELinearInstance)network.getInstance();
		IELinearInstance inst_output = (IELinearInstance) inst.duplicate();
		
		UnlabeledTextSpan span = (UnlabeledTextSpan)inst.getInput();
		
		int root_k = network.countNodes()-1;
		this.decode(iNetwork, root_k, span);
		
		return inst_output;
	}
	
	private void decode(IELinearNetwork network, int node_k, UnlabeledTextSpan span){
		
		int N = span.length();
		
		long node = network.getNode(node_k);
		int[] array = NetworkIDMapper.toHybridNodeArray(node);
		int hybridType = array[4];
		if(hybridType == IELinearConfig.NODE_TYPE.EXACT_START_TAG.ordinal())
		{
			int[] nodes_child_k = network.getMaxPath(node_k);
			
			if(nodes_child_k.length!=1){
				throw new RuntimeException("This is strange..."+nodes_child_k.length);
			}
			
			int node_child_k = nodes_child_k[0];
			long node_child = network.getNode(node_child_k);
			int[] array_child = NetworkIDMapper.toHybridNodeArray(node_child);
			int hybridType_child = array_child[4];
			if(hybridType_child == IELinearConfig.NODE_TYPE.TERMINATE.ordinal()){
				//DO NOTHING..
			}
			
			else if(hybridType_child == IELinearConfig.NODE_TYPE.INCOMPLETE_START_TAG.ordinal()){
				int start_index = N - array[0];
				int tag_id = array[3]-1;
				SemanticTag tag = this._tags[tag_id-1];
				if(tag_id!=tag.getId()){
					throw new RuntimeException(tag.getId()+"!="+tag_id);
				}
				
				ArrayList<Integer> end_indices = this.findBoundaries(N, network, node_child_k);
				for(int end_index : end_indices){
//					System.err.println(start_index+","+end_index+"\t"+tag);
//					span.label_predict(start_index, end_index, new Mention(start_index, end_index, start_index, end_index, tag));
					span.label_predict(start_index, end_index, new Mention(start_index, end_index, end_index-1, end_index, tag));
				}
			}
			
			else{
				throw new RuntimeException("This is strange..."+hybridType_child);
			}
			
		}
		
		int[] nodes_child_k = network.getMaxPath(node_k);
		
		for(int node_child_k : nodes_child_k){
			this.decode(network, node_child_k, span);
		}
	}
	
	private ArrayList<Integer> findBoundaries(int N, IELinearNetwork network, int node_k){
		ArrayList<Integer> results = new ArrayList<Integer>();
		this.findBoundariesHelper(N, results, network, node_k);
		return results;
	}
	
	private void findBoundariesHelper(int N, ArrayList<Integer> results, IELinearNetwork network, int node_k){
		long node = network.getNode(node_k);
		int[] array = NetworkIDMapper.toHybridNodeArray(node);
//		int hybridType = array[4];
//		System.err.println("<<<"+hybridType+"\t"+IELinearConfig.NODE_TYPE.INCOMPLETE_START_TAG.ordinal()+"\t"+array[0]);
		int[] nodes_child_k = network.getMaxPath(node_k);
		
		for(int node_child_k : nodes_child_k){
			long node_child = network.getNode(node_child_k);
			int[] array_child = NetworkIDMapper.toHybridNodeArray(node_child);
			int hybridType_child = array_child[4];
			if(hybridType_child == IELinearConfig.NODE_TYPE.TERMINATE.ordinal()){
				int end_index = N - array[0] + 1;
//				System.err.println(">>>\t"+hybridType+"\t"+IELinearConfig.NODE_TYPE.INCOMPLETE_START_TAG.ordinal()+"\t"+array[0]+"\t"+end_index+"\t"+N);
				results.add(end_index);
			} else {
				this.findBoundariesHelper(N, results, network, node_child_k);
			}
		}
	}
	
	
	private long toNode_after_start(int bIndex){
		int srcHeight = - bIndex;
		int srcWidth = IELinearConfig._MAX_SENT_LENGTH;
		int tgtHeight = srcHeight;
		int tgtWidth = this._tags.length + 3;
		int hybridType = IELinearConfig.NODE_TYPE.AFTER_START.ordinal();
		return NetworkIDMapper.toHybridNodeID(new int[]{srcHeight, srcWidth, tgtHeight, tgtWidth, hybridType});
	}
	
	private long toNode_exact_start(int bIndex){
		int srcHeight = - bIndex;
		int srcWidth = IELinearConfig._MAX_SENT_LENGTH;
		int tgtHeight = srcHeight;
		int tgtWidth = this._tags.length + 3;
		int hybridType = IELinearConfig.NODE_TYPE.EXACT_START.ordinal();
		return NetworkIDMapper.toHybridNodeID(new int[]{srcHeight, srcWidth, tgtHeight, tgtWidth, hybridType});
	}
	
	private long toNode_exact_start_tag(int bIndex, SemanticTag tag){
		int srcHeight = - bIndex;
		int srcWidth = IELinearConfig._MAX_SENT_LENGTH;
		int tgtHeight = srcHeight;
		int tgtWidth = tag.getId() + 1;
		int hybridType = IELinearConfig.NODE_TYPE.EXACT_START_TAG.ordinal();
		return NetworkIDMapper.toHybridNodeID(new int[]{srcHeight, srcWidth, tgtHeight, tgtWidth, hybridType});
	}

	private long toNode_incomplete_start_tag(int bIndex, SemanticTag tag){
		int srcHeight = - bIndex;
		int srcWidth = IELinearConfig._MAX_SENT_LENGTH;
		int tgtHeight = srcHeight;
		int tgtWidth = tag.getId() + 1;
		int hybridType = IELinearConfig.NODE_TYPE.INCOMPLETE_START_TAG.ordinal();
		return NetworkIDMapper.toHybridNodeID(new int[]{srcHeight, srcWidth, tgtHeight, tgtWidth, hybridType});
	}
	
//	private long toNode_incomplete_start_tag_numerator(int bIndex, SemanticTag tag){
//		int srcHeight = - bIndex;
//		int srcWidth = IELinearConfig._MAX_SENT_LENGTH;
//		int tgtHeight = srcHeight;
//		int tgtWidth = tag.getId() + 1;
//		int hybridType = IELinearConfig.NODE_TYPE.INCOMPLETE_START_TAG.ordinal();
//		return NetworkIDMapper.toHybridNodeID(new int[]{srcHeight, srcWidth, tgtHeight, tgtWidth, hybridType});
//	}
	
	private long toNode_root(int bIndex){
		int srcHeight = - bIndex;
		int srcWidth = IELinearConfig._MAX_SENT_LENGTH;
		int tgtHeight = srcHeight;
		int tgtWidth = this._tags.length + 4;
		int hybridType = IELinearConfig.NODE_TYPE.ROOT.ordinal();
		return NetworkIDMapper.toHybridNodeID(new int[]{srcHeight, srcWidth, tgtHeight, tgtWidth, hybridType});
	}
	
	private long toNode_terminate(){
		int srcHeight = 0;
		int srcWidth = 0;
		int tgtHeight = 0;
		int tgtWidth = 0;
		int hybridType = IELinearConfig.NODE_TYPE.TERMINATE.ordinal();
		return NetworkIDMapper.toHybridNodeID(new int[]{srcHeight, srcWidth, tgtHeight, tgtWidth, hybridType});
	}
	
}

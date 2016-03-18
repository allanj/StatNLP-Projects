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
package com.statnlp.ie.flatsemi.zeroth;

import java.util.ArrayList;
import java.util.Collections;

import com.statnlp.commons.types.Segment;
import com.statnlp.commons.types.TextSpan;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.hybridnetworks.GlobalNetworkParam.EXP_MODE;
import com.statnlp.ie.types.LabeledTextSpan;
import com.statnlp.ie.types.MentionType;
import com.statnlp.ie.types.SemanticTag;

public class FlatSemiZerothBuilder {
	
	private int _N;
	private SemanticTag[] _tags; //all possible tags, excluding start and end.
	private FlatSemiZerothFeatureManager _fm;
	private boolean _isLabeled = false;
	private int[] _roots;
	
	public FlatSemiZerothBuilder(FlatSemiZerothFeatureManager fm, SemanticTag[] tags, int maxLen){
		this._fm = fm;
		this._tags = new SemanticTag[tags.length+1];
		for(int k = 0; k<tags.length; k++){
			this._tags[k] = tags[k];
		}
		this._tags[tags.length] = MentionType._START_TYPE;
		this._N = FlatSemiZerothConfig._MAX_SENT_LENGTH;
		FlatSemiZerothConfig._MAX_MENTION_LENGTH = maxLen;
	}
	
	public FlatSemiZerothNetwork build(FlatSemiZerothInstance inst){
		
		this._isLabeled = inst.hasOutput();
		if(!this._isLabeled)
		{
//			boolean mode_a = true;
//			
//			if(mode_a){
//				if(!this._built){
//					this.build_unlabeled();
//					this._built = true;
//				}
//				FlatSemiNetwork network = new FlatSemiNetwork(inst, this._fm, this._nodes, this._children, this.getRootPositionForLen(inst.length())+1);
//				return network;
//			}
//			else 
			{
				return this.build_unlabeled(inst);
			}
////			System.err.println(network.countNodes()+" nodes");
////			System.exit(1);
		} else {
			return this.build_labeled(inst);
		}
		
	}
	
	public FlatSemiZerothNetwork build_labeled(FlatSemiZerothInstance inst){
		FlatSemiZerothNetwork network = new FlatSemiZerothNetwork(inst, this._fm, EXP_MODE.LOCAL);
		this.build_labeled(network, inst);
		network.finalizeNetwork();
//		int size = 1 + (this._M * 2 + 3) * inst.length();
//		System.err.println("There are "+network.countNodes()+" nodes. maximum: "+size);
		return network;
	}
	
	public FlatSemiZerothNetwork build_unlabeled(FlatSemiZerothInstance inst){
		this._N = inst.length();
		FlatSemiZerothNetwork network = new FlatSemiZerothNetwork(inst, this._fm, EXP_MODE.GLOBAL);
		this.build(network);
		network.finalizeNetwork();
		return network;
	}
	
//	public void build_unlabeled(){
//		FlatSemiNetwork network = new FlatSemiNetwork(null, null);
//		this.build(network);
//		network.finalizeNetwork();
//		this._nodes = network.getAllNodes();
//		this._children = network.getAllChildren();
//		int size = 1 + (this._M * 2 + 3) * this._N;
//		if(size!=this._nodes.length){
//			throw new RuntimeException("The size is "+this._nodes.length+" which is different from expected "+size);
//		}
//		this._roots = new int[this._N];
//		for(int k = 1; k<=this._N; k++){
//			this._roots[k-1] = 1 + (this._M * 2 + 3) * k - 1;
////			System.err.println(this._roots[k-1]+":"+NetworkIDMapper.viewHybridNode_ie(this._nodes[this._roots[k-1]], this._N, this._M));
//		}
////		System.err.println("size="+this._nodes.length+"\t"+size);
////		System.err.println(":"+NetworkIDMapper.viewHybridNode_ie(this._nodes[this._nodes.length-1], this._N, this._M));
//	}
	
	public int getRootPositionForLen(int len){
		return this._roots[len-1];
	}
	
	public void build_labeled(FlatSemiZerothNetwork network, FlatSemiZerothInstance inst){
		
		TextSpan span = inst.getSpan();
		
		LabeledTextSpan lspan = (LabeledTextSpan)span;
		
		ArrayList<Segment> segments = null;
		
		if(this._isLabeled){
			segments = ((LabeledTextSpan)span).getAllSegments();
		}
		
		//keep only the top-level segments
		for(int k = 0; k<segments.size(); k++){
			Segment segment1 = segments.get(k);
			for(int i = k+1; i<segments.size(); i++){
				Segment segment2 = segments.get(i);
				if(segment1.overlapsWith(segment2)){
					if(segment1.length()<=segment2.length()){
						segments.remove(k);
						k--;
						break;
					} else if(segment1.length()>segment2.length()){
						segments.remove(i);
						i--;
					}
				}
			}
		}
		
		for(int i = 0; i<segments.size(); i++){
			Segment segment = segments.get(i);
			if(segment.length()>FlatSemiZerothConfig._MAX_MENTION_LENGTH){
				segments.remove(i);
				i--;
			}
		}
		
		int bIndex = 0;
		
		int size = segments.size();
		for(int i = 0; i<size; i++){
			Segment segment = segments.get(i);
			int eIndex = segment.getBIndex();
			for(int index = bIndex; index<eIndex; index++){
				Segment seg = new Segment(index, index+1);
				segments.add(seg);
			}
			bIndex = segment.getEIndex();
		}
		int eIndex = span.length();
		for(int index = bIndex; index<eIndex; index++){
			Segment seg = new Segment(index, index+1);
			segments.add(seg);
		}
		Collections.sort(segments);
		
//		System.err.println(segments);
//		System.exit(1);
		
		{
			long node_root = this.toNode_root(0);
			network.addNode(node_root);
		}
		
		for(int i = 0; i<segments.size(); i++){
			Segment segment = segments.get(i);
			bIndex = segment.getBIndex();
			eIndex = segment.getEIndex();
			SemanticTag tag;
			if(lspan.getLabels(bIndex, eIndex)==null){
				tag = MentionType._START_TYPE;
			} else {
				tag = (SemanticTag)lspan.getLabels(bIndex, eIndex).get(0).getSemanticTag();
			}
			
			long node_exact = this.toNode_exact(bIndex, eIndex, tag);
			network.addNode(node_exact);
			
			long node_before = this.toNode_before(eIndex, tag);
			network.addNode(node_before);
			network.addEdge(node_before, new long[]{node_exact});
			
			long node_root = this.toNode_root(eIndex);
			network.addNode(node_root);
			network.addEdge(node_root, new long[]{node_before});
			
			long node_root_prev = this.toNode_root(bIndex);
			network.addEdge(node_exact, new long[]{node_root_prev});
		}
	}
	
	private SemanticTag[] getTags(int eIndex){
		if(eIndex==0){
			return new SemanticTag[]{};
		} else {
			return this._tags;
		}
	}
	
	public void build(FlatSemiZerothNetwork network){
		
		for(int eIndex = 0; eIndex<= this._N; eIndex++){
			long node_root = this.toNode_root(eIndex);
			network.addNode(node_root);
			
			SemanticTag[] tags = this.getTags(eIndex);
			
			for(int i = 0; i<tags.length; i++){
				SemanticTag tag = tags[i];
				long node_before = this.toNode_before(eIndex, tag);
				network.addNode(node_before);
				network.addEdge(node_root, new long[]{node_before});
				
				int maxLen = 1;
				if(!tag.equals(MentionType._START_TYPE)){
					maxLen = FlatSemiZerothConfig._MAX_MENTION_LENGTH;
				}
				if(eIndex<maxLen){
					maxLen = eIndex;
				}
				
				for(int len = 1; len<= maxLen; len++){
					int bIndex = eIndex - len;
					long node_exact = this.toNode_exact(bIndex, eIndex, tag);
					if(tag.equals(MentionType._START_TYPE) && eIndex-bIndex!=1){
						continue;
					}
					network.addNode(node_exact);
					network.addEdge(node_before, new long[]{node_exact});
					
					long node_root_prev = this.toNode_root(bIndex);
					network.addEdge(node_exact, new long[]{node_root_prev});
				}
			}
		}
		
	}
	
	private long toNode_before(int eIndex, SemanticTag tag){
		int srcHeight = eIndex;
		int srcWidth = eIndex;
		int tgtHeight = srcHeight;
		int tgtWidth = tag.getId() + 1;
		int hybridType = FlatSemiZerothConfig.NODE_TYPE.BEFORE.ordinal();
		return NetworkIDMapper.toHybridNodeID(new int[]{srcHeight, srcWidth, tgtHeight, tgtWidth, hybridType});
	}
	
	private long toNode_exact(int bIndex, int eIndex, SemanticTag tag){
		int srcHeight = eIndex;
		int srcWidth = eIndex-bIndex;
		int tgtHeight = srcHeight;
		int tgtWidth = tag.getId() + 1;
		int hybridType = FlatSemiZerothConfig.NODE_TYPE.EXACT.ordinal();
		return NetworkIDMapper.toHybridNodeID(new int[]{srcHeight, srcWidth, tgtHeight, tgtWidth, hybridType});
	}
	
	private long toNode_root(int eIndex){
		int srcHeight = eIndex;
		int srcWidth = eIndex;
		int tgtHeight = srcHeight;
		int tgtWidth = this._tags.length+4;
		int hybridType = FlatSemiZerothConfig.NODE_TYPE.ROOT.ordinal();
		return NetworkIDMapper.toHybridNodeID(new int[]{srcHeight, srcWidth, tgtHeight, tgtWidth, hybridType});
	}
	
}

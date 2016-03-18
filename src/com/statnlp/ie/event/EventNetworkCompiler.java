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
package com.statnlp.ie.event;

import java.util.ArrayList;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Segment;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.ie.types.Mention;
import com.statnlp.ie.types.MentionType;
import com.statnlp.ie.types.SemanticTag;
import com.statnlp.sp.SemanticType;

/**
 * @author wei_lu
 *
 */
public class EventNetworkCompiler extends NetworkCompiler{
	
	private static final long serialVersionUID = 4568405564335652949L;
	
	private Event _event;
	private EventRole[] _roles;
	private int _numRolesInNonTerminal = 2;
	private EventTemplate _template;
	
	public EventNetworkCompiler(EventTemplate template){
		this._template = template;
		this._event = template.getEvent();
		this._roles = this._template.getAllRoles();
	}
	
	public static void main(String args[]){
		
		int maxLen = 20;
		int k = 0;
		for(int L = 1; L<=maxLen; L++){
			System.err.println("L="+L);
			for(int bIndex= 0; bIndex<=maxLen-L; bIndex++){
				if(bIndex!=0)
					continue;
				
				int eIndex = bIndex+L;
				for(int cIndex = bIndex+1; cIndex<eIndex; cIndex++){
					for(int dIndex=cIndex; dIndex<eIndex; dIndex++){
						if(dIndex!=eIndex-1)
							continue;
						
						System.err.println("["+bIndex+","+cIndex+")["+dIndex+","+eIndex+")");
						k++;
					}
				}
			}
		}
		System.err.println(k);
		
	}
	
	@Override
	public EventNetwork compile(int networkId, Instance inst, LocalNetworkParam param) {
		
		if(inst.isLabeled()){
			return this.compile_labeled(networkId, (EventInstance)inst, param);
		} else {
			return this.compile_unlabeled(networkId, (EventInstance)inst, param);
		}
		
	}
	
	private EventNetwork compile_labeled(int networkId, EventInstance inst, LocalNetworkParam param){
		
		EventNetwork network = new EventNetwork(networkId, inst, param);
		
		EventLabeledTextSpan eSpan = inst.getOutput();
		ArrayList<Segment> allSegs = eSpan.getAllSegments();
		MentionLabeledTextSpan mSpan = inst.getMentionSpan();
		
		int L = allSegs.size();
		
		for(int bIndex= 0; bIndex<=L-1; bIndex++){
			Segment seg = allSegs.get(bIndex);
			
			ArrayList<Mention> mentions = mSpan.getLabels(seg.getBIndex(), seg.getEIndex());
			long[] nodes_mention = new long[mentions.size()];
			for(int k = 0; k<nodes_mention.length; k++){
				Mention mention = mentions.get(k);
				long node_mention = this.toNode_mention(seg.getBIndex(), seg.getEIndex(), (MentionType)mention.getSemanticTag());
				network.addNode(node_mention);
				nodes_mention[k] = node_mention;
			}
			
			ArrayList<Mention> roles = eSpan.getLabels(seg.getBIndex(), seg.getEIndex());
			for(Mention role : roles){
				long node_role = this.toNode_role(seg.getBIndex(), seg.getEIndex(), new EventRole[]{(EventRole)role.getSemanticTag()});
				network.addNode(node_role);
				for(long node_mention : nodes_mention){
					network.addEdge(node_role, new long[]{node_mention});
				}
			}
		}
		
		int bIndex = allSegs.get(0).getBIndex();
		
		for(int eIndex = 1; eIndex<=L-1; eIndex++){
			System.err.println("L="+eIndex);
			Segment seg_R = allSegs.get(eIndex);
			for(int cIndex = 0; cIndex<=eIndex-1; cIndex++){
				Segment seg_L = allSegs.get(cIndex);
				
				for(EventRole[] roles_L : this.getRolesToTheLeft(seg_L)){
					long node_L = this.toNode_role(bIndex, seg_L.getEIndex(), roles_L);
					
					if(!network.contains(node_L)){
						continue;
					}
					
					ArrayList<Mention> roles = eSpan.getLabels(seg_R.getBIndex(), seg_R.getEIndex());
					for(Mention role : roles){
						long node_R = this.toNode_role(seg_R.getBIndex(), seg_R.getEIndex(), new EventRole[]{(EventRole)role.getSemanticTag()});
						long node_curr = this.toNode_role(bIndex, seg_R.getEIndex(), this.toRoles(roles_L, (EventRole)role.getSemanticTag()));
						if(network.contains(node_R)){
							network.addNode(node_curr);
							network.addEdge(node_curr, new long[]{node_L, node_R});
						}
					}
					
				}
				
			}
		}
		
		return network;
		
	}
	
	private EventRole[] toRoles(EventRole[] prev, EventRole role){
		int size = prev.length+1;
		if(size > this._numRolesInNonTerminal){
			size = this._numRolesInNonTerminal;
		}
		EventRole[] roles = new EventRole[size];
		roles[size-1] = role;
		for(int k = size-2; k>=0; k--){
			roles[k] = prev[k-size+2+prev.length-1];
		}
		return roles;
	}
	
	private EventRole[][] getRolesToTheLeft(Segment seg){
		return null;
	}
	
//	private EventRole[] getPossibleRoles(MentionType type){
//		return null;
//	}
	
	private EventNetwork compile_unlabeled(int networkId, EventInstance inst, LocalNetworkParam param){

		EventNetwork network = new EventNetwork(networkId, inst, param);
		
//		EventLabeledTextSpan eSpan = inst.getOutput();
		MentionLabeledTextSpan mSpan = inst.getMentionSpan();
		ArrayList<Segment> allSegs = mSpan.getAllSegments();
		
		int L = allSegs.size();
		
		for(int bIndex= 0; bIndex<=L-1; bIndex++){
			Segment seg = allSegs.get(bIndex);
			
			ArrayList<Mention> mentions = mSpan.getLabels(seg.getBIndex(), seg.getEIndex());
			for(Mention mention : mentions){
				MentionType type = (MentionType)mention.getSemanticTag();
				long node_mention = this.toNode_mention(seg.getBIndex(), seg.getEIndex(), type);
				network.addNode(node_mention);
				
				for(EventRole role : this._roles){
					if(role.compatibleWith(type)){
						long node_role = this.toNode_role(seg.getBIndex(), seg.getEIndex(), new EventRole[]{role});
						network.addNode(node_role);
						network.addEdge(node_role, new long[]{node_mention});
					}
				}
			}
		}
		
		int bIndex = allSegs.get(0).getBIndex();
		
		for(int eIndex = 1; eIndex<=L-1; eIndex++){
			System.err.println("L="+eIndex);
			Segment seg_R = allSegs.get(eIndex);
			for(int cIndex = 0; cIndex<=eIndex-1; cIndex++){
				Segment seg_L = allSegs.get(cIndex);
				for(EventRole[] roles_L : this.getRolesToTheLeft(seg_L)){
					long node_L = this.toNode_role(bIndex, seg_L.getEIndex(), roles_L);
					if(!network.contains(node_L))
						continue;
					
					ArrayList<Mention> mentions = mSpan.getLabels(seg_R.getBIndex(), seg_R.getEIndex());
					for(Mention mention : mentions){
						MentionType type = (MentionType)mention.getSemanticTag();
						
						for(EventRole role : this._roles){
							if(role.compatibleWith(type)){
								long node_R = this.toNode_role(seg_R.getBIndex(), seg_R.getEIndex(), new EventRole[]{role});
								long node_curr = this.toNode_role(bIndex, seg_R.getEIndex(), this.toRoles(roles_L, role));
								if(network.contains(node_R)){
									network.addNode(node_curr);
									network.addEdge(node_curr, new long[]{node_L, node_R});
								}
							}
						}
					}
				}
			}
		}
		
		return network;
	}
	
	@Override
	public Instance decompile(Network network) {
		// TODO Auto-generated method stub
		return null;
	}

	private long toNode_role(int bIndex, int eIndex, EventRole role, int hIndex){
		int[] v = new int[this._numRolesInNonTerminal+3];
		v[0] = eIndex-bIndex;
		v[1] = bIndex;
		v[2] = 1;
		v[3] = hIndex;
		v[4] = role.getId() + 1;
		for(int k = 5; k<v.length; k++)
			v[k] = 0;
		return NetworkIDMapper.toHybridNodeID(v);
	}
	
	private long toNode_role(int bIndex, int eIndex, EventRole[] roles){
		int[] v = new int[this._numRolesInNonTerminal+3];
		v[0] = eIndex-bIndex;
		v[1] = bIndex;
		v[2] = roles.length;
		for(int k = 0; k<roles.length; k++)
			v[k+3] = roles[k].getId()+1;
		return NetworkIDMapper.toHybridNodeID(v);
	}
	
	private long toNode_mention(int bIndex, int eIndex, MentionType type){
		int[] v = new int[this._numRolesInNonTerminal+3];
		v[0] = eIndex-bIndex;
		v[1] = bIndex;
		v[2] = 0;
		v[3] = type.getId()+1;
		for(int k = 4; k<v.length; k++)
			v[k] = 0;
		return NetworkIDMapper.toHybridNodeID(v);
	}
	
}
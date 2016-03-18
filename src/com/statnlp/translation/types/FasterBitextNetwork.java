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
package com.statnlp.translation.types;

import com.statnlp.commons.BitextInstance;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.hybridnetworks.TableLookupNetwork;
import com.statnlp.hybridnetworks.GlobalNetworkParam.EXP_MODE;

public class FasterBitextNetwork extends BitextNetwork{
	
	private int _num_nodes;
	private int _max_src_eIndex;
	private int _max_tgt_eIndex;
	private double[][] _membership;
	
	public FasterBitextNetwork(BitextInstance inst, FeatureManager fm, long[] nodes, int[][][] children, int num_nodes, EXP_MODE exp_mode) {
		super(inst, fm, nodes, children, exp_mode);
		this._num_nodes = num_nodes;
		int[] ids = NetworkIDMapper.toHybridNodeArray(this.getRoot());
		this._max_src_eIndex = ids[0]+ids[1];
		this._max_tgt_eIndex = ids[2]+ids[3];
	}
	
	public FasterBitextNetwork(BitextInstance inst, FeatureManager fm, long[] nodes, int[][][] children, int num_nodes, double[][] membership, EXP_MODE exp_mode) {
		super(inst, fm, nodes, children, exp_mode);
		this._num_nodes = num_nodes;
		int[] ids = NetworkIDMapper.toHybridNodeArray(this.getRoot());
		this._max_src_eIndex = ids[0]+ids[1];
		this._max_tgt_eIndex = ids[2]+ids[3];
		this._membership = membership;
	}
	
	//check if node1 contains node2
	public boolean does_not_cover(long node2){
		
		int[] ids_child = NetworkIDMapper.toHybridNodeArray(node2);
		
		int srcEIndex_child = ids_child[0]+ids_child[1];
		int tgtEIndex_child = ids_child[2]+ids_child[3];
		
		if(srcEIndex_child > _max_src_eIndex || tgtEIndex_child > _max_tgt_eIndex)
			return true;
		
		return false;
	}
	
	@Override
	public int countNodes(){
		return this._num_nodes;
	}

	public FasterBitextNetwork(BitextInstance inst, FeatureManager fm, EXP_MODE exp_mode) {
		super(inst, fm, exp_mode);
		throw new RuntimeException("not allowed.");
	}

	//remove the node k from the network.
	public void remove(int k){
		//DO NOTHING..
	}
	
	//check if the node k is removed from the network.
	public boolean isRemoved(int k){
		long node = this.getNode(k);
		if(this.does_not_cover(node)){
			return true;
		}
		
		/*
		if(this._membership == null){
			return false;
		}
		
		int[] ids = NetworkIDMapper.toHybridNodeArray(node);
		int srcBIndex = ids[1];
		int srcEIndex = ids[1]+ids[0];
		int tgtBIndex = ids[3];
		int tgtEIndex = ids[3]+ids[2];
		int type = ids[4];
		
		if(type==BitextNetworkConstructor.TYPE.I00.ordinal()){
			if(srcEIndex==srcBIndex+1){
				for(int tgtIndex = tgtBIndex; tgtIndex<tgtEIndex; tgtIndex++){
					if(this._membership[tgtIndex][srcBIndex]<1E-30){
//						System.err.println("okkk"+"\t"+this._membership[tgtIndex][srcBIndex]);
						return true;
					}
				}
				return false;
			}
			return false;
		}
		*/
		
		return false;
	}
	
}
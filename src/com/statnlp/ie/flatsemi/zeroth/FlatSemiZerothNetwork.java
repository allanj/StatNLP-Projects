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

import java.util.Arrays;

import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.hybridnetworks.GlobalNetworkParam.EXP_MODE;
import com.statnlp.hybridnetworks.TableLookupNetwork;

public class FlatSemiZerothNetwork extends TableLookupNetwork{

	private int _numNodes;
	
	public FlatSemiZerothNetwork(FlatSemiZerothInstance inst, FlatSemiZerothFeatureManager fm, EXP_MODE exp_mode) {
		super(inst, fm, exp_mode);
	}
	
	public FlatSemiZerothNetwork(FlatSemiZerothInstance inst, FlatSemiZerothFeatureManager fm, long[] nodes, int[][][] children, int numNodes, EXP_MODE exp_mode) {
		super(inst, fm, nodes, children, exp_mode);
		this._numNodes = numNodes;
	}
	
	public void finalizeNetwork(){
		super.finalizeNetwork();
		this._numNodes = super.countNodes();
	}
	
	@Override
	public int countNodes(){
		return this._numNodes;
	}

	//remove the node k from the network.
//	@Override
	public void remove(int k){
		//DO NOTHING..
	}
	
	//check if the node k is removed from the network.
//	@Override
	public boolean isRemoved(int k){
		return false;
	}
	
	private long[] toNodes(int[] ks){
		long[] nodes = new long[ks.length];
		for(int i = 0; i<nodes.length; i++){
			nodes[i] = this.get(ks[i]);
		}
		return nodes;
	}
	
}
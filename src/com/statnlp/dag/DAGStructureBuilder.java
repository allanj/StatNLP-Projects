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
package com.statnlp.dag;

import com.statnlp.hybridnetworks.HyperGraph;
import com.statnlp.hybridnetworks.TableLookupNetwork;

/**
 * @author wei_lu
 *
 */
public abstract class DAGStructureBuilder {
	
	protected HGrammar _g;
	
	public DAGStructureBuilder(HGrammar g){
		this._g = g;
	}
	
	public void compile(TableLookupNetwork network, HyperGraph src, HyperGraph tgt){
		for(int src_k = 0; src_k<src.countNodes(); src_k++){
			for(int tgt_k = 0; tgt_k<tgt.countNodes(); tgt_k++){
				this.compile(network, src, src_k, tgt, tgt_k);
			}
		}
		network.finalizeNetwork();
	}
	
	protected void compile(TableLookupNetwork network, HyperGraph src, int src_k, HyperGraph tgt, int tgt_k){
		
		int[][] src_children_k = src.getChildren(src_k);
		int[][] tgt_children_k = tgt.getChildren(tgt_k);
		
		for(int[] src_child_k : src_children_k){
			for(int[] tgt_child_k : tgt_children_k){
				this.createNewNode(network, src, src_k, src_child_k, tgt_k, tgt_child_k);
			}
		}
	}
	
	protected abstract void createNewNode(TableLookupNetwork network, HyperGraph src, int src_k, int[] src_child_k, int tgt_k, int[] tgt_child_k);
	
	public DAGStructure decompile(DAGStructure struct){
		return null;
	}
	
}

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

/**
 * @author wei_lu
 *
 */
public class SentenceDAG implements HyperGraph{
	
	private static final long serialVersionUID = 1843891658919808669L;
	
	private int _n;
	
	public SentenceDAG(int n){
		this._n = n;
	}
	
	@Override
	public int countNodes() {
		return (1+this._n)*this._n/2;
	}
	
	@Override
	public long getNode(int k) {
		//TODO
		return -1;
	}
	
	@Override
	public int[][] getChildren(int k) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isRemoved(int k) {
		return false;
	}

	@Deprecated
	@Override
	public void remove(int k) {
		//DO NOTHING.
	}
	
	@Override
	public boolean isRoot(int k) {
		return k == this.countNodes()-1;
	}
	
	@Override
	public boolean isLeaf(int k) {
		return k<this._n;
	}
	
	@Deprecated
	@Override
	public boolean contains(long node) {
		throw new RuntimeException("This method is invalid.");
	}

	@Override
	public int[] getNodeArray(int k) {
		// TODO Auto-generated method stub
		return null;
	}
	
}

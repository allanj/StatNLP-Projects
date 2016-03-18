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

import java.io.Serializable;

/**
 * @author wei_lu
 *
 */
public abstract class DAGStructure implements Serializable{
	
	private static final long serialVersionUID = -4295413765685590687L;
	
	public long getRoot(){
		return this.getNode(this.countNodes()-1);
	}
	
	//count the total number of nodes.
	public abstract int countNodes();
	
	//get the node with index k.
	protected abstract long getNode(int k);
	
	//get the children for node with index k.
	protected abstract int[][] getChildren(int k);
	
}
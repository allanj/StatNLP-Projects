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
import java.util.Arrays;

/**
 * @author wei_lu
 *
 */
public class DAGNode implements Serializable, Comparable<DAGNode>{
	
	private static final long serialVersionUID = 5367880800192650454L;
	
	protected int[] _ids;
	protected DAGNode[][] _children;
	
	public DAGNode(int[] ids){
		this._ids = ids;
	}
	
	public int[] getIds(){
		return this._ids;
	}
	
	public void setChildren(DAGNode[][] children){
		for(DAGNode[] child : children){
			for(DAGNode c : child){
				if(this.compareTo(c)<=0){
					throw new RuntimeException("Invalid parent-child relation!");
				}
			}
		}
		this._children = children;
	}
	
	public DAGNode[][] getChildren(){
		return this._children;
	}
	
	@Override
	public int compareTo(DAGNode node) {
		if(this._ids.length!=node._ids.length){
			throw new RuntimeException("Invalid number of ids:"+this._ids.length+"!="+node._ids.length);
		}
		for(int k = 0; k<this._ids.length; k++){
			if(this._ids[k]!=node._ids[k])
				return this._ids[k] - node._ids[k];
		}
		return 0;
	}
	
	@Override
	public String toString(){
		return "DAGNode:"+Arrays.toString(this._ids);
	}
	
}

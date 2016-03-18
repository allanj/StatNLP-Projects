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
public class ID implements Serializable, Comparable<ID>{
	
	private static final long serialVersionUID = 1146223576273785962L;
	private int[] _ids;
	
	public ID(int[] ids){
		this._ids = ids;
	}
	
	public int[] getIds(){
		return this._ids;
	}
	
	@Override
	public int hashCode(){
		return Arrays.hashCode(this._ids) + 7;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof ID)
			return Arrays.equals(this._ids, ((ID)o)._ids);
		return false;
	}
	
	@Override
	public int compareTo(ID id) {
		if(this.size()!=id.size()){
			throw new RuntimeException("The size of these two IDs do not match:"+this.size()+"!="+id.size());
		}
		for(int k = 0; k<this.size(); k++){
			if(this._ids[k]!=id._ids[k])
				return this._ids[k] - id._ids[k];
		}
		return 0;
	}
	
	public int size(){
		return this._ids.length;
	}
	
	@Override
	public String toString(){
		return "ID:"+Arrays.toString(this._ids);
	}
	
}

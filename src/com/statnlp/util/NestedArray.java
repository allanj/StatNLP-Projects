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
package com.statnlp.util;

import java.io.Serializable;

public class NestedArray<T> implements Serializable{
	
	private static final long serialVersionUID = -740130866255676332L;
	
	private T[] _curr;
	private NestedArray<T> _next;
	
	public NestedArray(T[] curr){
		this._curr = curr;
		this._next = null;
	}
	
	public NestedArray(T[] curr, NestedArray<T> next){
		this._curr = curr;
		this._next = next;
	}
	
	public T[] getCurrent(){
		return this._curr;
	}
	
	public NestedArray<T> getNext(){
		return this._next;
	}
	
	public int size(){
		if(this._next==null) return this._curr.length;
		return this._curr.length + this._next.size();
	}
	
}

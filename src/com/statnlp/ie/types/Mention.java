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
package com.statnlp.ie.types;

import java.io.Serializable;

import com.statnlp.commons.types.Segment;

//note that the mention only makes sense in the context of a text span.

public class Mention implements Serializable, Comparable<Mention>{
	
	private static final long serialVersionUID = 7865497015173795033L;
	
	private Segment _seg;
	private Segment _head_seg;
	private SemanticTag _tag;
	
	public Mention(int bIndex, int eIndex, int head_bIndex, int head_eIndex, SemanticTag tag){
		this._seg = new Segment(bIndex, eIndex);
		this._head_seg = new Segment(head_bIndex, head_eIndex);
		this._tag = tag;
	}
	
	@Override
	public int compareTo(Mention m) {
		if(this._seg.compareTo(m._seg)!=0){
			return this._seg.compareTo(m._seg);
		}
		if(this._head_seg.compareTo(m._head_seg)!=0){
			return this._head_seg.compareTo(m._head_seg);
		}
		return 0;
	}
	
	@Override
	public int hashCode(){
		return (this._seg.hashCode() + 7) ^ (this._head_seg.hashCode() + 7) ^ (this._tag.hashCode() + 7);
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Mention){
			Mention m = (Mention)o;
			return this._seg.equals(m._seg) && this._head_seg.equals(m._head_seg) && this._tag.equals(m._tag);
		}
		return false;
	}
	
	public boolean spanMatches(Mention m){
		return this._seg.equals(m._seg) && this._tag.equals(m._tag);
	}
	
	public int length(){
		return this._seg.length();
	}
	
	public Segment getSegment(){
		return this._seg;
	}
	
	public Segment getHeadSegment(){
		return this._head_seg;
	}
	
	public SemanticTag getSemanticTag(){
		return this._tag;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(this._seg.toString());
		sb.append("|");
		sb.append(this._head_seg.toString());
		sb.append(":");
		sb.append(this._tag);
		return sb.toString();
	}

}
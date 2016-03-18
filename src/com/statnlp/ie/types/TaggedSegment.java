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

public class TaggedSegment extends Segment{
	
	private static final long serialVersionUID = 4928116872921854798L;
	
	private SemanticTag _tag;
	private int _id;
	
	public TaggedSegment(int bIndex, int eIndex, SemanticTag tag){
		super(bIndex, eIndex);
		this._tag = tag;
	}
	
	public int getId(){
		return this._id;
	}
	
	public void setId(int id){
		this._id = id;
	}
	
	public SemanticTag getTag(){
		return this._tag;
	}
	
	@Override
	public int hashCode(){
		return (super.hashCode() + 7) ^ (this._tag.hashCode() + 7);
	}
	
	@Override
	public boolean equals(Object o){
		TaggedSegment ts = (TaggedSegment)o;
		return super.equals(o) && this._tag.equals(ts._tag);
	}
	
	@Override
	public int compareTo(Segment seg) {
		if(super.compareTo(seg)!=0)
			return super.compareTo(seg);
		TaggedSegment ts = (TaggedSegment)seg;
		return -this._tag.compareTo(ts._tag);
	}
	
	@Override
	public String toString(){
		return super.toString()+"\t"+this._tag;
	}
	
}
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
import java.util.ArrayList;
import java.util.HashMap;

public class SemanticTagList implements Serializable{
	
	private static final long serialVersionUID = 1798850611272751697L;
	
	private static HashMap<SemanticTagList, SemanticTagList> _allSemanticTagLists = new HashMap<SemanticTagList, SemanticTagList>();
	private static ArrayList<SemanticTagList> _allSemanticTagLists_arr = new ArrayList<SemanticTagList>();
	
	private int _id = -1;
	private SemanticTag[] _types;
	private int _bIndex;
	private int _eIndex;
	
	private SemanticTagList _prefix;
	private SemanticTagList _suffix;
	
	public static void constructAllLists(SemanticTag[] tags){
		if(IEConfig._ORDER == 1){
			for(int k = 0; k<tags.length; k++){
				toSemanticTagList(new SemanticTag[]{tags[k]});
			}
		} else if(IEConfig._ORDER == 2){
			for(int k = 0; k<tags.length; k++){
				for(int i = 0; i<tags.length; i++){
					toSemanticTagList(new SemanticTag[]{tags[i], tags[k]});
				}
			}
		} else if(IEConfig._ORDER == 3){
			for(int k = 0; k<tags.length; k++){
				for(int i = 0; i<tags.length; i++){
					for(int j = 0; j<tags.length; j++){
						toSemanticTagList(new SemanticTag[]{tags[i], tags[k], tags[j]});
					}
				}
			}
		}
	}
	
	public static SemanticTagList getSemanticTagList(int id){
		return _allSemanticTagLists_arr.get(id);
	}
	
	public static int countSemanticTagLists(){
		return _allSemanticTagLists.size();
	}
	
	public static SemanticTagList toSemanticTagList(SemanticTag headType, SemanticTagList suffix){
		SemanticTag[] types = new SemanticTag[1+suffix.size()];
		types[0] = headType;
		for(int k = 0; k<suffix.size(); k++)
			types[k+1] = suffix.getSemanticTag(k);
		return toSemanticTagList(types);
	}
	
	public static SemanticTagList toSemanticTagList(SemanticTagList prefix, SemanticTag tailType){
		SemanticTag[] types = new SemanticTag[prefix.size()+1];
		for(int k = 0; k<prefix.size(); k++)
			types[k] = prefix.getSemanticTag(k);
		types[prefix.size()] = tailType;
		return toSemanticTagList(types);
	}
	
	public static SemanticTagList toSemanticTagList(SemanticTag[] types){
		return toSemanticTagList(types, 0, types.length);
	}
	
	public static SemanticTagList toSemanticTagList(SemanticTag[] types, int bIndex, int eIndex){
		SemanticTagList list = new SemanticTagList(types, bIndex, eIndex);
		if(_allSemanticTagLists.containsKey(list))
			return _allSemanticTagLists.get(list);
		list.setId(_allSemanticTagLists.size());
		_allSemanticTagLists.put(list, list);
		_allSemanticTagLists_arr.add(list);
		return list;
	}
	
	private SemanticTagList(SemanticTag[] types, int bIndex, int eIndex){
		this._types = types;
		this._bIndex = bIndex;
		this._eIndex = eIndex;
	}
	
	public SemanticTagList(SemanticTag[] types){
		this(types, 0, types.length);
	}
	
	public SemanticTag getSemanticTag(int pos){
		return this._types[pos+this._bIndex];
	}
	
	public void setId(int id){
		this._id = id;
	}
	
	public int getId(){
		if(this._id == -1)
			throw new RuntimeException("Gosh.. the id is "+this._id);
		return this._id;
	}

	public SemanticTagList getPrefix(){
		if(this._prefix!=null)
			return this._prefix;
		this._prefix = toSemanticTagList(this._types, this._bIndex, this._eIndex-1);
		return this._prefix;
	}

	public SemanticTagList getSuffix(){
		if(this._suffix!=null)
			return this._suffix;
		this._suffix = toSemanticTagList(this._types, this._bIndex+1, this._eIndex);
		return this._suffix;
	}
	
	public boolean matches(SemanticTagList list){
		return this.getSuffix().equals(list.getPrefix());
	}
	
	public int size(){
		return this._eIndex - this._bIndex;
	}
	
	@Override
	public int hashCode(){
		int code = this.size() + 7;
		for(int k = 0; k<this.size(); k++)
			code ^= this.getSemanticTag(k).hashCode() + 7;
		return code;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof SemanticTagList){
			SemanticTagList list = (SemanticTagList)o;
			if(this.size()!= list.size())
				return false;
			for(int k = 0; k<this.size(); k++){
				if(!this.getSemanticTag(k).equals(list.getSemanticTag(k)))
					return false;
			}
			return true;
		}
		return false;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(int k = 0; k<this.size(); k++){
			if(k!=0) sb.append(',');
			sb.append(this.getSemanticTag(k));
		}
		return sb.toString();
	}
	
}
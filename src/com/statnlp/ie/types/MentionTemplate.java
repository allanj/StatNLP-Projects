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

public class MentionTemplate implements Serializable{
	
	private static final long serialVersionUID = 2363255420549049068L;
	
	private MentionType[] _allTypes;
	private ArrayList<SemanticTag> _types_excluding_start_finish;
	private SemanticTag[] _types_excluding_start_finish_arr;
	
	public MentionTemplate(ArrayList<MentionType> allTypes){
		int index = -1;
		for(int k = 0; k<allTypes.size(); k++){
			MentionType type = allTypes.get(k);
			if(type.equals(MentionType._FINISH_TYPE)){
				index = k;
				break;
			}
		}
		allTypes.remove(index);
		allTypes.add(0, MentionType._FINISH_TYPE);
		
		for(int k = 0; k<allTypes.size(); k++){
			MentionType type = allTypes.get(k);
			if(type.equals(MentionType._START_TYPE)){
				index = k;
				break;
			}
		}
		allTypes.remove(index);
		allTypes.add(MentionType._START_TYPE);
		
		this._allTypes = new MentionType[allTypes.size()];
		for(int k = 0; k<allTypes.size(); k++){
			this._allTypes[k] = allTypes.get(k);
			this._allTypes[k].setId(k);
		}
	}
	
	private MentionType[][] _prevTypes;
	private MentionType[][] _nextTypes;

	public MentionType[] getSmallerTypes(MentionType type){
		if(this._prevTypes == null){
			this._prevTypes = new MentionType[this._allTypes.length][];
		}
		int id = type.getId();
		if(this._prevTypes[id] == null){
			this._prevTypes[id] = new MentionType[id];
			for(int k = 0; k <= id-1; k++){
				this._prevTypes[id][k] = this._allTypes[k];
			}
		}
		for(int k = 0; k<this._prevTypes[id].length; k++){
			if(this._prevTypes[id][k].getId() >= type.getId()){
				throw new RuntimeException("Error: "+this._prevTypes[id][k].getId()+">="+type.getId());
			}
		}
		return this._prevTypes[id];
	}

	public MentionType[] getLargerTypes(MentionType type){
		if(this._nextTypes == null){
			this._nextTypes = new MentionType[this._allTypes.length][];
		}
		int id = type.getId();
		if(this._nextTypes[id] == null){
			this._nextTypes[id] = new MentionType[this._allTypes.length - id-1];
			for(int k = id+1; k < this._allTypes.length; k++){
				this._nextTypes[id][k-id-1] = this._allTypes[k];
			}
		}
		for(int k = 0; k<this._nextTypes[id].length; k++){
			if(this._nextTypes[id][k].getId() <= type.getId()){
				throw new RuntimeException("Error: "+this._nextTypes[id][k].getId()+"<="+type.getId());
			}
		}
		return this._nextTypes[id];
	}
	
	public final ArrayList<SemanticTag> getAllTypesExcludingStartAndFinish(){
		if(this._types_excluding_start_finish!=null){
			return this._types_excluding_start_finish;
		}
		this._types_excluding_start_finish = new ArrayList<SemanticTag>();
		for(MentionType type : this._allTypes){
			if(!type.equals(MentionType._START_TYPE) && !type.equals(MentionType._FINISH_TYPE)){
				this._types_excluding_start_finish.add(type);
			}
		}
		return this._types_excluding_start_finish;
	}
	
	public final SemanticTag[] getAllTypesExcludingStartAndFinish_arr(){
		if(this._types_excluding_start_finish_arr!=null)
			return this._types_excluding_start_finish_arr;
		
		ArrayList<SemanticTag> tags = this.getAllTypesExcludingStartAndFinish();
		
		this._types_excluding_start_finish_arr = new SemanticTag[tags.size()];
		for(int k = 0; k<this._types_excluding_start_finish_arr.length; k++)
			this._types_excluding_start_finish_arr[k] = _types_excluding_start_finish.get(k);
		return this._types_excluding_start_finish_arr;
	}
	
	public final MentionType[] getAllTypes(){
		return this._allTypes;
	}
	
	public MentionType getMentionTypeById(int id){
		return this._allTypes[id];
	}
	
}
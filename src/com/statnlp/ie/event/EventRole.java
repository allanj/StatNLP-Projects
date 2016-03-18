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
package com.statnlp.ie.event;

import java.util.ArrayList;
import java.util.Collections;

import com.statnlp.ie.types.MentionType;
import com.statnlp.ie.types.SemanticTag;

public class EventRole extends SemanticTag{
	
	private static final long serialVersionUID = -7979123306691983530L;
	
	private Event _event;
	private MentionType[] _compatible_types;
	
	public static EventRole createEventRole(Event event, String name){
		return new EventRole(event, name);
	}
	
	private EventRole(Event event, String name) {
		super(name);
		this._event = event;
	}
	
	public Event getEvent(){
		return this._event;
	}
	
	public void setCompatibleTypes(MentionType[] compatible_types){
		this._compatible_types = compatible_types;
	}
	
	public MentionType[] getCompatibleTypes(){
		return this._compatible_types;
	}
	
	public boolean compatibleWith(MentionType type){
		for(MentionType t:this._compatible_types){
			if(t.equals(type))
				return true;
		}
		return false;
	}
	
	public String viewCompatibleTypes(){
		StringBuilder sb = new StringBuilder();
		sb.append(this.getName());
		sb.append('[');
		ArrayList<String> types = new ArrayList<String>();
		for(int k = 0; k<this._compatible_types.length; k++){
			types.add(this._compatible_types[k].getFineTypeName());
		}
		Collections.sort(types);
		for(int k = 0; k<types.size(); k++){
			if(k!=0) sb.append('|');
			sb.append(types.get(k));
		}
		sb.append(']');
		return sb.toString();
	}
	
	@Override
	public int compareTo(SemanticTag st) {
		EventRole role = (EventRole)st;
		return this._id - role._id;
	}
	
	@Override
	public int hashCode(){
		return (this._event.hashCode() + 7) ^ (this._name.hashCode() + 7);
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof EventRole){
			EventRole cat = (EventRole)o;
			return this._name.equals(cat._name) && this._event.equals(cat._event);
		}
		return false;
	}
	
	@Override
	public String toString(){
		return "ROLE:"+this.getEvent().getSpecificName()+":"+this.getName();
	}
	
}
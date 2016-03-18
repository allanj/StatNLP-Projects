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

public class MentionType extends SemanticTag{
	
	private static final long serialVersionUID = 4676358853692957090L;
	
	public static MentionType _START_TYPE; 
	public static MentionType _FINISH_TYPE;
	
	public MentionType(String name) {
		super(name);
	}
	
	public String getFineTypeName(){
		return super.getName();
	}
	
	@Override
	public int compareTo(SemanticTag st) {
		MentionType type = (MentionType)st;
		return this._id - type._id;
	}
	
	@Override
	public int hashCode(){
		return this._name.hashCode() + 7;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof MentionType){
			MentionType type = (MentionType)o;
			return this._name.equals(type._name);
		}
		return false;
	}
	
	@Override
	public String toString(){
		return "TYPE:"+this._name;
	}

}
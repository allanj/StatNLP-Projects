/** Statistical Natural Language Processing System
    Copyright (C) 2014-2016  Lu, Wei

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
package com.statnlp.projects.nndcrf.exactFCRF;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author allan
 * Actually this file is the chunk label
 *
 */
public class ExactLabel implements Serializable{
	
	private static final long serialVersionUID = -5006849791095171763L;
	private static boolean locked = false;
	public static final Map<String, ExactLabel> Labels = new HashMap<String, ExactLabel>();
	public static final Map<Integer, ExactLabel> Labels_Index = new HashMap<Integer, ExactLabel>();
	
	public static ExactLabel get(String form){
		if(!Labels.containsKey(form)){
			if(locked) 
				throw new RuntimeException("Unknown entity type:"+form);
			ExactLabel label = new ExactLabel(form, Labels.size());
			Labels.put(form, label);
			Labels_Index.put(label._id, label);
		}
		return Labels.get(form);
	}
	
	public static void lock(){locked = true;} 
	
	public static ExactLabel get(int id){
		return Labels_Index.get(id);
	}
	
	private String _form;
	private int _id;
	
	public ExactLabel(ExactLabel lbl){
		this._form = lbl._form;
		this._id = lbl._id;
	}
	
	private ExactLabel(String form, int id){
		this._form = form;
		this._id = id;
	}
	
	public void setId(int id){
		this._id = id;
	}
	
	public int getId(){
		return this._id;
	}
	
	public String getForm(){
		return this._form;
	}
	
	public boolean equals(Object o){
		if(o instanceof ExactLabel){
			ExactLabel l = (ExactLabel)o;
			return this._form.equals(l._form);
		}
		return false;
	}
	
	public int hashCode(){
		return _form.hashCode();
	}
	
	public String toString(){
		return _form;
	}
	
}

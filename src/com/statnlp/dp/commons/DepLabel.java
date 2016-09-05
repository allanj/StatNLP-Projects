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
package com.statnlp.dp.commons;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wei_lu
 * Remember "sentence" is the label that from the root
 */
public class DepLabel implements Serializable{
	
	private static final long serialVersionUID = -5006849791095171763L;
	
	public static final Map<String, DepLabel> LABELS = new HashMap<String, DepLabel>();
	public static final Map<Integer, DepLabel> LABELS_INDEX = new HashMap<Integer, DepLabel>();
	public static String rootDepLabel;
	
	
	public static DepLabel get(String form){
		if(!LABELS.containsKey(form)){
			DepLabel label = new DepLabel(form, LABELS.size());
			LABELS.put(form, label);
			LABELS_INDEX.put(label._id, label);
		}
		return LABELS.get(form);
	}
	
	public static DepLabel get(int id){
		return LABELS_INDEX.get(id);
	}
	
	private String _form;
	private int _id;
	
	public DepLabel(DepLabel lbl){
		this._form = lbl._form;
		this._id = lbl._id;
	}
	
	private DepLabel(String form, int id){
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
		if(o instanceof DepLabel){
			DepLabel l = (DepLabel)o;
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

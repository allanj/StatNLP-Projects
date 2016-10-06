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
package com.statnlp.projects.entity.dcrf;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wei_lu
 *
 */
public class DEntity implements Serializable{
	
	private static final long serialVersionUID = -5006849791095171763L;
	
	public static final Map<String, DEntity> ENTS = new HashMap<String, DEntity>();
	public static final Map<Integer, DEntity> ENTS_INDEX = new HashMap<Integer, DEntity>();
	
	public static DEntity get(String form){
		if(!ENTS.containsKey(form)){
			DEntity label = new DEntity(form, ENTS.size());
			ENTS.put(form, label);
			ENTS_INDEX.put(label._id, label);
		}
		return ENTS.get(form);
	}
	
	public static DEntity get(int id){
		return ENTS_INDEX.get(id);
	}
	
	private String _form;
	private int _id;
	
	public DEntity(DEntity lbl){
		this._form = lbl._form;
		this._id = lbl._id;
	}
	
	private DEntity(String form, int id){
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
		if(o instanceof DEntity){
			DEntity l = (DEntity)o;
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

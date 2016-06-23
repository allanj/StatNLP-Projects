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
package com.statnlp.dp.model.bruteforce;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wei_lu
 *
 */
public class Entity implements Serializable{
	
	private static final long serialVersionUID = -5006849791095171763L;
	
	public static final Map<String, Entity> ENTS = new HashMap<String, Entity>();
	public static final Map<Integer, Entity> ENTS_INDEX = new HashMap<Integer, Entity>();
	
	private static boolean lock  = false;
	
	public static Entity get(String form){
		if(!ENTS.containsKey(form) && !lock){
			Entity label = new Entity(form, ENTS.size());
			ENTS.put(form, label);
			ENTS_INDEX.put(label._id, label);
		}
		if(!ENTS.containsKey(form) && lock){
			throw new RuntimeException("The entity set does not contain the entity");
		}
		return ENTS.get(form);
	}
	
	/**
	 * Lock it and do not allow other entity joins
	 * @return
	 */
	public static void lock(){
		lock = true;
	}
	
	public static Entity get(int id){
		return ENTS_INDEX.get(id);
	}
	
	private String _form;
	private int _id;
	
	public Entity(Entity lbl){
		this._form = lbl._form;
		this._id = lbl._id;
	}
	
	private Entity(String form, int id){
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
		if(o instanceof Entity){
			Entity l = (Entity)o;
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

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
import java.util.Iterator;

import com.statnlp.ie.event.Event;
import com.statnlp.ie.event.EventRole;
import com.statnlp.ie.event.EventTemplate;

public class IEManager implements Serializable{
	
	private static final long serialVersionUID = -5196318853347123323L;
	
	private HashMap<MentionType, MentionType> _typeMap;
	private HashMap<EventRole, EventRole> _roleMap;
	private HashMap<Event, EventTemplate> _templateMap;
	private ArrayList<MentionType> _typeList;
	private ArrayList<EventRole> _roleList;
	private MentionTemplate _mentionTemplate;
	private ArrayList<Event> _events;
	
	public IEManager(){
		this._typeMap = new HashMap<MentionType, MentionType>();
		this._roleMap = new HashMap<EventRole, EventRole>();
		this._templateMap = new HashMap<Event, EventTemplate>();
		this._typeList = new ArrayList<MentionType>();
		this._roleList = new ArrayList<EventRole>();
		this._events = new ArrayList<Event>();
		MentionType._START_TYPE  = this.toMentionType("START-TYPE");
		MentionType._FINISH_TYPE = this.toMentionType("FINISH-TYPE");
	}
	
	public Event toEvent(String generalName, String specificName){
		Event event = Event.createEvent(generalName, specificName);
		int index = this._events.indexOf(event);
		if(index>=0)
			return this._events.get(index);
		this._events.add(event);
		return event;
	}
	
	public void finalize(){
		this._mentionTemplate = new MentionTemplate(this._typeList);
	}
	
	public MentionTemplate getMentionTemplate(){
		return this._mentionTemplate;
	}
	
	public EventTemplate getEventTemplate(Event event){
		return this._templateMap.get(event);
	}
	
	public void addEventTemplate(EventTemplate template){
		Event event = template.getEvent();
		if(this._templateMap.containsKey(event))
			throw new RuntimeException("The event "+event+" is already there.");
		this._templateMap.put(event, template);
	}
	
//	public MentionType toMentionType(String name){
//		MentionType type = new MentionType(new String[]{"", name, name});
//		if(!this._typeMap.containsKey(type)){
//			type.setId(this._typeMap.size());
//			this._typeMap.put(type, type);
//			this._typeList.add(type);
//			return type;
//		}
//		return this._typeMap.get(type);
//	}
	
	public MentionType toMentionType(String name){
		MentionType type = new MentionType(name);
		if(!this._typeMap.containsKey(type)){
			type.setId(this._typeMap.size());
			this._typeMap.put(type, type);
			this._typeList.add(type);
			return type;
		}
		return this._typeMap.get(type);
	}
	
	public EventRole toEventRole(Event event, String roleName){
		EventRole role = EventRole.createEventRole(event, roleName);
		if(!this._roleMap.containsKey(role)){
			role.setId(this._roleMap.size());
			this._roleMap.put(role, role);
			this._roleList.add(role);
			return role;
		}
		return this._roleMap.get(role);
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();

		sb.append("TYPES:");
		sb.append('\n');
		sb.append('[');
		for(int i = 0; i<this._typeList.size(); i++){
			MentionType type = this._typeList.get(i);
			if(i!=0)
				sb.append(',');
			sb.append(type.getFineTypeName());
		}
		sb.append(']');
		sb.append('\n');

		sb.append("ROLES:");
		sb.append('\n');
		sb.append('[');
		for(int i = 0; i<this._roleList.size(); i++){
			EventRole role = this._roleList.get(i);
			if(i!=0)
				sb.append(',');
			sb.append(role.toString());
		}
		sb.append(']');
		sb.append('\n');

		sb.append("EVENTS:");
		sb.append('\n');
		Iterator<Event> events = this._templateMap.keySet().iterator();
		while(events.hasNext()){
			Event event = events.next();
			EventTemplate template = this._templateMap.get(event);
			sb.append(template.toString());
			sb.append('\n');
		}
		
		return sb.toString();
	}
	
}

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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import com.statnlp.ie.types.IEManager;
import com.statnlp.ie.types.MentionType;
import com.statnlp.ie.types.SemanticTag;

public class EventTemplate implements Serializable{
	
	private static final long serialVersionUID = 3014630124980029565L;
	
	private IEManager _manager;
	private Event _event;
	
	private EventRole[] _allRoles;
	private ArrayList<SemanticTag> _allRolesExcludingStartFinish;
	
	private EventRole _START_ROLE;
	private EventRole _FINISH_ROLE;
	
	public EventTemplate(IEManager manager, Event event, EventRole[] allRoles){
		this._manager = manager;
		this._event = event;
		this._allRoles = new EventRole[allRoles.length+2];
		
		this._START_ROLE = manager.toEventRole(event, "Start-Role");
		this._FINISH_ROLE = manager.toEventRole(event, "Finish-Role");
		this._START_ROLE.setCompatibleTypes(new MentionType[]{MentionType._START_TYPE});
		this._FINISH_ROLE.setCompatibleTypes(new MentionType[]{MentionType._FINISH_TYPE});
		
		this._allRoles[0] = this._START_ROLE;
		for(int k = 0; k<allRoles.length; k++)
			this._allRoles[k+1] = allRoles[k];
		this._allRoles[allRoles.length+1] = this._FINISH_ROLE;
		
		for(int k = 0; k<this._allRoles.length; k++)
			this._allRoles[k].setId(k);
	}
	
	public IEManager getManager(){
		return this._manager;
	}
	
	public EventRole getStartRole(){
		return this._START_ROLE;
	}
	
	public EventRole getFinishRole(){
		return this._FINISH_ROLE;
	}
	
	public final Event getEvent(){
		return this._event;
	}
	
	public final EventRole[] getAllRoles(){
		return this._allRoles;
	}
	
	public final ArrayList<SemanticTag> getAllRolesExcludingStartAndFinish(){
		if(this._allRolesExcludingStartFinish!=null){
			return this._allRolesExcludingStartFinish;
		}
		this._allRolesExcludingStartFinish = new ArrayList<SemanticTag>();
		for(int k = 1; k<this._allRoles.length-1; k++){
			this._allRolesExcludingStartFinish.add(this._allRoles[k]);
		}
		Collections.sort(this._allRolesExcludingStartFinish);
		return this._allRolesExcludingStartFinish;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(this.getEvent().toString());
		sb.append(' ');
		sb.append('{');
		for(int k = 0; k<this._allRoles.length; k++){
			sb.append(' ');
			sb.append(this._allRoles[k]);
		}
		sb.append(' ');
		sb.append('}');
		return sb.toString();
	}
	
}
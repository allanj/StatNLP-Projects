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

public class Event implements Serializable{
	
	private static final long serialVersionUID = 2832063760888176733L;
	
	private String _generalName;
	private String _specificName;
	
	public static Event createEvent(String generalName, String specificName){
		return new Event(generalName, specificName);
	}
	
	private Event(String generalName, String specificName){
		this._generalName = generalName;
		this._specificName = specificName;
	}
	
	public String getGeneralName(){
		return this._generalName;
	}
	
	public String getSpecificName(){
		return this._specificName;
	}
	
	@Override
	public int hashCode(){
		return (this._generalName.hashCode()+7) ^ (this._specificName.hashCode()+7);
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Event){
			Event e = (Event)o;
			return this._generalName.equals(e._generalName) && this._specificName.equals(e._specificName);
		}
		return false;
	}
	
	@Override
	public String toString(){
		return "Event:"+this._generalName+":"+this._specificName;
	}
	
}

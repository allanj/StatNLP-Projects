package semie.types;

import java.util.Arrays;

/**
 * Event
 * @author luwei
 * @version 1.0
 */

public class Event extends Identifiable implements Comparable<Event>{

	private static final long serialVersionUID = 5647821371260977282L;
	
	/**
	 * The name of the event, from most general to most specific types...
	 */
	private String[] _names;
	
	/**
	 * The list of possible roles.
	 */
	private Role[] _roles;
	
	/**
	 * The list of possible triggers.
	 */
	private String[] _triggers;
	
	public Event(int id, String[] names, Role[] roles, String[] triggers){
		super(id);
		this._names = names;
		this._roles = roles;
		this._triggers = triggers;
	}
	
	/**
	 * Get the list of names of the event
	 * @return the list of names of the event
	 */
	public String[] getNames(){
		return this._names;
	}
	
	/**
	 * Get the most specific name of the event
	 * @return the most specific name of the event
	 */
	public String getMostSpecificName(){
		return this._names[this._names.length-1];
	}
	
	/**
	 * Get the list of all possible roles
	 * @return the list of all possible roles
	 */
	public Role[] getRoles(){
		return this._roles;
	}
	
	/**
	 * Get the list of all possible triggers
	 * @return the list of all possible triggers
	 */
	public String[] getTriggers(){
		return this._triggers;
	}

	@Override
	public int compareTo(Event e) {
		return this._id - e._id;
	}
	
	@Override
	public String toString(){
		return this.getMostSpecificName();
	}
	
	@Override
	public int hashCode(){
		return Arrays.hashCode(this._names);
	}
	
	@Override
	public boolean equals(Object o){
		return o instanceof Event ? Arrays.equals(((Event)o)._names, this._names) : false;
	}
	
}

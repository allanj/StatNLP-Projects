package semie.types;

import java.util.Arrays;

/**
 * The role.
 * @author luwei
 * @version 1.0
 */

public class Role extends Identifiable implements Comparable<Role>{
	
	private static final long serialVersionUID = -5018760739920979876L;
	
	/**
	 * The names for the role
	 */
	private String[] _names;
	
	/**
	 * The list of acceptable types
	 */
	private Type[] _types;
	
	public static final Role _dummy = new Role(-100, new String[0]);
	
	public Role(int id, String[] names){
		super(id);
		this._names = names;
	}
	
	@Override
	public boolean equals(Object o){
		return o instanceof Role ? Arrays.equals(this._names, ((Role)o)._names) : false;
	}
	
	@Override
	public int hashCode(){
		return Arrays.hashCode(this._names) + 17;
	}
	
	@Override
	public int compareTo(Role arg){
		if(this._names.length != arg._names.length)
			return this._names.length - arg._names.length;
		for(int i = 0; i<this._names.length; i++){
			int val = this._names[i].compareTo(arg._names[i]);
			if(val != 0) return val;
		}
		return 0;
	}
	
	/**
	 * Set the acceptable types for the role
	 * @param types the acceptable types
	 */
	public void setTypes(Type[] types){
		this._types = types;
	}
	
	/**
	 * Get the names
	 * @return the names of the role
	 */
	public String[] getNames(){
		return this._names;
	}
	
	/**
	 * Get the acceptable types of the role
	 * @return the acceptable types
	 */
	public Type[] getTypes(){
		return this._types;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(this._names[0]);
		return sb.toString();
	}
	
}

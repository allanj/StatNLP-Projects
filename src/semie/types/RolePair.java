package semie.types;

/**
 * A pair of roles
 * @author luwei
 * @version 1.0
 */

public class RolePair implements Comparable<RolePair>{
	
	/**
	 * The first role
	 */
	protected Role _role1;
	
	/**
	 * The second role
	 */
	protected Role _role2;
	
	/**
	 * The value
	 */
	protected int _val;
	
	public RolePair(Role role1, Role role2, int val){
		this._role1 = role1;
		this._role2 = role2;
		this._val = val;
	}
	
	/**
	 * Get the first role
	 * @return the first role
	 */
	public Role first(){
		return this._role1;
	}
	
	/**
	 * Get the second role
	 * @return the second role
	 */
	public Role second(){
		return this._role2;
	}
	
	/**
	 * Get the value
	 * @return the value
	 */
	public int value(){
		return this._val;
	}
	
	@Override
	public int hashCode(){
		return (this._role1.hashCode()+17) ^ (this._role2.hashCode()+17) ^ (this._val+17);
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof RolePair){
			RolePair t = (RolePair)o;
			return this._role1 == t._role1 && this._role2 == t._role2 && this._val == t._val;
		}
		return false;
	}
	
	@Override
	public int compareTo(RolePair tuple){
		if(this._val != tuple._val)
			return this._val > tuple._val ? +1 : this._val < tuple._val ? -1 : 0;
		else
			if(this._role2.getId() != tuple._role2.getId())
				return this._role2.getId() > tuple._role2.getId() ? +1 : this._role2.getId() < tuple._role2.getId() ? -1 : 0;
			else
				return this._role1.getId() > tuple._role1.getId() ? +1 : this._role1.getId() < tuple._role1.getId() ? -1 : 0;
	}
}

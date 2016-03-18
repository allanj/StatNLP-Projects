package semie.types;


import java.util.ArrayList;

/**
 * Role span
 * @author luwei
 * @version 1.0
 */

public class RoleSpan implements Comparable<RoleSpan>{
	
	/**
	 * The role assigned to the role span
	 */
	private Role _role;
	
	/**
	 * The start index
	 */
	private int _bIndex;
	
	/**
	 * The end index
	 */
	private int _eIndex;
	
	/**
	 * The score associated with the role span
	 */
	private double _score;
	
	/**
	 * The previous role span
	 */
	private RoleSpan _prev;
	
	/**
	 * The features associated with the role span
	 */
	private ArrayList<ArrayList<Integer>> _features;
	
	public RoleSpan(int bIndex, int eIndex, Role role, double score, ArrayList<ArrayList<Integer>> features){
		this._bIndex = bIndex;
		this._eIndex = eIndex;
		this._role = role;
		this._score = score;
		this._features = features;
	}
	
	public EventAnnotation toEventAnnotation(){
		EventAnnotation ea = new EventAnnotation();
		this.toEventAnnotation_helper(ea, this._prev);
		return ea;
	}
	
	private void toEventAnnotation_helper(EventAnnotation ea, RoleSpan rolespan){
		if(rolespan._prev!=null){
			ea.annotateInterval(rolespan._bIndex, rolespan._eIndex, rolespan._role);
			this.toEventAnnotation_helper(ea, rolespan._prev);
		}
	}
	
	public ArrayList<ArrayList<Integer>> getFeatures(){
		return this._features;
	}
	
	public void setPrev(RoleSpan prev){
		this._prev = prev;
	}
	
	public RoleSpan getPrev(){
		return this._prev;
	}
	
	public Role getRole(){
		return this._role;
	}
	
	public int getBIndex(){
		return this._bIndex;
	}
	
	public int getEIndex(){
		return this._eIndex;
	}
	
	public double getScore(){
		return this._score;
	}

	@Override
	public int hashCode(){
		int code = this._bIndex + 17;
		code ^= this._eIndex + 17;
		code ^= this._role.hashCode() + 17;
		return code;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof RoleSpan){
			RoleSpan span = (RoleSpan)o;
			boolean val = this._bIndex == span._bIndex && this._eIndex == span._eIndex && this._role == span._role && this._score == span._score;
			if(!val){
				return false;
			}
			if(this._prev==null && span._prev==null){
				return true;
			} else if(this._prev==null){
				return false;
			} else if(span._prev==null){
				return false;
			} else {
				return this._prev.equals(span._prev);
			}
		}
		return false;
	}
	
	@Override
	public int compareTo(RoleSpan span) {
		return this._score > span._score ? +1 : this._score < span._score ? -1 : 0;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(this._role);
		sb.append('[');
		sb.append(this._bIndex);
		sb.append(',');
		sb.append(this._eIndex);
		sb.append(']');
		sb.append(this._score);
		return sb.toString();
	}
	
}

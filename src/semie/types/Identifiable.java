package semie.types;

import java.io.Serializable;

/**
 * Something that has an id.
 * @author luwei
 * @version 1.0
 */

public abstract class Identifiable implements Serializable{
	
	private static final long serialVersionUID = 5833467853928500918L;
	
	/**
	 * The id
	 */
	protected int _id;
	
	public Identifiable(int id){
		this._id = id;
	}
	
	/**
	 * Set the id
	 * @param id the id to be assigned
	 */
	public void setId(int id){
		this._id = id;
	}
	
	/**
	 * Get the id
	 * @return the id assigned
	 */
	public int getId(){
		return this._id;
	}
	
}

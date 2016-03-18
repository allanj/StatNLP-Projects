package semie.types;

import java.io.Serializable;
import java.util.Arrays;

/**
 * a representation that encodes an integer array.
 * @author luwei
 * @version 1.0
 */

public class IntArray implements Serializable, Comparable<IntArray>{
	
	private static final long serialVersionUID = 4359354349756917488L;
	
	/**
	 * The id of the integer array
	 */
	private int _id;
	
	/**
	 * The internal integer array representation 
	 */
	private int[] _keys;
	
	public IntArray(int[] keys){
		this._keys = keys;
	}
	
	/**
	 * Get the id
	 * @return the id of the integer array
	 */
	public int getId(){
		return this._id;
	}
	
	/**
	 * Set the id
	 * @param id the id of the integer array
	 */
	public void setId(int id){
		this._id = id;
	}
	
	@Override
	public int compareTo(IntArray key){
		if(this._keys.length != key._keys.length)
			return this._keys.length - key._keys.length;
		for(int i = 0; i<this._keys.length; i++)
			if(this._keys[i]!=key._keys[i])
				return this._keys[i]-key._keys[i];
		return 0;
	}
	
	@Override
	public boolean equals(Object o){
		return o instanceof IntArray ? Arrays.equals(this._keys, ((IntArray)o)._keys) : false;
	}
	
	@Override
	public int hashCode(){
		return Arrays.hashCode(this._keys);
	}
	
}

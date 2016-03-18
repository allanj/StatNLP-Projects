package semie.pref;

import java.io.Serializable;

/**
 * This class is used to represent a preference
 * @author luwei
 * @version 1.0
 */

public abstract class Preference implements Serializable{
	
	private static final long serialVersionUID = -9128851587044073634L;
	
	/**
	 * The preference feature
	 */
	protected int _pf;
	
	public Preference(int pf){
		this._pf = pf;
	}
	
	/**
	 * Get the preference feature
	 * @return the preference feature
	 */
	public int getPreferenceFeature(){
		return this._pf;
	}
	
}

package semie.pref;

/**
 * Boolean preference
 * @author luwei
 * @version 1.0
 */

public class BooleanPreference extends Preference{
	
	private static final long serialVersionUID = 1195350077238603638L;
	
	public static final int _TRUE = -10700;
	public static final int _FALSE = -20700;
	
	/**
	 * The expected value
	 */
	private boolean _expectedValue;
	
	public BooleanPreference(int cf, boolean expectedValue) {
		super(cf);
		this._expectedValue = expectedValue;
	}
	
	/**
	 * Get the expected value
	 * @return the expected value
	 */
	public boolean getExpectedValue(){
		return this._expectedValue;
	}
	
	/**
	 * Check if the preference is satisfied
	 * @param cf the preference feature
	 * @param count the actual count
	 * @return true if satisfied, false otherwise
	 */
	public boolean satisfy(int cf, int count){
		return this._pf != cf ? false : this._expectedValue ? count > 0 : count == 0;
	}
	
}

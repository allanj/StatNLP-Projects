package semie.pref;

/**
 * Comparison preference
 * @author luwei
 * @version 1.0
 */

public class ComparisonPreference extends Preference{
	
	private static final long serialVersionUID = -8682051381775798384L;
	
	public static final int _LTEQ = -10090;
	public static final int _GTEQ = -20090;
	public static final int _LT = -30090;
	public static final int _GT = -40090;
	public static final int _EQ = -50090;
	
	private int _cf2;
	private int _relation;
	
	public ComparisonPreference(int cf1, int cf2, int relation){
		super(cf1);
		this._cf2 = cf2;
		this._relation = relation;
	}
	
	public boolean satisfy(int cf1, int cf2, int count1, int count2){
		int the_count1 = count1;
		int the_count2 = count2;
		if(this._pf == cf2 && this._cf2 == cf1){
			the_count1 = count2;
			the_count2 = count1;
		} else if(!(this._pf == cf1 && this._cf2 == cf2)){
			return true;
		}
		
		if(the_count1 > the_count2)
			return this._relation == _GTEQ || this._relation == _GT;
		else if(the_count1 < the_count2)
			return this._relation == _LTEQ || this._relation == _LT;
		else
			return this._relation == _EQ || this._relation == _GTEQ || this._relation == _LTEQ;
	}
}

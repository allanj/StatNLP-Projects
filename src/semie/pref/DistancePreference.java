package semie.pref;

public class DistancePreference extends Preference{
	
	private static final long serialVersionUID = 8791566374185352343L;
	
	public static final int _GTEQ = -10900;
	public static final int _LTEQ = -20900;
	public static final int _GT = -30900;
	public static final int _LT = -40900;
	public static final int _EQ = -50900;
	
	private int _cf2;
	private double _distance;
	private int _relation;
	
	public DistancePreference(int cf1, int cf2, int relation, double distance){
		super(cf1);
		this._cf2 = cf2;
		this._relation = relation;
		this._distance = distance;
	}

	public boolean satisfy(int cf1, int cf2, int relation, double distance){
		if(this._pf != cf1 || this._cf2 != cf2){
			return true;
		}
		if(distance > this._distance){
			return this._relation == _GTEQ || this._relation == _GT;
		} else if(distance < this._distance){
			return this._relation == _LTEQ || this._relation == _LT;
		} else {
			return this._relation == _LTEQ || this._relation == _GTEQ || this._relation == _EQ;
		}
	}
}

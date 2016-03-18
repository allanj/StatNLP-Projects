package semie.pref;

public class CountPreference extends Preference{
	
	private static final long serialVersionUID = -8998546168755520646L;
	
	public static final int _LTEQ = -10700; // less than or equal to
	public static final int _GTEQ = -20700; // greater than or equal to
	public static final int _EQAL = -30700; // equal to
	
	/**
	 * The relation
	 */
	private int _relation;
	
	/**
	 * The count
	 */
	private int _count;
	
	public CountPreference(int cf, int relation, int count){
		super(cf);
		this._relation = relation;
		this._count = count;
	}
	
	public int getRelation(){
		return this._relation;
	}
	
	public int getCount(){
		return this._count;
	}
	
	public boolean satisfy(int cf, int count){
		
		if(this._pf != cf)
			return false;
		
		if(this._relation == _EQAL)
			return this._count == count;
		else if(this._relation == _LTEQ)
			return count < this._count || count == this._count;
		else if(this._relation == _GTEQ)
			return count > this._count || count == this._count;
		
		throw new IllegalArgumentException("The relation "+this._relation+" is not valid.");
		
	}
	
}

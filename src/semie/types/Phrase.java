package semie.types;

/**
 * A phrase object. 
 * Note: This data structure is only useful when we need to cache
 * the features for each instance. 
 * Also, if we want to do contrastive estimation, this is essential too.
 * @author luwei
 * @version 1.0
 */

public class Phrase extends Identifiable{
	
	private static final long serialVersionUID = 5818056544208096772L;
	
	/**
	 * The event span to which the phrase belongs
	 */
	private EventSpan _span;
	
	/**
	 * The start index
	 */
	private int _bIndex;
	
	/**
	 * The end index
	 */
	private int _eIndex;
	
	/**
	 * The special begin phrase
	 */
	public static final Phrase _BEG_PHRASE = new Phrase(-100, EventSpan._dummy, -1, 0);
	
	/**
	 * The special end phrase
	 */
	public static final Phrase _END_PHRASE = new Phrase(-200, EventSpan._dummy, Integer.MAX_VALUE, Integer.MAX_VALUE);
	
	public Phrase(int id, EventSpan span, int bIndex, int eIndex){
		super(id);
		this._span = span;
		this._bIndex = bIndex;
		this._eIndex = eIndex;
	}
	
	/**
	 * Get the event span to which the phrase belongs
	 * @return the event span
	 */
	public EventSpan getEventSpan(){
		return this._span;
	}
	
	/**
	 * Get the start index
	 * @return the start index
	 */
	public int getBIndex(){
		return this._bIndex;
	}
	
	/**
	 * Get the end index
	 * @return the end index
	 */
	public int getEIndex(){
		return this._eIndex;
	}
	
	@Override
	public int hashCode(){
		return (this._span.getId() + 17) ^ (this._bIndex + 17) ^ (this._eIndex + 17);
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Phrase){
			Phrase p = (Phrase)o;
			return p._span.equals(this._span) && p._bIndex == this._bIndex && p._eIndex == this._eIndex;
		}
		return false;
	}
	
}

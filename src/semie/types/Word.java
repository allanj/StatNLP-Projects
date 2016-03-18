package semie.types;

/**
 * A word object.
 * @author luwei
 * @version 1.0
 */

public class Word extends Identifiable{
	
	private static final long serialVersionUID = 8617491065118427627L;
	
	/**
	 * Special begin word
	 */
	public static final Word _BEGIN_WORD = new Word(-100, "#BEGIN_WORD#");
	
	/**
	 * Special end word
	 */
	public static final Word _END_WORD = new Word(-200, "#END_WORD#");
	
	/**
	 * Special null word
	 */
	public static final Word _NULL_WORD = new Word(-300, "#NULL_WORD#");
	
	/**
	 * Special new word
	 */
	public static final Word _NEW_WORD = new Word(-400, "#NEW_WORD#");
	
	/**
	 * Special any word
	 */
	public static final Word _ANY_WORD = new Word(-500, "#ANY_WORD#");
	
	/**
	 * The string form of the word
	 */
	private String _form;
	
	public Word(int id, String form){
		super(id);
		this._form = form;
	}
	
	/**
	 * Get the string form of the word
	 * @return the string form of the word
	 */
	public String getWord(){
		return this._form;
	}
	
	@Override
	public int hashCode(){
		return _form.hashCode() + 17;
	}
	
	@Override
	public boolean equals(Object o){
		return o instanceof Word ? ((Word)o)._form.equals(this._form) : false;
	}
	
	@Override
	public String toString(){
		return this._form;
	}

}

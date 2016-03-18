package semie.types;

import java.io.Serializable;

/**
 * Document
 * @author luwei
 * @version 1.0
 */

public abstract class Document implements Serializable{
	
	private static final long serialVersionUID = 6599273152647567930L;
	
	/**
	 * The id of the document
	 */
	protected String _id;
	
	/**
	 * The body of the document, i.e., the text
	 */
	protected String _text;
	
	public Document(String id, String text){
		this._id = id;
		this._text = text;
	}
	
	/**
	 * Get the id of the document
	 * @return the id in string form
	 */
	public String getId(){
		return this._id;
	}
	
	/**
	 * Get the text of the document
	 * @return the text in string format
	 */
	public String getText(){
		return this._text;
	}
	
}

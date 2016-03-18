package semie.types;

/**
 * The tag object.
 * @author luwei
 * @version 1.0
 */

public class Tag extends Identifiable{
	
	private static final long serialVersionUID = 6901184575962367953L;
	
	/**
	 * The string representation of the tag
	 */
	private String _form;

	public Tag(int id, String form) {
		super(id);
		this._form = form;
	}
	
	/**
	 * Get the string representation of the tag
	 * @return the string representation of the tag
	 */
	public String getTag(){
		return this._form;
	}
	
	@Override
	public int hashCode(){
		return _form.hashCode() + 17;
	}
	
	@Override
	public boolean equals(Object o){
		return o instanceof Tag ? ((Tag)o)._form.equals(this._form) : false;
	}

}

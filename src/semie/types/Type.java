package semie.types;

/**
 * The type is a type/class associated with a mention.
 * @author luwei
 * @version 1.0
 */

public class Type extends Identifiable implements Comparable<Type>{
	
	private static final long serialVersionUID = -4609206821512991240L;
	
	/**
	 * Special begin type
	 */
	public static final Type _BEGIN_TYPE = new Type(-100, "#BEGIN_TYPE#");
	
	/**
	 * Special end type
	 */
	public static final Type _END_TYPE = new Type(-200, "#END_TYPE#");
	
	/**
	 * Special all type
	 */
	public static final Type _ALL_TYPE = new Type(-300, "#ALL_TYPE#");
	
	/**
	 * Special no type
	 */
	public static final Type _NO_TYPE = new Type(-400, "#NO_TYPE#");
	
	/**
	 * String representation of the type
	 */
	private String _form;
	
	public Type(int id, String form){
		super(id);
		this._form = form;
	}
	
	/**
	 * Get the string representation of the type
	 * @return the string representation of the type
	 */
	public String getType(){
		return this._form;
	}
	
	@Override
	public int compareTo(Type t) {
		return this._form.compareTo(t._form);
	}
	
	@Override
	public int hashCode(){
		return this._form.hashCode() + 17;
	}
	
	@Override
	public boolean equals(Object o){
		return o instanceof Type ? ((Type)o)._form.equals(this._form) : false;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append('t');
		sb.append(':');
		sb.append(this._form);
		return sb.toString();
	}
	
}

package com.statnlp.mt.commons;

import com.statnlp.commons.types.HiddenToken;

public class CeptToken extends HiddenToken{
	
	private static final long serialVersionUID = -2496548616444534994L;
	
	public CeptToken(String name) {
		super(name);
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof CeptToken){
			CeptToken token = (CeptToken)o;
			return this._name.equals(token._name);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return this._name.hashCode() + 7;
	}
	
	@Override
	public String toString() {
		return this._name;
	}
	
}

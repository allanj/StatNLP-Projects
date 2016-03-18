package com.statnlp.mt.commons;

import com.statnlp.commons.types.InputToken;

public class SrcWord extends InputToken{
	
	private static final long serialVersionUID = -9145813628752946037L;
	
	public SrcWord(String name) {
		super(name);
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof SrcWord){
			SrcWord token = (SrcWord)o;
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
		return "SRC:"+this._name;
	}
	
}

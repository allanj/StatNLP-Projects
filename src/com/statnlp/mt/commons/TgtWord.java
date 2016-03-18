package com.statnlp.mt.commons;

import com.statnlp.commons.types.OutputToken;

public class TgtWord extends OutputToken{
	
	private static final long serialVersionUID = -8909275164717297200L;
	
	public TgtWord(String name) {
		super(name);
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof TgtWord){
			TgtWord token = (TgtWord)o;
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
		return "TGT:"+this._name;
	}
	
}

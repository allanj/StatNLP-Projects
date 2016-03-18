package com.statnlp.topic.commons;

import com.statnlp.commons.types.InputToken;

public class TopicWordToken extends InputToken{
	
	private static final long serialVersionUID = -9145813628752946037L;
	
	public TopicWordToken(String name) {
		super(name);
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof TopicWordToken){
			TopicWordToken token = (TopicWordToken)o;
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
		return "Word:"+this._name;
	}
	
}

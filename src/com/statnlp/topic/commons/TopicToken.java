package com.statnlp.topic.commons;

import com.statnlp.commons.types.HiddenToken;

public class TopicToken extends HiddenToken{
	
	private static final long serialVersionUID = -2496548616444534994L;
	
	public TopicToken(String name) {
		super(name);
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof TopicToken){
			TopicToken token = (TopicToken)o;
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

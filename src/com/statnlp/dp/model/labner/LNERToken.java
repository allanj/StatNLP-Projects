package com.statnlp.dp.model.labner;

import com.statnlp.commons.types.WordToken;

public class LNERToken extends WordToken {

	private static final long serialVersionUID = 5946539674975614589L;

	private NERLabel depLabel;
	
	public LNERToken(String name) {
		super(name);
		this.depLabel = null;
	}
	
	public LNERToken(String name, String tag) {
		super(name,tag);
		this.depLabel = null;
	}
	
	public LNERToken(String name, String tag, int headIndex) {
		super(name,tag,headIndex);
		this.depLabel = null;
	}
	
	public LNERToken(String name, String tag, int headIndex, String entity) {
		super(name, tag, headIndex, entity);
		this.depLabel = null;
	}
	
	public LNERToken(String name, String tag, int headIndex, String entity, NERLabel depLabel) {
		super(name, tag, headIndex, entity);
		this.depLabel = depLabel;
	}
	
	
	public NERLabel getDepLabel(){return this.depLabel;}
	public void setDepLabel(NERLabel depLabel){this.depLabel = depLabel;}

}

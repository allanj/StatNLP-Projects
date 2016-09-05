package com.statnlp.dp.model.labelleddp;

import com.statnlp.commons.types.WordToken;
import com.statnlp.dp.commons.DepLabel;

public class LDPToken extends WordToken {

	private static final long serialVersionUID = 5946539674975614589L;

	private DepLabel depLabel;
	
	public LDPToken(String name) {
		super(name);
		this.depLabel = null;
	}
	
	public LDPToken(String name, String tag) {
		super(name,tag);
		this.depLabel = null;
	}
	
	public LDPToken(String name, String tag, int headIndex) {
		super(name,tag,headIndex);
		this.depLabel = null;
	}
	
	public LDPToken(String name, String tag, int headIndex, String entity) {
		super(name, tag, headIndex, entity);
		this.depLabel = null;
	}
	
	public LDPToken(String name, String tag, int headIndex, String entity, DepLabel depLabel) {
		super(name, tag, headIndex, entity);
		this.depLabel = depLabel;
	}
	
	
	public String getDepLabel(){return this.depLabel.getForm();}
	public void setDepLabel(DepLabel depLabel){this.depLabel = depLabel;}

}

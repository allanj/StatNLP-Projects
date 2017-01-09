package com.statnlp.projects.joint.mix;

import java.io.Serializable;
import java.util.ArrayList;

public class MixPair implements Serializable{

	private static final long serialVersionUID = -8034438633804820367L;
	protected int[] heads;
	protected ArrayList<MixSpan> entities;
	
	public MixPair() {
	}
	
	public MixPair(int[] heads, ArrayList<MixSpan> entities) {
		this.heads = heads;
		this.entities = entities;
	}
	
	@SuppressWarnings("unchecked")
	public MixPair duplicate() {
		MixPair output = new MixPair();
		if (heads == null)
			output.heads = null;
		else output.heads = heads.clone();
		
		if (entities == null) 
			output.entities = null;
		else output.entities = (ArrayList<MixSpan>)entities.clone();
		return output;
	}

}

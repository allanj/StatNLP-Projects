package com.statnlp.projects.mfjoint_linear;

public class MFLPair {

	protected int[] heads;
	protected String[] entities;
	
	public MFLPair() {
	}
	
	public MFLPair(int[] heads, String[] entities) {
		this.heads = heads;
		this.entities = entities;
	}
	
	public MFLPair duplicate() {
		MFLPair output = new MFLPair();
		if (heads == null)
			output.heads = null;
		else output.heads = heads.clone();
		
		if (entities == null) 
			output.entities = null;
		else output.entities = entities.clone();
		return output;
	}
}

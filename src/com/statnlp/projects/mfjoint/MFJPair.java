package com.statnlp.projects.mfjoint;

import java.util.ArrayList;

public class MFJPair {

	protected int[] heads;
	protected ArrayList<MFJSpan> entities;
	
	public MFJPair() {
	}
	
	public MFJPair(int[] heads, ArrayList<MFJSpan> entities) {
		this.heads = heads;
		this.entities = entities;
	}
	
	@SuppressWarnings("unchecked")
	public MFJPair duplicate() {
		MFJPair output = new MFJPair();
		if (heads == null)
			output.heads = null;
		else output.heads = heads.clone();
		
		if (entities == null) 
			output.entities = null;
		else output.entities = (ArrayList<MFJSpan>)entities.clone();
		return output;
	}
}

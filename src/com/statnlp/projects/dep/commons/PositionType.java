package com.statnlp.projects.dep.commons;

public class PositionType {

	protected String type;
	protected int length;
	
	/**
	 * Constructor functions
	 * @param type: doesn't contain the prefix B- I- encoding 
	 * @param length
	 */
	public PositionType(String type, int length){
		this.type = type;
		this.length = length;
	}
	
	
	public String getType(){
		return this.type;
	}
	
	public int getLength(){
		return this.length;
	}
}

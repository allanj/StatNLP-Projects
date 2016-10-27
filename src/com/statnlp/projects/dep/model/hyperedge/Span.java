package com.statnlp.projects.dep.model.hyperedge;

public class Span {

	int leftIndex;
	int rightIndex;
	Label label;
	
	/**
	 * Construct a span
	 * @param leftIdx: inclusive
	 * @param rightIdx: inclusive
	 * @param label: label type, entity.
	 */
	public Span(int leftIdx, int rightIdx, Label label){
		this.leftIndex = leftIdx;
		this.rightIndex = rightIdx;
		this.label = label;
	}
	
	/**
	 * Construct a span without the label;
	 * @param leftIdx
	 * @param rightIdx
	 */
	public Span(int leftIdx, int rightIdx){
		this.leftIndex = leftIdx;
		this.rightIndex = rightIdx;
		this.label = null;
	}
	
}

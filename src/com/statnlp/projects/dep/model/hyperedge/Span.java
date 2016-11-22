package com.statnlp.projects.dep.model.hyperedge;

import java.io.Serializable;


/**
 * Span like semi-CRFs.
 * @author allanjie
 *
 */
public class Span implements Comparable<Span>, Serializable{
	
	private static final long serialVersionUID = 1849557517361796614L;
	public Label label;
	public int start;
	public int end;
	public int headIndex;

	/**
	 * 
	 * @param start: inclusive
	 * @param end: inclusive
	 * @param label
	 */
	public Span(int start, int end, Label label, int headIndex) {
		if(start>end)
			throw new RuntimeException("Start cannot be larger than end");
		this.start = start;
		this.end = end;
		this.label = label;
		this.headIndex = headIndex;
	}
	
	public Span(int start, int end, Label label) {
		if(start>end)
			throw new RuntimeException("Start cannot be larger than end");
		this.start = start;
		this.end = end;
		this.label = label;
	}
	
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Span){
			Span s = (Span)o;
			if(start != s.start) return false;
			if(end != s.end) return false;
			//if(headIndex != s.headIndex) return false;
			return label.equals(s.label);
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + end;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + start;
		return result;
	}

	@Override
	public int compareTo(Span o) {
		if(start < o.start) return -1;
		if(start > o.start) return 1;
		if(end < o.start) return -1;
		if(end > o.end) return 1;
		return label.compareTo(o.label);
	}
	
	public String toString(){
		return String.format("%d,%d head:%d, %s", start, end, headIndex, label);
	}
	
}

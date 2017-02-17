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
//	public int headIndex;
	public Span headSpan;
	public String depLabel;

	/**
	 * 
	 * @param start: inclusive
	 * @param end: inclusive
	 * @param label
	 */
	public Span(int start, int end, Label label, Span headSpan) {
		if(start>end)
			throw new RuntimeException("Start cannot be larger than end");
		this.start = start;
		this.end = end;
		this.label = label;
		this.headSpan = headSpan;
	}
	
	public Span(int start, int end, Label label) {
		if(start>end)
			throw new RuntimeException("Start cannot be larger than end");
		this.start = start;
		this.end = end;
		this.label = label;
	}
	
	public Span(int start, int end) {
		if(start>end)
			throw new RuntimeException("Start cannot be larger than end");
		this.start = start;
		this.end = end;
	}
	
	public int length() {
		return this.end - this.start + 1;
	}
	
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Span){
			Span s = (Span)o;
			if(start != s.start) return false;
			if(end != s.end) return false;
			//if(headIndex != s.headIndex) return false;
			if (label == null)
				return true;
			else
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
		if (label != o.label)
			return label.compareTo(o.label);
		else return 0;
	}
	
	public String toString(){
		if (headSpan != null)
			return String.format("[%d, %d, %s head-span:(%d, %d), %s]", start, end, label, headSpan.start, headSpan.end, depLabel);
		else
			return String.format("[%d, %d, %s]", start, end, label);
	}
	
}

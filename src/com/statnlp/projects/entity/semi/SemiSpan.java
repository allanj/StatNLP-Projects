package com.statnlp.projects.entity.semi;

import java.io.Serializable;

public class SemiSpan implements Comparable<SemiSpan>, Serializable{
	
	private static final long serialVersionUID = 1849557517361796614L;
	public SemiLabel label;
	public int start;
	public int end;

	/**
	 * 
	 * @param start: inclusive
	 * @param end: inclusive
	 * @param label
	 */
	public SemiSpan(int start, int end, SemiLabel label) {
		if(start>end)
			throw new RuntimeException("Start cannot be larger than end");
		this.start = start;
		this.end = end;
		this.label = label;
	}
	
	public boolean equals(Object o){
		if(o instanceof SemiSpan){
			SemiSpan s = (SemiSpan)o;
			if(start != s.start) return false;
			if(end != s.end) return false;
			return label.equals(s.label);
		}
		return false;
	}

	@Override
	public int compareTo(SemiSpan o) {
		if(start < o.start) return -1;
		if(start > o.start) return 1;
		if(end < o.start) return -1;
		if(end > o.end) return 1;
		return label.compareTo(o.label);
	}
	
	public String toString(){
		return String.format("%d,%d %s", start, end, label);
	}

}

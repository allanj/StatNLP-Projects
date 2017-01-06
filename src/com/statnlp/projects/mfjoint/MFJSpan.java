package com.statnlp.projects.mfjoint;

import java.io.Serializable;


/**
 * Span like semi-CRFs.
 * @author allanjie
 *
 */
public class MFJSpan implements Comparable<MFJSpan>, Serializable{
	
	private static final long serialVersionUID = 1849557517361796614L;
	public MFJLabel label;
	public int start;
	public int end;

	public MFJSpan(int start, int end, MFJLabel label) {
		if(start>end)
			throw new RuntimeException("Start cannot be larger than end");
		this.start = start;
		this.end = end;
		this.label = label;
	}
	
	public int length() {
		return this.end - this.start + 1;
	}
	
	
	@Override
	public boolean equals(Object o){
		if(o instanceof MFJSpan){
			MFJSpan s = (MFJSpan)o;
			if(start != s.start) return false;
			if(end != s.end) return false;
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
	public int compareTo(MFJSpan o) {
		if(start < o.start) return -1;
		if(start > o.start) return 1;
		if(end < o.start) return -1;
		if(end > o.end) return 1;
		return label.compareTo(o.label);
	}
	
	public String toString(){
		return String.format("[%d, %d, %s]", start, end, label);
	}
	
}

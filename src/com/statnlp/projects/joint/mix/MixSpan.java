package com.statnlp.projects.joint.mix;

import java.io.Serializable;

public class MixSpan implements Comparable<MixSpan>, Serializable{

	private static final long serialVersionUID = -2448094154546460309L;
	public MixLabel label;
	public int start;
	public int end;

	public MixSpan(int start, int end, MixLabel label) {
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
		if(o instanceof MixSpan){
			MixSpan s = (MixSpan)o;
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
	public int compareTo(MixSpan o) {
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

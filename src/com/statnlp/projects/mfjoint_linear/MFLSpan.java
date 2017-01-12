package com.statnlp.projects.mfjoint_linear;

import java.io.Serializable;
import java.util.HashSet;

public class MFLSpan implements Comparable<MFLSpan>, Serializable{

	private static final long serialVersionUID = -2448094154546460309L;
	public MFLLabel label;
	public int start;
	public int end;
	public HashSet<Integer> heads;

	public MFLSpan(int start, int end, MFLLabel label, HashSet<Integer> heads) {
		if(start>end)
			throw new RuntimeException("Start cannot be larger than end");
		this.start = start;
		this.end = end;
		this.label = label;
		this.heads = heads;
	}
	
	public int length() {
		return this.end - this.start + 1;
	}
	
	
	@Override
	public boolean equals(Object o){
		if(o instanceof MFLSpan){
			MFLSpan s = (MFLSpan)o;
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
	public int compareTo(MFLSpan o) {
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

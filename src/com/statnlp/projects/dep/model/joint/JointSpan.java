package com.statnlp.projects.dep.model.joint;

import java.io.Serializable;


/**
 * Span like semi-CRFs.
 * @author allanjie
 *
 */
public class JointSpan implements Comparable<JointSpan>, Serializable{
	
	private static final long serialVersionUID = 1849557517361796614L;
	public Label label;
	public int start;
	public int end;
//	public int headIndex;
	public JointSpan headSpan;
	public String depLabel;

	/**
	 * 
	 * @param start: inclusive
	 * @param end: inclusive
	 * @param label
	 */
	public JointSpan(int start, int end, Label label, JointSpan headSpan) {
		if(start>end)
			throw new RuntimeException("Start cannot be larger than end");
		this.start = start;
		this.end = end;
		this.label = label;
		this.headSpan = headSpan;
	}
	
	public JointSpan(int start, int end, Label label) {
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
		if(o instanceof JointSpan){
			JointSpan s = (JointSpan)o;
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
	public int compareTo(JointSpan o) {
		if(start < o.start) return -1;
		if(start > o.start) return 1;
		if(end < o.start) return -1;
		if(end > o.end) return 1;
		return label.compareTo(o.label);
	}
	
	public String toString(){
		if (headSpan != null)
			return String.format("[%d, %d, %s head-span:(%d, %d), %s]", start, end, label, headSpan.start, headSpan.end, depLabel);
		else
			return String.format("[%d, %d, %s]", start, end, label);
	}
	
}
package com.statnlp.projects.dep.model.segdep;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SpanLabel implements Comparable<SpanLabel>, Serializable{
	
	private static final long serialVersionUID = -3314363044582374266L;
	public static final Map<String, SpanLabel> Labels = new HashMap<String, SpanLabel>();
	public static final Map<Integer, SpanLabel> Label_Index = new HashMap<Integer, SpanLabel>();
	public static boolean locked = false;
	
	public static SpanLabel get(String form){
		if(!Labels.containsKey(form)){
			if (locked) throw new RuntimeException("The label set is locked. cannot add more. ");
			SpanLabel label = new SpanLabel(form, Labels.size());
			Labels.put(form, label);
			Label_Index.put(label.id, label);
		}
		return Labels.get(form);
	}
	
	public static SpanLabel get(int id){
		return Label_Index.get(id);
	}
	
	/**
	 * Lock the label set to make it not add additional labels.
	 */
	public static void lock(){
		locked = true;
	}
	
	public String form;
	public int id;
	
	private SpanLabel(String form, int id) {
		this.form = form;
		this.id = id;
	}

	@Override
	public int hashCode() {
		return form.hashCode();
	}

	public String getForm(){
		return this.form;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SpanLabel))
			return false;
		SpanLabel other = (SpanLabel) obj;
		if (form == null) {
			if (other.form != null)
				return false;
		} else if (!form.equals(other.form))
			return false;
		return true;
	}
	
	public String toString(){
		return String.format("%s(%d)", form, id);
	}

	@Override
	public int compareTo(SpanLabel o) {
		return Integer.compare(id, o.id);
	}
	
	public static int compare(SpanLabel o1, SpanLabel o2){
		if(o1 == null){
			if(o2 == null) return 0;
			else return -1;
		} else {
			if(o2 == null) return 1;
			else return o1.compareTo(o2);
		}
	}
}

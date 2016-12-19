package com.statnlp.projects.entity.semi;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SemiLabel implements Comparable<SemiLabel>, Serializable{
	
	private static final long serialVersionUID = -3314363044582374266L;
	public static final Map<String, SemiLabel> LABELS = new HashMap<String, SemiLabel>();
	public static final Map<Integer, SemiLabel> LABELS_INDEX = new HashMap<Integer, SemiLabel>();
	
	public static SemiLabel get(String form){
		if(!LABELS.containsKey(form)){
			SemiLabel label = new SemiLabel(form, LABELS.size());
			LABELS.put(form, label);
			LABELS_INDEX.put(label.id, label);
		}
		return LABELS.get(form);
	}
	
	public static SemiLabel get(int id){
		return LABELS_INDEX.get(id);
	}
	
	public String form;
	public int id;
	
	private SemiLabel(String form, int id) {
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
		if (!(obj instanceof SemiLabel))
			return false;
		SemiLabel other = (SemiLabel) obj;
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
	public int compareTo(SemiLabel o) {
		return Integer.compare(id, o.id);
	}
	
	public static int compare(SemiLabel o1, SemiLabel o2){
		if(o1 == null){
			if(o2 == null) return 0;
			else return -1;
		} else {
			if(o2 == null) return 1;
			else return o1.compareTo(o2);
		}
	}
}

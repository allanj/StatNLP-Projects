package com.statnlp.projects.mfjoint_linear;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MFLLabel implements Comparable<MFLLabel>, Serializable{
	
	private static final long serialVersionUID = -3314363044582374266L;
	public static final Map<String, MFLLabel> Labels = new HashMap<String, MFLLabel>();
	public static final Map<Integer, MFLLabel> Label_Index = new HashMap<Integer, MFLLabel>();
	public static boolean locked = false;
	
	public static MFLLabel get(String form){
		if(!Labels.containsKey(form)){
			if (locked) throw new RuntimeException("The label set is locked. cannot add more. ");
			MFLLabel label = new MFLLabel(form, Labels.size());
			Labels.put(form, label);
			Label_Index.put(label.id, label);
		}
		return Labels.get(form);
	}
	
	public static MFLLabel get(int id){
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
	
	public MFLLabel(String form, int id) {
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
		if (!(obj instanceof MFLLabel))
			return false;
		MFLLabel other = (MFLLabel) obj;
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
	public int compareTo(MFLLabel o) {
		return Integer.compare(id, o.id);
	}
	
	public static int compare(MFLLabel o1, MFLLabel o2){
		if(o1 == null){
			if(o2 == null) return 0;
			else return -1;
		} else {
			if(o2 == null) return 1;
			else return o1.compareTo(o2);
		}
	}
}

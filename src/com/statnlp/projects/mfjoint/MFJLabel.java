package com.statnlp.projects.mfjoint;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MFJLabel implements Comparable<MFJLabel>, Serializable{
	
	private static final long serialVersionUID = -3314363044582374266L;
	public static final Map<String, MFJLabel> Labels = new HashMap<String, MFJLabel>();
	public static final Map<Integer, MFJLabel> Label_Index = new HashMap<Integer, MFJLabel>();
	public static boolean locked = false;
	
	public static MFJLabel get(String form){
		if(!Labels.containsKey(form)){
			if (locked) throw new RuntimeException("The label set is locked. cannot add more. ");
			MFJLabel label = new MFJLabel(form, Labels.size());
			Labels.put(form, label);
			Label_Index.put(label.id, label);
		}
		return Labels.get(form);
	}
	
	public static MFJLabel get(int id){
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
	
	public MFJLabel(String form, int id) {
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
		if (!(obj instanceof MFJLabel))
			return false;
		MFJLabel other = (MFJLabel) obj;
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
	public int compareTo(MFJLabel o) {
		return Integer.compare(id, o.id);
	}
	
	public static int compare(MFJLabel o1, MFJLabel o2){
		if(o1 == null){
			if(o2 == null) return 0;
			else return -1;
		} else {
			if(o2 == null) return 1;
			else return o1.compareTo(o2);
		}
	}
}

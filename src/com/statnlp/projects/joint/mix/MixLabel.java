package com.statnlp.projects.joint.mix;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MixLabel implements Comparable<MixLabel>, Serializable{

	private static final long serialVersionUID = -7091637177930153915L;
	public static final Map<String, MixLabel> Labels = new HashMap<String, MixLabel>();
	public static final Map<Integer, MixLabel> Label_Index = new HashMap<Integer, MixLabel>();
	public static boolean locked = false;
	
	public static MixLabel get(String form){
		if(!Labels.containsKey(form)){
			if (locked) throw new RuntimeException("The label set is locked. cannot add more. ");
			MixLabel label = new MixLabel(form, Labels.size());
			Labels.put(form, label);
			Label_Index.put(label.id, label);
		}
		return Labels.get(form);
	}
	
	public static MixLabel get(int id){
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
	
	public MixLabel(String form, int id) {
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
		if (!(obj instanceof MixLabel))
			return false;
		MixLabel other = (MixLabel) obj;
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
	public int compareTo(MixLabel o) {
		return Integer.compare(id, o.id);
	}
	
	public static int compare(MixLabel o1, MixLabel o2){
		if(o1 == null){
			if(o2 == null) return 0;
			else return -1;
		} else {
			if(o2 == null) return 1;
			else return o1.compareTo(o2);
		}
	}

}

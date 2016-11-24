package com.statnlp.projects.dep.model.segdep;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Label implements Comparable<Label>, Serializable{
	
	private static final long serialVersionUID = -3314363044582374266L;
	public static final Map<String, Label> Labels = new HashMap<String, Label>();
	public static final Map<Integer, Label> Label_Index = new HashMap<Integer, Label>();
	public static boolean locked = false;
	
	public static Label get(String form){
		if(!Labels.containsKey(form)){
			if (locked) throw new RuntimeException("The label set is locked. cannot add more. ");
			Label label = new Label(form, Labels.size());
			Labels.put(form, label);
			Label_Index.put(label.id, label);
		}
		return Labels.get(form);
	}
	
	public static Label get(int id){
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
	
	private Label(String form, int id) {
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
		if (!(obj instanceof Label))
			return false;
		Label other = (Label) obj;
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
	public int compareTo(Label o) {
		return Integer.compare(id, o.id);
	}
	
	public static int compare(Label o1, Label o2){
		if(o1 == null){
			if(o2 == null) return 0;
			else return -1;
		} else {
			if(o2 == null) return 1;
			else return o1.compareTo(o2);
		}
	}
}

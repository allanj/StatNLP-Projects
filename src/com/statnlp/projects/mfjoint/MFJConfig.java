package com.statnlp.projects.mfjoint;

public class MFJConfig {

	
	public static enum MFJTASK{
		NER,
		PARING,
		JOINT;
	}
	
	public static enum STRUCT{
		SEMI,
		TREE;
	}
	
	public static enum DIR {left, right};
	public static enum COMP{incomp, comp};
	
	//OS
	public static boolean windows = false;
}

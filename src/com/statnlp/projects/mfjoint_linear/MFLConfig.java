package com.statnlp.projects.mfjoint_linear;

public class MFLConfig {

	
	public static enum MFLTASK{
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

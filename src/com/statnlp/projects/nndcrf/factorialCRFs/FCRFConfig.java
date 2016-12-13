package com.statnlp.projects.nndcrf.factorialCRFs;

public class FCRFConfig {

	public static String CONLL_train = "data/conll2000/train.txt";
	public static String CONLL_test = "data/conll2000/test.txt";
	public static String CONLL_dev = "data/conll2000/dev.txt";
	
	public static String nerOut = "data/conll2000/output/nerOut.txt";
	public static String posOut = "data/conll2000/output/posOut.txt";
	
	public static String nerPipeOut = "data/conll2000/output/nerPipeOut.txt";
	public static String posPipeOut = "data/conll2000/output/posPipeOut.txt";
	
	public static String dataType = "conll";
	
	public static double l2val = 0.01;
	
	public static boolean windows = false;
	
	public static enum TASK{
		CHUNKING,
		TAGGING,
		JOINT;
	}
}

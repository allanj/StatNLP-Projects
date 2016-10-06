package com.statnlp.projects.entity.dcrf;

public class DCRFConfig {

	
	public static String CONLL_train = "dcrf/dat/train.txt";
	public static String CONLL_test = "dcrf/dat/test.txt";
	public static String CONLL_dev = "dcrf/dat/dev.txt";
	
	public static String nerOut = "dcrf/dat/nerOut.txt";
	public static String nerRes = "dcrf/dat/nerRes.txt";
	public static String posOut = "dcrf/dat/posOut.txt";
	
	public static String dataType = "conll";
	
	public static double l2val = 0.01;
	
	public static boolean windows = false;
}

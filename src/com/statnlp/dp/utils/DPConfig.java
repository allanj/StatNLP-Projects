package com.statnlp.dp.utils;

public class DPConfig {

	public static String trainingPath = "data/semeval10t1/en.train.txt";
	public static String devPath = "data/semeval10t1/en.devel.txt";
	public static String testingPath = "data/semeval10t1/en.test.txt";
	
	public static String ecrftrain = "data/semeval10t1/ecrf.train.MISC.txt";
	public static String ecrfdev = "data/semeval10t1/ecrf.devel.MISC.txt";
	public static String ecrftest = "data/semeval10t1/ecrf.test.MISC.txt";
	
	public static String data_prefix = "data/semeval10t1/output/";
	
	public static String ner2dp_ner_dev_input = "data/semeval10t1/pptest/ecrf.dev.ner.res.txt";
	public static String ner2dp_ner_test_input = "data/semeval10t1/pptest/ecrf.test.ner.res.txt";
	public static String dp2ner_dp_dev_input = "data/semeval10t1/pptest/only.dev.dp.res.txt";
	public static String dp2ner_dp_test_input = "data/semeval10t1/pptest/only.test.dp.res.txt";
	
	public static String ner_res_suffix = ".ner.res.txt";
	public static String dp_res_suffix = ".dp.res.txt";
	
	public static String ner_eval_suffix =".ner.eval.txt"; 
	
	public static String joint_res_suffix = ".joint.res.txt";
	
	/**
	 * This rand seed only for reading the input
	 */
	public static long randSeed = 1000;
	
	public static double L2 = 0.7;
	
	public static String[] others = {"plant","fac","loc","product","location",
			"event","animal","law","game","language","norp","org","disease","substance"};
	
	public static String O_TYPE = "O";
	public static String MISC = "MISC";
	public static String E_B_PREFIX = "B-";
	public static String E_I_PREFIX = "I-";
	
	public static String PARENT_IS = "pae:";
	public static String OE = "OE";
	public static String ONE = "ONE";
	
	public static enum MODEL {UMMODEL,ADM,UMINC, RPM, HYPEREDGE, DIVIDED};
	
	public static boolean DEBUG = false;
	
	public static boolean windows = false;
	
}

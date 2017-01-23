package com.statnlp.projects.nndcrf.exactFCRF;

public class ExactConfig {

	public static String CONLL_train = "data/conll2000/train.txt";
	public static String CONLL_test = "data/conll2000/test.txt";
	public static String CONLL_dev = "data/conll2000/dev.txt";
	
	public static String exactOut = "data/conll2000/output/exactoutput.txt";
	
	public static String dataType = "conll";
	
	public static double l2val = 0.01;
	
	public static boolean windows = false;
	
	public static String EXACT_SEP = "\t";
	
	public static void concatExactLabel() {
		for (String chunk: ChunkLabel.CHUNKS.keySet()) {
			for (String pos : TagLabel.TAGS.keySet()) {
				String concat = chunk + "\t" + pos;
				ExactLabel.get(concat);
			}
		}
	}
}

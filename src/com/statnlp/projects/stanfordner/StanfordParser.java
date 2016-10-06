package com.statnlp.projects.stanfordner;

import edu.stanford.nlp.parser.nndep.DependencyParser;

public class StanfordParser {

	public StanfordParser() {
		// TODO Auto-generated constructor stub
	}
	
	
	/**
	 * -trainingThreads 6 -maxIter 415 -trainFile data/alldata/abc/train.conllx 
	 * -devFile data/alldata/abc/dev.conllx 
	 * -embedFile /Users/allanjie/embedding/glove.6B.50d.txt 
	 * -embeddingSize 50 
	 * -model models/nndep.model.txt.gz
	 * @param args
	 */
	public static void main(String[] args){
		DependencyParser.main(args);
	}

}

package com.statnlp.allan.ner;

import java.io.BufferedReader;
import java.io.IOException;

import com.statnlp.commons.crf.RAWF;

import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;

public class Test {

	public static void main(String[] args) throws IOException{
		BufferedReader br = RAWF.reader("F:/phd/data/ptb/ptb.deve_sec22.bracketed.txt");
		PennTreeReader ptr = new PennTreeReader(br);
		Tree tree  = null;
		int num = 0;
		while((tree = ptr.readTree())!=null){
			num++;
		}
		System.out.println("number of trees:"+num);
	}
}

package com.statnlp.projects.stanfordner;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;

public class NERReader {

	
	public static ArrayList<Sentence> readSentence(String corpus, int numSents){
		ArrayList<Sentence> sents = new ArrayList<Sentence>();
		String line = null;
		try{
			BufferedReader br = RAWF.reader(corpus);
			ArrayList<WordToken> tokens = new ArrayList<WordToken>();
			int sentNum = 1;
			while((line = br.readLine())!=null){
				if(line.startsWith("#")) continue;
				String[] values = line.split("\\t");
				if(values[0].contains("-")){
					System.err.println("Multiple index at "+sentNum+"th sentence");
					System.err.println("The line info is: "+line);
					continue;
				}
				if(values.length==1){
					WordToken[] tokenArr = new WordToken[tokens.size()];
					tokens.toArray(tokenArr);
					sents.add(new Sentence(tokenArr));
//					System.err.println(sents.get(sents.size()-1).toString());
					if(numSents>0 && sents.size()==numSents) break;
					tokens = new ArrayList<WordToken>();
					sentNum++;
					continue;
				}
				
				tokens.add(new WordToken(values[1])); //values[1] is the word form
			}
			br.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		return sents;
	}
	
	public static ArrayList<Sentence> readSentence(String corpus){
		return readSentence(corpus,-1);
		//-1 means unlimit
	}

	public static void main(String[] args) {
		//Check if the reader works
		readSentence("/Users/allanjie/Allan/data/udtreebank/universal-dependencies-1.2/UD_English/en-ud-train.conllu");
	}

}

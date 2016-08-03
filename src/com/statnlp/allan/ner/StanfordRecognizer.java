package com.statnlp.allan.ner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.TypedDependency;

public class StanfordRecognizer {

	private String parserModel; 
	
	private DependencyParser parser;
	
	public StanfordRecognizer(String model){
		this.parserModel = model;
	}
	
	
	
	public void loadParser(){
		System.err.println("Loadding Parsing model.....");
		
		if(this.parserModel.equals("")) throw new RuntimeException("No parser model can be loaded");
		try {
			parser = DependencyParser.loadFromModelFile(parserModel);
		} catch (ClassCastException e) {
			System.err.println("Exception while getting recognizer");
			e.printStackTrace();
		}
	}
	
	
	public void parseDependency(ArrayList<Sentence> sents, String output) throws IOException{
		System.err.println("Starting to parse the dependency...");
		PrintWriter pw = RAWF.writer(output);
		
		for(Sentence sent: sents){
			String[] oneSent = new String[sent.length()];
			for(int i=0;i<sent.length();i++){
				oneSent[i] = sent.get(i).getName();
			}
			List<CoreLabel> rawWords = edu.stanford.nlp.ling.Sentence.toCoreLabelList(oneSent);
			rawWords.get(0).setTag("NNP");
			rawWords.get(1).setTag("VBZ");
			rawWords.get(2).setTag("NNP");
			GrammaticalStructure gs = parser.predict(rawWords);
		    List<TypedDependency> tdl = (List<TypedDependency>) gs.typedDependenciesCollapsedTree();
		    HashMap<Integer, Integer> dependencyParent = new HashMap<Integer, Integer>();
		    for(TypedDependency td: tdl){
//		    	GrammaticalRelation gr = td.reln();
		    	dependencyParent.put(td.dep().index(), td.gov().index());
//		    	System.err.println(td.gov().index()+","+td.dep().index());
		    }
		    for(int i=0;i<sent.length();i++){
				oneSent[i] = sent.get(i).getName();
//				EntityWordToken ewt = (EntityWordToken)(sent.get(i));
				pw.write((i+1)+"\t"+sent.get(i).getName()+"\t"+dependencyParent.get(i+1)+"\n");
			}
//		    System.err.println("***Separator between sentence***");
		    pw.write("\n");
		}
		pw.close();
	}
	
	public ArrayList<Sentence> readSentence(String path){
		return this.readSentence(path,-1);
	}
	
	public ArrayList<Sentence> readSentence(String path, int number){
		System.err.println("Reading sentence into memory....");
		ArrayList<Sentence> data = new ArrayList<Sentence>();
		try {
			BufferedReader br = RAWF.reader(path);
			String line = null;
			ArrayList<WordToken> words = new ArrayList<WordToken>();
			while((line = br.readLine())!=null){
				if(line.equals("")){
					WordToken[] wordsArr = new WordToken[words.size()];
					words.toArray(wordsArr);
					Sentence sent = new Sentence(wordsArr);
					words = new ArrayList<WordToken>();
					data.add(sent);
					if(number!= -1 && data.size()==number) break;
					continue;
				}
				String[] values = line.split("\\t");
				int headIndex = Integer.valueOf(values[6]);
				String tag = values[4];
				words.add(new WordToken(values[1], tag,headIndex));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.err.println("Total:"+ data.size()+" Instance read by Recognizer. ");
		return data;
	}
	
	
	public static void main(String[] args) throws IOException{
		String stanfordDependencyModel = "edu/stanford/nlp/models/parser/nndep/english_UD.gz";
		
		StanfordRecognizer sr = new StanfordRecognizer(stanfordDependencyModel);
		sr.loadParser();
		//which us city has the highest population density ?
		WordToken w1 = new WordToken("which");
		WordToken w2 = new WordToken("love");
		WordToken w3 = new WordToken("Singapore");
		
		Sentence sent  = new Sentence(new WordToken[]{w1,w2,w3});
		ArrayList<Sentence> sents = new ArrayList<Sentence>();
		sents.add(sent);
		sr.parseDependency(sents, "stanford-data/output.txt");
		
	}

}

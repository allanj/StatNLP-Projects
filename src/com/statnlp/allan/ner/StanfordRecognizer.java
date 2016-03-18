package com.statnlp.allan.ner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;
import com.statnlp.dp.DependInstance;
import com.statnlp.dp.DependencyReader;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.trees.EnglishGrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;

public class StanfordRecognizer {

	private String classifierModel;
	private String parserModel; 
	
	private AbstractSequenceClassifier<CoreLabel> classifier;
	private LexicalizedParser parser;
	
	public StanfordRecognizer(String recognizer){
		this.classifierModel = recognizer;
		this.parserModel = "";
	}
	
	public StanfordRecognizer(String recognizer, String parserModel) {
		this.classifierModel = recognizer;
		this.parserModel = parserModel;
	}
	
	public void loadClassifierModel(){
		System.err.println("Loadding Classifier model.....");
		try {
			classifier = CRFClassifier.getClassifier(classifierModel);
		} catch (ClassCastException | ClassNotFoundException | IOException e) {
			System.err.println("Exception while getting recognizer");
			e.printStackTrace();
		}
	}
	
	public void loadParser(){
		System.err.println("Loadding Parsing model.....");
		if(this.parserModel.equals("")) throw new RuntimeException("No parser model can be loaded");
		try {
			parser = LexicalizedParser.loadModel(parserModel);
		} catch (ClassCastException e) {
			System.err.println("Exception while getting recognizer");
			e.printStackTrace();
		}
	}
	
	public void recognizeEntity(ArrayList<Sentence> sents){
		System.err.println("Start to recognize the entity.....");
		int num = 0;
		for(Sentence sent: sents){
			StringBuilder sb = new StringBuilder();
			sb.append(sent.get(0).getName());
			for(int i=1;i<sent.length();i++){
				sb.append(" "+sent.get(i).getName());
			}
			String res = classifier.classifyToString(sb.toString(), "tsv", true);// return a whole lines with \n
			String[] values = res.split("\n");
//			System.err.println(res.toString());
			if(sent.length()==values.length){
				num++;
				for(int i=0;i<values.length;i++){
					values[i] = values[i].trim();
					String[] pairs = values[i].split("\t");
					if(pairs.length<=1) throw new RuntimeException("The pair length is no more than 1");
					sent.get(i).setEntity(pairs[1]);
				}
				sent.setRecognized();;
			}else sent.setUnRecognized();
		}
		System.err.println("The total number of sentence with equal length: "+num+" sentence");
	}
	
	public void parseDependency(ArrayList<Sentence> sents, String file, String modelFile) throws IOException{
		System.err.println("Starting to parse the dependency...");
		PrintWriter pw = RAWF.writer(file);
		DependencyParser dparser = DependencyParser.loadFromModelFile(modelFile);
		for(Sentence sent: sents){
			String[] oneSent = new String[sent.length()];
			for(int i=0;i<sent.length();i++){
				oneSent[i] = sent.get(i).getName();
			}
			List<CoreLabel> rawWords = edu.stanford.nlp.ling.Sentence.toCoreLabelList(oneSent);
			Tree parse = parser.apply(rawWords);
			List<Label> yieldLabels = parse.preTerminalYield();
//			System.err.println(yieldLabels.toString());
			for(int i=0;i<rawWords.size();i++){
				CoreLabel cl = rawWords.get(i);
				CoreLabel leafYield = (CoreLabel)yieldLabels.get(i);
//				System.err.println(leafLabel.value()+":"+leafYield.tag()+":"+leafYield.value());
				cl.setTag(leafYield.tag());
			}
			dparser.predict(rawWords);
//			TreebankLanguagePack tlp = parser.treebankLanguagePack(); // PennTreebankLanguagePack for English
//			
//		    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
//		    GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
		    
		    EnglishGrammaticalStructure gs = new EnglishGrammaticalStructure(parse);
		    
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
	
	
	public void postProcess(String path, String result) throws IOException{
		System.err.println("Post processing the file: removing the words that have null parent dependency...");
		BufferedReader br = RAWF.reader(path);
		PrintWriter pw = RAWF.writer(result);
		String line = null;
		ArrayList<CoreLabel> words = new ArrayList<CoreLabel>();
		while((line = br.readLine())!=null){
			if(line.equals("")){
				//postprocesHere
				for(int i=0;i<words.size();i++){
					CoreLabel label = words.get(i);
					int sentIndex = i+1;
					if(label.index()==-1){
						for(int j=0;j<words.size();j++){
							CoreLabel jlabel = words.get(j);
							int jLabelParent = jlabel.index();
							if(jLabelParent > sentIndex)
								jlabel.setIndex(jLabelParent-1);
						}
						words.remove(i);
						i=0;
					}
					
				}
				for(int i=0;i<words.size();i++){
					CoreLabel label = words.get(i);
					pw.write((i+1)+"\t"+label.value()+"\t"+label.index()+"\n");
				}
				words = new ArrayList<CoreLabel>();
				pw.write("\n");
				continue;
			}
			String[] values = line.split("\\t");
			CoreLabel label = new CoreLabel();
			label.setValue(values[1]);
//			label.setSentIndex(Integer.valueOf(values[0]));
			if(values[2].equals("null"))
				label.setIndex(-1); // this one is the dependency parent
			else label.setIndex(Integer.valueOf(values[2]));
			words.add(label);
		}
		br.close();
		pw.close();
	}
	
	public void parseDep(String model, String trainingFile, String testingFile){
		DependencyParser dparser = new DependencyParser(new Properties());
		dparser.train(trainingFile, model);
		dparser.loadModelFile(model);
		DependInstance[] testingInsts = DependencyReader.readInstance(testingFile, false,-1);
		int corr = 0;
		int total = 0;
		for(int i=0;i<testingInsts.length;i++){
			Sentence sent = testingInsts[i].getInput();
			String[] oneSent = new String[sent.length()-1];
			for(int j=1;j<sent.length();j++){
				oneSent[j-1] = sent.get(j).getName();
			}
			List<CoreLabel> rawWords = edu.stanford.nlp.ling.Sentence.toCoreLabelList(oneSent);
			for(int j=0;j<rawWords.size();j++){
				CoreLabel cl = rawWords.get(j);
//				System.err.println(leafLabel.value()+":"+leafYield.tag()+":"+leafYield.value());
				cl.setTag(sent.get(j+1).getTag());
			}
			GrammaticalStructure gs = dparser.predict(rawWords);
			List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
			HashMap<Integer, Integer> dependencyParent = new HashMap<Integer, Integer>();
			for(TypedDependency td: tdl){
		    	dependencyParent.put(td.dep().index(), td.gov().index());
		    }
			for(int j=1;j<sent.length();j++){
				if(sent.get(j).getHeadIndex()==dependencyParent.get(j).intValue())
					corr++;
				total++;
			}
		}
		System.err.println("Correct: "+corr);
		System.err.println("total: "+total);
		System.err.println("Precision: "+corr*1.0/total);
	}
	
	
	public static void main(String[] args) throws IOException{
		String entityClassifierModel = "recognizer/english.muc.7class.distsim.crf.ser.gz";
		String pcfgModel = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
		String stanfordDependencyModel = "edu/stanford/nlp/models/parser/nndep/english_UD.gz";
		
		String universalTraining = "/Users/allanjie/Allan/data/udtreebank/universal-dependencies-1.2/UD_English/en-ud-train.conllu";
		String universalTesting = "/Users/allanjie/Allan/data/udtreebank/universal-dependencies-1.2/UD_English/en-ud-test.conllu";
		
		String stanfordTraining = "data/dependency/UD_English/smalltest.conll";
		String stanfordPostProcess = "data/dependency/UD_English/stanford-train-postp-1.conllu";
		

		String stanfordModel = "data/dependency/UD_English/sfmodel";
		String testingPath = "data/dependency/UD_English/en-ud-test.conllu";
		
		StanfordRecognizer sr = new StanfordRecognizer(entityClassifierModel,pcfgModel);
		sr.loadClassifierModel();
		ArrayList<Sentence> orgSents = NERReader.readSentence(universalTraining,2);
		sr.recognizeEntity(orgSents);
		
//		ArrayList<Sentence> sents2 = sr.readSentence(universalTraining);
//		sr.parseDependency(sents2,stanfordTraining,stanfordDependencyModel);
//		sr.postProcess(stanfordTraining, stanfordPostProcess);
//		sr.parseDep(stanfordModel, stanfordTraining, testingPath);
		
	}

}

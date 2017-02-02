package com.statnlp.projects.nndcrf;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.statnlp.commons.io.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;
import com.statnlp.projects.nndcrf.factorialCRFs.FCRFEval;
import com.statnlp.projects.nndcrf.factorialCRFs.FCRFInstance;

public class Utils {

	
	public static void readEvalResults(String chunkOutput, String posOutput) throws IOException{
		BufferedReader br = RAWF.reader(chunkOutput);
		String line = null;
		List<FCRFInstance> insts = new ArrayList<FCRFInstance>();
		int index = 1;
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		ArrayList<String> es = new ArrayList<String>();
		ArrayList<String> chunkPrediction = new ArrayList<String>();
		while((line = br.readLine())!=null){
			if(line.equals("")){
				WordToken[] wordsArr = new WordToken[words.size()];
				words.toArray(wordsArr);
				Sentence sent = new Sentence(wordsArr);
				FCRFInstance inst = new FCRFInstance(index++,1.0,sent);
				inst.setChunks(es);
				inst.setChunkPredictons(chunkPrediction);
				words = new ArrayList<WordToken>();
				es = new ArrayList<String>();
				chunkPrediction = new ArrayList<String>();
				inst.setUnlabeled();
				insts.add(inst);
				continue;
			}
			String[] values = line.split(" ");
			String predChunk = values[3]; 
			String corrChunk = values[2];
			String pos = values[1];
			String word = values[0];
			
			words.add(new WordToken(word, pos, -1, corrChunk));
			es.add(corrChunk);
			chunkPrediction.add(predChunk);
		}
		br.close();
		//reading tag files
		br = RAWF.reader(posOutput);
		int idx = 0;
		ArrayList<String> tagPrediction = new ArrayList<String>();
		FCRFInstance currInst = insts.get(idx);
		int wIdx = 0;
		while((line = br.readLine())!=null){
			if(line.equals("")){
				currInst.setTagPredictons(tagPrediction);
				tagPrediction = new ArrayList<String>();
				idx++;
				if (idx != insts.size())
					currInst = insts.get(idx);
				wIdx = 0;
				continue;
			}
			String[] values = line.split(" ");
			String corrPOS = values[1];
			String predPOS = values[2];
			tagPrediction.add(predPOS);
			currInst.getInput().get(wIdx).setTag(corrPOS);
			wIdx++;
		}
		br.close();
		
		List<FCRFInstance> myInsts = insts;
		String type = "Testing";
		System.err.println(type+" instance, total:"+ myInsts.size()+" Instance. ");
		FCRFInstance[] testInsts = new FCRFInstance[insts.size()];
		insts.toArray(testInsts);
		FCRFEval.evalFscore(testInsts, "data/conll2000/output//test.txt");
		FCRFEval.evalChunkAcc(testInsts);
		FCRFEval.evalPOSAcc(testInsts, "data/conll2000/output//testpos.txt");
		FCRFEval.evalJointAcc(testInsts);
	}
	
	private static Set<String> setOfWords(String dataFile) throws IOException {
		BufferedReader br = RAWF.reader(dataFile);
		Set<String> set = new HashSet<String>();
		String line = null;
		while((line = br.readLine())!=null){
			if(line.equals("")){
				continue;
			}
			String[] values = line.split(" ");
			String word = values[0];
			set.add(word.toLowerCase());
		}
		br.close();
		return set;
	}
	
	
	public static void checkCoverageRate(String gloveFile, String trainFile, String testFile) throws IOException {
		Set<String> trainWords = setOfWords(trainFile);
		Set<String> testWords = setOfWords(testFile);
		trainWords.addAll(testWords);
		Set<String> dataWords = trainWords;
		Set<String> gloveWords = setOfWords(gloveFile);
		
		Set<String> intersection = new HashSet<String>(dataWords);
		intersection.retainAll(gloveWords);
		System.out.println("Coverage: " + (intersection.size()*1.0)/dataWords.size());
		
	}
	
	public static void main(String[] args) throws IOException{
		checkCoverageRate("F:/phd/data/glove.6B/glove.6B.50d.txt","data/conll2000/train.txt", "data/conll2000/test.txt");
//		String prefix = "F:/Dropbox/SUTD/ACL2017/mfexperiments/outputdata/";
//		prefix = "data/conll2000/output/";
//		readEvalResults(prefix+"nerPipeOut.txt", prefix+"posPipeOut.txt");
	}
} 

package com.statnlp.projects.nndcrf.exactFCRF;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.statnlp.commons.io.RAWF;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;

public class ExactEval {

	
	/**
	 * 
	 * @param testInsts
	 * @param nerOut: word, true pos, true entity, pred entity
	 * @throws IOException
	 */
	public static void evalFscore(Instance[] testInsts, String nerOut) throws IOException{
		PrintWriter pw = RAWF.writer(nerOut);
		for(int index=0;index<testInsts.length;index++){
			ExactInstance eInst = (ExactInstance)testInsts[index];
			ArrayList<String> prediction = eInst.getPrediction();
			ArrayList<String> gold = eInst.getOutput();
			Sentence sent = eInst.getInput();
			for(int i=0;i<sent.length();i++){
				String[] predVals = prediction.get(i).split(ExactConfig.EXACT_SEP);
				String[] goldVals = gold.get(i).split(ExactConfig.EXACT_SEP);
				pw.write(sent.get(i).getName()+" "+sent.get(i).getTag()+" "+goldVals[0]+" "+predVals[0]+"\n");
			}
			pw.write("\n");
		}
		pw.close();
		evalFscore(nerOut);
	}
	
	
	private static void evalFscore(String outputFile) throws IOException{
		try{
			System.err.println("perl data/semeval10t1/conlleval.pl < "+outputFile);
			ProcessBuilder pb = null;
			if(ExactConfig.windows){
				pb = new ProcessBuilder("D:/Perl64/bin/perl","E:/Framework/data/semeval10t1/conlleval.pl"); 
			}else{
				pb = new ProcessBuilder("eval/conlleval.pl"); 
			}
			pb.redirectInput(new File(outputFile));
			pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
			pb.redirectError(ProcessBuilder.Redirect.INHERIT);
			pb.start();
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	/**
	 * Evaluation of POS tagging result
	 * @param testInsts
	 * @param posOut: the output of the pos file: word, trueTag, predTag, trueChunk
	 * @throws IOException
	 */
	public static void evalPOSAcc(Instance[] testInsts) throws IOException{
		int corr = 0;
		int total = 0;
		for(int index=0;index<testInsts.length;index++){
			ExactInstance eInst = (ExactInstance)testInsts[index];
			ArrayList<String> prediction = eInst.getPrediction();
			ArrayList<String> gold = eInst.getOutput();
			Sentence sent = eInst.getInput();
			for(int i=0;i<sent.length();i++){
				String[] predVals = prediction.get(i).split(ExactConfig.EXACT_SEP);
				String[] goldVals = gold.get(i).split(ExactConfig.EXACT_SEP);
				if(goldVals[1].equals(predVals[1]))
					corr++;
				total++;
			}
		}
		System.out.printf("[POS Accuracy]: %.2f%%\n", corr*1.0/total*100);
	}
	
	public static void evalJointAcc(Instance[] testInsts, String output) throws IOException{
		int corr = 0;
		int total = 0;
		PrintWriter pw = RAWF.writer(output);
		for(int index=0;index<testInsts.length;index++){
			ExactInstance eInst = (ExactInstance)testInsts[index];
			ArrayList<String> prediction = eInst.getPrediction();
			ArrayList<String> gold = eInst.getOutput();
			Sentence sent = eInst.getInput();
			for(int i = 0; i < prediction.size();i++){
				if(prediction.get(i).equals(gold.get(i)))
					corr++;
				total++;
				String[] vals = prediction.get(i).split(ExactConfig.EXACT_SEP);
				String chunk = vals[0];
				String pos = vals[1];
				pw.write(sent.get(i).getName()+" "+pos+" "+chunk+"\n");
			}
			pw.println();
		}
		pw.close();
		System.out.printf("[Joint Accuracy]: %.2f%%\n", corr*1.0/total*100);
	}
	
}

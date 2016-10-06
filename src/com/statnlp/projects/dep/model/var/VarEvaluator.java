package com.statnlp.projects.dep.model.var;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.projects.dep.utils.DPConfig;

public class VarEvaluator {

	public static boolean DEBUG = false;

	/**
	 * Evaluate the dependency
	 * @param testInsts
	 * @param dpOut, index \t word \t tag \t true entity \t trueHead \t predHead
	 * @throws IOException
	 */
	public static void evalDP(Instance[] testInsts, String dpOut) throws IOException{
		int dp_corr=0;
		int dp_total=0;
		PrintWriter pw = RAWF.writer(dpOut);
		for(Instance org_inst: testInsts){
			VarInstance inst = (VarInstance)org_inst;
			Sentence sent = inst.getInput();
			int[] predHeads = inst.getDepOutput();
			int[] trueHeads = inst.getDepPrediction();
			for(int i=1;i<predHeads.length;i++){
				if(predHeads[i]==trueHeads[i])
					dp_corr++;
				dp_total++;
				pw.write(i+" "+sent.get(i).getName()+" "+sent.get(i).getTag()+" "+sent.get(i).getEntity()+" "+trueHeads[i]+" "+predHeads[i]+"\n");
			}
			pw.write("\n");
		}
		pw.close();
		System.out.println("**Evaluating Dependency Result**");
		System.out.println("[Dependency] Correct: "+dp_corr);
		System.out.println("[Dependency] total: "+dp_total);
		System.out.println("[Dependency] UAS: "+dp_corr*1.0/dp_total);
		System.out.println("*************************");
	}
	
	
	/**
	 * 
	 * @param testInsts
	 * @param nerOut: word, true pos, true entity, pred entity
	 * @throws IOException
	 */
	public static void evalNER(Instance[] testInsts, String nerOut) throws IOException{
		PrintWriter pw = RAWF.writer(nerOut);
		for(int index=0;index<testInsts.length;index++){
			VarInstance inst = (VarInstance)testInsts[index];
			String[] predEntities = inst.getEntityPrediction();
			Sentence sent = inst.getInput();
			for(int i=1;i<sent.length();i++){
				pw.write(sent.get(i).getName()+" "+sent.get(i).getTag()+" "+sent.get(i).getEntity()+" "+predEntities[i]+"\n");
			}
			pw.write("\n");
		}
		pw.close();
		evalNER(nerOut);
	}
	
	
	protected static void evalNER(String outputFile) throws IOException{
		try{
			System.out.println("perl data/semeval10t1/conlleval.pl < "+outputFile);
			ProcessBuilder pb = null;
			if(DPConfig.windows){
				pb = new ProcessBuilder("D:/Perl64/bin/perl","E:/Framework/data/semeval10t1/conlleval.pl"); 
			}else{
				pb = new ProcessBuilder("data/semeval10t1/conlleval.pl"); 
			}
			pb.redirectInput(new File(outputFile));
			pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
			pb.redirectError(ProcessBuilder.Redirect.INHERIT);
			pb.start();
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	
	
	public static void writeJointResult(Instance[] predictions, String jointResult, String modelType) throws IOException{
		PrintWriter pw = RAWF.writer(jointResult);
		PrintWriter pwDebug = null;
		if(DEBUG) {
			System.err.println("[Evaluator DEBUG]");
			pwDebug = RAWF.writer("data/debug/eval.bug");
		}
		for(int index=0;index<predictions.length;index++){
			VarInstance dInst = (VarInstance)predictions[index];
			Sentence sent = dInst.getInput();
			int[] predHeads = dInst.getDepOutput();
			int[] trueHeads = dInst.getDepPrediction();
			String[] predEntities = dInst.getEntityPrediction();
			for(int i=1;i<sent.length();i++){
				pw.write(i+" "+sent.get(i).getName()+" "+sent.get(i).getTag()+" "+sent.get(i).getEntity()+" "+predEntities[i]+" "+trueHeads[i]+" "+predHeads[i]+"\n");
			}
			pw.write("\n");
		}
		if(DEBUG) pwDebug.close();
		pw.close();
	}
	
	
	
	
	public static void main(String[] args) throws IOException{
		evalNER("data/semeval10t1/ecrf.dev.ner.res.txt");
	}

	
}

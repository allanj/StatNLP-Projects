package com.statnlp.dp.model.mhp;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.Transformer;
import com.statnlp.dp.utils.DPConfig;

import edu.stanford.nlp.trees.UnnamedDependency;

public class MHPEvaluator {

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
			MHPInstance inst = (MHPInstance)org_inst;
			Sentence sent = inst.getInput();
			ArrayList<UnnamedDependency> predDependencies = inst.toDependencies(inst.getPrediction());
			ArrayList<UnnamedDependency> corrDependencies = inst.getDependencies();
			int[] predHeads = Transformer.getHeads(predDependencies, inst.getInput());
			int[] trueHeads = Transformer.getHeads(corrDependencies, inst.getInput());
			for(int i=1;i<predHeads.length;i++){
				if(predHeads[i]==trueHeads[i])
					dp_corr++;
				dp_total++;
				pw.write(i+" "+sent.get(i).getName()+" "+sent.get(i).getTag()+" "+sent.get(i).getEntity()+" "+trueHeads[i]+" "+predHeads[i]+"\n");
			}
			pw.write("\n");
		}
		pw.close();
		System.err.println("****");
		System.err.println("[Dependency] Correct: "+dp_corr);
		System.err.println("[Dependency] total: "+dp_total);
		System.err.println("[Dependency] UAS: "+dp_corr*1.0/dp_total);
		System.err.println("****");
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
			Instance inst = testInsts[index];
			MHPInstance dInst = (MHPInstance)inst;
			String[] predEntities = dInst.toEntities(dInst.getPrediction());
			Sentence sent = dInst.getInput();
			for(int i=1;i<sent.length();i++){
				pw.write(sent.get(i).getName()+" "+sent.get(i).getTag()+" "+sent.get(i).getEntity()+" "+predEntities[i]+"\n");
			}
			pw.write("\n");
		}
		pw.close();
		evalNER(nerOut);
	}
	
	
	public static void evalNER(String outputFile) throws IOException{
		try{
			System.err.println("perl data/semeval10t1/conlleval.pl < "+outputFile);
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
	
	public static void writeNERResult(Instance[] predictions, String nerResult, boolean isNERInstance) throws IOException{
		PrintWriter pw = RAWF.writer(nerResult);
		for(int index=0;index<predictions.length;index++){
			MHPInstance dInst = (MHPInstance)predictions[index];
			String[] predEntities = dInst.toEntities(dInst.getPrediction());
			Sentence sent = dInst.getInput();
			for(int i=1;i<sent.length();i++){
				pw.write(i+" "+sent.get(i).getName()+" "+sent.get(i).getTag()+" "+sent.get(i).getEntity()+" "+predEntities[i]+" "+sent.get(i).getHeadIndex()+"\n");
			}
			pw.write("\n");
		}
		
		pw.close();
	}
	
	
	public static void writeJointResult(Instance[] predictions, String jointResult, String modelType) throws IOException{
		PrintWriter pw = RAWF.writer(jointResult);
		PrintWriter pwDebug = null;
		if(DEBUG) {
			System.err.println("[Evaluator DEBUG]");
			pwDebug = RAWF.writer("data/debug/eval.bug");
		}
		for(int index=0;index<predictions.length;index++){
			MHPInstance dInst = (MHPInstance)predictions[index];
			Sentence sent = dInst.getInput();
			ArrayList<UnnamedDependency> predDependencies = dInst.toDependencies(dInst.getPrediction());
			ArrayList<UnnamedDependency> corrDependencies = dInst.getDependencies();
			int[] predHeads = Transformer.getHeads(predDependencies, dInst.getInput());
			int[] trueHeads = Transformer.getHeads(corrDependencies, dInst.getInput());
			String[] predEntities = null;
			predEntities = dInst.toEntities(dInst.getPrediction());
			for(int i=1;i<sent.length();i++){
				pw.write(i+" "+sent.get(i).getName()+" "+sent.get(i).getTag()+" "+sent.get(i).getEntity()+" "+predEntities[i]+" "+trueHeads[i]+" "+predHeads[i]+"\n");
			}
			pw.write("\n");
			if(DEBUG){
				if(dInst.resCovered){
					pwDebug.write("instance Id:"+dInst.getInstanceId()+"\n" );
					pwDebug.write(dInst.getInput().toString()+"\n" );
					pwDebug.write(Arrays.toString(predEntities) + "\n" );
					pwDebug.write(dInst.getPrediction().pennString()+"\n" );
					pwDebug.write("***************Splitting Line*************** \n" );
				}
			}
		}
		if(DEBUG) pwDebug.close();
		pw.close();
	}
	
	public static void main(String[] args) throws IOException{
		evalNER("data/semeval10t1/ecrf.dev.ner.res.txt");
	}
}

package com.statnlp.dp;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.utils.DPConfig;
import com.statnlp.entity.lcr.ECRFInstance;

import edu.stanford.nlp.trees.UnnamedDependency;

public class Evaluator {

	

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
			DependInstance inst = (DependInstance)org_inst;
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
			if(inst instanceof ECRFInstance){
				ECRFInstance eInst = (ECRFInstance)inst;
				ArrayList<String> predEntities = eInst.getPrediction();
				ArrayList<String> trueEntities = eInst.getOutput();
				Sentence sent = eInst.getInput();
				for(int i=0;i<sent.length();i++){
					pw.write(sent.get(i).getName()+" "+sent.get(i).getTag()+" "+trueEntities.get(i)+" "+predEntities.get(i)+"\n");
				}
			}else{
				DependInstance dInst = (DependInstance)inst;
				String[] predEntities = dInst.toEntities(dInst.getPrediction());
				Sentence sent = dInst.getInput();
				for(int i=1;i<sent.length();i++){
					pw.write(sent.get(i).getName()+" "+sent.get(i).getTag()+" "+sent.get(i).getEntity()+" "+predEntities[i]+"\n");
				}
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
		if(isNERInstance){
			for(int index=0;index<predictions.length;index++){
				Instance inst = predictions[index];
				ECRFInstance eInst = (ECRFInstance)inst;
				ArrayList<String> predEntities = eInst.getPrediction();
				ArrayList<String> trueEntities = eInst.getOutput();
				Sentence sent = eInst.getInput();
				for(int i=0;i<sent.length();i++){
					int headIndex = sent.get(i).getHeadIndex()+1;
					pw.write((i+1)+" "+sent.get(i).getName()+" "+sent.get(i).getTag()+" "+trueEntities.get(i)+" "+predEntities.get(i)+" "+headIndex+"\n");
				}
				pw.write("\n");
			}
		}else{
			for(int index=0;index<predictions.length;index++){
				DependInstance dInst = (DependInstance)predictions[index];
				String[] predEntities = dInst.toEntities(dInst.getPrediction());
				Sentence sent = dInst.getInput();
				for(int i=1;i<sent.length();i++){
					pw.write(i+" "+sent.get(i).getName()+" "+sent.get(i).getTag()+" "+sent.get(i).getEntity()+" "+predEntities[i]+" "+sent.get(i).getHeadIndex()+"\n");
				}
				pw.write("\n");
			}
		}
		
		pw.close();
	}
	
	
	public static void writeJointResult(Instance[] predictions, String jointResult, String modelType) throws IOException{
		PrintWriter pw = RAWF.writer(jointResult);
		for(int index=0;index<predictions.length;index++){
			DependInstance dInst = (DependInstance)predictions[index];
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
			if(DPConfig.DEBUG){
				pw.write("## predicted span tree:\n");
				pw.write(dInst.getPrediction().pennString()+"\n");
				pw.write("## true span tree:\n");
				pw.write(dInst.getOutput().pennString()+"\n");
			}
		}
		pw.close();
	}
	
	public static void main(String[] args) throws IOException{
		evalNER("data/semeval10t1/ecrf.dev.ner.res.txt");
	}

	
}

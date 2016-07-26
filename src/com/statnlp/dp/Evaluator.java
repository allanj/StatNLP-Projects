package com.statnlp.dp;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.utils.DPConfig;
import com.statnlp.hybridnetworks.NetworkConfig;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.UnnamedDependency;

public class Evaluator {

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
			ModelInstance inst = (ModelInstance)org_inst;
			Sentence sent = inst.getInput();
			ArrayList<UnnamedDependency> predDependencies = inst.toDependencies(inst.getPrediction());
			ArrayList<UnnamedDependency> corrDependencies = inst.toDependencies(inst.getOutput());
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
		System.out.println("**Evaluating Dependency Result**");
		System.out.println("[Dependency] Correct: "+dp_corr);
		System.out.println("[Dependency] total: "+dp_total);
		System.out.println("[Dependency] UAS: "+dp_corr*1.0/dp_total);
		System.out.println("*************************");
	}
	
	/**
	 * Output the topK dependency structures, together with weight as well
	 * @param testInsts
	 * @param dpOut
	 * @throws IOException
	 */
	public static void outputTopKDep(Instance[] testInsts, String depOut) throws IOException{
		PrintWriter pw = RAWF.writer(depOut);
		for(Instance org_inst: testInsts){
			DependInstance inst = (DependInstance)org_inst;
			Sentence sent = inst.getInput();
			Tree[] topK = inst.getTopKPrediction();
			ArrayList<UnnamedDependency> corrDependencies = inst.toDependencies(inst.getOutput());
			int[] trueHeads = Transformer.getHeads(corrDependencies, inst.getInput());
			for(Tree prediction: topK){
				if(prediction==null) break;
				pw.write("[InstanceId+Weight]:"+inst.getInstanceId()+":"+prediction.score()+"\n");
				ArrayList<UnnamedDependency> predDependencies = inst.toDependencies(prediction);
				int[] predHeads = Transformer.getHeads(predDependencies, inst.getInput());
				for(int i=1;i<predHeads.length;i++){
					pw.write(i+" "+sent.get(i).getName()+" "+sent.get(i).getTag()+" "+sent.get(i).getEntity()+" "+trueHeads[i]+" "+predHeads[i]+"\n");
				}
				pw.write("\n");
			}
			
		}
		pw.close();
	}
	
	public static void evalLabelledDP(Instance[] testInsts, String depLabOut) throws IOException{
		int dp_corr=0;
		int dp_total=0;
		PrintWriter pw = RAWF.writer(depLabOut);
		for(Instance org_inst: testInsts){
			ModelInstance inst = (ModelInstance)org_inst;
			Sentence sent = inst.getInput();
			ArrayList<UnnamedDependency> predDependencies = inst.toDependencies(inst.getPrediction());
			ArrayList<UnnamedDependency> corrDependencies = inst.toDependencies(inst.getOutput());
			int[] predHeads = Transformer.getHeads(predDependencies, inst.getInput());
			int[] trueHeads = Transformer.getHeads(corrDependencies, inst.getInput());
			String[] predLabs = Transformer.getDepLabel(predDependencies, sent);
			String[] trueLabs = Transformer.getDepLabel(corrDependencies, sent);
			for(int i=1;i<predHeads.length;i++){
				if(predHeads[i]==trueHeads[i] && predLabs[i].equals(trueLabs[i]))
					dp_corr++;
				dp_total++;
				pw.write(i+" "+sent.get(i).getName()+" "+sent.get(i).getTag()+" "+sent.get(i).getEntity()+" "+trueHeads[i]+" "+predHeads[i]+" "+trueLabs[i]+" "+predLabs[i]+"\n");
			}
			pw.write("\n");
		}
		pw.close();
		System.out.println("****");
		System.out.println("[Labeled Dependency] Correct: "+dp_corr);
		System.out.println("[Labeled Dependency] total: "+dp_total);
		System.out.println("[Labeled Dependency] LAS: "+dp_corr*1.0/dp_total);
		System.out.println("****");
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
			ModelInstance inst = (ModelInstance)testInsts[index];
			String[] predEntities = inst.toEntities(inst.getPrediction());
			if(NetworkConfig._MAX_MARGINAL)
				predEntities = inst.getPredEntities();
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
			ModelInstance dInst = (ModelInstance)predictions[index];
			Sentence sent = dInst.getInput();
			ArrayList<UnnamedDependency> predDependencies = dInst.toDependencies(dInst.getPrediction());
			ArrayList<UnnamedDependency> corrDependencies = dInst.toDependencies(dInst.getOutput());
			int[] predHeads = Transformer.getHeads(predDependencies, dInst.getInput());
			int[] trueHeads = Transformer.getHeads(corrDependencies, dInst.getInput());
			String[] predEntities = null;
			predEntities = dInst.toEntities(dInst.getPrediction());
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

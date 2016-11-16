package com.statnlp.projects.dep;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.statnlp.commons.io.RAWF;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.projects.dep.utils.DPConfig;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.UnnamedDependency;

public class Evaluator {

	public static boolean DEBUG = false;

	
	public static void evalDP(Instance[] testInsts, String dpOut) throws IOException{
		evalDP(testInsts, dpOut, false);
	}
	
	/**
	 * Evaluate the dependency
	 * @param testInsts
	 * @param dpOut, index \t word \t tag \t true entity \t trueHead \t predHead
	 * @throws IOException
	 */
	public static void evalDP(Instance[] testInsts, String dpOut, boolean labeledDep) throws IOException{
		int dp_corr=0;
		int dp_total=0;
		int las_corr = 0;
		int lastGlobalId = Integer.MIN_VALUE;
		double max = Double.NEGATIVE_INFINITY;
		int bestId = -1;
		PrintWriter pw = RAWF.writer(dpOut);
		DependInstance bestInst = null;
		for(int index=0; index<testInsts.length; index++){
			DependInstance inst = (DependInstance)(testInsts[index]);
			int globalId = inst.getGlobalId();
			Tree prediction = inst.getPrediction();
			Sentence sent = inst.getInput();
			if(globalId!=-1){
				if(globalId==lastGlobalId){
					bestId = max > prediction.score()? bestId:inst.getInstanceId();
					max = max > prediction.score()? max:prediction.score();
					bestInst = max > prediction.score()?  bestInst:inst;
				}else{
					if(lastGlobalId != Integer.MIN_VALUE){
						Sentence bestSent = bestInst.getInput();
						ArrayList<UnnamedDependency> predDependencies = bestInst.toDependencies(bestInst.getPrediction());
						ArrayList<UnnamedDependency> corrDependencies = bestInst.toDependencies(bestInst.getOutput());
						int[] predHeads = Transformer.getHeads(predDependencies, bestInst.getInput());
						int[] trueHeads = Transformer.getHeads(corrDependencies, bestInst.getInput());
						for(int i=1;i<predHeads.length;i++){
							if(predHeads[i]==trueHeads[i])
								dp_corr++;
							dp_total++;
							pw.write(i+" "+bestSent.get(i).getName()+" "+bestSent.get(i).getTag()+" "+bestSent.get(i).getEntity()+" "+trueHeads[i]+" "+predHeads[i]+"\n");
						}
						pw.write("\n");
					}
					bestId = inst.getInstanceId();
					max = inst.getPrediction().score();
					bestInst = inst;
					lastGlobalId = globalId;
				}
				if(index==testInsts.length-1){
					Sentence bestSent = bestInst.getInput();
					ArrayList<UnnamedDependency> predDependencies = bestInst.toDependencies(bestInst.getPrediction());
					ArrayList<UnnamedDependency> corrDependencies = bestInst.toDependencies(bestInst.getOutput());
					int[] predHeads = Transformer.getHeads(predDependencies, bestInst.getInput());
					int[] trueHeads = Transformer.getHeads(corrDependencies, bestInst.getInput());
					for(int i=1;i<predHeads.length;i++){
						if(predHeads[i]==trueHeads[i])
							dp_corr++;
						dp_total++;
						pw.write(i+" "+bestSent.get(i).getName()+" "+bestSent.get(i).getTag()+" "+bestSent.get(i).getEntity()+" "+trueHeads[i]+" "+predHeads[i]+"\n");
					}
					pw.write("\n");
				}
				
			}else{
				ArrayList<UnnamedDependency> predDependencies = inst.toDependencies(prediction);
				int[] predHeads = Transformer.getHeads(predDependencies, inst.getInput());
				String[] predLabels = labeledDep? Transformer.getDepLabel(predDependencies, inst.getInput()): null;
				for(int i=1;i<predHeads.length;i++){
					if(predHeads[i]==sent.get(i).getHeadIndex()){
						dp_corr++;
						if(labeledDep && predLabels[i].equals(sent.get(i).getDepLabel()))
							las_corr++;
					}
					dp_total++;
					pw.write(i+" "+sent.get(i).getName()+" "+sent.get(i).getTag()+" "+sent.get(i).getEntity()+" "+sent.get(i).getHeadIndex()+" "+predHeads[i]+"\n");
				}
				pw.write("\n");
			}
			
		}
		pw.close();
		System.out.println("**Evaluating Dependency Result**");
		System.out.println("[Dependency] Correct: "+dp_corr);
		System.out.println("[Dependency] total: "+dp_total);
		System.out.println("[Dependency] UAS: "+dp_corr*1.0/dp_total);
		if(labeledDep) System.out.println("[Dependency] LAS: "+las_corr*1.0/dp_total);
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
			if(NetworkConfig.MAX_MARGINAL_DECODING)
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
	
	
	

	
}

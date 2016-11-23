package com.statnlp.projects.dep.model.hyperedge;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.io.RAWF;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.projects.dep.utils.DPConfig;


public class HPEEval {

	
	

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
		HPEInstance bestInst = null;
		for (int index = 0; index < testInsts.length; index++) {
			HPEInstance inst = (HPEInstance)(testInsts[index]);
			List<Span> prediction = inst.getPrediction();
			Sentence sent = inst.getInput();
			for (int i = 0; i < prediction.size(); i++) {
				Span span = prediction.get(i);
				
				dp_total++;
			}
			pw.write("\n");
			
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
	 * 
	 * @param testInsts
	 * @param nerOut: word, true pos, true entity, pred entity
	 * @throws IOException
	 */
	public static void evalNER(Instance[] testInsts, String nerOut) throws IOException{
		PrintWriter pw = RAWF.writer(nerOut);
		for(int index=0;index<testInsts.length;index++){
			HPEInstance inst = (HPEInstance)testInsts[index];
			List<Span> prediction = inst.getPrediction();
			List<Span> output = inst.getOutput();
			Sentence sent = inst.getInput();
			String[] outputE = new String[sent.length()];
			String[] predictionE = new String[sent.length()];
			for (int i = 0; i < output.size(); i++) {
				Span span = output.get(i); 
				for (int p = span.start; p <= span.end; p++) {
					if (!span.label.form.equals("O")) {
						if (p == span.start) {
							outputE[p] = "B-" + span.label.form;
						}else{
							outputE[p] = "I-" + span.label.form;
						}
					} else {
						outputE[p] = span.label.form;
					}
				}
			}
			for (int i = 0; i < prediction.size(); i++) {
				Span span = prediction.get(i); 
				for (int p = span.start; p <= span.end; p++) {
					if (!span.label.form.equals("O")) {
						if (p == span.start) {
							predictionE[p] = "B-" + span.label.form;
						}else{
							predictionE[p] = "I-" + span.label.form;
						}
					} else {
						predictionE[p] = span.label.form;
					}
				}
			}
			for (int i = 1; i < outputE.length; i++) {
				pw.write(sent.get(i).getName()+" "+sent.get(i).getTag()+" "+outputE[i]+" "+predictionE[i]+"\n");
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
}

package com.statnlp.projects.dep.model.pipeline;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.statnlp.commons.io.RAWF;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.projects.dep.DependInstance;
import com.statnlp.projects.dep.Transformer;
import com.statnlp.projects.dep.model.segdep.SDInstance;
import com.statnlp.projects.dep.model.segdep.SegSpan;
import com.statnlp.projects.dep.model.segdep.SpanLabel;
import com.statnlp.projects.entity.semi.SemiCRFInstance;
import com.statnlp.projects.entity.semi.SemiCRFMain;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.UnnamedDependency;

public class Eval {

	public static boolean windows = false;
	
	public static void evalNERFile(Instance[] testInsts, String testFile, String nerOut) throws IOException, InterruptedException {
		PrintWriter pw = RAWF.writer(nerOut);
		SemiCRFInstance[] testNERInsts = SemiCRFMain.readCoNLLData(testFile, false,	-1);
		for(int index=0;index<testInsts.length;index++){
			SemiCRFInstance eInst = (SemiCRFInstance)testInsts[index];
			String[] predEntities = eInst.toEntities(eInst.getPrediction());
			String[] trueEntities = testNERInsts[index].toEntities(testNERInsts[index].getOutput());
			Sentence sent = eInst.getInput();
			for(int i = 0; i < sent.length(); i++){
				pw.write(sent.get(i).getName()+" "+sent.get(i).getTag()+" "+trueEntities[i]+" "+predEntities[i]+"\n");
			}
			pw.write("\n");
		}
		pw.close();
		evalNER(nerOut);
	}
	
	public static void evalNER(Instance[] testInsts, ResultInstance[] res, String nerOut) throws IOException, InterruptedException {
		PrintWriter pw = RAWF.writer(nerOut);
		for(int index=0;index<testInsts.length;index++){
			SemiCRFInstance eInst = (SemiCRFInstance)testInsts[index];
			String[] predEntities = eInst.toEntities(eInst.getPrediction());
			String[] trueEntities = res[index].entities;
			Sentence sent = eInst.getInput();
			for(int i = 0; i < sent.length(); i++){
				pw.write(sent.get(i).getName()+" "+sent.get(i).getTag()+" "+trueEntities[i+1]+" "+predEntities[i]+"\n");
			}
			pw.write("\n");
		}
		pw.close();
		evalNER(nerOut);
	}
	
	private static void evalNER(String outputFile) throws IOException, InterruptedException{
		try{
			System.err.println("perl data/semeval10t1/conlleval.pl < "+outputFile);
			ProcessBuilder pb = null;
			if(windows){
				pb = new ProcessBuilder("D:/Perl64/bin/perl","E:/Framework/data/semeval10t1/conlleval.pl"); 
			}else{
				pb = new ProcessBuilder("data/semeval10t1/conlleval.pl");
			}
			pb.redirectInput(new File(outputFile));
			pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
			pb.redirectError(ProcessBuilder.Redirect.INHERIT);
			Process p = pb.start();
			p.waitFor();
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	public static void evalDP(Instance[] testInsts, ResultInstance[] res) throws IOException{
		int dp_corr=0;
		int dp_total=0;
		for(int index=0; index<testInsts.length; index++){
			DependInstance inst = (DependInstance)(testInsts[index]);
			Tree prediction = inst.getPrediction();
			ArrayList<UnnamedDependency> predDependencies = inst.toDependencies(prediction);
			int[] predHeads = Transformer.getHeads(predDependencies, inst.getInput());
			int[] goldHeads = res[index].headIdxs;
			for(int i = 1; i < predHeads.length; i++){
				if(predHeads[i] == goldHeads[i]){
					dp_corr++;
				}
				dp_total++;
			}
			
		}
		System.out.println("**Evaluating Dependency Result**");
		System.out.println("[Dependency] Correct: "+dp_corr);
		System.out.println("[Dependency] total: "+dp_total);
		System.out.println("[Dependency] UAS: "+dp_corr*1.0/dp_total);
		System.out.println("*************************");
	}
	
	/**
	 * For the segments are predicted as well.
	 * @param testInsts
	 */
	public static void evalSpanDep(Instance[] goldTestInsts, Instance[] testInsts) {
		int corr = 0;
		int goldTotal = 0;
		for (int index = 0; index < testInsts.length; index++) {
			SDInstance inst = (SDInstance)(testInsts[index]);
			SDInstance goldInst = (SDInstance)(goldTestInsts[index]);
			List<SegSpan> goldSegs = goldInst.getSegments();
			List<SegSpan> predSegs = inst.getSegments();
			processSegment(predSegs);
			processSegment(goldSegs);
			int[] output = goldInst.getOutput();
			int[] prediction = inst.getPrediction();
			Map<SegSpan, Integer> predSegHeadmap = new HashMap<SegSpan, Integer>(predSegs.size());
			for (int s = 1; s < predSegs.size(); s++) {
				predSegHeadmap.put(predSegs.get(s), prediction[s]);
			}
			for (int i = 1; i < output.length; i++) {
				SegSpan curr = goldSegs.get(i);
				if (predSegHeadmap.containsKey(curr)) {
					int predHeadSpanIdx = predSegHeadmap.get(curr);
					SegSpan predHeadSpan = predSegs.get(predHeadSpanIdx);
					SegSpan goldHeadSpan = goldSegs.get(output[i]);
					if (predHeadSpan.equals(goldHeadSpan))
						corr++;
				}
			}
			goldTotal += output.length - 1;
		}
		System.out.println("**Evaluation of Segmented Dependency Result**");
		System.out.println("[Dependency] Correct: "+corr);
		System.out.println("[Dependency] Gold Total: "+goldTotal);
		System.out.printf("[Dependency] UAS: %.2f\n", corr*1.0/goldTotal*100);
		System.out.println("*************************");
	}
	
	private static void processSegment(List<SegSpan> segments) {
		for (SegSpan span: segments) {
			span.label = new SpanLabel("", -1);
		}
	}
	
}

package com.statnlp.projects.dep.model.hyperedge;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.statnlp.commons.io.RAWF;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.projects.dep.utils.DPConfig;


public class HPEEval {

	
	private static HashSet<String> punct = new HashSet<>(Arrays.asList("''", ",", ".", ":", "``", "-LRB-", "-RRB-"));

	public static void writeJointResult(Instance[] testInsts, String resultFile) throws IOException {
		PrintWriter pw = RAWF.writer(resultFile);
		for(int index=0;index<testInsts.length;index++){
			HPEInstance eInst = (HPEInstance)testInsts[index];
			List<Span> prediction = eInst.getPrediction();
			Sentence sent = eInst.getInput();
			int[] headSpanIdxs = new int[prediction.size()];
			String[] predictionE = new String[sent.length()];
			for (int i = 0; i < prediction.size(); i++) {
				Span span = prediction.get(i); 
				Span headSpan = span.headSpan;
				int headSpanIdx = prediction.indexOf(headSpan);
				for (int p = span.start; p <= span.end; p++) {
					headSpanIdxs[p] = headSpanIdx;
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
			for(int i = 1; i < sent.length(); i++){
				pw.write(i + "\t" + sent.get(i).getName()+"\t"+sent.get(i).getTag()+"\t"+predictionE[i]+"\t"+headSpanIdxs[i]+"\n");
			}
			pw.write("\n");
		}
		pw.close();
	}
	
	public static void evalCombined(Instance[] testInsts) {
		int tp = 0;
		int tp_fp = 0;
		int tp_fn = 0;
		for(int index=0;index<testInsts.length;index++){
			HPEInstance eInst = (HPEInstance)testInsts[index];
			Sentence sent = eInst.getInput();
			List<Span> outputSpans = eInst.getOutput(); 
			List<Span> predSpans = eInst.getPrediction(); 
			Map<Span, Span> outputMap = new HashMap<Span, Span>(outputSpans.size());
			for (Span outputSpan : outputSpans) outputMap.put(outputSpan, outputSpan);
			Map<Span, Span> predMap = new HashMap<Span, Span>(outputSpans.size());
			for (Span predSpan : predSpans) predMap.put(predSpan, predSpan);
			for (Span predSpan: predSpans) {
				if (predSpan.start == predSpan.end && punct.contains(sent.get(predSpan.start).getTag())) {
					continue;
				}
				if (predSpan.start == predSpan.end && predSpan.start == 0) {
					continue;
				}
				if (outputMap.containsKey(predSpan)) {
					Span outputSpan = outputMap.get(predSpan);
					Set<Integer> intersection = new HashSet<>();
					for (int pos = outputSpan.headSpan.start; pos <= outputSpan.headSpan.end; pos++)
						intersection.add(pos);
					for (int pos = predSpan.headSpan.start; pos <= predSpan.headSpan.end; pos++) {
						if (intersection.contains(pos))
							tp++;
					}
				}
				tp_fp += predSpan.headSpan.end - predSpan.headSpan.start + 1;
			}
			
			for (Span outputSpan: outputSpans) {
				if (outputSpan.start == outputSpan.end && punct.contains(sent.get(outputSpan.start).getTag())) {
					continue;
				}
				if (outputSpan.start == outputSpan.end && outputSpan.start == 0) {
					continue;
				}
				tp_fn += outputSpan.headSpan.end - outputSpan.headSpan.start + 1;
			}
		}
		double precision = tp*1.0 / tp_fp * 100;
		double recall = tp*1.0 / tp_fn * 100;
		double fmeasure = 2.0*tp / (tp_fp + tp_fn) * 100;
		System.out.printf("[Unit Attachment Evaluation]\n");
		System.out.printf("TP: %d, TP+FP: %d, TP+FN: %d\n", tp, tp_fp, tp_fn);
		System.out.printf("Precision: %.2f%%, Recall: %.2f%%, F-measure: %.2f%%\n", precision, recall, fmeasure);
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

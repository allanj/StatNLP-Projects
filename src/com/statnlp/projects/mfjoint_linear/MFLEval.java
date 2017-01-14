package com.statnlp.projects.mfjoint_linear;

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


public class MFLEval {

	
	private static HashSet<String> punct = new HashSet<>(Arrays.asList("''", ",", ".", ":", "``", "-LRB-", "-RRB-"));
	
	public static void evalDep(Instance[] testInsts){
		int dp_corr=0;
		int noPunc_corr = 0;
		int noPunc_total = 0;
		int dp_total=0;
		for (int index = 0; index < testInsts.length; index++) {
			MFLInstance inst = (MFLInstance)(testInsts[index]);
			Sentence sent = inst.getInput();
			int[] prediction = inst.getPrediction().heads;
			int[] output = inst.getOutput().heads;
			for (int i = 1; i < prediction.length; i++) {
				if (output[i] == prediction[i]) {
					dp_corr++;
				}
				dp_total++;
				if (!punct.contains(sent.get(i).getTag())) {
					if (output[i] == prediction[i]) {
						noPunc_corr++;
					}
					noPunc_total++;
				}
			}
		}
		System.out.println("**Evaluating Dependency Result**");
		System.out.println("[Dependency] Correct: "+ dp_corr);
		System.out.println("[Dependency] total: "+ dp_total);
		System.out.printf("[Dependency] UAS: %.2f\n", dp_corr*1.0/dp_total*100);
		System.out.println("[Dependency] No Punctutation Correct: "+ noPunc_corr);
		System.out.println("[Dependency] No Punctutation Total: "+ noPunc_total);
		System.out.printf("[Dependency] No Punctutation UAS: %.2f\n", noPunc_corr*1.0/noPunc_total*100);
		System.out.println("*************************");
	}
	
	public static void evalNER(Instance[] testInsts, String nerOut) throws IOException, InterruptedException{
		PrintWriter pw = RAWF.writer(nerOut);
		for(int index=0;index<testInsts.length;index++){
			MFLInstance eInst = (MFLInstance)testInsts[index];
			String[] predEntities = eInst.getPrediction().entities;
			String[] trueEntities = eInst.getOutput().entities;
			Sentence sent = eInst.getInput();
			for(int i = 1; i < sent.length(); i++){
				pw.write(sent.get(i).getName()+" "+sent.get(i).getTag()+" "+trueEntities[i]+" "+predEntities[i]+"\n");
			}
			pw.write("\n");
		}
		pw.close();
		evalNER(nerOut);
	}
	
	private static void evalNER(String outputFile) throws IOException, InterruptedException{
		try{
			System.err.println("perl eval/conlleval.pl < "+outputFile);
			ProcessBuilder pb = null;
			if(MFLConfig.windows){
				pb = new ProcessBuilder("D:/Perl64/bin/perl","E:/Framework/eval/conlleval.pl"); 
			}else{
				pb = new ProcessBuilder("eval/conlleval.pl"); 
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
	
	public static void writeJointResult(Instance[] testInsts, String resultFile) throws IOException {
		PrintWriter pw = RAWF.writer(resultFile);
		for(int index=0;index<testInsts.length;index++){
			MFLInstance eInst = (MFLInstance)testInsts[index];
			String[] predEntities = eInst.getPrediction().entities;
			int[] headIdxs = eInst.getPrediction().heads;
			Sentence sent = eInst.getInput();
			for(int i = 1; i < sent.length(); i++){
				pw.write(i + "\t" + sent.get(i).getName()+"\t"+sent.get(i).getTag()+"\t"+predEntities[i]+"\t"+headIdxs[i]+"\n");
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
			MFLInstance eInst = (MFLInstance)testInsts[index];
			Sentence sent = eInst.getInput();
			List<MFLSpan> outputSpans = Utils.toSpan(eInst.getOutput().entities, eInst.getOutput().heads);
			List<MFLSpan> predSpans = Utils.toSpan(eInst.getPrediction().entities, eInst.getPrediction().heads);
			Map<MFLSpan, MFLSpan> outputMap = new HashMap<MFLSpan, MFLSpan>(outputSpans.size());
			for (MFLSpan outputSpan : outputSpans) outputMap.put(outputSpan, outputSpan);
			Map<MFLSpan, MFLSpan> predMap = new HashMap<MFLSpan, MFLSpan>(outputSpans.size());
			for (MFLSpan predSpan : predSpans) predMap.put(predSpan, predSpan);
			for (MFLSpan predSpan: predSpans) {
				if (predSpan.start == predSpan.end && punct.contains(sent.get(predSpan.start).getTag())) {
					continue;
				}
				if (predSpan.start == predSpan.end && predSpan.start == 0) {
					continue;
				}
				if (outputMap.containsKey(predSpan)) {
					MFLSpan outputSpan = outputMap.get(predSpan);
					Set<Integer> intersection = new HashSet<Integer>(predSpan.heads);
					intersection.retainAll(outputSpan.heads);
					tp += intersection.size();
				}
				tp_fp += predSpan.heads.size();
			}
			
			for (MFLSpan outputSpan: outputSpans) {
				if (outputSpan.start == outputSpan.end && punct.contains(sent.get(outputSpan.start).getTag())) {
					continue;
				}
				if (outputSpan.start == outputSpan.end && outputSpan.start == 0) {
					continue;
				}
				tp_fn += outputSpan.heads.size();
			}
		}
		double precision = tp*1.0 / tp_fp * 100;
		double recall = tp*1.0 / tp_fn * 100;
		double fmeasure = 2.0*tp / (tp_fp + tp_fn) * 100;
		System.out.printf("[Unit Attachment Evaluation]\n");
		System.out.printf("TP: %d, TP+FP: %d, TP+FN: %d\n", tp, tp_fp, tp_fn);
		System.out.printf("Precision: %.2f%%, Recall: %.2f%%, F-measure: %.2f%%\n", precision, recall, fmeasure);
	}
	
	
	public static void evalCombinedSeparateModel(String goldTrainFile, String goldTestFile, String depResult, String neResult) throws IOException {
		MFLReader.readCoNLLXData(goldTrainFile, true, -1, true, true);
		MFLInstance[] testInsts = MFLReader.readCoNLLXData(goldTestFile, false, -1, false, false);
		Utils.readSeparateResultFile(testInsts, depResult, neResult);
		MFLEval.evalCombined(testInsts);
	}
	
//	public static void main(String[] args) throws IOException{
//		String[] sections = new String[] {"abc", "cnn", "mnb", "nbc", "p25", "pri", "voa", "all"};
//		for (int s = 0; s < sections.length; s++) {
//			String depFile = "F:/Dropbox/SUTD/Work (1)/ACL2017/Experiments/depresults/"+sections[s]+".dep.test.noef.dp.res.txt";
//			String neResult = "F:/Dropbox/SUTD/Work (1)/ACL2017/Experiments/linearresult/"+sections[s]+".ecrf.test.depf-false.ner.res.txt";
//			String goldTrainFile = "data/allanprocess/"+sections[s]+"/train.conllx";
//			String goldTestFile = "data/allanprocess/"+sections[s]+"/test.conllx";
//			evalCombinedSeparateModel(goldTrainFile, goldTestFile, depFile, neResult);
//		}
//	}
	
}

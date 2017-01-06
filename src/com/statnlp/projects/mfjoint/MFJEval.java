package com.statnlp.projects.mfjoint;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;

import com.statnlp.commons.io.RAWF;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;


public class MFJEval {

	
	private static HashSet<String> punct = new HashSet<>(Arrays.asList("''", ",", ".", ":", "``", "-LRB-", "-RRB-"));
	
	public static void evalDep(Instance[] testInsts){
		int dp_corr=0;
		int noPunc_corr = 0;
		int noPunc_total = 0;
		int dp_total=0;
		for (int index = 0; index < testInsts.length; index++) {
			MFJInstance inst = (MFJInstance)(testInsts[index]);
			Sentence sent = inst.getInput();
			int[] prediction = inst.getPrediction().heads;
			int[] output = inst.getOutput().heads;
			for (int i = 1; i < prediction.length; i++) {
				if (output[i] == prediction[i]) {
					dp_corr++;
				}
				dp_total++;
				if (!punct.contains(sent.get(i).getName())) {
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
			MFJInstance eInst = (MFJInstance)testInsts[index];
			String[] predEntities = eInst.toEntities(eInst.getPrediction().entities);
			String[] trueEntities = eInst.toEntities(eInst.getOutput().entities);
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
			if(MFJConfig.windows){
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
}

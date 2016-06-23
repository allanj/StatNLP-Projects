package com.statnlp.dp.model.bruteforce;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.utils.DPConfig;


public class BFEval {

	
	/**
	 * 
	 * @param testInsts
	 * @param nerOut: word, true pos, true entity, pred entity
	 * @throws IOException
	 */
	public static void evalNER(Instance[] testInsts, String nerOut) throws IOException{
		PrintWriter pw = RAWF.writer(nerOut);
		for(int index=0;index<testInsts.length;index++){
			BFInstance eInst = (BFInstance)testInsts[index];
			ArrayList<String> predEntities = eInst.getPredEntities();
			ArrayList<String> trueEntities = eInst.getOutput();
			Sentence sent = eInst.getInput();
			for(int i=1;i<sent.length();i++){
				pw.write(sent.get(i).getName()+" "+sent.get(i).getTag()+" "+trueEntities.get(i)+" "+predEntities.get(i)+"\n");
			}
			pw.write("\n");
		}
		pw.close();
		evalNER(nerOut);
	}
	
	
	private static void evalNER(String outputFile) throws IOException{
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
			Instance inst = predictions[index];
			BFInstance eInst = (BFInstance)inst;
			ArrayList<String> predEntities = eInst.getPredEntities();
			ArrayList<String> trueEntities = eInst.getOutput();
			Sentence sent = eInst.getInput();
			for(int i=0;i<sent.length();i++){
				int headIndex = sent.get(i).getHeadIndex()+1;
				pw.write((i+1)+" "+sent.get(i).getName()+" "+sent.get(i).getTag()+" "+trueEntities.get(i)+" "+predEntities.get(i)+" "+headIndex+"\n");
			}
			pw.write("\n");
		}
		
		pw.close();
	}
	
	public static void evalDP(Instance[] testInsts, String dpOut) throws IOException{
		int dp_corr=0;
		int dp_total=0;
		PrintWriter pw = RAWF.writer(dpOut);
		for(Instance org_inst: testInsts){
			BFInstance inst = (BFInstance)org_inst;
			Sentence sent = inst.getInput();
			int[] predHeads = inst.getPredHeads();
			for(int i=1;i<predHeads.length;i++){
				if(predHeads[i]==sent.get(i).getHeadIndex())
					dp_corr++;
				dp_total++;
				pw.write(i+" "+sent.get(i).getName()+" "+sent.get(i).getTag()+" "+sent.get(i).getEntity()+" "+sent.get(i).getHeadIndex()+" "+predHeads[i]+"\n");
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
}

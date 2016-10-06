package com.statnlp.projects.entity.dcrf;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;

public class DCRFEval {

	
	/**
	 * 
	 * @param testInsts
	 * @param nerOut: word, true pos, true entity, pred entity
	 * @throws IOException
	 */
	public static void evalNER(Instance[] testInsts, String nerOut) throws IOException{
		PrintWriter pw = RAWF.writer(nerOut);
		for(int index=0;index<testInsts.length;index++){
			DCRFInstance eInst = (DCRFInstance)testInsts[index];
			ArrayList<String> predEntities = eInst.getEntityPrediction();
			ArrayList<String> trueEntities = eInst.getOutput();
			Sentence sent = eInst.getInput();
			for(int i=0;i<sent.length();i++){
				pw.write(sent.get(i).getName()+" "+trueEntities.get(i)+" "+predEntities.get(i)+"\n");
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
			if(DCRFConfig.windows){
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
	
	public static void evalPOS(Instance[] testInsts, String posOut) throws IOException{
		PrintWriter pw = RAWF.writer(posOut);
		int corr=0;
		int total = 0;
		for(int index=0;index<testInsts.length;index++){
			DCRFInstance eInst = (DCRFInstance)testInsts[index];
			ArrayList<String> predTag = eInst.getTagPrediction();
			Sentence sent = eInst.getInput();
			for(int i=0;i<sent.length();i++){
				if(sent.get(i).getTag().equals(predTag.get(i)))
					corr++;
				total++;
				pw.write(sent.get(i).getName()+" "+sent.get(i).getTag()+" "+predTag.get(i)+"\n");
			}
		}
		System.err.println("Accuracy:"+corr*1.0/total);
		pw.close();
	}
	
}

package com.statnlp.projects.nndcrf.linear_pos;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;

public class POSEval {

	
	/**
	 * 
	 * @param testInsts
	 * @param nerOut: word, true pos, true entity, pred entity
	 * @throws IOException
	 */
	public static void evalPOS(Instance[] testInsts, String posOutput) throws IOException{
		PrintWriter pw = RAWF.writer(posOutput);
		int corr = 0;
		int total = 0;
		for(int index=0;index<testInsts.length;index++){
			POSInstance eInst = (POSInstance)testInsts[index];
			ArrayList<String> predPOS = eInst.getPrediction();
			ArrayList<String> truePOS = eInst.getOutput();
			Sentence sent = eInst.getInput();
			for(int i=0;i<sent.length();i++){
				String predLabel  = predPOS.get(i);
				if(predLabel.equals(truePOS.get(i)))
					corr++;
				total++;
				//entity is the chunk.
				pw.write(sent.get(i).getName()+" "+sent.get(i).getEntity()+" "+truePOS.get(i)+" "+predLabel+"\n");
			}
			pw.write("\n");
		}
		System.out.println("Accuracy:"+corr*100.0/total);
		pw.close();
	}
	
	
	
}

package com.statnlp.dp.model.hybrid;

import java.io.IOException;
import java.io.PrintWriter;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.Evaluator;

public class HBDEvaluator extends Evaluator {

	
	public static void evalJointLinear(Instance[] testInsts, String jointLinearOut)  throws IOException{
		PrintWriter pw = RAWF.writer(jointLinearOut);
		for(int index=0;index<testInsts.length;index++){
			HBDInstance inst = (HBDInstance)testInsts[index];
			String[] predEntities = inst.getLinearPrediction();
			Sentence sent = inst.getInput();
			for(int i=1;i<sent.length();i++){
				pw.write(sent.get(i).getName()+" "+sent.get(i).getTag()+" "+sent.get(i).getEntity()+" "+predEntities[i]+"\n");
			}
			pw.write("\n");
		}
		pw.close();
		evalNER(jointLinearOut);
	}
	
}

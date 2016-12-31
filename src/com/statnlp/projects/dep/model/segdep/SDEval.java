package com.statnlp.projects.dep.model.segdep;


import com.statnlp.commons.types.Instance;


public class SDEval {

	/**
	 * Evaluate the dependency
	 * @param testInsts
	 * @param dpOut, index \t word \t tag \t true entity \t trueHead \t predHead
	 * @throws IOException
	 */
	public static void evalDep(Instance[] testInsts, boolean labeledDep){
		int dp_corr=0;
		int dp_total=0;
		int nonSeg_corr = 0;
		int nonSeg_total = 0;
		int las_corr = 0;
		for (int index = 0; index < testInsts.length; index++) {
			SDInstance inst = (SDInstance)(testInsts[index]);
			int[] prediction = inst.getPrediction();
			int[] output = inst.getOutput();
			for (int i = 1; i < prediction.length; i++) {
				if (output[i] == prediction[i]) {
					dp_corr++;
				}
				dp_total++;
				if (inst.segments.get(i).length() == 1 && inst.segments.get(i).label.equals(SpanLabel.get("O"))) {
					if (output[i] == prediction[i]) {
						nonSeg_corr++;
					}
					nonSeg_total++;
				}

			}
		}
		System.out.println("**Evaluating Dependency Result**");
		System.out.println("[Dependency] Correct: "+dp_corr);
		System.out.println("[Dependency] total: "+dp_total);
		System.out.printf("[Dependency] UAS: %.2f\n", dp_corr*1.0/dp_total*100);
		if(labeledDep) System.out.println("[Dependency] LAS: "+las_corr*1.0/dp_total*100);
		System.out.println("[Dependency] Non-Segment Correct: "+nonSeg_corr);
		System.out.println("[Dependency] Non-Segment total: "+nonSeg_total);
		System.out.printf("[Dependency] Non-Segment UAS: %.2f\n", nonSeg_corr*1.0/nonSeg_total*100);
		System.out.println("*************************");
	}
	
}

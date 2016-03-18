package com.statnlp.algomodel;

public class AlgoModelTester {

	public AlgoModelTester() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {

		//MentionExtractionAlgoModel algomodel = new MentionExtractionAlgoModel();
		AlgoModel algomodel = AlgoModel.create(AlgoModel.ALGOMODEL.MENTION_EXTRACTION.toString());	
		String[] newargs = new String[]{"8", "ACE20041", "10", "0.01", "English"};
		algomodel.Train(newargs);
		//algomodel.setDemoMode(true);
		//algomodel.Evaluate(newargs);
	}

}

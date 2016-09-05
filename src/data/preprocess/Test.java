package data.preprocess;

import edu.stanford.nlp.trees.EnglishGrammaticalStructure;

public class Test {

	public static String head_rule = "data/headrule_en_stanford.txt";
	public Test() {
		
	}
	public static void main(String[] args){
//		String[] myargs = new String[]{"-h",head_rule,"-i","/Users/allanjie/Downloads/abc_0020.parse"};
//		C2DConvert.main(args);
		String tmpParse = "F:/phd/data/ontonotes-release-5.0_LDC2013T19/ontonotes-release-5.0_LDC2013T19/ontonotes-release-5.0/data/files/data/english/annotations/bn/abc/00/abc_0001.parse";
		EnglishGrammaticalStructure.main(new String[]{"-basic","-keepPunct","-conllx","-treeFile",tmpParse});
	}

}

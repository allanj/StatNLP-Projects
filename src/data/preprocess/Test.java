package data.preprocess;

import edu.emory.clir.clearnlp.bin.C2DConvert;

public class Test {

	public static String head_rule = "data/headrule_en_stanford.txt";
	public Test() {
		
	}
	public static void main(String[] args){
//		String[] myargs = new String[]{"-h",head_rule,"-i","/Users/allanjie/Downloads/abc_0020.parse"};
		C2DConvert.main(args);
	}

}

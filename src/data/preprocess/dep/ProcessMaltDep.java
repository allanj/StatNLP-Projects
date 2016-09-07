package data.preprocess.dep;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.maltparser.Malt;

import com.statnlp.commons.crf.RAWF;

public class ProcessMaltDep {

	public static String[] datasets = {"abc","cnn","mnb","nbc","p25","pri","voa"};
//	public static String[] datasets = {"abc"};
	public static String prefix = "data/allanprocess/";
	public static String combName = "trainPdev.conllx";
	
	public static boolean windows = true;
	
	public static void predDepStructure(){
		for(String dataset: datasets){
			//create model
			createModel(dataset);
		}
		
//		for(String dataset: datasets){
//			//create model
//			predict(dataset);
//		}
	}
	
	private static void createModel(String dataset){
		combine(dataset);
		Malt.main(new String[]{"-m","learn",
		"-c",dataset+"model",
		"-i",prefix+dataset+"/"+combName,
		"-if","data/malt/myformat.xml",
		"-F","data/malt/NivreEager.xml" ,
		"-a","nivreeager",
		"-l","libsvm",
		"-it","1000"});
	}
	
	private static void predict(String dataset){
		Malt.main(new String[]{"-m","parse",
				"-c",dataset+"model",
				"-i",prefix+dataset+"/test.conllx",
				"-if","data/malt/myformat.xml",
				"-of","E:/Framework/data/malt/myformat.xml",
				"-F","data/malt/NivreEager.xml" ,
				"-a","nivreeager",
				"-l","libsvm",
				"-o",prefix+dataset+"/pred_test.conllx"});
	}

	private static void combine(String dataset) {
		String[] files = new String[]{prefix+dataset+"/train.conllx", prefix+dataset+"/dev.conllx"};
		String outFile = prefix+dataset+"/"+combName;
		BufferedReader br = null;
		PrintWriter pw = null;
		try{
			pw = RAWF.writer(outFile);
			for(String file: files){
				br = RAWF.reader(file);
				String line = null;
				while((line = br.readLine())!= null){
					pw.write(line+"\n");
				}
				br.close();
			}
		}catch(IOException ioe){
			ioe.printStackTrace();
		}finally{
			pw.close();
		}
		
	}
	
	
	public static void main(String[] args) {
		predDepStructure();
	}

}

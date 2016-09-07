package data.preprocess.dep;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.maltparser.Malt;

import com.statnlp.commons.crf.RAWF;

public class ProcessMaltDep {

//	public static String[] datasets = {"abc","cnn","mnb","nbc","p25","pri","voa"};
//	public static String[] datasets = {"abc"};
	public static String prefix = "data/allanprocess/";
	public static String combName = "trainPdev.conllx";
	public static String data = "abc";
	public static String mode = "train";
	public static boolean isDev = false;
	
	
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
	
	private static void predict(String dataset, boolean isDev){
		String testFile = isDev? "dev.conllx":"test.conllx";
		Malt.main(new String[]{"-m","parse",
				"-c",dataset+"model",
				"-i",prefix+dataset+"/"+testFile,
				"-if","data/malt/myformat.xml",
				"-of","data/malt/myformat.xml",
				"-F","data/malt/NivreEager.xml" ,
				"-a","nivreeager",
				"-l","libsvm",
				"-o",prefix+dataset+"/pred_"+testFile});
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
		System.err.println("Train+Dev. File Combined.");
	}
	
	/**
	 * Currently only for debug the output, not really used.
	 * @param goldConll
	 * @param predConll
	 */
	public static void eval(String goldConll, String predConll){
		try {
			BufferedReader gbr = RAWF.reader(goldConll);
			BufferedReader pbr = RAWF.reader(predConll);
			String gold_line = null;
			String pred_line = null;
			int uas = 0;
			int las = 0;
			int total = 0;
			while((gold_line = gbr.readLine())!=null){
				pred_line = pbr.readLine();
				if(gold_line.equals("")) continue;
				String[] gold_vals = gold_line.split("\\t+");
				String[] pred_vals = pred_line.split("\\t+");
				
				if(gold_vals[6].equals(pred_vals[6])){
					uas++;
					if(gold_vals[7].equals(pred_vals[7]))
						las++;
				}
				total++;
			}
			gbr.close();
			pbr.close();
			System.out.println("[Info] UAS:"+uas*1.0/total);
			System.out.println("[Info] LAS:"+las*1.0/total);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	
	public static void main(String[] args) {
		for(int i=0;i<args.length;i=i+2){
			switch(args[i]){
				case "-data": data = args[i+1]; break;
				case "-mode": mode = args[i+1]; break;
				case "-dev": isDev = args[i+1].equals("true")? true:false; break;
				default: System.err.println("Invalid arguments, please check usage."); System.exit(0);
			}
		}
		if(mode.equals("train")){
			createModel(data);
		}else if(mode.equals("test")){
			predict(data, isDev);
		}
		
	}

}

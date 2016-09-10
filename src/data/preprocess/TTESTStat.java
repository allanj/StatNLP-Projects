package data.preprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.apache.commons.math3.stat.inference.TTest;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.dp.utils.DPConfig;

/**
 * Compare two system.
 * @author 1001981
 *
 */
public class TTESTStat {

	public static String tmpEval1 = "tmpfolder/tmp1.eval";
	public static String tmpEval2 = "tmpfolder/tmp2.eval";
	public static String tmpLog = "tmpfolder/tmp.log";
	
	public static void testFiles(String eval1, String eval2) throws IOException{
		BufferedReader br1 = RAWF.reader(eval1);
		BufferedReader br2 = RAWF.reader(eval2);
		PrintWriter pw1 = RAWF.writer(tmpEval1);
		PrintWriter pw2 = RAWF.writer(tmpEval2);
		String line1 = null;
		String line2 = null;
		ArrayList<Double> sample1 = new ArrayList<Double>();
		ArrayList<Double> sample2 = new ArrayList<Double>();
		while((line1 = br1.readLine())!=null){
			line2 = br2.readLine();
			if(line1.equals("")){
				pw1.close();
				pw2.close();
				//eval the file here.
				double fscore1 = getScore(tmpEval1);
				double fscore2 = getScore(tmpEval2);
				sample1.add(fscore1);
				sample2.add(fscore2);
				pw1 = RAWF.writer(tmpEval1);
				pw2 = RAWF.writer(tmpEval2);
			}
			pw1.write(line1+"\n");
			pw2.write(line2+"\n");
		}
		br1.close();
		br1.close();
		
		double[] sample1arr = new double[sample1.size()];
		double[] sample2arr = new double[sample2.size()];
		for(int i=0; i< sample1.size(); i++) sample1arr[i] = sample1.get(i).doubleValue();
		for(int i=0; i< sample2.size(); i++) sample2arr[i] = sample2.get(i).doubleValue();
		TTest ttest = new TTest();
		double t_statistic = ttest.pairedTTest(sample1arr, sample2arr);
		System.out.println(Double.toString( t_statistic) );
	}
	
	private static double getScore(String evalFile) throws IOException{
		System.err.println("perl data/semeval10t1/conlleval.pl < "+evalFile);
		ProcessBuilder pb = null;
		if(DPConfig.windows){
			pb = new ProcessBuilder("D:/Perl64/bin/perl","E:/Framework/data/semeval10t1/conlleval.pl"); 
		}else{
			pb = new ProcessBuilder("data/semeval10t1/conlleval.pl"); 
		}
		pb.redirectInput(new File(evalFile));
		pb.redirectOutput(new File(tmpLog));
		//pb.redirectError(ProcessBuilder.Redirect.INHERIT);
		Process p = pb.start();
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return getAcc(tmpLog);
	}
	
	private static double getAcc(String log) throws IOException{
		BufferedReader br = RAWF.reader(log);
		String line = null;
		double acc = -1;
		while((line = br.readLine())!=null){
			if(line.startsWith("accuracy:")){
				String[] vals = line.split("\\s+");
				acc = Double.valueOf(vals[vals.length-1]);
			}
		}
		br.close();
		if(acc==-1) throw new RuntimeException("no acc returned?");
		return acc;
	}
	
	public static void main(String[] args) throws IOException{
		
//		//our sample should be groups of F-score.
//		double[] sample1 = { 1  , 2  , 3   ,4 , 3, 5, 6.1, 3.4, 2.9, 4.4};
//		double[] sample2 = { 5.2, 4.2, 7.24,4 , 5, 6, 4.1, 5.9, 7.0, 8.0};
//		double t_statistic;
//		TTest ttest = new TTest();
//		t_statistic = ttest.pairedTTest(sample1, sample2);
//		System.out.println(Double.toString( t_statistic) );
		testFiles("data/result_allan/all/semi.model0.gold.dep-false.noignore.all.txt", 
				"data/result_allan/all/model2.gold.dep-false.noignore.eval.all.txt");
	}

}

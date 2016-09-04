package data.preprocess;

import java.io.BufferedReader;
import java.io.IOException;

import com.statnlp.commons.crf.RAWF;

/**
 * This class is for reading the tuning log to read the best L2 parameter
 * @author allanjie
 *
 */
public class TuningLog {

	public static String[] newstypes = new String[]{"bc","bn","mz","nw","tc","wb"};
	public static String[] models = new String[]{"lcrf","semi","model1","model2"};
//	public static 
	
	
//	public static void main
	
	
	/**
	 * return the f-score accuracy of the file
	 * @param log
	 * @return
	 * @throws IOException
	 */
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
		
	}
}

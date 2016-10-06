package com.statnlp.projects.dep.benchmark;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import com.statnlp.commons.crf.RAWF;

public class MaltParser {

	String modelName;
	
	public MaltParser(String modelName) {
		this.modelName = modelName;
		/***Test the execution***/
		execCmd("java -version");
		
		
	}
	
	/**
	 * Extract number of training sentence from the universal dependency training data
	 * @param orgFile
	 * @param trainFile
	 * @param number: put value <=0 to extract all of them;
	 * @throws IOException
	 */
	public void extractTraining(String orgFile, String trainFile, int number){
		try{
			BufferedReader br = RAWF.reader(orgFile);
			PrintWriter pw = RAWF.writer(trainFile);
			
			String line = null;
			int num = 0;
			while((line = br.readLine())!=null){
				pw.write(line+"\n");;
				if(line.equals("")){
					num++;
					if(num==number) break;
				}
			}
			br.close();
			pw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
		
	}
	
	
	public void train(String data){
		
		String cmd = "java -jar maltparser-1.8.1.jar -c "+modelName+" -i "+data+" -m learn";
		System.err.println(cmd);
		execCmd(cmd);
	}
	
	public void predict(String testFile, String result){
		String cmd = "java -jar maltparser-1.8.1.jar -c "+modelName+" -i "+testFile+" -o "+result+" -m parse";
		System.err.println(cmd);
		execCmd(cmd);
	}
	
	private void execCmd(String cmd){
		try{
			Runtime runtime = Runtime.getRuntime();  
			Process process = runtime.exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line = null;
            while((line = br.readLine())!=null){
            	System.err.println(line);
            }
            br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	public void evaluate(String trueFile, String predFile){
		try {
			BufferedReader brtrue = RAWF.reader(trueFile);
			BufferedReader brpred = RAWF.reader(predFile);
			String line1 = null;
			String line2 = null;
			int corr = 0;
			int total = 0;
			while((line1 = brtrue.readLine())!=null){
				line2 = brpred.readLine();
				if(!line1.equals("")){
					String[] values1 = line1.split("\\t");
					String[] values2 = line2.split("\\t");
					if(values1[6].equals(values2[6])) corr++;
					total++;
				}
			}
			System.err.println("Correct: "+corr);
			System.err.println("total: "+total);
			System.err.println("Precision: "+corr*1.0/total);
			brtrue.close();
			brpred.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		String orgFile = "data/dependency/UD_English/en-ud-train.conllu";
		String trainFile = "data/dependency/UD_English/smalltest.conllu";
		String trueFile = "data/dependency/UD_English/en-ud-dev.conllu";
		String result = "data/dependency/UD_English/maltdev1.conllu";
		MaltParser mparser = new MaltParser("maltdevmodel");

		mparser.extractTraining(orgFile, trainFile, 100);
		mparser.train(trainFile);
		mparser.predict(trueFile, result);
		mparser.evaluate(trueFile, result);
	}
}

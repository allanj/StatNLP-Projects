package com.statnlp.entity;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import com.statnlp.commons.crf.RAWF;

public class LogReader {
	
	/**
	 * The direct source file for the data
	 */
	private String data;
	private String model;
	
	public LogReader(String model, String data) {
		this.model = model;
		this.data = data;
	}
	

	public void calculateDecodeTime(){
		try{
			BufferedReader reader = RAWF.reader(data);
			String line = null;
			boolean decode = false;
			ArrayList<Integer> instNum = new ArrayList<Integer>();
			double[] smallDecodeTime = null;
			while((line = reader.readLine())!=null){
				String[] vals = line.split(" ");
				if(line.startsWith("Training completes")) { decode = true; continue;}
				if(decode && line.startsWith("Thread")){
					instNum.add(Integer.valueOf(vals[3]));
					continue;
				}
				if(decode && line.startsWith("Okay. Decoding started.")){
					smallDecodeTime = new double[instNum.size()];
					continue;
				}
				if(decode && line.startsWith("Decoding time")){
					smallDecodeTime[Integer.valueOf(vals[7])] = Double.valueOf(vals[9]);
					continue;
				}
				
			}
			reader.close();
			int sum = 0;
			for(int x: instNum){
				sum+=x;
			}
			double sumTime = 0;
			for(double val: smallDecodeTime){
				sumTime += val;
			}
			System.out.println(sumTime/sum);
//			System.out.println("The average decode time for "+data+": "+sumTime/sum);
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	public void printFmeasure(){
		try{
			BufferedReader reader = RAWF.reader(data);
			String line = null;
			while((line = reader.readLine())!=null){
				String[] vals = line.split("\\s+");
				if(line.startsWith("accuracy:")){
					System.out.println(vals[vals.length-1]);
				}
			}
			reader.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		String[] types = new String[]{"abc","cnn","mnb","nbc","pri","voa"};
		String[] models = new String[]{"lcrf","semi","model1","model2"};
		String[] deps = new String[]{"nodep","dep"};
		String prefix = "/Users/allanjie/Dropbox/SUTD/AAAI17/exp/Testing";
		//String model = "lcrf";
//		String type = "abc";
//		String dep = "nodep";
		for(String model: models){
			for(String dep: deps){
				for(String type: types){
					String data = prefix+"/"+model+"/"+model+"-"+dep+"-test-"+type+".log";
					LogReader lr = new LogReader(model, data);
//					lr.calculateDecodeTime();
					lr.printFmeasure();
				}
				System.out.println();
			}
		}
		
	}
}

package com.statnlp.dp.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.statnlp.commons.crf.RAWF;

public class Test {

	public Test() {
		// TODO Auto-generated constructor stub
	}
	
	public static void countEntities(String inputFile) throws IOException{
		BufferedReader br = RAWF.reader(inputFile);
		String line = null;
		HashMap<String, Integer> maps = new HashMap<String, Integer>();
		while((line = br.readLine())!=null){
			if(line.equals("") || line.startsWith("#")) continue;
			String[] values = line.split("\\t");
			if(maps.containsKey(values[12])){
				int count = maps.get(values[12]);
				maps.put(values[12], count+1);
			}else{
				maps.put(values[12], 1);
			}
		}
		System.err.println(maps.toString());
	}
	
	public static void main(String[] args) throws IOException{
//		Pattern r = Pattern.compile("^[A-Z].+$");
//		Matcher ml= r.matcher("Dome");
		
		
		readTwoFiles("data/semeval10t1/ecrfWeight.txt","data/semeval10t1/divWeight.txt");
	}

	
	
	public static void readTwoFiles(String w1, String w2) throws IOException{
		BufferedReader br1 = RAWF.reader(w1);
		BufferedReader br2 = RAWF.reader(w2);
		String line1  = null;
		String line2 = null;
		while((line1 = br1.readLine())!=null){
			line2 = br2.readLine();
			String[] values1 = line1.split("<MYSEP>");
			String[] values2 = line2.split("<MYSEP>");
			if(values1[0].equals("[entity, ET, person:NNP]")) continue;
			if(!values1[0].equals(values2[0])){
				throw new RuntimeException("error");
			}else{
				double fw1 = Double.parseDouble(values1[1]);
				double fw2 = Double.parseDouble(values2[1]);
				double product = fw1*fw2;
				double diff = Math.abs(fw2)-Math.abs(fw1);
				if(diff>0){
					System.err.println(line1+",   "+line2);
				}
			}
		}
	}
}

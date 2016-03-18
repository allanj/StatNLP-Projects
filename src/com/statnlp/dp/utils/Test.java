package com.statnlp.dp.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
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
		Pattern r = Pattern.compile("^[A-Z].+$");
		Matcher ml= r.matcher("Dome");
	}

}

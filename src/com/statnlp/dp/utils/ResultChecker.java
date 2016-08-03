package com.statnlp.dp.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

import com.statnlp.commons.crf.RAWF;

public class ResultChecker {

	private String jointRes;
	private String[] entities;
	private HashMap<String, Integer> emap;
	
	/**
	 * Constructor function for result checker
	 * @param entities = {organizaiton, gpe, person and so on}
	 */
	public ResultChecker(String[] entities){
		this.entities = entities;
		emap = new HashMap<String, Integer>();
		for(int i=0;i<entities.length;i++) emap.put(entities[i], i);
		
		
	}
	
	/**
	 * This one check the predicted entity
	 * When the entity is wrong, what's the number of this wrong entity with length L.
	 */
	public void checkJointResult(){
		BufferedReader br;
		int[][] wrongTypeLen = new int[entities.length][10];
		try {
			br = RAWF.reader(jointRes);
			String line = null;
			boolean start = false;
			boolean correct = true;
			int len = -1;
			while((line = br.readLine())!=null){
				String[] values = line.split(" ");
				String pred = values[4];
				String corr = values[3];
				
				if(pred.equals("O") || (pred.startsWith("B-") && start)) start = false;
				if(pred.startsWith("B-")){
					start = true;
					len = 0;
				}
				if(start){
					len++;
				}
				
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}

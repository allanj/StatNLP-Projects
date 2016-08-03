package com.statnlp.entity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.dp.utils.DPConfig;

public class EntityChecker {

	
	public static void printAllEntities(String ecrfFile){
		PrintWriter pWriter;
		BufferedReader br;
		try {
			br = RAWF.reader(ecrfFile);
			pWriter = RAWF.writer("stat/testConnectedEntities.txt");
			String prev = "O";
			String prevLine = null;
			String line = null;
			int lineNum = 1;
			while((line = br.readLine())!=null){
				if(line.equals("")) {prev = "O";prevLine="";lineNum++; continue;}
				String[] vals = line.split("\\t");
				String entity = vals[3];
				if(entity.startsWith("B") && prev.startsWith("I")){
					pWriter.write("\n"+(lineNum-1)+" "+prevLine+"\n"+lineNum+" "+line+"\n");
				}
				prev = entity;
				prevLine = line;
				lineNum++;
//				else if(entity.startsWith("I")){
//					pWriter.write(line+"\n");
//				}
			}
			br.close();
			pWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		printAllEntities(DPConfig.ecrftrain);
	}
}

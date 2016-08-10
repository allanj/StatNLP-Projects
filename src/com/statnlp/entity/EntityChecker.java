package com.statnlp.entity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.commons.Entity;
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
	
	public static ArrayList<Entity> checkAllIncomplete(Sentence sent){
		int start = 0; int end = -1;
		ArrayList<Entity> elist = new ArrayList<Entity>();
		String prevEntity = "";
		for(int i=1;i<sent.length();i++){
			String e = sent.get(i).getEntity();
			e = e.equals("O")? e: e.substring(2);
			prevEntity = sent.get(i-1).getEntity();
			prevEntity = prevEntity.equals("O")? prevEntity:prevEntity.substring(2);
			//Need to fix the case of continuous two entity, start:inclusive, end:exclusive..Fixed by the reader already
			if(!e.equals(prevEntity)){
				if(!e.equals("O")){
					if(!prevEntity.equals("O")){
						end = i;
						if(notIncomplete(sent,start,end))
							elist.add(new Entity(prevEntity,start,end-1));
					}
					start = i;
				}else{
					end = i;
					if(notIncomplete(sent,start,end)){
						elist.add(new Entity(prevEntity,start,end-1));
					}
				}
			}
		}
		String lastE = sent.get(sent.length()-1).getEntity();
		lastE = lastE.equals("O")? lastE: lastE.substring(2);
		if(!lastE.equals("O")){
			end = sent.length();
			if(notIncomplete(sent,start,end))
				elist.add(new Entity(lastE,start,end-1));
		}
		return elist;
	}
	
	/**
	 * check this entity is incomplete or not
	 * @param sent: sentence
	 * @param start: inclusive
	 * @param end: exlucsive
	 * @return
	 */
	private static boolean notIncomplete(Sentence sent, int start, int end){
		return (end>start+1)  && !(sent.get(start).getHeadIndex()==end-1 || sent.get(end-1).getHeadIndex()==start);
	}
	
//	public static void main(String[] args){
//		printAllEntities(DPConfig.ecrftrain);
//	}
}

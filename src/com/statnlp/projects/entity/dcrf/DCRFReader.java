package com.statnlp.projects.entity.dcrf;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;

public class DCRFReader {

	public static List<DCRFInstance> readData(String path, boolean setLabel, int number,HashMap<String, Integer> entityMap) throws IOException{
		BufferedReader br = RAWF.reader(path);
		String line = null;
		List<DCRFInstance> insts = new ArrayList<DCRFInstance>();
		int index =1;
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		ArrayList<String> es = new ArrayList<String>();
		while((line = br.readLine())!=null){
			if(line.startsWith("#")) continue;
			if(line.equals("")){
				WordToken[] wordsArr = new WordToken[words.size()];
				words.toArray(wordsArr);
				Sentence sent = new Sentence(wordsArr);
				DCRFInstance inst = new DCRFInstance(index++,1.0,sent);
				inst.entities = es;
				if(setLabel) inst.setLabeled(); else inst.setUnlabeled();
				insts.add(inst);
				words = new ArrayList<WordToken>();
				es = new ArrayList<String>();
				if(number!=-1 && insts.size()==number) break;
				continue;
			}
			String[] values = line.split("\\t");
			//String entity = values[3].equals("O")? values[3]: values[3].substring(2, values[3].length());
			String entity = values[3];
			if(!entityMap.containsKey(entity)) {
				if(!entity.equals("O"))
					entity = entity.substring(0, 2)+"MISC";
				else entity = "O";
			}
			words.add(new WordToken(values[1],values[2],Integer.valueOf(values[4])-1,entity));
			es.add(entity);
		}
		br.close();
		List<DCRFInstance> myInsts = insts;
		String type = setLabel? "Training":"Testing";
		System.err.println(type+" instance, total:"+ myInsts.size()+" Instance. ");
		return myInsts;
	}
	
	
	public static List<DCRFInstance> readCONLLData(String path, boolean setLabel, int number) throws IOException{
		BufferedReader br = RAWF.reader(path);
		String line = null;
		List<DCRFInstance> insts = new ArrayList<DCRFInstance>();
		int index =1;
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		ArrayList<String> es = new ArrayList<String>();
		while((line = br.readLine())!=null){
			if(line.startsWith("#")) continue;
			if(line.equals("")){
				WordToken[] wordsArr = new WordToken[words.size()];
				words.toArray(wordsArr);
				Sentence sent = new Sentence(wordsArr);
				DCRFInstance inst = new DCRFInstance(index++,1.0,sent);
				inst.entities = es;
				words = new ArrayList<WordToken>();
				es = new ArrayList<String>();
//				if(sent.length()>25){
//					index--;
//					continue;
//				}
				if(setLabel) inst.setLabeled(); else inst.setUnlabeled();
				insts.add(inst);
				
				if(number!=-1 && insts.size()==number) break;
				continue;
			}
			String[] values = line.split(" ");
			String chunk = values[2];
			if(values[2].equals("B-NP") || values[2].equals("I-NP") || values[2].equals("O")){
				if(values[2].startsWith("B-")) chunk = "B";
				else if(values[2].startsWith("I-")) chunk = "I";
			}else chunk = "O";
			DEntity.get(chunk);
			Tag.get(values[1]);
			
			words.add(new WordToken(values[0], values[1], -1, chunk));
			es.add(chunk);
		}
		br.close();
		List<DCRFInstance> myInsts = insts;
		String type = setLabel? "Training":"Testing";
		System.err.println(type+" instance, total:"+ myInsts.size()+" Instance. ");
		return myInsts;
	}

	public static List<DCRFInstance> readCNN(String path, boolean setLabel, int number,HashMap<String, Integer> entityMap) throws IOException{
		BufferedReader br = RAWF.reader(path);
		String line = null;
		List<DCRFInstance> insts = new ArrayList<DCRFInstance>();
		int index =1;
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		ArrayList<String> es = new ArrayList<String>();
		while((line = br.readLine())!=null){
			if(line.startsWith("#")) continue;
			if(line.equals("")){
				WordToken[] wordsArr = new WordToken[words.size()];
				words.toArray(wordsArr);
				Sentence sent = new Sentence(wordsArr);
				DCRFInstance inst = new DCRFInstance(index++,1.0,sent);
				inst.entities = es;
				if(setLabel) inst.setLabeled(); else inst.setUnlabeled();
				insts.add(inst);
				words = new ArrayList<WordToken>();
				es = new ArrayList<String>();
				if(number!=-1 && insts.size()==number) break;
				continue;
			}
			String[] values = line.split(" ");
			String entity = values[3];
			if(!entityMap.containsKey(entity)) entity = "O";
			words.add(new WordToken(values[1],values[2],Integer.valueOf(values[4])-1,entity));
			es.add(entity);
		}
		br.close();
		List<DCRFInstance> myInsts = insts;
		String type = setLabel? "Training":"Testing";
		System.err.println(type+" instance, total:"+ myInsts.size()+" Instance. ");
		return myInsts;
	}
	
}

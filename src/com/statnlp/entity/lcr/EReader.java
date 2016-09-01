package com.statnlp.entity.lcr;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;
import com.statnlp.entity.Entity;

public class EReader {

	public static List<ECRFInstance> readData(String path, boolean setLabel, int number) throws IOException{
		BufferedReader br = RAWF.reader(path);
		String line = null;
		List<ECRFInstance> insts = new ArrayList<ECRFInstance>();
		int index =1;
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		ArrayList<String> es = new ArrayList<String>();
		while((line = br.readLine())!=null){
			if(line.startsWith("#")) continue;
			if(line.equals("")){
				WordToken[] wordsArr = new WordToken[words.size()];
				words.toArray(wordsArr);
				Sentence sent = new Sentence(wordsArr);
				ECRFInstance inst = new ECRFInstance(index++,1.0,sent);
				inst.entities = es;
				if(setLabel) inst.setLabeled(); else inst.setUnlabeled();
				insts.add(inst);
				words = new ArrayList<WordToken>();
				es = new ArrayList<String>();
				if(number!=-1 && insts.size()==number) break;
				continue;
			}
			String[] values = line.split("\\t");
			String entity = values[10];
			Entity.get(entity);
			words.add(new WordToken(values[1],values[4],Integer.valueOf(values[6])-1,entity,values[7]));
			es.add(entity);
		}
		br.close();
		List<ECRFInstance> myInsts = insts;
		String type = setLabel? "Training":"Testing";
		System.err.println(type+" instance, total:"+ myInsts.size()+" Instance. ");
		return myInsts;
	}

	
	public static List<ECRFInstance> readDP2NERPipe(String path, int number) throws IOException{
		BufferedReader br = RAWF.reader(path);
		String line = null;
		List<ECRFInstance> insts = new ArrayList<ECRFInstance>();
		int index = 1;
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		ArrayList<String> es = new ArrayList<String>();
		double instanceWeight = 1.0;
		int globalId = -1;
		while((line = br.readLine())!=null){
			if(line.startsWith("#")) continue;
			if(line.equals("")){
				WordToken[] wordsArr = new WordToken[words.size()];
				words.toArray(wordsArr);
				Sentence sent = new Sentence(wordsArr);
				ECRFInstance inst = new ECRFInstance(globalId, index++,instanceWeight,sent); //The instance weight is not used during testing phase
				inst.entities = es;
				inst.setUnlabeled();
				insts.add(inst);
				words = new ArrayList<WordToken>();
				es = new ArrayList<String>();
				instanceWeight = 1.0;
				globalId = -1;
				if(number!=-1 && insts.size()==number) break;
				continue;
			}
			if(line.startsWith("[InstanceId+Weight]")){
				String[] values = line.split(":");
				instanceWeight = Double.valueOf(values[2]);
				globalId = Integer.valueOf(values[1]);
				continue;
			}
			String[] values = line.split(" ");
			//String entity = values[3].equals("O")? values[3]: values[3].substring(2, values[3].length());
			String entity = values[3];
			Entity.get(entity);
			words.add(new WordToken(values[1],values[2],Integer.valueOf(values[5])-1,entity));
			es.add(entity);
		}
		br.close();
		List<ECRFInstance> myInsts = insts;
		System.err.println("[Pipeline] Testing instance, total:"+ myInsts.size()+" Instance. ");
		return myInsts;
	}
	
	public static List<ECRFInstance> readCNN(String path, boolean setLabel, int number,boolean isPipe) throws IOException{
		if(setLabel && isPipe) throw new RuntimeException("training instances always have the true dependency structure");
		BufferedReader br = RAWF.reader(path);
		String line = null;
		List<ECRFInstance> insts = new ArrayList<ECRFInstance>();
		int index =1;
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		ArrayList<String> es = new ArrayList<String>();
		String prevEntity = "O";
//		int lineNumber = 0;
		while((line = br.readLine())!=null){
//			lineNumber++;
			if(line.startsWith("#")) continue;
			if(line.equals("")){
				WordToken[] wordsArr = new WordToken[words.size()];
				words.toArray(wordsArr);
				Sentence sent = new Sentence(wordsArr);
				ECRFInstance inst = new ECRFInstance(index++,1.0,sent);
				inst.entities = es;
				if(setLabel) inst.setLabeled(); else inst.setUnlabeled();
				insts.add(inst);
				words = new ArrayList<WordToken>();
				es = new ArrayList<String>();
				prevEntity = "O";
				if(number!=-1 && insts.size()==number) break;
				continue;
			}
			String[] values = line.split(" ");
			String entity = values[3];
			/***This part of code check the consecutive entities with same types**/
			if(!prevEntity.equals("O") && !entity.equals("O") && prevEntity.substring(2).equals(entity.substring(2)) && entity.startsWith("B-")){
				//TODO: nothing 
			}
			/***/
			Entity.get(entity);
			int headIdx = Integer.valueOf(values[4])-1;
			if(isPipe) headIdx = Integer.valueOf(values[5])-1;
			words.add(new WordToken(values[1],values[2],headIdx,entity));
			es.add(entity);
			prevEntity = entity;
		}
		br.close();
		List<ECRFInstance> myInsts = insts;
		String type = setLabel? "Training":"Testing";
		System.err.println(type+" instance, total:"+ myInsts.size()+" Instance. ");
		return myInsts;
	}
	
	
	public static List<ECRFInstance> readCoNLLX(String path, boolean setLabel, int number,boolean isPipe) throws IOException{
		if(setLabel && isPipe) throw new RuntimeException("training instances always have the true dependency structure");
		BufferedReader br = RAWF.reader(path);
		String line = null;
		List<ECRFInstance> insts = new ArrayList<ECRFInstance>();
		int index =1;
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		ArrayList<String> es = new ArrayList<String>();
		String prevEntity = "O";
//		int lineNumber = 0;
		while((line = br.readLine())!=null){
//			lineNumber++;
			if(line.startsWith("#")) continue;
			if(line.equals("")){
				WordToken[] wordsArr = new WordToken[words.size()];
				words.toArray(wordsArr);
				Sentence sent = new Sentence(wordsArr);
				ECRFInstance inst = new ECRFInstance(index++,1.0,sent);
				inst.entities = es;
				if(setLabel) inst.setLabeled(); else inst.setUnlabeled();
				insts.add(inst);
				words = new ArrayList<WordToken>();
				es = new ArrayList<String>();
				prevEntity = "O";
				if(number!=-1 && insts.size()==number) break;
				continue;
			}
			String[] values = line.split("\\t");
			String entity = values[10];
			/***This part of code check the consecutive entities with same types**/
			if(!prevEntity.equals("O") && !entity.equals("O") && prevEntity.substring(2).equals(entity.substring(2)) && entity.startsWith("B-")){
				//TODO: nothing 
			}
			/***/
			Entity.get(entity);
			int headIdx = Integer.valueOf(values[6])-1;
			String depLabel = values[7];
			if(isPipe) {
				if(values.length<13) throw new RuntimeException("No predicted dependency apperared");
				headIdx = Integer.valueOf(values[11])-1;
				depLabel = values[12];
			}
			words.add(new WordToken(values[1],values[4],headIdx,entity, depLabel));
			es.add(entity);
			prevEntity = entity;
		}
		br.close();
		List<ECRFInstance> myInsts = insts;
		String type = setLabel? "Training":"Testing";
		System.err.println(type+" instance, total:"+ myInsts.size()+" Instance. ");
		return myInsts;
	}

	
}

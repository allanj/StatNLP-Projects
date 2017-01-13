package com.statnlp.projects.entity.lcr;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.io.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;
import com.statnlp.projects.dep.utils.DataChecker;
import com.statnlp.projects.entity.Entity;

public class EReader {

	public static List<ECRFInstance> readCoNLLX(String path, boolean setLabel, int number, boolean iobes) throws IOException{
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
				if (iobes) {
					encodeIOBES(es);
				}
				inst.entities = es;
				if(setLabel) inst.setLabeled(); else inst.setUnlabeled();
				/***debug info*/
				List<Integer> headList = new ArrayList<Integer>(words.size());
				for (int h = 0; h < words.size(); h++)
					headList.add(inst.getInput().get(h).getHeadIndex());
				boolean projective = DataChecker.checkProjective(headList);
				if (setLabel && !projective) {
					words = new ArrayList<WordToken>();
					es = new ArrayList<String>();
					continue;
				}
				/***/
				insts.add(inst);
				words = new ArrayList<WordToken>();
				es = new ArrayList<String>();
				if(number!=-1 && insts.size()==number) break;
				continue;
			}
			String[] values = line.split("\\t");
			String entity = values[10];
			Entity.get(entity);
			int headIdx = Integer.valueOf(values[6])-1;
			String depLabel = values[7];
			words.add(new WordToken(values[1],values[4],headIdx,entity, depLabel));
			es.add(entity);
		}
		br.close();
		List<ECRFInstance> myInsts = insts;
		String type = setLabel? "Training":"Testing";
		System.err.println(type+" instance, total:"+ myInsts.size()+" Instance. ");
		return myInsts;
	}
	
	private static void encodeIOBES(ArrayList<String> entities){
		for(int i = 0; i < entities.size(); i++){
			String curr = entities.get(i);
			if(curr.startsWith("B")){
				if ( i + 1 < entities.size()){
					if (!entities.get(i+1).startsWith("I")){
						entities.set(i, "S"+curr.substring(1));
						Entity.get(entities.get(i));
					} //else remains the same
				}else{
					entities.set(i, "S"+curr.substring(1));
					Entity.get(entities.get(i));
				}
			}else if (curr.startsWith("I")){
				if( i + 1 < entities.size()){
					if(!entities.get(i+1).startsWith("I")){
						entities.set(i, "E"+curr.substring(1));
						Entity.get(entities.get(i));
					}
				}else{
					entities.set(i, "E"+curr.substring(1));
					Entity.get(entities.get(i));
				}
			}
		}
	}

	
}

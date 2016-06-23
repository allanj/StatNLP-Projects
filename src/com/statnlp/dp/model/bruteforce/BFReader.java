package com.statnlp.dp.model.bruteforce;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;


public class BFReader {
	
	public static List<BFInstance> readData(String path, boolean setLabel, int number) throws IOException{
		BufferedReader br = RAWF.reader(path);
		String line = null;
		List<BFInstance> insts = new ArrayList<BFInstance>();
		int index =1;
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		words.add(new WordToken("ROOT","STR",-1,"NON-E"));
		ArrayList<String> es = new ArrayList<String>();
		es.add("O");
		while((line = br.readLine())!=null){
			if(line.startsWith("#")) continue;
			if(line.equals("")){
				WordToken[] wordsArr = new WordToken[words.size()];
				words.toArray(wordsArr);
				Sentence sent = new Sentence(wordsArr);
				BFInstance inst = new BFInstance(index++,1.0,sent);
				inst.entities = es;
				if(setLabel) inst.setLabeled(); else inst.setUnlabeled();
				insts.add(inst);
				words = new ArrayList<WordToken>();
				words.add(new WordToken("ROOT","STR",-1,"NON-E"));
				es = new ArrayList<String>();
				es.add("O");
				if(number!=-1 && insts.size()==number) break;
				continue;
			}
			String[] values = line.split("\\t");
			String entity = values[3];
			String eInSet = entity.length()>2? entity.substring(2):entity;
			Entity.get(eInSet);
			words.add(new WordToken(values[1],values[2],Integer.valueOf(values[4]),entity));
			es.add(entity);
		}
		br.close();
		List<BFInstance> myInsts = insts;
		String type = setLabel? "Training":"Testing";
		System.err.println(type+" instance, total:"+ myInsts.size()+" Instance. ");
		return myInsts;
	}

}

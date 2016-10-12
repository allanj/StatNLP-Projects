package com.statnlp.projects.nndcrf.linear_pos;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;

public class POSReader {

	
	/**
	 * This one is POS tag reader
	 * @param path
	 * @param setLabel
	 * @param number
	 * @return
	 * @throws IOException
	 */
	public static List<POSInstance> readCONLL2000Data(String path, boolean setLabel, int number) throws IOException{
		BufferedReader br = RAWF.reader(path);
		String line = null;
		List<POSInstance> insts = new ArrayList<POSInstance>();
		int index =1;
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		ArrayList<String> es = new ArrayList<String>();
		while((line = br.readLine())!=null){
			if(line.equals("")){
				WordToken[] wordsArr = new WordToken[words.size()];
				words.toArray(wordsArr);
				Sentence sent = new Sentence(wordsArr);
				POSInstance inst = new POSInstance(index++,1.0,sent);
				inst.tags = es;
				words = new ArrayList<WordToken>();
				es = new ArrayList<String>();
				if(setLabel) inst.setLabeled(); else inst.setUnlabeled();
				insts.add(inst);
				
				if(number!=-1 && insts.size()==number) break;
				continue;
			}
			String[] values = line.split(" ");
			POS.get(values[1]);
			
			words.add(new WordToken(values[0], values[1], -1, values[2]));
			es.add(values[1]);
		}
		br.close();
		POS.get("START");
		POS.get("END");
		List<POSInstance> myInsts = insts;
		String type = setLabel? "Training":"Testing";
		System.err.println(type+" instance, total:"+ myInsts.size()+" Instance. ");
		return myInsts;
	}
	
	
}

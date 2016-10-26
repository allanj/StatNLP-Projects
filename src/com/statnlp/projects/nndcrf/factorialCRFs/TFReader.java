package com.statnlp.projects.nndcrf.factorialCRFs;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;
import com.statnlp.projects.nndcrf.factorialCRFs.TFConfig.TASK;

public class TFReader {
	
	public static List<TFInstance> readCONLLData(String path, boolean setLabel, int number, boolean npchunk, boolean IOBES, TASK task) throws IOException{
		//By default it's not cascaded
		return readCONLLData(path, setLabel, number, npchunk, IOBES, task, false);
	}
	
	public static List<TFInstance> readCONLLData(String path, boolean setLabel, int number, boolean npchunk, boolean IOBES, TASK task, boolean cascade) throws IOException{
		BufferedReader br = RAWF.reader(path);
		String line = null;
		List<TFInstance> insts = new ArrayList<TFInstance>();
		int index = 1;
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		ArrayList<String> es = new ArrayList<String>();
		while((line = br.readLine())!=null){
			if(line.equals("")){
				WordToken[] wordsArr = new WordToken[words.size()];
				words.toArray(wordsArr);
				Sentence sent = new Sentence(wordsArr);
				TFInstance inst = new TFInstance(index++,1.0,sent);
				if((task==TASK.NER || task==TASK.JOINT) && IOBES) {
					encodeIOBES(es);
				}
				inst.entities = es;
				words = new ArrayList<WordToken>();
				es = new ArrayList<String>();
				if(setLabel) inst.setLabeled(); else inst.setUnlabeled();
				insts.add(inst);
				
				if(number!=-1 && insts.size()==number) break;
				continue;
			}
			String[] values = line.split(" ");
			String rawChunk = values[2];
			String pos = values[1];
			String word = values[0];
			
			if(task==TASK.NER && cascade) {
				pos = values[2];
				rawChunk = values[3];
			}
			if(task==TASK.TAGGING && cascade) {
				rawChunk = values[3];
			}
			
			String chunk = npchunk? getNPChunk(rawChunk):rawChunk;
			if(task==TASK.NER || task==TASK.JOINT) Entity.get(chunk);
			if(task==TASK.TAGGING || task==TASK.JOINT) Tag.get(pos);
			
			
			words.add(new WordToken(word, pos, -1, chunk));
			es.add(chunk);
		}
		br.close();
		List<TFInstance> myInsts = insts;
		String type = setLabel? "Training":"Testing";
		System.err.println(type+" instance, total:"+ myInsts.size()+" Instance. ");
		return myInsts;
	}
	
	
	/**
	 * Return the NP chunk label: 
	 * @param rawChunk
	 * @return B or I or O
	 */
	private static String getNPChunk(String rawChunk){
		String chunk = null;
		if(rawChunk.equals("B-NP") || rawChunk.equals("I-NP") || rawChunk.equals("O")){
			if(rawChunk.startsWith("B-")) chunk = "B";
			else if(rawChunk.startsWith("I-")) chunk = "I";
			else chunk = rawChunk;
		}else chunk = "O";
		return chunk;
	}
	
	
	private static void encodeIOBES(ArrayList<String> chunks){
		for(int i=0;i<chunks.size();i++){
			String curr = chunks.get(i);
			if(curr.startsWith("B")){
				if((i+1)<chunks.size()){
					if(!chunks.get(i+1).startsWith("I")){
						chunks.set(i, "S"+curr.substring(1));
						Entity.get(chunks.get(i));
					} //else remains the same
				}else{
					chunks.set(i, "S"+curr.substring(1));
					Entity.get(chunks.get(i));
				}
			}else if(curr.startsWith("I")){
				if((i+1)<chunks.size()){
					if(!chunks.get(i+1).startsWith("I")){
						chunks.set(i, "E"+curr.substring(1));
						Entity.get(chunks.get(i));
					}
				}else{
					chunks.set(i, "E"+curr.substring(1));
					Entity.get(chunks.get(i));
				}
			}
		}
	}
}

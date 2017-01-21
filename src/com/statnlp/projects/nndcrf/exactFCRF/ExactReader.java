package com.statnlp.projects.nndcrf.exactFCRF;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.io.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;

public class ExactReader {
	
	
	public static List<ExactInstance> readCONLLData(String path, boolean setLabel, int number, boolean npchunk, boolean IOBES) throws IOException{
		BufferedReader br = RAWF.reader(path);
		String line = null;
		List<ExactInstance> insts = new ArrayList<ExactInstance>();
		int index = 1;
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		ArrayList<String> es = new ArrayList<String>();
		while((line = br.readLine())!=null){
			if(line.equals("")){
				WordToken[] wordsArr = new WordToken[words.size()];
				words.toArray(wordsArr);
				Sentence sent = new Sentence(wordsArr);
				ExactInstance inst = new ExactInstance(index++,1.0,sent);
				if(IOBES) {
					encodeIOBES(es);
				}
				inst.output = es;
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
			
			
			String chunk = npchunk? getNPChunk(rawChunk):rawChunk;
			ChunkLabel.get(chunk);
			TagLabel.get(pos);
			words.add(new WordToken(word, pos, -1, chunk));
			es.add(chunk);
		}
		br.close();
		List<ExactInstance> myInsts = insts;
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
			if(rawChunk.startsWith("B-")) chunk = "B-NP";
			else if(rawChunk.startsWith("I-")) chunk = "I-NP";
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
						ChunkLabel.get(chunks.get(i));
					} //else remains the same
				}else{
					chunks.set(i, "S"+curr.substring(1));
					ChunkLabel.get(chunks.get(i));
				}
			}else if(curr.startsWith("I")){
				if((i+1)<chunks.size()){
					if(!chunks.get(i+1).startsWith("I")){
						chunks.set(i, "E"+curr.substring(1));
						ChunkLabel.get(chunks.get(i));
					}
				}else{
					chunks.set(i, "E"+curr.substring(1));
					ChunkLabel.get(chunks.get(i));
				}
			}
		}
	}

}

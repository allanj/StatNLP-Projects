package com.statnlp.projects.nndcrf.linear_chunk;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;

public class ChunkReader {

	public static List<ChunkInstance> readCONLL2000Data(String path, boolean setLabel, int number) throws IOException{
		//NPchunking and , IOB encoding
		return readCONLL2000Data(path, setLabel, number, true, false, false); 
	}
	
	public static List<ChunkInstance> readCONLL2000Data(String path, boolean setLabel, int number, boolean npchunk) throws IOException{
		//default: IOB encoding
		return readCONLL2000Data(path, setLabel, number, npchunk, false, false); 
	}
	
	
	public static List<ChunkInstance> readCONLL2000Data(String path, boolean setLabel, int number, boolean npchunk, boolean IOBES) throws IOException{
		return readCONLL2000Data(path, setLabel, number, npchunk, IOBES, false); 
	}
	
	/**
	 * This one is IOB encoding of NP chunks
	 * @param path
	 * @param setLabel
	 * @param number
	 * @param npchunk: NP chunking only?
	 * @param IOBES encoding
	 * @return
	 * @throws IOException
	 */
	public static List<ChunkInstance> readCONLL2000Data(String path, boolean setLabel, int number, boolean npchunk, boolean IOBES, boolean cascade) throws IOException{
		if(setLabel && cascade)
			throw new RuntimeException("labeled instance, shouldn't use predicted POS tag");
		BufferedReader br = RAWF.reader(path);
		String line = null;
		List<ChunkInstance> insts = new ArrayList<ChunkInstance>();
		int index =1;
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		ArrayList<String> es = new ArrayList<String>();
		while((line = br.readLine())!=null){
			if(line.equals("")){
				WordToken[] wordsArr = new WordToken[words.size()];
				words.toArray(wordsArr);
				Sentence sent = new Sentence(wordsArr);
				ChunkInstance inst = new ChunkInstance(index++,1.0,sent);
				if(IOBES) {
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
			String posTag = cascade? values[3]:values[1];
			String word = values[0];
			String chunk = npchunk? getNPChunk(rawChunk):rawChunk;
			Chunk.get(chunk);
			words.add(new WordToken(word, posTag, -1, chunk));
			es.add(chunk);
		}
		br.close();
		List<ChunkInstance> myInsts = insts;
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
						Chunk.get(chunks.get(i));
					} //else remains the same
				}else{
					chunks.set(i, "S"+curr.substring(1));
					Chunk.get(chunks.get(i));
				}
			}else if(curr.startsWith("I")){
				if((i+1)<chunks.size()){
					if(!chunks.get(i+1).startsWith("I")){
						chunks.set(i, "E"+curr.substring(1));
						Chunk.get(chunks.get(i));
					}
				}else{
					chunks.set(i, "E"+curr.substring(1));
					Chunk.get(chunks.get(i));
				}
			}
		}
	}
}

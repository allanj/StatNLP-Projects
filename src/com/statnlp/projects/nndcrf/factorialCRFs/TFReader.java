package com.statnlp.projects.nndcrf.factorialCRFs;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;

public class TFReader {
	
	public static List<TFInstance> readCONLLData(String path, boolean setLabel, int number) throws IOException{
		BufferedReader br = RAWF.reader(path);
		String line = null;
		List<TFInstance> insts = new ArrayList<TFInstance>();
		int index =1;
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		ArrayList<String> es = new ArrayList<String>();
		while((line = br.readLine())!=null){
			if(line.equals("")){
				WordToken[] wordsArr = new WordToken[words.size()];
				words.toArray(wordsArr);
				Sentence sent = new Sentence(wordsArr);
				TFInstance inst = new TFInstance(index++,1.0,sent);
				inst.entities = es;
				words = new ArrayList<WordToken>();
				es = new ArrayList<String>();
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
			Entity.get(chunk);
			Tag.get(values[1]);
			
			words.add(new WordToken(values[0], values[1], -1, chunk));
			es.add(chunk);
		}
		br.close();
		List<TFInstance> myInsts = insts;
		String type = setLabel? "Training":"Testing";
		System.err.println(type+" instance, total:"+ myInsts.size()+" Instance. ");
		return myInsts;
	}
	
	public static List<TFInstance> readGRMMData(String path, boolean isLabel, int number) throws IOException{
		BufferedReader br = RAWF.reader(path);
		String line = null;
		int index =1;
		List<TFInstance> insts = new ArrayList<TFInstance>();
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		ArrayList<String> es = new ArrayList<String>();
		while((line = br.readLine())!=null){
			if(line.equals("")){
				WordToken[] wordsArr = new WordToken[words.size()];
				words.toArray(wordsArr);
				Sentence sent = new Sentence(wordsArr);
				TFInstance inst = new TFInstance(index++,1.0,sent);
				inst.entities = es;
				words = new ArrayList<WordToken>();
				es = new ArrayList<String>();
				if(isLabel) inst.setLabeled(); else inst.setUnlabeled();
				insts.add(inst);
				if(number!=-1 && insts.size()==number) break;
				continue;
			}
			String[] vals = line.split(" ");
			String tag = vals[0];
			String entity = vals[1];
			Entity.get(entity);
			Tag.get(tag);
			//the feature string.
			String[] fs = new String[vals.length-3];
			for(int i=3;i<vals.length;i++){
				fs[i-3] = vals[i];
			}
			//System.err.println(Arrays.toString(fs));
			WordToken wt = new WordToken("noname", tag, -1, entity);
			wt.setFS(fs);
			words.add(wt);
			es.add(entity);
		}
		
		br.close();
		List<TFInstance> myInsts = insts;
		String type = isLabel? "Training":"Testing";
		System.err.println(type+" instance, total:"+ myInsts.size()+" Instance. ");
		return myInsts;
	}
	
	
	public static List<TFInstance> readGRMMDataAndWord(String path, boolean isLabel, int number, String wordPath) throws IOException{
		BufferedReader br = RAWF.reader(path);
		BufferedReader srcReader = RAWF.reader(wordPath);
		String line = null;
		String srcLine = null;
		int index =1;
		List<TFInstance> insts = new ArrayList<TFInstance>();
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		ArrayList<String> es = new ArrayList<String>();
		while((line = br.readLine())!=null){
			if(line.equals("")){
				WordToken[] wordsArr = new WordToken[words.size()];
				words.toArray(wordsArr);
				Sentence sent = new Sentence(wordsArr);
				TFInstance inst = new TFInstance(index++,1.0,sent);
				inst.entities = es;
				words = new ArrayList<WordToken>();
				es = new ArrayList<String>();
				if(isLabel) inst.setLabeled(); else inst.setUnlabeled();
				insts.add(inst);
				srcLine = srcReader.readLine();
				if(number!=-1 && insts.size()==number) break;
				continue;
			}
			String[] vals = line.split(" ");
			String tag = vals[0];
			String entity = vals[1];
			Entity.get(entity);
			Tag.get(tag);
			//the feature string.
			String[] fs = new String[vals.length-3];
			for(int i=3;i<vals.length;i++){
				fs[i-3] = vals[i];
			}
			//System.err.println(Arrays.toString(fs));
			srcLine = srcReader.readLine();
			String[] srcVals = srcLine.split(" ");
			WordToken wt = new WordToken(srcVals[0], tag, -1, entity);
			wt.setFS(fs);
			words.add(wt);
			es.add(entity);
		}
		br.close();
		srcReader.close();
		List<TFInstance> myInsts = insts;
		String type = isLabel? "Training":"Testing";
		System.err.println(type+" instance, total:"+ myInsts.size()+" Instance. ");
		return myInsts;
	}
	
}

package com.statnlp.projects.mfjoint_linear;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.io.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;
import com.statnlp.projects.dep.utils.DataChecker;

public class MFLReader {

	public static String ROOT_WORD = "ROOT";
	public static String ROOT_TAG = "ROOT";

	/**
	 * Reading the CoNLL-X format data
	 * @param fileName
	 * @param isLabeled
	 * @param number
	 * @param checkProjective: check the projectiveness or not
	 * @param lenOne: means if we restrict all segments are with length 1. The label is still without IOBES encoding
	 *               also it select the head for the span starting from the left portion.
	 * @return
	 * @throws IOException
	 */
	public static MFLInstance[] readCoNLLXData(String fileName, boolean isLabeled, int number, boolean checkProjective, boolean iobes) throws IOException{
		BufferedReader br = RAWF.reader(fileName);
		ArrayList<MFLInstance> result = new ArrayList<MFLInstance>();
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		List<Integer> originalHeads = new ArrayList<>();
		ArrayList<String> enList = new ArrayList<>();
		int maxSentenceLength = -1;
		int maxEntityLength = -1;
		words.add(new WordToken(ROOT_WORD, ROOT_TAG));
		enList.add("O");
		originalHeads.add(-1);
		int instanceId = 1;
		int numOfNotTree = 0;
		int nonProjective = 0;
		while(br.ready()){
			String line = br.readLine().trim();
			if(line.length() == 0){
				WordToken[] wtArr = new WordToken[words.size()];
				int[] heads = new int[originalHeads.size()];
				String[] entities = new String[words.size()];
				enList.toArray(entities);
				for (int i = 0; i < originalHeads.size(); i++) heads[i] = originalHeads.get(i);
				MFLInstance instance = new MFLInstance(instanceId, 1.0, new Sentence(words.toArray(wtArr)));
				instance.input.setRecognized();
				if(iobes) {
					encodeIOBES(entities);
				}
				instance.output = new MFLPair(heads, entities);
				
				boolean projectiveness = DataChecker.checkProjective(originalHeads);

				boolean isTree = DataChecker.checkIsTree(originalHeads);
				if (isLabeled) {
					if(!isTree || (checkProjective && !projectiveness)) {
						words = new ArrayList<WordToken>();
						words.add(new WordToken(ROOT_WORD, ROOT_TAG));
						originalHeads = new ArrayList<>();
						originalHeads.add(-1);
						enList = new ArrayList<>();
						enList.add("O");
						if (!isTree) numOfNotTree++;
						if (checkProjective && !projectiveness) nonProjective++;
						continue;
					}
				}
				if(isLabeled){
					instance.setLabeled(); // Important!
				} else {
					instance.setUnlabeled();
				}
				instanceId++;
				result.add(instance);
				maxSentenceLength = Math.max(maxSentenceLength, instance.size());
				words = new ArrayList<WordToken>();
				words.add(new WordToken(ROOT_WORD, ROOT_TAG));
				enList = new ArrayList<>();
				enList.add("O");
				originalHeads = new ArrayList<>();
				originalHeads.add(-1);
				if(result.size()==number)
					break;
			} else {
				String[] values = line.split("[\t ]");
				String word = values[1];
				String pos = values[4];
				int headIdx = Integer.parseInt(values[6]);
				String depLabel = values[7];
				String form = values[10];
				if(depLabel.contains("|")) throw new RuntimeException("Mutiple label?");
				words.add(new WordToken(word, pos));
				originalHeads.add(headIdx);
				enList.add(form);
				MFLLabel.get(form);
			}
		}
		br.close();
		String type = isLabeled? "train":"test";
		System.err.println("[Info] number of "+type+" instances:"+ result.size());
		System.err.println("[Info] max sentence length: " + maxSentenceLength);
		System.err.println("[Info] max entity length: " + maxEntityLength);
		if (isLabeled) {
			System.err.println("[Info] #not tree: " + numOfNotTree);
			System.err.println("[Info] #non projective: " + nonProjective);
		}
		return result.toArray(new MFLInstance[result.size()]);
	}
	
	private static void encodeIOBES(String[] entities){
		for(int i = 0; i < entities.length; i++){
			String curr = entities[i];
			if(curr.startsWith("B")){
				if ( i + 1 < entities.length){
					if (!entities[i+1].startsWith("I")){
						entities[i] = "S"+curr.substring(1);
						MFLLabel.get(entities[i]);
					} //else remains the same
				}else{
					entities[i] = "S"+curr.substring(1);
					MFLLabel.get(entities[i]);
				}
			}else if (curr.startsWith("I")){
				if( i + 1 < entities.length){
					if(!entities[i+1].startsWith("I")){
						entities[i] = "E"+curr.substring(1);
						MFLLabel.get(entities[i]);
					}
				}else{
					entities[i] = "E"+curr.substring(1);
					MFLLabel.get(entities[i]);
				}
			}
		}
	}
	
	public static void readResultFile(String resultFile, MFLInstance[] testInsts) throws IOException{
		BufferedReader br = RAWF.reader(resultFile);
		List<Integer> originalHeads = new ArrayList<>();
		ArrayList<String> enList = new ArrayList<>();
		enList.add("O");
		originalHeads.add(-1);
		int index = 0;
		while(br.ready()){
			String line = br.readLine().trim();
			if(line.length() == 0){
				int[] heads = new int[originalHeads.size()];
				String[] entities = new String[enList.size()];
				enList.toArray(entities);
				for (int i = 0; i < originalHeads.size(); i++) heads[i] = originalHeads.get(i);
				testInsts[index].setPrediction(new MFLPair(heads, entities));
				index++;
				enList = new ArrayList<>();
				enList.add("O");
				originalHeads = new ArrayList<>();
				originalHeads.add(-1);
			} else {
				String[] values = line.split("[\t ]");
				int headIdx = Integer.parseInt(values[4]);
				String form = values[3];
				originalHeads.add(headIdx);
				enList.add(form);
			}
		}
		br.close();
	}
}

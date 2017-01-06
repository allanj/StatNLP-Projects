package com.statnlp.projects.mfjoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.io.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;
import com.statnlp.projects.dep.utils.DataChecker;

public class MFJReader {

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
	public static MFJInstance[] readCoNLLXData(String fileName, boolean isLabeled, int number, boolean checkProjective) throws IOException{
		BufferedReader br = RAWF.reader(fileName);
		ArrayList<MFJInstance> result = new ArrayList<MFJInstance>();
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		List<Integer> originalHeads = new ArrayList<>();
		int maxSentenceLength = -1;
		int maxEntityLength = -1;
		words.add(new WordToken(ROOT_WORD, ROOT_TAG));
		originalHeads.add(-1);
		ArrayList<MFJSpan> segments = new ArrayList<MFJSpan>();
		segments.add(new MFJSpan(0, 0, MFJLabel.get("O")));
		int instanceId = 1;
		int start = -1;
		int end = 0;
		int numOfNotTree = 0;
		int nonProjective = 0;
		MFJLabel prevLabel = null;
		while(br.ready()){
			String line = br.readLine().trim();
			if(line.length() == 0){
				end = words.size()-1;
				if(start != -1){
					createSpan(segments, start, end, prevLabel);
					maxEntityLength = Math.max(maxEntityLength, segments.get(segments.size()-1).length());
				}
				start = -1;
				WordToken[] wtArr = new WordToken[words.size()];
				int[] heads = new int[originalHeads.size()];
				for (int i = 0; i < originalHeads.size(); i++) heads[i] = originalHeads.get(i);
				MFJInstance instance = new MFJInstance(instanceId, 1.0, new Sentence(words.toArray(wtArr)));
				instance.input.setRecognized();
				instance.output = new MFJPair(heads, segments);
				
				boolean projectiveness = DataChecker.checkProjective(originalHeads);

				boolean isTree = DataChecker.checkIsTree(originalHeads);
				if (isLabeled) {
					if(!isTree || (checkProjective && !projectiveness)) {
						words = new ArrayList<WordToken>();
						words.add(new WordToken(ROOT_WORD, ROOT_TAG));
						originalHeads = new ArrayList<>();
						originalHeads.add(-1);
						segments = new ArrayList<MFJSpan>();
						segments.add(new MFJSpan(0, 0, MFJLabel.get("O")));
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
				segments = new ArrayList<MFJSpan>();
				segments.add(new MFJSpan(0, 0, MFJLabel.get("O")));
				originalHeads = new ArrayList<>();
				originalHeads.add(-1);
				prevLabel = null;
				start = -1;
				end = 0;
				if(result.size()==number)
					break;
			} else {
				String[] values = line.split("[\t ]");
				int index = Integer.valueOf(values[0]); //because it is starting from 1
				String word = values[1];
				String pos = values[4];
				int headIdx = Integer.parseInt(values[6]);
				String depLabel = values[7];
				String form = values[10];
				if(depLabel.contains("|")) throw new RuntimeException("Mutiple label?");
				words.add(new WordToken(word, pos));
				originalHeads.add(headIdx);
				MFJLabel label = null;
				if(form.startsWith("B")){
					if(start != -1){
						end = index - 1;
						createSpan(segments, start, end, prevLabel);
						maxEntityLength = Math.max(maxEntityLength, segments.get(segments.size()-1).length());
					}
					start = index;
					label = MFJLabel.get(form.substring(2));
					
				} else if(form.startsWith("I")){
					label = MFJLabel.get(form.substring(2));
				} else if(form.startsWith("O")){
					if(start != -1){
						end = index - 1;
						createSpan(segments, start, end, prevLabel);
						maxEntityLength = Math.max(maxEntityLength, segments.get(segments.size()-1).length());
					}
					start = -1;
					createSpan(segments, index, index, MFJLabel.get("O"));
					maxEntityLength = Math.max(maxEntityLength, segments.get(segments.size()-1).length());
					label = MFJLabel.get("O");
				}
				prevLabel = label;
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
		return result.toArray(new MFJInstance[result.size()]);
	}
	
 	private static void createSpan(List<MFJSpan> segments, int start, int end, MFJLabel label){
		if (label == null) {
			throw new RuntimeException("The label is null");
		}
		if (start > end) {
			throw new RuntimeException("start cannot be larger than end");
		}
		if (label.form.equals("O")) {
			for (int i = start; i <= end; i++) {
				segments.add(new MFJSpan(i, i, label));
			}
		} else {
			segments.add(new MFJSpan(start, end, label));
		}
	}
}

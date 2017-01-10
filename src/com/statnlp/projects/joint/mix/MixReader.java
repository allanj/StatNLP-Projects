package com.statnlp.projects.joint.mix;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.io.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;
import com.statnlp.projects.dep.utils.DataChecker;

public class MixReader {

	public static String ROOT_WORD = "ROOT";
	public static String ROOT_TAG = "ROOT";

	/**
	 * Reading the CoNLL-X format data, need to modify a bit to make it corresponds to the index. refer to segdep
	 * @param fileName
	 * @param isLabeled
	 * @param number
	 * @param checkProjective: check the projectiveness or not
	 * @param lenOne: means if we restrict all segments are with length 1. The label is still without IOBES encoding
	 *               also it select the head for the span starting from the left portion.
	 * @return
	 * @throws IOException
	 */
	public static MixInstance[] readCoNLLXData(String fileName, boolean isLabeled, int number, boolean checkProjective) throws IOException{
		BufferedReader br = RAWF.reader(fileName);
		ArrayList<MixInstance> result = new ArrayList<MixInstance>();
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		List<Integer> originalHeads = new ArrayList<>();
		int maxSentenceLength = -1;
		int maxEntityLength = -1;
		words.add(new WordToken(ROOT_WORD, ROOT_TAG));
		originalHeads.add(-1);
		ArrayList<MixSpan> segments = new ArrayList<MixSpan>();
		segments.add(new MixSpan(0, 0, MixLabel.get("O")));
		int instanceId = 1;
		int start = -1;
		int end = 0;
		int numOfNotTree = 0;
		int nonProjective = 0;
		int numNotConnected = 0;
		MixLabel prevLabel = null;
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
				Sentence sent = new Sentence(words.toArray(wtArr));
				MixInstance instance = new MixInstance(instanceId, 1.0, sent);
				instance.input.setRecognized();
				instance.output = new MixPair(heads, segments);
				
				boolean projectiveness = DataChecker.checkProjective(originalHeads);

				boolean isTree = DataChecker.checkIsTree(originalHeads);
				if (isLabeled) {
					if(!isTree || (checkProjective && !projectiveness)) {
						words = new ArrayList<WordToken>();
						words.add(new WordToken(ROOT_WORD, ROOT_TAG));
						originalHeads = new ArrayList<>();
						originalHeads.add(-1);
						segments = new ArrayList<MixSpan>();
						segments.add(new MixSpan(0, 0, MixLabel.get("O")));
						if (!isTree) numOfNotTree++;
						if (checkProjective && !projectiveness) nonProjective++;
						continue;
					}
				}
				if (checkConnected(instance) > 0) {
					System.err.println(sent.toString());
					numNotConnected++;
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
				segments = new ArrayList<MixSpan>();
				segments.add(new MixSpan(0, 0, MixLabel.get("O")));
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
				MixLabel label = null;
				if(form.startsWith("B")){
					if(start != -1){
						end = index - 1;
						createSpan(segments, start, end, prevLabel);
						maxEntityLength = Math.max(maxEntityLength, segments.get(segments.size()-1).length());
					}
					start = index;
					label = MixLabel.get(form.substring(2));
					
				} else if(form.startsWith("I")){
					label = MixLabel.get(form.substring(2));
				} else if(form.startsWith("O")){
					if(start != -1){
						end = index - 1;
						createSpan(segments, start, end, prevLabel);
						maxEntityLength = Math.max(maxEntityLength, segments.get(segments.size()-1).length());
					}
					start = -1;
					createSpan(segments, index, index, MixLabel.get("O"));
					maxEntityLength = Math.max(maxEntityLength, segments.get(segments.size()-1).length());
					label = MixLabel.get("O");
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
			System.err.println("[Info] "+type+" #not tree: " + numOfNotTree);
			System.err.println("[Info] "+type+" #non projective: " + nonProjective);
		}
		System.err.println("[Info] #not connected "+type+" instance (after proj filter): " + numNotConnected);
		return result.toArray(new MixInstance[result.size()]);
	}
	
 	private static void createSpan(List<MixSpan> segments, int start, int end, MixLabel label){
		if (label == null) {
			throw new RuntimeException("The label is null");
		}
		if (start > end) {
			throw new RuntimeException("start cannot be larger than end");
		}
		if (label.form.equals("O")) {
			for (int i = start; i <= end; i++) {
				segments.add(new MixSpan(i, i, label));
			}
		} else {
			segments.add(new MixSpan(start, end, label));
		}
	}
 	
 	private static int checkConnected(MixInstance inst){
		int number = 0;
		List<MixSpan> output = inst.getOutput().entities;
		Sentence sent = inst.getInput();
		int[][] leftNodes = sent2LeftDepRel(sent, inst.getOutput().heads);
		for(MixSpan span: output){
			int start = span.start;
			int end = span.end;
			MixLabel label = span.label;
			if(label.equals(MixLabel.get("O")) || start==end) continue;
			boolean connected = traverseLeft(start, end, leftNodes);
			if(!connected) number++;
		}
		return number;
	}
 	
 	private static int[][] sent2LeftDepRel(Sentence sent, int[] heads){
		int[][] leftDepRel = new int[sent.length()][];
		ArrayList<ArrayList<Integer>> leftDepList = new ArrayList<ArrayList<Integer>>();
		for(int i = 0; i < leftDepRel.length; i++) leftDepList.add(new ArrayList<Integer>());
		for(int pos = 0; pos < sent.length(); pos++){
			int headIdx = heads[pos];
			if(headIdx < 0) continue;
			int smallOne = Math.min(pos, headIdx);
			int largeOne = Math.max(pos, headIdx);
			ArrayList<Integer> curr = leftDepList.get(largeOne);
			curr.add(smallOne);
		}
		for(int pos = 0; pos < sent.length(); pos++){
			ArrayList<Integer> curr = leftDepList.get(pos);
			leftDepRel[pos] = new int[curr.size()];
			for(int j=0; j<curr.size();j++)
				leftDepRel[pos][j] = curr.get(j);
		}
		return leftDepRel;
	}
	
	private static boolean traverseLeft(int start, int end, int[][] leftNodes){
		for(int l=0; l<leftNodes[end].length; l++){
			if(leftNodes[end][l] < start) continue;
			if(leftNodes[end][l] == start)
				return true;
			else if(traverseLeft(start, leftNodes[end][l], leftNodes))
				return true;
			else continue;
		}
		return false;
	}
}

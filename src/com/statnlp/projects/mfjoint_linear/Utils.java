package com.statnlp.projects.mfjoint_linear;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.statnlp.commons.io.RAWF;



public class Utils {

	/**
	 * 
	 * @param entities: starting from index-1. 0 is the root.
	 * @return
	 */
	public static List<MFLSpan> toSpan (String[] entities, int[] allHeads) {
		List<MFLSpan> segments = new ArrayList<MFLSpan>();
		int start = -1;
		int end = -1;
		MFLLabel prevLabel = null;
		MFLLabel label = null;
		for (int idx = 1; idx < entities.length; idx++) {
			String form = entities[idx];
			if(form.startsWith("B")){
				if(start != -1){
					end = idx - 1;
					createSpan(segments, start, end, prevLabel, allHeads);
				}
				start = idx;
				label = MFLLabel.get(form.substring(2));
				
			} else if(form.startsWith("I")){
				label = MFLLabel.get(form.substring(2));
			} else if(form.startsWith("O")){
				if(start != -1){
					end = idx - 1;
					createSpan(segments, start, end, prevLabel, allHeads);
				}
				start = -1;
				createSpan(segments, idx, idx, MFLLabel.get("O"), allHeads);
				label = MFLLabel.get("O");
			}
			prevLabel = label;
		}
		return segments;
	}
	
	private static void createSpan(List<MFLSpan> segments, int start, int end, MFLLabel label, int[] allHeads){
		if (label == null) {
			throw new RuntimeException("The label is null");
		}
		if (start > end) {
			throw new RuntimeException("start cannot be larger than end");
		}
		if (label.form.equals("O")) {
			for (int i = start; i <= end; i++) {
				HashSet<Integer> set = new HashSet<Integer>(1);
				set.add(allHeads[i]);
				segments.add(new MFLSpan(i, i, label, set));
			}
		} else {
			List<Integer> heads = new ArrayList<Integer>();
			for (int idx = start; idx <= end; idx++) {
				if (allHeads[idx] < start || allHeads[idx] > end) heads.add(allHeads[idx]);
			}
			HashSet<Integer> set = new HashSet<Integer>(heads);
			segments.add(new MFLSpan(start, end, label, set));
		}
	}
	
	/**
	 * Read from the two separate result file obtain from two models.
	 * @param testInsts
	 * @param depResult
	 * @param neResult
	 * @throws IOException
	 */
	public static void readSeparateResultFile(MFLInstance[] testInsts, String depResult, String neResult) throws IOException{
		BufferedReader brDep = RAWF.reader(depResult);
		BufferedReader brNE = RAWF.reader(neResult);
		List<Integer> originalHeads = new ArrayList<>();
		ArrayList<String> enList = new ArrayList<>();
		enList.add("O");
		originalHeads.add(-1);
		int index = 0;
		String line = null;
		while((line = brDep.readLine()) != null){
			String depline = line.trim();
			String neLine = brNE.readLine().trim();
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
				String[] depValues = depline.split("[\t ]");
				String[] neValues = neLine.split("[\t ]");
				int headIdx = Integer.parseInt(depValues[4]);
				String form = neValues[3];
				originalHeads.add(headIdx);
				enList.add(form);
			}
		}
		brDep.close();
		brNE.close();
	}
	
}

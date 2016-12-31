package com.statnlp.projects.dep.model.pipeline;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import com.statnlp.commons.io.RAWF;

public class Reader {

	
	public static ResultInstance[] readResults(String testFile, int testNumber) {
		String line = null;
		BufferedReader br;
		ArrayList<ResultInstance> insts = new ArrayList<>();
		try {
			br = RAWF.reader(testFile);
			ArrayList<String> enList = new ArrayList<>(); //1-indexing.
			enList.add("O");
			ArrayList<Integer> headIdxList = new ArrayList<>();
			headIdxList.add(-1);
			while((line = br.readLine()) != null) {
				if (line.equals("")) {
					String[] entities = new String[enList.size()];
					enList.toArray(entities);
					int[] headIdxArr = new int[headIdxList.size()];
					for (int i = 0; i < headIdxArr.length; i++) headIdxArr[i] = headIdxList.get(i);
					insts.add(new ResultInstance(entities, headIdxArr));
					enList = new ArrayList<>();
					headIdxList = new ArrayList<>();
					enList.add("O");
					headIdxList.add(-1);
					if (insts.size() == testNumber)
						break;
				} else {
					String[] values = line.split("[\t ]");
					enList.add(values[10]);
					headIdxList.add(Integer.parseInt(values[6]));
				}
			}
			br.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return insts.toArray(new ResultInstance[insts.size()]);
	}
}

package com.statnlp.projects.dep;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.io.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;
import com.statnlp.projects.dep.commons.DepLabel;
import com.statnlp.projects.dep.utils.DPConfig;
import com.statnlp.projects.dep.utils.DataChecker;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.UnnamedDependency;

public class DependencyReader {
	
	public static String ROOT_WORD = "ROOT";
	public static String ROOT_TAG = "ROOT";
	
	public static String O_TYPE = DPConfig.O_TYPE;
	public static String E_B_PREFIX = DPConfig.E_B_PREFIX;
	public static String E_I_PREFIX = DPConfig.E_I_PREFIX;
	public static String MISC = DPConfig.MISC;
	
	public static String[] others = DPConfig.others;
	

	public static DependInstance[] readCoNLLX(String path, boolean isLabeled, int number, Transformer trans, boolean checkProjective){
		ArrayList<DependInstance> data = new ArrayList<DependInstance>();
		int maxLength = -1;
		try {
			BufferedReader br = RAWF.reader(path);
			String line = null;
			int index = 1;
			ArrayList<WordToken> words = new ArrayList<WordToken>();
			words.add(new WordToken(ROOT_WORD,ROOT_TAG,-1,O_TYPE, "NOLABEL"));
			ArrayList<UnnamedDependency> dependencies = new ArrayList<UnnamedDependency>();
			while((line = br.readLine())!=null){
				if(line.equals("")){
					WordToken[] wordsArr = new WordToken[words.size()];
					words.toArray(wordsArr);
					Sentence sent = new Sentence(wordsArr);
					boolean projectiveness=  DataChecker.checkProjective(dependencies);
					if(checkProjective && !projectiveness) {
						dependencies = new ArrayList<UnnamedDependency>();
						words = new ArrayList<WordToken>();
						words.add(new WordToken(ROOT_WORD,ROOT_TAG,-1,O_TYPE, "NOLABEL"));
						continue;
					}
					Tree dependencyTree = trans.toDependencyTree(dependencies, sent);
					if(!checkProjective || dependencyTree.size()==sent.length()){
						sent.setRecognized();
						Tree spanTree = isLabeled? trans.toSpanTree(dependencyTree, sent): null;
						DependInstance inst = new DependInstance(index++,1.0,sent,dependencies,dependencyTree,spanTree);
						for(UnnamedDependency ud: dependencies){
							CoreLabel mo = (CoreLabel)ud.dependent();
							CoreLabel he = (CoreLabel)ud.governor();
							mo.setNER(sent.get(mo.sentIndex()).getEntity());
							he.setNER(sent.get(he.sentIndex()).getEntity());
						}
						maxLength = Math.max(inst.size(), maxLength);
						if(isLabeled) {
							sent.setRecognized();
							inst.setLabeled();
							data.add(inst);
						}
						else {
							inst.setUnlabeled();
							data.add(inst);
						}
						
					}
					words = new ArrayList<WordToken>();
					words.add(new WordToken(ROOT_WORD,ROOT_TAG,-1,O_TYPE, "NOLABEL"));
					dependencies = new ArrayList<UnnamedDependency>();
					if(number!= -1 && data.size()==number) break;
					continue;
				}
				String[] values = line.split("\\t");
				int headIndex = Integer.valueOf(values[6]);
				String entity = values.length>10? values[10]: null;
				String depLabel = values[7];
				DepLabel.get(depLabel);
				if(headIndex==0) DepLabel.rootDepLabel = depLabel;
				words.add(new WordToken(values[1], values[4], headIndex, entity, depLabel));
				CoreLabel headLabel = new CoreLabel();
				CoreLabel modifierLabel = new CoreLabel();
				headLabel.setSentIndex(headIndex);
				headLabel.setValue("index:"+headIndex);
				modifierLabel.setSentIndex(words.size()-1);
				modifierLabel.setValue("index:"+modifierLabel.sentIndex());
				modifierLabel.setTag(depLabel);
				dependencies.add(new UnnamedDependency(headLabel, modifierLabel));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<DependInstance> myData = data;
		DependInstance[] dataArr = new DependInstance[myData.size()];
		String type = isLabeled? "Training":"Testing"; 
		System.err.println("[Info] "+type+" instance, total:"+ dataArr.length+" Instance. ");
		System.err.println("[Info] "+type+" instance, max Length:"+ maxLength);
		myData.toArray(dataArr);
		return dataArr;
	}
	
	
	/***
	 * The data format should (index	word	tag	entity	headIndex)
	 * This one needed to be modified Later
	 * @param path
	 * @param isLabeled
	 * @param number
	 * @return
	 */
	public static DependInstance[] readFromPipeline(String path, int number){
		ArrayList<DependInstance> data = new ArrayList<DependInstance>();
		int maxLen = -1;
		try {
			BufferedReader br = RAWF.reader(path);
			String line = null;
			int index = 1;
			ArrayList<WordToken> words = new ArrayList<WordToken>();
			words.add(new WordToken(ROOT_WORD,ROOT_TAG,-1,O_TYPE));
			double instanceWeight = 1.0;
			int globalId = -1;
			while((line = br.readLine())!=null){
				if(line.equals("")){
					WordToken[] wordsArr = new WordToken[words.size()];
					words.toArray(wordsArr);
					Sentence sent = new Sentence(wordsArr);
					DependInstance inst = new DependInstance(globalId, index++,instanceWeight,sent, null, null, null);
					maxLen = Math.max(maxLen, inst.getInput().length());
					inst.setUnlabeled();
					data.add(inst);
					words = new ArrayList<WordToken>();
					words.add(new WordToken(ROOT_WORD,ROOT_TAG,-1,O_TYPE));
					if(number!= -1 && data.size()==number) break;
					continue;
				}
				String[] values = line.split("\t");
				String predEntity = values[3];
				int headIndex = Integer.parseInt(values[4]);
				words.add(new WordToken(values[1], values[2], headIndex, predEntity));
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<DependInstance> myData = data;
		DependInstance[] dataArr = new DependInstance[myData.size()];
		System.err.println("[Pipeline] Testing instance, total:"+ dataArr.length+" Instance. ");
		myData.toArray(dataArr);
		return dataArr;
	}

}

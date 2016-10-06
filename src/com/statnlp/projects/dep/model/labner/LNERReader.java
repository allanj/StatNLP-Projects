package com.statnlp.projects.dep.model.labner;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;
import com.statnlp.projects.dep.Transformer;
import com.statnlp.projects.dep.utils.DPConfig;
import com.statnlp.projects.dep.utils.DataChecker;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.UnnamedDependency;

public class LNERReader {
	
	public static String ROOT_WORD = "ROOT";
	public static String ROOT_TAG = "ROOT";
	
	public static String O_TYPE = DPConfig.O_TYPE;
	public static String E_B_PREFIX = DPConfig.E_B_PREFIX;
	public static String E_I_PREFIX = DPConfig.E_I_PREFIX;
	public static String MISC = DPConfig.MISC;
	
	public static String[] others = DPConfig.others;
	
	public static LNERInstance[] readInstance(String path, boolean isLabeled, int number,String[] entities, Transformer transformer){
		return readInstance(path,isLabeled,number,entities, 10000, transformer, false,null);
	}
	
	public static LNERInstance[] readInstance(String path, boolean isLabeled, int number,String[] entities, Transformer transformer, boolean splitEntity){
		return readInstance(path,isLabeled,number,entities, 10000, transformer, splitEntity,null);
	}
	
	public static LNERInstance[] readInstance(String path, boolean isLabeled, int number,String[] entities, Transformer transformer, boolean splitEntity, HashMap<String, Integer> typeMap){
		return readInstance(path,isLabeled,number,entities, 10000, transformer, splitEntity,typeMap);
	}
	

	public static LNERInstance[] readInstance(String path, boolean isLabeled, int number,String[] entities, int maxLength, Transformer transformer, boolean splitEntity, HashMap<String, Integer> typeMap){
		ArrayList<LNERInstance> data = new ArrayList<LNERInstance>();
		int maxSpanLen = -1;
		int maxLen = -1;
		HashSet<String> miscSet = new HashSet<String>();
		for(int m=0;m<others.length;m++){
			miscSet.add(others[m]);
		}
		try {
			BufferedReader br = RAWF.reader(path);
			String line = null;
			int index = 1;
			ArrayList<WordToken> words = new ArrayList<WordToken>();
			words.add(new WordToken(ROOT_WORD,ROOT_TAG,-1,O_TYPE));
			ArrayList<UnnamedDependency> dependencies = new ArrayList<UnnamedDependency>();
			String prev_Entity = "";
			int conNum = 0;
			while((line = br.readLine())!=null){
				if(line.startsWith("#")) continue;
				if(line.equals("")){
					WordToken[] wordsArr = new WordToken[words.size()];
					words.toArray(wordsArr);
					Sentence sent = new Sentence(wordsArr);
					boolean projectiveness=  DataChecker.checkProjective(dependencies);
//					System.err.println("Instance "+(index+1)+", projective:"+projectiveness); index++;
					if(!projectiveness) {
						dependencies = new ArrayList<UnnamedDependency>();
						words = new ArrayList<WordToken>();
						words.add(new WordToken(ROOT_WORD,ROOT_TAG,-1,O_TYPE));
						conNum = 0;
						continue;
					}
					Tree dependencyTree = transformer.toDependencyTree(dependencies, sent);
					if(dependencyTree.size()==sent.length() && sent.length()< maxLength){
						sent.setRecognized();
						LNERInstance inst = new LNERInstance(index++,1.0,sent,dependencies,dependencyTree,transformer.toSpanTree(dependencyTree, sent));
						
						if(entities!=null && typeMap!=null) inst.setHaveEntity(typeMap);
						inst.continousNum = conNum;
						for(UnnamedDependency ud: dependencies){
							CoreLabel mo = (CoreLabel)ud.dependent();
							CoreLabel he = (CoreLabel)ud.governor();
							mo.setNER(sent.get(mo.sentIndex()).getEntity());
							he.setNER(sent.get(he.sentIndex()).getEntity());
						}
						maxLen = Math.max(maxLen, inst.getInput().length());
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
					words.add(new WordToken(ROOT_WORD,ROOT_TAG,-1,O_TYPE));
					conNum = 0;
					dependencies = new ArrayList<UnnamedDependency>();
					if(number!= -1 && data.size()==number) break;
					continue;
				}
				String[] values = line.split("\\t");
				int headIndex = Integer.valueOf(values[8]);
				
				String entity = values[12];
				if(!prev_Entity.equals("")){
					words.add(new WordToken(values[1],values[4],headIndex,E_I_PREFIX+prev_Entity,values[10])); 
					if((entity.contains(prev_Entity) || prev_Entity.equals(MISC))  && entity.endsWith(")")) prev_Entity = "";
				}else{
					boolean added = false;
					String previousLastEn =  words.get(words.size()-1).getEntity();
					if(entity.startsWith("(") && !previousLastEn.equals(O_TYPE)){
						if(entity.contains(previousLastEn.substring(2))) conNum++;
						else if(previousLastEn.substring(2).equals(MISC)){
							if((miscSet.contains(entity.substring(1)) || miscSet.contains(entity.substring(1,entity.length()-1))))
									conNum++;
						}
					}
					for(int i=0;i<entities.length;i++){
						if(entity.contains(entities[i]) && entity.startsWith("(")){
							//merge the continuous case
							previousLastEn =  words.get(words.size()-1).getEntity();
							if(!splitEntity &&!previousLastEn.equals(O_TYPE) && entity.contains(previousLastEn.substring(2))){
								words.add(new WordToken(values[1],values[4],headIndex,E_I_PREFIX+entities[i]));
							}else
								words.add(new WordToken(values[1],values[4],headIndex,E_B_PREFIX+entities[i]));
							prev_Entity = entities[i];
							if(entity.endsWith(")")) prev_Entity="";
							added = true;
							break;
						}
					}
					if(!added) {
						//incase have something (plant)
						if(entity.startsWith("(") && (miscSet.contains(entity.substring(1)) || miscSet.contains(entity.substring(1,entity.length()-1)) )){
							previousLastEn =  words.get(words.size()-1).getEntity();
							if(!splitEntity &&!previousLastEn.equals(O_TYPE) && previousLastEn.substring(2).equals(MISC)){
								words.add(new WordToken(values[1],values[4],headIndex,E_I_PREFIX+MISC));
							}else
								words.add(new WordToken(values[1],values[4],headIndex,E_B_PREFIX+MISC));
							prev_Entity = MISC;
							if(entity.endsWith(")")) prev_Entity="";
						}else
							words.add(new WordToken(values[1],values[4],headIndex,O_TYPE)); 
					}
				}
				
				
				
				CoreLabel headLabel = new CoreLabel();
				CoreLabel modifierLabel = new CoreLabel();
				headLabel.setSentIndex(headIndex);
				headLabel.setValue("index:"+headIndex);
				modifierLabel.setSentIndex(words.size()-1);
				modifierLabel.setValue("index:"+modifierLabel.sentIndex());
				modifierLabel.setTag(values[10]); // modifier tag is the label.
				dependencies.add(new UnnamedDependency(headLabel, modifierLabel));
				maxSpanLen = Math.max(Math.abs(headIndex-modifierLabel.sentIndex()), maxSpanLen);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<LNERInstance> myData = data;
		LNERInstance[] dataArr = new LNERInstance[myData.size()];
		String type = isLabeled? "Training":"Testing"; 
		System.err.println("[Info] "+type+" instance, total:"+ dataArr.length+" Instance. ");
		myData.toArray(dataArr);
		return dataArr;
	}
	
	public static LNERInstance[] readCNN(String path, boolean isLabeled, int number, Transformer transformer){
		return readCNN(path, isLabeled, number, transformer, null);
	}
	
	//not labelled yet
	public static LNERInstance[] readCNN(String path, boolean isLabeled, int number, Transformer transformer, HashMap<String, Integer> typeMap){
		ArrayList<LNERInstance> data = new ArrayList<LNERInstance>();
		int maxLen = -1;
		try {
			BufferedReader br = RAWF.reader(path);
			String line = null;
			int index = 1;
			ArrayList<WordToken> words = new ArrayList<WordToken>();
			words.add(new WordToken(ROOT_WORD,ROOT_TAG,-1,O_TYPE));
			ArrayList<UnnamedDependency> dependencies = new ArrayList<UnnamedDependency>();
			
			while((line = br.readLine())!=null){
				if(line.startsWith("#")) continue;
				if(line.equals("")){
					WordToken[] wordsArr = new WordToken[words.size()];
					words.toArray(wordsArr);
					Sentence sent = new Sentence(wordsArr);
					boolean projectiveness=  DataChecker.checkProjective(dependencies);
					if(!projectiveness) {
						dependencies = new ArrayList<UnnamedDependency>();
						words = new ArrayList<WordToken>();
						words.add(new WordToken(ROOT_WORD,ROOT_TAG,-1,O_TYPE));
						continue;
					}
					Tree dependencyTree = transformer.toDependencyTree(dependencies, sent);
					if(dependencyTree.size()==sent.length()){
						LNERInstance inst = new LNERInstance(index++,1.0,sent,dependencies,dependencyTree,transformer.toSpanTree(dependencyTree, sent));
						maxLen = Math.max(maxLen, inst.getInput().length());
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
//					System.err.println("Reading: "+inst.getDependencies().toString());
					words = new ArrayList<WordToken>();
					words.add(new WordToken(ROOT_WORD,ROOT_TAG,-1,O_TYPE));
					if(number!= -1 && data.size()==number) break;
					dependencies = new ArrayList<UnnamedDependency>();
					continue;
				}
				String[] values = line.split(" ");
				int headIndex = Integer.valueOf(values[4]);
				String entity = values[3];
				
				
				words.add(new WordToken(values[1],values[2],headIndex,entity));
				CoreLabel headLabel = new CoreLabel();
				CoreLabel modifierLabel = new CoreLabel();
				
				headLabel.setSentIndex(headIndex); 
				headLabel.setValue("index:"+headIndex);
				modifierLabel.setSentIndex(words.size()-1);
				modifierLabel.setValue("index:"+modifierLabel.sentIndex());
				dependencies.add(new UnnamedDependency(headLabel, modifierLabel));
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<LNERInstance> myData = data;
		LNERInstance[] dataArr = new LNERInstance[myData.size()];
		String type = isLabeled? "Training":"Testing"; 
		System.err.println("[Info] "+type+" instance, total:"+ dataArr.length+" Instance. ");
		myData.toArray(dataArr);
		return dataArr;
	}



}

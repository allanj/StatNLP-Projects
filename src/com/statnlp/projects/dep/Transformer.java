package com.statnlp.projects.dep;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Sentence;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.UnnamedDependency;

public abstract class Transformer {

	
	/**
	 * Convert the list of the dependency to a tree representation
	 * Where each node is also a word in the tree
	 * @param dependencies :List of dependencies
	 * @param sentence : the corresponding sentence that includes the above dependencies
	 * @return
	 */
	public Tree toDependencyTree(ArrayList<UnnamedDependency> dependencies, Sentence sentence){
		HashMap<Integer, ArrayList<Integer>> map = dependencies2Map(dependencies);
		Tree root = new LabeledScoredTreeNode();
		CoreLabel rootLabel = new CoreLabel();
		rootLabel.setSentIndex(0);
		rootLabel.setValue(sentence.get(0).getName());
		rootLabel.setNER(sentence.get(0).getEntity());
		root.setLabel(rootLabel);
		toDependencyTree(root,map,sentence);
		return root;
	}
	
	/**
	 * Same as to dependency tree, but this one explicitly use the hash map to store the dependency relations
	 * @param current
	 * @param map : <head/parent, List<children>>
	 * @param sentence
	 */
	private void toDependencyTree(Tree current, HashMap<Integer, ArrayList<Integer>> map, Sentence sentence){
		CoreLabel cl = (CoreLabel)(current.label());
		if(map.containsKey(cl.sentIndex())){
			for(int childIndex: map.get(cl.sentIndex())){
				Tree child = new LabeledScoredTreeNode();
				CoreLabel childLabel = new CoreLabel();
				childLabel.setValue(sentence.get(childIndex).getName());
				child.setLabel(childLabel);
				childLabel.setSentIndex(childIndex);
				childLabel.setNER(sentence.get(childIndex).getEntity());
				current.addChild(child);
				toDependencyTree(child, map,sentence);
			}
		}
	}
	
	/**
	 * Convert the list of dependencies to the hash map with head as key, list of chidren as value.
	 * @param dependencies : list of the dependencies
	 * @return dependency map <head/parent, list<children>>
	 */
	public HashMap<Integer, ArrayList<Integer>> dependencies2Map(ArrayList<UnnamedDependency> dependencies){
		HashMap<Integer, ArrayList<Integer>> map = new HashMap<Integer, ArrayList<Integer>>();
		for(UnnamedDependency ud: dependencies){
			CoreLabel headLabel = (CoreLabel)(ud.governor());
			int headIndex = headLabel.sentIndex();
			CoreLabel modifierLabel = (CoreLabel)(ud.dependent());
			int modifierIndex = modifierLabel.sentIndex();
			if(map.containsKey(headIndex)){
				map.get(headIndex).add(modifierIndex);
			}else{
				ArrayList<Integer> list = new ArrayList<Integer>();
				list.add(modifierIndex);
				map.put(headIndex, list);
			}
		}
		Iterator<Integer> iter = map.keySet().iterator();
		while(iter.hasNext()){
			int key = iter.next();
			Collections.sort(map.get(key));
		}
		return map;
	}
	
	
	/**
	 * Input the dependencies array list, out the array that contains the head index for each index in sentence;
	 * @param dependencies
	 * @return map<modifier, head>. since each modifier can have and only have 1 head
	 */
	public static int[] dependencies2RelationMap(ArrayList<UnnamedDependency> dependencies){
		int[] heads = new int[dependencies.size()];
		heads[0] = -1;
		for(UnnamedDependency ud: dependencies){
			CoreLabel headLabel = (CoreLabel)(ud.governor());
			int headIndex = headLabel.sentIndex();
			CoreLabel modifierLabel = (CoreLabel)(ud.dependent());
			int modifierIndex = modifierLabel.sentIndex();
			heads[modifierIndex] = headIndex;
		}
		return heads;
	}
	
	
	

	
	public static int[] getHeads(ArrayList<UnnamedDependency> dependencies, Sentence sent){
		int[] heads = new int[sent.length()];
		for(UnnamedDependency dependency: dependencies){
			CoreLabel headLabel = (CoreLabel)dependency.governor();
			CoreLabel modifierLabel = (CoreLabel)dependency.dependent();
			int head = headLabel.sentIndex();
			int modifier = modifierLabel.sentIndex();
			heads[modifier] = head;
		}
		return heads;
	}
	
	public static String[] getDepLabel(ArrayList<UnnamedDependency> dependencies, Sentence sent){
		String[] labs = new String[sent.length()];
		for(UnnamedDependency dependency: dependencies){
			CoreLabel modifierLabel = (CoreLabel)dependency.dependent();
			int modifier = modifierLabel.sentIndex();
			labs[modifier] = modifierLabel.tag();
		}
		return labs;
	}
	
	public static void toMSTFormat(DependInstance[] insts, String output){
		try {
			PrintWriter pw  = RAWF.writer(output);
			for(DependInstance inst: insts){
				Sentence sent = inst.getInput();
				for(int i=1;i<sent.length();i++){
					if(i==sent.length()-1)
						pw.write(sent.get(i).getName()+"\n");
					else
						pw.write(sent.get(i).getName()+"\t");
				}
				for(int i=1;i<sent.length();i++){
					if(i==sent.length()-1)
						pw.write(sent.get(i).getTag()+"\n");
					else
						pw.write(sent.get(i).getTag()+"\t");
				}
				for(int i=1;i<sent.length();i++){
					if(i==sent.length()-1)
						pw.write(sent.get(i).getHeadIndex()+"\n");
					else
						pw.write(sent.get(i).getHeadIndex()+"\t");
				}
				pw.write("\n");
			}
			pw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static String[] getEntities(ArrayList<UnnamedDependency> dependencies, Sentence sent){
		String[] entities = new String[sent.length()];
		for(UnnamedDependency dependency: dependencies){
			CoreLabel modifierLabel = (CoreLabel)dependency.dependent();
			String modifierEntities = modifierLabel.ner();
			int modifier = modifierLabel.sentIndex();
			entities[modifier] = modifierEntities;
		}
		return entities;
	}

	
	public abstract Tree toSpanTree(Tree dependencyRoot, Sentence sentence);
	
	

}

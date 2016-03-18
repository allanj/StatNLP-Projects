package com.statnlp.example.treecrf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.statnlp.commons.crf.Label;
import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkModel;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.LabeledWord;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;

public class TreeMain {

	public static List<Label> allLabels;
	static Map<String, Integer> labelID;
	static Map<Integer, ArrayList<int[]>> nonTerminalRules;
	static Set<Integer> terminals;
	public static ArrayList<TCRFInstance> readData(String path, boolean isLabel) throws IOException{
		if(isLabel){
			allLabels = new ArrayList<Label>();
			labelID = new HashMap<String, Integer>();
		}
		PennTreeReader ptr = new PennTreeReader(RAWF.reader(path));
		Tree t = null;
		ArrayList<TCRFInstance> instList = new ArrayList<TCRFInstance>();
		int label_id = 0;
		int index = 1;
		while((t=ptr.readTree())!=null){
			TCRFInstance tcrf = new TCRFInstance(index++,1.0);
			t.setSpans();
			
			tcrf.words = new ArrayList<String>();
			for(Tree leaf: t.getLeaves())
				tcrf.words.add(leaf.toString());
			tcrf.tags = t;
			if(isLabel) tcrf.setLabeled();
			else tcrf.setUnlabeled();
			instList.add(tcrf);
			if(isLabel){
				Collection<edu.stanford.nlp.ling.Label> coll = t.labels();
				coll.removeAll(t.yield());
				//collect into the array list l
				Iterator<edu.stanford.nlp.ling.Label> iter = coll.iterator();
				while(iter.hasNext()){
					edu.stanford.nlp.ling.Label label = iter.next();
//					if(yieldLabels.contains(label)) continue;
					if(labelID.containsKey(label.value())){
						
					}
					else{
						Label newLabel = new Label(label_id, label.value());
						allLabels.add(newLabel);
						labelID.put(label.value(), label_id);
						label_id ++;
					}
				}
			}
		}
		ptr.close();
		setLabelID(instList);
		return instList;
	}
	
	public static void getRules(ArrayList<TCRFInstance> instList){

		terminals = new HashSet<Integer>();
		nonTerminalRules = new HashMap<Integer, ArrayList<int[]>>();
		for(TCRFInstance tcrfInst: instList){
			Tree t = tcrfInst.getOutput();
			List<LabeledWord> yields = t.labeledYield();
			for(LabeledWord lw: yields){
				terminals.add(labelID.get(lw.tag().value()));
			}
			traverse(t, true);
		}
		
		
	}
	
	public static void setLabelID(ArrayList<TCRFInstance> instList){
		for(TCRFInstance tcrfInst: instList){
			Tree t = tcrfInst.getOutput();
			traverse(t, false);
		}
	}
	
	public static void traverse(Tree t, boolean needRules){
		if(!t.isLeaf() && !needRules){
			String tag = t.label().value();
			CoreLabel cl = new CoreLabel();
			cl.setTag(tag);
			cl.setValue(tag);
			if(labelID.get(tag)==null){
				Random rand = new Random();
				cl.setIndex(rand.nextInt(allLabels.size()));
			}
			else cl.setIndex(labelID.get(tag));
			t.setLabel(cl);
			if(t.isPhrasal()){
				traverse(t.getChild(0), false);
				traverse(t.getChild(1),false);
			}else if(t.isPreTerminal()){
				traverse(t.getChild(0), false);
			}
		}
		if(t.isPhrasal() && needRules){
			int tag_id = labelID.get(t.label().value());
			if(nonTerminalRules.containsKey(tag_id)){
				ArrayList<int[]> list = nonTerminalRules.get(tag_id);
				int[] children = new int[]{labelID.get(t.getChild(0).label().value()), labelID.get(t.getChild(1).label().value())};
				boolean containsChildren = false;
				for(int[] exists: list){
					if(Arrays.toString(exists).equals(Arrays.toString(children))) containsChildren = true;
				}
				if(!containsChildren)
					list.add(children);
			}else{
				ArrayList<int[]> list = new ArrayList<int[]>();
				list.add(new int[]{labelID.get(t.getChild(0).label().value()), labelID.get(t.getChild(1).label().value())});
				nonTerminalRules.put(tag_id, list);
			}
			traverse(t.getChild(0), true);
			traverse(t.getChild(1),true);
		}
		
	}
	
	
	public static void main(String[] args) throws IOException, InterruptedException{
//		PennTreeReader ptr = new PennTreeReader(RAWF.reader("data/tcrf_train.txt"));
//		Tree t = null;
//		while((t=ptr.readTree())!=null){
//			t.setSpans();
//			Collection<edu.stanford.nlp.ling.Label> coll = t.labels();
//			System.out.println(t.getLeaves().toString());
//			System.out.println(coll.toString());
//			System.out.println(t.labeledYield().get(0).tag().value());
//			
//		}
		ArrayList<TCRFInstance> trainList = readData("data/tcrf_train.txt",true);
		TCRFInstance[] train = trainList.toArray(new TCRFInstance[trainList.size()]);
		getRules(trainList);
		/****DEBUG INFO***/
		int phrasalRulesNum = 0;
		Iterator<Integer> iter = nonTerminalRules.keySet().iterator();
		while(iter.hasNext()){
			phrasalRulesNum+=nonTerminalRules.get(iter.next()).size();
		}
		/****/
		System.err.println("[DEBUG] nonTerminals size: "+phrasalRulesNum);
		System.err.println("[DEBUG] preTerminals size: "+terminals.size());
		List<TCRFInstance> testList = readData("data/tcrf_test.txt",false);
		TCRFInstance[] test = testList.toArray(new TCRFInstance[testList.size()]);
//		for(Label label:allLabels){
//			System.err.println(label.getID()+","+label.getTag());
//		}
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig._CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig._numThreads = 8;
		TCRFFeatureManager fm = new TCRFFeatureManager(new GlobalNetworkParam());
		TCRFNetworkCompiler compiler = new TCRFNetworkCompiler(allLabels, labelID.get("ROOT"), nonTerminalRules, terminals);
		NetworkModel model = DiscriminativeNetworkModel.create(fm, compiler);
		model.train(train, 50);
		Instance[] predictions = model.decode(test);
		int corr = 0;
		int total = 0;
		//first test the label.
		for(Instance inst: predictions){
			TCRFInstance tcrfInst = (TCRFInstance)inst;
			Tree output = tcrfInst.getOutput();
			Tree prediction = tcrfInst.getPrediction();
//			System.out.print(prediction.getLeaves().toString());
//			System.out.println("inst output:"+output.labeledYield().get(0).tag().value());
//			System.out.println("inst prediction:"+prediction.getLeaves().toString());
			for(int i=0;i<output.labeledYield().size();i++){
				
				if(output.labeledYield().get(i).tag().value().equals(prediction.getLeaves().get(i).toString()))
					corr++;
				total++;
			}
		}
		System.out.println("Correct tags:"+corr);
		System.out.println("Total tags:"+total);
		System.out.println("Accuracy:"+(corr*1.0/total*100)+"%");
	}

}

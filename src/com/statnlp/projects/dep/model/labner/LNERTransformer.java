package com.statnlp.projects.dep.model.labner;

import java.util.ArrayList;
import java.util.Iterator;

import com.statnlp.commons.types.Sentence;
import com.statnlp.projects.dep.Transformer;
import com.statnlp.projects.dep.utils.DPConfig;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;

public class LNERTransformer extends Transformer {

	

	public static String COMP = LNERConfig.COMPLABEL;
	public static String O_TYPE = DPConfig.O_TYPE;
	public static String E_B_PREFIX = DPConfig.E_B_PREFIX;
	public static String E_I_PREFIX = DPConfig.E_I_PREFIX;
	
	private String[] getLeavesInfo(Sentence sent){
		String[] sentEntities = new String[sent.length()];
		sentEntities[0] = O_TYPE;
		for(int i=1;i<sentEntities.length;i++){
			String type = sent.get(i).getEntity();
			if(type.equals(O_TYPE)){
				sentEntities[i]= O_TYPE;
			}else if(type.startsWith(E_B_PREFIX)){
				sentEntities[i] = type.substring(2);
			}else if(type.startsWith(E_I_PREFIX)){
				sentEntities[i] = type.substring(2);
			}
		}
		return sentEntities;
	}
	
	public Tree toSpanTree(Tree dependencyRoot, Sentence sentence){
		Tree spanTreeRoot = new LabeledScoredTreeNode();
		CoreLabel spanRootLabel = new CoreLabel();
		spanRootLabel.setValue("0,"+(sentence.length()-1)+",1,1,"+COMP);
		spanTreeRoot.setLabel(spanRootLabel);
		String[] leaves = getLeavesInfo(sentence);
		constructSpanTree(spanTreeRoot,dependencyRoot,leaves);
		return spanTreeRoot;
	}
	
	private void constructSpanTree(Tree currentSpan, Tree currentDependency, String[] leaves){
		CoreLabel currentSpanLabel = (CoreLabel)(currentSpan.label());
		String[] info = currentSpanLabel.value().split(",");
		int pa_leftIndex = Integer.valueOf(info[0]);
		int pa_rightIndex = Integer.valueOf(info[1]);
		int pa_direction = Integer.valueOf(info[2]);
		int pa_completeness = Integer.valueOf(info[3]);
		if(pa_leftIndex==pa_rightIndex) return;
		if(pa_completeness==0){
			if(pa_direction==1){
				Tree lastChildWord = currentDependency.lastChild();
				Tree copyLastChildWord = lastChildWord.deepCopy(); 
				currentDependency.removeChild(currentDependency.numChildren()-1);
				Iterator<Tree> iter = currentDependency.iterator();
				int maxSentIndex = -1;
				while(iter.hasNext()){
					Tree now = iter.next();
					CoreLabel cl = (CoreLabel)(now.label());
					if(cl.sentIndex()>maxSentIndex) maxSentIndex = cl.sentIndex();
				}
				
				Tree leftChildSpan = new LabeledScoredTreeNode();
				Tree rightChildSpan = new LabeledScoredTreeNode();
				String curr = COMP;
				if(pa_leftIndex==maxSentIndex) curr = NERLabel.get(leaves[pa_leftIndex]).getForm();
				CoreLabel leftSpanLabel = new CoreLabel(); leftSpanLabel.setValue(pa_leftIndex+","+maxSentIndex+",1,1,"+curr);
				leftChildSpan.setLabel(leftSpanLabel);
				
				curr = COMP;
				if((maxSentIndex+1)==pa_rightIndex) curr = NERLabel.get(leaves[pa_rightIndex]).getForm();
				CoreLabel rightSpanLabel = new CoreLabel(); rightSpanLabel.setValue((maxSentIndex+1)+","+pa_rightIndex+",0,1,"+curr);
				rightChildSpan.setLabel(rightSpanLabel);
				currentSpan.addChild(leftChildSpan);
				currentSpan.addChild(rightChildSpan);
				constructSpanTree(leftChildSpan, currentDependency,leaves);
				constructSpanTree(rightChildSpan, copyLastChildWord,leaves);
			}else{
				Tree firstChildWord = currentDependency.firstChild();
				Tree copyFirstChildWord = firstChildWord.deepCopy();
				currentDependency.removeChild(0);
				
				Iterator<Tree> iter = currentDependency.iterator();
				int minIndex = leaves.length+1;
				while(iter.hasNext()){
					Tree now = iter.next();
					CoreLabel cl = (CoreLabel)(now.label());
					if(cl.sentIndex()<minIndex) minIndex = cl.sentIndex();
				}
				Tree leftChildSpan = new LabeledScoredTreeNode();
				Tree rightChildSpan = new LabeledScoredTreeNode();
				String curr = COMP;
				if(pa_leftIndex==(minIndex-1)) curr = NERLabel.get(leaves[pa_leftIndex]).getForm();
				CoreLabel leftSpanLabel = new CoreLabel(); leftSpanLabel.setValue(pa_leftIndex+","+(minIndex-1)+",1,1,"+curr);
				leftChildSpan.setLabel(leftSpanLabel);
				
				curr = COMP;
				if(minIndex==pa_rightIndex) curr = NERLabel.get(leaves[pa_rightIndex]).getForm();
				CoreLabel rightSpanLabel = new CoreLabel(); rightSpanLabel.setValue(minIndex+","+pa_rightIndex+",0,1,"+curr);
				rightChildSpan.setLabel(rightSpanLabel);
				currentSpan.addChild(leftChildSpan);
				currentSpan.addChild(rightChildSpan);
				constructSpanTree(leftChildSpan, copyFirstChildWord,leaves);
				constructSpanTree(rightChildSpan, currentDependency,leaves);
			}
		}else{
			if(pa_direction==1){
				Tree lastChildWord = currentDependency.lastChild();
				
				CoreLabel lastChildWordLabel = (CoreLabel)(lastChildWord.label());
				int lastChildWordIndex = lastChildWordLabel.sentIndex();
				Tree copyLastChildWord = lastChildWord.deepCopy();
				Tree[] children = lastChildWord.children();
				ArrayList<Integer> idsRemove4Last = new ArrayList<Integer>();
				ArrayList<Integer> idsRemove4Copy = new ArrayList<Integer>();
				for(int i=0; i<children.length;i++){
					CoreLabel label = (CoreLabel)(children[i].label());
					int sentIndex = label.sentIndex();
					if(sentIndex<lastChildWordIndex){
						idsRemove4Copy.add(i);
					}else{
						idsRemove4Last.add(i);
					}
				}
				for(int j=idsRemove4Last.size()-1;j>=0;j--) lastChildWord.removeChild(idsRemove4Last.get(j));
				for(int j=idsRemove4Copy.size()-1;j>=0;j--) copyLastChildWord.removeChild(idsRemove4Copy.get(j));
				Tree leftChildSpan = new LabeledScoredTreeNode();
				Tree rightChildSpan = new LabeledScoredTreeNode();
				
				boolean isMixed = false;
				String incomType = leaves[pa_leftIndex];
				for(int i=pa_leftIndex+1;i<=lastChildWordIndex;i++){
					if(!incomType.equals(leaves[i])){isMixed = true; break;}
				}
				String curr = isMixed?O_TYPE:incomType;
				CoreLabel leftSpanLabel = new CoreLabel(); leftSpanLabel.setValue(pa_leftIndex+","+lastChildWordIndex+",1,0,"+ NERLabel.get(curr).getForm());
				leftChildSpan.setLabel(leftSpanLabel);
				
				curr = COMP;
				if(lastChildWordIndex==pa_rightIndex) curr = NERLabel.get(leaves[pa_rightIndex]).getForm();
				CoreLabel rightSpanLabel = new CoreLabel(); rightSpanLabel.setValue(lastChildWordIndex+","+pa_rightIndex+",1,1,"+curr);
				rightChildSpan.setLabel(rightSpanLabel);
				currentSpan.addChild(leftChildSpan);
				currentSpan.addChild(rightChildSpan);
				constructSpanTree(leftChildSpan, currentDependency,leaves);
				constructSpanTree(rightChildSpan, copyLastChildWord,leaves);
			}else{
				
				Tree firstChildWord = currentDependency.firstChild();
				CoreLabel firstChildWordLabel = (CoreLabel)(firstChildWord.label());
				int firstChildWordIndex = firstChildWordLabel.sentIndex();
				Tree copyFirstChildWord = firstChildWord.deepCopy();
				Tree[] children = firstChildWord.children();
				ArrayList<Integer> idsRemove4First = new ArrayList<Integer>();
				ArrayList<Integer> idsRemove4Copy = new ArrayList<Integer>();
				for(int i=0; i<children.length;i++){
					CoreLabel label = (CoreLabel)(children[i].label());
					int sentIndex = label.sentIndex();
					if(sentIndex<firstChildWordIndex){
						idsRemove4First.add(i);
					}else{
						idsRemove4Copy.add(i);
					}
				}
				for(int j=idsRemove4First.size()-1;j>=0;j--) firstChildWord.removeChild(idsRemove4First.get(j));
				for(int j=idsRemove4Copy.size()-1;j>=0;j--) copyFirstChildWord.removeChild(idsRemove4Copy.get(j));
				Tree leftChildSpan = new LabeledScoredTreeNode();
				Tree rightChildSpan = new LabeledScoredTreeNode();
				
				String curr = COMP;
				if(pa_leftIndex==firstChildWordIndex) curr = NERLabel.get(leaves[pa_leftIndex]).getForm();
				CoreLabel leftSpanLabel = new CoreLabel(); leftSpanLabel.setValue(pa_leftIndex+","+firstChildWordIndex+",0,1,"+curr);
				leftChildSpan.setLabel(leftSpanLabel);
				
				boolean isMixed = false;
				String incomType = leaves[firstChildWordIndex];
				for(int i=firstChildWordIndex+1;i<=pa_rightIndex;i++){
					if(!incomType.equals(leaves[i])){isMixed = true; break;}
				}
				curr = isMixed?O_TYPE:incomType;
				CoreLabel rightSpanLabel = new CoreLabel(); rightSpanLabel.setValue(firstChildWordIndex+","+pa_rightIndex+",0,0,"+ NERLabel.get(curr).getForm());
				rightChildSpan.setLabel(rightSpanLabel);
				currentSpan.addChild(leftChildSpan);
				currentSpan.addChild(rightChildSpan);
				constructSpanTree(leftChildSpan, copyFirstChildWord, leaves);
				constructSpanTree(rightChildSpan, currentDependency, leaves);
			}
		}
	}

	

}

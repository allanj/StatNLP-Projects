package com.statnlp.projects.dep.model.hyperedge;

import java.util.ArrayList;
import java.util.Iterator;

import com.statnlp.commons.types.Sentence;
import com.statnlp.projects.dep.Transformer;
import com.statnlp.projects.dep.commons.EntitySpan;
import com.statnlp.projects.dep.utils.DPConfig;
import com.statnlp.projects.dep.utils.DataChecker;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;


public class HPETransformer extends Transformer {

	
	public static String OE = DPConfig.OE;
	public static String ONE = DPConfig.ONE;
	
	public static String O_TYPE = DPConfig.O_TYPE;
	public static String E_B_PREFIX = DPConfig.E_B_PREFIX;
	public static String E_I_PREFIX = DPConfig.E_I_PREFIX;
	public static String EMPTY = DPConfig.EMPTY; //empty type, means normal data structure.
	

	@Override
	public Tree toSpanTree(Tree dependencyRoot, Sentence sentence){
		Tree spanTreeERoot = new LabeledScoredTreeNode();
		CoreLabel label = new CoreLabel();
		String type = EMPTY; //Because the (tree) root node at top contains the left most (leaf) root node.
		label.setValue(setSpanInfo(0, sentence.length()-1, 1, 1,type));
		spanTreeERoot.setLabel(label);
		constructSpanTree(spanTreeERoot,dependencyRoot, sentence);
		return spanTreeERoot;
	}
	
	/**
	 * Construct the span tree recursively using the dependency tree.
	 * @param currentSpan
	 * @param currentDependency
     * @param sent
	 */
	private void constructSpanTree(Tree currentSpan, Tree currentDependency, Sentence sent){
		
		CoreLabel currentSpanLabel = (CoreLabel)(currentSpan.label());
		String[] info = currentSpanLabel.value().split(",");
		int pa_leftIndex = Integer.valueOf(info[0]);
		int pa_rightIndex = Integer.valueOf(info[1]);
		int pa_direction = Integer.valueOf(info[2]);
		int pa_completeness = Integer.valueOf(info[3]);
		String pa_type = info[4];
		String currType = null;
		if(pa_leftIndex==pa_rightIndex){
			return;
		}
		if(pa_completeness==0){
			if(pa_direction==1){
				//incomplete and direction is right.
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
				
				Tree leftChildSubSpan = new LabeledScoredTreeNode();
				Tree rightChildSubSpan = new LabeledScoredTreeNode();
				
				
				String leftType = sent.get(pa_leftIndex).getEntity();
				if(leftType.startsWith("B-")){
					boolean isEntity = true;
					for(int i=pa_leftIndex+1;i<=maxSentIndex;i++){
						if(!sent.get(i).getEntity().startsWith("I-")){
							isEntity = false;
							break;
						}
						if(i==maxSentIndex && sent.get(i+1).getEntity().startsWith("I-"))
							isEntity = false;
					}
					if(isEntity) leftType = sent.get(pa_leftIndex).getEntity().substring(2);
					else leftType = "empty";
				}else{
					leftType =  "empty";
				}
				currType = leftType;
				
				CoreLabel leftSubSpanLabel = new CoreLabel();
				leftSubSpanLabel.setValue(this.setSpanInfo(pa_leftIndex, maxSentIndex, 1, 1, currType));
				leftChildSubSpan.setLabel(leftSubSpanLabel);
				
				String rightType = sent.get(maxSentIndex+1).getEntity();
				if(rightType.startsWith("B-")){
					boolean isEntity = true;
					for(int i=maxSentIndex+2; i<=pa_rightIndex; i++){

					}
				}else{
					rightType = "empty";
				}
				for(int i=maxSentIndex+1; i<=pa_rightIndex-1;i++){
					if(!leaves[i][dir].equals(rightType)) {isMixed = true; break;}



				}
				rightType =isMixed?OE:rightType;
				currType = rightType;

                        CoreLabel rightSpanSubLabel = new CoreLabel();
                rightSpanSubLabel.setValue(this.setSpanInfo(maxSentIndex+1, pa_rightIndex, 0, 1, currType));
                rightChildSubSpan.setLabel(rightSpanSubLabel);

                currentSpan.addChild(leftChildSubSpan);
                currentSpan.addChild(rightChildSubSpan);
                constructSpanTree(leftChildSubSpan, currentDependency,leaves);
                constructSpanTree(rightChildSubSpan, copyLastChildWord,leaves);
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
				Tree leftChildSubSpan = new LabeledScoredTreeNode();
				Tree rightChildSubSpan = new LabeledScoredTreeNode();
				
				boolean isMixed = false;
				String leftType = leaves[pa_leftIndex][1];
				for(int i=pa_leftIndex+1;i<=minIndex-1;i++){
					for(int dir=0;dir<=1;dir++){
						if(!leaves[i][dir].equals(leftType)){isMixed = true; break;}
					}
				}
				leftType = isMixed? OE:leftType;
				
				currType = leftType;
				
				CoreLabel leftSpanSubLabel = new CoreLabel(); 
				leftSpanSubLabel.setValue(this.setSpanInfo(pa_leftIndex, minIndex-1, 1, 1, currType));
				leftChildSubSpan.setLabel(leftSpanSubLabel);
				
				isMixed = false;
				String rightType = leaves[pa_rightIndex][0];
				for(int i=minIndex;i<=pa_rightIndex-1;i++){
					for(int dir=0;dir<=1;dir++){
						if(!leaves[i][dir].equals(rightType)) {isMixed = true; break;}
					}
				}
				rightType =isMixed?OE:rightType;
				
				currType = rightType;
				
				CoreLabel rightSpanSubLabel = new CoreLabel(); 
				rightSpanSubLabel.setValue(this.setSpanInfo(minIndex, pa_rightIndex, 0, 1, currType));
				rightChildSubSpan.setLabel(rightSpanSubLabel);
				
				currentSpan.addChild(leftChildSubSpan);
				currentSpan.addChild(rightChildSubSpan);
				constructSpanTree(leftChildSubSpan, copyFirstChildWord,leaves);
				constructSpanTree(rightChildSubSpan, currentDependency,leaves);
			}
		}else{
			if(pa_direction==1){
				//parent node is complete and right
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
				Tree leftChildSubSpan = new LabeledScoredTreeNode();
				Tree rightChildSubSpan = new LabeledScoredTreeNode();
				
				boolean isMixed = false;
				String leftType = leaves[pa_leftIndex][1];
				for(int i=pa_leftIndex+1;i<=lastChildWordIndex;i++){
					for(int dir=0;dir<=1;dir++){
						if(i==lastChildWordIndex && dir==1) continue;
						if(!leaves[i][dir].equals(leftType)){isMixed = true; break;}
					}
				}
				leftType = isMixed? OE:leftType;
				currType = leftType;
				
				CoreLabel leftSpanSubLabel = new CoreLabel(); 
				leftSpanSubLabel.setValue(this.setSpanInfo(pa_leftIndex, lastChildWordIndex, 1, 0, currType));
				leftChildSubSpan.setLabel(leftSpanSubLabel);
				
				
				isMixed = false;
				String rightType = leaves[lastChildWordIndex][1];
				for(int i=lastChildWordIndex+1;i<=pa_rightIndex;i++){
					for(int dir=0;dir<=1;dir++){
						if(!leaves[i][dir].equals(rightType)){isMixed= true; break;}
					}
				}
				rightType = isMixed? OE:rightType;
				currType = rightType;
				
				CoreLabel rightSpanSubLabel = new CoreLabel(); 
				rightSpanSubLabel.setValue(this.setSpanInfo(lastChildWordIndex, pa_rightIndex, 1, 1, currType));
				rightChildSubSpan.setLabel(rightSpanSubLabel);
				
				currentSpan.addChild(leftChildSubSpan);
				currentSpan.addChild(rightChildSubSpan);
				constructSpanTree(leftChildSubSpan, currentDependency,leaves);
				constructSpanTree(rightChildSubSpan, copyLastChildWord,leaves);
			}else{
				//complete and left
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
				Tree leftChildSubSpan = new LabeledScoredTreeNode();
				Tree rightChildSubSpan = new LabeledScoredTreeNode();
				boolean isMixed = false;
				String leftType = leaves[firstChildWordIndex][0];
				for(int i=pa_leftIndex;i<=firstChildWordIndex-1;i++){
					for(int dir=0;dir<=1;dir++){
						if(!leaves[i][dir].equals(leftType)){isMixed = true; break;}
					}
				}
				leftType = isMixed? OE:leftType;	
			
				
				currType = leftType;
				
				CoreLabel leftSpanSubLabel = new CoreLabel(); 
				leftSpanSubLabel.setValue(this.setSpanInfo(pa_leftIndex, firstChildWordIndex, 0, 1, currType));
				leftChildSubSpan.setLabel(leftSpanSubLabel);
				
				
				isMixed = false;
				String rightType = leaves[firstChildWordIndex][1];
				for(int i=firstChildWordIndex+1;i<=pa_rightIndex;i++){
					for(int dir=0;dir<=1;dir++){
						if(i==pa_rightIndex && dir==1) continue;
						if(!leaves[i][dir].equals(rightType)){isMixed=true; break;}
					}
				}
				rightType = isMixed? OE:rightType;	
				currType = rightType;
				
				CoreLabel rightSpanSubLabel = new CoreLabel(); 
				rightSpanSubLabel.setValue(this.setSpanInfo(firstChildWordIndex, pa_rightIndex, 0, 0, currType));
				rightChildSubSpan.setLabel(rightSpanSubLabel);
				
				currentSpan.addChild(leftChildSubSpan);
				currentSpan.addChild(rightChildSubSpan);
				constructSpanTree(leftChildSubSpan, copyFirstChildWord,leaves);
				constructSpanTree(rightChildSubSpan, currentDependency,leaves);
			}
		}
	}

	/**
	 * Judge whether a specific span is an entity.
	 * @param start
	 * @param end
	 * @param sent
	 * @return
	 */
	private boolean isEntity(int start, int end, Sentence sent){
		if(!sent.get(start).getEntity().startsWith("B"))
			return false;
		String subEntity = sent.get(start).getEntity().substring(1);
		//check if every subEntity is the same
		for(int i=start;i<=end;i++) if(!sent.get(i).getEntity().substring(1).equals(subEntity)) return false;
		
		//check if end+1 still have some thing same
		if(end<(sent.length()-1))
			return true;
	}
	
	private String setSpanInfo(int start, int end, int direction, int completeness, String type){
		return new String(start+","+end+","+direction+","+completeness+","+type);
	}
}

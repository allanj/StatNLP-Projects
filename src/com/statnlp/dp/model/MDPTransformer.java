package com.statnlp.dp.model;

import java.util.ArrayList;
import java.util.Iterator;

import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.Transformer;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;

public class MDPTransformer extends Transformer {

	

	@Override
	public Tree toSpanTree(Tree dependencyRoot, Sentence sentence){
		Tree spanTreeRoot = new LabeledScoredTreeNode();
		CoreLabel spanRootLabel = new CoreLabel();
		spanRootLabel.setValue("0,"+(sentence.length()-1)+",1,1,EMPTY");
		spanTreeRoot.setLabel(spanRootLabel);
		Tree spanTreeERoot = new LabeledScoredTreeNode();
		CoreLabel label = new CoreLabel();
		label.setValue("0,"+(sentence.length()-1)+",1,1,O");
		spanTreeERoot.setLabel(label);
		spanTreeRoot.addChild(spanTreeERoot);
		String[] sentEntities = new String[sentence.length()];
		for(int i=0;i<sentEntities.length;i++){
			String type = sentence.get(i).getEntity();
			sentEntities[i] = type.equals("O") || type.equals("EMPTY")? type: type.substring(2, type.length());
		}
		constructSpanTree(spanTreeERoot,dependencyRoot, sentEntities, sentence);
		System.err.println(spanTreeRoot.pennString());
		System.exit(0);
		//have some problems, check the transformed span tree.
		return spanTreeRoot;
	}
	
	/**
	 * Construct the span tree recursively using the dependency tree.
	 * @param currentSpan
	 * @param currentDependency
	 * @param sentence
	 */
	private void constructSpanTree(Tree currentSpan, Tree currentDependency,String[] sentEntities, Sentence sent){
		
		CoreLabel currentSpanLabel = (CoreLabel)(currentSpan.label());
		String[] info = currentSpanLabel.value().split(",");
		int pa_leftIndex = Integer.valueOf(info[0]);
		int pa_rightIndex = Integer.valueOf(info[1]);
		int pa_direction = Integer.valueOf(info[2]);
		int pa_completeness = Integer.valueOf(info[3]);
		String pa_type = info[4];
		String currType = null;
		int leftBound = -100;
		int rightBound = -100;
		
		if(pa_leftIndex==pa_rightIndex){
			if(!pa_type.equals("O")){
				if(sent.get(pa_leftIndex).getEntity().startsWith("I-")){
					pa_type = "O";
					currentSpanLabel.setValue(pa_leftIndex+","+pa_rightIndex+","+pa_direction+","+pa_completeness+","+pa_type);
					return;
				}
				if(pa_rightIndex<sentEntities.length-1 && sent.get(pa_rightIndex+1).getEntity().startsWith("I-")){
					pa_type = "O";
					currentSpanLabel.setValue(pa_leftIndex+","+pa_rightIndex+","+pa_direction+","+pa_completeness+","+pa_type);
					return;
				}
			}
			return;
		}
			
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
				Tree leftChildSubSpan = new LabeledScoredTreeNode();
				Tree rightChildSpan = new LabeledScoredTreeNode();
				Tree rightChildSubSpan = new LabeledScoredTreeNode();
				
				CoreLabel leftSpanLabel = new CoreLabel(); 
				boolean isMixed = false;
				String leftType = sentEntities[pa_leftIndex];
				for(int i=pa_leftIndex+1;i<=maxSentIndex;i++)
					if(!leftType.equals(sentEntities[i])) {isMixed = true; break;}
				leftType = isMixed? "O":leftType;
				leftSpanLabel.setValue(pa_leftIndex+","+maxSentIndex+",1,1,EMPTY");
				leftChildSpan.setLabel(leftSpanLabel);
				CoreLabel leftSubSpanLabel = new CoreLabel();
				
				currType = leftType;
				leftBound = pa_leftIndex;
				rightBound = maxSentIndex;
				if(!currType.equals("O") && !currType.equals("EMPTY")){
					if(pa_type.equals(currType) && (leftBound!=pa_leftIndex || rightBound!=pa_rightIndex))
						currType="O";
				}
				
				leftSubSpanLabel.setValue(pa_leftIndex+","+maxSentIndex+",1,1,"+currType);
				leftChildSubSpan.setLabel(leftSubSpanLabel);
				leftChildSpan.addChild(leftChildSubSpan);
				
				isMixed = false;
				String rightType = sentEntities[maxSentIndex+1];
				for(int i=maxSentIndex+2;i<=pa_rightIndex;i++)
					if(!rightType.equals(sentEntities[i])) {isMixed=true; break;}
				rightType = isMixed?"O":rightType;
				CoreLabel rightSpanLabel = new CoreLabel(); rightSpanLabel.setValue((maxSentIndex+1)+","+pa_rightIndex+",0,1,EMPTY");
				
				currType = rightType;
				leftBound = maxSentIndex+1;
				rightBound = pa_rightIndex;
				if(!currType.equals("O") && !currType.equals("EMPTY")){
					if(pa_type.equals(currType) && (leftBound!=pa_leftIndex || rightBound!=pa_rightIndex))
						currType="O";
//					if(!sent.get(leftBound).getEntity().startsWith("B-"))
//						currType = "O";
//					if(rightBound<sent.length()-1 && sent.get(rightBound+1).getEntity().startsWith("I-"))
//						currType = "O";
				}
				
				CoreLabel rightSpanSubLabel = new CoreLabel(); rightSpanSubLabel.setValue((maxSentIndex+1)+","+pa_rightIndex+",0,1,"+currType);
				rightChildSubSpan.setLabel(rightSpanSubLabel);
				rightChildSpan.addChild(rightChildSubSpan);
				
				rightChildSpan.setLabel(rightSpanLabel);
				currentSpan.addChild(leftChildSpan);
				currentSpan.addChild(rightChildSpan);
				constructSpanTree(leftChildSubSpan, currentDependency,sentEntities,sent);
				constructSpanTree(rightChildSubSpan, copyLastChildWord,sentEntities,sent);
			}else{
				Tree firstChildWord = currentDependency.firstChild();
				Tree copyFirstChildWord = firstChildWord.deepCopy();
				currentDependency.removeChild(0);
				Iterator<Tree> iter = currentDependency.iterator();
				int minIndex = sentEntities.length+1;
				while(iter.hasNext()){
					Tree now = iter.next();
					CoreLabel cl = (CoreLabel)(now.label());
					if(cl.sentIndex()<minIndex) minIndex = cl.sentIndex();
				}
				Tree leftChildSpan = new LabeledScoredTreeNode();
				Tree leftChildSubSpan = new LabeledScoredTreeNode();
				Tree rightChildSpan = new LabeledScoredTreeNode();
				Tree rightChildSubSpan = new LabeledScoredTreeNode();
				
				boolean isMixed = false;
				String leftType = sentEntities[pa_leftIndex];
				for(int i=pa_leftIndex+1;i<=minIndex-1;i++)
					if(!leftType.equals(sentEntities[i])) {isMixed = true; break;}
				leftType = isMixed? "O":leftType;
				CoreLabel leftSpanLabel = new CoreLabel(); leftSpanLabel.setValue(pa_leftIndex+","+(minIndex-1)+",1,1,EMPTY");
				leftChildSpan.setLabel(leftSpanLabel);
				
				currType = leftType;
				leftBound =  pa_leftIndex;
				rightBound = minIndex-1;
				if(!currType.equals("O") && !currType.equals("EMPTY")){
					if(pa_type.equals(currType) && (leftBound!=pa_leftIndex || rightBound!=pa_rightIndex))
						currType="O";
//					if(!sent.get(leftBound).getEntity().startsWith("B-"))
//						currType = "O";
//					if(rightBound<sent.length()-1 && sent.get(rightBound+1).getEntity().startsWith("I-"))
//						currType = "O";
				}
				
				CoreLabel leftSpanSubLabel = new CoreLabel(); leftSpanSubLabel.setValue(pa_leftIndex+","+(minIndex-1)+",1,1,"+currType);
				leftChildSubSpan.setLabel(leftSpanSubLabel);
				leftChildSpan.addChild(leftChildSubSpan);
				
				isMixed = false;
				String rightType = sentEntities[minIndex];
				for(int i=minIndex+1;i<=pa_rightIndex;i++)
					if(!rightType.equals(sentEntities[i])) {isMixed=true; break;}
				rightType = isMixed?"O":rightType;
				CoreLabel rightSpanLabel = new CoreLabel(); rightSpanLabel.setValue(minIndex+","+pa_rightIndex+",0,1,EMPTY");
				rightChildSpan.setLabel(rightSpanLabel);
				
				currType = rightType;
				leftBound =  minIndex;
				rightBound = pa_rightIndex;
				if(!currType.equals("O") && !currType.equals("EMPTY")){
					if(pa_type.equals(currType) && (leftBound!=pa_leftIndex || rightBound!=pa_rightIndex))
						currType="O";
//					if(!sent.get(leftBound).getEntity().startsWith("B-"))
//						currType = "O";
//					if(rightBound<sent.length()-1 && sent.get(rightBound+1).getEntity().startsWith("I-"))
//						currType = "O";
				}
				
				CoreLabel rightSpanSubLabel = new CoreLabel(); rightSpanSubLabel.setValue(minIndex+","+pa_rightIndex+",0,1,"+currType);
				rightChildSubSpan.setLabel(rightSpanSubLabel);
				rightChildSpan.addChild(rightChildSubSpan);
				
				
				currentSpan.addChild(leftChildSpan);
				currentSpan.addChild(rightChildSpan);
				constructSpanTree(leftChildSubSpan, copyFirstChildWord,sentEntities,sent);
				constructSpanTree(rightChildSubSpan, currentDependency,sentEntities,sent);
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
				Tree leftChildSubSpan = new LabeledScoredTreeNode();
				Tree rightChildSpan = new LabeledScoredTreeNode();
				Tree rightChildSubSpan = new LabeledScoredTreeNode();
				
				boolean isMixed = false;
				String leftType = sentEntities[pa_leftIndex];
				for(int i=pa_leftIndex+1;i<=lastChildWordIndex;i++)
					if(!leftType.equals(sentEntities[i])) {isMixed = true; break;}
				leftType = isMixed? "O":leftType;
				CoreLabel leftSpanLabel = new CoreLabel(); leftSpanLabel.setValue(pa_leftIndex+","+lastChildWordIndex+",1,0,EMPTY");
				leftChildSpan.setLabel(leftSpanLabel);
				
				currType = leftType;
				leftBound =  pa_leftIndex;
				rightBound = lastChildWordIndex;
				if(!currType.equals("O") && !currType.equals("EMPTY")){
					if(pa_type.equals(currType) && (leftBound!=pa_leftIndex || rightBound!=pa_rightIndex))
						currType="O";
//					if(!sent.get(leftBound).getEntity().startsWith("B-"))
//						currType = "O";
//					if(rightBound<sent.length()-1 && sent.get(rightBound+1).getEntity().startsWith("I-"))
//						currType = "O";
				}
				
				CoreLabel leftSpanSubLabel = new CoreLabel(); leftSpanSubLabel.setValue(pa_leftIndex+","+lastChildWordIndex+",1,0,"+currType);
				leftChildSubSpan.setLabel(leftSpanSubLabel);
				leftChildSpan.addChild(leftChildSubSpan);
				
				
				isMixed = false;
				String rightType = sentEntities[lastChildWordIndex];
				for(int i=lastChildWordIndex+1;i<=pa_rightIndex;i++)
					if(!rightType.equals(sentEntities[i])) {isMixed=true; break;}
				rightType = isMixed?"O":rightType;
				CoreLabel rightSpanLabel = new CoreLabel(); rightSpanLabel.setValue(lastChildWordIndex+","+pa_rightIndex+",1,1,EMPTY");
				rightChildSpan.setLabel(rightSpanLabel);
				
				currType = rightType;
				leftBound =  lastChildWordIndex;
				rightBound = pa_rightIndex;
				if(!currType.equals("O") && !currType.equals("EMPTY")){
					if(pa_type.equals(currType) && (leftBound!=pa_leftIndex || rightBound!=pa_rightIndex))
						currType="O";
//					if(!sent.get(leftBound).getEntity().startsWith("B-"))
//						currType = "O";
//					if(rightBound<sent.length()-1 && sent.get(rightBound+1).getEntity().startsWith("I-"))
//						currType = "O";
				}
				
				CoreLabel rightSpanSubLabel = new CoreLabel(); rightSpanSubLabel.setValue(lastChildWordIndex+","+pa_rightIndex+",1,1,"+currType);
				rightChildSubSpan.setLabel(rightSpanSubLabel);
				rightChildSpan.addChild(rightChildSubSpan);
				
				currentSpan.addChild(leftChildSpan);
				currentSpan.addChild(rightChildSpan);
				constructSpanTree(leftChildSubSpan, currentDependency,sentEntities,sent);
				constructSpanTree(rightChildSubSpan, copyLastChildWord,sentEntities,sent);
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
				Tree leftChildSubSpan = new LabeledScoredTreeNode();
				Tree rightChildSubSpan = new LabeledScoredTreeNode();
				boolean isMixed = false;
				String leftType = sentEntities[pa_leftIndex];
				for(int i=pa_leftIndex+1;i<=firstChildWordIndex;i++)
					if(!leftType.equals(sentEntities[i])) {isMixed = true; break;}
				leftType = isMixed? "O":leftType;
				CoreLabel leftSpanLabel = new CoreLabel(); leftSpanLabel.setValue(pa_leftIndex+","+firstChildWordIndex+",0,1,EMPTY");
				leftChildSpan.setLabel(leftSpanLabel);
				
				currType = leftType;
				leftBound =  pa_leftIndex;
				rightBound = firstChildWordIndex;
				if(!currType.equals("O") && !currType.equals("EMPTY")){
					if(pa_type.equals(currType) && (leftBound!=pa_leftIndex || rightBound!=pa_rightIndex))
						currType="O";
				}
				
				CoreLabel leftSpanSubLabel = new CoreLabel(); leftSpanSubLabel.setValue(pa_leftIndex+","+firstChildWordIndex+",0,1,"+currType);
				leftChildSubSpan.setLabel(leftSpanSubLabel);
				leftChildSpan.addChild(leftChildSubSpan);
				
				
				isMixed = false;
				String rightType = sentEntities[firstChildWordIndex];
				for(int i=firstChildWordIndex+1;i<=pa_rightIndex;i++)
					if(!rightType.equals(sentEntities[i])) {isMixed=true; break;}
				rightType = isMixed?"O":rightType;
				CoreLabel rightSpanLabel = new CoreLabel(); rightSpanLabel.setValue(firstChildWordIndex+","+pa_rightIndex+",0,0,EMPTY");
				rightChildSpan.setLabel(rightSpanLabel);
				
				currType = rightType;
				leftBound =  firstChildWordIndex;
				rightBound = pa_rightIndex;
				if(!currType.equals("O") && !currType.equals("EMPTY")){
					if(pa_type.equals(currType) && (leftBound!=pa_leftIndex || rightBound!=pa_rightIndex))
						currType="O";
//					if(!sent.get(leftBound).getEntity().startsWith("B-"))
//						currType = "O";
//					if(rightBound<sent.length()-1 && sent.get(rightBound+1).getEntity().startsWith("I-"))
//						currType = "O";
				}
				
				CoreLabel rightSpanSubLabel = new CoreLabel(); rightSpanSubLabel.setValue(firstChildWordIndex+","+pa_rightIndex+",0,0,"+currType);
				rightChildSubSpan.setLabel(rightSpanSubLabel);
				rightChildSpan.addChild(rightChildSubSpan);
				
				currentSpan.addChild(leftChildSpan);
				currentSpan.addChild(rightChildSpan);
				constructSpanTree(leftChildSubSpan, copyFirstChildWord,sentEntities,sent);
				constructSpanTree(rightChildSubSpan, currentDependency,sentEntities,sent);
			}
		}
	}


	

}

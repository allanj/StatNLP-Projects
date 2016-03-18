package com.statnlp.dp.model.rp;

import java.util.ArrayList;
import java.util.Iterator;

import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.Transformer;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;

public class RPTransformer extends Transformer {

	

	@Override
	public Tree toSpanTree(Tree dependencyRoot, Sentence sentence){
		boolean haveEntities = false;
//		boolean allSame = true;
		String[] sentEntities = new String[sentence.length()];
		for(int i=0;i<sentEntities.length;i++){
			String type = sentence.get(i).getEntity();
			sentEntities[i] = type.equals("O")? type: type.substring(2, type.length());
			if(!type.equals("O"))
				haveEntities = true;
		}
//		for(int i=2;i<sentEntities.length;i++)
//			if(!sentEntities[i].equals(sentEntities[i-1])) allSame = false;
		
		Tree spanTreeRoot = new LabeledScoredTreeNode();
		CoreLabel spanRootLabel = new CoreLabel();
		spanRootLabel.setValue("0,"+(sentence.length()-1)+",1,1,pae:null");
		spanTreeRoot.setLabel(spanRootLabel);
		Tree spanTreeERoot = new LabeledScoredTreeNode();
		CoreLabel label = new CoreLabel();
		String type = !haveEntities? "ONE":"OE";
		label.setValue("0,"+(sentence.length()-1)+",1,1,"+type);
		spanTreeERoot.setLabel(label);
		spanTreeRoot.addChild(spanTreeERoot);
		constructSpanTree(spanTreeERoot,dependencyRoot, sentEntities, sentence);
//		System.err.println(spanTreeRoot.pennString());
//		System.exit(0);
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
		if(pa_leftIndex==pa_rightIndex){
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
				if(isMixed){
					if(pa_type.equals("OE")) leftType = "OE";
					else if(!pa_type.equals("ONE")) throw new RuntimeException("parent is E, but we are mixed now");
					else leftType="ONE"; //means parent is ONE
				}else{
					if(leftType.equals("O")) leftType = "ONE";
					else if(pa_type.equals(leftType)) {}
					else if(pa_type.equals("ONE")) {
						leftType = "ONE";
						//throw new RuntimeException("parent is ONE, but we have entity now");
					}//else then if pa_type is OE, then leftType no need to change, just E.
				}
				leftSpanLabel.setValue(pa_leftIndex+","+maxSentIndex+",1,1,pae:"+pa_type);
				leftChildSpan.setLabel(leftSpanLabel);
				CoreLabel leftSubSpanLabel = new CoreLabel();
				
				currType = leftType;
				
				leftSubSpanLabel.setValue(pa_leftIndex+","+maxSentIndex+",1,1,"+currType);
				leftChildSubSpan.setLabel(leftSubSpanLabel);
				leftChildSpan.addChild(leftChildSubSpan);
				
				isMixed = false;
				String rightType = sentEntities[maxSentIndex+1];
				for(int i=maxSentIndex+2;i<=pa_rightIndex;i++)
					if(!rightType.equals(sentEntities[i])) {isMixed=true; break;}
				
				if(isMixed){
					if(pa_type.equals("OE")) rightType = "OE";
					else if(!pa_type.equals("ONE")) throw new RuntimeException("parent is E, but we are mixed now");
					else rightType="ONE";
				}else{
					if(rightType.equals("O")) rightType = "ONE";
					else if(pa_type.equals(rightType)) {}
					else if(pa_type.equals("ONE")) {
						rightType = "ONE";
						//throw new RuntimeException("parent is ONE, but we have entity now");
					}
				}
				
				CoreLabel rightSpanLabel = new CoreLabel(); 
				rightSpanLabel.setValue((maxSentIndex+1)+","+pa_rightIndex+",0,1,pae:"+pa_type);
				
				currType = rightType;
				
				

				
				
				CoreLabel rightSpanSubLabel = new CoreLabel(); 
				rightSpanSubLabel.setValue((maxSentIndex+1)+","+pa_rightIndex+",0,1,"+currType);
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
				if(isMixed){
					if(pa_type.equals("OE")) leftType = "OE";
					else if(!pa_type.equals("ONE")) throw new RuntimeException("parent is E, but we are mixed now");
					else leftType="ONE";
				}else{
					if(leftType.equals("O")) leftType = "ONE";
					else if(pa_type.equals(leftType)) {}
					else if(pa_type.equals("ONE")) {
						leftType = "ONE";
						//throw new RuntimeException("parent is ONE, but we have entity now");
					}
				}
				CoreLabel leftSpanLabel = new CoreLabel(); leftSpanLabel.setValue(pa_leftIndex+","+(minIndex-1)+",1,1,pae:"+pa_type);
				leftChildSpan.setLabel(leftSpanLabel);
				
				currType = leftType;
				
				
				CoreLabel leftSpanSubLabel = new CoreLabel(); leftSpanSubLabel.setValue(pa_leftIndex+","+(minIndex-1)+",1,1,"+currType);
				leftChildSubSpan.setLabel(leftSpanSubLabel);
				leftChildSpan.addChild(leftChildSubSpan);
				
				isMixed = false;
				String rightType = sentEntities[minIndex];
				for(int i=minIndex+1;i<=pa_rightIndex;i++)
					if(!rightType.equals(sentEntities[i])) {isMixed=true; break;}
				if(isMixed){
					if(pa_type.equals("OE")) rightType = "OE";
					else if(!pa_type.equals("ONE")) throw new RuntimeException("parent is E, but we are mixed now");
					else rightType="ONE";
				}else{
					if(rightType.equals("O")) rightType = "ONE";
					else if(pa_type.equals(rightType)) {}
					else if(pa_type.equals("ONE")) {
						rightType = "ONE";
						//throw new RuntimeException("parent is ONE, but we have entity now");
					}
				}
				CoreLabel rightSpanLabel = new CoreLabel(); rightSpanLabel.setValue(minIndex+","+pa_rightIndex+",0,1,pae:"+pa_type);
				rightChildSpan.setLabel(rightSpanLabel);
				
				currType = rightType;
				
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
				//when decomposing the complete span, the purpose of this is to eliminate the rightBound entity which is repeated
				//derive the derivation to check
				for(int i=pa_leftIndex+1;i<=lastChildWordIndex;i++){
					String entityNow = sentEntities[i];
					if((i==lastChildWordIndex) && sent.get(i).getEntity().startsWith("B-")&& i<sent.length()-1 && sent.get(i+1).getEntity().startsWith("I-"))
						entityNow = "O";
					if(!leftType.equals(entityNow)) {isMixed = true; break;}
				}
				if(isMixed){
					if(pa_type.equals("OE")) leftType = "OE";
					else if(!pa_type.equals("ONE")) throw new RuntimeException("parent is E, but we are mixed now");
					else leftType="ONE";
				}else{
					if(leftType.equals("O")) leftType = "ONE";
					else if(pa_type.equals(leftType)) {}
					else if(pa_type.equals("ONE")) {
						leftType = "ONE";
						//throw new RuntimeException("parent is ONE, but we have entity now");
					}
				}
				CoreLabel leftSpanLabel = new CoreLabel(); leftSpanLabel.setValue(pa_leftIndex+","+lastChildWordIndex+",1,0,pae:"+pa_type);
				leftChildSpan.setLabel(leftSpanLabel);
				
				currType = leftType;
				
				
				CoreLabel leftSpanSubLabel = new CoreLabel(); leftSpanSubLabel.setValue(pa_leftIndex+","+lastChildWordIndex+",1,0,"+currType);
				leftChildSubSpan.setLabel(leftSpanSubLabel);
				leftChildSpan.addChild(leftChildSubSpan);
				
				
				isMixed = false;
				String rightType = sentEntities[lastChildWordIndex];
				if(sent.get(lastChildWordIndex).getEntity().startsWith("I-")){
					if(lastChildWordIndex<pa_rightIndex && !sent.get(lastChildWordIndex+1).getEntity().startsWith("I-"))
						rightType = "O";
					if(lastChildWordIndex==pa_rightIndex)
						rightType="O";
				}
					
					
				for(int i=lastChildWordIndex+1;i<=pa_rightIndex;i++)
					if(!rightType.equals(sentEntities[i])) {isMixed=true; break;}
				if(isMixed){
					if(pa_type.equals("OE")) rightType = "OE";
					else if(!pa_type.equals("ONE")) throw new RuntimeException("parent is E, but we are mixed now");
					else rightType="ONE";
				}else{
					if(rightType.equals("O")) rightType = "ONE";
					else if(pa_type.equals(rightType)) {}
					else if(pa_type.equals("ONE")) {
						rightType = "ONE";
						//throw new RuntimeException("parent is ONE, but we have entity now");
					}
				}
				CoreLabel rightSpanLabel = new CoreLabel(); rightSpanLabel.setValue(lastChildWordIndex+","+pa_rightIndex+",1,1,pae:"+pa_type);
				rightChildSpan.setLabel(rightSpanLabel);
				
				currType = rightType;
				
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
				for(int i=pa_leftIndex+1;i<=firstChildWordIndex;i++){
					String entityNow = sentEntities[i];
					if(i==firstChildWordIndex && sent.get(i).getEntity().startsWith("B-") && sent.get(i+1).getEntity().startsWith("I-"))
						entityNow = "O";
					if(!leftType.equals(entityNow)) {isMixed = true; break;}
				}
					
				if(isMixed){
					if(pa_type.equals("OE")) leftType = "OE";
					else if(!pa_type.equals("ONE")) throw new RuntimeException("parent is E, but we are mixed now");
					else leftType="ONE";
				}else{
					if(leftType.equals("O")) leftType = "ONE";
					else if(pa_type.equals(leftType)) {}
					else if(pa_type.equals("ONE")) {
						leftType = "ONE";
						//throw new RuntimeException("parent is ONE, but we have entity now");
					}
				}
				CoreLabel leftSpanLabel = new CoreLabel(); leftSpanLabel.setValue(pa_leftIndex+","+firstChildWordIndex+",0,1,pae:"+pa_type);
				leftChildSpan.setLabel(leftSpanLabel);
				
				currType = leftType;
				
				CoreLabel leftSpanSubLabel = new CoreLabel(); leftSpanSubLabel.setValue(pa_leftIndex+","+firstChildWordIndex+",0,1,"+currType);
				leftChildSubSpan.setLabel(leftSpanSubLabel);
				leftChildSpan.addChild(leftChildSubSpan);
				
				
				isMixed = false;
				String rightType = sentEntities[firstChildWordIndex];
				//also for decomposing the complete spans, to eliminate the repeated entities
				if(sent.get(firstChildWordIndex).getEntity().startsWith("I-") && !sent.get(firstChildWordIndex+1).getEntity().startsWith("I-"))
					rightType = "O";
				for(int i=firstChildWordIndex+1;i<=pa_rightIndex;i++){
					if(!rightType.equals(sentEntities[i])) {isMixed=true; break;}
				}
					
				if(isMixed){
					if(pa_type.equals("OE")) rightType = "OE";
					else if(!pa_type.equals("ONE")) {
						System.err.println(sent.toString());
						System.err.println(firstChildWordIndex+","+pa_rightIndex+",0,0,pae:"+pa_type);
						throw new RuntimeException("parent is E, but we are mixed now");
					}
					else rightType="ONE";
				}else{
					if(rightType.equals("O")) rightType = "ONE";
					else if(pa_type.equals(rightType)) {}
					else if(pa_type.equals("ONE")) {
						rightType = "ONE";
						//throw new RuntimeException("parent is ONE, but we have entity now");
					}
				}
				CoreLabel rightSpanLabel = new CoreLabel(); rightSpanLabel.setValue(firstChildWordIndex+","+pa_rightIndex+",0,0,pae:"+pa_type);
				rightChildSpan.setLabel(rightSpanLabel);
				
				currType = rightType;
				
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

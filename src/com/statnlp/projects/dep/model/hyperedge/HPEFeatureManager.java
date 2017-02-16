package com.statnlp.projects.dep.model.hyperedge;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureBox;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.projects.dep.model.hyperedge.HPENetworkCompiler.NodeType;
import com.statnlp.projects.dep.utils.DPConfig;
import com.statnlp.projects.dep.utils.Extractor;
import com.statnlp.projects.dep.utils.DPConfig.COMP;
import com.statnlp.projects.dep.utils.DPConfig.DIR;

public class HPEFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 7274939836196010680L;

	private final int prefixSuffixLen = 3;
	private enum FeaType {unigram, bigram,contextual, inbetween,seg_prev_word,
		seg_prev_word_shape,
		seg_prev_tag,
		seg_next_word,
		seg_next_word_shape,
		seg_next_tag,
		segment,
		seg_len,
		start_word,
		start_tag,
		end_word,
		end_tag,
		word,
		tag,
		shape,
		seg_pref,
		seg_suff, entity_tag,prefix,joint};
	
	
	public static String O_TYPE = DPConfig.O_TYPE;
	
	
	public HPEFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}
	
	
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		
		
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		
		HPEInstance di = (HPEInstance)network.getInstance();
		Sentence sent  = di.getInput();
		long parent = network.getNode(parent_k);
		
		int[] parentArr = NetworkIDMapper.toHybridNodeArray(parent);
		int comp = parentArr[2];
		int nodeType = parentArr[7];
			
		/**Add the entity features still only at the incomplete span as well.**/
		if (comp == COMP.incomp.ordinal() && nodeType == NodeType.normal.ordinal())
			addDepFeatures(featureList,network,parentArr,sent);
		if (nodeType == NodeType.entity.ordinal())
			addEntityFeatures(featureList,network,parentArr,sent);
		
	
		ArrayList<Integer> finalList = new ArrayList<Integer>();
		for(int i=0;i<featureList.size();i++){
			if(featureList.get(i)!=-1)
				finalList.add(featureList.get(i) );
		}
		if(finalList.size()==0) return FeatureArray.EMPTY;
		//specifically each feature index is a feature.
		int threadId = network.getThreadId();
		FeatureArray fa = new FeatureArray(FeatureBox.getFeatureBox(new int[]{finalList.get(0)}, this.getParams_L()[threadId]));
		FeatureArray prevFa = fa;
		for (int i = 1; i < finalList.size(); i++) {
			FeatureArray curr = new FeatureArray(FeatureBox.getFeatureBox(new int[]{finalList.get(i)}, this.getParams_L()[threadId]));
			prevFa.next(curr);
			prevFa = curr;
		}
		return fa;
	}
	
	/**
	 * Currently we support simple features.
	 * @param featureList
	 * @param network
	 * @param parentArr
	 * @param sent
	 */
	private void addEntityFeatures(ArrayList<Integer> featureList, Network network, int[] parentArr, Sentence sent) {
		int start = parentArr[0] - parentArr[1];
		int end = parentArr[0];
		int labelId = parentArr[6];
		String currEn = Label.get(labelId).getForm();
		
		String lw = start>0? sent.get(start-1).getName():"STR";
		String ls = start>0? shape(lw):"STR_SHAPE";
		String lt = start>0? sent.get(start-1).getTag():"STR";
		String rw = end<sent.length()-1? sent.get(end+1).getName():"END";
		String rt = end<sent.length()-1? sent.get(end+1).getTag():"END";
		String rs = end<sent.length()-1? shape(rw):"END_SHAPE";
//		featureList.add(this._param_g.toFeature(network, FeaType.seg_prev_word.name(), 		currEn,	lw));
//		featureList.add(this._param_g.toFeature(network, FeaType.seg_prev_word_shape.name(), currEn, ls));
//		featureList.add(this._param_g.toFeature(network, FeaType.seg_prev_tag.name(), 		currEn, lt));
//		featureList.add(this._param_g.toFeature(network, FeaType.seg_next_word.name(), 		currEn, rw));
//		featureList.add(this._param_g.toFeature(network, FeaType.seg_next_word_shape.name(), currEn, rs));
//		featureList.add(this._param_g.toFeature(network, FeaType.seg_next_tag.name(), 	currEn, rt));
//		
		StringBuilder segPhrase = new StringBuilder(sent.get(start).getName());
		StringBuilder segPhraseShape = new StringBuilder(shape(sent.get(start).getName()));
		for(int pos=start+1;pos<=end; pos++){
			String w = sent.get(pos).getName();
			segPhrase.append(" "+w);
			segPhraseShape.append(" " + shape(w));
		}
		featureList.add(this._param_g.toFeature(network, FeaType.segment.name(), currEn,	segPhrase.toString()));
//		
//		int lenOfSeg = end-start+1;
//		featureList.add(this._param_g.toFeature(network, FeaType.seg_len.name(), currEn, lenOfSeg+""));
//		
//		/** Start and end features. **/
//		String startWord = sent.get(start).getName();
//		String startTag = sent.get(start).getTag();
//		featureList.add(this._param_g.toFeature(network, FeaType.start_word.name(),	currEn,	startWord));
//		featureList.add(this._param_g.toFeature(network, FeaType.start_tag.name(),	currEn,	startTag));
//		String endW = sent.get(end).getName();
//		String endT = sent.get(end).getTag();
//		featureList.add(this._param_g.toFeature(network, FeaType.end_word.name(),		currEn,	endW));
//		featureList.add(this._param_g.toFeature(network, FeaType.end_tag.name(),		currEn,	endT));
//		
//		int insideSegLen = lenOfSeg; //Math.min(twoDirInsideLen, lenOfSeg);
//		for (int i = 0; i < insideSegLen; i++) {
//			featureList.add(this._param_g.toFeature(network, FeaType.word.name()+":"+i,		currEn, sent.get(start+i).getName()));
//			featureList.add(this._param_g.toFeature(network, FeaType.tag.name()+":"+i,		currEn, sent.get(start+i).getTag()));
//			featureList.add(this._param_g.toFeature(network, FeaType.shape.name()+":"+i,	currEn, shape(sent.get(start+i).getName())));
//
//			featureList.add(this._param_g.toFeature(network, FeaType.word.name()+":-"+i,	currEn,	sent.get(start+lenOfSeg-i-1).getName()));
//			featureList.add(this._param_g.toFeature(network, FeaType.tag.name()+":-"+i,		currEn,	sent.get(start+lenOfSeg-i-1).getTag()));
//			featureList.add(this._param_g.toFeature(network, FeaType.shape.name()+":-"+i,	currEn,	shape(sent.get(start+lenOfSeg-i-1).getName())));
//		}
//		/** needs to be modified maybe ***/
//		for(int i=0; i<prefixSuffixLen; i++){
//			String prefix = segPhrase.substring(0, Math.min(segPhrase.length(), i+1));
//			String suffix = segPhrase.substring(Math.max(segPhrase.length()-i-1, 0));
//			featureList.add(this._param_g.toFeature(network, FeaType.seg_pref.name()+"-"+i,	currEn,	prefix));
//			featureList.add(this._param_g.toFeature(network, FeaType.seg_suff.name()+"-"+i,	currEn,	suffix));
//		}		
	}

	private void addDepFeatures(ArrayList<Integer> featureList, Network network, int[] parentArr, Sentence sent){
		
		int leftIndex = parentArr[0] - parentArr[1];
		int rightIndex = parentArr[0];
		int completeness = parentArr[2];
		int direction = parentArr[3];
		int leftSpanLen = parentArr[4];
		int rightSpanLen = parentArr[5];
		
//		String leftTag = sent.get(leftIndex).getTag();
//		String rightTag = sent.get(rightIndex).getTag();
//		String leftA = leftTag.substring(0, 1);
//		String rightA = rightTag.substring(0, 1);
		
//		int dist = Math.abs(rightIndex-leftIndex);
//		String att = direction==1? "RA":"LA";
//		String distBool = "0";
//		if(dist > 1)  distBool = "1";
//		if(dist > 2)  distBool = "2";
//		if(dist > 3)  distBool = "3";
//		if(dist > 4)  distBool = "4";
//		if(dist > 5)  distBool = "5";
//		if(dist > 10) distBool = "10";
//		String attDist = "&"+att+"&"+distBool;
		//maybe we can concatenate the whole span.
		int headIndex = direction == DIR.right.ordinal()? leftIndex : rightIndex - rightSpanLen + 1;
		int modifierIndex = direction == DIR.right.ordinal()? rightIndex - rightSpanLen + 1 : leftIndex;
		
		if(completeness==0){
			String headWord = sent.get(headIndex).getName();
			String headTag = sent.get(headIndex).getTag();
			String modifierWord = sent.get(modifierIndex).getName();
			String modifierTag = sent.get(modifierIndex).getTag();
			
			/**Simple concatenation**/
			if (direction == DIR.right.ordinal()) {
				for (int i = leftIndex + 1; i <= leftIndex + leftSpanLen - 1; i++) {
					headWord += " " + sent.get(i).getName();
					//headTag += " " + sent.get(i).getTag();
				}
				for (int i = rightIndex - rightSpanLen + 2; i <= rightIndex; i++) {
					modifierWord += " " + sent.get(i).getName();
					//modifierTag += " " + sent.get(i).getTag();
				}
				if (leftSpanLen > 1) headTag = "PROPN";
				if (rightSpanLen > 1) modifierTag = "PROPN";
			} else {
				for (int i = leftIndex + 1; i <= leftIndex + leftSpanLen - 1; i++) {
					modifierWord += " & " + sent.get(i).getName();
					//modifierTag += " & " + sent.get(i).getTag();
				}
				for (int i = rightIndex - rightSpanLen + 2; i <= rightIndex; i++) {
					headWord += " & " + sent.get(i).getName();
					//headTag += " & " + sent.get(i).getTag();
				}
				if (leftSpanLen > 1) modifierTag = "PROPN";
				if (rightSpanLen > 1) headTag = "PROPN";
			}
			
//			if(headWord.length()>5 || modifierWord.length()>5){
//				int hL = headWord.length();
//				int mL = modifierWord.length();
//				String preHead = hL>5? headWord.substring(0,5):headWord;
//				String preModifier = mL>5?modifierWord.substring(0,5):modifierWord;
//				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "bigram-prefix-all-dist", preHead+","+headTag+","+preModifier+","+modifierTag+attDist));
//				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "bigram-prefix-word-dist", preHead+","+preModifier+attDist));
//				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "bigram-prefix-all", preHead+","+headTag+","+preModifier+","+modifierTag));
//				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "bigram-prefix-word", preHead+","+preModifier));
//				if(mL>5){
//					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-hTmomoT-dist", headTag+","+preModifier+","+modifierTag+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-hTmo-dist", headTag+","+preModifier+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-modiall-dist", preModifier+","+modifierTag+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-modi-dist", preModifier+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-hTmomoT", headTag+","+preModifier+","+modifierTag));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-hTmo", headTag+","+preModifier));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-modiall", preModifier+","+modifierTag));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-modiall", preModifier));
//				}
//				if(hL>5){
//					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hhTmoT-dist", preHead+","+headTag+","+modifierTag+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hmoT-dist", preHead+","+modifierTag+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hall-dist", preHead+","+headTag+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-h-dist", preHead+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hhTmoT", preHead+","+headTag+","+modifierTag));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hmoT", preHead+","+modifierTag));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hall", preHead+","+headTag));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-h", preHead));
//				}
//			}
			
			/**Unigram feature without dist info**/
			featureList.add(this._param_g.toFeature(network,FeaType.unigram.name(), "headword", headWord));
//			featureList.add(this._param_g.toFeature(network,FeaType.unigram.name(), "headtag", headTag));
			featureList.add(this._param_g.toFeature(network,FeaType.unigram.name(), "modifierword", modifierWord));
//			featureList.add(this._param_g.toFeature(network,FeaType.unigram.name(), "modifiertag", modifierTag));
//			featureList.add(this._param_g.toFeature(network,FeaType.unigram.name(), "headwordtag", headWord+","+headTag));
//			featureList.add(this._param_g.toFeature(network,FeaType.unigram.name(), "modifierwordtag", modifierWord+","+modifierTag));
			
//			/**Unigram feature with dist info**/
//			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headword-dist", headWord+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headtag-dist", headTag+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifierword-dist", modifierWord+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifiertag-dist", modifierTag+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headwordtag-dist", headWord+","+headTag+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifierwordtag-dist", modifierWord+","+modifierTag+attDist));
//			
//			/****Bigram features without dist info******/
//			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "bigramword", headWord+","+modifierWord));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(),"bigramtag", headTag+","+modifierTag));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "bigramnametag",  headWord+","+headTag+","+modifierWord+","+modifierTag));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headallmoditag", headWord+","+headTag+","+modifierTag));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headallmodiword", headWord+","+headTag+","+modifierWord));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headtagmodiall", headTag+","+modifierWord+","+modifierTag));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headwordmodiall", headWord+","+modifierWord+","+modifierTag));
//			
//			/****Bigram features with dist info******/
//			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "bigramword-dist", headWord+","+modifierWord+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(),"bigramtag-dist", headTag+","+modifierTag+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "bigramnametag-dist",  headWord+","+headTag+","+modifierWord+","+modifierTag+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headallmoditag-dist", headWord+","+headTag+","+modifierTag+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headallmodiword-dist", headWord+","+headTag+","+modifierWord+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headtagmodiall-dist", headTag+","+modifierWord+","+modifierTag+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headwordmodiall-dist", headWord+","+modifierWord+","+modifierTag+attDist));
//		
//			
//			
//			String leftMinusTag = leftIndex>0? sent.get(leftIndex-1).getTag(): "STR";
//			String rightPlusTag = rightIndex<sent.length()-1? sent.get(rightIndex+1).getTag():"END";
//			String leftPlusTag = leftIndex<rightIndex-1? sent.get(leftIndex+1).getTag():"MID";
//			String rightMinusTag = rightIndex-1 > leftIndex? sent.get(rightIndex-1).getTag():"MID";
//			
//			String leftMinusA = leftIndex>0? sent.get(leftIndex-1).getATag(): "STR";
//			String rightPlusA = rightIndex<sent.length()-1? sent.get(rightIndex+1).getATag():"END";
//			String leftPlusA = leftIndex<rightIndex-1? sent.get(leftIndex+1).getATag():"MID";
//			String rightMinusA = rightIndex-1 > leftIndex?sent.get(rightIndex-1).getATag():"MID";
//			
//			
//			//l-1,l,r,r+1
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-1-dist", leftMinusTag+","+leftTag+","+rightTag+","+rightPlusTag+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-2-dist", leftMinusTag+","+leftTag+","+rightTag+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-3-dist", leftMinusTag+","+rightTag+","+rightPlusTag+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-4-dist", leftMinusTag+","+leftTag+","+rightPlusTag+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-5-dist", leftTag+","+rightTag+","+rightPlusTag+attDist));
//			
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-1", leftMinusTag+","+leftTag+","+rightTag+","+rightPlusTag));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-2", leftMinusTag+","+leftTag+","+rightTag));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-3", leftMinusTag+","+rightTag+","+rightPlusTag));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-4", leftMinusTag+","+leftTag+","+rightPlusTag));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-5", leftTag+","+rightTag+","+rightPlusTag));
//			
//			
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-1a-dist", leftMinusA+","+leftA+","+rightA+","+rightPlusA+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-2a-dist", leftMinusA+","+leftA+","+rightA+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-3a-dist", leftMinusA+","+rightA+","+rightA+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-4a-dist", leftMinusA+","+leftA+","+rightPlusA+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-5a-dist", leftA+","+rightA+","+rightPlusA+attDist));
//			
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-1a", leftMinusA+","+leftA+","+rightA+","+rightPlusA));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-2a", leftMinusA+","+leftA+","+rightA));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-3a", leftMinusA+","+rightA+","+rightA));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-4a", leftMinusA+","+leftA+","+rightPlusA));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-5a", leftA+","+rightA+","+rightPlusA));
//			
//			//l,l+1,r-1,r
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-1-dist", leftTag+","+leftPlusTag+","+rightMinusTag+","+rightTag+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-2-dist", leftTag+","+rightMinusTag+","+rightTag+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-3-dist", leftTag+","+leftPlusTag+","+rightTag+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-4-dist", leftPlusTag+","+rightMinusTag+","+rightTag+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-5-dist", leftTag+","+leftPlusTag+","+rightMinusTag+attDist));
//			
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-1", leftTag+","+leftPlusTag+","+rightMinusTag+","+rightTag));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-2", leftTag+","+rightMinusTag+","+rightTag));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-3", leftTag+","+leftPlusTag+","+rightTag));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-4", leftPlusTag+","+rightMinusTag+","+rightTag));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-5", leftTag+","+leftPlusTag+","+rightMinusTag));
//			
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-1a-dist", leftA+","+leftPlusA+","+rightMinusA+","+rightA+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-2a-dist", leftA+","+rightMinusA+","+rightA+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-3a-dist", leftA+","+leftPlusA+","+rightA+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-4a-dist", leftPlusA+","+rightMinusA+","+rightA+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-5a-dist", leftA+","+leftPlusA+","+rightMinusA+attDist));
//			
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-1a", leftA+","+leftPlusA+","+rightMinusA+","+rightA));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-2a", leftA+","+rightMinusA+","+rightA));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-3a", leftA+","+leftPlusA+","+rightA));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-4a", leftPlusA+","+rightMinusA+","+rightA));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-5a", leftA+","+leftPlusA+","+rightMinusA));
//			
//			//l-1,l,r-1,r
//			//l,l+1,r,r+1
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-3-1-dist", leftMinusTag+","+leftTag+","+rightMinusTag+","+rightTag+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-3-1", leftMinusTag+","+leftTag+","+rightMinusTag+","+rightTag));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-3-1a-dist", leftMinusA+","+leftA+","+rightMinusA+","+rightA+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-3-1a", leftMinusA+","+leftA+","+rightMinusA+","+rightA));
//			
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-4-1-dist", leftTag+","+leftPlusTag+","+rightTag+","+rightPlusTag+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-4-1", leftTag+","+leftPlusTag+","+rightTag+","+rightPlusTag));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-4-1a-dist", leftA+","+leftPlusA+","+rightA+","+rightPlusA+attDist));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-4-1a", leftA+","+leftPlusA+","+rightA+","+rightPlusA));
//			
//			
//			for(int i=leftIndex+1;i<rightIndex;i++){
//				featureList.add(this._param_g.toFeature(network,FEATYPE.inbetween.name(), "inbetween-1", leftTag+","+sent.get(i).getTag()+","+rightTag+attDist));
//				featureList.add(this._param_g.toFeature(network,FEATYPE.inbetween.name(), "inbetween-2", leftTag+","+sent.get(i).getTag()+","+rightTag));
//				featureList.add(this._param_g.toFeature(network,FEATYPE.inbetween.name(), "inbetween-3", leftA+","+sent.get(i).getATag()+","+rightA+attDist));
//				featureList.add(this._param_g.toFeature(network,FEATYPE.inbetween.name(), "inbetween-4", leftA+","+sent.get(i).getATag()+","+rightA));
//			}
		}
	}

	private String shape(String word){
		return Extractor.wordShapeOf(word);
	}
}

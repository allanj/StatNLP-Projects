package com.statnlp.projects.dep.model.segdep;

import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.projects.dep.utils.DPConfig.COMP;
import com.statnlp.projects.dep.utils.DPConfig.DIR;

public class SDFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 7274939836196010680L;

	private enum FeaType {unigram, bigram, contextual, inbetween, prefix};
	
	public SDFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}
	
	
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		FeatureArray fa = null;
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		SDInstance inst = (SDInstance)network.getInstance();
		Sentence sent  = inst.getInput();
		List<Span> segments = inst.segments;
		long parent = network.getNode(parent_k);
		int[] parentArr = NetworkIDMapper.toHybridNodeArray(parent);
		
		int leftSpanIndex = parentArr[0] - parentArr[1];
		int rightSpanIndex = parentArr[0];
		int completeness = parentArr[2];
		
		if (completeness == COMP.incomp.ordinal()) {
			if (segments.get(leftSpanIndex).length() == 1 && segments.get(rightSpanIndex).length() == 1){
				addDepFeatures(featureList, network, parentArr, sent, segments);
			} else {
				addSegDepFeatures(featureList, network, parentArr, sent, segments);
			}
		}
	
		ArrayList<Integer> finalList = new ArrayList<Integer>();
		for(int i=0;i<featureList.size();i++){
			if(featureList.get(i)!=-1)
				finalList.add(featureList.get(i) );
		}
		int[] features = new int[finalList.size()];
		for(int i=0;i<finalList.size();i++) features[i] = finalList.get(i);
		if(features.length==0) return FeatureArray.EMPTY;
		fa = new FeatureArray(features);
		return fa;
	}
	
	/**
	 * Only available for those segments whose length is 1. since they are for token level.
	 * @param featureList
	 * @param network
	 * @param parentArr
	 * @param sent
	 */
	private void addDepFeatures(ArrayList<Integer> featureList, Network network, int[] parentArr, Sentence sent, List<Span> segments){
		
		int leftSpanIndex = parentArr[0] - parentArr[1];
		int rightSpanIndex = parentArr[0];
		int completeness = parentArr[2];
		int direction = parentArr[3];
		
		int leftIndex = segments.get(leftSpanIndex).start;
		int rightIndex = segments.get(rightSpanIndex).start;
		String leftTag = sent.get(leftIndex).getTag();
		String rightTag = sent.get(rightIndex).getTag();
		String leftA = leftTag.substring(0, 1);
		String rightA = rightTag.substring(0, 1);
		
		//dist in a token sense...
		int dist = Math.abs(rightIndex - leftIndex);
		String att = direction == 1 ? "RA" : "LA";
		String distBool = "0";
		if(dist > 1)  distBool = "1";
		if(dist > 2)  distBool = "2";
		if(dist > 3)  distBool = "3";
		if(dist > 4)  distBool = "4";
		if(dist > 5)  distBool = "5";
		if(dist > 10) distBool = "10";
		String attDist = "&" + att + "&" + distBool;
		//maybe we can concatenate the whole span.
		
		if (completeness == 0) {
			int headIndex = direction == DIR.right.ordinal()? leftIndex: rightIndex;
			int modifierIndex = direction == DIR.right.ordinal()? rightIndex : leftIndex;
			String headWord = sent.get(headIndex).getName();
			String headTag = sent.get(headIndex).getTag();
			String modifierWord = sent.get(modifierIndex).getName();
			String modifierTag = sent.get(modifierIndex).getTag();
			
			if (headWord.length() > 5 || modifierWord.length() > 5) {
				int hL = headWord.length();
				int mL = modifierWord.length();
				String preHead = hL > 5 ? headWord.substring(0, 5) : headWord;
				String preModifier = mL > 5 ? modifierWord.substring(0, 5) : modifierWord;
				featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "bigram-prefix-all-dist", preHead+","+headTag+","+preModifier+","+modifierTag+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "bigram-prefix-word-dist", preHead+","+preModifier+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "bigram-prefix-all", preHead+","+headTag+","+preModifier+","+modifierTag));
				featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "bigram-prefix-word", preHead+","+preModifier));
				if (mL > 5) {
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-hTmomoT-dist", headTag+","+preModifier+","+modifierTag+attDist));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-hTmo-dist", headTag+","+preModifier+attDist));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-modiall-dist", preModifier+","+modifierTag+attDist));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-modi-dist", preModifier+attDist));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-hTmomoT", headTag+","+preModifier+","+modifierTag));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-hTmo", headTag+","+preModifier));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-modiall", preModifier+","+modifierTag));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-modiall", preModifier));
				}
				if (hL > 5) {
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hhTmoT-dist", preHead+","+headTag+","+modifierTag+attDist));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hmoT-dist", preHead+","+modifierTag+attDist));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hall-dist", preHead+","+headTag+attDist));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-h-dist", preHead+attDist));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hhTmoT", preHead+","+headTag+","+modifierTag));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hmoT", preHead+","+modifierTag));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hall", preHead+","+headTag));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-h", preHead));
				}
			}
			
			/**Unigram feature without dist info**/
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headword", headWord));
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headtag", headTag));
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifierword", modifierWord));
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifiertag", modifierTag));
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headwordtag", headWord+","+headTag));
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifierwordtag", modifierWord+","+modifierTag));
			
			/**Unigram feature with dist info**/
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headword-dist", headWord+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headtag-dist", headTag+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifierword-dist", modifierWord+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifiertag-dist", modifierTag+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headwordtag-dist", headWord+","+headTag+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifierwordtag-dist", modifierWord+","+modifierTag+attDist));
			
			/****Bigram features without dist info******/
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "bigramword", headWord+","+modifierWord));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "bigramtag", headTag+","+modifierTag));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "bigramnametag",  headWord+","+headTag+","+modifierWord+","+modifierTag));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headallmoditag", headWord+","+headTag+","+modifierTag));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headallmodiword", headWord+","+headTag+","+modifierWord));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headtagmodiall", headTag+","+modifierWord+","+modifierTag));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headwordmodiall", headWord+","+modifierWord+","+modifierTag));
			
			/****Bigram features with dist info******/
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "bigramword-dist", headWord+","+modifierWord+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(),"bigramtag-dist", headTag+","+modifierTag+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "bigramnametag-dist",  headWord+","+headTag+","+modifierWord+","+modifierTag+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headallmoditag-dist", headWord+","+headTag+","+modifierTag+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headallmodiword-dist", headWord+","+headTag+","+modifierWord+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headtagmodiall-dist", headTag+","+modifierWord+","+modifierTag+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headwordmodiall-dist", headWord+","+modifierWord+","+modifierTag+attDist));
		
			
			
			String leftMinusTag = leftIndex > 0 ? sent.get(leftIndex - 1).getTag() : "STR";
			String rightPlusTag = rightIndex < sent.length() - 1 ? sent.get(rightIndex + 1).getTag() : "END";
			String leftPlusTag = leftIndex < rightIndex - 1 ? sent.get(leftIndex + 1).getTag() : "MID";
			String rightMinusTag = rightIndex - 1 > leftIndex ? sent.get(rightIndex - 1).getTag() : "MID";

			String leftMinusA = leftIndex > 0 ? sent.get(leftIndex - 1).getATag() : "STR";
			String rightPlusA = rightIndex < sent.length() - 1 ? sent.get(rightIndex + 1).getATag() : "END";
			String leftPlusA = leftIndex < rightIndex - 1 ? sent.get(leftIndex + 1).getATag() : "MID";
			String rightMinusA = rightIndex - 1 > leftIndex ? sent.get(rightIndex - 1).getATag() : "MID";
			
			
			//l-1,l,r,r+1
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-1-dist", leftMinusTag+","+leftTag+","+rightTag+","+rightPlusTag+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-2-dist", leftMinusTag+","+leftTag+","+rightTag+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-3-dist", leftMinusTag+","+rightTag+","+rightPlusTag+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-4-dist", leftMinusTag+","+leftTag+","+rightPlusTag+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-5-dist", leftTag+","+rightTag+","+rightPlusTag+attDist));
			
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-1", leftMinusTag+","+leftTag+","+rightTag+","+rightPlusTag));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-2", leftMinusTag+","+leftTag+","+rightTag));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-3", leftMinusTag+","+rightTag+","+rightPlusTag));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-4", leftMinusTag+","+leftTag+","+rightPlusTag));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-5", leftTag+","+rightTag+","+rightPlusTag));
			
			
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-1a-dist", leftMinusA+","+leftA+","+rightA+","+rightPlusA+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-2a-dist", leftMinusA+","+leftA+","+rightA+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-3a-dist", leftMinusA+","+rightA+","+rightA+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-4a-dist", leftMinusA+","+leftA+","+rightPlusA+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-5a-dist", leftA+","+rightA+","+rightPlusA+attDist));
			
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-1a", leftMinusA+","+leftA+","+rightA+","+rightPlusA));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-2a", leftMinusA+","+leftA+","+rightA));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-3a", leftMinusA+","+rightA+","+rightA));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-4a", leftMinusA+","+leftA+","+rightPlusA));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-5a", leftA+","+rightA+","+rightPlusA));
			
			//l,l+1,r-1,r
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-1-dist", leftTag+","+leftPlusTag+","+rightMinusTag+","+rightTag+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-2-dist", leftTag+","+rightMinusTag+","+rightTag+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-3-dist", leftTag+","+leftPlusTag+","+rightTag+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-4-dist", leftPlusTag+","+rightMinusTag+","+rightTag+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-5-dist", leftTag+","+leftPlusTag+","+rightMinusTag+attDist));
			
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-1", leftTag+","+leftPlusTag+","+rightMinusTag+","+rightTag));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-2", leftTag+","+rightMinusTag+","+rightTag));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-3", leftTag+","+leftPlusTag+","+rightTag));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-4", leftPlusTag+","+rightMinusTag+","+rightTag));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-5", leftTag+","+leftPlusTag+","+rightMinusTag));
			
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-1a-dist", leftA+","+leftPlusA+","+rightMinusA+","+rightA+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-2a-dist", leftA+","+rightMinusA+","+rightA+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-3a-dist", leftA+","+leftPlusA+","+rightA+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-4a-dist", leftPlusA+","+rightMinusA+","+rightA+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-5a-dist", leftA+","+leftPlusA+","+rightMinusA+attDist));
			
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-1a", leftA+","+leftPlusA+","+rightMinusA+","+rightA));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-2a", leftA+","+rightMinusA+","+rightA));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-3a", leftA+","+leftPlusA+","+rightA));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-4a", leftPlusA+","+rightMinusA+","+rightA));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-5a", leftA+","+leftPlusA+","+rightMinusA));
			
			//l-1,l,r-1,r
			//l,l+1,r,r+1
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-3-1-dist", leftMinusTag+","+leftTag+","+rightMinusTag+","+rightTag+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-3-1", leftMinusTag+","+leftTag+","+rightMinusTag+","+rightTag));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-3-1a-dist", leftMinusA+","+leftA+","+rightMinusA+","+rightA+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-3-1a", leftMinusA+","+leftA+","+rightMinusA+","+rightA));
			
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-4-1-dist", leftTag+","+leftPlusTag+","+rightTag+","+rightPlusTag+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-4-1", leftTag+","+leftPlusTag+","+rightTag+","+rightPlusTag));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-4-1a-dist", leftA+","+leftPlusA+","+rightA+","+rightPlusA+attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-4-1a", leftA+","+leftPlusA+","+rightA+","+rightPlusA));
			
			
			for (int i = leftIndex + 1; i < rightIndex; i++) {
				String middleTag = sent.get(i).getTag();
				String middleATag = middleTag.substring(0, 1);
				featureList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-1", leftTag + "," + middleTag + "," + rightTag + attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-2", leftTag + "," + middleTag + "," + rightTag));
				featureList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-3", leftA + "," + middleATag + "," + rightA + attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-4", leftA + "," + middleATag + "," + rightA));
			}
		}
	}

	private void addSegDepFeatures(ArrayList<Integer> featureList, Network network, int[] parentArr, Sentence sent, List<Span> segments){
		
		int leftSpanIndex = parentArr[0] - parentArr[1];
		int rightSpanIndex = parentArr[0];
		int completeness = parentArr[2];
		int direction = parentArr[3];
		
		Span leftSpan = segments.get(leftSpanIndex);
		Span rightSpan =  segments.get(rightSpanIndex);
		
		String leftTags = sent.get(leftSpan.start).getTag();
		String rightTags = sent.get(rightSpan.start).getTag();
		String leftAs = leftTags.substring(0, 1);
		String rightAs = rightTags.substring(0, 1);
		
		/***Simple concatenation for the tags**/
		for (int i = leftSpan.start + 1; i <= leftSpan.end; i++) {
			leftTags += " & " + sent.get(i).getTag();
			leftAs += " & " + sent.get(i).getATag();
		}
		for (int i = rightSpan.start + 1; i <= rightSpan.end; i++) {
			rightTags += " & " + sent.get(i).getTag();
			rightAs += " & " + sent.get(i).getATag();
		}
		/***End of concatenation**/
		
		//dist in a token sense...
		int dist = Math.abs(rightSpan.start - leftSpan.end);
		String att = direction == 1 ? "RA" : "LA";
		String distBool = "0";
		if(dist > 1)  distBool = "1";
		if(dist > 2)  distBool = "2";
		if(dist > 3)  distBool = "3";
		if(dist > 4)  distBool = "4";
		if(dist > 5)  distBool = "5";
		if(dist > 10) distBool = "10";
		String attDist = "&" + att + "&" + distBool;
		//maybe we can concatenate the whole span.
		
		if (completeness == 0) {
			
			Span headSpan = direction == DIR.right.ordinal()? segments.get(leftSpanIndex) : segments.get(rightSpanIndex);
			Span modifierSpan = direction == DIR.right.ordinal()? segments.get(rightSpanIndex) : segments.get(leftSpanIndex);
			String headWords = sent.get(headSpan.start).getName();
			String headTags = sent.get(headSpan.start).getTag();
			String modifierWords = sent.get(modifierSpan.start).getName();
			String modifierTags = sent.get(modifierSpan.start).getTag();
			
			/***Simple concatenation for the tags**/
			for (int index = headSpan.start + 1; index <= headSpan.end; index++) {
				headWords += " & " + sent.get(index).getName();
				headTags += " & " + sent.get(index).getTag();
			}
			for (int index = modifierSpan.start + 1; index <= modifierSpan.end; index++) {
				modifierWords += " & " + sent.get(index).getName();
				modifierTags += " & " + sent.get(index).getTag();
			}
			/***End of concatenation**/
			
			boolean allLongHead  = sent.get(headSpan.start).getName().length() > 5;
			boolean allLongModifier = sent.get(modifierSpan.start).getName().length() > 5;;
			for (int index = headSpan.start + 1; index <= headSpan.end; index++) {
				allLongHead = allLongHead && (sent.get(index).getName().length() > 5 );
			}
			for (int index = modifierSpan.start + 1; index <= modifierSpan.end; index++) {
				allLongModifier = allLongModifier && (sent.get(index).getName().length() > 5 );
			}

			
			if (allLongHead || allLongModifier) {
				String preHead;
				if (allLongHead) {
					preHead = sent.get(headSpan.start).getName().substring(0, 5);
					for (int index = headSpan.start + 1; index <= headSpan.end; index++) {
						preHead += " & " + sent.get(index).getName().substring(0, 5);
					}
				}else preHead = headWords;
				String preModifier;
				if (allLongModifier) {
					preModifier = sent.get(modifierSpan.start).getName().substring(0, 5);
					for (int index = modifierSpan.start + 1; index <= modifierSpan.end; index++) {
						preModifier += " & " + sent.get(index).getName().substring(0, 5);
					}
				}else preModifier = modifierWords;
				featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "bigram-prefix-all-dist", preHead + "," + headTags + "," + preModifier + "," + modifierTags + attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "bigram-prefix-word-dist", preHead + "," + preModifier + attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "bigram-prefix-all", preHead + "," + headTags + "," + preModifier + "," + modifierTags));
				featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "bigram-prefix-word", preHead + "," + preModifier));
				if (allLongModifier) {
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-hTmomoT-dist", headTags + "," + preModifier + "," + modifierTags + attDist));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-hTmo-dist", headTags + "," + preModifier + attDist));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-modiall-dist", preModifier + "," + modifierTags + attDist));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-modi-dist", preModifier + attDist));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-hTmomoT", headTags + "," + preModifier + "," + modifierTags));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-hTmo", headTags + "," + preModifier));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-modiall", preModifier + "," + modifierTags));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-modiall", preModifier));
				}
				if (allLongHead) {
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hhTmoT-dist", preHead + "," + headTags + "," + modifierTags + attDist));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hmoT-dist", preHead + "," + modifierTags + attDist));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hall-dist", preHead + "," + headTags + attDist));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-h-dist", preHead + attDist));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hhTmoT", preHead + "," + headTags + "," + modifierTags));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hmoT", preHead + "," + modifierTags));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hall", preHead + "," + headTags));
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-h", preHead));
				}
			}
			
			/**Unigram feature without dist info**/
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headword", headWords));
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headtag", headTags));
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifierword", modifierWords));
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifiertag", modifierTags));
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headwordtag", headWords + "," + headTags));
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifierwordtag", modifierWords + "," + modifierTags));
			
			/**Unigram feature with dist info**/
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headword-dist", headWords + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headtag-dist", headTags + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifierword-dist", modifierWords + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifiertag-dist", modifierTags + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headwordtag-dist", headWords + "," + headTags +attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifierwordtag-dist", modifierWords + "," + modifierTags + attDist));
			
			/****Bigram features without dist info******/
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "bigramword", headWords + "," + modifierWords));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "bigramtag", headTags + "," + modifierTags));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "bigramnametag",  headWords + "," + headTags + "," + modifierWords + "," + modifierTags));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headallmoditag", headWords + "," + headTags + "," + modifierTags));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headallmodiword", headWords + ","+ headTags + "," + modifierWords));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headtagmodiall", headTags + "," + modifierWords + "," + modifierTags));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headwordmodiall", headWords +","+modifierWords + "," + modifierTags));
			
			/****Bigram features with dist info******/
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "bigramword-dist", headWords + "," + modifierWords + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(),"bigramtag-dist", headTags + "," + modifierTags + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "bigramnametag-dist",  headWords + "," + headTags + "," + modifierWords + "," + modifierTags + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headallmoditag-dist", headWords + "," + headTags + "," + modifierTags + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headallmodiword-dist", headWords + "," + headTags + "," + modifierWords + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headtagmodiall-dist", headTags + "," + modifierWords + "," + modifierTags + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headwordmodiall-dist", headWords + "," + modifierWords + "," + modifierTags + attDist));
		
			
			//still token level, not leftMinusSpan.
			//intuitively should span level I guess?
			String leftMinusTag = leftSpan.start > 0 ? sent.get(leftSpan.start - 1).getTag() : "STR";
			String rightPlusTag = rightSpan.end < sent.length() - 1 ? sent.get(rightSpan.end + 1).getTag() : "END";
			String leftPlusTag = leftSpan.end < rightSpan.start - 1 ? sent.get(leftSpan.end + 1).getTag() : "MID";
			String rightMinusTag = rightSpan.start - 1 > leftSpan.end ? sent.get(rightSpan.start - 1).getTag() : "MID";

			String leftMinusA = leftSpan.start > 0 ? sent.get(leftSpan.start - 1).getATag() : "STR";
			String rightPlusA = rightSpan.end < sent.length() - 1 ? sent.get(rightSpan.end + 1).getATag() : "END";
			String leftPlusA = leftSpan.end < rightSpan.start - 1 ? sent.get(leftSpan.end + 1).getATag() : "MID";
			String rightMinusA = rightSpan.start - 1 > leftSpan.end ? sent.get(rightSpan.start - 1).getATag() : "MID";
			
			
			//l-1,l,r,r+1
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-1-dist", leftMinusTag + "," + leftTags + "," + rightTags + "," + rightPlusTag + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-2-dist", leftMinusTag + "," + leftTags + "," + rightTags + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-3-dist", leftMinusTag + "," + rightTags + "," + rightPlusTag + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-4-dist", leftMinusTag + "," + leftTags + "," + rightPlusTag + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-5-dist", leftTags + "," + rightTags + "," + rightPlusTag + attDist));
			
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-1", leftMinusTag + "," + leftTags + "," + rightTags + "," + rightPlusTag));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-2", leftMinusTag + "," + leftTags + "," + rightTags));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-3", leftMinusTag + "," + rightTags + "," + rightPlusTag));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-4", leftMinusTag + "," + leftTags + "," + rightPlusTag));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-5", leftTags + "," + rightTags + "," + rightPlusTag));
			
			
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-1a-dist", leftMinusA + "," + leftAs + "," + rightAs + "," + rightPlusA + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-2a-dist", leftMinusA + "," + leftAs + "," + rightAs + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-3a-dist", leftMinusA + "," + rightAs + "," + rightAs + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-4a-dist", leftMinusA + "," + leftAs + "," + rightPlusA + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-5a-dist", leftAs + "," + rightAs + "," + rightPlusA + attDist));
			
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-1a", leftMinusA + "," + leftAs + "," + rightAs + "," + rightPlusA));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-2a", leftMinusA + "," + leftAs + "," + rightAs));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-3a", leftMinusA + "," + rightAs + "," + rightAs));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-4a", leftMinusA + "," + leftAs + "," + rightPlusA));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-5a", leftAs + "," + rightAs + "," + rightPlusA));

			//l,l+1,r-1,r
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-1-dist", leftTags + "," + leftPlusTag + "," + rightMinusTag + "," + rightTags + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-2-dist", leftTags + "," + rightMinusTag + "," + rightTags + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-3-dist", leftTags + "," + leftPlusTag + "," + rightTags + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-4-dist", leftPlusTag + "," + rightMinusTag + "," + rightTags + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-5-dist", leftTags + "," + leftPlusTag + "," + rightMinusTag + attDist));

			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-1", leftTags + "," + leftPlusTag + "," + rightMinusTag + "," + rightTags));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-2", leftTags + "," + rightMinusTag + "," + rightTags));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-3", leftTags + "," + leftPlusTag + "," + rightTags));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-4", leftPlusTag + "," + rightMinusTag + "," + rightTags));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-5", leftTags + "," + leftPlusTag + "," + rightMinusTag));

			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-1a-dist", leftAs + "," + leftPlusA + "," + rightMinusA + "," + rightAs + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-2a-dist", leftAs + "," + rightMinusA + "," + rightAs + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-3a-dist", leftAs + "," + leftPlusA + "," + rightAs + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-4a-dist", leftPlusA + "," + rightMinusA + "," + rightAs + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-5a-dist", leftAs + "," + leftPlusA + "," + rightMinusA + attDist));

			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-1a", leftAs + "," + leftPlusA + "," + rightMinusA + "," + rightAs));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-2a", leftAs + "," + rightMinusA + "," + rightAs));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-3a", leftAs + "," + leftPlusA + "," + rightAs));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-4a", leftPlusA + "," + rightMinusA + "," + rightAs));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-5a", leftAs + "," + leftPlusA+","+rightMinusA));
			
			//l-1,l,r-1,r
			// l,l+1,r,r+1
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-3-1-dist", leftMinusTag + "," + leftTags + "," + rightMinusTag + "," + rightTags + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-3-1", leftMinusTag + "," + leftTags + "," + rightMinusTag + "," + rightTags));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-3-1a-dist", leftMinusA + "," + leftAs + "," + rightMinusA + "," + rightAs + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-3-1a", leftMinusA + "," + leftAs + "," + rightMinusA + "," + rightAs));

			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-4-1-dist", leftTags + "," + leftPlusTag + "," + rightTags + "," + rightPlusTag + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-4-1", leftTags + "," + leftPlusTag + "," + rightTags + "," + rightPlusTag));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-4-1a-dist", leftAs + "," + leftPlusA + "," + rightAs + "," + rightPlusA + attDist));
			featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-4-1a", leftAs + "," + leftPlusA + "," + rightAs + "," + rightPlusA));
			
			//currently it's also token level
			//should be span level also.
			for (int i = leftSpan.end + 1; i < rightSpan.start; i++) {
				String middleTag = sent.get(i).getTag();
				String middleATag = middleTag.substring(0, 1);
				featureList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-1", leftTags + "," + middleTag + "," + rightTags + attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-2", leftTags + "," + middleTag + "," + rightTags));
				featureList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-3", leftAs + "," + middleATag + "," + rightAs + attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-4", leftAs + "," + middleATag + "," + rightAs));
			}
		}
	}

}

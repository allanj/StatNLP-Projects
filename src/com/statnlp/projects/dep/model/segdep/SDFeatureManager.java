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
		int direction = parentArr[3];
		
		
		if (completeness == COMP.incomp.ordinal()) {
			for (int l = segments.get(leftSpanIndex).start; l <= segments.get(leftSpanIndex).end; l++) {
				for (int r = segments.get(rightSpanIndex).start; r <= segments.get(rightSpanIndex).end; r++) {
					addDepFeatures(featureList, network, l, r, completeness, direction, sent, segments);
				}
			}
		}
	
		ArrayList<Integer> finalList = new ArrayList<Integer>();
		for(int i=0;i<featureList.size();i++){
			if (featureList.get(i) != -1)
				finalList.add(featureList.get(i));
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
	private void addDepFeatures(ArrayList<Integer> featureList, Network network, int leftIndex, int rightIndex, int completeness, int direction, Sentence sent, List<Span> segments){
		
//		int leftSpanIndex = parentArr[0] - parentArr[1];
//		int rightSpanIndex = parentArr[0];
//		int completeness = parentArr[2];
//		int direction = parentArr[3];
//		
//		int leftIndex = segments.get(leftSpanIndex).start;
//		int rightIndex = segments.get(rightSpanIndex).start;
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

}
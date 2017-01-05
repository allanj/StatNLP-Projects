package com.statnlp.projects.dep.model.segdep;

import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureBox;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.projects.dep.utils.DPConfig.COMP;
import com.statnlp.projects.dep.utils.DPConfig.DIR;

public class SDFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 7274939836196010680L;

	private enum FeaType {unigram, bigram, contextual, inbetween, prefix, entity};
	
	public SDFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}
	
	
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		SDInstance inst = (SDInstance)network.getInstance();
		Sentence sent  = inst.getInput();
		List<SegSpan> segments = inst.segments;
		long parent = network.getNode(parent_k);
		int[] parentArr = NetworkIDMapper.toHybridNodeArray(parent);
		
		int leftSpanIndex = parentArr[0] - parentArr[1];
		int rightSpanIndex = parentArr[0];
		int completeness = parentArr[2];
		int direction = parentArr[3];
		int threadId = network.getThreadId();
		
		ArrayList<Integer> headList = new ArrayList<Integer>();
		ArrayList<Integer> headDistList = new ArrayList<Integer>();
		ArrayList<Integer> modifierList = new ArrayList<Integer>();
		ArrayList<Integer> modifierDistList = new ArrayList<Integer>();
		ArrayList<Integer> pairList = new ArrayList<Integer>();
		ArrayList<Integer> pairDistList = new ArrayList<Integer>();
		
		ArrayList<Integer> bound1 = new ArrayList<Integer>();
		ArrayList<Integer> bound1Dist = new ArrayList<Integer>();
		ArrayList<Integer> bound2 = new ArrayList<Integer>();
		ArrayList<Integer> bound2Dist = new ArrayList<Integer>();
		ArrayList<Integer> bound3 = new ArrayList<Integer>();
		ArrayList<Integer> bound3Dist = new ArrayList<Integer>();
		ArrayList<Integer> bound4 = new ArrayList<Integer>();
		ArrayList<Integer> bound4Dist = new ArrayList<Integer>();
		ArrayList<Integer> inDistList = new ArrayList<Integer>();
		ArrayList<Integer> inList = new ArrayList<Integer>();
		ArrayList<Integer> enList = new ArrayList<Integer>();
		
		if (completeness == COMP.incomp.ordinal()) {
			addDepFeatures(headList, headDistList, modifierList, modifierDistList, pairList, pairDistList, bound1, bound1Dist, bound2, bound2Dist, bound3, bound3Dist, bound4, bound4Dist, inDistList, inList, enList, 
					network, leftSpanIndex, rightSpanIndex, completeness, direction, sent, segments);
		}
	
		ArrayList<Integer> finalList = new ArrayList<Integer>();
		for(int i=0;i< headList.size();i++){
			if(headList.get(i)!=-1)
				finalList.add( headList.get(i) );
		}
		int[] fs = new int[finalList.size()];
		for(int i = 0; i < fs.length; i++) fs[i] = finalList.get(i);
		
		FeatureArray orgFa = new FeatureArray(FeatureBox.getFeatureBox(fs, this.getParams_L()[threadId]));
		FeatureArray fa = orgFa;
		ArrayList<ArrayList<Integer>> bigList = new ArrayList<ArrayList<Integer>>();
		bigList.add(headDistList);
		bigList.add(modifierList);
		bigList.add(modifierDistList);
		bigList.add(pairList);
		bigList.add(pairDistList);
		bigList.add(bound1); bigList.add(bound1Dist);
		bigList.add(bound2); bigList.add(bound2Dist);
		bigList.add(bound3); bigList.add(bound3Dist);
		bigList.add(bound4); bigList.add(bound4Dist);
		bigList.add(inList); bigList.add(inDistList);
		bigList.add(enList);
		for (int i = 0; i < bigList.size(); i++) {
			FeatureArray curr = addNext(fa, bigList.get(i), threadId);
			fa = curr;
		}
		return orgFa;
	}
	
	private FeatureArray addNext(FeatureArray fa, ArrayList<Integer> featureList, int threadId)  {
		ArrayList<Integer> finalList = new ArrayList<Integer>();
		for(int i=0;i<featureList.size();i++){
			if(featureList.get(i)!=-1)
				finalList.add( featureList.get(i) );
		}
		if(finalList.size()==0) return fa;
		else {
			int[] fs = new int[finalList.size()];
			for(int i = 0; i < fs.length; i++) fs[i] = finalList.get(i);
			FeatureArray curr = new FeatureArray(FeatureBox.getFeatureBox(fs, this.getParams_L()[threadId]));
			fa.next(curr);
			return curr;
		}
	}
	
	/**
	 * Only available for those segments whose length is 1. since they are for token level.
	 * @param featureList
	 * @param network
	 * @param parentArr
	 * @param sent
	 */
	private void addDepFeatures(List<Integer> headList, List<Integer> headDistList, List<Integer> modifierList, List<Integer> modifierDistList,
			List<Integer> pairList, List<Integer> pairDistList, List<Integer> bound1, List<Integer> bound1Dist, List<Integer> bound2, List<Integer> bound2Dist, 
			List<Integer> bound3, List<Integer> bound3Dist, List<Integer> bound4, List<Integer> bound4Dist, List<Integer> inDistList, List<Integer> inList, List<Integer> enList,
			 Network network, int leftSpanIndex, int rightSpanIndex, int completeness, int direction, Sentence sent, List<SegSpan> segments){
		
//		String leftTag = sent.get(leftIndex).getTag();
//		String rightTag = sent.get(rightIndex).getTag();
//		String leftA = leftTag.substring(0, 1);
//		String rightA = rightTag.substring(0, 1);
		SegSpan leftSpan = segments.get(leftSpanIndex);
		SegSpan rightSpan = segments.get(rightSpanIndex);
		String leftTag = "";
		String rightTag = "";
		String leftA = "";
		String rightA = "";
		for (int idx = leftSpan.start; idx <= leftSpan.end; idx++) {
			if (idx == leftSpan.end) {
				leftTag += sent.get(idx).getTag();
				leftA += sent.get(idx).getTag().substring(0, 1);
			} else {
				leftTag += sent.get(idx).getTag() + "_";
				leftA += sent.get(idx).getTag().substring(0, 1) + "_";
			}
		}
		
		for (int idx = rightSpan.start; idx <= rightSpan.end; idx++) {
			if (idx == rightSpan.end) {
				rightTag += sent.get(idx).getTag();
				rightA += sent.get(idx).getTag().substring(0, 1);
			} else {
				rightTag += sent.get(idx).getTag() + "_";
				rightA += sent.get(idx).getTag().substring(0, 1) + "_";
			}
		}
		
		//dist in a span sense sense...
		/**Dist is over span now.**/
		int dist = Math.abs(rightSpanIndex - leftSpanIndex);
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
//			int headIndex = direction == DIR.right.ordinal()? leftIndex: rightIndex;
//			int modifierIndex = direction == DIR.right.ordinal()? rightIndex : leftIndex;
			int headSpanIndex = direction == DIR.right.ordinal()? leftSpanIndex: rightSpanIndex;
			int modifierSpanIndex = direction == DIR.right.ordinal()? rightSpanIndex : leftSpanIndex;
			SegSpan headSpan = segments.get(headSpanIndex);
			SegSpan modifierSpan = segments.get(modifierSpanIndex);
//			String headWord = sent.get(headIndex).getName();
//			String headTag = sent.get(headIndex).getTag();
//			String modifierWord = sent.get(modifierIndex).getName();
//			String modifierTag = sent.get(modifierIndex).getTag();
			String headWord = "";
			String headTag = "";
			String modifierWord = "";
			String modifierTag = "";
			for (int idx = headSpan.start; idx <= headSpan.end; idx++) {
				if (idx == headSpan.end) {
					headWord += sent.get(idx).getName();
					headTag += sent.get(idx).getTag();
				} else {
					headWord += sent.get(idx).getName() + "_";
					headTag += sent.get(idx).getTag() + "_";
				}
			}
			for (int idx = modifierSpan.start; idx <= modifierSpan.end; idx++) {
				if (idx == modifierSpan.end) {
					modifierWord += sent.get(idx).getName();
					modifierTag += sent.get(idx).getTag();
				} else {
					modifierWord += sent.get(idx).getName() + "_";
					modifierTag += sent.get(idx).getTag() + "_";
				}
			}
			
			
			if(headWord.length()>5 || modifierWord.length()>5){
				int hL = headWord.length();
				int mL = modifierWord.length();
				String preHead = hL>5? headWord.substring(0,5):headWord;
				String preModifier = mL>5?modifierWord.substring(0,5):modifierWord;
				pairDistList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "bigram-prefix-all-dist", preHead+","+headTag+","+preModifier+","+modifierTag+attDist));
				pairDistList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "bigram-prefix-word-dist", preHead+","+preModifier+attDist));
				pairList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "bigram-prefix-all", preHead+","+headTag+","+preModifier+","+modifierTag));
				pairList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "bigram-prefix-word", preHead+","+preModifier));
				if(mL>5){
					pairDistList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-hTmomoT-dist", headTag+","+preModifier+","+modifierTag+attDist));
					pairDistList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-hTmo-dist", headTag+","+preModifier+attDist));
					pairDistList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-modiall-dist", preModifier+","+modifierTag+attDist));
					pairDistList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-modi-dist", preModifier+attDist));
					pairList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-hTmomoT", headTag+","+preModifier+","+modifierTag));
					pairList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-hTmo", headTag+","+preModifier));
					pairList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-modiall", preModifier+","+modifierTag));
					pairList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-modiall", preModifier));
				}
				if(hL>5){
					pairDistList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hhTmoT-dist", preHead+","+headTag+","+modifierTag+attDist));
					pairDistList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hmoT-dist", preHead+","+modifierTag+attDist));
					pairDistList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hall-dist", preHead+","+headTag+attDist));
					pairDistList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-h-dist", preHead+attDist));
					pairList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hhTmoT", preHead+","+headTag+","+modifierTag));
					pairList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hmoT", preHead+","+modifierTag));
					pairList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hall", preHead+","+headTag));
					pairList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-h", preHead));
				}
			}
			
			/**Unigram feature without dist info**/
			headList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headword", headWord));
			headList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headtag", headTag));
			modifierList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifierword", modifierWord));
			modifierList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifiertag", modifierTag));
			headList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headwordtag", headWord+","+headTag));
			modifierList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifierwordtag", modifierWord+","+modifierTag));
			
			/**Unigram feature with dist info**/
			headDistList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headword-dist", headWord+attDist));
			headDistList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headtag-dist", headTag+attDist));
			modifierDistList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifierword-dist", modifierWord+attDist));
			modifierDistList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifiertag-dist", modifierTag+attDist));
			headDistList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headwordtag-dist", headWord+","+headTag+attDist));
			modifierDistList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifierwordtag-dist", modifierWord+","+modifierTag+attDist));
			
			/****Bigram features without dist info******/
			pairList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "bigramword", headWord+","+modifierWord));
			pairList.add(this._param_g.toFeature(network, FeaType.bigram.name(),"bigramtag", headTag+","+modifierTag));
			pairList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "bigramnametag",  headWord+","+headTag+","+modifierWord+","+modifierTag));
			pairList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headallmoditag", headWord+","+headTag+","+modifierTag));
			pairList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headallmodiword", headWord+","+headTag+","+modifierWord));
			pairList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headtagmodiall", headTag+","+modifierWord+","+modifierTag));
			pairList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headwordmodiall", headWord+","+modifierWord+","+modifierTag));
			
			/****Bigram features with dist info******/
			pairDistList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "bigramword-dist", headWord+","+modifierWord+attDist));
			pairDistList.add(this._param_g.toFeature(network, FeaType.bigram.name(),"bigramtag-dist", headTag+","+modifierTag+attDist));
			pairDistList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "bigramnametag-dist",  headWord+","+headTag+","+modifierWord+","+modifierTag+attDist));
			pairDistList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headallmoditag-dist", headWord+","+headTag+","+modifierTag+attDist));
			pairDistList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headallmodiword-dist", headWord+","+headTag+","+modifierWord+attDist));
			pairDistList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headtagmodiall-dist", headTag+","+modifierWord+","+modifierTag+attDist));
			pairDistList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headwordmodiall-dist", headWord+","+modifierWord+","+modifierTag+attDist));
		
			
			
//			String leftMinusTag = leftIndex>0? sent.get(leftIndex-1).getTag(): "STR";
//			String rightPlusTag = rightIndex<sent.length()-1? sent.get(rightIndex+1).getTag():"END";
//			String leftPlusTag = leftIndex<rightIndex-1? sent.get(leftIndex+1).getTag():"MID";
//			String rightMinusTag = rightIndex-1 > leftIndex? sent.get(rightIndex-1).getTag():"MID";
//			
//			String leftMinusA = leftIndex>0? sent.get(leftIndex-1).getATag(): "STR";
//			String rightPlusA = rightIndex<sent.length()-1? sent.get(rightIndex+1).getATag():"END";
//			String leftPlusA = leftIndex<rightIndex-1? sent.get(leftIndex+1).getATag():"MID";
//			String rightMinusA = rightIndex-1 > leftIndex?sent.get(rightIndex-1).getATag():"MID";
			String leftMinusTag = "";
			String rightPlusTag = "";
			String leftPlusTag = "";
			String rightMinusTag = "";
			
			String leftMinusA = "";
			String rightPlusA = "";
			String leftPlusA = "";
			String rightMinusA = "";
			if (leftSpanIndex > 0) {
				SegSpan leftMinusSpan = segments.get(leftSpanIndex - 1);
				for (int idx = leftMinusSpan.start; idx <= leftMinusSpan.end; idx++) {
					if (idx == leftMinusSpan.end) {
						leftMinusTag += sent.get(idx).getTag();
						leftMinusA += sent.get(idx).getATag();
					} else {
						leftMinusTag += sent.get(idx).getTag() + "_";
						leftMinusA += sent.get(idx).getATag() + "_";
					}
				}
			} else {
				leftMinusTag = "STR";
				leftMinusA = "STR";
			}
			
			
			if (rightSpanIndex < segments.size() - 1) {
				SegSpan rightPlusSpan = segments.get(rightSpanIndex + 1);
				for (int idx = rightPlusSpan.start; idx <= rightPlusSpan.end; idx++) {
					if (idx == rightPlusSpan.end) {
						rightPlusTag += sent.get(idx).getTag();
						leftMinusA += sent.get(idx).getATag();
					} else {
						rightPlusTag += sent.get(idx).getTag() + "_";
						leftMinusA += sent.get(idx).getATag() + "_";
					}
				}
			} else {
				rightPlusTag = "END";
				rightPlusA = "END";
			}
			
			if (leftSpanIndex < rightSpanIndex - 1) {
				SegSpan leftPlusSpan = segments.get(leftSpanIndex + 1);
				for (int idx = leftPlusSpan.start; idx <= leftPlusSpan.end; idx++) {
					if (idx == leftPlusSpan.end) {
						leftPlusTag += sent.get(idx).getTag();
						leftPlusA += sent.get(idx).getATag();
					} else {
						leftPlusTag += sent.get(idx).getTag() + "_";
						leftPlusA += sent.get(idx).getATag() + "_";
					}
				}
			} else {
				leftPlusTag = "END";
				leftPlusA = "END";
			}
			
			if (rightSpanIndex - 1 > leftSpanIndex) {
				SegSpan rightMinusSpan = segments.get(rightSpanIndex - 1);
				for (int idx = rightMinusSpan.start; idx <= rightMinusSpan.end; idx++) {
					if (idx == rightMinusSpan.end) {
						rightMinusTag += sent.get(idx).getTag();
						rightMinusA += sent.get(idx).getATag();
					} else {
						rightMinusTag += sent.get(idx).getTag() + "_";
						rightMinusA += sent.get(idx).getATag() + "_";
					}
				}
			} else {
				rightMinusTag = "END";
				rightMinusA = "END";
			}
			
			//l-1,l,r,r+1
			bound1Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-1-dist", leftMinusTag+","+leftTag+","+rightTag+","+rightPlusTag+attDist));
			bound1Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-2-dist", leftMinusTag+","+leftTag+","+rightTag+attDist));
			bound1Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-3-dist", leftMinusTag+","+rightTag+","+rightPlusTag+attDist));
			bound1Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-4-dist", leftMinusTag+","+leftTag+","+rightPlusTag+attDist));
			bound1Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-5-dist", leftTag+","+rightTag+","+rightPlusTag+attDist));
			
			bound1.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-1", leftMinusTag+","+leftTag+","+rightTag+","+rightPlusTag));
			bound1.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-2", leftMinusTag+","+leftTag+","+rightTag));
			bound1.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-3", leftMinusTag+","+rightTag+","+rightPlusTag));
			bound1.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-4", leftMinusTag+","+leftTag+","+rightPlusTag));
			bound1.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-5", leftTag+","+rightTag+","+rightPlusTag));
			
			
			bound1Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-1a-dist", leftMinusA+","+leftA+","+rightA+","+rightPlusA+attDist));
			bound1Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-2a-dist", leftMinusA+","+leftA+","+rightA+attDist));
			bound1Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-3a-dist", leftMinusA+","+rightA+","+rightA+attDist));
			bound1Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-4a-dist", leftMinusA+","+leftA+","+rightPlusA+attDist));
			bound1Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-5a-dist", leftA+","+rightA+","+rightPlusA+attDist));
			
			bound1.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-1a", leftMinusA+","+leftA+","+rightA+","+rightPlusA));
			bound1.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-2a", leftMinusA+","+leftA+","+rightA));
			bound1.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-3a", leftMinusA+","+rightA+","+rightA));
			bound1.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-4a", leftMinusA+","+leftA+","+rightPlusA));
			bound1.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-5a", leftA+","+rightA+","+rightPlusA));
			
			//l,l+1,r-1,r
			bound2Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-1-dist", leftTag+","+leftPlusTag+","+rightMinusTag+","+rightTag+attDist));
			bound2Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-2-dist", leftTag+","+rightMinusTag+","+rightTag+attDist));
			bound2Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-3-dist", leftTag+","+leftPlusTag+","+rightTag+attDist));
			bound2Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-4-dist", leftPlusTag+","+rightMinusTag+","+rightTag+attDist));
			bound2Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-5-dist", leftTag+","+leftPlusTag+","+rightMinusTag+attDist));
			
			bound2.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-1", leftTag+","+leftPlusTag+","+rightMinusTag+","+rightTag));
			bound2.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-2", leftTag+","+rightMinusTag+","+rightTag));
			bound2.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-3", leftTag+","+leftPlusTag+","+rightTag));
			bound2.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-4", leftPlusTag+","+rightMinusTag+","+rightTag));
			bound2.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-5", leftTag+","+leftPlusTag+","+rightMinusTag));
			
			bound2Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-1a-dist", leftA+","+leftPlusA+","+rightMinusA+","+rightA+attDist));
			bound2Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-2a-dist", leftA+","+rightMinusA+","+rightA+attDist));
			bound2Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-3a-dist", leftA+","+leftPlusA+","+rightA+attDist));
			bound2Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-4a-dist", leftPlusA+","+rightMinusA+","+rightA+attDist));
			bound2Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-5a-dist", leftA+","+leftPlusA+","+rightMinusA+attDist));
			
			bound2.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-1a", leftA+","+leftPlusA+","+rightMinusA+","+rightA));
			bound2.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-2a", leftA+","+rightMinusA+","+rightA));
			bound2.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-3a", leftA+","+leftPlusA+","+rightA));
			bound2.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-4a", leftPlusA+","+rightMinusA+","+rightA));
			bound2.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-5a", leftA+","+leftPlusA+","+rightMinusA));
			
			//l-1,l,r-1,r
			//l,l+1,r,r+1
			bound3Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-3-1-dist", leftMinusTag+","+leftTag+","+rightMinusTag+","+rightTag+attDist));
			bound3.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-3-1", leftMinusTag+","+leftTag+","+rightMinusTag+","+rightTag));
			bound3Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-3-1a-dist", leftMinusA+","+leftA+","+rightMinusA+","+rightA+attDist));
			bound3.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-3-1a", leftMinusA+","+leftA+","+rightMinusA+","+rightA));
			
			bound4Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-4-1-dist", leftTag+","+leftPlusTag+","+rightTag+","+rightPlusTag+attDist));
			bound4.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-4-1", leftTag+","+leftPlusTag+","+rightTag+","+rightPlusTag));
			bound4Dist.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-4-1a-dist", leftA+","+leftPlusA+","+rightA+","+rightPlusA+attDist));
			bound4.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-4-1a", leftA+","+leftPlusA+","+rightA+","+rightPlusA));
			
			
//			for(int i = leftIndex + 1; i < rightIndex; i++){
//				inDistList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-1", leftTag+","+sent.get(i).getTag()+","+rightTag+attDist));
//				inList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-2", leftTag+","+sent.get(i).getTag()+","+rightTag));
//				inDistList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-3", leftA+","+sent.get(i).getATag()+","+rightA+attDist));
//				inList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-4", leftA+","+sent.get(i).getATag()+","+rightA));
//			}
			
			for(int i = leftSpanIndex + 1; i < rightSpanIndex; i++){
				SegSpan middleSpan = segments.get(i);
				String middleTag = "";
				String middleATag = "";
				for (int idx = middleSpan.start; idx <= middleSpan.end; idx++) {
					if (idx == middleSpan.end) {
						middleTag += sent.get(idx).getTag();
						middleATag += sent.get(idx).getATag();
					} else {
						middleTag += sent.get(idx).getTag() + "_";
						middleATag += sent.get(idx).getATag() + "_";
					}
				}
				inDistList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-1", leftTag+","+middleTag+","+rightTag+attDist));
				inList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-2", leftTag+","+middleTag+","+rightTag));
				inDistList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-3", leftA+","+middleATag+","+rightA+attDist));
				inList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-4", leftA+","+middleATag+","+rightA));
			}
			
			//entity features
			String me = segments.get(modifierSpanIndex).label.form;
			String he = segments.get(headSpanIndex).label.form;
			enList.add(this._param_g.toFeature(network, FeaType.entity.name(), "entity-hwmw-heme",  headWord + " & " + modifierWord + " & " + he + " & " + me));
			enList.add(this._param_g.toFeature(network, FeaType.entity.name(), "entity-htmt-heme",  headTag + " & " + modifierTag + " & " + he + " & " + me));
		}
	}

}

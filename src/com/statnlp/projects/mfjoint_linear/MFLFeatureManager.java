package com.statnlp.projects.mfjoint_linear;

import java.util.ArrayList;
import java.util.Arrays;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureBox;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.projects.dep.utils.Extractor;
import com.statnlp.projects.mfjoint_linear.MFLConfig.COMP;
import com.statnlp.projects.mfjoint_linear.MFLConfig.DIR;
import com.statnlp.projects.mfjoint_linear.MFLNetworkCompiler.NodeType;

public class MFLFeatureManager extends FeatureManager {


	private static final long serialVersionUID = -8984938159601419422L;
	
	private boolean useJointFeatures;
	private int prefixSuffixLen = 3;
	
	public enum FeaType {
		word, tag, shape, prev_word, prev_tag, prev_shape, next_word, next_tag, next_shape,
		 surround_tags, surround_shapes, word_n_gram, left_4, right_4, entity_prefix, entity_suffix,transition,
		unigram, bigram, contextual, inbetween, prefix, joint1, joint2, joint3, joint4
	}
	
	public MFLFeatureManager(GlobalNetworkParam param_g, boolean useJointFeature) {
		super(param_g);
		this.useJointFeatures = useJointFeature;
	}


	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		MFLInstance inst = (MFLInstance)network.getInstance();
		Sentence sent = inst.getInput();
		long node = network.getNode(parent_k);
		int[] nodeArr = NetworkIDMapper.toHybridNodeArray(node);
		int threadId = network.getThreadId();
		
		int pos = nodeArr[0];
		if (pos == 0 || pos == sent.length())
			return FeatureArray.EMPTY;
		
		FeatureArray fa = new FeatureArray(FeatureBox.getFeatureBox(new int[]{}, this.getParams_L()[threadId]));
		if (nodeArr[4] == NodeType.ENTITY.ordinal()) {
			int labelId = nodeArr[1];
			int[] child_arr = network.getNodeArray(children_k[0]);
			int start = child_arr[0] + 1;
			int childLabelId = child_arr[1];
			FeatureArray curr = addLinearCRFFeatures(fa, network, sent, pos, start, labelId, childLabelId, threadId);
			if (useJointFeatures) {
				addJointFeaturesForLinear(curr, network, sent, threadId, parent_k, nodeArr, child_arr);
			}
		} else if (nodeArr[4] == NodeType.DEP.ordinal()) {
			int leftIndex = nodeArr[0] - nodeArr[1];
			int rightIndex = nodeArr[0];
			int direction = nodeArr[3];
			int completeness = nodeArr[2];
			FeatureArray curr = addDepFeatures(fa, network, sent, leftIndex, rightIndex, completeness, direction, threadId);
			if (useJointFeatures) {
				addJointFeaturesForDep(curr, network, sent, threadId, parent_k, nodeArr, leftIndex, rightIndex, direction);
			}
		}
		
		return fa;
	}
	
	
	/**
	 * return the last feature array
	 * @param orgFa
	 * @param network
	 * @param pos
	 * @param childPos
	 * @param eId
	 * @param childLabelId
	 * @param threadId
	 * @return
	 */
	private FeatureArray addLinearCRFFeatures(FeatureArray orgFa, Network network, Sentence sent, int parentPos, int childPos, int labelId, int childLabelId, int threadId) {
		
		int idx = parentPos;
		String currEn = MFLLabel.get(labelId).getForm();

		ArrayList<Integer> currList = new ArrayList<Integer>();
		ArrayList<Integer> prevList = new ArrayList<Integer>();
		ArrayList<Integer> nextList = new ArrayList<Integer>();
		ArrayList<Integer> surrList = new ArrayList<Integer>();
		ArrayList<Integer> left4List = new ArrayList<Integer>();
		ArrayList<Integer> right4List = new ArrayList<Integer>();
		ArrayList<Integer> ngramList = new ArrayList<Integer>();
		ArrayList<Integer> prefixList = new ArrayList<Integer>();
		ArrayList<Integer> suffixList = new ArrayList<Integer>();
		ArrayList<Integer> transList = new ArrayList<Integer>();
		
		
		String w = sent.get(idx).getName();
		String t = sent.get(idx).getTag();
		String s = shape(w);
		String lw = idx > 1? sent.get(idx - 1).getName() : "STR";
		String ls = idx > 1? shape(lw):"STR_SHAPE";
		String lt = idx > 1? sent.get(idx - 1).getTag():"STR";
		String llw = idx - 2 > 0 ? sent.get(idx - 2).getName() : idx == 2 ? "STR" : "STR1";
		String lllw = idx - 3 > 0 ? sent.get(idx - 3).getName() : idx == 3 ? "STR" : idx == 2 ? "STR1" : "STR2";
		String llllw = idx - 4 > 0 ? sent.get(idx - 4).getName() : idx == 4 ? "STR" : idx == 3 ? "STR1" : idx == 2? "STR2" : "STR3";
		String rw = idx < sent.length()-1? sent.get(idx + 1).getName():"END";
		String rt = idx < sent.length()-1? sent.get(idx + 1).getTag():"END";
		String rs = idx < sent.length()-1? shape(rw):"END_SHAPE";
		String rrw = idx + 2 < sent.length() ? sent.get(idx + 2).getName() : idx + 2 == sent.length() ? "END" : "END1";
		String rrrw = idx + 3 < sent.length() ? sent.get(idx + 3).getName() : idx + 3 == sent.length() ? "END" : idx + 2 == sent.length() ? "END1" : "END2";
		String rrrrw = idx + 4 < sent.length() ? sent.get(idx + 3).getName() : idx + 4 == sent.length() ? "END" : idx + 3 == sent.length() ? "END1" : idx + 2 == sent.length() ? "END2" : "END3"; 
		
		currList.add(this._param_g.toFeature(network, FeaType.word.name(), 		currEn,	w));
		currList.add(this._param_g.toFeature(network, FeaType.tag.name(),   	currEn, t));
		currList.add(this._param_g.toFeature(network, FeaType.shape.name(), 	currEn, s));
		prevList.add(this._param_g.toFeature(network, FeaType.prev_word.name(), currEn, lw));
		prevList.add(this._param_g.toFeature(network, FeaType.prev_tag.name(), 	currEn, lt));
		prevList.add(this._param_g.toFeature(network, FeaType.prev_shape.name(),currEn, ls));
		nextList.add(this._param_g.toFeature(network, FeaType.next_word.name(), currEn, rw));
		nextList.add(this._param_g.toFeature(network, FeaType.next_tag.name(), 	currEn, rt));
		nextList.add(this._param_g.toFeature(network, FeaType.next_shape.name(),currEn, rs));
		
		surrList.add(this._param_g.toFeature(network, FeaType.surround_tags.name(), 	currEn,	lt + "-" + rt));
		surrList.add(this._param_g.toFeature(network, FeaType.surround_shapes.name(),	currEn,	shape(lw) + "-" + shape(rw)));
		
		
		for (int l = 1; l <= w.length(); l++) {
			for (int sp = 0; sp <= w.length() - l; sp++) {
				ngramList.add(this._param_g.toFeature(network, FeaType.word_n_gram.name() + l, 	currEn,	w.substring(sp, sp + l)));
			}
		}
		left4List.add(this._param_g.toFeature(network, FeaType.left_4.name(), 	currEn,	llllw + " & " + lllw + " & " + llw + " & " + lw));
		right4List.add(this._param_g.toFeature(network, FeaType.right_4.name(), currEn,	rw + " & " + rrw + " & " + rrrw + " & " + rrrrw));
		
		for(int plen = 1;plen <= prefixSuffixLen;plen++){
			if(w.length() >= plen){
				String suff = w.substring(w.length()-plen, w.length());
				prefixList.add(this._param_g.toFeature(network, FeaType.entity_suffix.name()+"-"+plen, currEn, suff));
				String pref = w.substring(0,plen);
				prefixList.add(this._param_g.toFeature(network, FeaType.entity_prefix.name()+"-"+plen, currEn, pref));
			}
		}
		
		String prevEntity = MFLLabel.get(childLabelId).getForm();
		transList.add(this._param_g.toFeature(network,FeaType.transition.name(), currEn,	prevEntity));
		
		
		ArrayList<ArrayList<Integer>> bigList = new ArrayList<ArrayList<Integer>>();
		bigList.add(currList);bigList.add(prevList); 
		bigList.add(nextList); bigList.add(surrList);
		bigList.add(left4List); bigList.add(right4List); 
		bigList.add(ngramList); bigList.add(prefixList); 
		bigList.add(suffixList); bigList.add(transList); 
		FeatureArray last = orgFa;
		for (int i = 0; i < bigList.size(); i++) {
			FeatureArray curr = addNext(last, bigList.get(i), threadId);
			last = curr;
		}
		return last;
	}

	private FeatureArray addDepFeatures(FeatureArray orgFa, Network network, Sentence sent, int leftIndex, int rightIndex, int completeness, int direction, int threadId) {
		
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
		
		String leftTag = sent.get(leftIndex).getTag();
		String rightTag = sent.get(rightIndex).getTag();
		String leftA = leftTag.substring(0, 1);
		String rightA = rightTag.substring(0, 1);
		
		//dist in a span sense sense...
		/**Dist is over span now.**/
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
		
			
			
			String leftMinusTag = leftIndex>0? sent.get(leftIndex-1).getTag(): "STR";
			String rightPlusTag = rightIndex<sent.length()-1? sent.get(rightIndex+1).getTag():"END";
			String leftPlusTag = leftIndex<rightIndex-1? sent.get(leftIndex+1).getTag():"MID";
			String rightMinusTag = rightIndex-1 > leftIndex? sent.get(rightIndex-1).getTag():"MID";
			
			String leftMinusA = leftIndex>0? sent.get(leftIndex-1).getATag(): "STR";
			String rightPlusA = rightIndex<sent.length()-1? sent.get(rightIndex+1).getATag():"END";
			String leftPlusA = leftIndex<rightIndex-1? sent.get(leftIndex+1).getATag():"MID";
			String rightMinusA = rightIndex-1 > leftIndex?sent.get(rightIndex-1).getATag():"MID";
			
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
			
			
			for(int i = leftIndex + 1; i < rightIndex; i++){
				inDistList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-1", leftTag+","+sent.get(i).getTag()+","+rightTag+attDist));
				inList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-2", leftTag+","+sent.get(i).getTag()+","+rightTag));
				inDistList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-3", leftA+","+sent.get(i).getATag()+","+rightA+attDist));
				inList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-4", leftA+","+sent.get(i).getATag()+","+rightA));
			}
		}
		
		ArrayList<ArrayList<Integer>> bigList = new ArrayList<ArrayList<Integer>>();
		bigList.add(headDistList); bigList.add(modifierList);
		bigList.add(modifierDistList); bigList.add(pairList); bigList.add(pairDistList);
		bigList.add(bound1); bigList.add(bound1Dist);
		bigList.add(bound2); bigList.add(bound2Dist);
		bigList.add(bound3); bigList.add(bound3Dist);
		bigList.add(bound4); bigList.add(bound4Dist);
		bigList.add(inList); bigList.add(inDistList);
		FeatureArray fa = orgFa;
		for (int i = 0; i < bigList.size(); i++) {
			FeatureArray curr = addNext(fa, bigList.get(i), threadId);
			fa = curr;
		}
		return fa;
	}
	
	private FeatureArray addNext(FeatureArray fa, ArrayList<Integer> featureList, int threadId) {
		return addNext(fa, featureList, threadId, false);
	}
	
	private FeatureArray addNext(FeatureArray fa, ArrayList<Integer> featureList, int threadId, boolean alwaysChanged)  {
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
			if (alwaysChanged)
				curr.setAlwaysChange();
			fa.next(curr);
			return curr;
		}
	}
	
	private void addJointFeaturesForLinear(FeatureArray curr, Network network, Sentence sent, int threadId, int node_k, int[] nodeArr, int[] childArr) {
		String label = MFLLabel.get(nodeArr[1]).form;
		int idx = nodeArr[0];
		MFLNetwork unlabeledNetwork = (MFLNetwork) network.getUnlabeledNetwork();
		ArrayList<Integer> joint1List = new ArrayList<Integer>();
		ArrayList<Integer> joint2List = new ArrayList<Integer>();
		ArrayList<Integer> joint3List = new ArrayList<Integer>();
		ArrayList<Integer> joint4List = new ArrayList<Integer>();
		int jf1, jf2, jf3, jf4;
		String currWord = sent.get(idx).getName();
		String currTag = sent.get(idx).getTag();
		for (int headIdx = 0; headIdx <= sent.length() - 1; headIdx++) {
			if (headIdx == idx) continue;
			int direction = headIdx < idx ? DIR.right.ordinal() : DIR.left.ordinal();
			String dir = headIdx < idx ? DIR.right.name() : DIR.left.name();
			int right = headIdx < idx ? idx : headIdx;
			int left = headIdx < idx ? headIdx : idx;
			String headWord = sent.get(headIdx).getName();
			String headTag = sent.get(headIdx).getTag();
			int[] dstNodeArr = new int[]{right, right-left, COMP.incomp.ordinal(), direction, NodeType.DEP.ordinal()};
			long unlabeledDstNode = NetworkIDMapper.toHybridNodeID(dstNodeArr);
			int unlabeledDstNodeIdx = Arrays.binarySearch(unlabeledNetwork.getAllNodes(), unlabeledDstNode);
			if (unlabeledDstNodeIdx >= 0) {
				jf1 = this._param_g.toFeature(network, FeaType.joint1.name(), label + "&" + dir, currWord);
				jf2 = this._param_g.toFeature(network, FeaType.joint2.name(), label + "&" + dir, headWord + " & " + currWord);
				jf3 = this._param_g.toFeature(network, FeaType.joint3.name(), label + "&" + dir, currTag);
				jf4 = this._param_g.toFeature(network, FeaType.joint4.name(), label + "&" + dir, headTag + " & " + currTag);
				if(jf1 != -1){
					joint1List.add(jf1); network.putJointFeature(node_k, jf1, unlabeledDstNodeIdx);
				}
				if(jf2 != -1){
					joint2List.add(jf2); network.putJointFeature(node_k, jf2, unlabeledDstNodeIdx);
				}
				if(jf3 != -1){
					joint3List.add(jf3); network.putJointFeature(node_k, jf3, unlabeledDstNodeIdx);
				}
				if(jf4 != -1){
					joint4List.add(jf4); network.putJointFeature(node_k, jf4, unlabeledDstNodeIdx);
				}
			}
		}
		ArrayList<ArrayList<Integer>> bigList = new ArrayList<ArrayList<Integer>>();
		bigList.add(joint1List);
		bigList.add(joint2List);
		bigList.add(joint3List);
		bigList.add(joint4List);
		FeatureArray last = curr;
		for (int i = 0; i < bigList.size(); i++) {
			//setting always changed is very important
			FeatureArray now = addNext(last, bigList.get(i), threadId, true);
			last = now;
		}
	}

	private void addJointFeaturesForDep(FeatureArray curr, Network network, Sentence sent, int threadId, int node_k, int[] nodeArr, int leftIndex, int rightIndex, int direction) {
		int headIndex = direction == DIR.right.ordinal()? leftIndex: rightIndex;
		int modifierIndex = direction == DIR.right.ordinal()? rightIndex : leftIndex;
		String dir = direction == DIR.right.ordinal() ? DIR.right.name() : DIR.left.name();
		String headWord = sent.get(headIndex).getName();
		String headTag = sent.get(headIndex).getTag();
		String modifierWord = sent.get(modifierIndex).getName();
		String modifierTag = sent.get(modifierIndex).getTag();
		ArrayList<Integer> joint1List = new ArrayList<Integer>();
		ArrayList<Integer> joint2List = new ArrayList<Integer>();
		ArrayList<Integer> joint3List = new ArrayList<Integer>();
		ArrayList<Integer> joint4List = new ArrayList<Integer>();
		int jf1, jf2, jf3, jf4;
		MFLNetwork unlabeledNetwork = (MFLNetwork) network.getUnlabeledNetwork();
		int endIdx = modifierIndex;
		for (int labelId = 0; labelId < MFLLabel.Labels.size(); labelId++) {
			String spanLabel = MFLLabel.Label_Index.get(labelId).form;
			int[] dstNodeArr = new int[]{endIdx, labelId, 0, 0, NodeType.ENTITY.ordinal()};
			long unlabeledDstNode = NetworkIDMapper.toHybridNodeID(dstNodeArr);
			int unlabeledDstNodeIdx = Arrays.binarySearch(unlabeledNetwork.getAllNodes(), unlabeledDstNode);
			if (unlabeledDstNodeIdx >= 0) {
				jf1 = this._param_g.toFeature(network, FeaType.joint1.name(), spanLabel + "&" + dir, modifierWord);
				jf2 = this._param_g.toFeature(network, FeaType.joint2.name(), spanLabel + "&" + dir, headWord + " & " + modifierWord);
				jf3 = this._param_g.toFeature(network, FeaType.joint3.name(), spanLabel + "&" + dir, modifierTag);
				jf4 = this._param_g.toFeature(network, FeaType.joint4.name(), spanLabel + "&" + dir, headTag + " & " + modifierTag);
				if(jf1 != -1){
					joint1List.add(jf1); network.putJointFeature(node_k, jf1, unlabeledDstNodeIdx);
				}
				if(jf2 != -1){
					joint2List.add(jf2); network.putJointFeature(node_k, jf2, unlabeledDstNodeIdx);
				}
				if(jf3 != -1){
					joint3List.add(jf3); network.putJointFeature(node_k, jf3, unlabeledDstNodeIdx);
				}
				if(jf4 != -1){
					joint4List.add(jf4); network.putJointFeature(node_k, jf4, unlabeledDstNodeIdx);
				}
			}
		}
		ArrayList<ArrayList<Integer>> bigList = new ArrayList<ArrayList<Integer>>();
		bigList.add(joint1List);
		bigList.add(joint2List);
		bigList.add(joint3List);
		bigList.add(joint4List);
		FeatureArray last = curr;
		for (int i = 0; i < bigList.size(); i++) {
			//setting always changed is very important
			FeatureArray now = addNext(last, bigList.get(i), threadId, true);
			last = now;
		}
	}
	
	private String shape(String word){
		return Extractor.wordShapeOf(word);
	}
	
}

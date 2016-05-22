package com.statnlp.dp.model.labner;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.entity.lcr2d.E2DFeatureManager.FEATYPE;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class LNERFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 7274939836196010680L;

	public enum FEATYPE {unigram, bigram,contextual, inbetween, prefix,pipe,label, entity};
	protected boolean isPipe;
	
	public LNERFeatureManager(GlobalNetworkParam param_g, boolean isPipe) {
		super(param_g);
		this.isPipe = isPipe;
		// TODO Auto-generated constructor stub
	}
	
	
	/**
	 * Only add the transition features here.
	 */
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		
		FeatureArray fa = null;
		
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		
		LNERInstance di = (LNERInstance)network.getInstance();
//		int instanceId = di.getInstanceId();
		Sentence sent  = di.getInput();
		long parent = network.getNode(parent_k);
		int[] parentArr = NetworkIDMapper.toHybridNodeArray(parent);
		int leftIndex = parentArr[0] - parentArr[1];
		int rightIndex = parentArr[0];
		int direction = parentArr[3];
		int completeness = parentArr[2];
		
		
		String leftTag = sent.get(leftIndex).getTag();
		String rightTag = sent.get(rightIndex).getTag();
		String leftA = leftTag.substring(0, 1);
		String rightA = rightTag.substring(0, 1);
		
		int headIndex = -1;
		int modifierIndex = -1;
		String headWord = null;
		String headTag = null;
		String modifierWord = null;
		String modifierTag = null;
		
		if(leftIndex==rightIndex){
			addEntityFeatures(network, sent, leftIndex, rightIndex, NERLabel.LABELS_INDEX.get(parentArr[4]).getForm(), featureList);
		}
		//if incomplete span or complete but with spanlen is 2
		if(completeness==0){
			String label = NERLabel.LABELS_INDEX.get(parentArr[4]).getForm();
			int dist = Math.abs(rightIndex-leftIndex);
			String att = direction==1? "RA":"LA";
			String distBool = "0";
			if(dist > 1)
			    distBool = "1";
			if(dist > 2)
			    distBool = "2";
			if(dist > 3)
			    distBool = "3";
			if(dist > 4)
			    distBool = "4";
			if(dist > 5)
			    distBool = "5";
			if(dist > 10)
			    distBool = "10";
			
			
			String attDist = "&"+att+"&"+distBool;
			
			if(direction==1){
				headIndex = leftIndex;
				modifierIndex = rightIndex;
			}
			else {
				headIndex = rightIndex;
				modifierIndex = leftIndex;
			}
			headWord = sent.get(headIndex).getName();
			headTag = sent.get(headIndex).getTag();
			modifierWord = sent.get(modifierIndex).getName();
			modifierTag = sent.get(modifierIndex).getTag();
			addLabeledFeatures(network, att, true, sent, modifierIndex, label, featureList);
			addLabeledFeatures(network, att, false, sent, headIndex, label, featureList);
			addEntityFeatures(network, sent, leftIndex, rightIndex, label, featureList);
			addJointFeatures(network, sent, leftIndex, rightIndex, direction, label, featureList, attDist);
			
			/***Prefix 5 gram features*********/
			if(headWord.length()>5 || modifierWord.length()>5){
				int hL = headWord.length();
				int mL = modifierWord.length();
				String preHead = hL>5? headWord.substring(0,5):headWord;
				String preModifier = mL>5?modifierWord.substring(0,5):modifierWord;
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "bigram-prefix-all-dist", preHead+","+headTag+","+preModifier+","+modifierTag+attDist));;
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "bigram-prefix-word-dist", preHead+","+preModifier+attDist));;
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "bigram-prefix-all", preHead+","+headTag+","+preModifier+","+modifierTag));;
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "bigram-prefix-word", preHead+","+preModifier));;
				if(mL>5){
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-hTmomoT-dist", headTag+","+preModifier+","+modifierTag+attDist));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-hTmo-dist", headTag+","+preModifier+attDist));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-modiall-dist", preModifier+","+modifierTag+attDist));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-modi-dist", preModifier+attDist));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-hTmomoT", headTag+","+preModifier+","+modifierTag));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-hTmo", headTag+","+preModifier));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-modiall", preModifier+","+modifierTag));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-modiall", preModifier));;
				}
				if(hL>5){
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hhTmoT-dist", preHead+","+headTag+","+modifierTag+attDist));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hmoT-dist", preHead+","+modifierTag+attDist));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hall-dist", preHead+","+headTag+attDist));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-h-dist", preHead+attDist));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hhTmoT", preHead+","+headTag+","+modifierTag));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hmoT", preHead+","+modifierTag));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hall", preHead+","+headTag));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-h", preHead));
				}
			}
			
			/**Unigram feature without dist info**/
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headword", headWord));;
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headtag", headTag));;
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifierword", modifierWord));;
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifiertag", modifierTag));;
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headwordtag", headWord+","+headTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifierwordtag", modifierWord+","+modifierTag));
			
			/**Unigram feature with dist info**/
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headword-dist", headWord+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headtag-dist", headTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifierword-dist", modifierWord+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifiertag-dist", modifierTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headwordtag-dist", headWord+","+headTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifierwordtag-dist", modifierWord+","+modifierTag+attDist));
			
			/****Bigram features without dist info******/
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "bigramword", headWord+","+modifierWord));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(),"bigramtag", headTag+","+modifierTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "bigramnametag",  headWord+","+headTag+","+modifierWord+","+modifierTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headallmoditag", headWord+","+headTag+","+modifierTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headallmodiword", headWord+","+headTag+","+modifierWord));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headtagmodiall", headTag+","+modifierWord+","+modifierTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headwordmodiall", headWord+","+modifierWord+","+modifierTag));
			
			/****Bigram features with dist info******/
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "bigramword-dist", headWord+","+modifierWord+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(),"bigramtag-dist", headTag+","+modifierTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "bigramnametag-dist",  headWord+","+headTag+","+modifierWord+","+modifierTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headallmoditag-dist", headWord+","+headTag+","+modifierTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headallmodiword-dist", headWord+","+headTag+","+modifierWord+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headtagmodiall-dist", headTag+","+modifierWord+","+modifierTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headwordmodiall-dist", headWord+","+modifierWord+","+modifierTag+attDist));
		
			
			
			String leftMinusTag = leftIndex>0? sent.get(leftIndex-1).getTag(): "STR";
			String rightPlusTag = rightIndex<sent.length()-1? sent.get(rightIndex+1).getTag():"END";
			String leftPlusTag = leftIndex<rightIndex-1? sent.get(leftIndex+1).getTag():"MID";
			String rightMinusTag = rightIndex-1 > leftIndex? sent.get(rightIndex-1).getTag():"MID";
			
			String leftMinusA = leftIndex>0? sent.get(leftIndex-1).getATag(): "STR";
			String rightPlusA = rightIndex<sent.length()-1? sent.get(rightIndex+1).getATag():"END";
			String leftPlusA = leftIndex<rightIndex-1? sent.get(leftIndex+1).getATag():"MID";
			String rightMinusA = rightIndex-1 > leftIndex?sent.get(rightIndex-1).getATag():"MID";
			
			//l-1,l,r,r+1
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-1-dist", leftMinusTag+","+leftTag+","+rightTag+","+rightPlusTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-2-dist", leftMinusTag+","+leftTag+","+rightTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-3-dist", leftMinusTag+","+rightTag+","+rightPlusTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-4-dist", leftMinusTag+","+leftTag+","+rightPlusTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-5-dist", leftTag+","+rightTag+","+rightPlusTag+attDist));
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-1", leftMinusTag+","+leftTag+","+rightTag+","+rightPlusTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-2", leftMinusTag+","+leftTag+","+rightTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-3", leftMinusTag+","+rightTag+","+rightPlusTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-4", leftMinusTag+","+leftTag+","+rightPlusTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-5", leftTag+","+rightTag+","+rightPlusTag));
			
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-1a-dist", leftMinusA+","+leftA+","+rightA+","+rightPlusA+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-2a-dist", leftMinusA+","+leftA+","+rightA+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-3a-dist", leftMinusA+","+rightA+","+rightA+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-4a-dist", leftMinusA+","+leftA+","+rightPlusA+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-5a-dist", leftA+","+rightA+","+rightPlusA+attDist));
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-1a", leftMinusA+","+leftA+","+rightA+","+rightPlusA));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-2a", leftMinusA+","+leftA+","+rightA));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-3a", leftMinusA+","+rightA+","+rightA));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-4a", leftMinusA+","+leftA+","+rightPlusA));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-5a", leftA+","+rightA+","+rightPlusA));
			
			//l,l+1,r-1,r
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-1-dist", leftTag+","+leftPlusTag+","+rightMinusTag+","+rightTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-2-dist", leftTag+","+rightMinusTag+","+rightTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-3-dist", leftTag+","+leftPlusTag+","+rightTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-4-dist", leftPlusTag+","+rightMinusTag+","+rightTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-5-dist", leftTag+","+leftPlusTag+","+rightMinusTag+attDist));
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-1", leftTag+","+leftPlusTag+","+rightMinusTag+","+rightTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-2", leftTag+","+rightMinusTag+","+rightTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-3", leftTag+","+leftPlusTag+","+rightTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-4", leftPlusTag+","+rightMinusTag+","+rightTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-5", leftTag+","+leftPlusTag+","+rightMinusTag));
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-1a-dist", leftA+","+leftPlusA+","+rightMinusA+","+rightA+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-2a-dist", leftA+","+rightMinusA+","+rightA+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-3a-dist", leftA+","+leftPlusA+","+rightA+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-4a-dist", leftPlusA+","+rightMinusA+","+rightA+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-5a-dist", leftA+","+leftPlusA+","+rightMinusA+attDist));
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-1a", leftA+","+leftPlusA+","+rightMinusA+","+rightA));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-2a", leftA+","+rightMinusA+","+rightA));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-3a", leftA+","+leftPlusA+","+rightA));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-4a", leftPlusA+","+rightMinusA+","+rightA));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-5a", leftA+","+leftPlusA+","+rightMinusA));
			
			//l-1,l,r-1,r
			//l,l+1,r,r+1
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-3-1-dist", leftMinusTag+","+leftTag+","+rightMinusTag+","+rightTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-3-1", leftMinusTag+","+leftTag+","+rightMinusTag+","+rightTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-3-1a-dist", leftMinusA+","+leftA+","+rightMinusA+","+rightA+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-3-1a", leftMinusA+","+leftA+","+rightMinusA+","+rightA));
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-4-1-dist", leftTag+","+leftPlusTag+","+rightTag+","+rightPlusTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-4-1", leftTag+","+leftPlusTag+","+rightTag+","+rightPlusTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-4-1a-dist", leftA+","+leftPlusA+","+rightA+","+rightPlusA+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-4-1a", leftA+","+leftPlusA+","+rightA+","+rightPlusA));
			
			
			for(int i=leftIndex+1;i<rightIndex;i++){
				featureList.add(this._param_g.toFeature(network,FEATYPE.inbetween.name(), "inbetween-1", leftTag+","+sent.get(i).getTag()+","+rightTag+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.inbetween.name(), "inbetween-2", leftTag+","+sent.get(i).getTag()+","+rightTag));
				featureList.add(this._param_g.toFeature(network,FEATYPE.inbetween.name(), "inbetween-3", leftA+","+sent.get(i).getATag()+","+rightA+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.inbetween.name(), "inbetween-4", leftA+","+sent.get(i).getATag()+","+rightA));
			}
			
			
			if(isPipe){
				String headEntity = sent.get(headIndex).getEntity();
				String modifierEntity = sent.get(modifierIndex).getEntity();
				featureList.add(this._param_g.toFeature(network,FEATYPE.pipe.name(),"PP-EHW",headEntity+":"+headWord));
				featureList.add(this._param_g.toFeature(network,FEATYPE.pipe.name(),"PP-EHT",headEntity+":"+headTag));
				featureList.add(this._param_g.toFeature(network,FEATYPE.pipe.name(),"PP-EMW",modifierEntity+":"+modifierWord));
				featureList.add(this._param_g.toFeature(network,FEATYPE.pipe.name(),"PP-EMT",modifierEntity+":"+modifierTag));
				
				
				featureList.add(this._param_g.toFeature(network,FEATYPE.pipe.name(),"PP-EEHWMW",headEntity+","+headWord+","+modifierEntity+","+modifierWord));
				featureList.add(this._param_g.toFeature(network,FEATYPE.pipe.name(),"PP-EEHTMT",headEntity+","+headTag+","+modifierEntity+","+modifierTag));
				featureList.add(this._param_g.toFeature(network,FEATYPE.pipe.name(),"PP-EEHWMW-dist",headEntity+","+headWord+","+modifierEntity+","+modifierWord+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.pipe.name(),"PP-EEHTMT-dist",headEntity+","+headTag+","+modifierEntity+","+modifierTag+attDist));
				
				featureList.add(this._param_g.toFeature(network,FEATYPE.pipe.name(),"PP-EHW-dist",headEntity+":"+headWord+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.pipe.name(),"PP-EHT-dist",headEntity+":"+headTag+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.pipe.name(),"PP-EMW-dist",modifierEntity+":"+modifierWord+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.pipe.name(),"PP-EMT-dist",modifierEntity+":"+modifierTag+attDist));
	

			}
		}
		
		
		
		ArrayList<Integer> finalList = new ArrayList<Integer>();
		for(int i=0;i<featureList.size();i++){
			if(featureList.get(i)!=-1)
				finalList.add( featureList.get(i) );
		}
		int[] features = new int[finalList.size()];
		for(int i=0;i<finalList.size();i++) features[i] = finalList.get(i);
		if(features.length==0) return FeatureArray.EMPTY;
		fa = new FeatureArray(features);
		return fa;
	}

	
	private void addLabeledFeatures(Network network, String att, boolean childFeatures, Sentence sent,int pos, String label, ArrayList<Integer> featureList){
		
		String w = sent.get(pos).getName();
		String wP = sent.get(pos).getTag();
		String wPm1 = pos > 0 ? sent.get(pos-1).getTag() : "STR";
		String wPp1 = pos < sent.length()-1 ? sent.get(pos+1).getTag() : "END";
		att+="&"+childFeatures;
		featureList.add(this._param_g.toFeature(network,FEATYPE.label.name(), "LABEL-dir", label+"&"+att));
		featureList.add(this._param_g.toFeature(network,FEATYPE.label.name(), "LABEL", label));
		for(int i = 0; i < 2; i++) {
			String suff = i < 1 ? "&"+att : "";
			suff = "&"+label+suff;
		 
		 	featureList.add(this._param_g.toFeature(network,FEATYPE.label.name(), "LABEL-W-WP-suff", w+" "+wP+suff));
		 	featureList.add(this._param_g.toFeature(network,FEATYPE.label.name(), "LABEL-WP-suff", wP+suff));
		 	featureList.add(this._param_g.toFeature(network,FEATYPE.label.name(), "LABEL-WM-WP-suff", wPm1+" "+wP+suff));
		 
		 	featureList.add(this._param_g.toFeature(network,FEATYPE.label.name(), "LABEL-WP-WPT-suff", wP+" "+wPp1+suff));
		 	featureList.add(this._param_g.toFeature(network,FEATYPE.label.name(), "LABEL-WM-WP-WPT-suff", wPm1+" "+wP+" "+wPp1+suff));
		 	featureList.add(this._param_g.toFeature(network,FEATYPE.label.name(), "LABEL-W-suff", w+suff));
		}
		
//		String currWord = sent.get(pos).getName();
//		String currTag = sent.get(pos).getTag();
//		String lw = pos>1? sent.get(pos-1).getName():"STR";
//		String lt = pos>1? sent.get(pos-1).getTag():"STR";
//		String rw = pos<sent.length()-1? sent.get(pos+1).getName():"END";
//		String rt = pos<sent.length()-1? sent.get(pos+1).getTag():"END";
//		String currEn = label;
//		featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "EW",  	currEn+":"+currWord));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "ET",	currEn+":"+currTag));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "ELW",	currEn+":"+lw));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "ELT",	currEn+":"+lt));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "ERW",	currEn+":"+rw));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "ERT",	currEn+":"+rt));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "ELT-T",	currEn+":"+lt+","+currTag));
		
		
	}

	private void addEntityFeatures(Network network, Sentence sent, int left, int right, String label, ArrayList<Integer> featureList){
		if(left==right){
			int pos = left;
			String currWord = sent.get(pos).getName();
			String currTag = sent.get(pos).getTag();
			String lw = pos>1? sent.get(pos-1).getName():"STR";
			String lt = pos>1? sent.get(pos-1).getTag():"STR";
			String rw = pos<sent.length()-1? sent.get(pos+1).getName():"END";
			String rt = pos<sent.length()-1? sent.get(pos+1).getTag():"END";
			String currEn = label;
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "EW",  	currEn+":"+currWord));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "ET",	currEn+":"+currTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "ELW",	currEn+":"+lw));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "ELT",	currEn+":"+lt));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "ERW",	currEn+":"+rw));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "ERT",	currEn+":"+rt));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "ELT-T",	currEn+":"+lt+","+currTag));
			for(int plen = 1;plen<=6;plen++){
				if(currWord.length()>=plen){
					String suff = currWord.substring(currWord.length()-plen, currWord.length());
					String pref = currWord.substring(0,plen);
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-PATTERN-SUFF-"+plen, currEn+":"+suff));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-PATTERN-PREF-"+plen, currEn+":"+pref));
				}
			}
		}
		if(!label.equals(NERLabel.get("O").getForm()) && left!=right){
			for(int i=left+1;i<=right;i++){
				int pos = i;
				String currWord = sent.get(pos).getName();
				String currTag = sent.get(pos).getTag();
				String lw = pos>1? sent.get(pos-1).getName():"STR";
				String lt = pos>1? sent.get(pos-1).getTag():"STR";
				String rw = pos<sent.length()-1? sent.get(pos+1).getName():"END";
				String rt = pos<sent.length()-1? sent.get(pos+1).getTag():"END";
				String currEn = label;
				String prevEntity = label;
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-prev-E",prevEntity+":"+currEn));
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "currW-prevE-currE",currWord+":"+prevEntity+":"+currEn));
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "prevW-prevE-currE",lw+":"+prevEntity+":"+currEn));
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "nextW-prevE-currE",rw+":"+prevEntity+":"+currEn));
				
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "currT-prevE-currE",currTag+":"+prevEntity+":"+currEn));
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "prevT-prevE-currE",lt+":"+prevEntity+":"+currEn));
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "nextT-prevE-currE",rt+":"+prevEntity+":"+currEn));
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "prevT-currT-prevE-currE",lt+":"+currTag+":"+prevEntity+":"+currEn));	
				//maybe add the pairwise features here
			}
			StringBuilder sb = new StringBuilder("");
			StringBuilder sbTags = new StringBuilder("");
			for(int i=left;i<=right;i++){
				String word = sent.get(i).getName();
				String tag = sent.get(i).getTag();
				sb.append("<sep>"+word);
				sbTags.append("<sep>"+tag);
				
			}
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-whole-words",label+":"+sb.toString()) );
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-whole-tags",label+":"+sbTags.toString()) );
			
			String lb = left>1? sent.get(left-1).getName():"STR";
			String lbt = left>1? sent.get(left-1).getTag():"STR";
			String rb = right<sent.length()-1? sent.get(right+1).getName():"END";
			String rbt = right<sent.length()-1? sent.get(right+1).getTag():"END";
			
			//boundary
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ELBW-1","B-"+label+":"+lb+":LEFT_1_BD"));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ELBT-1","B-"+label+":"+lbt+":LEFT_1_BD"));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ELBWT-1","B-"+label+":"+lb+","+lbt+":LEFT_1_BD"));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ERBW-1","I-"+label+":"+rb+":RIGHT_1_BD"));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ERBT-1","I-"+label+":"+rbt+":RIGHT_1_BD"));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ERBWT-1","I-"+label+":"+rb+","+rbt+":RIGHT_1_BD"));
			
			
			String lw = sent.get(left).getName();
			String rw = sent.get(right).getName();
			String lt = sent.get(left).getTag();
			String rt =sent.get(right).getTag();
//			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-LBW-RBW",child_1_type+":"+lb+":"+rb));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-LBT-RBT",child_1_type+":"+lbt+":"+rbt));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-LBWT-RBWT",child_1_type+":"+lb+":"+lbt+"-"+rb+":"+rbt));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-LBW-RBT",child_1_type+":"+lb+":"+rbt));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-LBT-RBW",child_1_type+":"+lbt+":"+rb));
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-IN-LW-RW",label+":"+lw+":"+rw));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-IN-LT-RT",label+":"+lt+":"+rt));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-IN-LWT-RWT",label+":"+lw+":"+lt+"-"+rw+":"+rt));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-IN-LW-RT",label+":"+lw+":"+rt));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-IN-LT-RW",label+":"+lt+":"+rw));
			
			
			featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-LBT-LT", "B-"+label+":"+lbt+","+lt));
			featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-LBT-LW", "B-"+label+":"+lbt+","+lw));
			featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-LBW-LT", "B-"+label+":"+lb+","+lt));
			featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-LBW-LW", "B-"+label+":"+lb+","+lw));
			
			featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-RBT-RT", "I-"+label+":"+rbt+","+rt));
			featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-RBT-RW", "I-"+label+":"+rbt+","+rw));
			featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-RBW-RT", "I-"+label+":"+rb+","+rt));
			featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-RBW-RW", "I-"+label+":"+rb+","+rw));
		}
		
		
		
	}
	
	
	private void addJointFeatures(Network network, Sentence sent, int left, int right, int direction, String label, ArrayList<Integer> featureList, String attDist){
		
		if(!label.equals(NERLabel.get("O").getForm()) && left!=right){
			int headIndex = -1;
			int modifierIndex = -1;
			if(direction==1){
				headIndex = left;
				modifierIndex = right;
			}
			else {
				headIndex = left;
				modifierIndex = right;
			}
			String headWord = sent.get(headIndex).getName();
			String headTag = sent.get(headIndex).getTag();
			String modifierWord = sent.get(modifierIndex).getName();
			String modifierTag = sent.get(modifierIndex).getTag();
				
			/****Bigram features without dist info******/
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "joint-bigramword", "JOINT:"+label+":"+headWord+":"+modifierWord));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"joint-bigramtag", "JOINT:"+label+":"+headTag+":"+modifierTag));
			
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "joint-bigramword-dist", "JOINT:"+label+":"+headWord+":"+modifierWord+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"joint-joint-bigramtag-dist", "JOINT:"+label+":"+headTag+":"+modifierTag+attDist));
			
			//add more dependency features here.
			String leftTag = sent.get(left).getTag();
			String rightTag = sent.get(right).getTag();
			for(int i=left+1;i<right;i++){
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "joint-inbetween-1", "JOINT:"+label+":"+leftTag+","+sent.get(i).getTag()+","+rightTag));
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "joint-inbetween-2", "JOINT:"+label+":"+leftTag+","+sent.get(i).getTag()+","+rightTag+attDist));
			}
		}
		
	}
}

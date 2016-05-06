package com.statnlp.dp.model.labelleddp;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class LDPFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 7274939836196010680L;

	public enum FEATYPE {unigram, bigram,contextual, inbetween, prefix,pipe};
	protected boolean isPipe;
	
	public LDPFeatureManager(GlobalNetworkParam param_g, boolean isPipe) {
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
		
		LDPInstance di = (LDPInstance)network.getInstance();
		int instanceId = di.getInstanceId();
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
		
		
		//if incomplete span or complete but with spanlen is 2
		if(completeness==0){
			String label = DepLabel.LABELS_INDEX.get(parentArr[4]).getForm();
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
			
			
			/***Prefix 5 gram features*********/
			if(headWord.length()>5 || modifierWord.length()>5){
				int hL = headWord.length();
				int mL = modifierWord.length();
				String preHead = hL>5? headWord.substring(0,5):headWord;
				String preModifier = mL>5?modifierWord.substring(0,5):modifierWord;
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "bigram-prefix-all-dist", preHead+","+headTag+","+preModifier+","+modifierTag+","+label+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "bigram-prefix-word-dist", preHead+","+preModifier+","+label+attDist));;
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "bigram-prefix-all", preHead+","+headTag+","+preModifier+","+modifierTag+","+label));;
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "bigram-prefix-word", preHead+","+preModifier+","+label));;
				if(mL>5){
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-hTmomoT-dist", headTag+","+preModifier+","+modifierTag+","+label+attDist));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-hTmo-dist", headTag+","+preModifier+","+label+attDist));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-modiall-dist", preModifier+","+modifierTag+","+label+attDist));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-modi-dist", preModifier+","+label+attDist));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-hTmomoT", headTag+","+preModifier+","+modifierTag+","+label));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-hTmo", headTag+","+preModifier+","+label));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-modiall", preModifier+","+modifierTag+","+label));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-modiall", preModifier+","+label));;
				}
				if(hL>5){
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hhTmoT-dist", preHead+","+headTag+","+modifierTag+","+label+attDist));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hmoT-dist", preHead+","+modifierTag+","+label+attDist));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hall-dist", preHead+","+headTag+","+label+attDist));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-h-dist", preHead+","+label+attDist));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hhTmoT", preHead+","+headTag+","+modifierTag+","+label));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hmoT", preHead+","+modifierTag+","+label));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hall", preHead+","+headTag+","+label));;
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-h", preHead+","+label));
				}
			}
			
			/**Unigram feature without dist info**/
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headword", headWord+","+label));;
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headtag", headTag+","+label));;
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifierword", modifierWord+","+label));;
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifiertag", modifierTag+","+label));;
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headwordtag", headWord+","+headTag+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifierwordtag", modifierWord+","+modifierTag+","+label));
			
			/**Unigram feature with dist info**/
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headword-dist", headWord+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headtag-dist", headTag+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifierword-dist", modifierWord+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifiertag-dist", modifierTag+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headwordtag-dist", headWord+","+headTag+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifierwordtag-dist", modifierWord+","+modifierTag+","+label+attDist));
			
			/****Bigram features without dist info******/
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "bigramword", headWord+","+modifierWord+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(),"bigramtag", headTag+","+modifierTag+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "bigramnametag",  headWord+","+headTag+","+modifierWord+","+modifierTag+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headallmoditag", headWord+","+headTag+","+modifierTag+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headallmodiword", headWord+","+headTag+","+modifierWord+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headtagmodiall", headTag+","+modifierWord+","+modifierTag+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headwordmodiall", headWord+","+modifierWord+","+modifierTag+","+label));
			
			/****Bigram features with dist info******/
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "bigramword-dist", headWord+","+modifierWord+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(),"bigramtag-dist", headTag+","+modifierTag+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "bigramnametag-dist",  headWord+","+headTag+","+modifierWord+","+modifierTag+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headallmoditag-dist", headWord+","+headTag+","+modifierTag+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headallmodiword-dist", headWord+","+headTag+","+modifierWord+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headtagmodiall-dist", headTag+","+modifierWord+","+modifierTag+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headwordmodiall-dist", headWord+","+modifierWord+","+modifierTag+","+label+attDist));
		
			
			
			String leftMinusTag = leftIndex>0? sent.get(leftIndex-1).getTag(): "STR";
			String rightPlusTag = rightIndex<sent.length()-1? sent.get(rightIndex+1).getTag():"END";
			String leftPlusTag = leftIndex<rightIndex-1? sent.get(leftIndex+1).getTag():"MID";
			String rightMinusTag = rightIndex-1 > leftIndex? sent.get(rightIndex-1).getTag():"MID";
			
			String leftMinusA = leftIndex>0? sent.get(leftIndex-1).getATag(): "STR";
			String rightPlusA = rightIndex<sent.length()-1? sent.get(rightIndex+1).getATag():"END";
			String leftPlusA = leftIndex<rightIndex-1? sent.get(leftIndex+1).getATag():"MID";
			String rightMinusA = rightIndex-1 > leftIndex?sent.get(rightIndex-1).getATag():"MID";
			
			//l-1,l,r,r+1
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-1-dist", leftMinusTag+","+leftTag+","+rightTag+","+rightPlusTag+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-2-dist", leftMinusTag+","+leftTag+","+rightTag+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-3-dist", leftMinusTag+","+rightTag+","+rightPlusTag+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-4-dist", leftMinusTag+","+leftTag+","+rightPlusTag+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-5-dist", leftTag+","+rightTag+","+rightPlusTag+","+label+attDist));
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-1", leftMinusTag+","+leftTag+","+rightTag+","+rightPlusTag+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-2", leftMinusTag+","+leftTag+","+rightTag+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-3", leftMinusTag+","+rightTag+","+rightPlusTag+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-4", leftMinusTag+","+leftTag+","+rightPlusTag+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-5", leftTag+","+rightTag+","+rightPlusTag+","+label));
			
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-1a-dist", leftMinusA+","+leftA+","+rightA+","+rightPlusA+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-2a-dist", leftMinusA+","+leftA+","+rightA+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-3a-dist", leftMinusA+","+rightA+","+rightA+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-4a-dist", leftMinusA+","+leftA+","+rightPlusA+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-5a-dist", leftA+","+rightA+","+rightPlusA+","+label+attDist));
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-1a", leftMinusA+","+leftA+","+rightA+","+rightPlusA+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-2a", leftMinusA+","+leftA+","+rightA+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-3a", leftMinusA+","+rightA+","+rightA+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-4a", leftMinusA+","+leftA+","+rightPlusA+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-5a", leftA+","+rightA+","+rightPlusA+","+label));
			
			//l,l+1,r-1,r
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-1-dist", leftTag+","+leftPlusTag+","+rightMinusTag+","+rightTag+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-2-dist", leftTag+","+rightMinusTag+","+rightTag+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-3-dist", leftTag+","+leftPlusTag+","+rightTag+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-4-dist", leftPlusTag+","+rightMinusTag+","+rightTag+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-5-dist", leftTag+","+leftPlusTag+","+rightMinusTag+","+label+attDist));
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-1", leftTag+","+leftPlusTag+","+rightMinusTag+","+rightTag+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-2", leftTag+","+rightMinusTag+","+rightTag+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-3", leftTag+","+leftPlusTag+","+rightTag+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-4", leftPlusTag+","+rightMinusTag+","+rightTag+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-5", leftTag+","+leftPlusTag+","+rightMinusTag+","+label));
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-1a-dist", leftA+","+leftPlusA+","+rightMinusA+","+rightA+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-2a-dist", leftA+","+rightMinusA+","+rightA+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-3a-dist", leftA+","+leftPlusA+","+rightA+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-4a-dist", leftPlusA+","+rightMinusA+","+rightA+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-5a-dist", leftA+","+leftPlusA+","+rightMinusA+","+label+attDist));
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-1a", leftA+","+leftPlusA+","+rightMinusA+","+rightA+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-2a", leftA+","+rightMinusA+","+rightA+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-3a", leftA+","+leftPlusA+","+rightA+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-4a", leftPlusA+","+rightMinusA+","+rightA+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-5a", leftA+","+leftPlusA+","+rightMinusA+","+label));
			
			//l-1,l,r-1,r
			//l,l+1,r,r+1
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-3-1-dist", leftMinusTag+","+leftTag+","+rightMinusTag+","+rightTag+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-3-1", leftMinusTag+","+leftTag+","+rightMinusTag+","+rightTag+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-3-1a-dist", leftMinusA+","+leftA+","+rightMinusA+","+rightA+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-3-1a", leftMinusA+","+leftA+","+rightMinusA+","+rightA+","+label));
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-4-1-dist", leftTag+","+leftPlusTag+","+rightTag+","+rightPlusTag+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-4-1", leftTag+","+leftPlusTag+","+rightTag+","+rightPlusTag+","+label));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-4-1a-dist", leftA+","+leftPlusA+","+rightA+","+rightPlusA+","+label+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-4-1a", leftA+","+leftPlusA+","+rightA+","+rightPlusA+","+label));
			
			
			for(int i=leftIndex+1;i<rightIndex;i++){
				featureList.add(this._param_g.toFeature(network,FEATYPE.inbetween.name(), "inbetween-1", leftTag+","+sent.get(i).getTag()+","+rightTag+","+label+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.inbetween.name(), "inbetween-2", leftTag+","+sent.get(i).getTag()+","+rightTag+","+label));
				featureList.add(this._param_g.toFeature(network,FEATYPE.inbetween.name(), "inbetween-3", leftA+","+sent.get(i).getATag()+","+rightA+","+label+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.inbetween.name(), "inbetween-4", leftA+","+sent.get(i).getATag()+","+rightA+","+label));
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

	
}

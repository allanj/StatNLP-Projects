package com.statnlp.projects.dep.model.bruteforce;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class BFFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 376931974939202432L;

	public enum FEATYPE {local,entity, unigram, bigram, prefix, contextual, inbetween, joint,cheat};
	
	public static boolean CHEAT = true;
	
	public BFFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
		
	}
	//
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		// TODO Auto-generated method stub
		BFInstance inst = ((BFInstance)network.getInstance());
		//int instanceId = inst.getInstanceId();
		Sentence sent = inst.getInput();
//		long node = network.getNode(parent_k);
//		int[] nodeArr = NetworkIDMapper.toHybridNodeArray(node);
		
		if(CHEAT){
			return new FeatureArray(new int[]{this._param_g.toFeature(network,FEATYPE.cheat.name(), "cheat", "nothing")});
		}
		
		FeatureArray fa = null;
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		
		if(children_k.length==2)
			addDepFeatures(featureList, network, children_k, sent);
		
		/*********Pairwise features********/
		ArrayList<Integer> finalList = new ArrayList<Integer>();
		for(int i=0;i<featureList.size();i++){
			if(featureList.get(i)!=-1)
				finalList.add(featureList.get(i));
		}
		int[] features = new int[finalList.size()];
		for(int i=0;i<finalList.size();i++) features[i] = finalList.get(i);
		if(features.length==0) return FeatureArray.EMPTY;
		fa = new FeatureArray(features);
		
		return fa;
	}

	private void addDepFeatures(ArrayList<Integer> featureList, Network network, int[] children_k, Sentence sent){
		
		int[] headArr = NetworkIDMapper.toHybridNodeArray(network.getNode(children_k[1]));
		
		int headIndex = headArr[1]; 
		int modifierIndex = headArr[0];
		
		if(headIndex>(sent.length()-1)) return;
		
		int leftIndex = modifierIndex < headIndex? modifierIndex:headIndex;
		int rightIndex = modifierIndex < headIndex? headIndex:modifierIndex;
		int direction = modifierIndex < headIndex? 0:1;
		String leftTag = sent.get(leftIndex).getTag();
		String rightTag = sent.get(rightIndex).getTag();
		String leftA = leftTag.substring(0, 1);
		String rightA = rightTag.substring(0, 1);
		
		int dist = Math.abs(rightIndex-leftIndex);
		String att = direction==1? "RA":"LA";
		String distBool = "0";
		if(dist > 1)  distBool = "1";
		if(dist > 2)  distBool = "2";
		if(dist > 3)  distBool = "3";
		if(dist > 4)  distBool = "4";
		if(dist > 5)  distBool = "5";
		if(dist > 10) distBool = "10";
		String attDist = "&"+att+"&"+distBool;
		
		
		String headWord = sent.get(headIndex).getName();
		String headTag = sent.get(headIndex).getTag();
		String modifierWord = sent.get(modifierIndex).getName();
		String modifierTag = sent.get(modifierIndex).getTag();
		
		
		if(children_k.length<2) return;
		
		
			
		//add joint features.
		if(children_k.length!=2){
			throw new RuntimeException("no entity node for the incomplete span?");
		}
		
		if(headWord.length()>5 || modifierWord.length()>5){
			int hL = headWord.length();
			int mL = modifierWord.length();
			String preHead = hL>5? headWord.substring(0,5):headWord;
			String preModifier = mL>5?modifierWord.substring(0,5):modifierWord;
			featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "bigram-prefix-all-dist", preHead+","+headTag+","+preModifier+","+modifierTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "bigram-prefix-word-dist", preHead+","+preModifier+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "bigram-prefix-all", preHead+","+headTag+","+preModifier+","+modifierTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "bigram-prefix-word", preHead+","+preModifier));
			if(mL>5){
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-hTmomoT-dist", headTag+","+preModifier+","+modifierTag+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-hTmo-dist", headTag+","+preModifier+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-modiall-dist", preModifier+","+modifierTag+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-modi-dist", preModifier+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-hTmomoT", headTag+","+preModifier+","+modifierTag));
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-hTmo", headTag+","+preModifier));
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-modiall", preModifier+","+modifierTag));
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-modiall", preModifier));
			}
			if(hL>5){
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hhTmoT-dist", preHead+","+headTag+","+modifierTag+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hmoT-dist", preHead+","+modifierTag+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hall-dist", preHead+","+headTag+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-h-dist", preHead+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hhTmoT", preHead+","+headTag+","+modifierTag));
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hmoT", preHead+","+modifierTag));
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hall", preHead+","+headTag));
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-h", preHead));
			}
		}
		
		/**Unigram feature without dist info**/
		featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headword", headWord));
		featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headtag", headTag));
		featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifierword", modifierWord));
		featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifiertag", modifierTag));
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
			
			
	}
}

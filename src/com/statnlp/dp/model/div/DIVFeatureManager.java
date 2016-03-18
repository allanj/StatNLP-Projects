package com.statnlp.dp.model.div;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.DependInstance;
import com.statnlp.dp.utils.DPConfig;
import com.statnlp.dp.utils.Extractor;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class DIVFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 7274939836196010680L;

	private enum FEATYPE {unigram, bigram,contextual, inbetween,entity,prefix,joint};
	private String[] types;
	
	public static String PARENT_IS = DPConfig.PARENT_IS;
	public static String OE = DPConfig.OE;
	public static String ONE = DPConfig.ONE;
	
	public static String O_TYPE = DPConfig.O_TYPE;
	public static String E_B_PREFIX = DPConfig.E_B_PREFIX;
	public static String E_I_PREFIX = DPConfig.E_I_PREFIX;
	
	
	public DIVFeatureManager(GlobalNetworkParam param_g, String[] types) {
		super(param_g);
		this.types = types;
	}
	
	
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		
		FeatureArray fa = null;
		
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		
		DependInstance di = (DependInstance)network.getInstance();
		Sentence sent  = di.getInput();
		long parent = network.getNode(parent_k);
		
		int[] parentArr = NetworkIDMapper.toHybridNodeArray(parent);
		
		int leftIndex = parentArr[0] - parentArr[1];
		int rightIndex = parentArr[0];
		int direction = parentArr[3];
		int completeness = parentArr[2];
		String type = null;
		if(parentArr[4]==2*types.length)
			type = PARENT_IS+"null";
		else if(parentArr[4]>types.length-1){
			type = PARENT_IS+types[parentArr[4]-types.length];
		}else{
			type = types[parentArr[4]];
		}
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
		
		int headIndex = direction==1? leftIndex:rightIndex;
		int modifierIndex = direction==1? rightIndex:leftIndex;
		String headWord = sent.get(headIndex).getName();
		String headTag = sent.get(headIndex).getTag();
		String modifierWord = sent.get(modifierIndex).getName();
		String modifierTag = sent.get(modifierIndex).getTag();
		
		/*****Simple Entity Features******/
		//split it out and test. while leftIndex==rightIndex
		
		String pa_type = type.startsWith(PARENT_IS)?type.split(":")[1]:type;
		if(children_k.length==0) return FeatureArray.EMPTY;
		long child_1 = network.getNode(children_k[0]);
		int[] childArr_1 = NetworkIDMapper.toHybridNodeArray(child_1);
		String child_1_type = null;
		if(childArr_1[4]==2*types.length)
			child_1_type = "pae:null";
		else if(childArr_1[4]>types.length-1){
			child_1_type = "pae:"+types[childArr_1[4]-types.length];
		}else{
			child_1_type = types[childArr_1[4]];
		}
		String[] dirs = {"LEFTDIR","RIGHTDIR"};
		
		
		if(type.startsWith(PARENT_IS)){
			if(children_k.length!=1)
				throw new RuntimeException("parent is general span, but have "+children_k.length+" children?");

			
			if((pa_type.equals(OE)||pa_type.equals("null")) && !child_1_type.equals(OE) && !child_1_type.equals(ONE)
					&& (leftIndex==rightIndex || (leftIndex!=rightIndex && completeness==0))){
				
				
				//when comes in must be incomplete
				if(leftIndex!=rightIndex && completeness!=0){
					System.err.println(sent.toString());
					System.err.println("Current Parent:"+leftIndex+","+rightIndex+","+completeness+","+att);
					throw new RuntimeException("Something must be wrong, outsidest entity or non-entity must be an incomplete span");
				}
			}
			
		}
		
		/****End of Entity features*******/
		if(completeness==0 && type.startsWith(PARENT_IS)){
			String currentType = "";
			if(!child_1_type.equals(OE) &&!child_1_type.equals(ONE)){
				currentType = child_1_type+":";
			}
			
			
			if(headWord.length()>5 || modifierWord.length()>5){
				int hL = headWord.length();
				int mL = modifierWord.length();
				String preHead = hL>5? headWord.substring(0,5):headWord;
				String preModifier = mL>5?modifierWord.substring(0,5):modifierWord;
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "bigram-prefix-all-dist", currentType+preHead+","+headTag+","+preModifier+","+modifierTag+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "bigram-prefix-word-dist", currentType+preHead+","+preModifier+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "bigram-prefix-all", currentType+preHead+","+headTag+","+preModifier+","+modifierTag));
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "bigram-prefix-word", currentType+preHead+","+preModifier));
				if(mL>5){
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-hTmomoT-dist", currentType+headTag+","+preModifier+","+modifierTag+attDist));
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-hTmo-dist", currentType+headTag+","+preModifier+attDist));
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-modiall-dist", currentType+preModifier+","+modifierTag+attDist));
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-modi-dist", currentType+preModifier+attDist));
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-hTmomoT", currentType+headTag+","+preModifier+","+modifierTag));
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-hTmo", currentType+headTag+","+preModifier));
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-modiall", currentType+preModifier+","+modifierTag));
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "modi-prefix-modiall", currentType+preModifier));
				}
				if(hL>5){
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hhTmoT-dist", currentType+preHead+","+headTag+","+modifierTag+attDist));
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hmoT-dist", currentType+preHead+","+modifierTag+attDist));
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hall-dist", currentType+preHead+","+headTag+attDist));
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-h-dist", currentType+preHead+attDist));
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hhTmoT", currentType+preHead+","+headTag+","+modifierTag));
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hmoT", currentType+preHead+","+modifierTag));
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-hall", currentType+preHead+","+headTag));
					featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name(), "head-prefix-h", currentType+preHead));
				}
			}
			
			/**Unigram feature without dist info**/
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headword", currentType+headWord));
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headtag", currentType+headTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifierword", currentType+modifierWord));
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifiertag", currentType+modifierTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headwordtag", currentType+headWord+","+headTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifierwordtag", currentType+modifierWord+","+modifierTag));
			
			/**Unigram feature with dist info**/
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headword-dist", currentType+headWord+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headtag-dist", currentType+headTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifierword-dist", currentType+modifierWord+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifiertag-dist", currentType+modifierTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "headwordtag-dist", currentType+headWord+","+headTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.unigram.name(), "modifierwordtag-dist", currentType+modifierWord+","+modifierTag+attDist));
			
			/****Bigram features without dist info******/
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "bigramword", currentType+headWord+","+modifierWord));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(),"bigramtag", currentType+headTag+","+modifierTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "bigramnametag",  currentType+headWord+","+headTag+","+modifierWord+","+modifierTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headallmoditag", currentType+headWord+","+headTag+","+modifierTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headallmodiword", currentType+headWord+","+headTag+","+modifierWord));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headtagmodiall", currentType+headTag+","+modifierWord+","+modifierTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headwordmodiall", currentType+headWord+","+modifierWord+","+modifierTag));
			
			/****Bigram features with dist info******/
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "bigramword-dist", currentType+headWord+","+modifierWord+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(),"bigramtag-dist", currentType+headTag+","+modifierTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "bigramnametag-dist",  currentType+headWord+","+headTag+","+modifierWord+","+modifierTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headallmoditag-dist", currentType+headWord+","+headTag+","+modifierTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headallmodiword-dist", currentType+headWord+","+headTag+","+modifierWord+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headtagmodiall-dist", currentType+headTag+","+modifierWord+","+modifierTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.bigram.name(), "headwordmodiall-dist", currentType+headWord+","+modifierWord+","+modifierTag+attDist));
		
			
			
			String leftMinusTag = leftIndex>0? sent.get(leftIndex-1).getTag(): "STR";
			String rightPlusTag = rightIndex<sent.length()-1? sent.get(rightIndex+1).getTag():"END";
			String leftPlusTag = leftIndex<rightIndex-1? sent.get(leftIndex+1).getTag():"MID";
			String rightMinusTag = rightIndex-1 > leftIndex? sent.get(rightIndex-1).getTag():"MID";
			
			String leftMinusA = leftIndex>0? sent.get(leftIndex-1).getATag(): "STR";
			String rightPlusA = rightIndex<sent.length()-1? sent.get(rightIndex+1).getATag():"END";
			String leftPlusA = leftIndex<rightIndex-1? sent.get(leftIndex+1).getATag():"MID";
			String rightMinusA = rightIndex-1 > leftIndex?sent.get(rightIndex-1).getATag():"MID";
			
			
			//l-1,l,r,r+1
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-1-dist", currentType+leftMinusTag+","+leftTag+","+rightTag+","+rightPlusTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-2-dist", currentType+leftMinusTag+","+leftTag+","+rightTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-3-dist", currentType+leftMinusTag+","+rightTag+","+rightPlusTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-4-dist", currentType+leftMinusTag+","+leftTag+","+rightPlusTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-5-dist", currentType+leftTag+","+rightTag+","+rightPlusTag+attDist));
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-1", currentType+leftMinusTag+","+leftTag+","+rightTag+","+rightPlusTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-2", currentType+leftMinusTag+","+leftTag+","+rightTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-3", currentType+leftMinusTag+","+rightTag+","+rightPlusTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-4", currentType+leftMinusTag+","+leftTag+","+rightPlusTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-5", currentType+leftTag+","+rightTag+","+rightPlusTag));
			
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-1a-dist", currentType+leftMinusA+","+leftA+","+rightA+","+rightPlusA+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-2a-dist", currentType+leftMinusA+","+leftA+","+rightA+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-3a-dist", currentType+leftMinusA+","+rightA+","+rightA+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-4a-dist", currentType+leftMinusA+","+leftA+","+rightPlusA+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-5a-dist", currentType+leftA+","+rightA+","+rightPlusA+attDist));
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-1a", currentType+leftMinusA+","+leftA+","+rightA+","+rightPlusA));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-2a", currentType+leftMinusA+","+leftA+","+rightA));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-3a", currentType+leftMinusA+","+rightA+","+rightA));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-4a", currentType+leftMinusA+","+leftA+","+rightPlusA));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-1-5a", currentType+leftA+","+rightA+","+rightPlusA));
			
			//l,l+1,r-1,r
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-1-dist", currentType+leftTag+","+leftPlusTag+","+rightMinusTag+","+rightTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-2-dist", currentType+leftTag+","+rightMinusTag+","+rightTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-3-dist", currentType+leftTag+","+leftPlusTag+","+rightTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-4-dist", currentType+leftPlusTag+","+rightMinusTag+","+rightTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-5-dist", currentType+leftTag+","+leftPlusTag+","+rightMinusTag+attDist));
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-1", currentType+leftTag+","+leftPlusTag+","+rightMinusTag+","+rightTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-2", currentType+leftTag+","+rightMinusTag+","+rightTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-3", currentType+leftTag+","+leftPlusTag+","+rightTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-4", currentType+leftPlusTag+","+rightMinusTag+","+rightTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-5", currentType+leftTag+","+leftPlusTag+","+rightMinusTag));
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-1a-dist", currentType+leftA+","+leftPlusA+","+rightMinusA+","+rightA+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-2a-dist", currentType+leftA+","+rightMinusA+","+rightA+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-3a-dist", currentType+leftA+","+leftPlusA+","+rightA+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-4a-dist", currentType+leftPlusA+","+rightMinusA+","+rightA+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-5a-dist", currentType+leftA+","+leftPlusA+","+rightMinusA+attDist));
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-1a", currentType+leftA+","+leftPlusA+","+rightMinusA+","+rightA));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-2a", currentType+leftA+","+rightMinusA+","+rightA));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-3a", currentType+leftA+","+leftPlusA+","+rightA));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-4a", currentType+leftPlusA+","+rightMinusA+","+rightA));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-2-5a", currentType+leftA+","+leftPlusA+","+rightMinusA));
			
			//l-1,l,r-1,r
			//l,l+1,r,r+1
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-3-1-dist", currentType+leftMinusTag+","+leftTag+","+rightMinusTag+","+rightTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-3-1", currentType+leftMinusTag+","+leftTag+","+rightMinusTag+","+rightTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-3-1a-dist", currentType+leftMinusA+","+leftA+","+rightMinusA+","+rightA+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-3-1a", currentType+leftMinusA+","+leftA+","+rightMinusA+","+rightA));
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-4-1-dist", currentType+leftTag+","+leftPlusTag+","+rightTag+","+rightPlusTag+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-4-1", currentType+leftTag+","+leftPlusTag+","+rightTag+","+rightPlusTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-4-1a-dist", currentType+leftA+","+leftPlusA+","+rightA+","+rightPlusA+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.contextual.name(), "contextual-4-1a", currentType+leftA+","+leftPlusA+","+rightA+","+rightPlusA));
			
			
			for(int i=leftIndex+1;i<rightIndex;i++){
				featureList.add(this._param_g.toFeature(network,FEATYPE.inbetween.name(), "inbetween-1", currentType+leftTag+","+sent.get(i).getTag()+","+rightTag+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.inbetween.name(), "inbetween-2", currentType+leftTag+","+sent.get(i).getTag()+","+rightTag));
				featureList.add(this._param_g.toFeature(network,FEATYPE.inbetween.name(), "inbetween-3", currentType+leftA+","+sent.get(i).getATag()+","+rightA+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.inbetween.name(), "inbetween-4", currentType+leftA+","+sent.get(i).getATag()+","+rightA));
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
	
	
	private String wordShape(String word){
		return Extractor.wordShapeOf(word);
	}
	
	
	
}

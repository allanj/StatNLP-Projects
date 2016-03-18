package com.statnlp.dp.model;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.DependInstance;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class ADPFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 7274939836196010680L;

	private enum FEATYPE {unigram, bigram,contextual, inbetween,entity,prefix,joint};
	private String[] types;
	
	
	public ADPFeatureManager(GlobalNetworkParam param_g, String[] types, boolean nested) {
		super(param_g);
		this.types = types;
	}
	
	
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		
		FeatureArray fa = null;
		
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		
		DependInstance di = (DependInstance)network.getInstance();
//		int instanceId = di.getInstanceId();
		Sentence sent  = di.getInput();
		long parent = network.getNode(parent_k);
		
		int[] parentArr = NetworkIDMapper.toHybridNodeArray(parent);
		
		int leftIndex = parentArr[0] - parentArr[1];
		int rightIndex = parentArr[0];
		int direction = parentArr[3];
		int completeness = parentArr[2];
		String type = types[parentArr[4]];
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
		
		headIndex = direction==1? leftIndex:rightIndex;
		modifierIndex = direction==1? rightIndex:leftIndex;
		headWord = sent.get(headIndex).getName();
		headTag = sent.get(headIndex).getTag();
		modifierWord = sent.get(modifierIndex).getName();
		modifierTag = sent.get(modifierIndex).getTag();
		
		/*****Simple Entity Features******/
		//split it out and test. while leftIndex==rightIndex
		if(!type.equals("EMPTY")){	
			StringBuilder sb = new StringBuilder(sent.get(leftIndex).getName() );
			StringBuilder sbAlls = new StringBuilder(sent.get(leftIndex).getName()+","+sent.get(leftIndex).getTag() );
			StringBuilder sbTags = new StringBuilder(sent.get(leftIndex).getTag() );
			for(int i=leftIndex+1;i<=rightIndex;i++){				
				sb.append(","+sent.get(i).getName() );
				sbAlls.append(","+sent.get(i).getName()+","+sent.get(i).getTag() ) ;
				sbTags.append(","+sent.get(i).getTag() );
			}
			//span entity words && tags feature
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"EWs",type+":"+sb.toString()) );
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ETs",type+":"+sbTags.toString()) );
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"EWTs",type+":"+sb.toString()+":"+sbTags.toString()) );
			
			//boundary features
			String lw = sent.get(leftIndex).getName();
			String lt = sent.get(leftIndex).getTag();
			String rw = sent.get(rightIndex).getName();
			String rt = sent.get(rightIndex).getTag();
			String lb = leftIndex>0? sent.get(leftIndex-1).getName():"STR";
			String lbt = leftIndex>0? sent.get(leftIndex-1).getTag():"STR";
			String rb = rightIndex<sent.length()-1? sent.get(rightIndex+1).getName():"END";
			String rbt = rightIndex<sent.length()-1? sent.get(rightIndex+1).getTag():"END";
			
			//enhanced boundary
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ELBW-1",type+":"+lb+","+lw));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ELBT-1",type+":"+lbt+","+lt));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ELBWT-1",type+":"+lb+","+lbt+","+lw+","+lt));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ERBW-1",type+":"+rw+","+rb));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ERBT-1",type+":"+rt+","+rbt));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ERBWT-1",type+":"+rw+","+rt+","+rb+","+rbt));
//			
//			
//			//bigram
//			for(int i=leftIndex-1;i<=rightIndex;i++){				
//				String blw = i==leftIndex-1? "ESTR":sent.get(i).getName();
//				String brw = i==rightIndex? "EEND":sent.get(i+1).getName();
//				String blt = i==leftIndex-1? "ESTR":sent.get(i).getTag();
//				String brt = i==rightIndex? "EEND":sent.get(i+1).getTag();
//				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-BI-W",type+":"+blw+","+brw));
//				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-BI-T",type+":"+blt+","+brt));
//				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-BI-WT",type+":"+blw+","+brw+","+blt+","+brt));
//			}
//			//trigram
//			for(int i=leftIndex-2;i<=rightIndex;i++){				
//				String tlw = i==leftIndex-2? "ESTR1":i==leftIndex-1? "ESTR":sent.get(i).getName();
//				String tmw = i==leftIndex-2? "ESTR": i==rightIndex? "EEND":sent.get(i+1).getName();
//				String trw = i==rightIndex-1? "EEND": i==rightIndex? "EEND1":sent.get(i+2).getName();
//				String tlt = i==leftIndex-2? "ESTR1":i==leftIndex-1? "ESTR":sent.get(i).getTag();
//				String tmt = i==leftIndex-2? "ESTR": i==rightIndex? "EEND":sent.get(i+1).getTag();
//				String trt = i==rightIndex-1? "EEND": i==rightIndex? "EEND1":sent.get(i+2).getTag();
//				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-TRI-W",type+":"+tlw+","+tmw+","+trw));
//				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-TRI-T",type+":"+tlt+","+tmt+","+trt));
//				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-TRI-WT",type+":"+tlw+","+tmw+","+trw+","+tlt+","+tmt+","+trt));
//			}
//			//four-gram
//			for(int i=leftIndex-3;i<=rightIndex;i++){				
//				String flw = i==leftIndex-3? "ESTR2":i==leftIndex-2? "ESTR1":i==leftIndex-1?"ESTR":sent.get(i).getName();
//				String fm1w = i==leftIndex-3? "ESTR1": i==leftIndex-2? "ESTR":i==rightIndex? "EEND":sent.get(i+1).getName();
//				String fm2w = i==leftIndex-3? "ESTR": i==rightIndex-1? "EEND":i==rightIndex?"EEND1":sent.get(i+2).getName();
//				String frw = i==rightIndex-2? "EEND": i==rightIndex-1? "EEND1":i==rightIndex? "EEND2":sent.get(i+3).getName();
//				
//				String flt = i==leftIndex-3? "ESTR2":i==leftIndex-2? "ESTR1":i==leftIndex-1?"ESTR":sent.get(i).getTag();
//				String fm1t = i==leftIndex-3? "ESTR1": i==leftIndex-2? "ESTR":i==rightIndex? "EEND":sent.get(i+1).getTag();
//				String fm2t = i==leftIndex-3? "ESTR": i==rightIndex-1? "EEND":i==rightIndex?"EEND1":sent.get(i+2).getTag();
//				String frt = i==rightIndex-2? "EEND": i==rightIndex-1? "EEND1":i==rightIndex? "EEND2":sent.get(i+3).getTag();
//				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-FG-W",type+":"+flw+","+fm1w+","+fm2w+","+frw));
//				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-FG-T",type+":"+flt+","+fm1t+","+fm2t+","+frt));
//				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-FG-WT",type+":"+flw+","+fm1w+","+fm2w+","+frw+","+flt+","+fm1t+","+fm2t+","+frt));
//			}
//			//unigram: upgrade the dependency somehow
//			for(int i=leftIndex;i<=rightIndex;i++){	
//				String word = sent.get(i).getName();
//				String tag = sent.get(i).getTag();
//				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-UN-W", type+":"+word));
//				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-UN-T", type+":"+tag));
//				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-UN-WT", type+":"+word+","+tag));
//			}
			
			
			
			/****Joint Features, which should be important**/
			if(completeness==0){
				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"EHW",type+":"+headWord));
				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"EHT",type+":"+headTag));
				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"EHWT",type+":"+headWord+","+headTag));
				
				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"EMW",type+":"+modifierWord));
				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"EMT",type+":"+modifierTag));
				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"EMWT",type+":"+modifierWord+","+modifierTag));
				
				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"EHMW",type+":"+headWord+","+modifierWord));
				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"EHMT",type+":"+headTag+","+modifierTag));
				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"EHMWT",type+":"+headWord+","+headTag+","+modifierWord+","+modifierTag));
				
				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"EHW-dist",type+":"+headWord+":"+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"EHT-dist",type+":"+headTag+":"+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"EHWT-dist",type+":"+headWord+","+headTag+":"+attDist));
				
				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"EMW-dist",type+":"+modifierWord+":"+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"EMT-dist",type+":"+modifierTag+":"+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"EMWT-dist",type+":"+modifierWord+","+modifierTag+":"+attDist));
				
				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"EHMW-dist",type+":"+headWord+","+modifierWord+":"+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"EHMT-dist",type+":"+headTag+","+modifierTag+":"+attDist));
				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"EHMWT-dist",type+":"+headWord+","+headTag+","+modifierWord+","+modifierTag+":"+attDist));	
			}	
		}
		
		if(type.equals("EMPTY")){
			//child feature
			long child = network.getNode(children_k[0]);
			int[] childArr = NetworkIDMapper.toHybridNodeArray(child);
			String ctype = types[childArr[4]];
			featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"CTYPE","child:"+ctype));
		}
		
		/****End of Entity features*******/
		
		
		//if incomplete span or complete but with spanlen is 2
		if(completeness==0 && type.equals("EMPTY")){
			
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

	
	
	
}

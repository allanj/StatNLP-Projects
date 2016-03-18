package com.statnlp.dp.model.um;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.DependInstance;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class UMFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 7274939836196010680L;

	private enum FEATYPE {unigram, bigram,contextual, inbetween,entity,prefix,joint};
	private String[] types;
	
	
	public UMFeatureManager(GlobalNetworkParam param_g, String[] types) {
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
		String type = null;
		if(parentArr[4]==2*types.length)
			type = "pae:null";
		else if(parentArr[4]>types.length-1){
			type = "pae:"+types[parentArr[4]-types.length];
		}else{
			type = types[parentArr[4]];
		}
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
		
		String pa_type = type.startsWith("pae")?type.split(":")[1]:type;
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
		
		if(!child_1_type.equals("OE") && type.startsWith("pae")){
			if(children_k.length!=1)
				throw new RuntimeException("parent is general span, but have "+children_k.length+" children?");
			
			if((pa_type.equals("OE")||pa_type.equals("null")) && !child_1_type.equals("OE") && !child_1_type.equals("ONE")){
				
				//unigram: upgrade the dependency somehow
				for(int i=leftIndex;i<=rightIndex;i++){	
					String word = sent.get(i).getName();
					String tag = sent.get(i).getTag();
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-UN-W", child_1_type+":"+word));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-UN-T", child_1_type+":"+tag));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-UN-WT", child_1_type+":"+word+","+tag));
				}
				
				//bigram
				for(int i=leftIndex-1;i<=rightIndex;i++){				
					String blw = i==leftIndex-1? "ESTR":sent.get(i).getName();
					String brw = i==rightIndex? "EEND":sent.get(i+1).getName();
					String blt = i==leftIndex-1? "ESTR":sent.get(i).getTag();
					String brt = i==rightIndex? "EEND":sent.get(i+1).getTag();
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-BI-W",child_1_type+":"+blw+","+brw));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-BI-T",child_1_type+":"+blt+","+brt));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-BI-WT",child_1_type+":"+blw+","+brw+","+blt+","+brt));
				}
				//trigram
				for(int i=leftIndex-2;i<=rightIndex;i++){				
					String tlw = i==leftIndex-2? "ESTR1":i==leftIndex-1? "ESTR":sent.get(i).getName();
					String tmw = i==leftIndex-2? "ESTR": i==rightIndex? "EEND":sent.get(i+1).getName();
					String trw = i==rightIndex-1? "EEND": i==rightIndex? "EEND1":sent.get(i+2).getName();
					String tlt = i==leftIndex-2? "ESTR1":i==leftIndex-1? "ESTR":sent.get(i).getTag();
					String tmt = i==leftIndex-2? "ESTR": i==rightIndex? "EEND":sent.get(i+1).getTag();
					String trt = i==rightIndex-1? "EEND": i==rightIndex? "EEND1":sent.get(i+2).getTag();
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-TRI-W",child_1_type+":"+tlw+","+tmw+","+trw));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-TRI-T",child_1_type+":"+tlt+","+tmt+","+trt));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-TRI-WT",child_1_type+":"+tlw+","+tmw+","+trw+","+tlt+","+tmt+","+trt));
				}
				//four-gram
				for(int i=leftIndex-3;i<=rightIndex;i++){				
					String flw = i==leftIndex-3? "ESTR2":i==leftIndex-2? "ESTR1":i==leftIndex-1?"ESTR":sent.get(i).getName();
					String fm1w = i==leftIndex-3? "ESTR1": i==leftIndex-2? "ESTR":i==rightIndex? "EEND":sent.get(i+1).getName();
					String fm2w = i==leftIndex-3? "ESTR": i==rightIndex-1? "EEND":i==rightIndex?"EEND1":sent.get(i+2).getName();
					String frw = i==rightIndex-2? "EEND": i==rightIndex-1? "EEND1":i==rightIndex? "EEND2":sent.get(i+3).getName();
					
					String flt = i==leftIndex-3? "ESTR2":i==leftIndex-2? "ESTR1":i==leftIndex-1?"ESTR":sent.get(i).getTag();
					String fm1t = i==leftIndex-3? "ESTR1": i==leftIndex-2? "ESTR":i==rightIndex? "EEND":sent.get(i+1).getTag();
					String fm2t = i==leftIndex-3? "ESTR": i==rightIndex-1? "EEND":i==rightIndex?"EEND1":sent.get(i+2).getTag();
					String frt = i==rightIndex-2? "EEND": i==rightIndex-1? "EEND1":i==rightIndex? "EEND2":sent.get(i+3).getTag();
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-FG-W",child_1_type+":"+flw+","+fm1w+","+fm2w+","+frw));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-FG-T",child_1_type+":"+flt+","+fm1t+","+fm2t+","+frt));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-FG-WT",child_1_type+":"+flw+","+fm1w+","+fm2w+","+frw+","+flt+","+fm1t+","+fm2t+","+frt));
				}
				
				StringBuilder sb = new StringBuilder(sent.get(leftIndex).getName() );
				StringBuilder sbAlls = new StringBuilder(sent.get(leftIndex).getName()+","+sent.get(leftIndex).getTag() );
				StringBuilder sbTags = new StringBuilder(sent.get(leftIndex).getTag() );
				for(int i=leftIndex+1;i<=rightIndex;i++){				
					sb.append(","+sent.get(i).getName() );
					sbAlls.append(","+sent.get(i).getName()+","+sent.get(i).getTag() ) ;
					sbTags.append(","+sent.get(i).getTag() );
				}
				//span entity words && tags feature
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"EWs",child_1_type+":"+sb.toString()) );
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ETs",child_1_type+":"+sbTags.toString()) );
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"EWTs",child_1_type+":"+sb.toString()+":"+sbTags.toString()) );
				
				
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
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ELBW-1",child_1_type+":"+lb+","+lw));
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ELBT-1",child_1_type+":"+lbt+","+lt));
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ELBWT-1",child_1_type+":"+lb+","+lbt+","+lw+","+lt));
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ERBW-1",child_1_type+":"+rw+","+rb));
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ERBT-1",child_1_type+":"+rt+","+rbt));
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ERBWT-1",child_1_type+":"+rw+","+rt+","+rb+","+rbt));
				
				
				/****Joint Features, which should be important**/
				if(completeness==0){
//					/**Unigram feature without dist info**/
					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-headword", "JOINT:"+child_1_type+":"+headWord));
					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-headtag", "JOINT:"+child_1_type+":"+headTag));
					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-modifierword", "JOINT:"+child_1_type+":"+modifierWord));
					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-modifiertag", "JOINT:"+child_1_type+":"+modifierTag));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-headwordtag", "JOINT:"+child_1_type+":"+headWord+","+headTag));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-modifierwordtag", child_1_type+":"+modifierWord+","+modifierTag));
//					
//					/**Unigram feature with dist info**/
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-headword-dist", child_1_type+":"+headWord+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-headtag-dist", child_1_type+":"+headTag+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-modifierword-dist", child_1_type+":"+modifierWord+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-modifiertag-dist", child_1_type+":"+modifierTag+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-headwordtag-dist", child_1_type+":"+headWord+","+headTag+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-modifierwordtag-dist", child_1_type+":"+modifierWord+","+modifierTag+attDist));
//					
//					/****Bigram features without dist info******/
					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-bigramword", "JOINT:"+child_1_type+":"+headWord+","+modifierWord));
					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"joint-bigramtag", "JOINT:"+child_1_type+":"+headTag+","+modifierTag));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-bigramnametag",  child_1_type+":"+headWord+","+headTag+","+modifierWord+","+modifierTag));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-headallmoditag", child_1_type+":"+headWord+","+headTag+","+modifierTag));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-headallmodiword", child_1_type+":"+headWord+","+headTag+","+modifierWord));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-headtagmodiall", child_1_type+":"+headTag+","+modifierWord+","+modifierTag));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-headwordmodiall", child_1_type+":"+headWord+","+modifierWord+","+modifierTag));
//					
//					/****Bigram features with dist info******/
					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-bigramword-att", "JOINT:"+child_1_type+":"+headWord+","+modifierWord+": DIR:"+att));
					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"joint-bigramtag-att", "JOINT:"+child_1_type+":"+headTag+","+modifierTag+": DIR:"+att));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-bigramnametag-dist",  child_1_type+":"+headWord+","+headTag+","+modifierWord+","+modifierTag+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-headallmoditag-dist", child_1_type+":"+headWord+","+headTag+","+modifierTag+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-headallmodiword-dist", child_1_type+":"+headWord+","+headTag+","+modifierWord+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-headtagmodiall-dist", child_1_type+":"+headTag+","+modifierWord+","+modifierTag+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-headwordmodiall-dist", child_1_type+":"+headWord+","+modifierWord+","+modifierTag+attDist));
//				
//					
//					
//					String leftMinusTag = leftIndex>0? sent.get(leftIndex-1).getTag(): "STR";
//					String rightPlusTag = rightIndex<sent.length()-1? sent.get(rightIndex+1).getTag():"END";
//					String leftPlusTag = leftIndex<rightIndex-1? sent.get(leftIndex+1).getTag():"MID";
//					String rightMinusTag = rightIndex-1 > leftIndex? sent.get(rightIndex-1).getTag():"MID";
//					
//					
//					
//					//l-1,l,r,r+1
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-1-1-dist", child_1_type+":"+leftMinusTag+","+leftTag+","+rightTag+","+rightPlusTag+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-1-2-dist", child_1_type+":"+leftMinusTag+","+leftTag+","+rightTag+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-1-3-dist", child_1_type+":"+leftMinusTag+","+rightTag+","+rightPlusTag+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-1-4-dist", child_1_type+":"+leftMinusTag+","+leftTag+","+rightPlusTag+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-1-5-dist", child_1_type+":"+leftTag+","+rightTag+","+rightPlusTag+attDist));
//					
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-1-1", child_1_type+":"+leftMinusTag+","+leftTag+","+rightTag+","+rightPlusTag));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-1-2", child_1_type+":"+leftMinusTag+","+leftTag+","+rightTag));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-1-3", child_1_type+":"+leftMinusTag+","+rightTag+","+rightPlusTag));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-1-4", child_1_type+":"+leftMinusTag+","+leftTag+","+rightPlusTag));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-1-5", child_1_type+":"+leftTag+","+rightTag+","+rightPlusTag));
//					
//					
//					//l,l+1,r-1,r
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-2-1-dist", child_1_type+":"+leftTag+","+leftPlusTag+","+rightMinusTag+","+rightTag+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-2-2-dist", child_1_type+":"+leftTag+","+rightMinusTag+","+rightTag+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-2-3-dist", child_1_type+":"+leftTag+","+leftPlusTag+","+rightTag+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-2-4-dist", child_1_type+":"+leftPlusTag+","+rightMinusTag+","+rightTag+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-2-5-dist", child_1_type+":"+leftTag+","+leftPlusTag+","+rightMinusTag+attDist));
//					
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-2-1", child_1_type+":"+leftTag+","+leftPlusTag+","+rightMinusTag+","+rightTag));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-2-2", child_1_type+":"+leftTag+","+rightMinusTag+","+rightTag));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-2-3", child_1_type+":"+leftTag+","+leftPlusTag+","+rightTag));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-2-4", child_1_type+":"+leftPlusTag+","+rightMinusTag+","+rightTag));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-2-5", child_1_type+":"+leftTag+","+leftPlusTag+","+rightMinusTag));
//					
////						
//					//l-1,l,r-1,r
//					//l,l+1,r,r+1
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-3-1-dist", child_1_type+":"+leftMinusTag+","+leftTag+","+rightMinusTag+","+rightTag+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-3-1", child_1_type+":"+leftMinusTag+","+leftTag+","+rightMinusTag+","+rightTag));
//					
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-4-1-dist", child_1_type+":"+leftTag+","+leftPlusTag+","+rightTag+","+rightPlusTag+attDist));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-contextual-4-1", child_1_type+":"+leftTag+","+leftPlusTag+","+rightTag+","+rightPlusTag));
//					
//					
//					for(int i=leftIndex+1;i<rightIndex;i++){
//						featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-inbetween-1", child_1_type+":"+leftTag+","+sent.get(i).getTag()+","+rightTag+attDist));
//						featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-inbetween-2", child_1_type+":"+leftTag+","+sent.get(i).getTag()+","+rightTag));
//						featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-inbetween-3", child_1_type+":"+leftA+","+sent.get(i).getATag()+","+rightA+attDist));
//						featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-inbetween-4", child_1_type+":"+leftA+","+sent.get(i).getATag()+","+rightA));
//					}
				}
				
				
				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"pa-child-type","parent:"+pa_type+","+"child:"+child_1_type));
				String comType = completeness==0? "INCOM":"COM";
				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"pa-child-type-dir","parent:"+pa_type+","+"child:"+child_1_type+","+att+",type:"+comType));
				
				

			}
			
			
		}
		
		/****End of Entity features*******/
		
		
		//if incomplete span or complete but with spanlen is 2
		if(completeness==0 && type.startsWith("pae")){
			
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

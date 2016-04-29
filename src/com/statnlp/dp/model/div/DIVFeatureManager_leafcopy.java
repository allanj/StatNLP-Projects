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

public class DIVFeatureManager_leafcopy extends FeatureManager {

	private static final long serialVersionUID = 7274939836196010680L;

	private enum FEATYPE {unigram, bigram,contextual, inbetween,entity,prefix,joint};
	private String[] types;
	
	public static String PARENT_IS = DPConfig.PARENT_IS;
	public static String OE = DPConfig.OE;
	public static String ONE = DPConfig.ONE;
	
	public static String O_TYPE = DPConfig.O_TYPE;
	public static String E_B_PREFIX = DPConfig.E_B_PREFIX;
	public static String E_I_PREFIX = DPConfig.E_I_PREFIX;
	public static String LD = "LEFT";
	public static String RD = "RIGHT";
	public static String separator=":"+RD+":"+LD+":";
	
	
	public DIVFeatureManager_leafcopy(GlobalNetworkParam param_g, String[] types) {
		super(param_g);
		this.types = types;
	}
	
	//do not read the weight from linear CRF
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
		
		String att = direction==1? "RA":"LA";
		//used for joi
		int headIndex = direction==1? leftIndex:rightIndex;
		int modifierIndex = direction==1? rightIndex:leftIndex;
		String headWord = sent.get(headIndex).getName();
		String headTag = sent.get(headIndex).getTag();
		String modifierWord = sent.get(modifierIndex).getName();
		String modifierTag = sent.get(modifierIndex).getTag();
		String leftTag = sent.get(leftIndex).getTag();
		String rightTag = sent.get(rightIndex).getTag();
		String distBool = "0";
		int dist = Math.abs(rightIndex-leftIndex);
		if(dist > 1)  distBool = "1";
		if(dist > 2)  distBool = "2";
		if(dist > 3)  distBool = "3";
		if(dist > 4)  distBool = "4";
		if(dist > 5)  distBool = "5";
		if(dist > 10) distBool = "10";
		String attDist = "&"+att+"&"+distBool;
			
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
		
		
		/********************pairwise features********************/
//		//split the incomplete the span to acquire pairwise features, exclude the O features
		if(!type.startsWith(PARENT_IS) && !type.equals(OE) && !type.equals(ONE) && completeness == 0){
			int splitPoint = childArr_1[0]; // the rightIndex of the left child
			String word = sent.get(splitPoint+1).getName();
			String tag = sent.get(splitPoint+1).getTag();
			String prevWord = splitPoint==0?"STR":sent.get(splitPoint).getName();
			String prevTag = splitPoint==0?"STR":sent.get(splitPoint).getTag();
			String currEn = type;
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-pair",currEn));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-prevW-currW",currEn+separator+prevWord+separator+word));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-prevT-currT",currEn+separator+prevTag+separator+tag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "currW-E",word+separator+currEn));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "prevW-E",prevWord+separator+currEn));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "currT-E",tag+separator+currEn));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "prevT-E",prevTag+separator+currEn));
		}
//		
//		if(!type.startsWith(PARENT_IS) && !type.equals(OE) && !type.equals(ONE) && leftIndex!=rightIndex && completeness==1){
//			int splitPoint = childArr_1[0];
//			String currEn = type;
//			String word = sent.get(splitPoint).getName();
//			String tag = sent.get(splitPoint).getTag();
//			currEn = E_I_PREFIX+currEn;
//			
//			featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-2D-W", LD+","+currEn+":"+RD+","+currEn+":"+word));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-2D-T", LD+","+currEn+":"+RD+","+currEn+":"+tag));
//		}
		/****************************End of pairwise features********************************************/
		
		
		if(type.startsWith(PARENT_IS)){
			if(children_k.length!=1)
				throw new RuntimeException("parent is general span, but have "+children_k.length+" children?");

			//leaf nodes.. local features: include the O features
			if(!child_1_type.equals(OE) && leftIndex==rightIndex && !sent.get(leftIndex).getName().equals("ROOT")){
				int i = leftIndex;
				String word = sent.get(i).getName();
				String tag = sent.get(i).getTag();
				String prevWord = i>1?sent.get(i-1).getName():"STR";
				String prevTag = i>1?sent.get(i-1).getTag():"STR";
				String nextWord = i<sent.length()-1? sent.get(i+1).getName():"END";
				String nextTag = i<sent.length()-1? sent.get(i+1).getTag():"END";
				String child_type = child_1_type;
				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "EW", child_type+":"+word));
				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ET", child_type+":"+tag));
				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ELW", child_type+":"+prevWord));
				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ELT", child_type+":"+prevTag));
				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ERW", child_type+":"+nextWord));
				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ERT", child_type+":"+nextTag));
				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ELT-T",child_type+":"+prevTag+","+tag));
				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "EW-dir", child_type+":"+word+":"+att));
				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ET-dir", child_type+":"+tag+":"+att));
				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ELW-dir", child_type+":"+prevWord+":"+att));
				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ELT-dir", child_type+":"+prevTag+":"+att));
				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ERW-dir", child_type+":"+nextWord+":"+att));
				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ERT-dir", child_type+":"+nextTag+":"+att));
				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ELT-T-dir",child_type+":"+prevTag+","+tag+":"+att));
				
	
				/****Add some prefix features******/
				for(int plen = 1;plen<=6;plen++){
					if(word.length()>=plen){
						String suff = word.substring(word.length()-plen, word.length());
						String pref = word.substring(0,plen);
						featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-PATTERN-SUFF-"+plen, child_type+":"+suff));
						featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-PATTERN-SUFF-"+plen+"-dir", child_type+":"+suff+":"+att));
						featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-PATTERN-PREF-"+plen, child_type+":"+pref));
						featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-PATTERN-PREF-"+plen+"-dir", child_type+":"+pref+":"+att));
					}
				}
			}
			
			//the outmost entity
			if((pa_type.equals(OE)||pa_type.equals("null")) && !child_1_type.equals(OE) && !child_1_type.equals(ONE)){
				
				//when comes in must be incomplete
				if(leftIndex!=rightIndex && completeness!=0){
					System.err.println(sent.toString());
					System.err.println("Current Parent:"+leftIndex+","+rightIndex+","+completeness+","+att);
					throw new RuntimeException("Something must be wrong, outsidest entity or non-entity must be an incomplete span");
				}
				if(leftIndex==rightIndex && direction==0){throw new RuntimeException("single outside entity shouldn't be in left direction");}
				
				/******************The begin of that entity*********************/
				if( (leftIndex==rightIndex && direction==1) || completeness==0){
					String word = sent.get(leftIndex).getName();
					String tag = sent.get(leftIndex).getTag();
					String prevWord = leftIndex>1?sent.get(leftIndex-1).getName():"STR";
					String prevTag = leftIndex>1?sent.get(leftIndex-1).getTag():"STR";
					String nextWord = leftIndex<sent.length()-1? sent.get(leftIndex+1).getName():"END";
					String nextTag = leftIndex<sent.length()-1? sent.get(leftIndex+1).getTag():"END";
					String child_type = E_B_PREFIX+child_1_type;
					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "B-EW", child_type+":"+word));
					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "B-ET", child_type+":"+tag));
					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "B-ELW", child_type+":"+prevWord));
					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "B-ELT", child_type+":"+prevTag));
					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "B-ERW", child_type+":"+nextWord));
					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "B-ERT", child_type+":"+nextTag));
					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "B-ELT-T",child_type+":"+prevTag+","+tag));
					/****Add some prefix features******/
					for(int plen = 1;plen<=6;plen++){
						if(word.length()>=plen){
							String suff = word.substring(word.length()-plen, word.length());
							String pref = word.substring(0,plen);
							featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "B-E-PATTERN-SUFF-"+plen, child_type+":"+suff));
							featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "B-E-PATTERN-PREF-"+plen, child_type+":"+pref));
						}
					}
					
				}
				
				/************whole entity feature*************/
//				StringBuilder sb = new StringBuilder("");
//				for(int i=leftIndex;i<=rightIndex;i++){
//					String word = sent.get(i).getName();
//					if(i==leftIndex) sb.append("<sep>"+word+":"+RD);
//					else if(i==rightIndex) sb.append("<sep>"+word+":"+LD);
//					else sb.append("<sep>"+word);
//				}
//				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-whole-words",child_1_type+":"+sb.toString()) );
				
				
					/**************inside boundary features**************/
//					String lw = sent.get(leftIndex).getName();
//					String rw = sent.get(rightIndex).getName();
//					String lt = sent.get(leftIndex).getTag();
//					String rt = sent.get(rightIndex).getTag();
//					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-IN-LW-RW",  child_1_type+":"+lw+":"+rw));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-IN-LT-RT",  child_1_type+":"+lt+":"+rt));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-IN-LWT-RWT",child_1_type+":"+lw+":"+lt+"-"+rw+":"+rt));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-IN-LW-RT",  child_1_type+":"+lw+":"+rt));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-IN-LT-RW",  child_1_type+":"+lt+":"+rw));
//
//					/********************outside boundary features***************************/
					String lb = leftIndex>1? sent.get(leftIndex-1).getName():"STR";
					String lbt = leftIndex>1? sent.get(leftIndex-1).getTag():"STR";
					String rb = rightIndex<sent.length()-1? sent.get(rightIndex+1).getName():"END";
					String rbt = rightIndex<sent.length()-1? sent.get(rightIndex+1).getTag():"END";
//					String rightPrefix = leftIndex==rightIndex?E_B_PREFIX:E_I_PREFIX;
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ELBW-1",E_B_PREFIX+child_1_type+":"+lb+":START"));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ELBT-1",E_B_PREFIX+child_1_type+":"+lbt+":START"));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ELBWT-1",E_B_PREFIX+child_1_type+":"+lb+","+lbt+":LEFT_1_BD"));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ERBW-1",child_1_type+":"+rb+":END"));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ERBT-1",child_1_type+":"+rbt+":END"));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ERBWT-1",rightPrefix+child_1_type+":"+rb+","+rbt+":RIGHT_1_BD"));
//					
//					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ELBW-RBW",E_B_PREFIX+child_1_type+":"+lb+":"+rb));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ELBT-RBT",child_1_type+":"+lbt+":"+rbt));
					for(int i=leftIndex;i<=rightIndex;i++){
						featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"ELBT-RBT-INB",child_1_type+":"+lbt+":"+sent.get(i).getTag()+":"+rbt));
					}
//
//					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-LBT-LT", E_B_PREFIX+child_1_type+":"+lbt+","+lt));
//					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-LBT-LW", E_B_PREFIX+child_1_type+":"+lbt+","+lw));
//					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-LBW-LT", E_B_PREFIX+child_1_type+":"+lb+","+lt));
//					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-LBW-LW", E_B_PREFIX+child_1_type+":"+lb+","+lw));
//					
//					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-RBT-RT", rightPrefix+child_1_type+":"+rbt+","+rt));
//					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-RBT-RW", rightPrefix+child_1_type+":"+rbt+","+rw));
//					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-RBW-RT", rightPrefix+child_1_type+":"+rb+","+rt));
//					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-RBW-RW", rightPrefix+child_1_type+":"+rb+","+rw));
				
//
				/**********************add some joint features**************under the IF statement of outmost entity****************/
				/****Bigram features without dist info******/
//				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-bigramword", "JOINT:"+child_1_type+":"+headWord+"-"+modifierWord));
//				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"joint-bigramtag", "JOINT:"+child_1_type+":"+headTag+"-"+modifierTag));
//				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-bigramword-dist", "JOINT:"+child_1_type+":"+headWord+"-"+modifierWord+attDist));
//				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"joint-bigramtag-dist", "JOINT:"+child_1_type+":"+headTag+"-"+modifierTag+attDist));
//				for(int i=leftIndex+1;i<rightIndex;i++){
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-inbetween-1", "JOINT:"+child_1_type+":"+leftTag+","+sent.get(i).getTag()+","+rightTag));
//					featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-inbetween-2", "JOINT:"+child_1_type+":"+leftTag+","+sent.get(i).getTag()+","+rightTag+attDist));
//				}
			}
			
			addDepFeatures(featureList,network,parentArr,children_k,sent);
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

	private void addDepFeatures(ArrayList<Integer> featureList, Network network, int[] parentArr, int[] children_k, Sentence sent){
		
		
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
		
		
		if(children_k.length==0) return;
		
		
		if(completeness==0 && type.startsWith(PARENT_IS)){
			
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
	

}

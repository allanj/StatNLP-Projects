package com.statnlp.projects.dep.model.hyperedge;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.projects.dep.DependInstance;
import com.statnlp.projects.dep.utils.DPConfig;
import com.statnlp.projects.dep.utils.Extractor;

public class HPEFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 7274939836196010680L;

	private enum FEATYPE {unigram, bigram,contextual, inbetween,entity,prefix,joint};
	private String[] types;
	
	public static String OE = DPConfig.OE;
	public static String ONE = DPConfig.ONE;
	
	public static String O_TYPE = DPConfig.O_TYPE;
	public static String E_B_PREFIX = DPConfig.E_B_PREFIX;
	public static String E_I_PREFIX = DPConfig.E_I_PREFIX;
	
	
	public HPEFeatureManager(GlobalNetworkParam param_g, String[] types) {
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
		String pa_type = null;
		if(parentArr[4]==types.length)
			pa_type = "null";
		else pa_type=types[parentArr[4]];
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
		
		if(children_k.length==0) return FeatureArray.EMPTY;
		if(children_k.length==1 && !pa_type.equals("null")) throw new RuntimeException("parent is root span, but have "+children_k.length+" children?");
		if(children_k.length==1) return FeatureArray.EMPTY;

		
		long[] children = new long[2];
		int[][] childrenArr = new int[children.length][];
		String[] childrenType = new String[children.length];
		for(int i=0;i<children.length;i++) {
			children[i] = network.getNode(children_k[i]);
			childrenArr[i] = NetworkIDMapper.toHybridNodeArray(children[i]);
			if(childrenArr[i][4]==types.length)
				childrenType[i] = "null";
			else childrenType[i]=types[childrenArr[i][4]];
		}
		//System.err.println("patype:"+pa_type+", "+Arrays.toString(childrenType));
		
		//same position feature:
		if(!childrenType[0].equals(OE) && !childrenType[1].equals(OE) && completeness==1){
			int splitPoint = childrenArr[0][0];
			String word = sent.get(splitPoint).getName();
			String tag = sent.get(splitPoint).getTag();
			String prevEntity = childrenType[0];
			String currEn = childrenType[1];
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "currW-prevE-currE-samePos",word+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "currT-prevE-currE-samePos",tag+":"+prevEntity+":"+currEn));
		}
		
		//pairwise features
		if(completeness==0 && !childrenType[0].equals(OE) && !childrenType[1].equals(OE)){
			int splitPoint = childrenArr[0][0]; // the rightIndex of the left child
			String word = sent.get(splitPoint+1).getName();
			String tag = sent.get(splitPoint+1).getTag();
			String prevWord = splitPoint==0?"STR":sent.get(splitPoint).getName();
			String prevTag = splitPoint==0?"STR":sent.get(splitPoint).getTag();
			String nextWord = splitPoint+2<sent.length()?sent.get(splitPoint+2).getName():"END";
			String nextTag = splitPoint+2<sent.length()?sent.get(splitPoint+2).getTag():"END";
			String prevEntity = childrenType[0];
			String currEn = childrenType[1];
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-prev-E",prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "currW-prevE-currE",word+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "prevW-prevE-currE",prevWord+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "nextW-prevE-currE",nextWord+":"+prevEntity+":"+currEn));
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "currT-prevE-currE",tag+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "prevT-prevE-currE",prevTag+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "nextT-prevE-currE",nextTag+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "prevT-currT-prevE-currE",prevTag+":"+tag+":"+prevEntity+":"+currEn));
		}
		
		for(int c=0;c<children.length;c++){
			int cleft = childrenArr[c][0]-childrenArr[c][1];
			int cright = childrenArr[c][0];
			String catt = childrenArr[c][3] == 1 ? "RA":"LA" ;
			if(!childrenType[c].equals(OE) && cleft==cright && !sent.get(cleft).getName().equals("ROOT")){
				int i = cleft;
				String word = sent.get(i).getName();
				String tag = sent.get(i).getTag();
				String prevWord = i>1?sent.get(i-1).getName():"STR";
				String prevTag = i>1?sent.get(i-1).getTag():"STR";
				String nextWord = i<sent.length()-1? sent.get(i+1).getName():"END";
				String nextTag = i<sent.length()-1? sent.get(i+1).getTag():"END";
				String child_type = childrenType[c];
//				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "EW", child_type+":"+word));
//				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ET", child_type+":"+tag));
//				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ELW", child_type+":"+prevWord));
//				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ELT", child_type+":"+prevTag));
//				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ERW", child_type+":"+nextWord));
//				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ERT", child_type+":"+nextTag));
//				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ELT-T",child_type+":"+prevTag+","+tag));
				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "EW-dir", child_type+":"+word+":"+catt));
				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ET-dir", child_type+":"+tag+":"+catt));
				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ELW-dir", child_type+":"+prevWord+":"+catt));
				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ELT-dir", child_type+":"+prevTag+":"+catt));
				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ERW-dir", child_type+":"+nextWord+":"+catt));
				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ERT-dir", child_type+":"+nextTag+":"+catt));
				featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "ELT-T-dir",child_type+":"+prevTag+","+tag+":"+catt));

				/****Add some prefix features******/
				for(int plen = 1;plen<=6;plen++){
					if(word.length()>=plen){
						String suff = word.substring(word.length()-plen, word.length());
						String pref = word.substring(0,plen);
//						featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-PATTERN-SUFF-"+plen, child_type+":"+suff));
						featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-PATTERN-SUFF-"+plen+"-dir", child_type+":"+suff+":"+catt));
//						featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-PATTERN-PREF-"+plen, child_type+":"+pref));
						featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-PATTERN-PREF-"+plen+"-dir", child_type+":"+pref+":"+catt));
					}
				}
			}
			if(childrenArr[c][2]==0 && isEntity(childrenType[c]) && pa_type.equals(OE)){
				if( (cleft==cright && direction==1) || completeness==0){
					String word = sent.get(cleft).getName();
					String tag = sent.get(cleft).getTag();
					String prevWord = leftIndex>1?sent.get(cleft-1).getName():"STR";
					String prevTag = leftIndex>1?sent.get(cleft-1).getTag():"STR";
					String nextWord = leftIndex<sent.length()-1? sent.get(cleft+1).getName():"END";
					String nextTag = leftIndex<sent.length()-1? sent.get(cleft+1).getTag():"END";
					String child_type = E_B_PREFIX+childrenType[c];
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
				
				for(int i=cleft;i<=cright-1;i++){
					String blw = sent.get(i).getName();
					String brw = sent.get(i+1).getName();
					String blt = sent.get(i).getTag();
					String brt =sent.get(i+1).getTag();
					String prevWord = i>1?sent.get(i-1).getName():"STR";
					String prevTag = i>1?sent.get(i-1).getTag():"STR";
					String nextWord = i<sent.length()-2? sent.get(i+2).getName():"END";
					String nextTag = i<sent.length()-2? sent.get(i+2).getTag():"END";
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-BI-WORD", childrenType[c]+":"+blw+":"+brw));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-BI-TAG", childrenType[c]+":"+blt+":"+brt));
					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-BI-ELW", childrenType[c]+":"+prevWord));
					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-BI-ELT", childrenType[c]+":"+prevTag));
					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-BI-ERW", childrenType[c]+":"+nextWord));
					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-BI-ERT", childrenType[c]+":"+nextTag));
					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-BI-ELT-T",childrenType[c]+":"+prevTag+","+blt+":"+brt));
				}
				for(int i=cleft;i<=cleft-2;i++){
					String tlw = sent.get(i).getName();
					String tlt = sent.get(i).getTag();
					String tmw = sent.get(i+1).getName();
					String tmt =sent.get(i+1).getTag();
					String trw = sent.get(i+2).getName();
					String trt =sent.get(i+2).getTag();
					String prevWord = i>1?sent.get(i-1).getName():"STR";
					String prevTag = i>1?sent.get(i-1).getTag():"STR";
					String nextWord = i<sent.length()-3? sent.get(i+3).getName():"END";
					String nextTag = i<sent.length()-3? sent.get(i+3).getTag():"END";
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-TRI-WORD",  childrenType[c]+":"+tlw+":"+tmw+":"+trw));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-TRI-TAG",  childrenType[c]+":"+tlt+":"+tmt+":"+trt));
					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-TRI-ELW",  childrenType[c]+":"+prevWord));
					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-TRI-ELT",  childrenType[c]+":"+prevTag));
					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-TRI-ERW",  childrenType[c]+":"+nextWord));
					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-TRI-ERT",  childrenType[c]+":"+nextTag));
					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-TRI-ELT-T", childrenType[c]+":"+prevTag+","+tlt+":"+tmt+":"+trt));
				}
				//4-gram
				for(int i=cleft;i<=cleft-3;i++){
					String flw = sent.get(i).getName();
					String flt = sent.get(i).getTag();
					String fm1w = sent.get(i+1).getName();
					String fm1t =sent.get(i+1).getTag();
					String fm2w = sent.get(i+2).getName();
					String fm2t =sent.get(i+2).getTag();
					String frw = sent.get(i+3).getName();
					String frt =sent.get(i+3).getTag();
					String prevWord = i>1?sent.get(i-1).getName():"STR";
					String prevTag = i>1?sent.get(i-1).getTag():"STR";
					String nextWord = i<sent.length()-4? sent.get(i+4).getName():"END";
					String nextTag = i<sent.length()-4? sent.get(i+4).getTag():"END";
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-FG-WORD",  childrenType[c]+":"+flw+":"+fm1w+":"+fm2w+":"+frw));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-FG-TAG",  childrenType[c]+":"+flt+":"+fm1t+":"+fm2t+":"+frt));
					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-FG-ELW",  childrenType[c]+":"+prevWord));
					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-FG-ELT",  childrenType[c]+":"+prevTag));
					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-FG-ERW",  childrenType[c]+":"+nextWord));
					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-FG-ERT",  childrenType[c]+":"+nextTag));
					featureList.add(this._param_g.toFeature(network, FEATYPE.entity.name(), "E-FG-ELT-T", childrenType[c]+":"+prevTag+","+flt+":"+fm1t+":"+fm2t+":"+frt));
				}
				StringBuilder sb = new StringBuilder("");
				StringBuilder sbTags = new StringBuilder("");
				for(int i=cleft;i<=cright;i++){
					String word = sent.get(i).getName();
					String tag = sent.get(i).getTag();
					sb.append("<sep>"+word);
					sbTags.append("<sep>"+tag);
					
				}
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-whole-words",childrenType[c]+":"+sb.toString()) );
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(),"E-whole-tags",childrenType[c]+":"+sbTags.toString()) );
			}
		}
		
		if(isEntity(pa_type) && completeness==0){
			featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-bigramword", "JOINT:"+pa_type+":"+headWord+":"+modifierWord));
			featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"joint-bigramtag", "JOINT:"+pa_type+":"+headTag+":"+modifierTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-bigramword-dist", "JOINT:"+pa_type+":"+headWord+":"+modifierWord+attDist));
			featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(),"joint-bigramtag-dist", "JOINT:"+pa_type+":"+headTag+":"+modifierTag+attDist));
			for(int i=leftIndex+1;i<rightIndex;i++){
				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-inbetween-1", "JOINT:"+pa_type+":"+leftTag+","+sent.get(i).getTag()+","+rightTag));
				featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "joint-inbetween-2", "JOINT:"+pa_type+":"+leftTag+","+sent.get(i).getTag()+","+rightTag+attDist));
			}
		}
		
		
		addDepFeatures(featureList,network,parentArr,sent);
	
		
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
	
	

	private void addDepFeatures(ArrayList<Integer> featureList, Network network, int[] parentArr, Sentence sent){
		
		
		int leftIndex = parentArr[0] - parentArr[1];
		int rightIndex = parentArr[0];
		int direction = parentArr[3];
		int completeness = parentArr[2];
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
		
		
		
		if(completeness==0){
			
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
	
	private boolean isEntity(String type){
		return !type.equals("null") && !type.equals(ONE) && !type.equals(OE);
	}

}
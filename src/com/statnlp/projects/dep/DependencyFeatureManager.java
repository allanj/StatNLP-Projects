package com.statnlp.projects.dep;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.neural.NeuralConfig;
import com.statnlp.projects.dep.commons.DepLabel;

public class DependencyFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 7274939836196010680L;

	public enum FeaType {unigram, bigram,contextual, inbetween, prefix,pipe,label, neural_1};
	protected boolean isPipe;
	protected boolean labeledDep;
	protected boolean basicFeature = true; 
	protected boolean entityFeature = false;
	protected int windowSize = 1;
	protected String insep = NeuralConfig.IN_SEP;
	protected String outsep = NeuralConfig.OUT_SEP;
	
	public DependencyFeatureManager(GlobalNetworkParam param_g, boolean isPipe, boolean labeledDep, int windowSize) {
		this(param_g, isPipe, labeledDep, windowSize, true, false);
	}
	
	public DependencyFeatureManager(GlobalNetworkParam param_g, boolean isPipe, boolean labeledDep, int windowSize, boolean basicFeature, boolean entityFeature) {
		super(param_g);
		this.isPipe = isPipe;
		this.labeledDep = labeledDep;
		this.windowSize = windowSize;
		this.basicFeature = basicFeature;
		this.entityFeature = entityFeature;
	}
	
	
	/**
	 * Only add the transition features here.
	 */
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		
		FeatureArray fa = null;
		
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		
		DependInstance di = (DependInstance)network.getInstance();
		//int instanceId = di.getInstanceId();
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
			String label = labeledDep? DepLabel.LABELS_INDEX.get(parentArr[4]).getForm(): null;
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
			
			if (basicFeature) {
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
				if(labeledDep){
					addLabeledFeatures(network, att, true, sent, modifierIndex, label, featureList);
					addLabeledFeatures(network, att, false, sent, headIndex, label, featureList);
				}
				
				
				/***Prefix 5 gram features*********/
				if(headWord.length()>5 || modifierWord.length()>5){
					int hL = headWord.length();
					int mL = modifierWord.length();
					String preHead = hL>5? headWord.substring(0,5):headWord;
					String preModifier = mL>5?modifierWord.substring(0,5):modifierWord;
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "bigram-prefix-all-dist", preHead+","+headTag+","+preModifier+","+modifierTag+attDist));;
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "bigram-prefix-word-dist", preHead+","+preModifier+attDist));;
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "bigram-prefix-all", preHead+","+headTag+","+preModifier+","+modifierTag));;
					featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "bigram-prefix-word", preHead+","+preModifier));;
					if(mL>5){
						featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-hTmomoT-dist", headTag+","+preModifier+","+modifierTag+attDist));;
						featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-hTmo-dist", headTag+","+preModifier+attDist));;
						featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-modiall-dist", preModifier+","+modifierTag+attDist));;
						featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-modi-dist", preModifier+attDist));;
						featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-hTmomoT", headTag+","+preModifier+","+modifierTag));;
						featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-hTmo", headTag+","+preModifier));;
						featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-modiall", preModifier+","+modifierTag));;
						featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "modi-prefix-modiall", preModifier));;
					}
					if(hL>5){
						featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hhTmoT-dist", preHead+","+headTag+","+modifierTag+attDist));;
						featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hmoT-dist", preHead+","+modifierTag+attDist));;
						featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hall-dist", preHead+","+headTag+attDist));;
						featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-h-dist", preHead+attDist));;
						featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hhTmoT", preHead+","+headTag+","+modifierTag));;
						featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hmoT", preHead+","+modifierTag));;
						featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-hall", preHead+","+headTag));;
						featureList.add(this._param_g.toFeature(network, FeaType.prefix.name(), "head-prefix-h", preHead));
					}
				}
				
				/**Unigram feature without dist info**/
				featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headword", headWord));;
				featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headtag", headTag));;
				featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifierword", modifierWord));;
				featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifiertag", modifierTag));;
				featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headwordtag", headWord+","+headTag));
				featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifierwordtag", modifierWord+","+modifierTag));
				
				/**Unigram feature with dist info**/
				featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headword-dist", headWord+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headtag-dist", headTag+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifierword-dist", modifierWord+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifiertag-dist", modifierTag+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "headwordtag-dist", headWord+","+headTag+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.unigram.name(), "modifierwordtag-dist", modifierWord+","+modifierTag+attDist));
				
				/****Bigram features without dist info******/
				featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "bigramword", headWord+","+modifierWord));
				featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(),"bigramtag", headTag+","+modifierTag));
				featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "bigramnametag",  headWord+","+headTag+","+modifierWord+","+modifierTag));
				featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headallmoditag", headWord+","+headTag+","+modifierTag));
				featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headallmodiword", headWord+","+headTag+","+modifierWord));
				featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headtagmodiall", headTag+","+modifierWord+","+modifierTag));
				featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headwordmodiall", headWord+","+modifierWord+","+modifierTag));
				
				/****Bigram features with dist info******/
				featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "bigramword-dist", headWord+","+modifierWord+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(),"bigramtag-dist", headTag+","+modifierTag+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "bigramnametag-dist",  headWord+","+headTag+","+modifierWord+","+modifierTag+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headallmoditag-dist", headWord+","+headTag+","+modifierTag+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headallmodiword-dist", headWord+","+headTag+","+modifierWord+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headtagmodiall-dist", headTag+","+modifierWord+","+modifierTag+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.bigram.name(), "headwordmodiall-dist", headWord+","+modifierWord+","+modifierTag+attDist));
			
				
				
				String leftMinusTag = leftIndex>0? sent.get(leftIndex-1).getTag(): "STR";
				String rightPlusTag = rightIndex<sent.length()-1? sent.get(rightIndex+1).getTag():"END";
				String leftPlusTag = leftIndex<rightIndex-1? sent.get(leftIndex+1).getTag():"MID";
				String rightMinusTag = rightIndex-1 > leftIndex? sent.get(rightIndex-1).getTag():"MID";
				
				String leftMinusA = leftIndex>0? sent.get(leftIndex-1).getATag(): "STR";
				String rightPlusA = rightIndex<sent.length()-1? sent.get(rightIndex+1).getATag():"END";
				String leftPlusA = leftIndex<rightIndex-1? sent.get(leftIndex+1).getATag():"MID";
				String rightMinusA = rightIndex-1 > leftIndex?sent.get(rightIndex-1).getATag():"MID";
				
				//l-1,l,r,r+1
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-1-dist", leftMinusTag+","+leftTag+","+rightTag+","+rightPlusTag+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-2-dist", leftMinusTag+","+leftTag+","+rightTag+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-3-dist", leftMinusTag+","+rightTag+","+rightPlusTag+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-4-dist", leftMinusTag+","+leftTag+","+rightPlusTag+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-5-dist", leftTag+","+rightTag+","+rightPlusTag+attDist));
				
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-1", leftMinusTag+","+leftTag+","+rightTag+","+rightPlusTag));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-2", leftMinusTag+","+leftTag+","+rightTag));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-3", leftMinusTag+","+rightTag+","+rightPlusTag));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-4", leftMinusTag+","+leftTag+","+rightPlusTag));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-5", leftTag+","+rightTag+","+rightPlusTag));
				
				
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-1a-dist", leftMinusA+","+leftA+","+rightA+","+rightPlusA+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-2a-dist", leftMinusA+","+leftA+","+rightA+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-3a-dist", leftMinusA+","+rightA+","+rightA+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-4a-dist", leftMinusA+","+leftA+","+rightPlusA+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-5a-dist", leftA+","+rightA+","+rightPlusA+attDist));
				
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-1a", leftMinusA+","+leftA+","+rightA+","+rightPlusA));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-2a", leftMinusA+","+leftA+","+rightA));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-3a", leftMinusA+","+rightA+","+rightA));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-4a", leftMinusA+","+leftA+","+rightPlusA));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-1-5a", leftA+","+rightA+","+rightPlusA));
				
				//l,l+1,r-1,r
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-1-dist", leftTag+","+leftPlusTag+","+rightMinusTag+","+rightTag+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-2-dist", leftTag+","+rightMinusTag+","+rightTag+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-3-dist", leftTag+","+leftPlusTag+","+rightTag+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-4-dist", leftPlusTag+","+rightMinusTag+","+rightTag+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-5-dist", leftTag+","+leftPlusTag+","+rightMinusTag+attDist));
				
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-1", leftTag+","+leftPlusTag+","+rightMinusTag+","+rightTag));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-2", leftTag+","+rightMinusTag+","+rightTag));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-3", leftTag+","+leftPlusTag+","+rightTag));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-4", leftPlusTag+","+rightMinusTag+","+rightTag));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-5", leftTag+","+leftPlusTag+","+rightMinusTag));
				
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-1a-dist", leftA+","+leftPlusA+","+rightMinusA+","+rightA+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-2a-dist", leftA+","+rightMinusA+","+rightA+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-3a-dist", leftA+","+leftPlusA+","+rightA+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-4a-dist", leftPlusA+","+rightMinusA+","+rightA+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-5a-dist", leftA+","+leftPlusA+","+rightMinusA+attDist));
				
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-1a", leftA+","+leftPlusA+","+rightMinusA+","+rightA));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-2a", leftA+","+rightMinusA+","+rightA));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-3a", leftA+","+leftPlusA+","+rightA));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-4a", leftPlusA+","+rightMinusA+","+rightA));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-2-5a", leftA+","+leftPlusA+","+rightMinusA));
				
				//l-1,l,r-1,r
				//l,l+1,r,r+1
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-3-1-dist", leftMinusTag+","+leftTag+","+rightMinusTag+","+rightTag+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-3-1", leftMinusTag+","+leftTag+","+rightMinusTag+","+rightTag));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-3-1a-dist", leftMinusA+","+leftA+","+rightMinusA+","+rightA+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-3-1a", leftMinusA+","+leftA+","+rightMinusA+","+rightA));
				
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-4-1-dist", leftTag+","+leftPlusTag+","+rightTag+","+rightPlusTag+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-4-1", leftTag+","+leftPlusTag+","+rightTag+","+rightPlusTag));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-4-1a-dist", leftA+","+leftPlusA+","+rightA+","+rightPlusA+attDist));
				featureList.add(this._param_g.toFeature(network, FeaType.contextual.name(), "contextual-4-1a", leftA+","+leftPlusA+","+rightA+","+rightPlusA));
				
				
				for(int i=leftIndex+1;i<rightIndex;i++){
					featureList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-1", leftTag+","+sent.get(i).getTag()+","+rightTag+attDist));
					featureList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-2", leftTag+","+sent.get(i).getTag()+","+rightTag));
					featureList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-3", leftA+","+sent.get(i).getATag()+","+rightA+attDist));
					featureList.add(this._param_g.toFeature(network, FeaType.inbetween.name(), "inbetween-4", leftA+","+sent.get(i).getATag()+","+rightA));
				}
				
				if(isPipe || entityFeature){
					String he = sent.get(headIndex).getEntity();
					he = he.length() > 1? he.substring(2) : he;
					String me = sent.get(modifierIndex).getEntity();
					me = me.length() > 1? me.substring(2) : me;
					featureList.add(this._param_g.toFeature(network, FeaType.pipe.name(), "entity",  he+":"+me+":"+headTag+":"+modifierTag));
					featureList.add(this._param_g.toFeature(network, FeaType.pipe.name(), "entity-word",	he+":"+me+":"+headWord+":"+modifierWord));
				}
			}
			
			
			/** Adding neural features ***/
			if (NetworkConfig.USE_NEURAL_FEATURES) {
				String lw = sent.get(leftIndex).getName().toLowerCase();
				String rw = sent.get(rightIndex).getName().toLowerCase();
				String lt = sent.get(leftIndex).getTag();
				String rt = sent.get(rightIndex).getTag();
				if (windowSize == 1)
					featureList.add(this._param_g.toFeature(network, FeaType.neural_1.name(), att, lw + insep + rw + outsep + 
																									lt + insep + rt));
				else if (windowSize == 3) {
					String llw = leftIndex == 0? "<PAD>" : sent.get(leftIndex - 1).getName().toLowerCase();
					String lrw = sent.get(leftIndex + 1).getName().toLowerCase();
					String rlw = sent.get(rightIndex - 1).getName().toLowerCase();
					String rrw = rightIndex < sent.length()-1? sent.get(rightIndex+1).getName().toLowerCase() : "<PAD>";
					String llt = leftIndex == 0? "<PAD>" : sent.get(leftIndex - 1).getTag();
					String lrt = sent.get(leftIndex + 1).getTag();
					String rlt = sent.get(rightIndex - 1).getTag();
					String rrt = rightIndex < sent.length()-1? sent.get(rightIndex+1).getTag() : "<PAD>";
					featureList.add(this._param_g.toFeature(network, FeaType.neural_1.name(), att, llw + insep + lw + insep + lrw + insep + rlw + insep + rw + insep + rrw + outsep +
																							llt + insep + lt + insep + lrt + insep + rlt + insep + rt + insep + rrt));
				} else {
					throw new RuntimeException("Unknown window size: " + windowSize);
				}
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
		
		featureList.add(this._param_g.toFeature(network, FeaType.label.name(), "LABEL-dir", label+"&"+att));
		featureList.add(this._param_g.toFeature(network, FeaType.label.name(), "LABEL", label));
		for(int i = 0; i < 2; i++) {
			String suff = i < 1 ? "&"+att : "";
			suff = "&"+label+suff;
		 
		 	featureList.add(this._param_g.toFeature(network, FeaType.label.name(), "LABEL-W-WP-suff", w+" "+wP+suff));
		 	featureList.add(this._param_g.toFeature(network, FeaType.label.name(), "LABEL-WP-suff", wP+suff));
		 	featureList.add(this._param_g.toFeature(network, FeaType.label.name(), "LABEL-WM-WP-suff", wPm1+" "+wP+suff));
		 
		 	featureList.add(this._param_g.toFeature(network, FeaType.label.name(), "LABEL-WP-WPT-suff", wP+" "+wPp1+suff));
		 	featureList.add(this._param_g.toFeature(network, FeaType.label.name(), "LABEL-WM-WP-WPT-suff", wPm1+" "+wP+" "+wPp1+suff));
		 	featureList.add(this._param_g.toFeature(network, FeaType.label.name(), "LABEL-W-suff", w+suff));
		}
	}
	
}

package com.statnlp.entity.semi;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.utils.Extractor;
import com.statnlp.entity.semi.SemiCRFNetworkCompiler.NodeType;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.neural.NeuralConfig;

public class SemiCRFFeatureManager extends FeatureManager {
	
	private static final long serialVersionUID = 6510131496948610905L;
	

	public enum FeatureType{
		local, pairwise,semi,neural,cheat
	}
	
	private String OUT_SEP = NeuralConfig.OUT_SEP; 
	private String IN_SEP = NeuralConfig.IN_SEP;
	private boolean CHEAT = false;
	private boolean nonMarkovFeature;
	private int maxSegLength;
	
	public SemiCRFFeatureManager(GlobalNetworkParam param_g, int maxSegL, boolean nonMarkov) {
		super(param_g);
		this.maxSegLength = maxSegL;
		this.nonMarkovFeature = nonMarkov;
	}
	
	@Override
	protected FeatureArray extract_helper(Network net, int parent_k, int[] children_k) {
		SemiCRFNetwork network = (SemiCRFNetwork)net;
		SemiCRFInstance instance = (SemiCRFInstance)network.getInstance();
		
		Sentence sent = instance.getInput();
		
		
		int[] parent_arr = network.getNodeArray(parent_k);
		int parentPos = parent_arr[0] - 1;
		
		NodeType parentType = NodeType.values()[parent_arr[2]];
		int parentLabelId = parent_arr[1];
		
		//since unigram, root is not needed
		if(parentType == NodeType.LEAF || parentType == NodeType.ROOT){
			return FeatureArray.EMPTY;
		}
		
		int[] child_arr = network.getNodeArray(children_k[0]);
		int childPos = child_arr[0] + 1 - 1;
		NodeType childType = NodeType.values()[child_arr[2]];
		int childLabelId = child_arr[1];

		if(CHEAT){
			int instanceId = Math.abs(instance.getInstanceId());
			int cheatFeature = _param_g.toFeature(network, FeatureType.cheat.name(), parentLabelId+"", instanceId+" "+parentPos);
			return new FeatureArray(new int[]{cheatFeature});
		}
		
		FeatureArray fa = null;
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		
		int start = childPos;
		if(parentPos==0 || childType==NodeType.LEAF ) start = childPos;
//		String childWord = childPos>=0? sent.get(childPos).getName():"STR";
//		String childTag = childPos>=0? sent.get(childPos).getTag():"STR";
		String currEn = Label.get(parentLabelId).getForm();
		//word-level features
		for(int i=start;i<=parentPos;i++){
			int pos = i;
			String lw = pos>0? sent.get(pos-1).getName():"STR";
			String ls = pos>0? shape(lw):"STR_SHAPE";
			String lt = pos>0? sent.get(pos-1).getTag():"STR";
			String llw = pos==0? "STR1": pos==1? "STR":sent.get(pos-2).getName();
			String llt = pos==0? "STR1": pos==1? "STR":sent.get(pos-2).getTag();
			String rw = pos<sent.length()-1? sent.get(pos+1).getName():"END";
			String rt = pos<sent.length()-1? sent.get(pos+1).getTag():"END";
			String rs = pos<sent.length()-1? shape(rw):"END_SHAPE";
			String rrw = pos==sent.length()-1? "END1": pos==sent.length()-2? "END":sent.get(pos+2).getName();
			String rrt = pos==sent.length()-1? "END1": pos==sent.length()-2? "END":sent.get(pos+2).getTag();
			String currWord = sent.get(pos).getName();
			String currTag = sent.get(pos).getTag();
			String currShape = shape(currWord);
			
			String prevEntity = i==start? Label.get(childLabelId).getForm():Label.get(parentLabelId).getForm();
			
			if(NetworkConfig.USE_NEURAL_FEATURES && !nonMarkovFeature){
				featureList.add(this._param_g.toFeature(network, FeatureType.neural.name(), currEn, llw+IN_SEP+lw+IN_SEP+currWord+IN_SEP+rw+IN_SEP+rrw+OUT_SEP+
																					llt+IN_SEP+lt+IN_SEP+currTag+IN_SEP+rt+IN_SEP+rrt));
			}
			
			/** Features adapted from Jenny Rose Finkel et.al 2009. (Order follows the table)**/
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "EW",  	currEn+":"+currWord));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "ELW",	currEn+":"+lw));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "ERW",	currEn+":"+rw));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "ET",		currEn+":"+currTag));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "ELT",	currEn+":"+lt));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "ERT",	currEn+":"+rt));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "ES",		currEn+":"+currShape));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "ELS",	currEn+":"+ls));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "ERS",	currEn+":"+rs));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "ELT-T",	currEn+":"+lt+","+currTag));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "ELS-S",	currEn+":"+ls+","+currShape));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "ES-RS",	currEn+":"+currShape+","+rs));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "ELW-S",	currEn+":"+lw+","+currShape));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "ES-RW",	currEn+":"+currShape+","+rw));
			/** 5-word window features **/
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "E-5-word",	currEn+":"+llw+","+lw+","+currWord+","+rw+","+rrw));
			/****Add some prefix features******/
			for(int plen = 1;plen<=6;plen++){
				if(currWord.length()>=plen){
					String suff = currWord.substring(currWord.length()-plen, currWord.length());
					featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "E-PATTERN-SUFF-"+plen, currEn+":"+suff));
					String pref = currWord.substring(0,plen);
					featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "E-PATTERN-PREF-"+plen, currEn+":"+pref));
				}
			}
			/*********Pairwise features********/
			featureList.add(this._param_g.toFeature(network,FeatureType.pairwise.name(), "E-prev-E",			prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FeatureType.pairwise.name(), "currW-prevE-currE",	currWord+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FeatureType.pairwise.name(), "prevW-prevE-currE",	lw+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FeatureType.pairwise.name(), "nextW-prevE-currE",	rw+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FeatureType.pairwise.name(), "currT-prevE-currE",	currTag+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FeatureType.pairwise.name(), "prevT-prevE-currE",	lt+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FeatureType.pairwise.name(), "nextT-prevE-currE",	rt+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FeatureType.pairwise.name(), "prevT-currT-prevE-currE",lt+":"+currTag+":"+prevEntity+":"+currEn));	
			featureList.add(this._param_g.toFeature(network,FeatureType.pairwise.name(), "S-LE-E",			currShape+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FeatureType.pairwise.name(), "LS-LE-E",			ls+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FeatureType.pairwise.name(), "RS-LE-E",			rs+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FeatureType.pairwise.name(), "LS-S-LE-E",		ls+":"+currShape+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FeatureType.pairwise.name(), "LS-RS-LE-E",		ls+":"+rs+":"+prevEntity+":"+currEn));
			/** (END) Features adapted from Jenny Rose Finkel et.al 2009 **/
		}
		
		
		/** add non-markovian neural features **/
		if(NetworkConfig.USE_NEURAL_FEATURES && nonMarkovFeature){
			StringBuilder segPhrase = new StringBuilder(sent.get(start).getName());
			for(int pos=start+1;pos<=parentPos; pos++){
				String w = sent.get(pos).getName();
				segPhrase.append(IN_SEP);
				segPhrase.append(w);
			}
			int segL = parentPos-start+1;
			for(int t=0;t<(maxSegLength-segL);t++) { segPhrase.append(IN_SEP);segPhrase.append("<UNK>");}
			featureList.add(this._param_g.toFeature(network, FeatureType.neural.name(), currEn, segPhrase.toString()));
		}
		
		
		/**  End (non-markovian neural features)**/
		
		
		/**Features from semi CRF, adapted from Sunita Sarawagi et.al 2004***/
		StringBuilder segPhrase = new StringBuilder();
		StringBuilder shapePhrase = new StringBuilder();
		for(int pos=start;pos<=parentPos; pos++){
			String w = sent.get(pos).getName();
			segPhrase.append(w+":");
			shapePhrase.append(shape(w)+":");
		}
		featureList.add(this._param_g.toFeature(network,FeatureType.semi.name(), "word-phrase-E",	segPhrase.toString()+"::"+currEn));
		featureList.add(this._param_g.toFeature(network,FeatureType.semi.name(), "shape-phrase-E",	shapePhrase.toString()+"::"+currEn));
		
		String segLw = start>0? sent.get(start-1).getName():"STR";
		String segLLw = start==0? "STR1": start==1? "STR":sent.get(start-2).getName();
		String segLLLw = start==0? "STR2": start==1? "STR1": start==2? "STR":sent.get(start-3).getName();
		String segRw = parentPos<sent.length()-1? sent.get(parentPos+1).getName():"END";
		String segRRw = parentPos==sent.length()-1? "END1": parentPos==sent.length()-2? "END":sent.get(parentPos+2).getName();
		String segRRRw = parentPos==sent.length()-1? "END2": parentPos==sent.length()-2? "END1": parentPos==sent.length()-3?"END":sent.get(parentPos+3).getName();
		
		/** 3-word/pattern window features before and after the segment**/
		featureList.add(this._param_g.toFeature(network,FeatureType.semi.name(), "3-word-BF-E",		segLLLw+","+segLLw+","+segLw+":"+currEn));
		featureList.add(this._param_g.toFeature(network,FeatureType.semi.name(), "3-word-AF-E",		segRw+","+segRRw+","+segRRRw+":"+currEn));
		featureList.add(this._param_g.toFeature(network,FeatureType.semi.name(), "3-word-BF-E",		shape(segLLLw)+","+shape(segLLw)+","+shape(segLw)+":"+currEn));
		featureList.add(this._param_g.toFeature(network,FeatureType.semi.name(), "3-word-AF-E",		shape(segRw)+","+shape(segRRw)+","+shape(segRRRw)+":"+currEn));
		
		/** Start and end features. **/
		String startWord = sent.get(start).getName();
		String startTag = sent.get(start).getTag();
		featureList.add(this._param_g.toFeature(network,FeatureType.semi.name(), "STR-W",  	currEn+":"+startWord));
		featureList.add(this._param_g.toFeature(network,FeatureType.semi.name(), "STR-T",	currEn+":"+startTag));
		String endW = sent.get(parentPos).getName();
		String endT = sent.get(parentPos).getTag();
		featureList.add(this._param_g.toFeature(network,FeatureType.semi.name(), "END-W",  	currEn+":"+endW));
		featureList.add(this._param_g.toFeature(network,FeatureType.semi.name(), "END-T",	currEn+":"+endT));
		/** needs to be modified maybe ***/
		
		/**Segemetn length features **/
		int lenOfSeg = parentPos-start+1;
		featureList.add(this._param_g.toFeature(network,FeatureType.semi.name(), "STR-W-LEN",  	currEn+":"+startWord+"&L:"+lenOfSeg));
		featureList.add(this._param_g.toFeature(network,FeatureType.semi.name(), "END-W-LEN",  	currEn+":"+endW+"&L:"+lenOfSeg));
		/**(END) Features from semi CRF, adapted from Sunita Sarawagi et.al 2004**/
		
		
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

	private String shape(String word){
		return Extractor.wordShapeOf(word);
	}
}

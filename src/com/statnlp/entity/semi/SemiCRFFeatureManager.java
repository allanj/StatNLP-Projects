package com.statnlp.entity.semi;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.entity.semi.SemiCRFNetworkCompiler.NodeType;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;

public class SemiCRFFeatureManager extends FeatureManager {
	
	private static final long serialVersionUID = 6510131496948610905L;
	

	public enum FeatureType{
		local
	}
	
	public int unigramWindowSize = 5;
	public int substringWindowSize = 5;

	public SemiCRFFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}
	
	@Override
	protected FeatureArray extract_helper(Network net, int parent_k, int[] children_k) {
		SemiCRFNetwork network = (SemiCRFNetwork)net;
		SemiCRFInstance instance = (SemiCRFInstance)network.getInstance();
		
		Sentence sent = instance.getInput();
		
		
		int[] parent_arr = network.getNodeArray(parent_k);
		int parentPos = parent_arr[0];
		
		NodeType parentType = NodeType.values()[parent_arr[1]];
		int parentLabelId = parent_arr[2];
		
		//since unigram, root is not needed
		if(parentType == NodeType.LEAF || parentType == NodeType.ROOT){
			return FeatureArray.EMPTY;
		}
		
		int[] child_arr = network.getNodeArray(children_k[0]);
		int childPos = child_arr[0];
		NodeType childType = NodeType.values()[child_arr[1]];
		int childLabelId = child_arr[2];

		FeatureArray fa = null;
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		
		int start = childPos + 1;
		if(parentPos==0 || childType==NodeType.LEAF ) start = childPos;
		for(int i=start;i<=parentPos;i++){
			String lw = i>0? sent.get(i-1).getName():"STR";
			String lt = i>0? sent.get(i-1).getTag():"STR";
			String rw = i<sent.length()-1? sent.get(i+1).getName():"END";
			String rt = i<sent.length()-1? sent.get(i+1).getTag():"END";
			String currWord = sent.get(i).getName();
			String currTag = sent.get(i).getTag();
			String currEn = Label.get(parentLabelId).getForm();
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "EW",  	currEn+":"+currWord));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "ET",		currEn+":"+currTag));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "ELW",	currEn+":"+lw));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "ELT",	currEn+":"+lt));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "ERW",	currEn+":"+rw));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "ERT",	currEn+":"+rt));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "ELT-T",	currEn+":"+lt+","+currTag));
			/****Add some prefix features******/
			for(int plen = 1;plen<=6;plen++){
				if(currWord.length()>=plen){
					String suff = currWord.substring(currWord.length()-plen, currWord.length());
					featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "E-PATTERN-SUFF-"+plen, currEn+":"+suff));
					String pref = currWord.substring(0,plen);
					featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "E-PATTERN-PREF-"+plen, currEn+":"+pref));
				}
			}
		}
		
		for(int i=start;i<parentPos;i++){
			String lw = i>0? sent.get(i-1).getName():"STR";
			String lt = i>0? sent.get(i-1).getTag():"STR";
			String rw = i<sent.length()-1? sent.get(i+1).getName():"END";
			String rt = i<sent.length()-1? sent.get(i+1).getTag():"END";
			String currWord = sent.get(i).getName();
			String currTag = sent.get(i).getTag();
			String prevEntity = Label.get(parentLabelId).getForm();
			String currEn = Label.get(parentLabelId).getForm();
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "E-prev-E",prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "currW-prevE-currE",currWord+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "prevW-prevE-currE",lw+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "nextW-prevE-currE",rw+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "currT-prevE-currE",currTag+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "prevT-prevE-currE",lt+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "nextT-prevE-currE",rt+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FeatureType.local.name(), "prevT-currT-prevE-currE",lt+":"+currTag+":"+prevEntity+":"+currEn));	
		}
		
		
		
		
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

}

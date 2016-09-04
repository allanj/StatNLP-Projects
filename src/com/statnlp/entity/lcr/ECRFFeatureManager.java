package com.statnlp.entity.lcr;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.utils.Extractor;
import com.statnlp.entity.Entity;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.neural.NeuralConfig;

public class ECRFFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 376931974939202432L;

	public enum FEATYPE {word,
		tag,
		prev_tag,
		prev_word,
		shape,
		prev_shape,
		prefix, suffix,transition,
		head_word,
		head_tag,
		head_token,
		dep_word_label,
		dep_tag_label,
		neural};
	protected boolean isPipeLine; 
	protected boolean useDepF; 
//	private String OUT_SEP = NeuralConfig.OUT_SEP; 
	private String IN_SEP = NeuralConfig.IN_SEP;
	private int prefixLength = 3;
	
	public ECRFFeatureManager(GlobalNetworkParam param_g, boolean isPipeLine, boolean depf) {
		super(param_g);
		this.isPipeLine = isPipeLine;
		this.useDepF = depf;
	}
	
	//
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		// TODO Auto-generated method stub
		ECRFInstance inst = ((ECRFInstance)network.getInstance());
		//int instanceId = inst.getInstanceId();
		Sentence sent = inst.getInput();
		long node = network.getNode(parent_k);
		int[] nodeArr = NetworkIDMapper.toHybridNodeArray(node);
		
		FeatureArray fa = null;
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		
		int pos = nodeArr[0]-1;
		if(pos<0 || pos >= inst.size())
			return FeatureArray.EMPTY;
			
		int eId = nodeArr[1];
		int[] child = NetworkIDMapper.toHybridNodeArray(network.getNode(children_k[0]));
		int childEId = child[1];
//		int childPos = child[0]-1;
		
		String lw = pos>0? sent.get(pos-1).getName():"STR";
		String ls = pos>0? shape(lw):"STR_SHAPE";
		String lt = pos>0? sent.get(pos-1).getTag():"STR";
		String llw = pos==0? "STR1": pos==1? "STR":sent.get(pos-2).getName();
//		String llt = pos==0? "STR1": pos==1? "STR":sent.get(pos-2).getTag();
		
		String rw = pos<sent.length()-1? sent.get(pos+1).getName():"END";
//		String rt = pos<sent.length()-1? sent.get(pos+1).getTag():"END";
//		String rs = pos<sent.length()-1? shape(rw):"END_SHAPE";
		String rrw = pos==sent.length()-1? "END1": pos==sent.length()-2? "END":sent.get(pos+2).getName();
//		String rrt = pos==sent.length()-1? "END1": pos==sent.length()-2? "END":sent.get(pos+2).getTag();
		
		String currWord = inst.getInput().get(pos).getName();
		String currTag = inst.getInput().get(pos).getTag();
		String currShape = shape(currWord);
//		String childWord = childPos>=0? inst.getInput().get(childPos).getName():"STR";
//		String childTag = childPos>=0? inst.getInput().get(childPos).getTag():"STR";
		
		
		
		
		String currEn = Entity.get(eId).getForm();
		String prevEntity = Entity.get(childEId).getForm();
		if(NetworkConfig.USE_NEURAL_FEATURES){
//			featureList.add(this._param_g.toFeature(network, FEATYPE.neural.name(), currEn, llw+IN_SEP+lw+IN_SEP+currWord+IN_SEP+rw+IN_SEP+rrw+OUT_SEP+
//																				llt+IN_SEP+lt+IN_SEP+currTag+IN_SEP+rt+IN_SEP+rrt));
			featureList.add(this._param_g.toFeature(network, FEATYPE.neural.name(), currEn, llw+IN_SEP+lw+IN_SEP+currWord+IN_SEP+rw+IN_SEP+rrw));
		}
		
		/** Features adapted from Jenny Rose Finkel et.al 2009. (Order follows the table)**/
		featureList.add(this._param_g.toFeature(network,FEATYPE.word.name(), 		currEn,	currWord));
		featureList.add(this._param_g.toFeature(network,FEATYPE.prev_word.name(), 	currEn,	lw));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "ERW",	currEn+":"+rw));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tag.name(), 		currEn,	currTag));
		featureList.add(this._param_g.toFeature(network,FEATYPE.prev_tag.name(), 	currEn,	lt));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "ERT",	currEn+":"+rt));
		featureList.add(this._param_g.toFeature(network,FEATYPE.shape.name(), 		currEn,	currShape));
		featureList.add(this._param_g.toFeature(network,FEATYPE.prev_shape.name(), 	currEn,	ls));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "ERS",	currEn+":"+rs));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "ELT-T",	currEn+":"+lt+","+currTag));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "ELS-S",	currEn+":"+ls+","+currShape));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "ES-RS",	currEn+":"+currShape+","+rs));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "ELW-S",	currEn+":"+lw+","+currShape));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "ES-RW",	currEn+":"+currShape+","+rw));
		/** 5-word window features **/
//		featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "E-5-word",	currEn+":"+llw+","+lw+","+currWord+","+rw+","+rrw));
		/****Add some prefix features******/
		for(int plen = 1;plen<=prefixLength;plen++){
			if(currWord.length()>=plen){
				String suff = currWord.substring(currWord.length()-plen, currWord.length());
				featureList.add(this._param_g.toFeature(network,FEATYPE.suffix.name()+"-"+plen, currEn, suff));
				String pref = currWord.substring(0,plen);
				featureList.add(this._param_g.toFeature(network,FEATYPE.prefix.name()+"-"+plen, currEn, pref));
			}
		}
		/*********Pairwise features********/
		featureList.add(this._param_g.toFeature(network,FEATYPE.transition.name(), currEn,	prevEntity));
		
//		featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "currW-prevE-currE",	currWord+":"+prevEntity+":"+currEn));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "prevW-prevE-currE",	lw+":"+prevEntity+":"+currEn));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "nextW-prevE-currE",	rw+":"+prevEntity+":"+currEn));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "currT-prevE-currE",	currTag+":"+prevEntity+":"+currEn));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "prevT-prevE-currE",	lt+":"+prevEntity+":"+currEn));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "nextT-prevE-currE",	rt+":"+prevEntity+":"+currEn));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "prevT-currT-prevE-currE",lt+":"+currTag+":"+prevEntity+":"+currEn));	
//		featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "S-LE-E",		currShape+":"+prevEntity+":"+currEn));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "LS-LE-E",		ls+":"+prevEntity+":"+currEn));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "RS-LE-E",		rs+":"+prevEntity+":"+currEn));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "LS-S-LE-E",		ls+":"+currShape+":"+prevEntity+":"+currEn));
//		featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "LS-RS-LE-E",	ls+":"+rs+":"+prevEntity+":"+currEn));
		/** (END) Features adapted from Jenny Rose Finkel et.al 2009 **/
		
		
//		if(true){
		if(this.isPipeLine || this.useDepF){
			/** This option is for the pipeline from the dependency result to named entity recogntion.**/
			int currHeadIndex = sent.get(pos).getHeadIndex();
			String currHead = currHeadIndex>=0? sent.get(currHeadIndex).getName():"STR";
			String currHeadTag = currHeadIndex>=0? sent.get(currHeadIndex).getTag():"STR";
			String currDepLabel = currHeadIndex>=0? sent.get(currHeadIndex).getDepLabel():"NOLABEL";
			//This is the features that really help the model: most important features
			if(currDepLabel==null || currDepLabel.equals("null")) throw new RuntimeException("The depenency label is null?");
			featureList.add(this._param_g.toFeature(network,FEATYPE.head_word.name(), 		currEn, currWord+"& head:"+currHead));
			featureList.add(this._param_g.toFeature(network,FEATYPE.head_tag.name(), 		currEn, currTag+"& head:"+currHeadTag)); //the most powerful one
			featureList.add(this._param_g.toFeature(network,FEATYPE.dep_word_label.name(), 	currEn, currWord+"& head:"+currHead+"& label:"+currDepLabel));
			featureList.add(this._param_g.toFeature(network,FEATYPE.dep_tag_label.name(), 	currEn, currTag+"& head:"+currHeadTag+"& label:"+currDepLabel));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.dep_word_label.name(), 	currEn, currWord+"& label:"+currDepLabel));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.dep_tag_label.name(), 	currEn, currTag+"& label:"+currDepLabel));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.head_token.name(), 		currEn, currDepLabel));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.head_token.name(), 	currEn, currTag+"& head:"+currHead+" & headt:"+currHeadTag));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.head_word.name(), 	currEn, currHead));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.head_tag.name(), 	currEn, currHeadTag));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.head_token.name(), 	entities[eId], currHeadTag.charAt(0)+""));
			
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
	
	private String shape(String word){
		return Extractor.wordShapeOf(word);
	}

}

package com.statnlp.projects.entity.dcrf;

import java.util.ArrayList;
import java.util.Arrays;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.projects.dep.utils.Extractor;
import com.statnlp.projects.entity.dcrf.DCRFNetworkCompiler.NODE_TYPES;

public class DCRFFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 376931974939202432L;

	public enum FEATYPE {local,entity, transition, emission,cheat};
	private boolean CHEAT = false;
	
	public DCRFFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}
	//
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		// TODO Auto-generated method stub
		DCRFInstance inst = ((DCRFInstance)network.getInstance());
		int instanceId = inst.getInstanceId();
		Sentence sent = inst.getInput();
		long node = network.getNode(parent_k);
		int[] nodeArr = NetworkIDMapper.toHybridNodeArray(node);
		
		FeatureArray fa = null;
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		
		int pos = nodeArr[0]-1;
		int nodeType = nodeArr[4];
		if(pos<0 || pos >= inst.size() || nodeType==NODE_TYPES.ROOT.ordinal())
			return FeatureArray.EMPTY;
		
		if(nodeType == NODE_TYPES.entLEAF.ordinal() || nodeType == NODE_TYPES.tagLEAF.ordinal() || nodeType == NODE_TYPES.ROOT.ordinal()){
			System.err.println("inst size: "+inst.size()+" "+Arrays.toString(nodeArr)+" isLabel:"+inst.isLabeled());
			throw new RuntimeException("cannot be leaf or root node in feature extraction");
		}
		
		int[] child = NetworkIDMapper.toHybridNodeArray(network.getNode(children_k[0]));
		int childEId = child[1];
		int childPos = child[0]-1;
		
		int[] tag_child = NetworkIDMapper.toHybridNodeArray(network.getNode(children_k[1]));
		int childTagId = tag_child[1];
		int tagPos = tag_child[0]-1;
		
		String currWord = inst.getInput().get(pos).getName();
		if(nodeType==NODE_TYPES.entNODE.ordinal()){
			int eId = nodeArr[1];
			
			String lw = pos>0? sent.get(pos-1).getName():"STR";
			String lt = pos>0? Tag.TAGS_INDEX.get(childTagId).getForm():"STR";
			String rw = pos<sent.length()-1? sent.get(pos+1).getName():"END";
			//String rt = pos<sent.length()-1? sent.get(pos+1).getTag():"END";
			
			
			//String currTag = inst.getInput().get(pos).getTag();
			
			String currEn = DEntity.ENTS_INDEX.get(eId).getForm();
			featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "EW",  	currEn+":"+currWord));
			//featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "ET",	currEn+":"+currTag));
			featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "ELW",	currEn+":"+lw));
			featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "ELT",	currEn+":"+lt));
			featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "ERW",	currEn+":"+rw));
			//featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "ERT",	currEn+":"+rt));
			//featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "ELT-T",	currEn+":"+lt+","+currTag));
			/****Add some prefix features******/
			for(int plen = 1;plen<=6;plen++){
				if(currWord.length()>=plen){
					String suff = currWord.substring(currWord.length()-plen, currWord.length());
					featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "E-PATTERN-SUFF-"+plen, currEn+":"+suff));
					String pref = currWord.substring(0,plen);
					featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "E-PATTERN-PREF-"+plen, currEn+":"+pref));
				}
			}
			
			
			String prevEntity =  DEntity.ENTS_INDEX.get(childEId).getForm();

			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-prev-E",prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "currW-prevE-currE",currWord+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "prevW-prevE-currE",lw+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "nextW-prevE-currE",rw+":"+prevEntity+":"+currEn));
			
			//featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "currT-prevE-currE",currTag+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "prevT-prevE-currE",lt+":"+prevEntity+":"+currEn));
			//featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "nextT-prevE-currE",rt+":"+prevEntity+":"+currEn));
			//featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "prevT-currT-prevE-currE",lt+":"+currTag+":"+prevEntity+":"+currEn));
			
		}else if(nodeType==NODE_TYPES.tagNODE.ordinal() ){
			
			int tagId = nodeArr[1];
			String lt = pos>0? Tag.TAGS_INDEX.get(childTagId).getForm():"STR";
			String currTag = Tag.TAGS_INDEX.get(tagId).getForm();
			featureList.add(this._param_g.toFeature(network,FEATYPE.transition.name(), currTag, lt));
			featureList.add(this._param_g.toFeature(network,FEATYPE.transition.name(), "HyperEdge", currTag+":"+lt+":"+DEntity.ENTS_INDEX.get(childEId).getForm()));
			featureList.add(this._param_g.toFeature(network,FEATYPE.transition.name(), "EntityTrans:"+currTag, DEntity.ENTS_INDEX.get(childEId).getForm()));
			featureList.add(this._param_g.toFeature(network,FEATYPE.emission.name(), currWord, currTag));
			
		}
		
//		if(CHEAT){
//			featureList.add(this._param_g.toFeature(network,FEATYPE.cheat.name(), "node", "node id:"+Arrays.toString(nodeArr)+" : "+inst.getInput().get(pos).getName()));
//			if(instanceId>0){
//				//System.err.println(inst.getInput().get(pos).getName()+","+Arrays.toString(nodeArr));
//			}
//		}

		
		
		
		
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

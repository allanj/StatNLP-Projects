package com.statnlp.entity.lcr;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.utils.Extractor;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class ECRFFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 376931974939202432L;

	public enum FEATYPE {entity};
	protected String[] entities;
	protected boolean isPipeLine; 
	
	public ECRFFeatureManager(GlobalNetworkParam param_g, String[] entities, boolean isPipeLine) {
		super(param_g);
		this.entities = entities;
		this.isPipeLine = isPipeLine;
	}
	//
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		// TODO Auto-generated method stub
		ECRFInstance inst = ((ECRFInstance)network.getInstance());
		int instanceId = inst.getInstanceId();
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
		int childPos = child[0]-1;
		
		String lw = pos>0? sent.get(pos-1).getName():"STR";
		String lt = pos>0? sent.get(pos-1).getTag():"STR";
		String rw = pos<sent.length()-1? sent.get(pos+1).getName():"END";
		String rt = pos<sent.length()-1? sent.get(pos+1).getTag():"END";
		
		String currWord = inst.getInput().get(pos).getName();
		String currTag = inst.getInput().get(pos).getTag();
		String childWord = childPos>=0? inst.getInput().get(childPos).getName():"STR";
		String childTag = childPos>=0? inst.getInput().get(childPos).getTag():"STR";
		
		if(!entities[eId].equals("O")){
			boolean corr = true;
			  
			if(entities[childEId].equals("O") && entities[eId].startsWith("I-")) corr = false;
			if(entities[childEId].equals("STR") && entities[eId].startsWith("I-")) corr = false;
			if(entities[eId].startsWith("I-") && entities[childEId].length()>2 && !entities[childEId].substring(2).equals(entities[eId].substring(2))) corr = false;
			if(entities[childEId].startsWith("I-") && entities[eId].startsWith("I-") && childPos==0) corr = false;
			if(corr){
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "EW",entities[eId]+":"+currWord));
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "ET",entities[eId]+":"+currTag));
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "ELW",entities[eId]+":"+lw));
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "ELT",entities[eId]+":"+lt));
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "ERW",entities[eId]+":"+rw));
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "ERT",entities[eId]+":"+rt));
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "ELT-T",entities[eId]+":"+lt+","+currTag));
				/****Add some prefix features******/
				for(int plen = 1;plen<=6;plen++){
					if(currWord.length()>=plen){
						String suff = currWord.substring(currWord.length()-plen, currWord.length());
						featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-PATTERN-SUFF-"+plen, entities[eId]+":"+suff));
						String pref = currWord.substring(0,plen);
						featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-PATTERN-PREF-"+plen, entities[eId]+":"+pref));
					}
				}
				
				String prevEntity = entities[childEId];
				if(entities[eId].startsWith("B-"))
					prevEntity = "ESTR";
				if(prevEntity.equals("ESTR") || (prevEntity.length()>2 &&entities[eId].length()>2 && prevEntity.substring(2).equals(entities[eId].substring(2)))){
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-prev-E",prevEntity+":"+entities[eId]));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "currW-prevE-currE",currWord+":"+prevEntity+":"+entities[eId]));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "prevW-prevE-currE",lw+":"+prevEntity+":"+entities[eId]));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "nextW-prevE-currE",rw+":"+prevEntity+":"+entities[eId]));
					
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "currT-prevE-currE",currTag+":"+prevEntity+":"+entities[eId]));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "prevT-prevE-currE",lt+":"+prevEntity+":"+entities[eId]));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "nextT-prevE-currE",rt+":"+prevEntity+":"+entities[eId]));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "prevT-currT-prevE-currE",lt+":"+rt+":"+prevEntity+":"+entities[eId]));
				}
			}
			
		}
		
		
		
		/*********Pairwise features********/
		
		
		if(this.isPipeLine){
			
			int currHeadIndex = sent.get(pos).getHeadIndex();
			String currHead = currHeadIndex>=0? sent.get(currHeadIndex).getName():"STR";
			String currHeadTag = currHeadIndex>=0? sent.get(currHeadIndex).getEntity():"STR";
			
			
			//This is the features that really help the model: most important features
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "DPE-WH", entities[eId]+":"+currWord+","+currHead));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "DPE-WTHT", entities[eId]+":"+currTag+","+currHeadTag));
			
			
			if(childPos>=0){
				int childHeadIndex = sent.get(childPos).getHeadIndex();
				//remove this feature, just down a bit
				String childHead = childHeadIndex>=0? sent.get(childHeadIndex).getName():"STR";
				String childHeadTag = childHeadIndex>=0? sent.get(childHeadIndex).getTag():"STR";
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "DPE-CWCH", entities[childEId]+":"+childWord+","+childHead));
				featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "DPE-CWTCHT", entities[childEId]+":"+childTag+","+childHeadTag));

				if(childHeadIndex==pos || currHeadIndex==childPos){
					String dir = childHeadIndex==pos? "LEFTDIR":"RIGHTDIR";
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "DPE-WC-CON",entities[eId]+","+entities[childEId]+":"+currWord+","+childWord));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "DPE-WC-CON",entities[eId]+","+entities[childEId]+":"+currWord+","+childWord+":"+dir));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "DPE-WC-CON",entities[eId]+","+entities[childEId]+":"+currTag+","+childTag));
					featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "DPE-WC-CON",entities[eId]+","+entities[childEId]+":"+currTag+","+childTag+":"+dir));
				}
			}
			
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

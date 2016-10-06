package com.statnlp.projects.factorialCRFs;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.projects.factorialCRFs.TFNetworkCompiler.NODE_TYPES;

public class TFFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 376931974939202432L;

	public enum FEATYPE {local,entity, tagging, joint};
	
	public TFFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}
	//
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		// TODO Auto-generated method stub
		TFInstance inst = ((TFInstance)network.getInstance());
		//int instanceId = inst.getInstanceId();
		Sentence sent = inst.getInput();
		long node = network.getNode(parent_k);
		int[] nodeArr = NetworkIDMapper.toHybridNodeArray(node);
		
		FeatureArray fa = null;
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		
		int pos = nodeArr[0]-1;
		if(pos<0 || pos >= inst.size() || nodeArr[1]==NODE_TYPES.TAG_IN_E.ordinal() || nodeArr[1]==NODE_TYPES.E_IN_TAG.ordinal())
			return FeatureArray.EMPTY;
		
		int eId = nodeArr[2];
		//System.err.println(Arrays.toString(nodeArr));
		int[] child = NetworkIDMapper.toHybridNodeArray(network.getNode(children_k[0]));
		
		int childPos = child[0]-1;
		
		if(nodeArr[1]==NODE_TYPES.ENODE.ordinal() || nodeArr[1]==NODE_TYPES.ENODE_HYP.ordinal() || child[1]==NODE_TYPES.ENODE_HYP.ordinal()){
			int childEId = child[2];
			if(child[1]!=NODE_TYPES.ENODE.ordinal()){
				//must be in the ne chain and not tag in e node
				addEntityFeatures(featureList, network, sent, pos, childPos, eId, childEId);
			}else{
				//should be the joint feature here
				addJointFeatures(featureList, network, sent, pos, eId, children_k, false);
			}
		}
		
		if(nodeArr[1]==NODE_TYPES.TNODE.ordinal() || nodeArr[1]==NODE_TYPES.TNODE_HYP.ordinal() || child[1]==NODE_TYPES.TNODE_HYP.ordinal()){
			int childEId = child[2];
			if(child[1]!=NODE_TYPES.TNODE.ordinal()){
				//must be in the pos chain and no e involve
				//System.err.println("POS:"+Arrays.toString(nodeArr)+": child:"+Arrays.toString(child));
				addPOSFeatures(featureList, network, sent, pos, childPos, eId, childEId);
			}else{
				//should be the joint feature here
				addJointFeatures(featureList, network, sent, pos, eId, children_k, true);
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
		return (word);
	}
	
	private void addEntityFeatures(ArrayList<Integer> featureList,Network network, Sentence sent, int pos, int childPos, int eId, int childEId){
		
		if(eId!=(Entity.ENTS.size()+Tag.TAGS.size())){
			String lw = pos>0? sent.get(pos-1).getName():"STR";
			String l2w = lw.equals("STR")? "STR1":pos==1?"STR":sent.get(pos-2).getName();
			String l3w = l2w.equals("STR1")?"STR2":pos==1? "STR1":pos==2? "STR": sent.get(pos-3).getName();
			String rw = pos<sent.length()-1? sent.get(pos+1).getName():"END";
			String r2w = rw.equals("END")? "END1":pos==(sent.length()-2)? "END": sent.get(pos+2).getName();
			String r3w = r2w.equals("END1")?"END2":pos==(sent.length()-2)? "END1":pos==(sent.length()-3)?"END":sent.get(pos+3).getName();
			String currWord = sent.get(pos).getName();
			String currEn = Entity.get(eId).getForm();
			String prevEntity = childPos<0? "O":Entity.get(childEId).getForm();
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "EW",  	currEn+":"+currWord));
			featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "ELW",	currEn+":"+lw));
			featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "EL2W",	currEn+":"+l2w));
			featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "EL3W",	currEn+":"+l3w));
			featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "ERW",	currEn+":"+rw));
			featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "ER2W",	currEn+":"+r2w));
			featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "ER3W",	currEn+":"+r3w));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "E-LWS",	currEn+":"+lshape));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "E-RWS",	currEn+":"+rshape));
			
//			featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "E-WS-RWS",	currEn+":"+currShape+":"+rshape));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "E-WS-LWS",	currEn+":"+currShape+":"+lshape));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "E-WL-WS",	currEn+":"+currShape+":"+lw));
			
			/****Add some prefix features******/
			for(int plen = 1;plen<=6;plen++){
				if(currWord.length()>=plen){
					String suff = currWord.substring(currWord.length()-plen, currWord.length());
					featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "E-PATTERN-SUFF-"+plen, currEn+":"+suff));
					String pref = currWord.substring(0,plen);
					featureList.add(this._param_g.toFeature(network,FEATYPE.local.name(), "E-PATTERN-PREF-"+plen, currEn+":"+pref));
				}
			}
			
			/*********Pairwise features********/
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "E-prev-E",prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "currW-prevE-currE",currWord+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "prevW-prevE-currE",lw+":"+prevEntity+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "nextW-prevE-currE",rw+":"+prevEntity+":"+currEn));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "prevS-prevE-currE",lshape+":"+prevEntity+":"+currEn));
//			featureList.add(this._param_g.toFeature(network,FEATYPE.entity.name(), "nextS-prevE-currE",rshape+":"+prevEntity+":"+currEn));
		}
	}

	private void addPOSFeatures(ArrayList<Integer> featureList, Network network, Sentence sent, int pos, int childPos, int tId, int childtId){
		String currTag = tId==(Entity.ENTS.size()+Tag.TAGS.size())? "END":Tag.get(tId).getForm();
		String currWord = tId==(Entity.ENTS.size()+Tag.TAGS.size())? "END": sent.get(pos).getName();
		String currShape = tId==(Entity.ENTS.size()+Tag.TAGS.size())? "END-SHA": shape(sent.get(pos).getName());
		String prevTag = childPos<0? "STR":Tag.get(childtId).getForm();
		String prevWord = childPos<0? "STR":sent.get(childPos).getName();
		String p2w = prevWord.equals("STR")? "STR1":pos==1?"STR":sent.get(pos-2).getName();
		String p3w = p2w.equals("STR1")?"STR2":pos==1?"STR1":pos==2?"STR":sent.get(pos-3).getName();
		String prevShape = childPos<0? "STR-SHA":shape(sent.get(childPos).getName());
		String n3w = null;
		String nextWord = tId==(Entity.ENTS.size()+Tag.TAGS.size())? "END1": pos==(sent.length()-1)? "END": sent.get(pos+1).getName();
		String n2w = nextWord.equals("END1")? "END2": nextWord.equals("END")? "END1": pos==(sent.length()-2)? "END":sent.get(pos+2).getName();
		if(!n2w.equals("END2"))
			n3w = n2w.equals("END1")?"END2":pos==(sent.length()-2)? "END1":pos==(sent.length()-3)?"END":sent.get(pos+3).getName();
		String nextShape = tId==(Entity.ENTS.size()+Tag.TAGS.size())? "END1-SHA": pos==(sent.length()-1)? "END-SHA": shape(sent.get(pos+1).getName());
		
		featureList.add(this._param_g.toFeature(network,FEATYPE.tagging.name(), "currT-currW", currTag+":"+currWord));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tagging.name(), "currT-prevW", currTag+":"+prevWord));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tagging.name(), "currT-p2W", currTag+":"+p2w));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tagging.name(), "currT-p3W", currTag+":"+p3w));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tagging.name(), "currT-nextW", currTag+":"+nextWord));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tagging.name(), "currT-n2w", currTag+":"+n2w));
		if(n3w!=null)
			featureList.add(this._param_g.toFeature(network,FEATYPE.tagging.name(), "currT-n3w", currTag+":"+n3w));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tagging.name(), "currT-currS", currTag+":"+currShape));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tagging.name(), "currT-prevS", currTag+":"+prevShape));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tagging.name(), "currT-nextS", currTag+":"+nextShape));
		
		featureList.add(this._param_g.toFeature(network,FEATYPE.tagging.name(), "prevT-currT",prevTag+":"+currTag));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tagging.name(), "prevT-currT-currW",prevTag+":"+currTag+":"+currWord));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tagging.name(), "prevT-currT-prevW",prevTag+":"+currTag+":"+prevWord));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tagging.name(), "prevT-currT-nextW",prevTag+":"+currTag+":"+nextWord));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tagging.name(), "prevT-currT-currS",prevTag+":"+currTag+":"+currShape));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tagging.name(), "prevT-currT-prevS",prevTag+":"+currTag+":"+prevShape));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tagging.name(), "prevT-currT-nextS",prevTag+":"+currTag+":"+nextShape));
	}
	
	private void addJointFeatures(ArrayList<Integer> featureList, Network network, Sentence sent, int pos, int paId, int[] children_k, boolean paTchildE){
		int[] first_child = NetworkIDMapper.toHybridNodeArray(network.getNode(children_k[0]));
		if(first_child[2]!=paId){
			throw new RuntimeException("These two should be the same");
		}
		String currWord = sent.get(pos).getName();
		String prevWord = pos==0? "STR":sent.get(pos-1).getName();
		String nextWord = pos==(sent.length()-1)? "END":sent.get(pos+1).getName();
		String currShape = shape(sent.get(pos).getName());
		String prevShape = pos==0? "STR-SHA":shape(sent.get(pos-1).getName());
		String nextShape = pos==(sent.length()-1)? "END-SHA":shape(sent.get(pos+1).getName());
		
		for(int k=1;k<children_k.length;k++){
			int[] k_child = NetworkIDMapper.toHybridNodeArray(network.getNode(children_k[k]));
			String currTag = paTchildE? Tag.get(paId).getForm():Tag.get(k_child[2]).getForm();
			String currEn = paTchildE? Entity.get(k_child[2]).getForm():Entity.get(paId).getForm();
			featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "Tag-Entity", currTag+":"+currEn));
			featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "Tag-Entity-Word", currTag+":"+currEn+":"+currWord));
			featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "Tag-Entity-prevW", currTag+":"+currEn+":"+prevWord));
			featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "Tag-Entity-nextW", currTag+":"+currEn+":"+nextWord));
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "Tag-Entity-WS", currTag+":"+currEn+":"+currShape));
			featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "Tag-Entity-prevWS", currTag+":"+currEn+":"+prevShape));
			featureList.add(this._param_g.toFeature(network,FEATYPE.joint.name(), "Tag-Entity-nextWS", currTag+":"+currEn+":"+nextShape));
		}
		
	}

}

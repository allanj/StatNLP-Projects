package com.statnlp.projects.nndcrf.factorialCRFs;

import java.util.ArrayList;
import java.util.Arrays;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.projects.nndcrf.factorialCRFs.TFNetworkCompiler.NODE_TYPES;

public class GRMMFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 376931974939202432L;

	public enum FEATYPE {grmm, entity_joint, tag_joint};
	
	public GRMMFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}
	
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		
		TFInstance inst = ((TFInstance)network.getInstance());
		Sentence sent = inst.getInput();
		long node = network.getNode(parent_k);
		int[] nodeArr = NetworkIDMapper.toHybridNodeArray(node);
		
		FeatureArray fa = null;
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		
		int pos = nodeArr[0]-1;
		if(pos<0 || pos >= inst.size() )
			return FeatureArray.EMPTY;
		
		int eId = nodeArr[2];
		
		
		if (nodeArr[1] == NODE_TYPES.ENODE.ordinal()) {
			String[] fs = sent.get(pos).getFS();
			for (String f : fs)
				featureList.add(this._param_g.toFeature(network, FEATYPE.grmm.name(), Entity.get(eId).getForm(), f));
			 addJointFeatures(featureList, network, sent, pos, eId, parent_k, children_k, false);
		}

		if (nodeArr[1] == NODE_TYPES.TNODE.ordinal()) {
			String[] fs = sent.get(pos).getFS();
			for (String f : fs)
				featureList.add(this._param_g.toFeature(network, FEATYPE.grmm.name(), Tag.get(eId).getForm(), f));
			 addJointFeatures(featureList, network, sent, pos, eId, parent_k, children_k, true);
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
	
	
	private void addJointFeatures(ArrayList<Integer> featureList, Network network, Sentence sent, int pos, int paId, int parent_k, int[] children_k, boolean paTchildE){
		//TFNetwork tfnetwork = (TFNetwork)network;
		if(children_k.length!=1)
			throw new RuntimeException("The joint features should only have one children also");
		String currLabel = paTchildE? Tag.get(paId).getForm():Entity.get(paId).getForm();
		int jointFeatureIdx = -1; 
		int[] arr = null;
		int nodeType = -1;
		if(!paTchildE){
			//current it's NE structure, need to refer to Tag node.
			nodeType = NODE_TYPES.TNODE.ordinal();
			for(int t=0;t<Tag.TAGS_INDEX.size();t++){
				String tag =  Tag.get(t).getForm();
				arr = new int[]{pos+1, nodeType, t};
				long unlabeledDstNode = NetworkIDMapper.toHybridNodeID(arr);
				TFNetwork unlabeledNetwork = (TFNetwork)network.getUnlabeledNetwork();
				int unlabeledDstNodeIdx = Arrays.binarySearch(unlabeledNetwork.getAllNodes(), unlabeledDstNode);
				if(unlabeledDstNodeIdx>=0){
					jointFeatureIdx = this._param_g.toFeature(network, FEATYPE.entity_joint.name(), currLabel, tag);
					network.putJointFeature(parent_k, jointFeatureIdx, unlabeledDstNodeIdx);
					featureList.add(jointFeatureIdx);
				}
			}
			
		}else{
			//current it's POS structure, need to refer to Entity node
			nodeType = NODE_TYPES.ENODE.ordinal();
			for(int e=0; e<Entity.ENTS_INDEX.size(); e++){
				String entity = Entity.get(e).getForm();
				arr = new int[]{pos+1, nodeType, e};
				long unlabeledDstNode = NetworkIDMapper.toHybridNodeID(arr);
				TFNetwork unlabeledNetwork = (TFNetwork)network.getUnlabeledNetwork();
				int unlabeledDstNodeIdx = Arrays.binarySearch(unlabeledNetwork.getAllNodes(), unlabeledDstNode);
				if(unlabeledDstNodeIdx>=0){
					jointFeatureIdx = this._param_g.toFeature(network, FEATYPE.tag_joint.name(), currLabel, entity);
					featureList.add(jointFeatureIdx);
					network.putJointFeature(parent_k, jointFeatureIdx, unlabeledDstNodeIdx);
				}
			}
			
		}
			
		
	}
	

}

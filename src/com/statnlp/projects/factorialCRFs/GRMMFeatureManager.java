package com.statnlp.projects.factorialCRFs;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.neural.NeuralConfig;
import com.statnlp.projects.factorialCRFs.TFNetworkCompiler.NODE_TYPES;

public class GRMMFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 376931974939202432L;

	public enum FEATYPE {grmm, neural_1, neural_2};
	private String IN_SEP = NeuralConfig.IN_SEP;
	
	public GRMMFeatureManager(GlobalNetworkParam param_g) {
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
		
		
		if(nodeArr[1]==NODE_TYPES.ENODE_HYP.ordinal() ){
			String[] fs = sent.get(pos).getFS();
			for(String f: fs)
				featureList.add(this._param_g.toFeature(network, FEATYPE.grmm.name(), Entity.get(eId).getForm(), f));
			if(NetworkConfig.USE_NEURAL_FEATURES){
				String lw = pos>0? sent.get(pos-1).getName():"<PAD>";
//				String llw = pos==0? "<PAD>": pos==1? "<PAD>":sent.get(pos-2).getName();
				String rw = pos<sent.length()-1? sent.get(pos+1).getName():"<PAD>";
//				String rrw = pos==sent.length()-1? "<PAD>": pos==sent.length()-2? "<PAD>":sent.get(pos+2).getName();
				String currWord = sent.get(pos).getName();
				featureList.add(this._param_g.toFeature(network, FEATYPE.neural_1.name(), Entity.get(eId).getForm(), lw.toLowerCase()+IN_SEP+
						currWord.toLowerCase()+IN_SEP+rw.toLowerCase()));
//				featureList.add(this._param_g.toFeature(network, FEATYPE.neural.name(), Entity.get(eId).getForm(), currWord.toLowerCase() ));
				/**Use collapsed features**/
				//int[] tag_child = NetworkIDMapper.toHybridNodeArray(network.getNode(children_k[1]));
				//int tag_child_id = tag_child[2];
//				featureList.add(this._param_g.toFeature(network, FEATYPE.neural_1.name(), Entity.get(eId).getForm(), currWord.toLowerCase() ));
			}
		}
		
		if(nodeArr[1]==NODE_TYPES.TNODE_HYP.ordinal()){
			String[] fs = sent.get(pos).getFS();
			for(String f: fs)
				featureList.add(this._param_g.toFeature(network, FEATYPE.grmm.name(), Tag.get(eId).getForm(), f));
			String lw = pos>0? sent.get(pos-1).getName():"<PAD>";
			String currWord = sent.get(pos).getName();
			String rw = pos<sent.length()-1? sent.get(pos+1).getName():"<PAD>";
			if(NetworkConfig.USE_NEURAL_FEATURES){
				//featureList.add(this._param_g.toFeature(network, FEATYPE.neural.name(), Tag.get(eId).getForm(), currWord.toLowerCase()));
				//int[] entity_child = NetworkIDMapper.toHybridNodeArray(network.getNode(children_k[1]));
				//int entity_child_id = entity_child[2];
				featureList.add(this._param_g.toFeature(network, FEATYPE.neural_2.name(), Tag.get(eId).getForm(), lw.toLowerCase()+IN_SEP+
						currWord.toLowerCase()+IN_SEP+rw.toLowerCase() ));
			}
		}
		
//		if(NetworkConfig.USE_NEURAL_FEATURES && (nodeArr[1]==NODE_TYPES.ENODE_HYP.ordinal() || nodeArr[1]==NODE_TYPES.TNODE_HYP.ordinal())){
//			String lw = pos>0? sent.get(pos-1).getName():"<PAD>";
//			String llw = pos==0? "<PAD>": pos==1? "<PAD>":sent.get(pos-2).getName();
//			String rw = pos<sent.length()-1? sent.get(pos+1).getName():"<PAD>";
//			String rrw = pos==sent.length()-1? "<PAD>": pos==sent.length()-2? "<PAD>":sent.get(pos+2).getName();
//			String currWord = sent.get(pos).getName();
//			featureList.add(this._param_g.toFeature(network, FEATYPE.neural.name(), Entity.get(eId).getForm(), currWord));
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
	
	

}

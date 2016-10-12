package com.statnlp.projects.nndcrf.linear_pos;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.neural.NeuralConfig;

public class POSFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 376931974939202432L;

	public enum FEATYPE {chunk, neural_1, neural_2};
//	private String OUT_SEP = NeuralConfig.OUT_SEP; 
	private String IN_SEP = NeuralConfig.IN_SEP;
	
	public POSFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}
	
	
	
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		
		//the tag in the sentence is actually the caps id.
		
		POSInstance inst = ((POSInstance)network.getInstance());
		//int instanceId = inst.getInstanceId();
		Sentence sent = inst.getInput();
		long node = network.getNode(parent_k);
		int[] nodeArr = NetworkIDMapper.toHybridNodeArray(node);
		
		FeatureArray fa = null;
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		
		int pos = nodeArr[0]-1;
		int eId = nodeArr[1];
		if(pos<0 || pos > inst.size())
			return FeatureArray.EMPTY;
			
//		System.err.println(Arrays.toString(nodeArr) + Entity.get(eId).toString());
		int[] child = NetworkIDMapper.toHybridNodeArray(network.getNode(children_k[0]));
		int childEId = child[1];
		
		String lw = pos>0? sent.get(pos-1).getName():"<PAD>";
		String llw = pos==0? "<PAD>": pos==1? "<PAD>":sent.get(pos-2).getName();
		String rw = pos<sent.length()-1? sent.get(pos+1).getName():"<PAD>";
		String rrw = pos==sent.length()? "<PAD>":pos==sent.length()-1? "<PAD>": pos==sent.length()-2? "<PAD>":sent.get(pos+2).getName();
		
		String currWord = pos==inst.size()? "<PAD>":inst.getInput().get(pos).getName();
		
		
		
		
		String currTag = POS.get(eId).getForm();
		if(NetworkConfig.USE_NEURAL_FEATURES){
			featureList.add(this._param_g.toFeature(network,FEATYPE.neural_1.name(), currTag,  currWord.toLowerCase()));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.neural_1.name(), currEn, llw+IN_SEP+lw+IN_SEP+currWord+IN_SEP+rw+IN_SEP+rrw+OUT_SEP+
//										llt+IN_SEP+lt+IN_SEP+currTag+IN_SEP+rt+IN_SEP+rrt));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.neural_1.name(), currTag, llw.toLowerCase()+IN_SEP+lw.toLowerCase()
//								+IN_SEP+currWord.toLowerCase()+IN_SEP+rw.toLowerCase()+IN_SEP+rrw.toLowerCase()));
		}
		String prevTag = POS.get(childEId).getForm();
		featureList.add(this._param_g.toFeature(network,FEATYPE.chunk.name(), currTag,  prevTag));
		
		
		
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

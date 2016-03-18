package com.statnlp.example.lcrf;

import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class LCRFFeatureManager extends FeatureManager {

	public enum FEATYPE {EMIS,TRAN};
	
	public LCRFFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		// TODO Auto-generated method stub
		LCRFInstance lcrfInstance = ((LCRFInstance)network.getInstance());
		int instanceID = lcrfInstance.getInstanceId();
		long node = network.getNode(parent_k);
		int[] nodeArr = NetworkIDMapper.toHybridNodeArray(node);
		
		int pos = nodeArr[0]-1;
		if(pos<0 || pos >= lcrfInstance.size())
			return FeatureArray.EMPTY;
			
		int tagId = nodeArr[1];
		int[] child = NetworkIDMapper.toHybridNodeArray(network.getNode(children_k[0]));
		int childTagId = child[1];
		
		int emissionFeature = this._param_g.toFeature(FEATYPE.EMIS.name(), lcrfInstance.getInput().get(pos), String.valueOf(tagId));
		int transitionFeature = this._param_g.toFeature(FEATYPE.TRAN.name(), String.valueOf(childTagId),String.valueOf(tagId));
		FeatureArray fa = null;
		if(emissionFeature!=-1 && transitionFeature!=-1)
			fa = new FeatureArray(new int[]{emissionFeature,transitionFeature});
		else if(emissionFeature!=-1 && transitionFeature==-1){
			fa = new FeatureArray(new int[]{emissionFeature});
		}else if(emissionFeature==-1 && transitionFeature!=-1){
			fa = new FeatureArray(new int[]{transitionFeature});
		}else return FeatureArray.EMPTY;
		
		return fa;
	}

}

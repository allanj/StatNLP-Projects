package com.statnlp.example.treecrf;

import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class TCRFFeatureManager extends FeatureManager {

	private enum FEATYPE {NOTER,TER};
	
	public TCRFFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		TCRFInstance tcrfInst = (TCRFInstance)network.getInstance();
		long parent = network.getNode(parent_k);
		int[] parentArr = NetworkIDMapper.toHybridNodeArray(parent);
//		System.out.println("Parent:"+Arrays.toString(parentArr));
		// if this node is a leaf
		int tagID = parentArr[2];
		FeatureArray fa = null;
//		System.err.println("children_k length:"+children_k.length);
		if(children_k.length==0){
			int pos = parentArr[1];
			if(pos<0 || pos >= tcrfInst.size())
				return FeatureArray.EMPTY;
			int terminalFeature = this._param_g.toFeature(FEATYPE.TER.name(), String.valueOf(tagID), tcrfInst.getInput().get(pos));
			fa = new FeatureArray(new int[]{terminalFeature});
		}else{
			int[] firstChild = NetworkIDMapper.toHybridNodeArray(network.getNode(children_k[0]));
//			System.err.println("First child:"+Arrays.toString(firstChild));
			int firstChildTagID = firstChild[2];
			int[] secondChild = NetworkIDMapper.toHybridNodeArray(network.getNode(children_k[1]));
			int secondChildTagID = secondChild[2];
			int nonTerminalFeature = this._param_g.toFeature(FEATYPE.NOTER.name(), String.valueOf(tagID), String.valueOf(firstChildTagID)+","+String.valueOf(secondChildTagID));
			fa = new FeatureArray(new int[]{nonTerminalFeature});
		}
		return fa;
	}

}

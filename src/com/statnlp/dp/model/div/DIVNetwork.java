package com.statnlp.dp.model.div;

import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.DependencyNetwork;
import com.statnlp.dp.model.mhp.MHPMain;
import com.statnlp.dp.utils.DPConfig;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;

public class DIVNetwork extends DependencyNetwork {

	
	private static final long serialVersionUID = 991556477287748391L;
	
	public DIVNetwork() {
		// TODO Auto-generated constructor stub
	}

	public DIVNetwork(int networkId, DIVInstance inst, LocalNetworkParam param) {
		super(networkId, inst, param);
		// TODO Auto-generated constructor stub
	}

	public DIVNetwork(int networkId, DIVInstance inst, long[] nodes, int[][][] children, LocalNetworkParam param, int numNodes) {
		super(networkId, inst, nodes, children, param,numNodes);
	}
	
	public void remove(int k){
		
	}
	
	public boolean isRemoved(int k){
		return false;
	}
	
	public double totalLossUpTo(int k, int[] child_k){
		DIVInstance instance = (DIVInstance)this.getInstance();
		
		int[] paArr = getNodeArray(k);
		Sentence sent = instance.getInput();
		
		double loss = 0;
		if(paArr[1]==0){
			int idx = paArr[0];
			String e = sent.get(idx).getEntity().length()<2? DPConfig.ONE: sent.get(idx).getEntity().substring(2);
			int eIdx = DIVMain.typeMap.get(e);
			if(paArr[4]!=eIdx)
				return 1.0;
			else return 0.0;
		}else{
			for(int i=0;i<child_k.length;i++)
				loss+=_loss[child_k[i]];
			return loss;
		}
	}

}

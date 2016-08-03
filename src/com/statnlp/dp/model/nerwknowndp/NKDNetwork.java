package com.statnlp.dp.model.nerwknowndp;

import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.DependencyNetwork;
import com.statnlp.dp.model.mhp.MHPMain;
import com.statnlp.dp.utils.DPConfig;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;

public class NKDNetwork extends DependencyNetwork {

	
	private static final long serialVersionUID = 991556477287748391L;
	private static String OE = DPConfig.OE;
	private static String ONE = DPConfig.ONE;
	private static String PARENT_IS = DPConfig.PARENT_IS;
	
	
	public NKDNetwork() {
		// TODO Auto-generated constructor stub
	}

	public NKDNetwork(int networkId, NKDInstance inst, LocalNetworkParam param) {
		super(networkId, inst, param);
		// TODO Auto-generated constructor stub
	}

	public NKDNetwork(int networkId, NKDInstance inst, long[] nodes, int[][][] children, LocalNetworkParam param, int numNodes) {
		super(networkId, inst, nodes, children, param,numNodes);
	}
	
	public void remove(int k){
		
	}
	
	public boolean isRemoved(int k){
		return false;
	}
	

}

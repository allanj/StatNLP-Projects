package com.statnlp.projects.dep.model.hybrid;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.projects.dep.DependencyNetwork;
import com.statnlp.projects.dep.model.mhp.MHPMain;
import com.statnlp.projects.dep.utils.DPConfig;

public class HBDNetwork extends DependencyNetwork {

	
	private static final long serialVersionUID = 991556477287748391L;
	private static String OE = DPConfig.OE;
	private static String ONE = DPConfig.ONE;
	private static String PARENT_IS = DPConfig.PARENT_IS;
	
	
	public HBDNetwork() {
		// TODO Auto-generated constructor stub
	}

	public HBDNetwork(int networkId, HBDInstance inst, LocalNetworkParam param) {
		super(networkId, inst, param);
		// TODO Auto-generated constructor stub
	}

	public HBDNetwork(int networkId, HBDInstance inst, long[] nodes, int[][][] children, LocalNetworkParam param, int numNodes) {
		super(networkId, inst, nodes, children, param,numNodes);
	}
	
	public void remove(int k){
		
	}
	
	public boolean isRemoved(int k){
		return false;
	}
	

}

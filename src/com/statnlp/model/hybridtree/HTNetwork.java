package com.statnlp.model.hybridtree;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.TableLookupNetwork;

public class HTNetwork extends TableLookupNetwork {

	public HTNetwork() {
		// TODO Auto-generated constructor stub
	}

	public HTNetwork(int networkId, Instance inst, LocalNetworkParam param) {
		super(networkId, inst, param);
		// TODO Auto-generated constructor stub
	}

	public HTNetwork(int networkId, Instance inst, long[] nodes, int[][][] children, LocalNetworkParam param) {
		super(networkId, inst, nodes, children, param);
		// TODO Auto-generated constructor stub
	}

}

package com.statnlp.mt.commons;

import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.TableLookupNetwork;

public class BitextNetwork extends TableLookupNetwork{
	
	private static final long serialVersionUID = 6161162639677335156L;
	
	public BitextNetwork(){
		super();
	}
	
	public BitextNetwork(int networkId, BitextInstance inst, LocalNetworkParam param){
		super(networkId, inst, param);
	}
	
}
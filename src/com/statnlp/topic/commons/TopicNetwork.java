package com.statnlp.topic.commons;

import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.TableLookupNetwork;

public class TopicNetwork extends TableLookupNetwork{
	
	private static final long serialVersionUID = 6161162639677335156L;
	
	public TopicNetwork(){
		super();
	}
	
	public TopicNetwork(int networkId, TopicInstance inst, LocalNetworkParam param){
		super(networkId, inst, param);
	}
	
	public TopicNetwork(int networkId, TopicInstance inst, long[] nodes, int[][][] children, LocalNetworkParam param){
		super(networkId, inst, nodes, children, param);
	}
	
}
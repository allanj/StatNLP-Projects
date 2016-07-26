package com.statnlp.dp.model.var;

import java.util.HashMap;

import com.statnlp.commons.types.Instance;
import com.statnlp.dp.Transformer;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;

public class VarNetworkCompiler extends NetworkCompiler{

	private static final long serialVersionUID = -3158302997644353166L;
	protected TreeCompiler tc;
	protected LinearCompiler lc;
	
	/**
	 * Compiler for both
	 * @param typeMap: typeMap from DPConfig which also include those type with "pae"
	 */
	public VarNetworkCompiler(HashMap<String, Integer> typeMap, HashMap<String, Integer> entityMap, String[] linearEntities, Transformer trans) {
		tc = new TreeCompiler(typeMap, null, trans);
		lc = new LinearCompiler(entityMap, linearEntities, null);
	}

	@Override
	public Network compile(int networkId, Instance inst, LocalNetworkParam param) {
		TreeNetwork tn = (TreeNetwork)tc.compile(networkId, inst, param);
		LinearNetwork ln = (LinearNetwork)lc.compile(networkId, inst, param);
		return new VarNetwork(ln , tn);
	}

	@Override
	public Instance decompile(Network network) {
		VarNetwork lcrfNetwork = (VarNetwork)network;
		VarInstance lcrfInstance = (VarInstance)lcrfNetwork.getInstance();
		VarInstance result = lcrfInstance.duplicate();
		lc.decompile(network, result);
		tc.decompile(network, result);
		return result;
	}

	
}

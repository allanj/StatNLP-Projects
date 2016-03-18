/** Statistical Natural Language Processing System
    Copyright (C) 2014  Lu, Wei

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.statnlp.commons.ml.classification.logisticregression;

import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.hybridnetworks.GlobalNetworkParam;

public class LRFeatureManager extends FeatureManager{
	
	public LRFeatureManager(GlobalNetworkParam param) {
		super(param);
	}
	
	@Override
	public void setNetwork(Network network) {
		this._network = (LRNetwork) network;
	}

	@Override
	public FeatureArray extract(int parent_k, int[] children_k) {
		long node_parent = ((LRNetwork)this._network).get(parent_k);
		
		int[] ids_parent = NetworkIDMapper.toHybridNodeArray(node_parent);
		int node_type = ids_parent[0];
		
		if(node_type==LRConfig.NODE_TYPE.ROOT.ordinal()){
			return new FeatureArray(this._param);
		}
		
		int label_id = ids_parent[1];
		
		LRInstance inst = (LRInstance)this._network.getInstance();
		
		String[] inputs = inst.getInput();
		
		int[] f = new int[inputs.length];
		for(int k = 0; k<inputs.length; k++){
			f[k] = this._param.toFeature(label_id+"", inputs[k]);
		}
		
		FeatureArray fa = new FeatureArray(f, this._param);
		
		return fa;
	}
	
}
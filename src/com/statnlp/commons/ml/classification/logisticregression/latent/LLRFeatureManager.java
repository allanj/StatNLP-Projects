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
package com.statnlp.commons.ml.classification.logisticregression.latent;

import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.hybridnetworks.GlobalNetworkParam;

public class LLRFeatureManager extends FeatureManager{
	
	public LLRFeatureManager(GlobalNetworkParam param) {
		super(param);
	}
	
	@Override
	public void setNetwork(Network network) {
		this._network = (LLRNetwork) network;
	}

	@Override
	public FeatureArray extract(int parent_k, int[] children_k) {
		long node_parent = ((LLRNetwork)this._network).get(parent_k);
		
		int[] ids_parent = NetworkIDMapper.toHybridNodeArray(node_parent);
		int node_type = ids_parent[0];
		
		if(node_type==LLRConfig.NODE_TYPE.ROOT.ordinal() || node_type==LLRConfig.NODE_TYPE.LABEL.ordinal()){
			return new FeatureArray(this._param);
		}
		
		int label_id = ids_parent[1];
		
		LLRInstance inst = (LLRInstance)this._network.getInstance();
		
		String[] inputs = inst.getInput();
		
		FeatureArray fa = new FeatureArray(this._param);
		
//		int[] f = new int[inputs.length];
		for(int k = 0; k<inputs.length; k++){
			int[] f = new int[1];
//			f[0] = this._param.toFeature(label_id+""+node_type, inputs[k]);
//			fa = new FeatureArray(f, fa);
			if(label_id%3==0){
				String input = inputs[k];
				if(Integer.parseInt(input.substring(input.length()-1))<=2){
					f[0] = this._param.toFeature(label_id+""+node_type, inputs[k]);
					fa = new FeatureArray(f, fa);
				}
			} else if(label_id%3==1){
				String input = inputs[k];
				if(Integer.parseInt(input.substring(input.length()-1))>=3){
					f[0] = this._param.toFeature(label_id+""+node_type, inputs[k]);
					fa = new FeatureArray(f, fa);
				}
			} else {
				String input = inputs[k];
				if(Integer.parseInt(input.substring(input.length()-1))<=2){
					f[0] = this._param.toFeature(label_id+""+node_type, inputs[k]);
					fa = new FeatureArray(f, fa);
				}
			}
		}
		
		return fa;
	}
	
}
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

import java.util.ArrayList;

import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.hybridnetworks.GlobalNetworkParam.EXP_MODE;

public class LRNetworkBuilder {
	
	private LRModel _model;
	
	public LRNetworkBuilder(LRModel model){
		this._model = model;
	}
	
	public LRNetwork build(LRInstance inst, LRFeatureManager fm, EXP_MODE exp_mode){
		if(inst.hasOutput()){
			return this.build_labeled(inst, fm);
		} else {
			return this.build_unlabeled(inst, fm);
		}
	}
	
	public LRNetwork build(LRInstance inst, LRFeatureManager fm, EXP_MODE exp_mode, OutputLabel output){
		return this.build_unlabeled(inst, fm, output);
	}
	
	private LRNetwork build_unlabeled(LRInstance inst, LRFeatureManager fm, OutputLabel label){
		LRNetwork network = new LRNetwork(inst, fm, this._model, EXP_MODE.GLOBAL);
		long node_root = this.toNode_root();
		network.addNode(node_root);
		long node = this.toNode(label);
		network.addNode(node);
		network.addEdge(node_root, new long[]{node});
		network.finalizeNetwork();
		return network;
	}
	
	private LRNetwork build_unlabeled(LRInstance inst, LRFeatureManager fm){
		LRNetwork network = new LRNetwork(inst, fm, this._model, EXP_MODE.GLOBAL);
		long node_root = this.toNode_root();
		network.addNode(node_root);
		ArrayList<OutputLabel> labels = this._model.getLabels();
		for(OutputLabel label : labels){
			long node = this.toNode(label);
			network.addNode(node);
			network.addEdge(node_root, new long[]{node});
		}
		network.finalizeNetwork();
		return network;
	}
	
	private LRNetwork build_labeled(LRInstance inst, LRFeatureManager fm){
		LRNetwork network = new LRNetwork(inst, fm, this._model, EXP_MODE.LOCAL);
		long node_root = this.toNode_root();
		network.addNode(node_root);
		long node = this.toNode(((LabeledLRInstance)inst).getOutput());
		network.addNode(node);
		network.addEdge(node_root, new long[]{node});
		network.finalizeNetwork();
		return network;
	}
	
	private long toNode_root(){
		return NetworkIDMapper.toHybridNodeID(new int[]{LRConfig.NODE_TYPE.ROOT.ordinal(), 0, LRConfig.NODE_TYPE.ROOT.ordinal(), 0, 0});
	}
	
	private long toNode(OutputLabel label){
		return NetworkIDMapper.toHybridNodeID(new int[]{LRConfig.NODE_TYPE.LABEL.ordinal(), label.getId(), LRConfig.NODE_TYPE.LABEL.ordinal(), label.getId(), 0});
	}
	
}
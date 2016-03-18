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

import java.util.ArrayList;

import com.statnlp.commons.ml.classification.logisticregression.OutputLabel;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.hybridnetworks.GlobalNetworkParam.EXP_MODE;

public class LLRNetworkBuilder {
	
	private LLRModel _model;
	
	public LLRNetworkBuilder(LLRModel model){
		this._model = model;
	}
	
	public LLRNetwork build(LLRInstance inst, LLRFeatureManager fm){
		if(inst.hasOutput()){
			return this.build_labeled(inst, fm);
		} else {
			return this.build_unlabeled(inst, fm);
		}
	}
	
	private LLRNetwork build_unlabeled(LLRInstance inst, LLRFeatureManager fm){
		LLRNetwork network = new LLRNetwork(inst, fm, this._model, EXP_MODE.GLOBAL);
		long node_root = this.toNode_root();
		network.addNode(node_root);
		ArrayList<OutputLabel> labels = this._model.getLabels();
		for(OutputLabel label_output : labels){
			long node_output = this.toNode_output(label_output);
			network.addNode(node_output);
			network.addEdge(node_root, new long[]{node_output});

			ArrayList<LatentLabel> labels_latent = this._model.getLatentLabels(label_output);
			
			for(LatentLabel label_latent : labels_latent){
				long node_latent = this.toNode_latent(label_latent);
				network.addNode(node_latent);
				network.addEdge(node_output, new long[]{node_latent});
			}
		}
		network.finalizeNetwork();
		return network;
	}
	
	private LLRNetwork build_labeled(LLRInstance inst, LLRFeatureManager fm){
		LLRNetwork network = new LLRNetwork(inst, fm, this._model, EXP_MODE.LOCAL);
		long node_root = this.toNode_root();
		network.addNode(node_root);
		OutputLabel label_output = ((LabeledLLRInstance)inst).getOutput();
		long node_output = this.toNode_output(label_output);
		network.addNode(node_output);
		network.addEdge(node_root, new long[]{node_output});
		
		ArrayList<LatentLabel> labels_latent = this._model.getLatentLabels(label_output);
		
		for(LatentLabel label_latent : labels_latent){
			long node_latent = this.toNode_latent(label_latent);
			network.addNode(node_latent);
			network.addEdge(node_output, new long[]{node_latent});
		}
		
		network.finalizeNetwork();
		return network;
	}
	
	private long toNode_latent(LatentLabel label){
		return NetworkIDMapper.toHybridNodeID(new int[]{LLRConfig.NODE_TYPE.LATENT_LABEL.ordinal(), label.getId(), 
				LLRConfig.NODE_TYPE.LATENT_LABEL.ordinal(), label.getId(), 0});
	}
	
	private long toNode_output(OutputLabel label){
		return NetworkIDMapper.toHybridNodeID(new int[]{LLRConfig.NODE_TYPE.LABEL.ordinal(), label.getId(), 
				LLRConfig.NODE_TYPE.LABEL.ordinal(), label.getId(), 0});
	}
	
	private long toNode_root(){
		return NetworkIDMapper.toHybridNodeID(new int[]{LLRConfig.NODE_TYPE.ROOT.ordinal(), 0, 
				LLRConfig.NODE_TYPE.ROOT.ordinal(), 0, 0});
	}
	
}
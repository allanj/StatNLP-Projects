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
package com.statnlp.ie.linear.semi;

import java.util.ArrayList;

import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.ie.linear.semi.IELinearSemiConfig;
import com.statnlp.ie.types.Mention;
import com.statnlp.ie.types.SemanticTag;
import com.statnlp.ie.types.UnlabeledTextSpan;

public class IELinearSemiModel {
	
	private transient ArrayList<IELinearSemiInstance> _labeledInstances;
	private transient IELinearSemiBuilder _builder;
	private IELinearSemiFeatureManager _fm;
	private SemanticTag[] _tags;
	private boolean _cacheFeatures = true;
	
	public IELinearSemiModel(IELinearSemiFeatureManager fm, SemanticTag[] tags){
		this._fm = fm;
		this._tags = tags;
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		this._builder = new IELinearSemiBuilder(fm, tags);
	}
	
	public void train(ArrayList<IELinearSemiInstance> labeledInstances){
		this._fm.disableCache();
		if(_cacheFeatures)
			this._fm.enableCache(labeledInstances.size(), this._tags.length);
		
		this._labeledInstances = labeledInstances;
		this.touch();
		this.train();
		this._fm.disableCache();
	}
	
	private IELinearSemiNetwork[] _networks_labeled;
	private IELinearSemiNetwork[] _networks_unlabeled;
	
	private void touch(){
		int num_instances = this._labeledInstances.size();
		IELinearSemiNetwork network1;
		IELinearSemiNetwork network2;
		for(int k = 0; k<num_instances; k++){
			IELinearSemiInstance inst_labeled = this._labeledInstances.get(k);
			
//			System.err.println(inst_labeled.getId());
			if(!inst_labeled.hasOutput()){
				throw new RuntimeException("This instance is not labeled."+inst_labeled);
			}
			network1 = this.getNetwork(inst_labeled);
			network1.touch();
			
			IELinearSemiInstance inst_unlabeled = inst_labeled.removeOutput();
			if(inst_unlabeled.hasOutput()){
				throw new RuntimeException("This instance is labeled."+inst_unlabeled);
			}
			network2 = this.getNetwork(inst_unlabeled);
			network2.touch();
			
			if(!network2.contains(network1)){
				throw new RuntimeException("xx");
			}
		}
		System.err.println(this._fm.getParam().countFeatures()+" features");
		this._fm.getParam().lockIt();
	}
	
	private void train(){
		for(int it = 0; it<IELinearSemiConfig._MAX_LBFGS_ITRS; it++){
			if(this.train_oneIteration(it))
				return;
		}
	}
	
	private boolean _cacheNetworks = true;
	
	private IELinearSemiNetwork getNetwork(IELinearSemiInstance inst){
		
		if(this._cacheNetworks && this._networks_labeled == null){
//			System.err.println("xxx"+_labeledInstances.size());
			this._networks_labeled = new IELinearSemiNetwork[this._labeledInstances.size()];
			this._networks_unlabeled = new IELinearSemiNetwork[this._labeledInstances.size()];
		}
		
		if(this._cacheNetworks){
			int id = inst.getInstanceId();
			if(id>0){
				id--;
				if(this._networks_labeled[id]!=null){
					return this._networks_labeled[id];
				}
			} else {
				id = -id;
				id--;
				if(this._networks_unlabeled[id]!=null){
					return this._networks_unlabeled[id];
				}
			}
		}
		
		IELinearSemiNetwork network = this._builder.build(inst);
		
		if(this._cacheNetworks){
			int id = inst.getInstanceId();
			if(id>0){
				id--;
				this._networks_labeled[id] = network;
			} else {
				id = -id;
				id--;
				this._networks_unlabeled[id] = network;
			}
		}
		
		return network;
	}
	
	private boolean train_oneIteration(int it){
		
		long bTime = System.currentTimeMillis();
		int num_instances = this._labeledInstances.size();
		IELinearSemiNetwork network;
		for(int k = 0; k<num_instances; k++){
			IELinearSemiInstance inst_labeled = this._labeledInstances.get(k);
			
			NetworkConfig.EXP_MODE = NetworkConfig.EXP_LOCAL;
//			network = this._builder.build(inst_labeled);
			network = this.getNetwork(inst_labeled);
			network.train();
			double inside_num = network.getInside();
//			if((it+1)%100==0)
//				System.err.println("numerator="+Math.exp(inside_num)+"\t"+inside_num);
//			System.err.println(network.toString());
//			System.exit(1);
			
			NetworkConfig.EXP_MODE = NetworkConfig.EXP_GLOBAL;
			IELinearSemiInstance inst_unlabeled = inst_labeled.removeOutput();
//			network = this._builder.build(inst_unlabeled);
			network = this.getNetwork(inst_unlabeled);
			network.train();
			double inside_denom = network.getInside();
//			if((it+1)%100==0)
//				System.err.println("denominator="+Math.exp(inside_denom)+"\t"+inside_denom);
//			System.err.println(network.toString_ie(inst_unlabeled._span.length()));
//			System.exit(1);
			double prob = Math.exp(inside_num-inside_denom);
//			if((it+1)%100==0)
//				System.err.println("prob="+prob);
			if(prob>1.0){
				throw new RuntimeException("The prob exceeds 1:\t"+prob);
			}
		}
		double obj = this._fm.getParam().getObj();
		long eTime = System.currentTimeMillis();
		System.out.println("Iteration "+it+"\t Obj="+obj+"\t"+(eTime-bTime)/1000.0+" secs.");
//		this._fm.getParam().preview();
		return this._fm.getParam().update();
	}

	public void max(IELinearSemiInstance inst){
		
		this._cacheNetworks = false;
		
		this._fm.disableCache();
		this._fm.enableCache(1, this._tags.length);
		inst.setInstanceId(1);
		
		UnlabeledTextSpan span = (UnlabeledTextSpan)inst.getSpan();
		
		IELinearSemiNetwork network = this.getNetwork(inst);
		network.max();
		
		int root_k = network.countNodes()-1;
		this.decode(network, root_k, span);
		
	}
	
	private void decode(IELinearSemiNetwork network, int node_k, UnlabeledTextSpan span){
		
		int N = span.length();
		
		long node = network.get(node_k);
		int[] array = NetworkIDMapper.toHybridNodeArray(node);
		int hybridType = array[4];
		if(hybridType == IELinearSemiConfig.NODE_TYPE.MENTION_TAG.ordinal()){
			int start_index = N - array[0];
			int end_index = start_index + array[1];
			int tag_id = array[3]-1;
			SemanticTag tag = this._tags[tag_id-1];
			if(tag_id!=tag.getId()){
				throw new RuntimeException(tag.getId()+"!="+tag_id);
			}
			span.label_predict(start_index, end_index, new Mention(start_index, end_index, start_index, end_index, tag));
		}
		
		int[] nodes_child_k = network.getMaxPath(node_k);
		
		for(int node_child_k : nodes_child_k){
			this.decode(network, node_child_k, span);
		}
		
	}
	
}

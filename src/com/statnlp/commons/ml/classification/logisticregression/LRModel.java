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

import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.hybridnetworks.GlobalNetworkParam.EXP_MODE;

public class LRModel {
	
	private transient ArrayList<LabeledLRInstance> _labeledInstances;
	private transient LRNetworkBuilder _builder;
	
	private ArrayList<OutputLabel> _labels = new ArrayList<OutputLabel>();
	private LRFeatureManager _fm;
	
	public LRModel(LRFeatureManager fm){
		this._fm = fm;
		this._labeledInstances = new ArrayList<LabeledLRInstance>();
	}
	
	public ArrayList<OutputLabel> getLabels(){
		return this._labels;
	}
	
	public OutputLabel getLabel(int id){
		return this._labels.get(id);
	}
	
	public void initBuilder(){
		if(this._builder==null)
			this._builder = new LRNetworkBuilder(this);
	}
	
	public OutputLabel toOutputLabel(String output){
		OutputLabel label = new OutputLabel(output);
		int index = this._labels.indexOf(label);
		if(index>=0){
			return this._labels.get(index);
		}
		label.setId(this._labels.size());
		this._labels.add(label);
		return label;
	}
	
	public void train(ArrayList<LabeledLRInstance> labeledInstances){
		this.initBuilder();
		this._labeledInstances = labeledInstances;
		
		this.touch();
		this.train();
	}
	
	private void touch(){
		long bTime, eTime;
		int num_instances = this._labeledInstances.size();
//		this._networks_l = new LinearNetwork[num_instances];
//		this._networks_u = new LinearNetwork[num_instances];
		LRNetwork network;
		for(int k = 0; k<num_instances; k++){
//			System.err.println("k="+k);
			System.err.print('.');
			if(k%100==0){
				System.err.print('\n');
			}
			
			bTime = System.currentTimeMillis();
			
			LabeledLRInstance inst_labeled = this._labeledInstances.get(k);
			network = this._builder.compile(inst_labeled, this._fm, EXP_MODE.LOCAL);
//			this._networks_l[k] = network;
			network.touch();
			
			eTime = System.currentTimeMillis();
//			System.err.println((eTime-bTime)/1000.0+" sec");
			
			bTime = System.currentTimeMillis();
			
			UnlabeledLRInstance inst_unlabeled = inst_labeled.removeOutput();
			network = this._builder.compile(inst_unlabeled, this._fm, EXP_MODE.GLOBAL);
//			this._networks_u[k] = network;
			network.touch();
			
			eTime = System.currentTimeMillis();
//			System.err.println((eTime-bTime)/1000.0+" sec");
		}
		this._fm.getParam().lockIt();
		System.err.println("There are "+this._fm.getParam().countFeatures()+" features.");
	}
	
	private void train(){
		for(int it = 0; it<LRConfig.MAX_LBFGS_ITRS; it++){
			if(this.train_oneIteration(it))
				return;
		}
	}
	
	private boolean train_oneIteration(int it){
		int num_instances = this._labeledInstances.size();
		LRNetwork network;
		for(int k = 0; k<num_instances; k++){
			NetworkConfig.EXP_MODE = NetworkConfig.EXP_LOCAL;
			LabeledLRInstance inst_labeled = this._labeledInstances.get(k);
			network = this._builder.compile(inst_labeled, this._fm, EXP_MODE.LOCAL);
//			network = this._networks_l[k];//this._builder.build();
			network.train();
//			double inside = network.getInside();
//			System.err.println("numerator="+inside);
			
			if(!NetworkConfig.TRAIN_MODE_IS_GENERATIVE){
				NetworkConfig.EXP_MODE = NetworkConfig.EXP_GLOBAL;
				UnlabeledLRInstance inst_unlabeled = inst_labeled.removeOutput();
				network = this._builder.compile(inst_unlabeled, this._fm, EXP_MODE.GLOBAL);
//				network = this._networks_u[k];//this._builder.build();
				network.train();
//				inside = network.getInside();
//				System.err.println("denominator="+inside);
			}
		}
		double obj = this._fm.getParam().getObj();
		System.out.println("Iteration "+it+"\t Obj="+obj);
//		this._fm.getParam().preview();
		return this._fm.getParam().update();
	}
	
	public double residual(LabeledLRInstance inst){
		UnlabeledLRInstance inst_un = inst.removeOutput();
		LRNetwork network = this._builder.build(inst_un, this._fm, EXP_MODE.GLOBAL, inst.getOutput());
		network.max();
		double pred_score = network.getMax();
		double sum = sum(inst_un);
		double prob = Math.exp(pred_score-sum);
		if(prob < 0 || prob > 1){
			throw new RuntimeException("invalid:"+prob+"="+pred_score+"/"+sum);
		}
		return 1.0-prob;
	}
	
	public double sum(UnlabeledLRInstance inst){
		LRNetwork network = this._builder.compile(inst, this._fm, EXP_MODE.GLOBAL);
		return network.sum();
	}
	
	public double max(UnlabeledLRInstance inst){
		LRNetwork network = this._builder.compile(inst, this._fm, EXP_MODE.GLOBAL);
		network.max();
		
		int root_k = network.countNodes()-1;
		int node_k = root_k;
		
		int[] nodes_child_k = network.getMaxPath(node_k);
		node_k = nodes_child_k[0];
		long node = network.get(node_k);
		int node_type = NetworkIDMapper.toHybridNodeArray(node)[0];
		if(node_type == LRConfig.NODE_TYPE.LABEL.ordinal()){
			int tag_id = NetworkIDMapper.toHybridNodeArray(node)[1];
			OutputLabel label = this._labels.get(tag_id);
			inst.setPredictedOutput(label);
		}
		return network.getMax();
	}
	
}
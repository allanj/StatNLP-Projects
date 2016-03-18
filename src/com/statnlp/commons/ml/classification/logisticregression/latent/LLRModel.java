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
import java.util.HashMap;

import com.statnlp.commons.ml.classification.logisticregression.OutputLabel;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.hybridnetworks.NetworkParam_LV;

public class LLRModel {
	
	private transient ArrayList<LabeledLLRInstance> _labeledInstances;
	private transient LLRNetworkBuilder _builder;
	
	private ArrayList<OutputLabel> _output_labels = new ArrayList<OutputLabel>();
	private ArrayList<LatentLabel> _latent_labels = new ArrayList<LatentLabel>();
	private HashMap<OutputLabel, ArrayList<LatentLabel>> _output2latent = new HashMap<OutputLabel, ArrayList<LatentLabel>>();
	private LLRFeatureManager _fm;
	
	private int _num_latent = 2;
	
	public LLRModel(LLRFeatureManager fm){
		this._fm = fm;
		this._labeledInstances = new ArrayList<LabeledLLRInstance>();
		this._output2latent = new HashMap<OutputLabel, ArrayList<LatentLabel>>();
	}
	
	public ArrayList<LatentLabel> getLatentLabels(OutputLabel label){
		return this._output2latent.get(label);
	}
	
	public void addLatentLabel(OutputLabel output, LatentLabel latent){
		if(!this._output2latent.containsKey(output)){
			this._output2latent.put(output, new ArrayList<LatentLabel>());
		}
		ArrayList<LatentLabel> labels_latent = this._output2latent.get(output);
		if(!labels_latent.contains(latent)){
			labels_latent.add(latent);
		}
	}
	
	public ArrayList<OutputLabel> getLabels(){
		return this._output_labels;
	}
	
	public OutputLabel getLabel(int id){
		return this._output_labels.get(id);
	}
	
	public void initBuilder(){
		if(this._builder==null)
			this._builder = new LLRNetworkBuilder(this);
	}
	
	public LatentLabel toLatentLabel(String output){
		LatentLabel label = new LatentLabel(output);
		int index = this._latent_labels.indexOf(label);
		if(index>=0){
			return this._latent_labels.get(index);
		}
		label.setId(this._latent_labels.size());
		this._latent_labels.add(label);
		return label;
	}
	
	public OutputLabel toOutputLabel(String output){
		OutputLabel label_output = new OutputLabel(output);
		int index = this._output_labels.indexOf(label_output);
		if(index>=0){
			return this._output_labels.get(index);
		}
		label_output.setId(this._output_labels.size());
		this._output_labels.add(label_output);
		
		for(int k = 0; k<this._num_latent; k++){
			LatentLabel label_latent = this.toLatentLabel(output+":"+k);
			this.addLatentLabel(label_output, label_latent);
		}
		
		return label_output;
	}
	
	private void addLatentLabels(){
		
		OutputLabel A = this.toOutputLabel("A");
		OutputLabel B = this.toOutputLabel("B");
		OutputLabel C = this.toOutputLabel("C");
		OutputLabel D = this.toOutputLabel("D");
		OutputLabel E = this.toOutputLabel("E");
		
		//assume there are two latent labels for each true output label.
		
		LatentLabel X = this.toLatentLabel("X");
		LatentLabel Y = this.toLatentLabel("Y");
		LatentLabel Z = this.toLatentLabel("Z");
		
		this.addLatentLabel(A, X);
		this.addLatentLabel(A, Y);
		
		this.addLatentLabel(B, X);
		this.addLatentLabel(B, Z);
		
		this.addLatentLabel(C, X);
		this.addLatentLabel(C, Z);
		
		this.addLatentLabel(D, Y);
		this.addLatentLabel(D, Z);
		
		this.addLatentLabel(E, X);
		this.addLatentLabel(E, Y);
		this.addLatentLabel(E, Z);
		
	}
	
	public void train(ArrayList<LabeledLLRInstance> labeledInstances){
		this.initBuilder();
		this._labeledInstances = labeledInstances;
		
//		this.addLatentLabels();
		
		this.touch();
		this.train();
	}
	

	
	public void train_likelihood(ArrayList<LabeledLLRInstance> labeledInstances){
		this.initBuilder();
		this._labeledInstances = labeledInstances;
		
//		this.addLatentLabels();
		
		this.train_likelihood();
	}
	
	private LLRNetwork _networks_l[];
	private LLRNetwork _networks_u[];
	
	private void touch(){
		long bTime, eTime;
		int num_instances = this._labeledInstances.size();
		this._networks_l = new LLRNetwork[num_instances];
		this._networks_u = new LLRNetwork[num_instances];
		LLRNetwork network;
		for(int k = 0; k<num_instances; k++){
//			System.err.println("k="+k);
			System.err.print('.');
			if(k%100==0){
				System.err.print('\n');
			}
			
			bTime = System.currentTimeMillis();
			
			LabeledLLRInstance inst_labeled = this._labeledInstances.get(k);
			network = this._builder.build(inst_labeled, this._fm);
			this._networks_l[k] = network;
			network.touch();
			
			eTime = System.currentTimeMillis();
//			System.err.println((eTime-bTime)/1000.0+" sec");
			
			bTime = System.currentTimeMillis();
			
			UnlabeledLLRInstance inst_unlabeled = inst_labeled.removeOutput();
			network = this._builder.build(inst_unlabeled, this._fm);
			this._networks_u[k] = network;
			network.touch();
			
			eTime = System.currentTimeMillis();
//			System.err.println((eTime-bTime)/1000.0+" sec");
		}
		this._fm.getParam().lockIt();
		System.err.println("There are "+this._fm.getParam().countFeatures()+" features.");
	}
	
	private void train(){
		for(int it = 0; it<LLRConfig.MAX_LBFGS_ITRS; it++){
			if(this.train_oneIteration(it))
				return;
		}
	}
	

	public void train_likelihood(){
		
		for(int it = 0; it<LLRConfig.MAX_LBFGS_ITRS; it++){
			double obj = this._fm.getParam().getObj_old();
			((NetworkParam_LV)this._fm.getParam()).setFactorAndSwitchToOptimizeLikelihood(-obj);

			if(this.train_likelihood_oneIteration(it))
				return;
		}
	}
	
	private boolean train_oneIteration(int it){
		int num_instances = this._labeledInstances.size();
		LLRNetwork network;
		for(int k = 0; k<num_instances; k++){
			NetworkConfig.EXP_MODE = NetworkConfig.EXP_LOCAL;
			LabeledLLRInstance inst_labeled = this._labeledInstances.get(k);
//			network = this._builder.build(inst_labeled, this._fm);
			network = this._networks_l[k];//this._builder.build();
			network.train();
//			double numerator = network.getInside();
//			System.err.println("numerator="+numerator);
			
			if(!NetworkConfig.TRAIN_MODE_IS_GENERATIVE){
				NetworkConfig.EXP_MODE = NetworkConfig.EXP_GLOBAL;
				UnlabeledLLRInstance inst_unlabeled = inst_labeled.removeOutput();
//				network = this._builder.build(inst_unlabeled, this._fm);
				network = this._networks_u[k];//this._builder.build();
				network.train();
//				double denominator = network.getInside();
//				System.err.println("denominator="+denominator);
//				System.err.println("prob="+numerator/denominator);
			}
		}
		double obj = this._fm.getParam().getObj();
		System.out.println("Iteration "+it+"\t Obj="+obj);
//		this._fm.getParam().preview();
		return this._fm.getParam().update();
	}
	

	private boolean train_likelihood_oneIteration(int it){
		int num_instances = this._labeledInstances.size();
		LLRNetwork network;
		for(int k = 0; k<num_instances; k++){
			NetworkConfig.EXP_MODE = NetworkConfig.EXP_LOCAL;
			LabeledLLRInstance inst_labeled = this._labeledInstances.get(k);
//			network = this._builder.build(inst_labeled, this._fm);
			network = this._networks_l[k];//this._builder.build();
			network.train();
//			double numerator = network.getInside();
//			System.err.println("numerator="+numerator);
			
			if(!NetworkConfig.TRAIN_MODE_IS_GENERATIVE){
				NetworkConfig.EXP_MODE = NetworkConfig.EXP_GLOBAL;
				UnlabeledLLRInstance inst_unlabeled = inst_labeled.removeOutput();
//				network = this._builder.build(inst_unlabeled, this._fm);
				network = this._networks_u[k];//this._builder.build();
				network.train();
//				double denominator = network.getInside();
//				System.err.println("denominator="+denominator);
//				System.err.println("prob="+numerator/denominator);
			}
		}
		double obj = this._fm.getParam().getObj();
		System.out.println("Iteration "+it+"\t Obj="+obj);
//		this._fm.getParam().preview();
		return this._fm.getParam().update();
	}
	
	public void max(UnlabeledLLRInstance inst){
		LLRNetwork network = this._builder.build(inst, this._fm);
		network.max();
		
		int root_k = network.countNodes()-1;
		int node_k = root_k;
		
//		System.err.println("max score="+network.getMax()+"\t"+root_k+"\t"+Arrays.toString(NetworkIDMapper.toHybridNodeArray(network.getRoot())));
//		System.exit(1);
		
		int[] nodes_child_k = network.getMaxPath(node_k);
		node_k = nodes_child_k[0];
		long node = network.get(node_k);
		int node_type = NetworkIDMapper.toHybridNodeArray(node)[0];
		if(node_type == LLRConfig.NODE_TYPE.LABEL.ordinal()){
			int tag_id = NetworkIDMapper.toHybridNodeArray(node)[1];
			OutputLabel label = this._output_labels.get(tag_id);
			inst.setPredictedOutput(label);
		}
	}
	
}
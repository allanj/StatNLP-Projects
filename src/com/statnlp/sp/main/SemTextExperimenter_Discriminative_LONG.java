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
package com.statnlp.sp.main;

import java.io.IOException;
import java.util.ArrayList;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GenerativeNetworkModel;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.sp.GeoqueryEvaluator;
import com.statnlp.sp.HybridGrammar;
import com.statnlp.sp.HybridGrammarReader;
import com.statnlp.sp.SemTextDataManager;
import com.statnlp.sp.SemTextFeatureManager;
import com.statnlp.sp.SemTextFeatureManager_Discriminative_NEW;
import com.statnlp.sp.SemTextFeatureManager_Discriminative_LONG;
import com.statnlp.sp.SemTextInstance;
import com.statnlp.sp.SemTextInstanceReader;
import com.statnlp.sp.SemTextNetworkCompiler;
import com.statnlp.sp.SemanticForest;

public class SemTextExperimenter_Discriminative_LONG {
	
	public static void main(String args[]) throws IOException, InterruptedException{
		
		System.err.println(SemTextExperimenter_Discriminative_LONG.class.getCanonicalName());
		
		String lang = args[1];
		String inst_filename = "data/geoquery/geoFunql-"+lang+".corpus";
		String init_filename = "data/geoquery/geoFunql-"+lang+".init.corpus";
		String g_filename = "data/hybridgrammar.txt";
		
		String train_ids = "data/geoquery-2012-08-27/splits/split-880/run-0/fold-0/train-N600";//+args[1];
		String test_ids = "data/geoquery-2012-08-27/splits/split-880/run-0/fold-0/test";
		
		boolean isGeoquery = true;
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig._CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig._numThreads = Integer.parseInt(args[0]);
		NetworkConfig._SEMANTIC_PARSING_NGRAM = Integer.parseInt(args[2]);
		
		int numIterations = Integer.parseInt(args[3]);
		
		NetworkConfig._SEMANTIC_FOREST_MAX_DEPTH = Integer.parseInt(args[4]);
		
		SemTextDataManager dm = new SemTextDataManager();
		
		ArrayList<SemTextInstance> inits = SemTextInstanceReader.readInit(init_filename, dm);
		ArrayList<SemTextInstance> insts_train = SemTextInstanceReader.read(inst_filename, dm, train_ids, true);
		ArrayList<SemTextInstance> insts_test = SemTextInstanceReader.read(inst_filename, dm, test_ids, false);
		
//		insts_test = insts_train;
		
		int size = insts_train.size();
		if(NetworkConfig.TRAIN_MODE_IS_GENERATIVE){
			size += inits.size();
		}
		
		SemTextInstance train_instances[] = new SemTextInstance[size];
		for(int k = 0; k<insts_train.size(); k++){
			train_instances[k] = insts_train.get(k);
			train_instances[k].setInstanceId(k);
			train_instances[k].setLabeled();
		}
		
		if(NetworkConfig.TRAIN_MODE_IS_GENERATIVE){
			for(int k = 0; k<inits.size(); k++){
				train_instances[k+insts_train.size()] = inits.get(k);
				train_instances[k+insts_train.size()].setInstanceId(k+insts_train.size());
				train_instances[k+insts_train.size()].setLabeled();
			}
		}
		
		System.err.println("Read.."+train_instances.length+" instances.");
		
		HybridGrammar g = HybridGrammarReader.read(g_filename);
		
		SemanticForest forest_global = SemTextInstanceReader.toForest(dm);
		
		SemTextFeatureManager_Discriminative_LONG fm = new SemTextFeatureManager_Discriminative_LONG(new GlobalNetworkParam(), g, dm);
//		SemTextFeatureManager fm = new SemTextFeatureManager(new GlobalNetworkParam(), g, dm);
		
		SemTextNetworkCompiler compiler = new SemTextNetworkCompiler(g, forest_global, dm);
		
		NetworkModel model = NetworkConfig.TRAIN_MODE_IS_GENERATIVE ? GenerativeNetworkModel.create(fm, compiler) : DiscriminativeNetworkModel.create(fm, compiler);
		
		fm.isTrain = true;
		model.train(train_instances, numIterations);
		
		fm.isTrain = false;
		fm.clearCache();
		
		SemTextInstance test_instances[];
		Instance[] output_instances_unlabeled;
		
		test_instances = new SemTextInstance[insts_test.size()];
		for(int k = 0; k<test_instances.length; k++){
			test_instances[k] = insts_test.get(k);
			test_instances[k].setUnlabeled();
		}
		output_instances_unlabeled = model.decode(test_instances);
		
		double total = output_instances_unlabeled.length;
		double corr = 0;
		
		GeoqueryEvaluator eval = new GeoqueryEvaluator();
		
		ArrayList<String> expts = new ArrayList<String>();
		ArrayList<String> preds = new ArrayList<String>();
		
		for(int k = 0; k<output_instances_unlabeled.length; k++){
			Instance output_inst_U = output_instances_unlabeled[k];
			boolean r;
			try{
				r = output_inst_U.getOutput().equals(output_inst_U.getPrediction());
			} catch(Exception e){
				System.err.println("STRANGE");
				System.err.println("OUT:"+output_inst_U.getOutput());
				System.err.println("PRE:"+output_inst_U.getPrediction());
				r = false;
			}
			System.err.println(output_inst_U.getInstanceId()+":\t"+r);
			if(r){
				corr ++;
			}
			System.err.println("=INPUT=");
			System.err.println(output_inst_U.getInput());
			System.err.println("=OUTPUT=");
			System.err.println(output_inst_U.getOutput());
			System.err.println("=PREDICTION=");
			System.err.println(output_inst_U.getPrediction());
			
			String expt = eval.toGeoQuery((SemanticForest)output_inst_U.getOutput());
			String pred = eval.toGeoQuery((SemanticForest)output_inst_U.getPrediction());
			
			expts.add(expt);
			preds.add(pred);
			
			if(isGeoquery){
				System.err.println("output:\t"+expt);
				System.err.println("predic:\t"+pred);
			}
		}
		
		System.err.println("text accuracy="+corr/total+"="+corr+"/"+total);
		eval.eval(preds, expts);
		
	}
	
}
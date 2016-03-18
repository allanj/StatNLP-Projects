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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

import com.statnlp.commons.ml.classification.logisticregression.OutputLabel;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.GlobalNetworkParam.TRAIN_MODE;
import com.statnlp.hybridnetworks.NetworkParam_LV;

public class LLRExperimenter {
	
	public static void main(String args[]) throws FileNotFoundException{
		
		NetworkConfig.L2_REGULARIZATION_CONSTANT = 0;//1.0;
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		
		NetworkParam_LV param = new NetworkParam_LV(TRAIN_MODE.DISCRIMINATIVE);
		
		LLRFeatureManager fm = new LLRFeatureManager(param);
		
		String filename_train = "data/simple/a1a.train.txt";
		String filename_test = "data/simple/a1a.test.txt";
		
		LLRModel model = new LLRModel(fm);
		
		ArrayList<LabeledLLRInstance> insts_train = readTrain(filename_train, model);
		ArrayList<UnlabeledLLRInstance> insts_test = readTest(filename_test, model);
				
		System.err.println("There are "+insts_train.size()+" training instances.");
		System.err.println("There are "+insts_test.size()+" test instances.");
		
		param.setFactorAndSwitchToOptimizeLikelihood(80);
		
		model.train(insts_train);
		
		double count_corr = 0;
		
		for(int k = 0; k<insts_test.size(); k++){
			UnlabeledLLRInstance inst = insts_test.get(k);
			model.max(inst);
			count_corr += inst.countNumCorrectlyPredicted();
			
		}
		
//		double obj = param.getOldObj();
		
//		param.setFactorAndSwitchToOptimizeLikelihood(-obj);
		
//		LLRConfig.MAX_LBFGS_ITRS = 1000;
//		
//		model.train_likelihood(insts_train);
		
		
		System.err.println(insts_test.size());
		System.err.println(count_corr);
		
		System.err.println("-ACCURACY-");
		System.err.println("P:"+(count_corr/insts_test.size()));
		
	}
	
	private static ArrayList<UnlabeledLLRInstance> readTest(String filename, LLRModel model) throws FileNotFoundException{
		ArrayList<LabeledLLRInstance> insts = readTrain(filename, model);
		ArrayList<UnlabeledLLRInstance> insts_unlabeled = new ArrayList<UnlabeledLLRInstance>();
		for(int k = 0; k<insts.size(); k++){
			insts_unlabeled.add(insts.get(k).removeOutput());
		}
		return insts_unlabeled;
	}
	
	private static ArrayList<LabeledLLRInstance> readTrain(String filename, LLRModel model) throws FileNotFoundException{
		Scanner scan = new Scanner(new File(filename));
		ArrayList<LabeledLLRInstance> insts = new ArrayList<LabeledLLRInstance>();
		
		int id = 0;
		String line;
		while(scan.hasNextLine()){
			id ++;
			line = scan.nextLine();
			StringTokenizer st = new StringTokenizer(line);
			String label = st.nextToken();
			String[] inputs = new String[st.countTokens()];
			int k = 0;
			while(st.hasMoreTokens()){
				inputs[k++] = st.nextToken().split("\\:")[0];
			}
			OutputLabel output = model.toOutputLabel(label);
			LabeledLLRInstance inst = new LabeledLLRInstance(id, inputs, output);
			insts.add(inst);
		}
		
		return insts;
	}
	
}

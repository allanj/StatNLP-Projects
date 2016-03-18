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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.GlobalNetworkParam.TRAIN_MODE;

public class LRWeightLearner {
	
	private static double computeVariance(double weights[]){
		double mean = computeMean(weights);
		double square_mean = 0;
		for(int k = 0; k<weights.length; k++){
			square_mean += weights[k]*weights[k];
		}
		square_mean /= weights.length;
		return square_mean - mean * mean;
	}
	
	private static double computeMean(double weights[]){
		double mean = 0;
		for(int k = 0; k<weights.length; k++){
			mean += weights[k];
		}
		mean /= weights.length;
		return mean;
	}
	
	public static void main(String args[]) throws FileNotFoundException{
		
		NetworkConfig.L2_REGULARIZATION_CONSTANT = 10;
		
		GlobalNetworkParam param = new GlobalNetworkParam(TRAIN_MODE.DISCRIMINATIVE);
		
		LRFeatureManager fm = new LRFeatureManager(param);
		
		LRModel model = new LRModel(fm);
		
		for(int it = 0; it < 20; it++){
			
			param = new GlobalNetworkParam(TRAIN_MODE.DISCRIMINATIVE);
			fm = new LRFeatureManager(param);
			model = new LRModel(fm);
			
			NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
			
			String filename_train = "data/simple/a1a.train.txt.back";
			
			ArrayList<LabeledLRInstance> insts_train = readTrain(filename_train, model);
			
			System.err.println("There are "+insts_train.size()+" training instances.");
			
			model.train(insts_train);
			
			double variance_weights = computeVariance(param.getWeights());
			
			double residual[] = new double[insts_train.size()];
			for(int k = 0; k<insts_train.size(); k++){
				LabeledLRInstance inst = insts_train.get(k);
				residual[k] = model.residual(inst);
			}
			
			double variance_res = computeVariance(residual);
			
			double lambda = variance_res/variance_weights;
			
			NetworkConfig.L2_REGULARIZATION_CONSTANT = lambda;
			
			System.err.println("variance_weights="+variance_weights);
			System.err.println("variance_res="+variance_res);
			System.err.println("lambda="+lambda);
			
//			System.err.println("=PARAMETERS=");
//			for(int k = 0; k<param.getWeights().length; k++){
//				System.err.println(k+"="+param.getWeights()[k]);
//			}
//			System.err.println();
//			
//			System.err.println("=RESIDUALS=");
//			for(int k = 0; k<residual.length; k++){
//				System.err.println(k+"="+residual[k]);
//			}
//			System.err.println();
		}
		
		
		/**
		 * For testing...
		 */
		
		String filename_test = "data/simple/a1a.test.txt";
		ArrayList<UnlabeledLRInstance> insts_test = readTest(filename_test, model);
		System.err.println("There are "+insts_test.size()+" test instances.");
		
		double count_corr = 0;
		
		for(int k = 0; k<insts_test.size(); k++){
			UnlabeledLRInstance inst = insts_test.get(k);
			model.max(inst);
			count_corr += inst.countNumCorrectlyPredicted();
		}
		
		System.err.println(insts_test.size());
		System.err.println(count_corr);
		
		System.err.println("-ACCURACY-");
		System.err.println("P:"+(count_corr/insts_test.size()));
		
	}
	
	private static ArrayList<UnlabeledLRInstance> readTest(String filename, LRModel model) throws FileNotFoundException{
		ArrayList<LabeledLRInstance> insts = readTrain(filename, model);
		ArrayList<UnlabeledLRInstance> insts_unlabeled = new ArrayList<UnlabeledLRInstance>();
		for(int k = 0; k<insts.size(); k++){
			insts_unlabeled.add(insts.get(k).removeOutput());
		}
		return insts_unlabeled;
	}
	
	private static ArrayList<LabeledLRInstance> readTrain(String filename, LRModel model) throws FileNotFoundException{
		Scanner scan = new Scanner(new File(filename));
		ArrayList<LabeledLRInstance> insts = new ArrayList<LabeledLRInstance>();
		
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
			LabeledLRInstance inst = new LabeledLRInstance(id, inputs, output);
			insts.add(inst);
		}
		
		return insts;
	}
	
}

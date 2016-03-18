/** Statistical Natural Language Processing System
    Copyright (C) 2014-2015  Lu, Wei

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
/**
 * 
 */
package com.statnlp.cws;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GenerativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkModel;

/**
 * @author wei_lu
 *
 */
public class CWSExperimenter_TMP {
	
	public static void main(String args[])throws IOException, InterruptedException{
		
		System.err.println(CWSExperimenter_TMP.class.getCanonicalName());
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = 0;//0.01;
		CWSNetworkConfig.TAG_GRAM = 2;//Integer.parseInt(args[0]);
		NetworkConfig._numThreads = 10;//Integer.parseInt(args[1]);
		int numIterations = 100;//Integer.parseInt(args[2]);
		
		NetworkConfig._CACHE_FEATURES_DURING_TRAINING = true;
		
		String filename_train = "data/ctb5/ctb5.train.1";
		String filename_dev = "data/ctb5/ctb5.dev";
		String filename_test = "data/ctb5/ctb5.test";
		
		CWSReader reader = new CWSReader();
		
		CWSInstance[] insts_train = reader.readTrain(filename_train);
		
		ArrayList<CWSOutputTokenList> lists = reader.getLists(CWSNetworkConfig.TAG_GRAM);
		
		for(int k = 0; k<lists.size(); k++){
			System.err.println(k+"\t"+lists.get(k));
		}
		
		CWSOutputTokenSet tokenSet = reader.getOutputTokenSet();
		
		CWSNetworkCompiler compiler = new CWSNetworkCompiler(tokenSet, lists);
		
		CWSFeatureManager fm = new CWSFeatureManager(new GlobalNetworkParam());
		
		NetworkModel model = NetworkConfig.TRAIN_MODE_IS_GENERATIVE ? GenerativeNetworkModel.create(fm, compiler) : DiscriminativeNetworkModel.create(fm, compiler);
		
		model.train(insts_train, numIterations);

		ObjectOutputStream out = new ObjectOutputStream(
				new FileOutputStream("Gram="+CWSNetworkConfig.TAG_GRAM+"-Itrs="+numIterations+"-L2="+NetworkConfig.L2_REGULARIZATION_CONSTANT+".model"));
		out.writeObject(model);
		out.flush();
		out.close();
		
		System.err.println("Model saved.");
		
//		CWSInstance[] insts_test = reader.readTest(filename_test);
//		CWSInstance[] insts_test = reader.readTest(filename_train);
//		
//		Instance[] insts_predictions = model.decode(insts_test);
		
		for(String filename : new String[]{filename_dev, filename_test}){
		
			System.err.println("Results on: "+filename);
			
			CWSInstance[] insts_test = reader.readTest(filename);
			
			Instance[] insts_predictions = model.decode(insts_test);
			
			double count_expt, count_pred, count_corr;
			double P, R, F;

			{
				count_expt = 0;
				count_pred = 0;
				count_corr = 0;
				
				for(int k = 0; k<insts_predictions.length; k++){
					CWSInstance inst = (CWSInstance)insts_predictions[k];
//					
//					System.err.println(inst.getOutput());
//					System.err.println(inst.getPrediction());
//					System.exit(1);
					
					CWSOutput expt = inst.getOutput();
					CWSOutput pred = inst.getPrediction();
					
					count_expt += expt.getSegments().size();
					count_pred += pred.getSegments().size();
					count_corr += expt.countOverlaps(pred);
				}
				
				System.err.println(count_expt);
				System.err.println(count_pred);
				System.err.println(count_corr);
				
				P = count_corr/count_pred;
				R = count_corr/count_expt;
				F = 2/ (1/P + 1/R);
				
				System.err.println("P="+P);
				System.err.println("R="+R);
				System.err.println("F="+F);
			}
			
			{
				count_expt = 0;
				count_pred = 0;
				count_corr = 0;
				
				for(int k = 0; k<insts_predictions.length; k++){
					CWSInstance inst = (CWSInstance)insts_predictions[k];
//					
//					System.err.println(inst.getOutput());
//					System.err.println(inst.getPrediction());
//					System.exit(1);
					
					CWSOutput expt = inst.getOutput().toChunkOutput(tokenSet);
					CWSOutput pred = inst.getPrediction().toChunkOutput(tokenSet);
					
					count_expt += expt.getSegments().size();
					count_pred += pred.getSegments().size();
					count_corr += expt.countOverlaps(pred);
				}
				
				System.err.println(count_expt);
				System.err.println(count_pred);
				System.err.println(count_corr);
				
				P = count_corr/count_pred;
				R = count_corr/count_expt;
				F = 2/ (1/P + 1/R);
				
				System.err.println("P="+P);
				System.err.println("R="+R);
				System.err.println("F="+F);
			}
			
		}
		
	}
	
}

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

import com.statnlp.commons.types.InputToken;
import com.statnlp.commons.types.WordToken;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.GlobalNetworkParam;

public class LRExperimenter {
	
	public static void main(String args[]) throws FileNotFoundException{
		
		NetworkConfig.L2_REGULARIZATION_CONSTANT = 10.0;
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		
		GlobalNetworkParam param = new GlobalNetworkParam();
		
		LRFeatureManager fm = new LRFeatureManager(param);
		
		String filename_train = "data/simple/a1a.train.txt";
		String filename_test = "data/simple/a1a.test.txt";
		
		LRModel model = new LRModel(fm);
		
		ArrayList<LabeledLRInstance> insts_train = readTrain(filename_train, model);
		ArrayList<UnlabeledLRInstance> insts_test = readTest(filename_test, model);
		
		System.err.println("There are "+insts_train.size()+" training instances.");
		System.err.println("There are "+insts_test.size()+" test instances.");
		
		model.train(insts_train);
		
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
//			String[] inputs = new String[st.countTokens()];
			InputToken[] inputs = new InputToken[st.countTokens()];
			int k = 0;
			while(st.hasMoreTokens()){
				inputs[k++] = new WordToken(st.nextToken().split("\\:")[0]);
			}
			OutputLabel output = model.toOutputLabel(label);
			LabeledLRInstance inst = new LabeledLRInstance(id, 1.0, inputs, output);
			insts.add(inst);
		}
		
		return insts;
	}
	
}

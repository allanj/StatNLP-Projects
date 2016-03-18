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
package com.statnlp.mt.commons;

import java.io.IOException;

import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GenerativeNetworkModel;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkModel;

public class BitextEmbeddingExperimenter {
	
	public static void main(String args[]) throws IOException, InterruptedException{
		
		String src_filename = "data/FBIS/c.output.txt.1";
		String tgt_filename = "data/FBIS/e.output.txt.1";
		
		NetworkConfig.RANDOM_INIT_WEIGHT = true;
		NetworkConfig.RANDOM_INIT_FEATURE_SEED = 111;
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = true;
		NetworkConfig._numThreads = 4;//Integer.parseInt(args[0]);
		int numCepts = 1000;//Integer.parseInt(args[1]);
		
		BitextInstanceReader reader = new BitextInstanceReader();
		
		BitextInstance[] train_instances = reader.readBitext(src_filename, tgt_filename, 100);
		
		BitextFeatureManager fm = new BitextFeatureManager(new GlobalNetworkParam());
		
		BitextNetworkCompiler compiler = new BitextNetworkCompiler(numCepts);
		
		NetworkModel model = NetworkConfig.TRAIN_MODE_IS_GENERATIVE ? GenerativeNetworkModel.create(fm, compiler) : DiscriminativeNetworkModel.create(fm, compiler);
		
		model.train(train_instances, 10);
		
		BitextEmbeddingManager manager = new BitextEmbeddingManager(fm.getParam_G());
		manager.createEmbeddings(numCepts);
		manager.displayVectorRepresentation_src("中国");
		manager.displayVectorRepresentation_tgt("China");
		manager.displayDistance_src_tgt("中国", 0.2);
		manager.displayDistance_src_tgt("中国", "China");
		manager.displayDistance_src_tgt("主席", "president");
		manager.displayDistance_src_tgt("很", "China");
		manager.displayDistance_tgt("president", "chairman");
		
	}
	
}
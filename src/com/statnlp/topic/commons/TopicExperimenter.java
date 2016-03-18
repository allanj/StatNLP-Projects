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
package com.statnlp.topic.commons;

import java.io.IOException;

import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GenerativeNetworkModel;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkModel;

public class TopicExperimenter {
	
	public static void main(String args[]) throws IOException, InterruptedException{
		
		String src_filename = "data/IWSLT09/train_sub/train.en";
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = true;
		NetworkConfig._numThreads = Integer.parseInt(args[0]);
		int numTopics = 5;
		
		TopicInstanceReader reader = new TopicInstanceReader();
		
		TopicInstance[] train_instances = reader.readInstances(src_filename);
		
		TopicFeatureManager fm = new TopicFeatureManager(new GlobalNetworkParam());
		
		TopicNetworkCompiler compiler = new TopicNetworkCompiler(numTopics);
		
		NetworkModel model = NetworkConfig.TRAIN_MODE_IS_GENERATIVE ? GenerativeNetworkModel.create(fm, compiler)
				: DiscriminativeNetworkModel.create(fm, compiler);
		
		model.train(train_instances, 1000);
		
	}
	
}
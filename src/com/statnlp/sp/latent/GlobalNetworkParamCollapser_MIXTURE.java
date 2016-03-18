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
package com.statnlp.sp.latent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;

/**
 * @author wei_lu
 *
 */
public class GlobalNetworkParamCollapser_MIXTURE {
	
	public static void collapse(GlobalNetworkParam param){
		
		param.unlockForNewFeaturesAndFixCurrentFeatures();
		
		HashMap<String, HashMap<String, HashMap<String, Integer>>> map = param.getFeatureIntMap();
		
		Iterator<String> keys;
		keys = map.keySet().iterator();
		ArrayList<String> keys_str = new ArrayList<String>();
		while(keys.hasNext()){
			String key = keys.next();
			System.err.println(key);
			keys_str.add(key);
		}
		
		String type1 = "unit->cept";
		String type2 = "cept->word";
		
		HashMap<String, HashMap<String, Integer>> map1 = map.get(type1);
		HashMap<String, HashMap<String, Integer>> map2 = map.get(type2);
		
		keys = map1.keySet().iterator();
		while(keys.hasNext()){
			String unit = keys.next();
			HashMap<String, Integer> subMap = map1.get(unit);
			Iterator<String> cepts = subMap.keySet().iterator();
			while(cepts.hasNext()){
				String cept = cepts.next();
				HashMap<String, Integer> word2f = map2.get(cept);
				Iterator<String> words = word2f.keySet().iterator();
				while(words.hasNext()){
					String word = words.next();
					param.toFeature("unit->word", unit, word);
				}
			}
		}
		
		NetworkConfig.RANDOM_INIT_WEIGHT = false;
		NetworkConfig.FEATURE_INIT_WEIGHT = Double.NEGATIVE_INFINITY;
		param.lockIt();
		
		keys = map1.keySet().iterator();
		while(keys.hasNext()){
			String unit = keys.next();
//			System.err.println(unit);
			HashMap<String, Integer> subMap = map1.get(unit);
			Iterator<String> cepts = subMap.keySet().iterator();
			while(cepts.hasNext()){
				String cept = cepts.next();
				int f1 = subMap.get(cept);
				double weight1 = param.getWeight(f1);
//				System.err.println(f1+"\t"+weight1);
				HashMap<String, Integer> word2f = map2.get(cept);
				Iterator<String> words = word2f.keySet().iterator();
				while(words.hasNext()){
					String word = words.next();
//					System.err.println(word);
					int f2 = word2f.get(word);
					double weight2 = param.getWeight(f2);
					double weight = weight1 + weight2;
//					System.err.println(f2+"\t"+weight2);
					int f = param.toFeature("unit->word", unit, word);
					double oldWeight = param.getWeight(f);
					
					if(oldWeight == Double.NEGATIVE_INFINITY){
						param.overRideWeight(f, weight);
					} else if(weight == Double.NEGATIVE_INFINITY){
						//do nothing...
					} else if(oldWeight < weight){
						double newWeight = Math.log1p(Math.exp(oldWeight-weight))+weight;
						if(newWeight == Double.POSITIVE_INFINITY){
							System.err.println(oldWeight);
							System.err.println(weight);
							System.err.println("xx");
							System.exit(1);
						}
						param.overRideWeight(f, newWeight);
					} else {
						double newWeight = Math.log1p(Math.exp(weight-oldWeight))+oldWeight;
						if(newWeight == Double.POSITIVE_INFINITY){
							System.err.println("yy");
							System.exit(1);
						}
						param.overRideWeight(f, newWeight);
					}
				}
			}
		}
		
		keys = map1.keySet().iterator();
		while(keys.hasNext()){
			String unit = keys.next();
			HashMap<String, Integer> subMap = map1.get(unit);
			Iterator<String> cepts = subMap.keySet().iterator();
			while(cepts.hasNext()){
				String cept = cepts.next();
				int f = subMap.get(cept);
				param.overRideWeight(f, 0.0);
			}
		}
		
		keys = map.keySet().iterator();
		while(keys.hasNext()){
			String type = keys.next();
			System.err.println(type+"\t"+map.get(type).size());
		}
		
	}

}

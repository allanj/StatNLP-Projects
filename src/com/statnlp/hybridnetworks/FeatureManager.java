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
package com.statnlp.hybridnetworks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

public abstract class FeatureManager implements Serializable{
	
	private static final long serialVersionUID = 7999836838043433954L;
	
	//the number of networks.
	protected transient int _numNetworks;
	//the cache that stores the features
	protected transient FeatureArray[][][] _cache;
	
	//the parameters associated with the network.
	protected GlobalNetworkParam _param_g;
	//the local feature maps, one for each thread.
	protected LocalNetworkParam[] _params_l;
	//check whether the cache is enabled.
	protected boolean _cacheEnabled = false;
	
	
	protected int _numThreads;
	
	public FeatureManager(GlobalNetworkParam param_g){
		this._param_g = param_g;
		this._numThreads = NetworkConfig._numThreads;
		this._params_l = new LocalNetworkParam[this._numThreads];
		this._cacheEnabled = false;
	}
	
	public void setLocalNetworkParams(int threadId, LocalNetworkParam param_l){
		this._params_l[threadId] = param_l;
	}
	
	
	//update the parameters.
	public synchronized boolean update(){
		
		//if the number of thread is 1, then your local param fetches information directly from the global param.
		if(NetworkConfig._numThreads!=1){
			this._param_g.resetCountsAndObj();
			
			for(LocalNetworkParam param_l : this._params_l){
				int[] fs = param_l.getFeatures();
				for(int f_local = 0; f_local<fs.length; f_local++){
					int f_global = fs[f_local];
					double count = param_l.getCount(f_local);
					this._param_g.addCount(f_global, count);
				}
				this._param_g.addObj(param_l.getObj());
			}
		}
		
		boolean done = this._param_g.update();
		
		if(NetworkConfig._numThreads != 1){
			for(LocalNetworkParam param_l : this._params_l){
				param_l.reset();
			}
		} else {
			this._param_g.resetCountsAndObj();
		}
		return done;
	}
	
	public void enableCache(int numNetworks){
		this._numNetworks = numNetworks;
		this._cache = new FeatureArray[numNetworks][][];
		this._cacheEnabled = true;
	}
	
	public void disableCache(){
		this._cache = null;
		this._cacheEnabled = false;
	}
	
	public boolean isCacheEnabled(){
		return this._cacheEnabled;
	}
	
	public GlobalNetworkParam getParam_G(){
		return this._param_g;
	}
	
	public LocalNetworkParam[] getParams_L(){
		return this._params_l;
	}
	
	public void mergeSubFeaturesToGlobalFeatures(){
		HashMap<String, HashMap<String, HashMap<String, Integer>>> globalFeature2IntMap = this._param_g.getFeatureIntMap();
		
		//if global mode, only 1 thread
		if(this._param_g._subFeatureIntMaps.size()==1){
			this._param_g._featureIntMap = new HashMap<String, HashMap<String, HashMap<String, Integer>>>(this._param_g._subFeatureIntMaps.get(0));
			this._param_g._size = this._param_g._subSize[0];
			this._params_l[0].finalizeIt();
			this._param_g._subFeatureIntMaps.set(0, null);
		}else{
			this._param_g._size = 0;
			for(int t=0;t<this._param_g._subFeatureIntMaps.size();t++){
				addIntoGlobalFeatures(globalFeature2IntMap, this._param_g._subFeatureIntMaps.get(t),this._params_l[t]._globalFeature2LocalFeature);
				this._params_l[t].finalizeIt();
				this._param_g._subFeatureIntMaps.set(t, null);
			}
			//System.err.println(globalFeature2IntMap.toString());
		}
		//to release the memory
//		for(int t=0;t<this._param_g._subFeatureIntMaps.size();t++){
//			this._param_g._subFeatureIntMaps.set(t, null);
//		}
		
		//complete the type2int map. only in generative model
		if(NetworkConfig.TRAIN_MODE_IS_GENERATIVE){
			completeType2Int(globalFeature2IntMap, this._param_g._type2inputMap); 
		}
		
	}
	
	private void addIntoGlobalFeatures(HashMap<String, HashMap<String, HashMap<String, Integer>>> globalMap, HashMap<String, HashMap<String, HashMap<String, Integer>>> localMap, HashMap<Integer, Integer> gf2lf){
		Iterator<String> iter1 = localMap.keySet().iterator();
		while(iter1.hasNext()){
			String localType = iter1.next();
			HashMap<String, HashMap<String, Integer>> localOutput2input = localMap.get(localType);
			if(globalMap.containsKey(localType)){
				HashMap<String, HashMap<String, Integer>> globalOutput2input = globalMap.get(localType);
				Iterator<String> iter2 = localOutput2input.keySet().iterator();
				while(iter2.hasNext()){
					String localOutput = iter2.next();
					HashMap<String, Integer> localInput2int = localOutput2input.get(localOutput);
					if(globalOutput2input.containsKey(localOutput)){
						HashMap<String, Integer> globalInput2int = globalOutput2input.get(localOutput);
						Iterator<String> iter3 = localInput2int.keySet().iterator();
						while(iter3.hasNext()){
							String localInput = iter3.next();
							if(!globalInput2int.containsKey(localInput)){
								globalInput2int.put(localInput, this._param_g._size++);
							}
							gf2lf.put(globalInput2int.get(localInput), localInput2int.get(localInput));
						}
					}else{
						//not contain the local output
						HashMap<String, Integer> globalInput2int = new HashMap<String, Integer>(); 
						Iterator<String> iter3 = localInput2int.keySet().iterator();
						while(iter3.hasNext()){
							String localInput = iter3.next();
							globalInput2int.put(localInput, this._param_g._size++);
							gf2lf.put(globalInput2int.get(localInput), localInput2int.get(localInput));
						}
						globalOutput2input.put(localOutput, globalInput2int);
					}
				}
			}else{
				HashMap<String, HashMap<String, Integer>> globalOutput2input = new HashMap<String, HashMap<String, Integer>>();
				Iterator<String> iter2 = localOutput2input.keySet().iterator();
				while(iter2.hasNext()){
					String localOutput = iter2.next();
					HashMap<String, Integer> globalInput2int = new HashMap<String, Integer>(); 
					HashMap<String, Integer> localInput2int = localOutput2input.get(localOutput);
					Iterator<String> iter3 = localInput2int.keySet().iterator();
					while(iter3.hasNext()){
						String localInput = iter3.next();
						globalInput2int.put(localInput, this._param_g._size++);
						gf2lf.put(globalInput2int.get(localInput), localInput2int.get(localInput));
					}
					globalOutput2input.put(localOutput, globalInput2int);
				}
				globalMap.put(localType, globalOutput2input);
			}
		}
	}
	
	private void completeType2Int(HashMap<String, HashMap<String, HashMap<String, Integer>>> globalMap,HashMap<String, ArrayList<String>> type2Input){
		Iterator<String> iterType = globalMap.keySet().iterator();
		while(iterType.hasNext()){
			String type = iterType.next();
			if(!type2Input.containsKey(type)){
				type2Input.put(type, new ArrayList<String>());
			}
			HashMap<String, HashMap<String, Integer>> output2input  = globalMap.get(type);
			Iterator<String> iterOutput = output2input.keySet().iterator();
			while(iterOutput.hasNext()){
				String output = iterOutput.next();
				HashMap<String, Integer> input2int = output2input.get(output);
				Iterator<String> iterInput = input2int.keySet().iterator();
				while(iterInput.hasNext()){
					String input = iterInput.next();
					ArrayList<String> inputs = type2Input.get(type);
					int index = Collections.binarySearch(inputs, input);
					if(index<0){
						inputs.add(-1-index, input);
					}
				}
			}
		}
	}
	
	
	public FeatureArray extract(Network network, int parent_k, int[] children_k, int children_k_index){
		if(this.isCacheEnabled()){
			if(this._cache[network.getNetworkId()] == null){
				this._cache[network.getNetworkId()] = new FeatureArray[network.countNodes()][];
			}
			if(this._cache[network.getNetworkId()][parent_k] == null){
				this._cache[network.getNetworkId()][parent_k] = new FeatureArray[network.getChildren(parent_k).length];
			}
			if(this._cache[network.getNetworkId()][parent_k][children_k_index] != null){
				return this._cache[network.getNetworkId()][parent_k][children_k_index];
			}
		}
		
		FeatureArray fa = this.extract_helper(network, parent_k, children_k);
		
		if(this.isCacheEnabled()){
			this._cache[network.getNetworkId()][parent_k][children_k_index] = fa;
		}
		return fa;
	}
	
	protected abstract FeatureArray extract_helper(Network network, int parent_k, int[] children_k);
	
}
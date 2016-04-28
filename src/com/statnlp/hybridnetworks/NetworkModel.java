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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Instance;
import com.statnlp.dp.utils.DPConfig;

public abstract class NetworkModel implements Serializable{
	
	private static final long serialVersionUID = 8695006398137564299L;
	
	//the global feature manager.
	protected FeatureManager _fm;
	//the builder
	protected NetworkCompiler _compiler;
	//the list of instances.
	
	protected transient Instance[] _allInstances;
	//the number of threads.
	protected transient int _numThreads = NetworkConfig._numThreads;
	//the local learners.
	private transient LocalNetworkLearnerThread[] _learners;
	//the local decoder.
	private transient LocalNetworkDecoderThread[] _decoders;
	
	public NetworkModel(FeatureManager fm, NetworkCompiler compiler){
		this._fm = fm;
		this._numThreads = NetworkConfig._numThreads;
		this._compiler = compiler;
	}
	
	public int getNumThreads(){
		return this._numThreads;
	}
	
	protected abstract Instance[][] splitInstancesForTrain();
	
	public Instance[][] splitInstancesForTest() {
		
		System.err.println("#instances="+this._allInstances.length);
		
		Instance[][] insts = new Instance[this._numThreads][];

		ArrayList<ArrayList<Instance>> insts_list = new ArrayList<ArrayList<Instance>>();
		int threadId;
		for(threadId = 0; threadId<this._numThreads; threadId++){
			insts_list.add(new ArrayList<Instance>());
		}
		
		threadId = 0;
		for(int k = 0; k<this._allInstances.length; k++){
			Instance inst = this._allInstances[k];
			insts_list.get(threadId).add(inst);
			threadId = (threadId+1)%this._numThreads;
		}
		for(threadId = 0; threadId<this._numThreads; threadId++){
			int size = insts_list.get(threadId).size();
			insts[threadId] = new Instance[size];
			for(int i = 0; i < size; i++){
				Instance inst = insts_list.get(threadId).get(i);
				insts[threadId][i] = inst;
			}
			System.out.println("Thread "+threadId+" has "+insts[threadId].length+" instances.");
		}
		
		return insts;
	}
	
	public void train(Instance[] allInstances, int maxNumIterations) throws InterruptedException{
		
		this._numThreads = NetworkConfig._numThreads;
		
		this._allInstances = allInstances;
		for(int k = 0; k<this._allInstances.length; k++){
			//System.err.println(k);
			this._allInstances[k].setInstanceId(k+1);
		}
		
		//create the threads.
		this._learners = new LocalNetworkLearnerThread[this._numThreads];
		
		Instance[][] insts = this.splitInstancesForTrain();
		
		//distribute the works into different threads.
		//WARNING: must do the following sequentially..
		if(NetworkConfig._SEQUENTIAL_FEATURE_EXTRACTION){
			for(int threadId = 0; threadId<this._numThreads; threadId++){
				this._learners[threadId] = new LocalNetworkLearnerThread(threadId, this._fm, insts[threadId], this._compiler, -1);
				this._learners[threadId].touch();
				System.err.println("Okay..thread "+threadId+" touched.");
			}
		}else{
			for(int threadId = 0; threadId<this._numThreads; threadId++){
				this._learners[threadId] = new LocalNetworkLearnerThread(threadId, this._fm, insts[threadId], this._compiler, -1);
				this._learners[threadId].setTouch();
				this._learners[threadId].start();
			}
			for(int k = 0; k<this._numThreads; k++){
				this._learners[k].join();
				this._learners[k].setUnTouch();
			}
			this._fm.mergeSubFeaturesToGlobalFeatures();
		}
		
		//finalize the features.
		this._fm.getParam_G().lockIt();
		double obj_old = Double.NEGATIVE_INFINITY;
		
		//run the EM-style algorithm now...
		for(int it = 0; it<maxNumIterations; it++){
			
			long time = System.currentTimeMillis();
			for(int threadId = 0; threadId<this._numThreads; threadId++){
				this._learners[threadId] = this._learners[threadId].copyThread();
				this._learners[threadId].start();
			}
			for(int k = 0; k<this._numThreads; k++){
				this._learners[k].join();
			}
			boolean done = this._fm.update();
			time = System.currentTimeMillis() - time;
			double obj = this._fm.getParam_G().getObj_old();
			System.out.println("Iteration "+it+"\tObjective="+obj+"\tTime="+time/1000.0+" seconds."+"\t"+obj/obj_old);
			if(NetworkConfig.TRAIN_MODE_IS_GENERATIVE && it>1 && obj<obj_old && Math.abs(obj-obj_old)>1E-5){
				throw new RuntimeException("Error:\n"+obj_old+"\n>\n"+obj);
			}
			obj_old = obj;
//			if(done){
//				System.out.println("Training completes. No significant progress after "+it+" iterations.");
//				break;
//			}
		}
		/******DEBUG info: write the feature weight***/
		if(DPConfig.writeWeight){
			try {
				PrintWriter pw = RAWF.writer(DPConfig.weightPath);
				for(int tf=0;tf<this._fm._param_g._feature2rep.length;tf++){
					//System.err.println(Arrays.toString(this._fm._param_g._feature2rep[tf])+"  feature weight:"+this._fm.getParam_G().getWeight(tf));
					pw.write(Arrays.toString(this._fm._param_g._feature2rep[tf])+"<MYSEP>"+this._fm.getParam_G().getWeight(tf)+"\n");
				}
				pw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		 //now read the weight
		if(DPConfig.readWeight){
			try {
				HashMap<String, Double> f2weight = new HashMap<String, Double>();
				//HashSet<String> obtained = new HashSet<String>();
				BufferedReader br = RAWF.reader(DPConfig.weightPath);
				String line = null;
				while((line = br.readLine())!=null){
					String[] values = line.split("<MYSEP>");
					f2weight.put(values[0], Double.parseDouble(values[1]));
				}
				System.err.println("total number of ecrf weight:"+f2weight.size());
				int totalObtained = 0;
				for(int tf=0;tf<this._fm._param_g._feature2rep.length;tf++){
					String nowf = Arrays.toString(this._fm._param_g._feature2rep[tf]);
					if(!f2weight.containsKey(nowf)){
						//System.err.println(nowf);
						//this._fm.getParam_G().setWeight(tf, 0.0);
					}else{
						if(this._fm._param_g._feature2rep[tf][0].equals("local"))
							this._fm.getParam_G().overRideWeight(tf, f2weight.get(nowf)/2);
						else{
							this._fm.getParam_G().overRideWeight(tf, f2weight.get(nowf));
						}
						totalObtained++;
						f2weight.remove(nowf);
					}
				}
				System.err.println("Total feature weight obtained from ECRF:"+totalObtained);
				Iterator<String> iter = f2weight.keySet().iterator();
				while(iter.hasNext()){
					System.err.println(iter.next());
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
//		System.err.println("feature 2317:"+Arrays.toString(this._fm._param_g._feature2rep[2317])+" weight:"+this._fm.getParam_G().getWeight(2317));
//		System.err.println("feature 997:"+Arrays.toString(this._fm._param_g._feature2rep[998])+" weight:"+this._fm.getParam_G().getWeight(998));
//		System.err.println("feature 998:"+Arrays.toString(this._fm._param_g._feature2rep[997])+" weight:"+this._fm.getParam_G().getWeight(997));
//		System.err.println("feature 16:"+Arrays.toString(this._fm._param_g._feature2rep[16])+" weight:"+this._fm.getParam_G().getWeight(16));
//		System.err.println("feature 17:"+Arrays.toString(this._fm._param_g._feature2rep[17])+" weight:"+this._fm.getParam_G().getWeight(17));
		/*********/
	}
	
	public Instance[] decode(Instance[] allInstances) throws InterruptedException{
		
//		if(NetworkConfig.TRAIN_MODE_IS_GENERATIVE){
//			this._fm.getParam_G().expandFeaturesForGenerativeModelDuringTesting();
//		}
		
		this._numThreads = NetworkConfig._numThreads;
		System.err.println("#threads:"+this._numThreads);
		
		Instance[] results = new Instance[allInstances.length];
		
		//all the instances.
		this._allInstances = allInstances;
		
		//create the threads.
		this._decoders = new LocalNetworkDecoderThread[this._numThreads];
		
		Instance[][] insts = this.splitInstancesForTest();
		
		//distribute the works into different threads.
		for(int threadId = 0; threadId<this._numThreads; threadId++){
			this._decoders[threadId] = new LocalNetworkDecoderThread(threadId, this._fm, insts[threadId], this._compiler);
		}
		
		System.err.println("Okay. Decoding started.");
		
		long time = System.currentTimeMillis();
		for(int threadId = 0; threadId<this._numThreads; threadId++){
			this._decoders[threadId].start();
		}
		for(int threadId = 0; threadId<this._numThreads; threadId++){
			this._decoders[threadId].join();
		}
		
		System.err.println("Okay. Decoding done.");
		time = System.currentTimeMillis() - time;
		System.err.println("Overall decoding time = "+ time/1000.0 +" secs.");
		
		int k = 0;
		for(int threadId = 0; threadId<this._numThreads; threadId++){
			Instance[] outputs = this._decoders[threadId].getOutputs();
			for(Instance output : outputs){
				results[k++] = output;
			}
		}
		
		Arrays.sort(results, new Comparator<Instance>(){
			@Override
			public int compare(Instance o1, Instance o2) {
				if(o1.getInstanceId()<o2.getInstanceId())
				return -1;
				else return 1;
			}
		});
		
		return results;
	}
	
}

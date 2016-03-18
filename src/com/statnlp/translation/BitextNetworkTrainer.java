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
package com.statnlp.translation;

import java.io.IOException;
import java.util.ArrayList;

import com.statnlp.commons.BitextInstance;
import com.statnlp.commons.Word;
import com.statnlp.commons.algo.IBMModel1;
import com.statnlp.commons.types.SequencePair;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.GlobalNetworkParam.TRAIN_MODE;
import com.statnlp.translation.types.BitextNetwork;
import com.statnlp.translation.types.BitextNetworkConstructor;
import com.statnlp.translation.types.NoRemovalBitextNetwork;
import com.statnlp.util.BitextReader;

public class BitextNetworkTrainer {
	
	private ArrayList<BitextInstance> _instances;
	private IBMModel1<Word, Word> _model1;
	
	public static void main(String args[])throws IOException{
		
		ArrayList<BitextInstance> instances = BitextReader.read("data/IWSLT/CE/train/train.zh", "data/IWSLT/CE/train/train.en", -1, 25);
		
		System.err.println(instances.size()+" instances.");
		GlobalNetworkParam param = new GlobalNetworkParam(TRAIN_MODE.GENERATIVE);
		BitextFeatureManager fm = new BitextFeatureManager(null, param);
		
		BitextNetworkTrainer trainer = new BitextNetworkTrainer(instances);
		trainer.train(10);
		
//
////		Network[] networks = new Network[instances.size()];
//		long xtime = System.currentTimeMillis();
//		for(int k = 0; k<instances.size(); k++){
//			BitextInstance instance = instances.get(k);
//			double[][] membership = null;//trainer._model1.getMembership(k);
//			Network network = trainer.buildNetwork(instance, membership, fm);
//		}
//		xtime = System.currentTimeMillis() - xtime;
//		System.err.println("time:"+xtime);
//		System.exit(1);
//		
//		Network[] networks = new Network[instances.size()];
		long bTime = System.currentTimeMillis();
		for(int k = 0; k<instances.size(); k++){
			if((k+1)%100==0)
			{
				System.err.print('.');
				bTime = System.currentTimeMillis() - bTime;
				System.err.println("time:"+bTime+"ms");
				bTime = System.currentTimeMillis();
			}
//			System.err.println(k+"/"+instances.size());
			BitextInstance instance = instances.get(k);
//			if(!instance.getSrc().toString().contains("法郎")){
//				continue;
//			}
//			double[][] membership = trainer._model1.getMembership(k);
			double[][] membership = null;//trainer._model1.getMembership(k);
			Network network = trainer.buildNetwork(instance, membership, fm);
			network.touch();
		}
		System.err.println();
		
		long eTime = System.currentTimeMillis();
		System.err.println((eTime-bTime)+"ms");
		
		param.lockIt();
		System.err.println("OK."+param.countFeatures()+" features created.");
		
		{
			System.gc();
			long ram = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			System.err.println("ram:"+ram/1000/1000+"MB");
		}

		double obj_old = Double.NEGATIVE_INFINITY;
		for(int it = 0; it<1000; it++){
			long time = System.currentTimeMillis();
			
			for(int k = 0; k<instances.size(); k++){
				System.err.print('.');
				BitextInstance instance = instances.get(k);
//				if(!instance.getSrc().toString().contains("法郎")){
//					continue;
//				}
				double[][] membership = null;//trainer._model1.getMembership(k);
				Network network = trainer.buildNetwork(instance, membership, fm);
				network.train();
//				param.preview_count();
//				System.exit(1);
			}
			System.err.println();
			
			System.err.println();
			double obj = param.getObj();
			param.update();
			time = System.currentTimeMillis() - time;
			double improvement = (obj-obj_old);
			System.err.println("Time for this iteration:"+time+";\tobj="+obj+";\timprovement="+improvement);
			obj_old = obj;
			System.gc();
			long ram = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			System.err.println("ram:"+ram/1000/1000+"MB");
			if(improvement<0 && it>1){
				System.exit(1);
			}
		}
		
	}
	
//	private long[][] _cache_nodes = new long[1000][];
//	private int instanceId = 0;
//	
	public BitextNetwork buildNetwork(BitextInstance instance, double[][] membership, BitextFeatureManager fm){
		Word[] srcWords = instance.getSrc().getWords();
		Word[] tgtWords = instance.getTgt().getWords();
		BitextNetworkConstructor c = new BitextNetworkConstructor(srcWords.length, tgtWords.length, membership);
		c.build_topdown_exact();
		BitextNetwork network = new BitextNetwork(instance, fm, c.getNodes(), c.getChildren());
		return network;
	}
	
	public BitextNetworkTrainer(ArrayList<BitextInstance> instances){
		this._instances = instances;
		ArrayList<SequencePair<Word, Word>> pairList = new ArrayList<SequencePair<Word, Word>>();
		for(BitextInstance instance : this._instances){
			Word[] srcWords = instance.getSrc().getWords();
			Word[] tgtWords = instance.getTgt().getWords();
			ArrayList<Word> srcWordArray = new ArrayList<Word>();
			ArrayList<Word> tgtWordArray = new ArrayList<Word>();
			for(Word srcWord : srcWords){
				srcWordArray.add(srcWord);
			}
			for(Word tgtWord : tgtWords)
				tgtWordArray.add(tgtWord);
			pairList.add(new SequencePair<Word, Word>(srcWordArray, tgtWordArray, 1.0));
		}
		this._model1 = new IBMModel1<Word, Word>(pairList);
	}
	
	public void train(int N){
		this._model1.EM(N);
	}
	
	public IBMModel1 getModel(){
		return this._model1;
	}
	
}

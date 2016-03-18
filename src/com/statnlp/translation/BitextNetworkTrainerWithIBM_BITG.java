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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import com.statnlp.commons.BitextInstance;
import com.statnlp.commons.Word;
import com.statnlp.commons.algo.IBMModel1;
import com.statnlp.commons.types.SequencePair;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.GlobalNetworkParam.TRAIN_MODE;
import com.statnlp.translation.types.BitextNetworkConstructor;
import com.statnlp.translation.types.FasterBitextNetwork;
import com.statnlp.util.BitextReader;

public class BitextNetworkTrainerWithIBM_BITG {
	
	private ArrayList<BitextInstance> _instances;
	private IBMModel1<String, String> _model1;
	
	public static void main(String args[])throws IOException{
		
		long time;
		int maxLen = 25;
		
		String src = args[0];
		String tgt = args[1];
		int BITG_MAX_LEN = Integer.parseInt(args[2]);
		
		ArrayList<BitextInstance> instances = BitextReader.read("data/IWSLT/CE/train-"+maxLen+"/train."+src, "data/IWSLT/CE/train-"+maxLen+"/train."+tgt, -1, -1);
		System.err.println(instances.size()+" instances");
		
		GlobalNetworkParam param = new GlobalNetworkParam(TRAIN_MODE.GENERATIVE);
		BitextFeatureManager fm = new BitextFeatureManager(null, param);
		
		BitextNetworkConstructor c = new BitextNetworkConstructor(maxLen+2, maxLen+2);
		time = System.currentTimeMillis();
		c.build_topdown_BITG(BITG_MAX_LEN);
		time = System.currentTimeMillis() - time;
		System.err.println(time+"ms for building.");
		{
			System.gc();
			long ram = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			System.err.println("ram:"+ram/1000/1000+"MB");
		}
		
		long[] nodes = c.getNodes();
		int[][][] children = c.getChildren();
		
		BitextNetworkTrainerWithIBM_BITG trainer = new BitextNetworkTrainerWithIBM_BITG(instances);
		trainer.trainEM(5);
		
//		Network[] networks = new Network[instances.size()];
		long bTime = System.currentTimeMillis();
		long tm = System.currentTimeMillis();
		for(int k = 0; k<instances.size(); k++){
			if((k+1)%100==0) 
			{
				System.err.print('.');
				tm = System.currentTimeMillis() - tm;
				System.err.println("time:"+tm+"ms");
				tm = System.currentTimeMillis();
			}
//			System.err.println(k+"/"+instances.size());
			BitextInstance instance = instances.get(k);
			Word[] srcwords = instance.getSrc().getWords();
			Word[] tgtwords = instance.getTgt().getWords();
			int num_nodes = c.countNodes(srcwords.length, tgtwords.length);
//			System.err.println(NetworkIDMapper.viewHybridNode(nodes[num_nodes-1])+"<<-");
			Network network = trainer.buildNetwork(instance, null, fm, nodes, children, num_nodes);
			network.touch();
		}
		System.err.println();
		
		long eTime = System.currentTimeMillis();
		tm = (eTime-bTime);
		System.err.println(time+"ms;"+(tm/1000.0)/3600+"hrs");
		
//		param.lockIt(trainer._model1);
		param.lockIt();
		System.err.println("OK."+param.countFeatures()+" features created.");
		
		{
			System.gc();
			long ram = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			System.err.println("ram:"+ram/1000/1000+"MB");
		}

		double obj_old = Double.NEGATIVE_INFINITY;
		for(int it = 0; it<20; it++){
			time = System.currentTimeMillis();
			
			tm = System.currentTimeMillis();
			for(int k = 0; k<instances.size(); k++){
				if((k+1)%100==0) 
				{
					System.err.print('.');
					tm = System.currentTimeMillis() - tm;
					System.err.println("time:"+tm+"ms");
					tm = System.currentTimeMillis();
				}
				BitextInstance instance = instances.get(k);
				Word[] srcwords = instance.getSrc().getWords();
				Word[] tgtwords = instance.getTgt().getWords();
				int num_nodes = c.countNodes(srcwords.length, tgtwords.length);
				Network network = trainer.buildNetwork(instance, null, fm, nodes, children, num_nodes);
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
			System.err.println("Time for this iteration:"+time+"ms="+time/1000.0/3600+"hrs;\tobj="+obj+";\timprovement="+improvement);
			obj_old = obj;
			System.gc();
			long ram = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			System.err.println("ram:"+ram/1000/1000+"MB");
			if(improvement<0 && it>1){
				System.exit(1);
			}
			
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File("param/bitg/with_ibm/"+src+"-"+tgt+"."+it+".len="+BITG_MAX_LEN+".data")));
			out.writeObject(param);
			out.flush();
			out.close();
			
		}
		
	}
	
//	private long[][] _cache_nodes = new long[1000][];
//	private int instanceId = 0;
//	
	public FasterBitextNetwork buildNetwork(BitextInstance instance, double[][] membership, BitextFeatureManager fm, long[] nodes, int[][][] children, int num_nodes){
		FasterBitextNetwork network = new FasterBitextNetwork(instance, fm, nodes, children, num_nodes, null);
		return network;
	}
	
	public BitextNetworkTrainerWithIBM_BITG(ArrayList<BitextInstance> instances){
		this._instances = instances;
		ArrayList<SequencePair<String, String>> pairList = new ArrayList<SequencePair<String, String>>();
		for(BitextInstance instance : this._instances){
			Word[] srcWords = instance.getSrc().getWords();
			Word[] tgtWords = instance.getTgt().getWords();
			ArrayList<String> srcWordArray = new ArrayList<String>();
			ArrayList<String> tgtWordArray = new ArrayList<String>();
			for(int k = 0; k<srcWords.length; k++){
				srcWordArray.add(srcWords[k].getName());
			}
			srcWordArray.add("<NULL>");
			for(int k = 0; k<tgtWords.length; k++){
				tgtWordArray.add(tgtWords[k].getName());
			}
			tgtWordArray.add("<NULL>");
			pairList.add(new SequencePair<String, String>(srcWordArray, tgtWordArray, 1.0));
		}
		{
			ArrayList<String> srcWordArray;
			ArrayList<String> tgtWordArray;
			
			srcWordArray = new ArrayList<String>();
			tgtWordArray = new ArrayList<String>();
			srcWordArray.add(Word.START.getName());
			tgtWordArray.add(Word.START.getName());
			pairList.add(new SequencePair<String, String>(srcWordArray, tgtWordArray, 1000.0));
			
			srcWordArray = new ArrayList<String>();
			tgtWordArray = new ArrayList<String>();
			srcWordArray.add(Word.FINISH.getName());
			tgtWordArray.add(Word.FINISH.getName());
			pairList.add(new SequencePair<String, String>(srcWordArray, tgtWordArray, 1000.0));
		}
		this._model1 = new IBMModel1<String, String>(pairList);
	}
	
	public void trainEM(int N){
		this._model1.EM(N);
//		System.err.println(this._model1.getProb_ext("[START]", "[START]")+"<<START");
//		System.err.println(this._model1.getProb_ext("[FINISH]", "[FINISH]")+"<<FINISH");
//		System.exit(1);
	}
	
}

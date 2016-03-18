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
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.GlobalNetworkParam.EXP_MODE;
import com.statnlp.hybridnetworks.GlobalNetworkParam.TRAIN_MODE;
import com.statnlp.translation.types.BitextNetworkConstructor;
import com.statnlp.translation.types.FasterBitextNetwork;
import com.statnlp.util.BitextReader;

public class BitextNetworkTrainerWithoutIBM {
	
	private ArrayList<BitextInstance> _instances;
	
	public static void main(String args[])throws IOException{
		
		long time;
		int maxLen = 25;
		
		int num_it = 10;//Integer.parseInt(args[0]);
		
		String src = args[0];
		String tgt = args[1];
		
		ArrayList<BitextInstance> instances = BitextReader.read("data/IWSLT/CE/train-"+maxLen+"/train."+src, "data/IWSLT/CE/train-"+maxLen+"/train."+tgt, -1, -1);
		System.err.println(instances.size()+" instances");
		
		GlobalNetworkParam param = new GlobalNetworkParam(TRAIN_MODE.GENERATIVE);
		BitextFeatureManager fm = new BitextFeatureManager(null, param);
		
		BitextNetworkConstructor c = new BitextNetworkConstructor(maxLen+2, maxLen+2);
		time = System.currentTimeMillis();
		c.build_topdown();
		time = System.currentTimeMillis() - time;
		System.err.println(time+"ms for building.");
		{
			System.gc();
			long ram = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			System.err.println("ram:"+ram/1000/1000+"MB");
		}
		
		long[] nodes = c.getNodes();
		int[][][] children = c.getChildren();
		
		BitextNetworkTrainerWithoutIBM trainer = new BitextNetworkTrainerWithoutIBM(instances);
		
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
//			System.err.println("num_nodes="+num_nodes);
//			System.err.println(NetworkIDMapper.viewHybridNode(nodes[num_nodes-1])+"<<-");
			Network network = trainer.buildNetwork(instance, fm, nodes, children, num_nodes);
			network.touch();
		}
		System.err.println();
		
		long eTime = System.currentTimeMillis();
		tm = (eTime-bTime);
		System.err.println(time+"ms;"+(tm/1000.0)/3600+"hrs");
		
		param.lockIt();
		System.err.println("OK."+param.countFeatures()+" features created.");
		
		{
			System.gc();
			long ram = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			System.err.println("ram:"+ram/1000/1000+"MB");
		}

		double obj_old = Double.NEGATIVE_INFINITY;
		for(int it = 0; it<num_it; it++){
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
				Network network = trainer.buildNetwork(instance, fm, nodes, children, num_nodes);
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
			
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File("param/itg/no_ibm/"+src+"-"+tgt+"."+it+".data")));
			out.writeObject(param);
			out.flush();
			out.close();
			
		}
		
	}
	
//	private long[][] _cache_nodes = new long[1000][];
//	private int instanceId = 0;
	
	public FasterBitextNetwork buildNetwork(BitextInstance instance, BitextFeatureManager fm, long[] nodes, int[][][] children, int num_nodes, EXP_MODE exp_mode){
		FasterBitextNetwork network = new FasterBitextNetwork(instance, fm, nodes, children, num_nodes, exp_mode);
		return network;
	}
	
	public BitextNetworkTrainerWithoutIBM(ArrayList<BitextInstance> instances){
		this._instances = instances;
	}
	
}

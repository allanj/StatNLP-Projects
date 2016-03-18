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
package com.statnlp.util;

import java.io.IOException;
import java.util.ArrayList;

import com.statnlp.commons.BitextInstance;
import com.statnlp.commons.Word;
import com.statnlp.commons.algo.IBMModel1;
import com.statnlp.commons.types.SequencePair;
import com.statnlp.translation.types.BitextNetwork;
import com.statnlp.translation.types.BitextNetworkConstructor;

public class IBMModel1Trainer {
	
	private ArrayList<BitextInstance> _instances;
	private IBMModel1<Word, Word> _model1;
	
	public static void main(String args[])throws IOException{
		
		ArrayList<BitextInstance> instances = BitextReader.read("data/IWSLT/CE/train/train.zh", "data/IWSLT/CE/train/train.en", 100, 20);
//		
//		int maxSrcLen = -1;
//		int maxTgtLen = -1;
//		for(int k = 0; k<instances.size(); k++){
//			BitextInstance instance = instances.get(k);
//			Word[] tgtWords = instance.getTgt().getWords();
//			Word[] srcWords = instance.getSrc().getWords();
//			if(tgtWords.length>maxSrcLen)
//				maxSrcLen = srcWords.length;
//			if(srcWords.length>maxTgtLen)
//				maxTgtLen = tgtWords.length;
//		}
//		
//		System.err.println(maxSrcLen+","+maxTgtLen);
//		
//		System.exit(1);
		
		IBMModel1Trainer trainer = new IBMModel1Trainer(instances);
		trainer.train(10);
		
		long bTime = System.currentTimeMillis();
		for(int k = 0; k<instances.size(); k++){
			System.err.println(k+"/"+instances.size());
			BitextInstance instance = instances.get(k);
//			Word[] tgtWords = instance.getTgt().getWords();
//			Word[] srcWords = instance.getSrc().getWords();
//			if(tgtWords.length>25 || srcWords.length>25){
//				continue;
//			}
			long time;
			time = System.currentTimeMillis();
			double[][] membership = trainer._model1.getMembership(k);
			BitextNetwork network = trainer.buildNetwork(instance, membership);
			time = System.currentTimeMillis() - time;
			System.err.println("time:"+time+"ms");
		}
		long eTime = System.currentTimeMillis();
		System.err.println((eTime-bTime)+"ms");
		
//		
//		for(int k = 0; k<instances.size(); k++){
//			BitextInstance instance = instances.get(k);
//			Word[] tgtWords = instance.getTgt().getWords();
//			Word[] srcWords = instance.getSrc().getWords();
//			double[][] membership = trainer._model1.getMembership(k);
//			System.err.println(tgtWords.length+","+srcWords.length+"/"+membership.length+","+membership[0].length);
//			System.err.println("-MEMBERSHIP TABLE-");
//			for(int tgtIndex = 0; tgtIndex<tgtWords.length; tgtIndex++){
//				for(int srcIndex = 0; srcIndex<srcWords.length; srcIndex++){
//					double v = membership[tgtIndex][srcIndex];
//					if(v<1E-10)
//						v = 0.00;
//					if(v==1.0)
//						System.err.print("1.00"+"\t");
//					else if(v==0)
//						System.err.print("0.00"+"\t");
//					else
//						System.err.print(v+"\t");
////					else if(v<0.1)
////						System.err.print(("0.0"+(int)(v*100))+"\t");
////					else
////						System.err.print(("0."+(int)(v*100))+"\t");
//				}
//				System.err.println();
//			}
//			System.err.println();
//
//			System.err.println("-PROBABILITY TABLE-");
//			for(int tgtIndex = 0; tgtIndex<tgtWords.length; tgtIndex++){
//				for(int srcIndex = 0; srcIndex<srcWords.length; srcIndex++){
//					double v = trainer._model1.getProb(srcWords[srcIndex], tgtWords[tgtIndex]);
//					if(v<1E-10)
//						v = 0.00;
//					if(v==1.0)
//						System.err.print("1.00"+"\t");
//					else if(v==0)
//						System.err.print("0.00"+"\t");
//					else
//						System.err.print(v+"\t");
//				}
//				System.err.println();
//			}
//			System.err.println();
//		}
		
	}

	public BitextNetwork buildNetwork(BitextInstance instance, double[][] membership){
		Word[] srcWords = instance.getSrc().getWords();
		Word[] tgtWords = instance.getTgt().getWords();
		
//		BitextNetwork network = new BitextNetwork();
		System.err.println(srcWords.length+"x"+tgtWords.length);
		BitextNetworkConstructor c = new BitextNetworkConstructor(srcWords.length, tgtWords.length, membership);
		c.build_topdown_exact();
		System.err.println("done.");
		
		return null;
	}
	
	public IBMModel1Trainer(ArrayList<BitextInstance> instances){
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

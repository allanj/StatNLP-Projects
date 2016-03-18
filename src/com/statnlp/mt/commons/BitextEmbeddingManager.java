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
package com.statnlp.mt.commons;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import com.statnlp.commons.ml.opt.MathsVector;
import com.statnlp.hybridnetworks.GlobalNetworkParam;

/**
 * @author wei_lu
 *
 */
public class BitextEmbeddingManager implements Serializable{
	
	private static final long serialVersionUID = 5803240514449518506L;
	
	private HashMap<String, double[]> _src2vec;
	private HashMap<String, double[]> _tgt2vec;
	private HashMap<String, HashMap<String, HashMap<String, Integer>>> _featureIntMap;
	private GlobalNetworkParam _param;
	
	public BitextEmbeddingManager(GlobalNetworkParam param){
		this._param = param;
		this._src2vec = new HashMap<String, double[]>();
		this._tgt2vec = new HashMap<String, double[]>();
		this._featureIntMap = param.getFeatureIntMap();
	}
	
	public void displayVectorRepresentation_src(String word){
		double[] v =  this._src2vec.get(word);
		System.err.println(word+"\t"+Arrays.toString(v));
	}
	
	public void displayVectorRepresentation_tgt(String word){
		double[] v =  this._tgt2vec.get(word);
		System.err.println(word+"\t"+Arrays.toString(v));
	}
	
	public void createEmbeddings(int v_dim){
//		Iterator<String> types = this._featureIntMap.keySet().iterator();
		
		String type;
		HashMap<String, HashMap<String, Integer>> output2input;
		Iterator<String> outputs;
		
		int v_size = -1;
		
		type = "src-cept";
		
		output2input = this._featureIntMap.get(type);
		outputs = output2input.keySet().iterator();
		while(outputs.hasNext()){
			String output = outputs.next();
			HashMap<String, Integer> input2id = output2input.get(output);
			Iterator<String> inputs = input2id.keySet().iterator();
			double[] v = new double[v_dim];//[input2id.size()];
			v_size = v.length;
			while(inputs.hasNext()){
				String input = inputs.next();
				String s = input.split("\\-")[1];
				if(!s.startsWith("i=")){
					continue;
				}
				int k = Integer.parseInt(s.split("\\=")[1]);
				int feature = input2id.get(input);
				double value = this._param.getWeight(feature);
//				System.err.println(output+"\t"+input+"\t"+value);
				v[k] = Math.exp(value);
			}
			this._src2vec.put(output, v);
			if(output.equals("#SRC_NULL#"))
				System.err.println(output+"\t"+Arrays.toString(v));
		}
		
		type = "cept-tgt";
		
		output2input = this._featureIntMap.get(type);
		outputs = output2input.keySet().iterator();
		while(outputs.hasNext()){
			String output = outputs.next();
			String s = output.split("\\-")[1];
			if(!s.startsWith("i=")){
				continue;
			}
			int k = Integer.parseInt(s.split("\\=")[1]);
			
			HashMap<String, Integer> input2id = output2input.get(output);
			Iterator<String> inputs = input2id.keySet().iterator();
			while(inputs.hasNext()){
				String input = inputs.next();
				int feature = input2id.get(input);
				double value = this._param.getWeight(feature);
				
				if(!this._tgt2vec.containsKey(input)){
					this._tgt2vec.put(input, new double[v_size]);
				}
				this._tgt2vec.get(input)[k] = Math.exp(value);
			}
		}
		
	}
	
	public void displayDistance_src(String word_src, double threshold){
		double[] v = this._src2vec.get(word_src);
		Iterator<String> src_words = this._src2vec.keySet().iterator();
		System.err.println(word_src+":");
		while(src_words.hasNext()){
			String src_word = src_words.next();
//			System.err.println("checking.."+src_word);
			if(src_word.equals(word_src)){
				continue;
			}
			double[] v1 = this._src2vec.get(src_word);
			double distance = MathsVector.distance(v, v1);
			if(distance<=threshold){
				System.err.println(src_word+"\t"+distance);
			}
		}
	}
	
	public void displayDistance_src_tgt(String word_src, double threshold){
		double[] v = this._src2vec.get(word_src);
		Iterator<String> tgt_words = this._tgt2vec.keySet().iterator();
		System.err.println(word_src+":");
		while(tgt_words.hasNext()){
			String tgt_word = tgt_words.next();
//			System.err.println("checking.."+src_word);
			double[] v1 = this._tgt2vec.get(tgt_word);
			double distance = MathsVector.distance(v, v1);
			if(distance<=threshold){
				System.err.println(tgt_word+"\t"+distance);
			}
		}
	}
	
	public void displayDistance_tgt(String word_tgt, double threshold){
		double[] v = this._tgt2vec.get(word_tgt);
		Iterator<String> tgt_words = this._tgt2vec.keySet().iterator();
		System.err.println(word_tgt+":");
		while(tgt_words.hasNext()){
			String tgt_word = tgt_words.next();
//			System.err.println("checking.."+src_word);
			if(tgt_word.equals(word_tgt)){
				continue;
			}
			double[] v1 = this._tgt2vec.get(tgt_word);
			double distance = MathsVector.distance(v, v1);
			if(distance<=threshold){
				System.err.println(tgt_word+"\t"+distance);
			}
		}
	}

	public void displayDistance_src(String word1, String word2){
		double[] v1 = this._src2vec.get(word1);
		double[] v2 = this._src2vec.get(word2);
		double sim = MathsVector.distance(v1, v2);
		System.err.println("distance("+word1+","+word2+")="+sim);
		System.err.println(Arrays.toString(v1));
		System.err.println(Arrays.toString(v2));
	}
	
	public void displayDistance_tgt(String word1, String word2){
		double[] v1 = this._tgt2vec.get(word1);
		double[] v2 = this._tgt2vec.get(word2);
		double sim = MathsVector.distance(v1, v2);
		System.err.println("distance("+word1+","+word2+")="+sim);
		System.err.println(Arrays.toString(v1));
		System.err.println(Arrays.toString(v2));
	}
	
	public void displayDistance_src_tgt(String word1, String word2){
		double[] v1 = this._src2vec.get(word1);
		double[] v2 = this._tgt2vec.get(word2);
		double sim = MathsVector.distance(v1, v2);
		System.err.println("distance("+word1+","+word2+")="+sim);
		System.err.println(Arrays.toString(v1));
		System.err.println(Arrays.toString(v2));
	}
	
}

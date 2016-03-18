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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import com.statnlp.commons.BitextInstance;
import com.statnlp.commons.Word;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.translation.types.BitextNetworkConstructor;
import com.statnlp.translation.types.FasterBitextNetwork;
import com.statnlp.util.BitextReader;

public class BitextAlignmentFinder_BITG {
	
	public static void main(String args[]) throws FileNotFoundException, IOException, ClassNotFoundException{
		
		String src = args[0];
		String tgt = args[1];
		int BITG_MAX_LEN = Integer.parseInt(args[2]);
		int it = Integer.parseInt(args[3]);
		int maxLen = 25;
		
		BitextNetworkConstructor c = new BitextNetworkConstructor(maxLen+2, maxLen+2);
		c.build_topdown_BITG(BITG_MAX_LEN);
		System.err.println("Networks built.");
		
		long[] nodes = c.getNodes();
		int[][][] children = c.getChildren();
		
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(new File("param/bitg/with_ibm/"+src+"-"+tgt+"."+it+".len="+BITG_MAX_LEN+".data")));
		GlobalNetworkParam param = (GlobalNetworkParam)in.readObject();
		in.close();
		BitextFeatureManager fm = new BitextFeatureManager(null, param);
		
		BitextAlignmentFinder_BITG finder = new BitextAlignmentFinder_BITG();
		
		ArrayList<BitextInstance> instances = BitextReader.read("data/IWSLT/CE/train-"+maxLen+"/train."+src, "data/IWSLT/CE/train-"+maxLen+"/train."+tgt, -1, -1);
		
		String data_filename = "all_exp/bitg/with_ibm/len"+BITG_MAX_LEN+"/it"+it+"/giza."+tgt+"-"+src+"/"+tgt+"-"+src+".A3.final";
		OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(data_filename), "UTF-8");
		BufferedWriter p = new BufferedWriter(out);
		
		int num_inst = instances.size();
		for(int k = 0; k<num_inst; k++){
			System.err.println("Instance "+k);
			BitextInstance instance = instances.get(k);
			Word[] srcWords = instance.getSrc().getWords();
			Word[] tgtWords = instance.getTgt().getWords();
			
			int num_nodes = c.countNodes(srcWords.length, tgtWords.length);
			FasterBitextNetwork network = finder.buildNetwork(instance, fm, nodes, children, num_nodes);
			network.max();
			double score = network.getMax();
			AlignmentInfo info = finder.findAlignments(BITG_MAX_LEN, network, k+1);
			p.write(info.toGiza(score));
			p.write('\n');
			p.flush();
		}
		p.close();
	}
	
	public FasterBitextNetwork buildNetwork(BitextInstance instance, BitextFeatureManager fm, long[] nodes, int[][][] children, int num_nodes){
		FasterBitextNetwork network = new FasterBitextNetwork(instance, fm, nodes, children, num_nodes);
		return network;
	}
	
	public AlignmentInfo findAlignments(int BITG_MAX_LEN, FasterBitextNetwork network, int id) throws IOException{
		BitextInstance inst = (BitextInstance)network.getInstance();
		Word[] srcWords = inst.getSrc().getWords();
		Word[] tgtWords = inst.getTgt().getWords();
		AlignmentInfo info = new AlignmentInfo(id, srcWords, tgtWords);
		this.findAlignments_helper(BITG_MAX_LEN, network, srcWords, tgtWords, info, network.countNodes()-1);
		return info;
	}
	
	private void findAlignments_helper(int BITG_MAX_LEN, FasterBitextNetwork network, Word[] srcWords, Word[] tgtWords, AlignmentInfo info, int node_k){
		long node = network.get(node_k);
		
		BitextInstance inst = (BitextInstance)network.getInstance();
		if(!inst.getSrc().getWords().equals(srcWords) || !inst.getTgt().getWords().equals(tgtWords)){
			throw new RuntimeException("Why??96");
		}
		
		int[] array = NetworkIDMapper.toHybridNodeArray(node);
		int srcBIndex = array[1];
		int srcEIndex = array[1]+array[0];
		int tgtBIndex = array[3];
		int tgtEIndex = array[3]+array[2];
		int type = array[4];
		
		int[] paths_k = network.getMaxPath(node_k);
		
		if(type == BitextNetworkConstructor.TYPE.I00.ordinal()){
			if(srcEIndex!=srcBIndex+1){
				if(paths_k.length==0){
					throw new RuntimeException("Oh no..81:"+type+"/"+srcBIndex+","+srcEIndex+"///"+srcWords.length+","+tgtWords.length);
				} else {
					//not the terminal...
				}
			}
			else{
				if(tgtEIndex-tgtBIndex>BITG_MAX_LEN){
					throw new RuntimeException("...."+(tgtEIndex-tgtBIndex));
				}
				for(int tgtIndex = tgtBIndex; tgtIndex<tgtEIndex; tgtIndex++){
					info.addAlignment(srcBIndex, tgtIndex);
				}
			}
		}
		
		/*
		if(type == BitextNetworkConstructor.TYPE.I11.ordinal()){
			int child_k = paths_k[0];
			long child_node = network.get(child_k);
			
			int[] child_array = NetworkIDMapper.toHybridNodeArray(child_node);
			int child_type = child_array[4];
			if(child_type == BitextNetworkConstructor.TYPE.I11.ordinal()){
				info.addAlignment(srcEIndex-1, -1);
			}
		}
		*/

		if(type == BitextNetworkConstructor.TYPE.I10.ordinal()){
			int child_k = paths_k[0];
			long child_node = network.get(child_k);
			
			int[] child_array = NetworkIDMapper.toHybridNodeArray(child_node);
			int child_type = child_array[4];
			if(child_type == BitextNetworkConstructor.TYPE.I10.ordinal()){
				info.addAlignment(-1, tgtEIndex-1);
			}
		}
		
		/*
		if(type == BitextNetworkConstructor.TYPE.N11.ordinal()){
			int child_k = paths_k[0];
			long child_node = network.get(child_k);
			
			int[] child_array = NetworkIDMapper.toHybridNodeArray(child_node);
			int child_type = child_array[4];
			if(child_type == BitextNetworkConstructor.TYPE.N11.ordinal()){
				info.addAlignment(srcEIndex-1, -1);
			}
		}
		*/

		if(type == BitextNetworkConstructor.TYPE.N10.ordinal()){
			int child_k = paths_k[0];
			long child_node = network.get(child_k);
			
			int[] child_array = NetworkIDMapper.toHybridNodeArray(child_node);
			int child_type = child_array[4];
			if(child_type == BitextNetworkConstructor.TYPE.N10.ordinal()){
				info.addAlignment(-1, tgtBIndex);
			}
		}
		
		for(int path_k : paths_k){
			this.findAlignments_helper(BITG_MAX_LEN, network, srcWords, tgtWords, info, path_k);
		}
	}
	
	private class AlignmentInfo implements Serializable{
		private static final long serialVersionUID = 7940804922017696333L;
		
		private int _id;
		private Word[] _srcWords;
		private Word[] _tgtWords;
		private HashMap<Integer, ArrayList<Integer>> _map;
		
		public AlignmentInfo(int id, Word[] srcWords, Word[] tgtWords){
			this._id = id;
			this._srcWords = srcWords;
			this._tgtWords = tgtWords;
			this._map = new HashMap<Integer, ArrayList<Integer>>();
		}
		
		public String toGiza(double score){
			StringBuilder sb = new StringBuilder();
			
			sb.append("# Sentence pair ("+this._id+") source length "+(this._srcWords.length-2)+" target length "+(this._tgtWords.length-2)+" alignment score : "+Math.exp(score));
			sb.append('\n');
			for(int i=1; i<this._tgtWords.length-1; i++){
				sb.append(this._tgtWords[i].getName());
				sb.append(' ');
			}
			sb.append('\n');
			
			{
				sb.append("NULL ({");
				ArrayList<Integer> tgtwordids = this._map.get(0);
				if(tgtwordids != null){
					for(int k = 0; k<tgtwordids.size(); k++){
						sb.append(' ');
						sb.append(tgtwordids.get(k));
					}
				}
				sb.append(" }) ");
			}
			
			for(int i=1; i<this._srcWords.length-1; i++){
				sb.append(this._srcWords[i]);
				sb.append(' ');
				sb.append("({");
				ArrayList<Integer> tgtwordids = this._map.get(i);
				if(tgtwordids != null){
					for(int k = 0; k<tgtwordids.size(); k++){
						sb.append(' ');
						sb.append(tgtwordids.get(k));
					}
				}
				sb.append(" }) ");
			}
			
			return sb.toString();
		}
		
		public void addAlignment(int srcIndex, int tgtIndex){
			if(srcIndex==0 || tgtIndex==0)
				return;
			if(srcIndex==this._srcWords.length-1 || tgtIndex==this._tgtWords.length-1)
				return;
			if(srcIndex==-1)
				srcIndex = 0;
			
			if(!this._map.containsKey(srcIndex)){
				this._map.put(srcIndex, new ArrayList<Integer>());
			}
			ArrayList<Integer> tgtIndices = this._map.get(srcIndex);
			tgtIndices.add(tgtIndex);
			Collections.sort(tgtIndices);
		}
	}
	
}

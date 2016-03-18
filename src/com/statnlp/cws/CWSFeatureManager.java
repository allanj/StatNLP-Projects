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
package com.statnlp.cws;

import java.util.Arrays;

import com.statnlp.commons.types.WordToken;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;

/**
 * @author wei_lu
 *
 */
public class CWSFeatureManager extends FeatureManager{
	
	private static final long serialVersionUID = -1694459068236227592L;
	
	public boolean _createNoFeatures = false;
	
	public CWSFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
		
//		System.err.println(Arrays.toString(NetworkIDMapper.getCapacity()));
//		System.exit(1);
	}
	
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {

		if(_createNoFeatures)
			return FeatureArray.EMPTY;
		
		CWSNetwork cNetwork = (CWSNetwork)network;
		
		CWSInstance inst = (CWSInstance)cNetwork.getInstance();
		WordToken[] inputs = inst.getInput();
		
		long parent = cNetwork.getNode(parent_k);
		int[] parent_ids = NetworkIDMapper.toHybridNodeArray(parent);
		
		if(children_k.length>1){
			throw new RuntimeException("children_k has length "+children_k.length+"\t"+parent_ids[1]);
		}
		
		if(parent_ids[1] == 2){
			int child_k = children_k[0];
			long child = cNetwork.getNode(child_k);
			int[] child_ids = NetworkIDMapper.toHybridNodeArray(child);
			
			if(child_ids[1] == 1){
				return FeatureArray.EMPTY;
			}
			else {
				throw new RuntimeException("child_ids[1]="+child_ids[1]);
			}
		}
		
		else if(parent_ids[1] == 1){
			//TODO..
			return FeatureArray.EMPTY;
		}
		
		else if(parent_ids[1] == 0){
			
//			int child_k = children_k[0];
//			long child = cNetwork.getNode(child_k);
//			int[] child_ids = NetworkIDMapper.toHybridNodeArray(child);
			
//			System.err.println("child_ids="+Arrays.toString(child_ids)+"\t"+child);
			
			int eIndex = parent_ids[0];
			int bIndex = eIndex - parent_ids[2];
			int[] fs = this.extract_features(inputs, bIndex, eIndex, parent_ids);
			return new FeatureArray(fs);
		}
		
		else {
			throw new RuntimeException("parent_ids[1]="+parent_ids[1]);
		}
		
	}
	
	private int[] extract_features(WordToken[] words, int bIndex, int eIndex, int[] tag_ids){
		
		if(bIndex==eIndex){
			System.err.println(bIndex+","+eIndex+"???");
			System.exit(1);
		}
		
		int[] fs = new int[eIndex-bIndex+1];
		
		{
			String type = "WORD_FEATURE";
			StringBuilder output_sb = new StringBuilder();
			for(int k = 3; k<tag_ids.length; k++){
				output_sb.append('+');
				output_sb.append(tag_ids[k]);
			}
			String output = output_sb.toString();
			for(int index = bIndex; index<eIndex; index++){
				String input = words[index].getName();
				fs[index-bIndex] = this._param_g.toFeature(type, output, input);
			}
		}
		
		{
			String type = "SPAN_FEATURE";
			StringBuilder output_sb = new StringBuilder();
			for(int k = 3; k<tag_ids.length; k++){
				output_sb.append('+');
				output_sb.append(tag_ids[k]);
			}
			String output = output_sb.toString();
			StringBuilder input_sb = new StringBuilder();
			for(int index = bIndex; index<eIndex; index++){
				input_sb.append('+');
				input_sb.append(words[index].getName());
			}
			String input = input_sb.toString();
			fs[fs.length-1] = this._param_g.toFeature(type, output, input);
		}
		return fs;
	}
//	
//	private int[] extract_span_features(WordToken[] words, int bIndex, int eIndex, int[] tag_ids){
//		int[] fs = new int[1];
//		
//		String type = "SPAN_FEATURE";
//		StringBuilder output_sb = new StringBuilder();
//		for(int k = 3; k<tag_ids.length; k++){
//			output_sb.append('+');
//			output_sb.append(tag_ids[k]);
//		}
//		String output = output_sb.toString();
//		StringBuilder input_sb = new StringBuilder();
//		for(int index = bIndex; index<eIndex; index++){
//			input_sb.append('+');
//			input_sb.append(words[index].getName());
//		}
//		String input = input_sb.toString();
//		fs[0] = this._param_g.toFeature(type, output, input);
//		return fs;
//	}

}

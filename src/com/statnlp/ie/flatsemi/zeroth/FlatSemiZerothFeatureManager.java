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
package com.statnlp.ie.flatsemi.zeroth;

import java.util.ArrayList;
import java.util.Iterator;

import com.statnlp.commons.AttributedWord;
import com.statnlp.commons.types.TextSpan;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.ie.types.MentionType;

public class FlatSemiZerothFeatureManager extends FeatureManager{
	
	private FlatSemiZerothNetwork _network;
	private FlatSemiZerothInstance _inst;
	private TextSpan _span;
	private final FeatureArray _EMPTY_ = new FeatureArray(this._param);
	
	public FlatSemiZerothFeatureManager(GlobalNetworkParam param) {
		super(param);
	}
	
	public int[][][][] _cache;
	public int[][][][][] _cache_span;
	
	public void enableCache(int num_insts, int num_tags){
		if(num_insts>1)
			System.err.println("cache enabled.");
		this._cache = new int[num_insts][num_tags][][];
		this._cache_span = new int[num_insts][num_tags][][][];
	}
	
	public void disableCache(){
		this._cache = null;
		this._cache_span = null;
	}
	
	private FeatureArray extract_span(int bWord_id, int eWord_id, int tag_id, FeatureArray fa){
		if(this._cache_span != null){
			int inst_id = this._inst.getInstanceId();
			if(inst_id<0){
				inst_id = -inst_id;
			}
			if(this._cache_span[inst_id-1][tag_id-1]==null){
				this._cache_span[inst_id-1][tag_id-1] = new int[this._span.length()][][];
			}
			if(this._cache_span[inst_id-1][tag_id-1][bWord_id]==null){
				this._cache_span[inst_id-1][tag_id-1][bWord_id] = new int[this._span.length()-bWord_id][];
			}
			if(this._cache_span[inst_id-1][tag_id-1][bWord_id][eWord_id-bWord_id-1]!=null){
				int[] fs = this._cache_span[inst_id-1][tag_id-1][bWord_id][eWord_id-bWord_id-1];
				return new FeatureArray(fs, fa);
			}
		}
		
		ArrayList<String> forms = new ArrayList<String>();
		String prev2 = "";
		String prev1 = "";
		String prev2_POS = "";
		String prev1_POS = "";
		for(int word_id = bWord_id; word_id<eWord_id; word_id++){
			String word = this._span.getWord(word_id).getName();
			String POS = this._span.getWord(word_id).getAttribute("POS").get(0);
			forms.add("BIGRAM:"+prev1+"_"+word);
			forms.add("TRIGRAM:"+prev2+"_"+prev1+"_"+word);
			forms.add("BIGRAMPOS:"+prev1_POS+"_"+POS);
			forms.add("TRIGRAMPOS:"+prev2_POS+"_"+prev1_POS+"_"+POS);
			prev2 = prev1;
			prev1 = word;
		}
		
		String POS = "";
		String word = "";
		forms.add("BIGRAM:"+prev1+"_"+word);
		forms.add("TRIGRAM:"+prev2+"_"+prev1+"_"+word);
		forms.add("BIGRAMPOS:"+prev1_POS+"_"+POS);
		forms.add("TRIGRAMPOS:"+prev2_POS+"_"+prev1_POS+"_"+POS);
		
		StringBuilder sb;
		sb = new StringBuilder();
		for(int word_id = bWord_id; word_id<eWord_id; word_id++){
			word = this._span.getWord(word_id).getName();
			sb.append('+');
			sb.append(word);
		}
		forms.add("WORDFORM:"+sb.toString());
		
		int[] fs = new int[forms.size()];
		for(int k = 0; k<fs.length; k++){
			fs[k] = this._param.toFeature("tag:"+tag_id, sb.toString());
		}
		
		if(this._cache_span!=null){
			int inst_id = this._inst.getInstanceId();
			if(inst_id<0){
				inst_id = -inst_id;
			}
			this._cache_span[inst_id-1][tag_id-1][bWord_id][eWord_id-bWord_id-1] = fs;
		}
		
		fa  = new FeatureArray(fs, fa);
		return fa;
	}
	
	private FeatureArray extract(int word_id, int tag_id, FeatureArray fa){
		if(this._cache != null){
			int inst_id = this._inst.getInstanceId();
			if(inst_id<0){
				inst_id = -inst_id;
			}
			if(this._cache[inst_id-1][tag_id-1]==null){
				this._cache[inst_id-1][tag_id-1] = new int[this._span.length()][];
			}
			
			if(this._cache[inst_id-1][tag_id-1][word_id]!=null){
				int[] fs = this._cache[inst_id-1][tag_id-1][word_id];
				return new FeatureArray(fs, fa);
			}
		}
		
		int[] fs = this.extract_helper(word_id, tag_id);
		
		if(this._cache!=null){
			int inst_id = this._inst.getInstanceId();
			if(inst_id<0){
				inst_id = -inst_id;
			}
			this._cache[inst_id-1][tag_id-1][word_id] = fs;
		}
		
		fa  = new FeatureArray(fs, fa);
		return fa;
	}
	
	private int[] extract_helper(int word_id, int tag_id){
		TextSpan span = this._inst.getSpan();
		AttributedWord word = span.getWord(word_id);
		Iterator<String> atts = word.getAttributes().iterator();
		
		int num_atts = 0;
		for(String att :word.getAttributes()){
			num_atts += word.getAttribute(att).size();
		}
		
		int size = num_atts;
		
		int[] f = new int[size];
		int k = 0;
		while(atts.hasNext()){
			String att = atts.next();
			ArrayList<String> vals = word.getAttribute(att);
			
			for(String val : vals){
				f[k++] = this._param.toFeature(":"+tag_id+":"+att, val);
			}
		}
		
		if(k!=size)
			throw new RuntimeException("Wrong.."+k+"!="+size);
		
		return f;
	}
	
	@Override
	public void setNetwork(Network network) {
		this._network = (FlatSemiZerothNetwork)network;
		this._inst = (FlatSemiZerothInstance)this._network.getInstance();
		this._span = this._inst.getSpan();
	}
	
	@Override
	public FeatureArray extract(int parent_k, int[] children_k) {
		
		long node_parent = this._network.get(parent_k);
		int[] node_parent_arr = NetworkIDMapper.toHybridNodeArray(node_parent);
		int node_parent_type = node_parent_arr[4];
		
		if(node_parent_type == FlatSemiZerothConfig.NODE_TYPE.ROOT.ordinal()){
			return _EMPTY_;
		}
		
		if(node_parent_type == FlatSemiZerothConfig.NODE_TYPE.BEFORE.ordinal()){
			return _EMPTY_;
		}
		
		if(node_parent_type == FlatSemiZerothConfig.NODE_TYPE.EXACT.ordinal()){
			int eIndex = node_parent_arr[0];
			int bIndex = node_parent_arr[0] - node_parent_arr[1];
			int tag_id = node_parent_arr[3]-1;
			
			if(tag_id == MentionType._START_TYPE.getId()){
				return this._EMPTY_;
			}
			
			FeatureArray fa = this._EMPTY_;
			for(int word_id = bIndex; word_id<eIndex; word_id++){
				fa = this.extract(word_id, tag_id, fa);
			}
			
			fa = this.extract_span(bIndex, eIndex, tag_id, fa);
			
			return fa;
		}
		
		throw new RuntimeException("This should not happen.");
		
//		long node = this._network.get(parent_k);
//		long[] children_nodes = new long[children_k.length];
//		for(int k = 0; k<children_nodes.length; k++){
//			children_nodes[k] = this._network.get(children_k[k]);
//		}
//		
//		int id = this._inst.getId();
//		if(id<0)
//			id = -id;
//		
////		System.err.println(node);
//		
////		//TODO
////		int f = this._param.toFeature("x"+node, "y"+java.util.Arrays.toString(children_nodes));
//		int f = this._param.toFeature("x"+node, "y"+java.util.Arrays.toString(children_nodes)+":z"+id);
//		return new FeatureArray(new int[]{f}, this._param);
		
	}
	
}
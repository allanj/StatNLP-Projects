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
package com.statnlp.ie.linear.semi;

import java.util.Iterator;

import com.statnlp.commons.AttributedWord;
import com.statnlp.commons.types.TextSpan;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.hybridnetworks.GlobalNetworkParam;

public class IELinearSemiFeatureManager extends FeatureManager{
	
	private IELinearSemiInstance _inst;
	private TextSpan _span;
	private final FeatureArray _EMPTY_ = new FeatureArray(this._param);
	
	public IELinearSemiFeatureManager(GlobalNetworkParam param) {
		super(param);
	}
	
	public FeatureArray[][][][][][] _cache;
	
	public void enableCache(int num_insts, int num_tags){
		if(num_insts>1)
			System.err.println("cache enabled.");
		this._cache = new FeatureArray[num_insts][num_tags][FEATURE_TYPE.values().length][FEATURE_CAT.values().length][][];
	}
	
	public void disableCache(){
//		System.err.println("cache disabled.");
		this._cache = null;
	}

	private enum FEATURE_TYPE {IS_MENTION, IS_NOT_MENTION};
	private enum FEATURE_CAT {MARKOV, SEMI_MARKOV};
	
	private FeatureArray extract_semi_markov(FEATURE_TYPE ft, int bIndex, int eIndex, int tag_id){
		int fc = 1;
		if(this._cache != null){
			int inst_id = this._inst.getInstanceId();
			if(inst_id<0){
				inst_id = -inst_id;
			}
			if(this._cache[inst_id-1][tag_id-1][ft.ordinal()][fc]==null){
				this._cache[inst_id-1][tag_id-1][ft.ordinal()][fc] = new FeatureArray[this._span.length()][];
			}
			if(this._cache[inst_id-1][tag_id-1][ft.ordinal()][fc][bIndex]==null){
				this._cache[inst_id-1][tag_id-1][ft.ordinal()][fc][bIndex] = new FeatureArray[IELinearSemiConfig._MAX_MENTION_LEN];
			}
			
			if(this._cache[inst_id-1][tag_id-1][ft.ordinal()][fc][bIndex][eIndex-bIndex-1]!=null){
				FeatureArray fs = this._cache[inst_id-1][tag_id-1][ft.ordinal()][fc][bIndex][eIndex-bIndex-1];
				return fs;
			}
		}
		
		int[] fs = new int[1];
		String form = "";
		for(int index = bIndex; index<eIndex; index++){
			form += this._inst.getSpan().getWord(index)+" ";
		}
		form = form.trim();
		fs[0] = this._param.toFeature("COMPLETE_WORD:"+ft.name()+":"+tag_id, form);
//		
//		int[] fs = new int[0];
		FeatureArray fa = new FeatureArray(fs, this._param);
		
		if(this._cache!=null){
			int inst_id = this._inst.getInstanceId();
			if(inst_id<0)
				inst_id = -inst_id;
			this._cache[inst_id-1][tag_id-1][ft.ordinal()][fc][bIndex][eIndex-bIndex-1] = fa;
		}
		
		return fa;
	}

	private FeatureArray extract_markov(FEATURE_TYPE ft, int bIndex, int eIndex, int tag_id){
		int fc = 0;
		if(this._cache != null){
			int inst_id = this._inst.getInstanceId();
			if(inst_id<0){
				inst_id = -inst_id;
			}
			if(this._cache[inst_id-1][tag_id-1][ft.ordinal()][fc]==null){
				this._cache[inst_id-1][tag_id-1][ft.ordinal()][fc] = new FeatureArray[this._span.length()][];
			}
			if(this._cache[inst_id-1][tag_id-1][ft.ordinal()][fc][bIndex]==null){
				this._cache[inst_id-1][tag_id-1][ft.ordinal()][fc][bIndex] = new FeatureArray[IELinearSemiConfig._MAX_MENTION_LEN];
			}
			
			if(this._cache[inst_id-1][tag_id-1][ft.ordinal()][fc][bIndex][eIndex-bIndex-1]!=null){
				FeatureArray fa = this._cache[inst_id-1][tag_id-1][ft.ordinal()][fc][bIndex][eIndex-bIndex-1];
				return fa;
			}
		}
		
		FeatureArray fa;
		int[] fs = this.extract_one_word_helper(ft, eIndex-1, tag_id);
		if(eIndex-bIndex>1){
			fa = this.extract_markov(ft, bIndex, eIndex-1, tag_id);
			fa = new FeatureArray(fs, fa);
		} else {
			fa = new FeatureArray(fs, this._param);
		}
		
		if(this._cache!=null){
			int inst_id = this._inst.getInstanceId();
			if(inst_id<0)
				inst_id = -inst_id;
			this._cache[inst_id-1][tag_id-1][ft.ordinal()][fc][bIndex][eIndex-bIndex-1] = fa;
		}
		
		return fa;
	}
	
	private FeatureArray extract(FEATURE_TYPE ft, int bIndex, int eIndex, int tag_id, FeatureArray fa){
		FeatureArray fa1 = this.extract_markov(ft, bIndex, eIndex, tag_id);
		FeatureArray fa2 = this.extract_semi_markov(ft, bIndex, eIndex, tag_id);
		return new FeatureArray(new int[0], new FeatureArray[]{fa, fa1, fa2});
	}
	
	private int[] extract_one_word_helper(FEATURE_TYPE ft, int bIndex, int tag_id){
		TextSpan span = this._inst.getSpan();
		
		int num_atts = span.getWord(bIndex).getAttributes().size();
		int size = num_atts;
		int num_add_features = 10;
		size += num_add_features;
		int[] f = new int[size];
		int k = 0;
		AttributedWord word = span.getWord(bIndex);
		Iterator<String> atts = word.getAttributes().iterator();
		while(atts.hasNext()){
			String att = atts.next();
			String val = word.getAttribute(att).get(0);
//			if(!att.equals("curr")){
//				continue;
//			}
			f[k++] = this._param.toFeature(ft.name()+":"+tag_id+":"+att, val);
//			f[k++] = this._param.toFeature(tag_id+"", word.getForm()+"");
		}
		
		//additional features...
		String init_cap = "no";
		if(Character.isUpperCase(word.getName().charAt(0))){
			init_cap = "yes";
		}
		f[k++] = this._param.toFeature(ft.name()+":init_cap"+":"+tag_id, init_cap);
		
		String all_caps = "no";
		if(word.getName().equals(word.getName().toUpperCase())){
			all_caps = "yes";
		}
		f[k++] = this._param.toFeature(ft.name()+":all_caps"+":"+tag_id, all_caps);
		
		String contains_digits = "no";
		for(char c : word.getName().toCharArray()){
			if(Character.isDigit(c)){
				contains_digits = "yes";
				break;
			}
		}
		f[k++] = this._param.toFeature(ft.name()+":contains_digits"+":"+tag_id, contains_digits);
		
		String all_digits = "yes";
		for(char c : word.getName().toCharArray()){
			if(!Character.isDigit(c)){
				all_digits = "no";
				break;
			}
		}
		f[k++] = this._param.toFeature(ft.name()+":all_digits"+":"+tag_id, all_digits);
		
		String all_alphanumeric = "yes";
		for(char c : word.getName().toCharArray()){
			if(!Character.isDigit(c) && !Character.isLetter(c)){
				all_alphanumeric = "no";
				break;
			}
		}
		f[k++] = this._param.toFeature(ft.name()+":all_alphanumeric"+":"+tag_id, all_alphanumeric);
		
		String roman_number = "yes";
		for(char c : word.getName().toCharArray()){
			if(c=='I' || c=='V' || c=='X'){
			} else {
				roman_number = "no";
				break;
			}
		}
		f[k++] = this._param.toFeature(ft.name()+":roman_number"+":"+tag_id, roman_number);

		String contain_dots = "no";
		for(char c : word.getName().toCharArray()){
			if(c=='.'){
				contain_dots = "yes";
				break;
			}
		}
		f[k++] = this._param.toFeature(ft.name()+":contain_dots"+":"+tag_id, contain_dots);

		String contain_hyphen = "no";
		for(char c : word.getName().toCharArray()){
			if(c=='-'){
				contain_hyphen = "yes";
				break;
			}
		}
		f[k++] = this._param.toFeature(ft.name()+":contain_hyphen"+":"+tag_id, contain_hyphen);

		String single_char = "no";
		if(word.getName().length()==1){
			single_char = "yes";
		}
		f[k++] = this._param.toFeature(ft.name()+":single_char"+":"+tag_id, single_char);
		
		String punc_mark = "no";
		if(word.getName().length()==1){
			char ch = word.getName().charAt(0);
			if(ch=='?' || ch==',' || ch=='.' || ch=='!' || ch==':' || ch==';'){
				punc_mark = "yes";
			}
		}
		f[k++] = this._param.toFeature(ft.name()+":punc_mark"+":"+tag_id, punc_mark);
		
		if(k!=size){
			throw new RuntimeException("k="+k+";size="+size);
		}
		
		return f;
	}
//	
//	private int[] extract_helper(String prefix, int bIndex, int eIndex, int tag_id){
//		TextSpan span = this._inst.getSpan();
//		
//		int num_atts = span.getWord(bIndex).getAttributes().size();
//		int len = eIndex - bIndex;
//		int[] f = new int[len * num_atts + 1 + 2];
//		int k = 0;
////		System.err.println("len="+len);
//		String form = "";
//		for(int index = bIndex; index<eIndex; index++){
//			AttributedWord word = span.getWord(index);
//			Iterator<String> atts = word.getAttributes().iterator();
//			while(atts.hasNext()){
//				String att = atts.next();
////				System.err.println("size="+word.getAttributes().size()+"\t"+att);
//				String val = word.getAttribute(att).get(0);
////				if(att.equals("POS") || att.equals("curr") || att.equals("prev1_curr"))
//				{
////					System.err.println("GD");
//					f[k++] = this._param.toFeature(prefix+tag_id+":"+att, val);
//				}
//				if(index==bIndex){
//					if(att.equals("POS")){
//						f[k++] = this._param.toFeature(prefix+"start:"+tag_id+":"+att, val);
//					}
//				}
//				if(index==eIndex-1){
//					if(att.equals("POS")){
//						f[k++] = this._param.toFeature(prefix+"end:"+tag_id+":"+att, val);
//					}
//				}
//			}
//			form += " "+ word.getForm();
//		}
//		f[k++] = this._param.toFeature(prefix+tag_id+":form", form);
//		if(k!=f.length){
//			throw new RuntimeException("HHH."+k+"/"+f.length);
//		}
//		
//		return f;
//	}
	
	@Override
	public FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		
//		if(true){
//			int id = this._inst.getId();
//			if(id<0)
//				id = -id;
//			long node_parent = this._network.get(parent_k);
//			int[] node_parent_arr = NetworkIDMapper.toHybridNodeArray(node_parent);
//			int node_parent_type = node_parent_arr[4];
//			if(node_parent_type != IELinearSemiConfig.NODE_TYPE.TERMINATE.ordinal()
//					&& node_parent_type != IELinearSemiConfig.NODE_TYPE.MENTION_TAG.ordinal()
//					&& children_k.length==0){
//				throw new RuntimeException("???");
//			}
//			long[] node_children = new long[children_k.length];
//			for(int k = 0; k<node_children.length; k++){
//				node_children[k] = this._network.get(children_k[k]);
//			}
//			Arrays.sort(node_children);
//			int f = this._param.toFeature(node_parent+":"+Arrays.toString(node_children), ""+id);
////			System.err.println(node_parent+":"+node_children.length+":"+Arrays.toString(node_children));
//			FeatureArray fa = new FeatureArray(new int[]{f}, this._param);
//			return fa;
//		}
		
		long node_parent = ((IELinearSemiNetwork)network).get(parent_k);
		int[] node_parent_arr = NetworkIDMapper.toHybridNodeArray(node_parent);
		int node_parent_type = node_parent_arr[4];
		
		if(node_parent_type == IELinearSemiConfig.NODE_TYPE.ROOT.ordinal()){
			return _EMPTY_;
		}
		if(node_parent_type == IELinearSemiConfig.NODE_TYPE.TERMINATE.ordinal()){
			return _EMPTY_;
		}
		if(node_parent_type == IELinearSemiConfig.NODE_TYPE.AFTER_START.ordinal()){
			return _EMPTY_;
		}
		if(node_parent_type == IELinearSemiConfig.NODE_TYPE.EXACT_START.ordinal()){
			return _EMPTY_;
		}
		if(node_parent_type == IELinearSemiConfig.NODE_TYPE.EXACT_START_TAG.ordinal()){
			return _EMPTY_;
		}
		if(node_parent_type == IELinearSemiConfig.NODE_TYPE.MENTION_TAG_PARENT.ordinal()){
			FeatureArray fa = this._EMPTY_;
//			TextSpan span = this._inst.getSpan();
//			int bIndex = span.length() - node_parent_arr[0];
//			int eIndex = bIndex + node_parent_arr[1];
//			int tag_id = node_parent_arr[3]-1;
//			
//			if(children_k.length>1){
//				System.err.println("????");
//				System.exit(1);
//			}
//			
//			long[] node_children = new long[children_k.length];
//			for(int k = 0; k<node_children.length; k++){
//				node_children[k] = this._network.get(children_k[k]);
//				int[] node_child_arr = NetworkIDMapper.toHybridNodeArray(node_children[k]);
//				int node_child_type = node_child_arr[4];
//				if(node_child_type == IELinearSemiConfig.NODE_TYPE.TERMINATE.ordinal()){
//					fa = this.extract(FEATURE_TYPE.IS_NOT_MENTION, bIndex, eIndex, tag_id, fa);
//				}
//			}
			
			return fa;
		}
		
		if(node_parent_type == IELinearSemiConfig.NODE_TYPE.MENTION_TAG.ordinal()){
			FeatureArray fa = this._EMPTY_;
			TextSpan span = this._inst.getSpan();
			int bIndex = span.length() - node_parent_arr[0];
			int eIndex = bIndex + node_parent_arr[1];
			int tag_id = node_parent_arr[3]-1;
			fa = this.extract(FEATURE_TYPE.IS_MENTION, bIndex, eIndex, tag_id, fa);
			return fa;
		}
		
		throw new RuntimeException("This should not happen.");
		
	}
	
}
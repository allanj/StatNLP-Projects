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
package com.statnlp.sp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkIDMapper;

/**
 * @author wei_lu
 *
 */
public class SemTextFeatureManager_Discriminative_LONG extends FeatureManager{
	
	private static final long serialVersionUID = -1424454809943983877L;
	
	private HybridGrammar _g;
	private SemTextDataManager _dm;
	
//	private int _prevWord;
//	private int historySize;
	
	private enum FEATURE_TYPE {emission, transition, pattern, emission_long};
	
	public SemTextFeatureManager_Discriminative_LONG(GlobalNetworkParam param_g, HybridGrammar g, SemTextDataManager dm) {
		super(param_g);
		this._g = g;
		this._dm = dm;
//		historySize = NetworkConfig._SEMANTIC_PARSING_NGRAM-1;
//		if(historySize == 0){
//			_prevWord = 1;
//		} else {
//			_prevWord = historySize;
//		}
	}
	
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		
		SemTextNetwork stNetwork = (SemTextNetwork)network;
		SemTextInstance inst = (SemTextInstance)stNetwork.getInstance();
		Sentence sent = inst.getInput();
		
		
//		System.err.println("x:"+inst);
//		System.exit(1);
		
		long parent = stNetwork.getNode(parent_k);
		int[] ids_parent = NetworkIDMapper.toHybridNodeArray(parent);
		
		//if it is a leaf, but the pattern is not w.
		if(ids_parent[4]!=0 && children_k.length==0){
			throw new RuntimeException("xxx:"+Arrays.toString(ids_parent));
//			return FeatureArray.NEGATIVE_INFINITY;
		}
		
		if(ids_parent[0]==0){
			return FeatureArray.EMPTY;
		}
		
		//it is the root...
		if(ids_parent[1]==0 && ids_parent[2]==0 && ids_parent[3]==0 && ids_parent[4]==0){
			
			if(children_k.length==0){
				return FeatureArray.EMPTY;
			}
			
			long child = stNetwork.getNode(children_k[0]);
			int[] ids_child = NetworkIDMapper.toHybridNodeArray(child);
			SemanticUnit c_unit = this._dm.getAllUnits().get(ids_child[3]);
			
			int f = this._param_g.toFeature(FEATURE_TYPE.transition.name(), "ROOT", c_unit.toString());
			int[] fs = new int[]{f};
			return FeatureArray.createFeatureArray(fs);
			
		}
		
		else {
			
			HybridPattern pattern_parent = this._g.getPatternById(ids_parent[4]);

			int eIndex = ids_parent[0];
			int bIndex = ids_parent[0] - ids_parent[1];
			int cIndex = -1;
			
			SemanticUnit p_unit = this._dm.getAllUnits().get(ids_parent[3]);
			SemanticUnit c_units[] = new SemanticUnit[children_k.length];
			
			HybridPattern pattern_children[] = new HybridPattern[children_k.length];
			for(int k = 0; k<children_k.length; k++){
				long child = stNetwork.getNode(children_k[k]);
				int[] ids_child = NetworkIDMapper.toHybridNodeArray(child);
				pattern_children[k] = this._g.getPatternById(ids_child[4]);
				if(k==1){
					cIndex = ids_child[0]-ids_child[1];
				}
				c_units[k] = this._dm.getAllUnits().get(ids_child[3]);
			}
			
//			if(isTrain)
				return this.extract_helper(inst, p_unit, c_units, pattern_parent, pattern_children, sent, bIndex, cIndex, eIndex);
//			else 
//				return this.extract_helper_test(inst, p_unit, c_units, pattern_parent, pattern_children, sent, bIndex, cIndex, eIndex);
		}
		
	}
	
	public boolean isTrain = true;
	
	private HashMap<Integer, FeatureArray[][][]> _globalFeatures;
	private boolean considerGlobal = true;
	
	public synchronized void clearCache(){
		this._globalFeatures = new HashMap<Integer, FeatureArray[][][]>();
	}
	
	public synchronized void addCache(SemTextInstance inst, Sentence sent){
//		this._globalFeatures = new HashMap<Integer, FeatureArray[][][]>();
		int num_unit = 300;
		FeatureArray[][][] fa = new FeatureArray[num_unit][sent.length()][sent.length()+1];
		this._globalFeatures.put(inst.getInstanceId(), fa);
//		for(int id_unit = 0; id_unit<num_unit; id_unit++){
//			for(int index=0; index<=sent.length()-1; index++){
//				fa[id_unit][index] = FeatureArray.createFeatureArray[sent.length()-index];
////				System.err.println("xxx"+this._globalFeatures.get(inst.getInstanceId())[p_unit.getId()][index]+","+inst.getInstanceId()+","+p_unit.getId()+","+index);
//			}
//		}
	}
	
	private synchronized FeatureArray extract_helper(SemTextInstance inst, SemanticUnit p_unit, SemanticUnit[] c_units, 
			HybridPattern pattern_parent, HybridPattern[] pattern_children, 
			Sentence sent, int bIndex, int cIndex, int eIndex){

//		System.err.println(p_unit.getId());
		if(considerGlobal){
			if(_globalFeatures==null){
				clearCache();
//				_globalFeatures = new HashMap<Integer, FeatureArray[][][]>();
			}
			
			if(!this._globalFeatures.containsKey(inst.getInstanceId())){
				addCache(inst, sent);
			}
			
			for(int xIndex = 0; xIndex<=sent.length()-1; xIndex++){
				for(int yIndex = xIndex + 1; yIndex<=sent.length(); yIndex++){
					
				if(this._globalFeatures.get(inst.getInstanceId())[p_unit.getId()][xIndex][yIndex]==null){
//				
					if(yIndex-xIndex==1){
						int f = this._param_g.toFeature(FEATURE_TYPE.emission_long.name(), p_unit.toString(), sent.get(yIndex-1).getName());
						FeatureArray fa1 = FeatureArray.createFeatureArray(new int[]{f});
						FeatureArray[][][] fas = this._globalFeatures.get(inst.getInstanceId());
						fas[p_unit.getId()][xIndex][yIndex] = fa1;
//						if(p_unit.getId()==123){
//							System.err.println("Found you...");
//						}
					} else if(yIndex-xIndex==2){
						FeatureArray fa1 = this._globalFeatures.get(inst.getInstanceId())[p_unit.getId()][xIndex][yIndex-1];
						int f1 = this._param_g.toFeature(FEATURE_TYPE.emission_long.name(), p_unit.toString(), sent.get(yIndex-1).getName());
						int f2 = this._param_g.toFeature(FEATURE_TYPE.emission_long.name(), p_unit.toString(), sent.get(yIndex-2).getName()+"|||"+sent.get(yIndex-1).getName());
						FeatureArray[][][] fas = this._globalFeatures.get(inst.getInstanceId());
						fas[p_unit.getId()][xIndex][yIndex] = fa1;
						FeatureArray fa2 = FeatureArray.createFeatureArray(new int[]{f1, f2}, fa1);
						this._globalFeatures.get(inst.getInstanceId())[p_unit.getId()][xIndex][yIndex] = fa2;
					} else {
						FeatureArray fa1 = this._globalFeatures.get(inst.getInstanceId())[p_unit.getId()][xIndex][yIndex-1];
						int f1 = this._param_g.toFeature(FEATURE_TYPE.emission_long.name(), p_unit.toString(), sent.get(yIndex-1).getName());
						int f2 = this._param_g.toFeature(FEATURE_TYPE.emission_long.name(), p_unit.toString(), sent.get(yIndex-2).getName()+"|||"+sent.get(yIndex-1).getName());
						int f3 = this._param_g.toFeature(FEATURE_TYPE.emission_long.name(), p_unit.toString(), sent.get(yIndex-3).getName()+"|||"+sent.get(yIndex-2).getName()+"|||"+sent.get(yIndex-1).getName());
						FeatureArray fa2 = FeatureArray.createFeatureArray(new int[]{f1, f2, f3}, fa1);
						this._globalFeatures.get(inst.getInstanceId())[p_unit.getId()][xIndex][yIndex] = fa2;
					}
					
//					StringBuilder sb1 = new StringBuilder();
//					StringBuilder sb2 = new StringBuilder();
//					
//					int[] fs = new int[yIndex-xIndex];
//					for(int wIndex = xIndex; wIndex < yIndex; wIndex++){
//						fs[wIndex-xIndex] = this._param_g.toFeature(FEATURE_TYPE.emission_long.name(), p_unit.toString(), sent.get(wIndex).getName());
//						sb1.append(sent.get(wIndex).getName());
//					}
//					FeatureArray fa = FeatureArray.createFeatureArray(fs, null);
//					
//					for(int b = xIndex; b<yIndex; b++){
////						System.err.println(b+"\t"+sent.get(b).getName());
//						sb2.append(sent.get(b).getName());
//					}
//					
////					System.err.println(sb1.toString());
////					System.err.println(sb2.toString());
//					boolean match = sb1.toString().equals(sb2.toString());
//					if(!match){
//						System.err.println("Wrong....");
//						System.exit(1);
//					}
//					
//					this._globalFeatures.get(inst.getInstanceId())[p_unit.getId()][xIndex][yIndex-xIndex-1] = fa;
					
				}
				}
			
			}
			
//			int num_unit = 300;
//			FeatureArray[][][] fa = FeatureArray.createFeatureArray[num_unit][sent.length()][];
//			this._globalFeatures.put(inst.getInstanceId(), fa);
//			for(int id_unit = 0; id_unit<num_unit; id_unit++){
//				for(int index=0; index<=sent.length()-1; index++){
//					fa[id_unit][index] = FeatureArray.createFeatureArray[sent.length()-index];
////					System.err.println("xxx"+this._globalFeatures.get(inst.getInstanceId())[p_unit.getId()][index]+","+inst.getInstanceId()+","+p_unit.getId()+","+index);
//				}
//			}
				
		}
		
		if(pattern_parent.isw()){
			return FeatureArray.EMPTY;
		}
		
		else if(pattern_parent.isA() || pattern_parent.isB() || pattern_parent.isC()){
			
//			System.err.println(inst.getInstanceId()+":\t"+p_unit+"\t"+ bIndex+"\t"+eIndex);
			
			if(pattern_children.length!=1){
				throw new RuntimeException("The pattern_children has size "+pattern_children.length);
			}
			FeatureArray fa = FeatureArray.createFeatureArray(new int[0]);
			
			if(considerGlobal){
				
//				if(eIndex-bIndex==1){
//					int f = this._param_g.toFeature(FEATURE_TYPE.emission_long.name(), p_unit.toString(), sent.get(bIndex).getName());
//					FeatureArray fa1 = FeatureArray.createFeatureArray(new int[]{f}, null);
////					System.err.println(inst.getNetworkId()+"<<<");
////					System.err.println("xxx"+this._globalFeatures.get(inst.getInstanceId())[p_unit.getId()][bIndex]+","+bIndex+"\t"+this._globalFeatures.get(inst.getInstanceId())[p_unit.getId()][bIndex][0]);
////					this._globalFeatures.get(inst.getInstanceId())[p_unit.getId()][bIndex][0] = fa1;
//					FeatureArray[][][] fas = this._globalFeatures.get(inst.getInstanceId());
////					System.err.println("zz"+fas[p_unit.getId()][bIndex][0]);
////					fas[p_unit.getId()][bIndex][0] = FeatureArray.EMPTY;
////					System.err.println("zz"+fas[p_unit.getId()][bIndex]);
////					if(this._globalFeatures.get(inst.getInstanceId())[p_unit.getId()][bIndex]==null){
////						throw new RuntimeException("x!"+inst.getInstanceId()+","+p_unit.getId()+","+bIndex);
////					}
////					try{
////					System.err.println(p_unit.getId()+"\t"+bIndex);
//					fas[p_unit.getId()][bIndex][0] = fa1;
//					fa = fa1;
////					}catch(Exception e){
////						System.err.println("yy.."+sent.length()+"\t"+bIndex+"\t"+p_unit.getId()+"\t"+fas.length+"\t"+fas[p_unit.getId()].length);
////						throw new RuntimeException("xx.."+e.getMessage());
////					}
//				} else {
//					FeatureArray fa1 = this._globalFeatures.get(inst.getInstanceId())[p_unit.getId()][bIndex][eIndex-bIndex-2];
////					if(p_unit.arity()<2 && !p_unit.toString().equals("*n:River -> ({ traverse_2 ( *n:City ) })")){
////						if(fa1==null){
////							throw new RuntimeException("some thing wrong..."+bIndex+","+eIndex);
////						}
////					}
////					else if(p_unit.arity()==2){
////						if(fa1==null && eIndex-bIndex!=2){
////							throw new RuntimeException("some thing wrong..."+bIndex+","+eIndex);
////						}
////					}
//					int f = this._param_g.toFeature(FEATURE_TYPE.emission_long.name(), p_unit.toString(), sent.get(eIndex-1).getName());
//					FeatureArray fa2 = FeatureArray.createFeatureArray(new int[]{f}, fa1);
//					this._globalFeatures.get(inst.getInstanceId())[p_unit.getId()][bIndex][eIndex-bIndex-1] = fa2;
//					fa = fa2;
//				}
				
				fa = this._globalFeatures.get(inst.getInstanceId())[p_unit.getId()][bIndex][eIndex];
				
				if(fa==null){
					System.err.println("zzz");
					System.err.println(p_unit.getId());
					System.err.println(bIndex);
					System.err.println(eIndex);
					System.exit(1);
				}
				
//				return fa;
			}
			
//			System.exit(1);
			
			if(p_unit.isContextIndependent()){
				StringBuilder sb_phrase = new StringBuilder();
				for(int index = bIndex; index<eIndex; index++){
					sb_phrase.append(sent.get(index).getName());
					sb_phrase.append(' ');
				}
				String phrase = sb_phrase.toString().trim();
				ArrayList<String> phrases = this._dm.getPriorUnitToPhrases(p_unit);
				if(!phrases.contains(phrase)){
//					System.err.println("xx["+phrase+"]");
					return FeatureArray.NEGATIVE_INFINITY;
				} else {
//					System.err.println("YES["+phrase+"]");
					return FeatureArray.EMPTY;
				}
			}
			
			for(int historySize = 0; historySize<=NetworkConfig._SEMANTIC_PARSING_NGRAM-1; historySize++)
			{
				int prevWord;
				if(historySize == 0){
					prevWord = 1;
				} else {
					prevWord = historySize;
				}
				
				int[] fs = new int[1+1+prevWord];

				int t = 0;
				
				fs[t++] = this._param_g.toFeature(FEATURE_TYPE.pattern.name(), p_unit.toString(), pattern_children[0].toString());
				
				String output, input;
				
				ArrayList<String> wordsInWindow;
				
				wordsInWindow = new ArrayList<String>();
				for(int k = 0; k<historySize; k++){
					String word = this.getForm(pattern_children[0], k-historySize, sent, eIndex-(historySize-k));
					wordsInWindow.add(word);
				}
				
				//single last word.
				output = p_unit.toString();
				for(int k = 0; k<historySize; k++){
					output += wordsInWindow.get(k)+"|||";
				}
				input = "[END]";
				fs[t++] = this._param_g.toFeature(FEATURE_TYPE.emission.name(), output, input);

				wordsInWindow = new ArrayList<String>();
				for(int k = 0; k<historySize; k++){
					wordsInWindow.add("[BEGIN]");
				}
				for(int w = 0; w<prevWord; w++){
					//the first _prevWord words
					output = p_unit.toString();
					for(int k = 0; k<historySize; k++){
						output += wordsInWindow.get(k)+"|||";
					}
					input = this.getForm(pattern_children[0], w, sent, bIndex+w);
					fs[t++] = this._param_g.toFeature(FEATURE_TYPE.emission.name(), output, input);
					
					wordsInWindow.add(input);
					wordsInWindow.remove(0);
				}
				
				fa = FeatureArray.createFeatureArray(fs, fa);
			}
			
			return fa;
		}
		
		else if(pattern_parent.isX()){
			if(c_units[0].isContextIndependent()){
				return FeatureArray.EMPTY;
			}
			int f = this._param_g.toFeature(FEATURE_TYPE.transition.name(), p_unit.toString()+":0", c_units[0].toString().toString());
			int[] fs = new int[]{f};
			return FeatureArray.createFeatureArray(fs);
		}
		
		else if(pattern_parent.isY()){
			if(c_units[0].isContextIndependent()){
				return FeatureArray.EMPTY;
			}
			int f = this._param_g.toFeature(FEATURE_TYPE.transition.name(), p_unit.toString()+":1", c_units[0].toString().toString());
			int[] fs = new int[]{f};
			return FeatureArray.createFeatureArray(fs);
		}
		
		else if(pattern_children.length==1){
//			System.err.println(pattern_parent+"\t"+Arrays.toString(pattern_children));
			return FeatureArray.EMPTY;
		}
		
		else {
			
			FeatureArray fa = null;
			
			for(int historySize = 0; historySize<=NetworkConfig._SEMANTIC_PARSING_NGRAM-1; historySize++) 
			{
				int prevWord;
				if(historySize == 0){
					prevWord = 1;
				} else {
					prevWord = historySize;
				}
				
				int[] fs = new int[prevWord];
				String output, input;
				
				ArrayList<String> wordsInWindow = new ArrayList<String>();
				for(int k = 0; k<historySize; k++){
//					System.err.println(pattern_parent+"\t"+pattern_children[0]+"\t"+(k-_prevWord)+"\t"+(cIndex-(_prevWord-k))+"\t"+k);
					String word = this.getForm(pattern_children[0], k-historySize, sent, cIndex-(historySize-k));
					wordsInWindow.add(word);
				}
				
				for(int w = 0; w<prevWord; w++){
					//the first _prevWord words
					output = p_unit.toString();
					for(int k = 0; k<historySize; k++){
						output += wordsInWindow.get(k)+"|||";
					}
					
					input = this.getForm(pattern_children[1], w, sent, cIndex+w);
					fs[w] = this._param_g.toFeature(FEATURE_TYPE.emission.name(), output, input);
					
					wordsInWindow.add(input);
					wordsInWindow.remove(0);
				}
				fa = FeatureArray.createFeatureArray(fs, fa);
			}
			return fa;
			
		}
		
	}
	
	private synchronized FeatureArray extract_helper_test(SemTextInstance inst, SemanticUnit p_unit, SemanticUnit[] c_units, 
			HybridPattern pattern_parent, HybridPattern[] pattern_children, 
			Sentence sent, int bIndex, int cIndex, int eIndex){

//		System.err.println(p_unit.getId());
		if(considerGlobal){
			if(_globalFeatures==null){
				clearCache();
//				_globalFeatures = new HashMap<Integer, FeatureArray[][][]>();
			}
			if(!this._globalFeatures.containsKey(inst.getInstanceId())){
				addCache(inst, sent);
//				int num_unit = 300;
//				FeatureArray[][][] fa = FeatureArray.createFeatureArray[num_unit][sent.length()][];
//				this._globalFeatures.put(inst.getInstanceId(), fa);
//				for(int id_unit = 0; id_unit<num_unit; id_unit++){
//					for(int index=0; index<=sent.length()-1; index++){
//						fa[id_unit][index] = FeatureArray.createFeatureArray[sent.length()-index];
////						System.err.println("xxx"+this._globalFeatures.get(inst.getInstanceId())[p_unit.getId()][index]+","+inst.getInstanceId()+","+p_unit.getId()+","+index);
//					}
//				}
			}
		}
		
		if(pattern_parent.isw()){
			return FeatureArray.EMPTY;
		}
		
		else if(pattern_parent.isA() || pattern_parent.isB() || pattern_parent.isC()){
			
//			System.err.println(inst.getInstanceId()+":\t"+p_unit+"\t"+ bIndex+"\t"+eIndex);
			
			if(pattern_children.length!=1){
				throw new RuntimeException("The pattern_children has size "+pattern_children.length);
			}
			FeatureArray fa = null;
			
			if(considerGlobal){
				if(eIndex-bIndex==1){
					int f = this._param_g.toFeature(FEATURE_TYPE.emission_long.name(), p_unit.toString(), sent.get(bIndex).getName());
					FeatureArray fa1 = FeatureArray.createFeatureArray(new int[]{f});
//					System.err.println(inst.getNetworkId()+"<<<");
//					System.err.println("xxx"+this._globalFeatures.get(inst.getInstanceId())[p_unit.getId()][bIndex]+","+bIndex+"\t"+this._globalFeatures.get(inst.getInstanceId())[p_unit.getId()][bIndex][0]);
//					this._globalFeatures.get(inst.getInstanceId())[p_unit.getId()][bIndex][0] = fa1;
					FeatureArray[][][] fas = this._globalFeatures.get(inst.getInstanceId());
//					System.err.println("zz"+fas[p_unit.getId()][bIndex][0]);
//					fas[p_unit.getId()][bIndex][0] = FeatureArray.EMPTY;
//					System.err.println("zz"+fas[p_unit.getId()][bIndex]);
//					if(this._globalFeatures.get(inst.getInstanceId())[p_unit.getId()][bIndex]==null){
//						throw new RuntimeException("x!"+inst.getInstanceId()+","+p_unit.getId()+","+bIndex);
//					}
//					try{
//					System.err.println(p_unit.getId()+"\t"+bIndex);
					fas[p_unit.getId()][bIndex][0] = fa1;
					fa = fa1;
//					}catch(Exception e){
//						System.err.println("yy.."+sent.length()+"\t"+bIndex+"\t"+p_unit.getId()+"\t"+fas.length+"\t"+fas[p_unit.getId()].length);
//						throw new RuntimeException("xx.."+e.getMessage());
//					}
				} else {
					FeatureArray fa1 = this._globalFeatures.get(inst.getInstanceId())[p_unit.getId()][bIndex][eIndex-bIndex-2];
//					if(p_unit.arity()<2 && !p_unit.toString().equals("*n:River -> ({ traverse_2 ( *n:City ) })")){
//						if(fa1==null){
//							throw new RuntimeException("some thing wrong..."+bIndex+","+eIndex);
//						}
//					}
//					else if(p_unit.arity()==2){
//						if(fa1==null && eIndex-bIndex!=2){
//							throw new RuntimeException("some thing wrong..."+bIndex+","+eIndex);
//						}
//					}
					int f = this._param_g.toFeature(FEATURE_TYPE.emission_long.name(), p_unit.toString(), sent.get(eIndex-1).getName());
					FeatureArray fa2 = FeatureArray.createFeatureArray(new int[]{f}, fa1);
					this._globalFeatures.get(inst.getInstanceId())[p_unit.getId()][bIndex][eIndex-bIndex-1] = fa2;
					fa = fa2;
				}
			}

			
			if(p_unit.isContextIndependent()){
				StringBuilder sb_phrase = new StringBuilder();
				for(int index = bIndex; index<eIndex; index++){
					sb_phrase.append(sent.get(index).getName());
					sb_phrase.append(' ');
				}
				String phrase = sb_phrase.toString().trim();
				ArrayList<String> phrases = this._dm.getPriorUnitToPhrases(p_unit);
				if(!phrases.contains(phrase)){
//					System.err.println("xx["+phrase+"]");
					return FeatureArray.NEGATIVE_INFINITY;
				} else {
//					System.err.println("YES["+phrase+"]");
					return FeatureArray.EMPTY;
				}
			}
			
			for(int historySize = 0; historySize<=NetworkConfig._SEMANTIC_PARSING_NGRAM-1; historySize++)
			{
				int prevWord;
				if(historySize == 0){
					prevWord = 1;
				} else {
					prevWord = historySize;
				}
				
				int[] fs = new int[1+1+prevWord];

				int t = 0;
				
				fs[t++] = this._param_g.toFeature(FEATURE_TYPE.pattern.name(), p_unit.toString(), pattern_children[0].toString());
				
				String output, input;
				
				ArrayList<String> wordsInWindow;
				
				wordsInWindow = new ArrayList<String>();
				for(int k = 0; k<historySize; k++){
					String word = this.getForm(pattern_children[0], k-historySize, sent, eIndex-(historySize-k));
					wordsInWindow.add(word);
				}
				
				//single last word.
				output = p_unit.toString();
				for(int k = 0; k<historySize; k++){
					output += wordsInWindow.get(k)+"|||";
				}
				input = "[END]";
				fs[t++] = this._param_g.toFeature(FEATURE_TYPE.emission.name(), output, input);

				wordsInWindow = new ArrayList<String>();
				for(int k = 0; k<historySize; k++){
					wordsInWindow.add("[BEGIN]");
				}
				for(int w = 0; w<prevWord; w++){
					//the first _prevWord words
					output = p_unit.toString();
					for(int k = 0; k<historySize; k++){
						output += wordsInWindow.get(k)+"|||";
					}
					input = this.getForm(pattern_children[0], w, sent, bIndex+w);
					fs[t++] = this._param_g.toFeature(FEATURE_TYPE.emission.name(), output, input);
					
					wordsInWindow.add(input);
					wordsInWindow.remove(0);
				}
				
				fa = FeatureArray.createFeatureArray(fs, fa);
			}
			
			return fa;
		}
		
		else if(pattern_parent.isX()){
			if(c_units[0].isContextIndependent()){
				return FeatureArray.EMPTY;
			}
			int f = this._param_g.toFeature(FEATURE_TYPE.transition.name(), p_unit.toString()+":0", c_units[0].toString().toString());
			int[] fs = new int[]{f};
			return FeatureArray.createFeatureArray(fs);
		}
		
		else if(pattern_parent.isY()){
			if(c_units[0].isContextIndependent()){
				return FeatureArray.EMPTY;
			}
			int f = this._param_g.toFeature(FEATURE_TYPE.transition.name(), p_unit.toString()+":1", c_units[0].toString().toString());
			int[] fs = new int[]{f};
			return FeatureArray.createFeatureArray(fs);
		}
		
		else if(pattern_children.length==1){
//			System.err.println(pattern_parent+"\t"+Arrays.toString(pattern_children));
			return FeatureArray.EMPTY;
		}
		
		else {
			
			FeatureArray fa = null;
			
			for(int historySize = 0; historySize<=NetworkConfig._SEMANTIC_PARSING_NGRAM-1; historySize++) 
			{
				int prevWord;
				if(historySize == 0){
					prevWord = 1;
				} else {
					prevWord = historySize;
				}
				
				int[] fs = new int[prevWord];
				String output, input;
				
				ArrayList<String> wordsInWindow = new ArrayList<String>();
				for(int k = 0; k<historySize; k++){
//					System.err.println(pattern_parent+"\t"+pattern_children[0]+"\t"+(k-_prevWord)+"\t"+(cIndex-(_prevWord-k))+"\t"+k);
					String word = this.getForm(pattern_children[0], k-historySize, sent, cIndex-(historySize-k));
					wordsInWindow.add(word);
				}
				
				for(int w = 0; w<prevWord; w++){
					//the first _prevWord words
					output = p_unit.toString();
					for(int k = 0; k<historySize; k++){
						output += wordsInWindow.get(k)+"|||";
					}
					
					input = this.getForm(pattern_children[1], w, sent, cIndex+w);
					fs[w] = this._param_g.toFeature(FEATURE_TYPE.emission.name(), output, input);
					
					wordsInWindow.add(input);
					wordsInWindow.remove(0);
				}
				fa = FeatureArray.createFeatureArray(fs, fa);
			}
			return fa;
			
		}
		
	}
	
	private String getForm(HybridPattern p, int offset, Sentence sent, int index){
		char c = p.getFormat(offset);
		if(c=='w' || c=='W'){
			return sent.get(index).getName();
		}
		if(c!='X' && c!='Y'){
			throw new RuntimeException("Invalid:"+p+"\t"+c);
		}
		return "["+c+"]";
	}
	
}

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
package com.statnlp.sp.latent;

import java.util.Arrays;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.sp.HybridGrammar;
import com.statnlp.sp.HybridPattern;
import com.statnlp.sp.SemTextDataManager;
import com.statnlp.sp.SemTextInstance;
import com.statnlp.sp.SemTextNetwork;
import com.statnlp.sp.SemanticUnit;

/**
 * @author wei_lu
 *
 */
public class SemTextLatentFeatureManager extends FeatureManager{
	
	private static final long serialVersionUID = -1424454809943983877L;
	
	private HybridGrammar _g;
	private SemTextDataManager _dm;
	
	public static boolean _considerENDAsWord = false;
	public static boolean _considerXYAsWords = false;
	
	//note that if latent flag is set to true, it will by pass the explicit flag.
	public static boolean _constructLatentSemantics = false;
	public static boolean _constructExplicitSemantics = false;
	
	private enum FEATURE_TYPE {emission, transition, latent, pattern};
	
	public SemTextLatentFeatureManager(GlobalNetworkParam param_g, HybridGrammar g, SemTextDataManager dm) {
		super(param_g);
		this._g = g;
		this._dm = dm;
		
		System.err.println("ConstructLatentSemantics? "+_constructLatentSemantics);
		System.err.println("ConstructExplicitSemantics? "+_constructExplicitSemantics);
		System.err.println("Consider END? "+_considerENDAsWord);
		System.err.println("Consider X,Y? "+_considerXYAsWords);
	}
	
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		
		SemTextNetwork stNetwork = (SemTextNetwork)network;
		SemTextInstance inst = (SemTextInstance)stNetwork.getInstance();
		Sentence sent = inst.getInput();
		
		long parent = stNetwork.getNode(parent_k);
		int[] ids_parent = NetworkIDMapper.toHybridNodeArray(parent);
		
		//if it is a leaf node.
		if(ids_parent[0]==0){
			return FeatureArray.EMPTY;
		}
		
		//it is the root...
		if(ids_parent[1]==0 && ids_parent[2]==0 && ids_parent[3]==0 && ids_parent[4]==0 && ids_parent[5]==0 && ids_parent[6]==0){
			
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
		
		else if(ids_parent[5]==0){
			
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
			
			return this.extract_helper(p_unit, c_units, pattern_parent, pattern_children, sent, bIndex, cIndex, eIndex);
			
		}
		
		else if(ids_parent[5]==2){

			int eIndex = ids_parent[0];
			int bIndex = ids_parent[0] - ids_parent[1];
			SemanticUnit p_unit = this._dm.getAllUnits().get(ids_parent[3]);
			HybridPattern pattern_parent = this._g.getPatternById(ids_parent[4]);
			int cept = ids_parent[6];
			
			if(pattern_parent.isw()){
				if(eIndex-bIndex!=1){
					throw new RuntimeException("You have a w node with ["+bIndex+","+eIndex+") as span.");
				}
				if(_constructLatentSemantics){
					String input = sent.get(bIndex).getName();
					String output = "cept-"+cept;
					int[] fs = new int[1];
					fs[0] = this._param_g.toFeature("cept->word", output, input);
					return FeatureArray.createFeatureArray(fs);
				} else if(_constructExplicitSemantics){
					String input = sent.get(bIndex).getName();
					String output;
					if(cept==0){
						output = p_unit.toString();
					} else if(cept==1){
						output = p_unit.getName();
					} else if(cept==2){
						output = p_unit.getLHS().getName()+"|"+p_unit.getName();
					} else if(cept==3){
						output = p_unit.getName()+"|"+Arrays.toString(p_unit.getRHS());
					} else {
						output = p_unit.toString()+"|"+cept;
					}
					int[] fs = new int[1];
					fs[0] = this._param_g.toFeature("unit->word", output, input);
					return FeatureArray.createFeatureArray(fs);
				} else {
					String input = sent.get(bIndex).getName();
					String output = p_unit.toString()+"|"+cept;
					int[] fs = new int[1];
					fs[0] = this._param_g.toFeature("unit+cept->word", output, input);
					return FeatureArray.createFeatureArray(fs);
				}
			}
			
			else if(pattern_parent.isX()){
				if(_considerXYAsWords){
					
					String input = "[X]";
					if(_constructLatentSemantics){
						String output = "cept-"+cept;
						int[] fs = new int[1];
						fs[0] = this._param_g.toFeature("cept->word", output, input);
						return FeatureArray.createFeatureArray(fs);
					} else if(_constructExplicitSemantics){
						String output;
						if(cept==0){
							output = p_unit.toString();
						} else if(cept==1){
							output = p_unit.getName();
						} else if(cept==2){
							output = p_unit.getLHS().getName()+"|"+p_unit.getName();
						} else if(cept==3){
							output = p_unit.getName()+"|"+Arrays.toString(p_unit.getRHS());
						} else {
							output = p_unit.toString()+"|"+cept;
						}
						int[] fs = new int[1];
						fs[0] = this._param_g.toFeature("unit->word", output, input);
						return FeatureArray.createFeatureArray(fs);
					} else {
						String output = p_unit.toString()+"|"+cept;
						int[] fs = new int[1];
						fs[0] = this._param_g.toFeature("unit+cept->word", output, input);
						return FeatureArray.createFeatureArray(fs);
					}
					
				}
				return FeatureArray.EMPTY;
			}
			
			else if(pattern_parent.isY()){
				if(_considerXYAsWords){

					String input = "[Y]";
					if(_constructLatentSemantics){
						String output = "cept-"+cept;
						int[] fs = new int[1];
						fs[0] = this._param_g.toFeature("cept->word", output, input);
						return FeatureArray.createFeatureArray(fs);
					} else if(_constructExplicitSemantics){
						String output;
						if(cept==0){
							output = p_unit.toString();
						} else if(cept==1){
							output = p_unit.getName();
						} else if(cept==2){
							output = p_unit.getLHS().getName()+"|"+p_unit.getName();
						} else if(cept==3){
							output = p_unit.getName()+"|"+Arrays.toString(p_unit.getRHS());
						} else {
							output = p_unit.toString()+"|"+cept;
						}
						int[] fs = new int[1];
						fs[0] = this._param_g.toFeature("unit->word", output, input);
						return FeatureArray.createFeatureArray(fs);
					} else {
						String output = p_unit.toString()+"|"+cept;
						int[] fs = new int[1];
						fs[0] = this._param_g.toFeature("unit+cept->word", output, input);
						return FeatureArray.createFeatureArray(fs);
					}
					
				}
				return FeatureArray.EMPTY;
			}
			
			else if(pattern_parent.isA() || pattern_parent.isB() || pattern_parent.isC()){
				if(_considerENDAsWord){

					String input = "[END]";
					if(_constructLatentSemantics){
						String output = "cept-"+cept;
						int[] fs = new int[1];
						fs[0] = this._param_g.toFeature("cept->word", output, input);
						return FeatureArray.createFeatureArray(fs);
					} else if(_constructExplicitSemantics){
						String output;
						if(cept==0){
							output = p_unit.toString();
						} else if(cept==1){
							output = p_unit.getName();
						} else if(cept==2){
							output = p_unit.getLHS().getName()+"|"+p_unit.getName();
						} else if(cept==3){
							output = p_unit.getName()+"|"+Arrays.toString(p_unit.getRHS());
						} else {
							output = p_unit.toString()+"|"+cept;
						}
						int[] fs = new int[1];
						fs[0] = this._param_g.toFeature("unit->word", output, input);
						return FeatureArray.createFeatureArray(fs);
					} else {
						String output = p_unit.toString()+"|"+cept;
						int[] fs = new int[1];
						fs[0] = this._param_g.toFeature("unit+cept->word", output, input);
						return FeatureArray.createFeatureArray(fs);
					}
					
				}
				return FeatureArray.EMPTY;
			}
			
			else {
				throw new RuntimeException("pattern="+pattern_parent);
			}
		}
		
		else if(ids_parent[5]==4){
			
			SemanticUnit p_unit = this._dm.getAllUnits().get(ids_parent[3]);
			
			if(children_k.length!=1){
				throw new RuntimeException("size of children_k is "+children_k.length);
			}
			
			long child = stNetwork.getNode(children_k[0]);
			int[] ids_child = NetworkIDMapper.toHybridNodeArray(child);
			
			if(ids_child[5]==0){
				return FeatureArray.EMPTY;
			} else if(ids_child[5]==2){
				int[] fs = new int[1];
				int cept = ids_child[6];
				fs[0] = this._param_g.toFeature("unit->cept", p_unit.toString(), "cept-"+cept);
//				fs[0] = this._param_g.toFeature("unit->cept", p_unit.getName(), "cept-"+cept);
//				System.err.println("["+p_unit.getName()+"]");
				return FeatureArray.createFeatureArray(fs);
			} else {
				throw new RuntimeException("ids_child[5]="+ids_child[5]);
			}
			
		} 
		
		else {
			throw new RuntimeException("Not possible:"+ids_parent[5]);
		}
		
	}
	
	private FeatureArray extract_helper(SemanticUnit p_unit, SemanticUnit[] c_units, 
			HybridPattern pattern_parent, HybridPattern[] pattern_children, 
			Sentence sent, int bIndex, int cIndex, int eIndex){
		
		if(pattern_parent.isw()){
			//IN THIS CASE, THE EMISSION FEATURES ARE CREATED AT THE LATENT NODE.
			//THUS YOU SHOULD NOT CREATE SUCH FEATURES HERE AGAIN.
			return FeatureArray.EMPTY;
		}
		
		else if(pattern_parent.isA() || pattern_parent.isB() || pattern_parent.isC()){
			
			if(pattern_children.length!=1){
				throw new RuntimeException("The pattern_children has size "+pattern_children.length);
			}
			
			int[] fs = new int[1];
			fs[0] = this._param_g.toFeature(FEATURE_TYPE.pattern.name(), p_unit.toString(), pattern_children[0].toString());
			return FeatureArray.createFeatureArray(fs);
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
			return FeatureArray.EMPTY;
		}
		
		else {
			return FeatureArray.EMPTY;
		}
		
		
	}
	
}

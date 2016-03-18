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

import com.statnlp.commons.BitextInstance;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.HybridGrammar;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.translation.types.BitextNetwork;
import com.statnlp.translation.types.BitextNetworkConstructor;

public class BitextFeatureManager extends FeatureManager{

	public BitextFeatureManager(HybridGrammar g, GlobalNetworkParam param) {
		super(g, param);
	}
	
	@Override
	public void setNetwork(Network network) {
		this._network = (BitextNetwork)network;
	}
	
	@Override
	public FeatureArray extract(int parent_k, int[] children_k) {
		
		int[] ids_parent = NetworkIDMapper.toHybridNodeArray(((BitextNetwork)this._network).get(parent_k));
		int srcBIndex = ids_parent[1];
		int srcEIndex = ids_parent[0]+ids_parent[1];
		int tgtBIndex = ids_parent[3];
		int tgtEIndex = ids_parent[2]+ids_parent[3];
		int type = ids_parent[4];
		
		String superPrefix = "";//"UNIGRAM:";
		String NULLSTRING = "<NULL>";
		
		FeatureArray fa = new FeatureArray(this._param);
		
		if(children_k.length==0){
			
			if(type !=  BitextNetworkConstructor.TYPE.I00.ordinal()){
				throw new RuntimeException("mm 70: "+type);
			}
			
			if(srcEIndex!=srcBIndex+1){
				throw new RuntimeException("mm 90: "+srcBIndex+","+srcEIndex);
			}
			
			if(tgtEIndex!=tgtBIndex+1){
				
				if(type!=BitextNetworkConstructor.TYPE.I00.ordinal()){
					throw new RuntimeException("mm 80: "+tgtBIndex+","+tgtEIndex);
				}
				
				BitextInstance inst = (BitextInstance)this.getNetwork().getInstance();
				int srcLen = inst.getSrc().getWords().length;
				int tgtLen = inst.getTgt().getWords().length;
				
				if(srcBIndex==srcLen-1 && srcEIndex==srcLen && tgtBIndex==tgtLen-1 && tgtEIndex==tgtLen){
					throw new RuntimeException("mm 99: "+srcBIndex+","+srcEIndex);
				}
				
				String src = inst.getSrc().getWords()[srcBIndex].getName();
				int[] fs = new int[tgtEIndex-tgtBIndex];
				for(int index = tgtBIndex; index<tgtEIndex; index++){
					String tgt = inst.getTgt().getWords()[index].getName();
					fs[index-tgtBIndex] = this._param.toFeature(superPrefix+src, tgt);
				}
				
				fa = new FeatureArray(fs, fa);
				return fa;
				
			}
			
			if(type ==  BitextNetworkConstructor.TYPE.I00.ordinal()){
				
				if(srcBIndex==0 && tgtBIndex==0 && srcEIndex==1 && tgtEIndex==1){
					return fa;
				}
				
				BitextInstance inst = (BitextInstance)this.getNetwork().getInstance();
				int srcLen = inst.getSrc().getWords().length;
				int tgtLen = inst.getTgt().getWords().length;
				
				if(srcBIndex==srcLen-1 && srcEIndex==srcLen && tgtBIndex==tgtLen-1 && tgtEIndex==tgtLen){
					return fa;
				}
				
				String src = inst.getSrc().getWords()[srcBIndex].getName();
				String tgt = inst.getTgt().getWords()[tgtBIndex].getName();
				int f = this._param.toFeature(superPrefix+src, tgt);
				
				fa = new FeatureArray(new int[]{f}, fa);
				return fa;
			} else {
				throw new RuntimeException("how come?");
			}
			
		}
		
		else if(children_k.length==1){
			int child_k_0 = children_k[0];
			
			int[] ids_child_0 = NetworkIDMapper.toHybridNodeArray(((BitextNetwork)this._network).get(child_k_0));
			
			int srcBindex_child_0 = ids_child_0[1];
			int srcEindex_child_0 = ids_child_0[0]+ids_child_0[1];
			int tgtBindex_child_0 = ids_child_0[3];
			int tgtEindex_child_0 = ids_child_0[2]+ids_child_0[3];
			int type_child_0 = ids_child_0[4];
			
//			if(type == BitextNetworkConstructor.TYPE.N11.ordinal() 
//					&& type_child_0 == BitextNetworkConstructor.TYPE.N10.ordinal()
//					&& srcEindex_child_0==srcEIndex-1){
//				BitextInstance inst = (BitextInstance)this.getNetwork().getInstance();
//				String src = inst.getSrc().getWords()[srcEIndex-1].getForm();
//				String tgt = "<NULL>";
//				int f = this._param.toFeature("UNIGRAM:"+src, tgt);
//				fa = new FeatureArray(new int[]{f}, fa);
//				return fa;
//			}
//			
//			if(type == BitextNetworkConstructor.TYPE.I11.ordinal() 
//					&& type_child_0 == BitextNetworkConstructor.TYPE.I10.ordinal()
//					&& srcEindex_child_0==srcEIndex-1){
//				BitextInstance inst = (BitextInstance)this.getNetwork().getInstance();
//				String src = inst.getSrc().getWords()[srcEIndex-1].getForm();
//				String tgt = "<NULL>";
//				int f = this._param.toFeature("UNIGRAM:"+src, tgt);
//				fa = new FeatureArray(new int[]{f}, fa);
//				return fa;
//			}
			
			if(type == BitextNetworkConstructor.TYPE.N10.ordinal() ){
				if(type_child_0 == BitextNetworkConstructor.TYPE.N10.ordinal()){
					if(tgtBindex_child_0==tgtBIndex+1){
						BitextInstance inst = (BitextInstance)this.getNetwork().getInstance();
						String src = NULLSTRING;
						String tgt =  inst.getTgt().getWords()[tgtBIndex].getName();
						int f = this._param.toFeature(superPrefix+src, tgt);
						fa = new FeatureArray(new int[]{f}, fa);
						return fa;
					} else{
						throw new RuntimeException("mm 4: "+tgtBindex_child_0+","+tgtBIndex);
					}
				} else if(type_child_0 != BitextNetworkConstructor.TYPE.N00.ordinal()){
					throw new RuntimeException("mm 9: "+type_child_0 +"\t"+BitextNetworkConstructor.TYPE.N00.ordinal());
				}
			}
			
			if(type == BitextNetworkConstructor.TYPE.I10.ordinal()){
				if(type_child_0 == BitextNetworkConstructor.TYPE.I10.ordinal()){
					if(tgtEindex_child_0==tgtEIndex-1){
						BitextInstance inst = (BitextInstance)this.getNetwork().getInstance();
						String src = NULLSTRING;
						String tgt = inst.getTgt().getWords()[tgtEIndex-1].getName();
						int f = this._param.toFeature(superPrefix+src, tgt);
						fa = new FeatureArray(new int[]{f}, fa);
						return fa;
					} else{
						throw new RuntimeException("mm 8: "+tgtEindex_child_0+","+tgtEIndex);
					}
				} else if(type_child_0 != BitextNetworkConstructor.TYPE.I00.ordinal()){
					throw new RuntimeException("mm 5: "+type_child_0+"\t"+BitextNetworkConstructor.TYPE.I00.ordinal());
				}
			}
			
		}
		
		else if(children_k.length == 2){
			
			/*
			int child_k_0 = children_k[0];
			int child_k_1 = children_k[1];
			
			int[] ids_child_0 = NetworkIDMapper.toHybridNodeArray(((BitextNetwork)this._network).get(child_k_0));
			int[] ids_child_1 = NetworkIDMapper.toHybridNodeArray(((BitextNetwork)this._network).get(child_k_1));
			
			int type_child_0 = ids_child_0[4];
			int type_child_1 = ids_child_1[4];
			
			if(type == BitextNetworkConstructor.TYPE.I00.ordinal()){
				if(type_child_0 == BitextNetworkConstructor.TYPE.N11.ordinal()
						&& type_child_1 == BitextNetworkConstructor.TYPE.IBAR.ordinal()){
					
				}else {
					throw new RuntimeException("mm x5: "+BitextNetworkConstructor.toType(type_child_0)+"\t"+BitextNetworkConstructor.toType(type_child_1));
				}
			}
			
			else if(type == BitextNetworkConstructor.TYPE.IBAR.ordinal()){
				if(type_child_0 == BitextNetworkConstructor.TYPE.N11.ordinal()
						&& type_child_1 == BitextNetworkConstructor.TYPE.IBAR.ordinal()){
					
				}else {
					throw new RuntimeException("mm x6: "+BitextNetworkConstructor.toType(type_child_0)+"\t"+BitextNetworkConstructor.toType(type_child_1));
				}
			}
			
			else if(type == BitextNetworkConstructor.TYPE.N00.ordinal()){
				if(type_child_0 == BitextNetworkConstructor.TYPE.I11.ordinal()
						&& type_child_1 == BitextNetworkConstructor.TYPE.NBAR.ordinal()){
					
				}else {
					throw new RuntimeException("mm x7: "+BitextNetworkConstructor.toType(type_child_0)+"\t"+BitextNetworkConstructor.toType(type_child_1));
				}
			}
			
			else if(type == BitextNetworkConstructor.TYPE.NBAR.ordinal()){
				if(type_child_0 == BitextNetworkConstructor.TYPE.I11.ordinal()
						&& type_child_1 == BitextNetworkConstructor.TYPE.NBAR.ordinal()){
					
				}else {
					throw new RuntimeException("mm x8: "+BitextNetworkConstructor.toType(type_child_0)+"\t"+BitextNetworkConstructor.toType(type_child_1));
				}
			}
			
			else {
				throw new RuntimeException("mm 777: "+type);
			}
			
			*/
			
			return fa;
		}
		
		else{
			throw new RuntimeException("children_k.length="+children_k.length);
		}
		
		return fa;
	}

}

package com.statnlp.mt.commons;

import java.util.Arrays;

import com.statnlp.commons.types.InputToken;
import com.statnlp.commons.types.OutputToken;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.mt.commons.BitextNetworkCompiler.nodeType;

public class BitextFeatureManager extends FeatureManager{
	
	private static final long serialVersionUID = -8702911622042935389L;
	
	public BitextFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}
	
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		
		BitextNetwork bn = (BitextNetwork)network;
		BitextInstance inst = (BitextInstance)bn.getInstance();
		InputToken[] srcWords = inst.getInput();
		OutputToken[] tgtWords = inst.getOutput();
		
		long node_parent = bn.getNode(parent_k);
		int[] ids_parent = NetworkIDMapper.toHybridNodeArray(node_parent);
		if(ids_parent[4] == nodeType.ROOT.ordinal()){
			return FeatureArray.EMPTY;
		}
		else if(ids_parent[4] == nodeType.TGT.ordinal()){
			OutputToken tgt = tgtWords[ids_parent[2]];
			int fs [] = new int[children_k.length];
			int k = 0;
//			System.err.println("#children="+children_k.length);
			for(int child_k : children_k){
				long node_child = bn.getNode(child_k);
				int[] ids_child = NetworkIDMapper.toHybridNodeArray(node_child);
				if(ids_child[4] == nodeType.SRC.ordinal()){
					String srcName;
					if(ids_child[2]!=srcWords.length){
						InputToken src = srcWords[ids_child[2]];
						srcName = src.getName();
					} else {
						srcName = "#NULL#";
					}
					fs[k++] = this._param_g.toFeature("src-tgt", srcName, tgt.getName());
				} else if(ids_child[4] == nodeType.HIDDEN.ordinal()){
					String cept = "cept-i="+ids_child[2];
					fs[k++] = this._param_g.toFeature("cept-tgt", cept, tgt.getName());
				} else if(ids_child[4] == nodeType.EXPLICIT.ordinal()){
//					InputToken src = srcWords[ids_child[2]];
					String srcName;
					if(ids_child[2]!=srcWords.length){
						InputToken src = srcWords[ids_child[2]];
						srcName = src.getName();
					} else {
						srcName = "#NULL#";
					}
					String cept = "cept-w="+srcName;
					fs[k++] = this._param_g.toFeature("cept-tgt", cept, tgt.getName());
				} else {
					throw new RuntimeException("(0) The node type is "+ids_child[4]+"\t"+Arrays.toString(ids_child)+"\t"+Arrays.toString(ids_parent));
				}
			}
			return new FeatureArray(fs);
		}
		else if(ids_parent[4] == nodeType.EXPLICIT.ordinal()){
			String explicitName;
			if(ids_parent[2]!=srcWords.length){
				InputToken src = srcWords[ids_parent[2]];
				explicitName = src.getName();
			} else {
				explicitName = "#NULL#";
			}
			String cept = "cept-w="+explicitName;
			int fs [] = new int[children_k.length];
			int k = 0;
//			System.err.println("#children="+children_k.length);
			for(int child_k : children_k){
				long node_child = bn.getNode(child_k);
				int[] ids_child = NetworkIDMapper.toHybridNodeArray(node_child);
				if(ids_child[4] == nodeType.SRC.ordinal()){
					String srcName;
					if(ids_child[2]!=srcWords.length){
						InputToken src = srcWords[ids_child[2]];
						srcName = src.getName();
					} else {
						srcName = "#NULL#";
					}
					fs[k++] = this._param_g.toFeature("src-cept", srcName, cept);
				} else {
					throw new RuntimeException("(1) The node type is "+ids_child[4]);
				}
			}
			return new FeatureArray(fs);
		}
		else if(ids_parent[4] == nodeType.HIDDEN.ordinal()){
			String cept = "cept-i="+ids_parent[2];
			int fs [] = new int[children_k.length];
			int k = 0;
//			System.err.println("#children="+children_k.length);
			for(int child_k : children_k){
				long node_child = bn.getNode(child_k);
				int[] ids_child = NetworkIDMapper.toHybridNodeArray(node_child);
				if(ids_child[4] == nodeType.SRC.ordinal()){
					String srcName;
					if(ids_child[2]!=srcWords.length){
						InputToken src = srcWords[ids_child[2]];
						srcName = src.getName();
					} else {
						srcName = "#NULL#";
					}
					fs[k++] = this._param_g.toFeature("src-cept", srcName, cept);
				} else {
					throw new RuntimeException("(1) The node type is "+ids_child[4]);
				}
			}
			return new FeatureArray(fs);
		}
		else if(ids_parent[4] == nodeType.SRC.ordinal()){
			return FeatureArray.EMPTY;
		}
		else if(ids_parent[4] == nodeType.LEAF.ordinal()){
			return FeatureArray.EMPTY;
		}
		else {
			throw new RuntimeException("(2) The node type is "+ids_parent[4]);
		}
		
	}
	
}

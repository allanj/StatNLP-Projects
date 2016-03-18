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

import java.util.ArrayList;
import java.util.Arrays;

import com.statnlp.commons.BitextInstance;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class InstanceSpecificNetworkConstructor {
	
	private int _maxSrcLength;
	private int _maxTgtLength;
	
	private int _numEdges=0;
	private int _numNodes=0;
	private int _nextId = 0;
	
	private enum TYPE {N10, I10, IBAR, N11, I00, I11 ,NBAR, N00};
	
	private long[] _nodes;
	private int[][][][][] _node2id;
	private int[][][] _children_arr;
	
	public static void main(String args[]){
		//TODO
	}
	
	public InstanceSpecificNetworkConstructor(int maxSrcLength, int maxTgtLength, BitextInstance instance){
		this._maxSrcLength = maxSrcLength;
		this._maxTgtLength = maxTgtLength;
		this.init();
	}
	
	private void init(){
		int size = (1+this._maxSrcLength)*this._maxSrcLength/2*(1+this._maxTgtLength)*this._maxTgtLength/2*8;
		System.err.println(size);
		this._nodes = new long[size];
		this._children_arr = new int[size][][];
		this._node2id = new int[this._maxSrcLength][][][][];
		for(int i = 0; i<this._node2id.length; i++){
			this._node2id[i] = new int[this._maxSrcLength-i][][][];
			for(int j = 0; j<this._node2id[i].length; j++){
				this._node2id[i][j] = new int[this._maxTgtLength][][];
				for(int k = 0; k<this._node2id[i][j].length; k++){
					this._node2id[i][j][k] = new int[this._maxTgtLength-k][8];
					for(int l = 0; l<this._node2id[i][j][k].length; l++){
						Arrays.fill(this._node2id[i][j][k][l], -1);
					}
				}
			}
		}
	}
	
	private void setEdge(int parent, ArrayList<int[]> childrenList){
		int[][] childrenList_arr = new int[childrenList.size()][];
		for(int k = 0; k<childrenList.size(); k++){
			childrenList_arr[k] = childrenList.get(k);
		}
		this._children_arr[parent] = childrenList_arr;
		this._numEdges+=childrenList.size();
		childrenList = null;
	}
	
	private boolean contains(long node){
		boolean r = this.getNodeId(node) != -1;
		return r;
	}
	
	public long getNode(int id){
		return this._nodes[id];
	}
	
	private int getNodeId(long node){
		int[] ids = NetworkIDMapper.toHybridNodeArray(node);
		int srcLen = ids[0];
		int srcBIndex = ids[1];
		int tgtLen = ids[2];
		int tgtBIndex = ids[3];
		int type = ids[4];
		return this._node2id[srcLen-1][srcBIndex][tgtLen-1][tgtBIndex][type];
	}

	private void setNodeId(long node, int id){
		int[] ids = NetworkIDMapper.toHybridNodeArray(node);
		int srcLen = ids[0];
		int srcBIndex = ids[1];
		int tgtLen = ids[2];
		int tgtBIndex = ids[3];
		int type = ids[4];
		this._node2id[srcLen-1][srcBIndex][tgtLen-1][tgtBIndex][type] = id;
	}
	
	private int addNode(long node){
		if(this.contains(node)){
			return this.getNodeId(node);
		}
		this._nodes[this._nextId] = node;
		this.setNodeId(node, this._nextId++);
		this._numNodes++;
		return this._nextId-1;
	}
	
	public void build_topdown_exact(){
		this.build(0, this._maxSrcLength, 0, this._maxTgtLength, TYPE.N00);
	}
	
	public int get(int srcLen, int tgtLen){
		long node = this.toNetworkID(0, srcLen, 0, tgtLen, TYPE.N00.ordinal());
		if(this.contains(node)){
			System.err.println("contains:"+srcLen+","+tgtLen);
		} else {
			System.err.println("no contains:"+srcLen+","+tgtLen);
		}
		return this.getNodeId(node);
	}
	
	public int build(int srcBIndex, int srcEIndex, int tgtBIndex, int tgtEIndex, TYPE type){
		
		long node = this.toNetworkID(srcBIndex, srcEIndex, tgtBIndex, tgtEIndex, type.ordinal());
		int result = -1;
		
		if(this.contains(node)){
			return this.getNodeId(node);
		}
		
		boolean hasChild = false;
		
		int srcLen = srcEIndex - srcBIndex;
		int tgtLen = tgtEIndex - tgtBIndex;
		
		ArrayList<int[]> childrenList = new ArrayList<int[]>();
		
		if(srcLen == 1 && tgtLen == 1 && type == TYPE.I00){
			result = this.addNode(node);
		}
		
		else {
			
			if(type == TYPE.N00){
				for(int srcCIndex = srcBIndex+1; srcCIndex < srcEIndex; srcCIndex++){
					for(int tgtCIndex = tgtBIndex+1; tgtCIndex < tgtEIndex; tgtCIndex++){
						int child1 = this.build(srcBIndex, srcCIndex, tgtBIndex, tgtCIndex, TYPE.I11);
						int child2 = this.build(srcCIndex, srcEIndex, tgtCIndex, tgtEIndex, TYPE.NBAR);
						if(child1!=-1 && child2!=-1){
							hasChild = true;
							childrenList.add(new int[]{child1, child2});
						}
					}
				}
			}

			if(type == TYPE.I00){
				for(int srcCIndex = srcBIndex+1; srcCIndex < srcEIndex; srcCIndex++){
					for(int tgtCIndex = tgtBIndex+1; tgtCIndex < tgtEIndex; tgtCIndex++){
						int child1 = this.build(srcBIndex, srcCIndex, tgtCIndex, tgtEIndex, TYPE.N11);
						int child2 = this.build(srcCIndex, srcEIndex, tgtBIndex, tgtCIndex, TYPE.IBAR);
						if(child1!=-1 && child2!=-1){
							hasChild = true;
							childrenList.add(new int[]{child1, child2});
						}
					}
				}
			}

			if(type == TYPE.NBAR){
				for(int srcCIndex = srcBIndex+1; srcCIndex < srcEIndex; srcCIndex++){
					for(int tgtCIndex = tgtBIndex+1; tgtCIndex < tgtEIndex; tgtCIndex++){
						int child1 = this.build(srcBIndex, srcCIndex, tgtBIndex, tgtCIndex, TYPE.I11);
						int child2 = this.build(srcCIndex, srcEIndex, tgtCIndex, tgtEIndex, TYPE.NBAR);
						if(child1!=-1 && child2!=-1){
							hasChild = true;
							childrenList.add(new int[]{child1, child2});
						}
					}
				}
				int child = this.build(srcBIndex, srcEIndex, tgtBIndex, tgtEIndex, TYPE.I00);
				if(child!=-1){
					hasChild = true;
					childrenList.add(new int[]{child});
				}
			}

			if(type == TYPE.IBAR){
				for(int srcCIndex = srcBIndex+1; srcCIndex < srcEIndex; srcCIndex++){
					for(int tgtCIndex = tgtBIndex+1; tgtCIndex < tgtEIndex; tgtCIndex++){
						int child1 = this.build(srcBIndex, srcCIndex, tgtCIndex, tgtEIndex, TYPE.N11);
						int child2 = this.build(srcCIndex, srcEIndex, tgtBIndex, tgtCIndex, TYPE.IBAR);
						if(child1!=-1 && child2!=-1){
							hasChild = true;
							childrenList.add(new int[]{child1, child2});
						}
					}
				}
				int child = this.build(srcBIndex, srcEIndex, tgtBIndex, tgtEIndex, TYPE.N00);
				if(child!=-1){
					hasChild = true;
					childrenList.add(new int[]{child});
				}
			}
			
			if(type == TYPE.N10){
				if(tgtBIndex+1<tgtEIndex){
					int child = this.build(srcBIndex, srcEIndex, tgtBIndex+1, tgtEIndex, TYPE.N10);
					if(child!=-1){
						hasChild = true;
						childrenList.add(new int[]{child});
					}
				}
				int child = this.build(srcBIndex, srcEIndex, tgtBIndex, tgtEIndex, TYPE.N00);
				if(child!=-1){
					hasChild = true;
					childrenList.add(new int[]{child});
				}
			}
			
			if(type == TYPE.N11){
				if(srcBIndex<srcEIndex-1){
					int child = this.build(srcBIndex, srcEIndex-1, tgtBIndex, tgtEIndex, TYPE.N11);
					if(child!=-1){
						hasChild = true;
						childrenList.add(new int[]{child});
					}
				}
				int child = this.build(srcBIndex, srcEIndex, tgtBIndex, tgtEIndex, TYPE.N10);
				if(child!=-1){
					hasChild = true;
					childrenList.add(new int[]{child});
				}
			}
			
			if(type == TYPE.I10){
				if(tgtBIndex<tgtEIndex-1){
					int child = this.build(srcBIndex, srcEIndex, tgtBIndex, tgtEIndex-1, TYPE.I10);
					if(child!=-1){
						hasChild = true;
						childrenList.add(new int[]{child});
					}
				}
				int child = this.build(srcBIndex, srcEIndex, tgtBIndex, tgtEIndex, TYPE.I00);
				if(child!=-1){
					hasChild = true;
					childrenList.add(new int[]{child});
				}
			}
			
			if(type == TYPE.I11){
				if(srcBIndex<srcEIndex-1){
					int child = this.build(srcBIndex, srcEIndex-1, tgtBIndex, tgtEIndex, TYPE.I11);
					if(child!=-1){
						hasChild = true;
						childrenList.add(new int[]{child});
					}
				}
				int child = this.build(srcBIndex, srcEIndex, tgtBIndex, tgtEIndex, TYPE.I10);
				if(child!=-1){
					hasChild = true;
					childrenList.add(new int[]{child});
				}
			}
		}
		
		if(hasChild){
			result = this.addNode(node);
			this.setEdge(result, childrenList);
		}
		
		return result;
	}

	public long toNetworkID(int srcBIndex, int srcEIndex, int tgtBIndex, int tgtEIndex, int type){
		int srcLength = srcEIndex - srcBIndex;
		int tgtLength = tgtEIndex - tgtBIndex;
		return NetworkIDMapper.toHybridNodeID(new int[]{srcLength, srcBIndex, tgtLength, tgtBIndex, type});
	}
	
	
}

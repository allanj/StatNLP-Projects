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
package com.statnlp.translation.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import com.statnlp.hybridnetworks.NetworkIDMapper;

public class FasterBitextNetworkConstructor {
	
	private int _maxSrcLength;
	private int _maxTgtLength;
	
	private int _numEdges=0;
	private int _numNodes=0;
	private int _nextId = 0;
	
	public enum TYPE {N10, I10, IBAR, N11, I00, I11 ,NBAR, N00};
	
	private long[] _nodes;
	private int[][][][][] _node2id;
	private int[][][] _children_arr;
	
	private double[][] _membership;
	
	private double _avgNumNodes = 0;
	private double _avgNumEdges = 0;
	
	public static String toType(int type){
		TYPE[] types = TYPE.values();
		for(TYPE t : types){
			if(t.ordinal() == type){
				return t.name();
			}
		}
		return "NOT FOUND";
	}
	
	public long[] getNodes(){
//		if(_cache_nodes[this._maxSrcLength-1][this._maxTgtLength-1]!=null){
//			return _cache_nodes[this._maxSrcLength-1][this._maxTgtLength-1];
//		}
		long[] nodes = new long[this._numNodes];
//		_cache_nodes[this._maxSrcLength-1][this._maxTgtLength-1] = new long[this._numNodes];
		System.arraycopy(this._nodes, 0, nodes, 0, this._numNodes);
		return nodes;
//		return _cache_nodes[this._maxSrcLength-1][this._maxTgtLength-1];
	}
	
	public int[][][] getChildren(){
//		if(_cache_children[this._maxSrcLength-1][this._maxTgtLength-1]!=null){
//			return _cache_children[this._maxSrcLength-1][this._maxTgtLength-1];
//		}
		int[][][] children = new int[this._numNodes][][];
//		_cache_children[this._maxSrcLength-1][this._maxTgtLength-1] = new int[this._numNodes][][];
		System.arraycopy(this._children_arr, 0, children, 0, this._numNodes);
//		return _cache_children[this._maxSrcLength-1][this._maxTgtLength-1];
		return children;
	}
	
	public static void main(String args[]){
		
		int maxLen = 50;
		long[][][] _allNodes = new long[maxLen][maxLen][];
		int[][][][][][][] _allNode2id = new int[maxLen][maxLen][][][][][];
		
		long bTime, eTime;
		bTime = System.currentTimeMillis();
		int [] curr = new int[]{25, 25};
		
		while(curr!=null){
			int srcLen = curr[0];
			int tgtLen = curr[1];
			FasterBitextNetworkConstructor c = new FasterBitextNetworkConstructor(srcLen, tgtLen);
			c.build_topdown_exact();
			_allNodes[srcLen-1][tgtLen-1] =  c._nodes;
			_allNode2id[srcLen-1][tgtLen-1] = c._node2id;
			curr = nextPair(curr, maxLen, maxLen);
		}
		eTime = System.currentTimeMillis();
		System.gc();
		long ram = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
		System.err.println(ram/1024/1024+"MB");
		System.err.println((eTime-bTime)/1000+" s.");
		
	}

//	public static int[][][][][] _cache_children = new int[30][30][][][];
//	public static long[][][] _cache_nodes = new long[30][30][];
	
	public FasterBitextNetworkConstructor(int maxSrcLength, int maxTgtLength){
		this._maxSrcLength = maxSrcLength;
		this._maxTgtLength = maxTgtLength;
		this.init();
	}

	public FasterBitextNetworkConstructor(int maxSrcLength, int maxTgtLength, double[][] membership){
		this._maxSrcLength = maxSrcLength;
		this._maxTgtLength = maxTgtLength;
		this._membership = membership;
		this.init();
	}
	
	private void init(){
		int size = (1+this._maxSrcLength)*this._maxSrcLength/2*(1+this._maxTgtLength)*this._maxTgtLength/2*8;
		size = 8000000;
//		System.err.println(size);
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
//		System.err.println(srcLen+","+srcBIndex+","+tgtLen+","+tgtBIndex+","+type);
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
		if(this._numNodes<=0){
			System.err.println("#nodes:"+this._numNodes);
			System.err.println("#edges:"+this._numEdges);
			throw new RuntimeException("This should not happen...");
		}
	}
	
	public void build_topdown(){
		int k = 0;
		int[] curr = new int[]{1,1};
		while(curr!=null){
			this.build(0, curr[0], 0, curr[1], TYPE.N00);
			System.err.println(curr[0]+","+curr[1]+"\t"+this._numNodes+","+this._numEdges+"\t"+(curr[0]+curr[1]));
			this._avgNumNodes += this._numNodes;
			this._avgNumEdges += this._numEdges;
			curr = nextPair(curr, this._maxSrcLength, this._maxTgtLength);
			k++;
		}
		System.err.println("avg nodes:"+this._avgNumNodes/k);
		System.err.println("avg edges:"+this._avgNumEdges/k);
	}
	
	public static int[] nextPair(int[] currPair, int maxSrcLen, int maxTgtLen){
		int x = currPair[0];
		int y = currPair[1];
		if(y == maxTgtLen){
			int new_x = maxSrcLen;
			int new_y = x + y - maxSrcLen + 1;
			if(new_x > maxSrcLen || new_y > maxTgtLen){
				return null;
			}
			return new int[]{new_x, new_y};
		} else if(x == 1){
			int new_x = maxSrcLen;
			int new_y = x + y - maxSrcLen + 1;
			if(new_y<1){
				new_y = 1;
				new_x = x + y;
			}
			return new int[]{new_x, new_y};
		} else {
			int new_x = x - 1;
			int new_y = y + 1;
			return new int[]{new_x, new_y};
		}
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
//	
//	private HashSet<String> vs = new HashSet<String>();
//	
	public int build(int srcBIndex, int srcEIndex, int tgtBIndex, int tgtEIndex, TYPE type){
		
		long node = this.toNetworkID(srcBIndex, srcEIndex, tgtBIndex, tgtEIndex, type.ordinal());
		int result = -1;
		
		if(this.contains(node)){
			int id = this.getNodeId(node);
			if(id==-2)
				return -1;
			return id;
		}
//		
//		String s = srcBIndex+","+srcEIndex+","+tgtBIndex+","+tgtEIndex+","+type.ordinal();
//		if(vs.contains(s)){
//			throw new RuntimeException("xx"+s);
//		}
//		vs.add(s);
//		
		boolean hasChild = false;
		
		int srcLen = srcEIndex - srcBIndex;
		int tgtLen = tgtEIndex - tgtBIndex;
		
		ArrayList<int[]> childrenList = new ArrayList<int[]>();
		
		if(srcLen == 1 && tgtLen == 1 && type == TYPE.I00){
			if(_membership!=null){
//				System.err.println(_membership[tgtBIndex][srcBIndex]);
				if(_membership[tgtBIndex][srcBIndex]>1E-10){
					hasChild = true;
					childrenList.add(new int[]{});
//					result = this.addNode(node);
				}
			} else {
				hasChild = true;
				childrenList.add(new int[]{});
//				result = this.addNode(node);
			}
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

			/**/
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
			/**/

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
		
		if(result==-1)
			this.setNodeId(node, -2);
		
		return result;
	}

	public long toNetworkID(int srcBIndex, int srcEIndex, int tgtBIndex, int tgtEIndex, int type){
		int srcLength = srcEIndex - srcBIndex;
		int tgtLength = tgtEIndex - tgtBIndex;
		return NetworkIDMapper.toHybridNodeID(new int[]{srcLength, srcBIndex, tgtLength, tgtBIndex, type});
	}
	
	
}

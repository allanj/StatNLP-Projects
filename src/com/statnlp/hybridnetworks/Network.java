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
package com.statnlp.hybridnetworks;

import java.io.Serializable;
import java.util.Arrays;

import com.statnlp.commons.types.Instance;

public abstract class Network implements Serializable, HyperGraph{
	
	public static enum NODE_TYPE {sum, max};
	
	private static final long serialVersionUID = -3630379919120581209L;
	
	protected static double[][] insideSharedArray = new double[NetworkConfig._numThreads][];
	protected static double[][] outsideSharedArray = new double[NetworkConfig._numThreads][];
	protected static double[][] maxSharedArray = new double[NetworkConfig._numThreads][];
	protected static int[][][] maxPathsSharedArrays = new int[NetworkConfig._numThreads][][];
	
//	public static GlobalNetworkParam g_Param = null;
	
	//the ids associated with the network (within the scope of the thread).
	protected int _networkId;
	//the id of the thread
	protected int _threadId;
	//the instance
	protected transient Instance _inst;
	//the weight
	protected transient double _weight;
	//the feature parameters
	protected transient LocalNetworkParam _param;
	
	//at each index, store the node's inside score
	protected transient double[] _inside;
	//at each index, store the node's outside score
	protected transient double[] _outside;
	//at each index, store the score of the max tree
	protected transient double[] _max;
	//this stores the paths associated with the above tree
	protected transient int[][] _max_paths;
	
	public Network(){
	}
	
	public Network(int networkId, Instance inst, LocalNetworkParam param){
		this._networkId = networkId;
		this._threadId = param.getThreadId();
		this._inst = inst;
		this._weight = this._inst.getWeight();
		this._param = param;
	}
	
	protected double[] getInsideSharedArray(){
		if(insideSharedArray[this._threadId] == null || this.countNodes() > insideSharedArray[this._threadId].length)
			insideSharedArray[this._threadId] = new double[this.countNodes()];
		return insideSharedArray[this._threadId];
	}
	
	protected double[] getOutsideSharedArray(){
		if(outsideSharedArray[this._threadId] == null || this.countNodes() > outsideSharedArray[this._threadId].length)
			outsideSharedArray[this._threadId] = new double[this.countNodes()];
		return outsideSharedArray[this._threadId];
	}

	protected double[] getMaxSharedArray(){
		if(maxSharedArray[this._threadId] == null || this.countNodes() > maxSharedArray[this._threadId].length)
			maxSharedArray[this._threadId] = new double[this.countNodes()];
		return maxSharedArray[this._threadId];
	}

	protected int[][] getMaxPathSharedArray(){
		if(maxPathsSharedArrays[this._threadId] == null || this.countNodes() > maxPathsSharedArrays[this._threadId].length)
			maxPathsSharedArrays[this._threadId] = new int[this.countNodes()][];
		return maxPathsSharedArrays[this._threadId];
	}
	
	public int getNetworkId(){
		return this._networkId;
	}
	
	public int getThreadId(){
		return this._threadId;
	}
	
	public Instance getInstance(){
		return this._inst;
	}
	
	//get the inside score for the root node.
	public double getInside(){
		return this._inside[this.countNodes()-1];
//		return this._inside[this._inside.length-1];
	}
	
	public double getMax(){
		return this._max[this.countNodes()-1];
//		return this._max[this._max.length-1];
	}

	public double getMax(int k){
//		if(k==87095){
//			int[] arr = NetworkIDMapper.toHybridNodeArray(this.getNode(k));
//			System.err.println("x:"+Arrays.toString(arr)+"\t"+k);
//			System.err.println(Arrays.toString(this.getMaxPath(k)));
//			int child_k = this.getMaxPath(k)[0];
//			double child_max = this.getMax(child_k);
//			System.err.println(child_max);
////			System.err.println(Arrays.toString(this._max));
//			System.err.println();
//		}
		return this._max[k];
	}
	
	public int[] getMaxPath(){
		return this._max_paths[this.countNodes()-1];
	}
	
	public int[] getMaxPath(int k){
		return this._max_paths[k];
	}

	//get the sum of the network.
	public double sum(){
		this.inside();
		return this.getInside();
	}
	
	//train the network.
	public void train(){
		if(this._weight == 0)
			return;
		
		this.inside();
		this.outside();
		this.updateInsideOutside();
//		System.err.println("Inside:"+this.getInside()+" weight:"+this._weight);
		this._param.addObj(this.getInside() * this._weight);
	}
	
//	private static double[] inside_fixed = new double[100000];
	
	protected void inside(){
//		long time = System.currentTimeMillis();
		
		this._inside = this.getInsideSharedArray();
		Arrays.fill(this._inside, 0.0);
		for(int k=0; k<this.countNodes(); k++){
			this.inside(k);
//			System.err.println("inside score at "+k+"="+Math.exp(this._inside[k]));
		}
		
		if(this.getInside()==Double.NEGATIVE_INFINITY){
			throw new RuntimeException("Error! This instance has zero inside score!");
		}
		
//		time = System.currentTimeMillis() - time;
//		System.err.println("INSIDE TIME:"+time+" ms\t"+Math.exp(this.getInside())+"\t"+this.countNodes()+Arrays.toString(NetworkIDMapper.toHybridNodeArray(this.getNode(this.countNodes()-1))));
	}
	
	protected void outside(){
//		long time = System.currentTimeMillis();
		
		this._outside = this.getOutsideSharedArray();
		Arrays.fill(this._outside, Double.NEGATIVE_INFINITY);
		for(int k=this.countNodes()-1; k>=0; k--){
			this.outside(k);
//			System.err.println("outside score at "+k+"="+Math.exp(this._outside[k]));
		}
		
//		time = System.currentTimeMillis() - time;
//		System.err.println("OUTSIDE TIME:"+time+" ms");
	}
	
	protected void updateInsideOutside(){
//		long time = System.currentTimeMillis();
		
		for(int k=0; k<this.countNodes(); k++)
			this.updateInsideOutside(k);
		
//		time = System.currentTimeMillis() - time;
//		System.err.println("UPDATE TIME:"+time+" ms");
	}
	
	//just to gather the features.
	public synchronized void touch(){
//		long time = System.currentTimeMillis();
		
		for(int k=0; k<this.countNodes(); k++)
			this.touch(k);
		
//		time = System.currentTimeMillis() - time;
//		System.err.println("TOUCH TIME:"+time+" ms");
	}
	
	public void max(){
//		long time = System.currentTimeMillis();
		
		this._max = new double[this.countNodes()]; //this._decoder.getMaxArray();
		Arrays.fill(this._max, Double.NEGATIVE_INFINITY);
		
//		this._max_paths = this.getMaxPathSharedArray();//new int[this.countNodes()][];// this._decoder.getMaxPaths();
		this._max_paths = new int[this.countNodes()][];// this._decoder.getMaxPaths();
//		for(int k = 0; k<this.countNodes(); k++)
//			Arrays.fill(this._max_paths[k], 0);
		
		for(int k=0; k<this.countNodes(); k++)
			this.max(k);
		
//		time = System.currentTimeMillis() - time;
//		System.err.println("MAX TIME:"+time+" ms");
	}
	
	protected void inside(int k){
		if(this.isRemoved(k)){
			this._inside[k] = Double.NEGATIVE_INFINITY;
			return;
		}
		
		double inside = 0.0;
		int[][] childrenList_k = this.getChildren(k);
		
		if(childrenList_k.length==0){
			childrenList_k = new int[1][0];
		}
		
		{
			int children_k_index = 0;
			int[] children_k = childrenList_k[children_k_index];

			boolean ignoreflag = false;
			for(int child_k : children_k)
				if(this.isRemoved(child_k))
					ignoreflag = true;
			if(ignoreflag){
				inside = Double.NEGATIVE_INFINITY;
			} else {
				FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
				double score = fa.getScore(this._param);
				for(int child_k : children_k)
					score += this._inside[child_k];
				inside = score;
			}
		}
		
		for(int children_k_index = 1; children_k_index < childrenList_k.length; children_k_index++){
			int[] children_k = childrenList_k[children_k_index];

			boolean ignoreflag = false;
			for(int child_k : children_k)
				if(this.isRemoved(child_k))
					ignoreflag = true;
			if(ignoreflag)
				continue;
			
			FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
			double score = fa.getScore(this._param);
			for(int child_k : children_k)
				score += this._inside[child_k];
			
			double v1 = inside;
			double v2 = score;
			//fix:luwei.31.Dec.2014.
			if(v1==v2 && v2==Double.NEGATIVE_INFINITY){
				inside = Double.NEGATIVE_INFINITY;
			} else if(v1==v2 && v2==Double.POSITIVE_INFINITY){
				inside = Double.POSITIVE_INFINITY;
			} else if(v1>v2){
				inside = Math.log1p(Math.exp(score-inside))+inside;
			} else {
				inside = Math.log1p(Math.exp(inside-score))+score;
			}
			
//			if(this.getInstance().getInstanceId()==10 && k==13){
//				System.err.println();
//				System.err.println("v1 "+v1);
//				System.err.println("v2 "+v2);
//				System.err.println("inside "+inside);
//			}
		}
		
		this._inside[k] = inside;
		
		if(this._inside[k]==Double.NEGATIVE_INFINITY)
			this.remove(k);

//		if(this.getInstance().getInstanceId()==10){
//			System.err.println("inside "+k+"="+this._inside[k]);
//			if(Double.isNaN(this._inside[k])){
//				System.exit(1);
//			}
//		}
	}
	
	protected void outside(int k){
		if(this.isRemoved(k)){
			this._outside[k] = Double.NEGATIVE_INFINITY;
			return;
		}
		else
			this._outside[k] = this.isRoot(k) ? 0.0 : this._outside[k];
		
		if(this._inside[k]==Double.NEGATIVE_INFINITY)
			this._outside[k] = Double.NEGATIVE_INFINITY;
		
		int[][] childrenList_k = this.getChildren(k);
		for(int children_k_index = 0; children_k_index< childrenList_k.length; children_k_index++){
			int[] children_k = childrenList_k[children_k_index];
			
			boolean ignoreflag = false;
			for(int child_k : children_k)
				if(this.isRemoved(child_k)){
					ignoreflag = true; break;
				}
			if(ignoreflag)
				continue;
			
			FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
			double score = fa.getScore(this._param);
			score += this._outside[k];
			for(int child_k : children_k){
				score += this._inside[child_k];
			}

			if(score == Double.NEGATIVE_INFINITY)
				continue;
			
			for(int child_k : children_k){
				double v1 = this._outside[child_k];
				double v2 = score - this._inside[child_k];
				if(v1>v2){
					this._outside[child_k] = v1 + Math.log1p(Math.exp(v2-v1));
				} else {
					this._outside[child_k] = v2 + Math.log1p(Math.exp(v1-v2));
				}
			}
		}
		
		if(this._outside[k]==Double.NEGATIVE_INFINITY){
			this.remove(k);
		}
		
//		if(this.getInstance().getInstanceId()==10){
//			System.err.println("outside "+k+"="+this._outside[k]);
//			if(Double.isNaN(this._outside[k])){
//				System.exit(1);
//			}
//		}

	}
	
	protected void updateInsideOutside(int k){
		if(this.isRemoved(k))
			return;
		
		int[][] childrenList_k = this.getChildren(k);
		
		for(int children_k_index = 0; children_k_index<childrenList_k.length; children_k_index++){
			int[] children_k = childrenList_k[children_k_index];
			
			boolean ignoreflag = false;
			for(int child_k : children_k)
				if(this.isRemoved(child_k)){
					ignoreflag = true; break;
				}
			if(ignoreflag)
				continue;
			
			FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
			double score = fa.getScore(this._param);
			score += this._outside[k];
			for(int child_k : children_k)
				score += this._inside[child_k];
			double count = Math.exp(score-this.getInside());
			count *= this._weight;
			
			/****This part used to check the expected counts*****/
//			if(fa.size()==1  &&  this._weight==-1.0){
//				long parent = getNode(k);
//				int[] parentArr = NetworkIDMapper.toHybridNodeArray(parent);
//				int leftIndex = parentArr[0] - parentArr[1];
//				int rightIndex = parentArr[0];
//				int direction = parentArr[3];
//				int completeness = parentArr[2];
//				System.err.println(fa.toString()+", weight is:"+this._weight+","+leftIndex+","+rightIndex+","+direction+","+completeness+","+parentArr[4]);
//			}
//				
//			if(fa.size()==1&& this._weight==-1.0){
//				System.err.println("inside is:"+this.getInside()+"   Count is:"+count);
//			}
			/*********/
				
//			if(Double.isNaN(count))
//			if(this.getInstance().getInstanceId()==10)
//			{
//				System.err.println(this.getInstance().getInstanceId()+" update at "+k+"="+count+" for features "+fa.size()+"\t"+fa.viewCurrent()+"\t"+this._inside[k]+"+"+this._outside[k]+"||"+score+"|"+this.getInside());
//			}
			fa.update(this._param, count);
		}
	}
	
	protected void touch(int k){
		if(this.isRemoved(k))
			return;
		
		int[][] childrenList_k = this.getChildren(k);
		for(int children_k_index = 0; children_k_index < childrenList_k.length; children_k_index++){
			int[] children_k = childrenList_k[children_k_index];
			this._param.extract(this, k, children_k, children_k_index);
		}
	}
	
	protected void max(int k){
		if(this.isRemoved(k)){
			this._max[k] = Double.NEGATIVE_INFINITY;
			return;
		}
		if(this.isSumNode(k)){

			double inside = 0.0;
			int[][] childrenList_k = this.getChildren(k);
			
			if(childrenList_k.length==0){
				childrenList_k = new int[1][0];
			}
			
			{
				int children_k_index = 0;
				
				int[] children_k = childrenList_k[children_k_index];
				
				boolean ignoreflag = false;
				for(int child_k : children_k)
					if(this.isRemoved(child_k))
						ignoreflag = true;
				if(ignoreflag){
					inside = Double.NEGATIVE_INFINITY;
				} else {
					FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
					double score = fa.getScore(this._param);
					for(int child_k : children_k)
						score += this._max[child_k];
					inside = score;
				}
				
				//if it is a sum node, then any path is the same for such a node.
				//this is something you need to make sure when constructing such a network.
				this._max_paths[k] = children_k;
			}
			
			for(int children_k_index = 1; children_k_index < childrenList_k.length; children_k_index++){
				int[] children_k = childrenList_k[children_k_index];

				boolean ignoreflag = false;
				for(int child_k : children_k)
					if(this.isRemoved(child_k))
						ignoreflag = true;
				if(ignoreflag)
					continue;
				
				FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
				double score = fa.getScore(this._param);
				for(int child_k : children_k)
					score += this._max[child_k];
				
				double v1 = inside;
				double v2 = score;
				//fix:luwei.31.Dec.2014.
				if(v1==v2 && v2==Double.NEGATIVE_INFINITY){
					inside = Double.NEGATIVE_INFINITY;
				} else if(v1==v2 && v2==Double.POSITIVE_INFINITY){
					inside = Double.POSITIVE_INFINITY;
				} else if(v1>v2){
					inside = Math.log1p(Math.exp(score-inside))+inside;
				} else {
					inside = Math.log1p(Math.exp(inside-score))+score;
				}
				
			}
			
			this._max[k] = inside;
		}
		
		else{

			int[][] childrenList_k = this.getChildren(k);
			this._max[k] = Double.NEGATIVE_INFINITY;
			
			for(int children_k_index = 0; children_k_index < childrenList_k.length; children_k_index++){
				int[] children_k = childrenList_k[children_k_index];
				boolean ignoreflag = false;
				for(int child_k : children_k)
					if(this.isRemoved(child_k)){
						ignoreflag = true; break;
					}
				if(ignoreflag)
					continue;
				
				FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
				double max = fa.getScore(this._param);
				for(int child_k : children_k)
					max += this._max[child_k];
				if(max >= this._max[k]){
					this._max[k] = max;
					this._max_paths[k] = children_k;
				}
			}
			
		}
//		System.err.println("max["+k+"]"+_max[k]);
	}

	//count the number of removed nodes.
	public int countRemovedNodes(){
		int count = 0;
		for(int k = 0; k<this.countNodes(); k++)
			if(this.isRemoved(k))
				count++;
		return count;
	}
	
	//get the root node of the network.
	public long getRoot(){
		return this.getNode(this.countNodes()-1);
	}
	
	//get the array form of the network.
	public int[] getNodeArray(int k){
		long node = this.getNode(k);
		return NetworkIDMapper.toHybridNodeArray(node);
	}
	
	//this ad-hoc method is useful when performing
	//some special sum operations (in conjunction with max operations)
	//in the decoding phase.
	protected boolean isSumNode(int k){
		return false;
	}
	
	//display the string form of the network.
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i<this.countNodes(); i++)
			sb.append(Arrays.toString(NetworkIDMapper.toHybridNodeArray(this.getNode(i))));
		return sb.toString();
	}
	
	public LocalNetworkParam getLocalParam(){
		return this._param;
	}
	
}
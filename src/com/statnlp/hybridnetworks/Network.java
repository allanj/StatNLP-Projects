/** Statistical Natural Language Processing System
    Copyright (C) 2014-2016  Lu, Wei

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
import java.util.HashSet;

import com.statnlp.commons.types.Instance;

/**
 * The base class for representing networks. This class is equipped with algorithm to calculate the 
 * inside-outside score, which is also a generalization to the forward-backward score.<br>
 * You might want to use {@link TableLookupNetwork} for more functions such as adding nodes and edges.
 * @see TableLookupNetwork
 * @author Wei Lu <luwei@statnlp.com>
 *
 */
public abstract class Network implements Serializable, HyperGraph{
	
	public static enum NODE_TYPE {sum, max};
	
	private static final long serialVersionUID = -3630379919120581209L;
	
	protected static double[][] insideSharedArray = new double[NetworkConfig._numThreads][];
	protected static double[][] outsideSharedArray = new double[NetworkConfig._numThreads][];
	protected static double[][] maxSharedArray = new double[NetworkConfig._numThreads][];
	protected static int[][][] maxPathsSharedArrays = new int[NetworkConfig._numThreads][][];
	
	/** The working array for each thread for calculating max k  scores */
	protected static double[][][] maxKSharedArray = new double[NetworkConfig._numThreads][][];
	/** The working array for each thread for storing max k  paths (for backtracking) */
	protected static int[][][][] maxKPathsSharedArrays = new int[NetworkConfig._numThreads][][][];
	/** The working array for each thread for storing max k  paths (for backtracking) */
	protected static int[][][][] maxKPathsListBestSharedArrays = new int[NetworkConfig._numThreads][][][];
	
	/** The ids associated with the network (within the scope of the thread). */
	protected int _networkId;
	/** The id of the thread */
	protected int _threadId;
	/** The instance */
	protected transient Instance _inst;
	/** The weight */
	protected transient double _weight;
	/** The feature parameters */
	protected transient LocalNetworkParam _param;
	
	//at each index, store the node's inside score
	protected transient double[] _inside;
	//at each index, store the node's outside score
	protected transient double[] _outside;
	//at each index, store the score of the max tree
	protected transient double[] _max;
	//at each index, store the loss of the max tree
	protected transient double[] _loss;
	//this stores the paths associated with the above tree
	protected transient int[][] _max_paths;
	
	/** At each index, store the score of the max k  tree */
	protected transient double[][] _max_k;
	/** Stores the paths associated with the above tree */
	protected transient int[][][] _max_k_paths;
	/** Stores the paths best list associated with the above tree */
	protected transient int[][][] _max_k_path_listbest;
	
	//store the marginal score for each index.
	protected transient double[] _marginal;
	
	/**
	 * Default constructor. Note that the network constructed using this default constructor is lacking 
	 * the {@link LocalNetworkParam} object required for actual use.
	 * Use this only for generating generic network, which is later actualized using another constructor.
	 * @see #Network(int, Instance, LocalNetworkParam)
	 */
	public Network(){
	}
	
	/**
	 * Construct a network
	 * @param networkId
	 * @param inst
	 * @param param
	 */
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
	
	protected int[][][] getMaxKPathSharedArray(){
		if(maxKPathsSharedArrays[this._threadId] == null || this.countNodes() > maxKPathsSharedArrays[this._threadId].length)
			maxKPathsSharedArrays[this._threadId] = new int[this.countNodes()][NetworkConfig._topKValue][];
		return maxKPathsSharedArrays[this._threadId];
	}
	
	protected double[][] getMaxKSharedArray(){
		if(maxKSharedArray[this._threadId] == null || this.countNodes() > maxKSharedArray[this._threadId].length)
			maxKSharedArray[this._threadId] = new double[this.countNodes()][NetworkConfig._topKValue];
		return maxKSharedArray[this._threadId];
	}
	
	protected int[][][] getMaxKPathListBestSharedArray(){
		if(maxKPathsListBestSharedArrays[this._threadId] == null || this.countNodes() > maxKPathsListBestSharedArrays[this._threadId].length)
			maxKPathsListBestSharedArrays[this._threadId] = new int[this.countNodes()][NetworkConfig._topKValue][];
		return maxKPathsListBestSharedArrays[this._threadId];
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
	
	/**
	 * Return the maximum score for this network
	 * @return
	 */
	public double getMax(){
		int rootIdx = this.countNodes()-1;
		if(NetworkConfig.USE_STRUCTURED_SVM){
			return this._max[rootIdx]+this._loss[rootIdx];	
		} else {
//			System.err.println("Printing topK decoding as well");
//			for(int k=0;k<NetworkConfig._topKValue;k++){
//				System.err.println(k+"-best:"+this._max_k[rootIdx][k]);
//			}
			return this._max[rootIdx];
		}
//		return this._max[this._max.length-1];
	}
	
	public double getMaxTopK(int nodeIdx, int k){
		return this._max_k[nodeIdx][k];
	}

	/**
	 * Return the maximum score for this network ending in the node with the specified index
	 * @param k
	 * @return
	 */
	public double getMax(int k){
		if(NetworkConfig.USE_STRUCTURED_SVM){
			return this._max[k]+this._loss[k];
		} else {
			return this._max[k];
		}
	}
	
	/**
	 * Return the children of the hyperedge which is part of the maximum path of this network
	 * @return
	 */
	public int[] getMaxPath(){
		return this._max_paths[this.countNodes()-1];
	}

	/**
	 * Return the children of the hyperedge which is part of the maximum path of this network
	 * ending at the node at the specified index
	 * @return
	 */
	public int[] getMaxPath(int k){
		return this._max_paths[k];
	}
	
	
	public int[] getMaxTopKPath(int nodeIdx, int k){
		return this._max_k_paths[nodeIdx][k];
	}

	public int[] getMaxTopKBestListPath(int nodeIdx, int k){
		return this._max_k_path_listbest[nodeIdx][k];
	}
	
	/**
	 * Get the sum of the network (i.e., the inside score)
	 * @return
	 */
	public double sum(){
		this.inside();
		return this.getInside();
	}
	
	/**
	 * Return the marginal score for the network at a specific index (Note: do not support SSVM yet)
	 * @param k
	 * @return
	 */
	public double getMarginal(int k){
		return this._marginal[k];
	}
	
	/**
	 * The loss of the structure from leaf nodes up to node <code>k</code>.<br>
	 * This is used for structured SVM, and generally the implementation requires the labeled Instance.<br>
	 * This does some check whether the loss is actually required. Loss is not calculated during test, since
	 * there is no labeled instance.<br>
	 * This will call {@link #totalLossUpTo(int, int[])}, where the actual implementation resides.
	 * @param k
	 * @param child_k
	 * @return
	 */
	public double loss(int k, int[] child_k){
		if(_inst.getInstanceId() > 0 && !_inst.isLabeled()){
			return 0.0;
		}
		return totalLossUpTo(k, child_k);
	}

	/**
	 * The loss of the structure from leaf nodes up to node <code>k</code>.<br>
	 * This is used for structured SVM, and generally the implementation requires the labeled Instance.<br>
	 * Note that the implementation can access the loss of the child nodes at _loss[child_idx] and
	 * the best path so far is stored at getMaxPath(child_idx), which represents the hyperedge connected to
	 * node <code>child_idx</code> which is part of the best path so far.
	 * @param k
	 * @param child_k
	 * @return
	 */
	public abstract double totalLossUpTo(int k, int[] child_k);
	
	/**
	 * Train the network
	 */
	public void train(){
		if(this._weight == 0)
			return;
		if(NetworkConfig.USE_STRUCTURED_SVM){
			this.max();
		} else {
			this.inside();
			this.outside();
		}
		this.updateGradient();
		this.updateObjective();
	}
	
//	private static double[] inside_fixed = new double[100000];
	
	/**
	 * Calculate the inside score of all nodes
	 */
	protected void inside(){
//		long time = System.currentTimeMillis();
		
		this._inside = this.getInsideSharedArray();
		Arrays.fill(this._inside, 0.0);
		for(int k=0; k<this.countNodes(); k++){
			this.inside(k);
			//if(k==11 && !this.getInstance().isLabeled()) System.err.println("[11 inside]:"+this._inside[k]);
		}
		
		if(this.getInside()==Double.NEGATIVE_INFINITY){
			throw new RuntimeException("Error! This instance has zero inside score!");
		}
//		time = System.currentTimeMillis() - time;
		//System.err.println("[total inside]:"+this.getInside());
//		System.err.println("INSIDE TIME:"+time+" ms\t"+Math.exp(this.getInside())+"\t"+this.countNodes()+Arrays.toString(NetworkIDMapper.toHybridNodeArray(this.getNode(this.countNodes()-1))));
	}
	
	/**
	 * Calculate the outside score of all nodes
	 */
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
	
	/**
	 * Calculate and update the inside-outside score of all nodes
	 */
	protected void updateGradient(){
//		long time = System.currentTimeMillis();
		
		if(NetworkConfig.USE_STRUCTURED_SVM){
			// Max is already calculated
			int rootIdx = this.countNodes()-1;
			this.updateGradient(rootIdx);
		} else {
			for(int k=0; k<this.countNodes(); k++){
				this.updateGradient(k);
			}
		}			
//		time = System.currentTimeMillis() - time;
//		System.err.println("UPDATE TIME:"+time+" ms");
	}
	
	protected void updateObjective(){
		if(NetworkConfig.USE_STRUCTURED_SVM){
			this._param.addObj(this.getMax() * this._weight);
		} else {
			this._param.addObj(this.getInside() * this._weight);
		}
	}
	
	/**
	 * Goes through each nodes in the network to gather list of features
	 */
	public synchronized void touch(){
//		long time = System.currentTimeMillis();
		
		for(int k=0; k<this.countNodes(); k++)
			this.touch(k);
		
//		time = System.currentTimeMillis() - time;
//		System.err.println("TOUCH TIME:"+time+" ms");
	}
	
	/**
	 * Calculate the maximum score for all nodes
	 */
	public void max(){
//		long time = System.currentTimeMillis();
		
		this._max = new double[this.countNodes()]; //this._decoder.getMaxArray();
		this._loss = new double[this.countNodes()];
		Arrays.fill(this._max, Double.NEGATIVE_INFINITY);
		Arrays.fill(this._loss, 0.0);
		
//		this._max_paths = this.getMaxPathSharedArray();//new int[this.countNodes()][];// this._decoder.getMaxPaths();
		this._max_paths = new int[this.countNodes()][];// this._decoder.getMaxPaths();
//		for(int k = 0; k<this.countNodes(); k++)
//			Arrays.fill(this._max_paths[k], 0);
		for(int k=0; k<this.countNodes(); k++){
			this.max(k);
		}
//		time = System.currentTimeMillis() - time;
//		System.err.println("MAX TIME:"+time+" ms");
	}
	
	/**
	 * Calculate the marginal score for all nodes
	 */
	public void marginal(){
		this._marginal = new double[this.countNodes()];
		double sum = this.sum();
		this.outside();
		Arrays.fill(this._marginal, Double.NEGATIVE_INFINITY);
		for(int k=0; k<this.countNodes(); k++){
			this.marginal(k,sum);
		}
	}

	
	/**
	 * Calculate the inside score for the specified node
	 * @param k
	 */
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
			inside = sumLog(inside, score);
			
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
	
	/**
	 * Calculate the outside score for the specified node
	 * @param k
	 */
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
	
	/**
	 * Calculate and update the gradient for features present at the specified node
	 * @param k
	 */
	protected void updateGradient(int k){
		if(this.isRemoved(k))
			return;
		
		int[][] childrenList_k = this.getChildren(k);
		int[] maxChildren = null;
		if(NetworkConfig.USE_STRUCTURED_SVM){
			maxChildren = this.getMaxPath(k); // For Structured SVM
		}
		
		for(int children_k_index = 0; children_k_index<childrenList_k.length; children_k_index++){
			double count = 0.0;
			int[] children_k = childrenList_k[children_k_index];
			
			boolean ignoreflag = false;
			for(int child_k : children_k){
				if(this.isRemoved(child_k)){
					ignoreflag = true;
					break;
				}
			}
			if(NetworkConfig.USE_STRUCTURED_SVM){ // Consider only max path
				if(!Arrays.equals(children_k, maxChildren)){
					continue;
				}
			}
			if(ignoreflag){
				continue;
			}
			
			FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
			if(NetworkConfig.USE_STRUCTURED_SVM){
				count = 1;
			} else {
				double score = fa.getScore(this._param); // w*f
				score += this._outside[k];  // beta(s')
				for(int child_k : children_k)
					score += this._inside[child_k]; // alpha(s)
				count = Math.exp(score-this.getInside()); // Divide by normalization term Z
			}
			count *= this._weight;
			
			fa.update(this._param, count);
			if(NetworkConfig.USE_STRUCTURED_SVM){
				for(int child_k: children_k){
					this.updateGradient(child_k);	
				}
			}
		}
	}
	
	/**
	 * Gather features from the specified node
	 * @param k
	 */
	protected void touch(int k){
		
		if(this.isRemoved(k))
			return;
		
		int[][] childrenList_k = this.getChildren(k);
		for(int children_k_index = 0; children_k_index < childrenList_k.length; children_k_index++){
			int[] children_k = childrenList_k[children_k_index];
			this._param.extract(this, k, children_k, children_k_index);
		}
	}
	
	/**
	 * Calculate the maximum score at the specified node
	 * @param k
	 */
	protected void max(int k){
		if(this.isRemoved(k)){
			this._max[k] = Double.NEGATIVE_INFINITY;
			this._loss[k] = 0.0;
			return;
		}
		
		if(this.isSumNode(k)){

			double inside = 0.0;
			double loss = 0.0;
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
					loss = 0.0;
				} else {
					FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
					double score = fa.getScore(this._param);
					loss = loss(k, children_k);
					for(int child_k : children_k){
						score += this._max[child_k];
					}
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
				loss += loss(k, children_k);
				for(int child_k : children_k){
					score += this._max[child_k];
				}
				
				inside = sumLog(inside, score);
				
			}
			
			this._max[k] = inside;
			this._loss[k] = loss;
		}
		
		else{

			int[][] childrenList_k = this.getChildren(k);
			this._max[k] = Double.NEGATIVE_INFINITY;
			this._loss[k] = 0.0;
			
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
				double loss = loss(k, children_k);
				for(int child_k : children_k){
					max += this._max[child_k];
				}
				boolean shouldUpdate = false;
				if(NetworkConfig.USE_STRUCTURED_SVM){
					if(max+loss >= this._max[k]+this._loss[k]){
						shouldUpdate = true;
					}
				} else {
					if(max >= this._max[k]){
						shouldUpdate = true;
					}
				}
				if(shouldUpdate){
					this._max[k] = max;
					this._loss[k] = loss;
					this._max_paths[k] = children_k;
				}
			}
		}
		//System.err.println("max["+k+"]"+_max[k]);
	}

	 /*
	 * Top K decoding approach
	 */
	protected void topK(){
		int topK = NetworkConfig._topKValue;
		this._max_k = getMaxKSharedArray();
		this._max_k_paths = getMaxKPathSharedArray();
		this._max_k_path_listbest  = getMaxKPathListBestSharedArray();
//		this._max_k[0] = this._max;
//		this._max_k_paths[0] = this._max_paths;
		
		for(int nodeIdx=0; nodeIdx<this.countNodes(); nodeIdx++){
			this.askKBest(nodeIdx, topK);
		}
		
	}

	/**
	 * Ask the k^{th} best of nodeIdx, currently specific for CKY-styly parsing
	 * @param nodeIdx
	 * @param q
	 */
	protected void askKBest(int nodeIdx, int TOPK){
		int[][] childrenList_k = this.getChildren(nodeIdx);
		for(int children_k_index = 0; children_k_index < childrenList_k.length; children_k_index++){
			int[] children_k = childrenList_k[children_k_index];
			boolean ignoreflag = false;
			for(int child_k : children_k)
				if(this.isRemoved(child_k)){
					ignoreflag = true; break;
				}
			if(ignoreflag)
				continue;
			
			BinaryHeap heap = new BinaryHeap(NetworkConfig._topKValue+1);
			int n = 0;
			
			int currMaxPath[][] = new int[TOPK][children_k.length]; //topk and (l-best, r-best)
			
			FeatureArray fa = this._param.extract(this, nodeIdx, children_k, children_k_index);
			double score = fa.getScore(this._param);
			double firstBest = score;
			for(int child_k : children_k){
				firstBest += this._max_k[child_k][0];
			}
			int[] firstBestListIdx = new int[children_k.length];
			Arrays.fill(firstBestListIdx, 0);
			ValueIndexPair vip = new ValueIndexPair(firstBest, firstBestListIdx); //first best of left, first best of right
			heap.add(vip);
			HashSet<IndexPair> beenPushedSet  = new HashSet<IndexPair>();
			beenPushedSet.add(new IndexPair(firstBestListIdx));
			if(children_k.length==0){
				vip = heap.removeMax();
				currMaxPath[0] = vip.bestListIdx;
				for(int k=1;k<TOPK;k++) currMaxPath[k] = null;
			}else{
				while(n < TOPK) {
				    vip = heap.removeMax();
				    if(vip.val == Double.NEGATIVE_INFINITY)
					break;
				    currMaxPath[n] = vip.bestListIdx; 
				    n++;
				    if(n >= TOPK)
				    	break;
					
				    for(int ith=0;ith<vip.bestListIdx.length;ith++){
				    	int[] listIdx = vip.bestListIdx.clone();
				    	IndexPair ip = new IndexPair(listIdx);
				    	ip.set(ith, listIdx[ith]+1);
				    	if(!beenPushedSet.contains(ip)){
				    		double kbestScore  = 0;
				    		for(int ck=0;ck<children_k.length;ck++){
				    			kbestScore += ith==ck? this._max_k[children_k[ck]][vip.bestListIdx[ck]+1]: this._max_k[children_k[ck]][vip.bestListIdx[ck]];
				    		}
				    		kbestScore+=score;
				    		heap.add(new ValueIndexPair(kbestScore, ip.indices));
				    		beenPushedSet.add(ip);
				    	}
				    }
				}
				for(int x=n;x<TOPK;x++) currMaxPath[x] = null;
			}
			//merge two topK vectors into one
			if(children_k_index == 0){
				this._max_k_path_listbest[nodeIdx] = currMaxPath;
				for(int tk=0;tk<this._max_k_path_listbest[nodeIdx].length;tk++){
					if(this._max_k_path_listbest[nodeIdx][tk]==null){
						this._max_k_paths[nodeIdx][tk] = null;
						this._max_k[nodeIdx][tk] = Double.NEGATIVE_INFINITY;
						continue;
					}
					this._max_k_paths[nodeIdx][tk] = children_k;
					double tkbest = 0;
					int c=0;
					for(int child_k : children_k){
						tkbest += this._max_k[child_k][this._max_k_path_listbest[nodeIdx][tk][c++]];
					}
					this._max_k[nodeIdx][tk] = tkbest+score;
				}
			}else{
				this._max_k_path_listbest[nodeIdx] = this.merge(currMaxPath, children_k,nodeIdx, score);
			}
			
		}
	}
	
	/**
	 * 
	 * @param currMaxPath: nth best, and the best pair from child
	 * @param globalMaxKPath
	 * @param maxKScore[nodeIdx][kthbest]: only know the children
	 * @return
	 */
	private int[][] merge(int currMaxPath[][], int[] children, int nodeIdx, double score){
		int[][] answer = new int[NetworkConfig._topKValue][children.length];//pair is two
		int[][] answerPath = new int[NetworkConfig._topKValue][children.length];
		int i=0, j=0, k=0;
		while(k<answer.length){
//			System.err.println("node idx:"+nodeIdx+" i:"+i+" j:"+j);
			double left = Double.NEGATIVE_INFINITY;
			if(currMaxPath[i]!=null){
				left = 0;
				for(int ith=0;ith<children.length;ith++){
					left += this._max_k[children[ith]][currMaxPath[i][ith]];
				}
			}
			
			int[] pathChildren = this._max_k_paths[nodeIdx][j]==null? null:this._max_k_paths[nodeIdx][j].clone();
//			int leftChild = this._max_k_paths[nodeIdx][j]==null? -1:this._max_k_paths[nodeIdx][j][0]; 
//			int rightChild = this._max_k_paths[nodeIdx][j]==null? -1:this._max_k_paths[nodeIdx][j][1];
			
			double right = Double.NEGATIVE_INFINITY;
			if(!(pathChildren==null || this._max_k_path_listbest[nodeIdx][j]==null)){
				right = 0;
				for(int pth=0;pth<pathChildren.length;pth++)
					right += this._max_k[pathChildren[pth]][this._max_k_path_listbest[nodeIdx][j][pth]];
			}
			
			
			if(left==Double.NEGATIVE_INFINITY && right==Double.NEGATIVE_INFINITY){
				break;
			}
			if(left > right){
				answer[k] = currMaxPath[i];
				this._max_k[nodeIdx][k] = left + score;
				answerPath[k] = children;
				i++;
			}else{
				answer[k] = this._max_k_path_listbest[nodeIdx][j];
				this._max_k[nodeIdx][k] = right + score;
				answerPath[k] = this._max_k_paths[nodeIdx][j].clone();
				j++;
			}
			k++;
			
		}
		for(int x=k;x<answer.length;x++) {answer[x] = null; answerPath[x]= null;}
		this._max_k_paths[nodeIdx] = answerPath;
		return answer;
	}
	
	/**
	 * Calculate the marginal score at the specific node
	 * @param k
	 */
	protected void marginal(int k, double sum){
		if(this.isRemoved(k)){
			this._marginal[k] = Double.NEGATIVE_INFINITY;
			return;
		}
		//since inside and outside are in log space
//		double alpha = this._inside[k];
//		double beta = this._outside[k];
//		double total = sum;
		
		this._marginal[k] = this._inside[k] + this._outside[k] - sum;
	}
	
	private double sumLog(double inside, double score) {
		double v1 = inside;
		double v2 = score;
		if(v1==v2 && v2==Double.NEGATIVE_INFINITY){
			return Double.NEGATIVE_INFINITY;
		} else if(v1==v2 && v2==Double.POSITIVE_INFINITY){
			return Double.POSITIVE_INFINITY;
		} else if(v1>v2){
			return Math.log1p(Math.exp(v2-v1))+v1;
		} else {
			return Math.log1p(Math.exp(v1-v2))+v2;
		}
	}

	/**
	 * Count the number of removed nodes
	 */
	public int countRemovedNodes(){
		int count = 0;
		for(int k = 0; k<this.countNodes(); k++)
			if(this.isRemoved(k))
				count++;
		return count;
	}
	
	/**
	 * Get the root node of the network.
	 * @return
	 */
	public long getRoot(){
		return this.getNode(this.countNodes()-1);
	}
	
	/**
	 * Get the array form of the node at the specified index in the node array
	 */
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
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i<this.countNodes(); i++)
			sb.append(Arrays.toString(NetworkIDMapper.toHybridNodeArray(this.getNode(i))));
		return sb.toString();
	}
	
	
	
}


class ValueIndexPair {
    public double val;
    public int[] bestListIdx;
		
    public ValueIndexPair(double val, int[] bestListIdx) {
	this.val = val;
	this.bestListIdx = bestListIdx;
    }

    public int compareTo(ValueIndexPair other) {
	if(val < other.val)
	    return -1;
	if(val > other.val)
	    return 1;
	return 0;
    }
		
}

// Max Heap
// We know that never more than K elements on Heap
class BinaryHeap { 
    private int DEFAULT_CAPACITY; 
    protected int currentSize; 
    private ValueIndexPair[] theArray;
  
    public BinaryHeap(int def_cap) {
	DEFAULT_CAPACITY = def_cap;
	theArray = new ValueIndexPair[DEFAULT_CAPACITY+1]; 
	// theArray[0] serves as dummy parent for root (who is at 1) 
	// "largest" is guaranteed to be larger than all keys in heap
	theArray[0] = new ValueIndexPair(Double.POSITIVE_INFINITY,new int[]{-1,-1});          
	currentSize = 0; 
    } 
  
    public ValueIndexPair getMax() { 
	return theArray[1]; 
    }
  
    private int parent(int i) { return i / 2; } 
    private int leftChild(int i) { return 2 * i; } 
    private int rightChild(int i) { return 2 * i + 1; } 
  
    public void add(ValueIndexPair e) { 
   
		// bubble up: 
		int where = currentSize + 1; // new last place 
		while ( e.compareTo(theArray[parent(where)]) > 0 ){ 
		    theArray[where] = theArray[parent(where)]; 
		    where = parent(where); 
		} 
		theArray[where] = e; currentSize++;
    }
 
    public ValueIndexPair removeMax() {
		ValueIndexPair min = theArray[1];
		theArray[1] = theArray[currentSize];
		currentSize--;
		boolean switched = true;
		// bubble down
		for ( int parent = 1; switched && parent < currentSize; ) {
		    switched = false;
		    int leftChild = leftChild(parent);
		    int rightChild = rightChild(parent);
	
		    if(leftChild <= currentSize) {
			// if there is a right child, see if we should bubble down there
			int largerChild = leftChild;
			if ((rightChild <= currentSize) && 
			    (theArray[rightChild].compareTo(theArray[leftChild])) > 0){
			    largerChild = rightChild; 
			}
			if (theArray[largerChild].compareTo(theArray[parent]) > 0) {      
			    ValueIndexPair temp = theArray[largerChild];
			    theArray[largerChild] = theArray[parent];
			    theArray[parent] = temp;
			    parent = largerChild;
			    switched = true;
			}
		    }
		} 
		return min;
    }
 
}
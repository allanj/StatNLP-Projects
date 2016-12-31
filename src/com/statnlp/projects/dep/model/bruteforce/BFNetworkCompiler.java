package com.statnlp.projects.dep.model.bruteforce;

import java.util.ArrayList;
import java.util.Arrays;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkIDMapper;


public class BFNetworkCompiler extends NetworkCompiler{

	private static final long serialVersionUID = -2388666010977956073L;

	public enum NODE_TYPES {LEAF, depInLinear,ROOT};
	public int _size;
	public BFNetwork genericUnlabeledNetwork;
	public int number = 0;
	public long addedNum = 0;
	public BFNetworkCompiler(int size){
		_size  = size;
		this.compileUnlabeledInstancesGeneric();
	}
	
	@Override
	public BFNetwork compile(int networkId, Instance inst, LocalNetworkParam param) {
		// TODO Auto-generated method stub
		BFInstance lcrfInstance = (BFInstance)inst;
		if(lcrfInstance.isLabeled())
			return compileLabeledInstances(networkId, lcrfInstance, param);
		else
			return compileUnlabeledInstances(networkId, lcrfInstance, param);
	}
	
	public long toNode_leaf(){
		int[] arr = new int[]{0,0,0,0,NODE_TYPES.LEAF.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	
	private long toNodeDepInLinear(int currIndex, int headIndex){
		return NetworkIDMapper.toHybridNodeID(new int[]{currIndex, headIndex, 0,0, NODE_TYPES.depInLinear.ordinal()});
	}
	
	public long toNode_root(int size){
		int[] arr = new int[]{size, 0,0,0,NODE_TYPES.ROOT.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}

	private int[][] sent2LeftDepRel(int[] heads){
		int[][] leftDepRel = new int[_size][];
		ArrayList<ArrayList<Integer>> leftDepList = new ArrayList<ArrayList<Integer>>();
		for(int i=0;i<leftDepRel.length;i++) leftDepList.add(new ArrayList<Integer>());
		for(int pos = 1; pos<_size; pos++){
			int headIdx = heads[pos];
			if(headIdx<0) continue;
			int smallOne = Math.min(pos, headIdx);
			int largeOne = Math.max(pos, headIdx);
			ArrayList<Integer> curr = leftDepList.get(largeOne);
			curr.add(smallOne);
		}
		for(int pos = 1; pos<_size; pos++){
			ArrayList<Integer> curr = leftDepList.get(pos);
			leftDepRel[pos] = new int[curr.size()];
			for(int j=0; j<curr.size();j++)
				leftDepRel[pos][j] = curr.get(j);
		}
		return leftDepRel;
	}
	
	
	public void findAllNetworks(Network network){
		BFNetwork lcrfNetwork = (BFNetwork)network;
		
		int[] heads = new int[_size];
		long root = toNode_root(_size);
		int rootIdx = Arrays.binarySearch(lcrfNetwork.getAllNodes(),root);
		find(lcrfNetwork, rootIdx, heads);
		System.out.println("number is:"+number);
		System.out.println("added edges is: "+addedNum);
	}
	
	private void find(BFNetwork network, int curr_k, int[] curr_heads){
		
		int[][] children = network.getChildren(curr_k);
		for(int i=0; i<children.length; i++){
			int[] new_curr_heads = curr_heads.clone();
			if(children[i].length==0) continue;
			int[] childArr = network.getNodeArray(children[i][0]);
			int pos = childArr[0];
			int headIndex = childArr[1];
			new_curr_heads[pos] = headIndex;
			if(pos==(_size-1) && headIndex!=0){
//				System.out.println(pos);
				continue;
			}
			find(network, children[i][0], new_curr_heads);
		}
		if(network.getNodeArray(curr_k)[0]==0){
			
			int[][] graph = new int[this._size][_size];
			boolean haveRootHead = false;
			for(int i=1; i<curr_heads.length;i++){
				graph[i][curr_heads[i]] = 1;
				graph[curr_heads[i]][i] = 1;
				if(curr_heads[i]==0) haveRootHead = true;
			}
			if(haveRootHead){
//				System.out.println("now "+Arrays.toString(curr_heads));
				boolean[] visited = new boolean[_size];
				visited[1] = true;
				boolean connected = isConnected(graph, visited);
				if(connected){
					graph = new int[_size][_size];
					for(int i=1; i<curr_heads.length;i++){
						graph[curr_heads[i]][i] = 1;
					}
					//build a directed graph. 
					visited = new boolean[_size];
					visited[0] = true;
					boolean cyclic = traverseCycle(graph, 0, visited);
					if(!cyclic){
						long added = checkNumAdded(curr_heads);
//						System.out.println(Arrays.toString(curr_heads)+ " added:"+added);
//						for(int k=1; k<curr_heads.length-1;k++)
//							System.out.print(curr_heads[k]+"");
//						System.out.println();
						addedNum+=added;
						number++;
					}
				}
			}
		}
	}
	
	private long checkNumAdded(int[] heads){
		long added = 0;
		int[][] leftNodes = sent2LeftDepRel(heads);
		int[][] addedEdges = new int[_size][_size];
		for(int span = 3; span <= _size-1; span++){
			for(int start = 1; start <=  _size - span; start++ ){
				int end = start + span - 1; 
				if(heads[start]==end || heads[end]==start || addedEdges[start][end]==1) continue;
				
				boolean connected = traverseLeft(start, end, leftNodes);
				
				if(connected && addedEdges[start][end]!=1) {
					added++;
					addedEdges[start][end]=1;
					addedEdges[end][start]=1;
				}
			}
		}
		return added;
	}
	
	private boolean traverseLeft(int start, int end, int[][] leftNodes){
		for(int l=0; l<leftNodes[end].length; l++){
			if(leftNodes[end][l]<start) continue;
			if(leftNodes[end][l]==start)
				return true;
			else if(traverseLeft(start, leftNodes[end][l], leftNodes))
				return true;
			else continue;
		}
		return false;
	}
	
	private boolean isConnected(int[][] graph, boolean[] visited){
		boolean connected = true;
		//traverse from node 1
		for(int b = 2; b<graph.length; b++){
			if(!visited[b] && graph[1][b] == 1) {
				visited[b] = true;
				traverse(graph, b, visited);
			}
		}
		for(int idx=1;idx<visited.length; idx++)
			if(!visited[idx]) connected = false;
		
		return connected;
	}
	
	private void traverse(int[][] graph, int curr, boolean[] visited){
		for(int c=1; c<graph.length; c++){
			if(!visited[c] && graph[curr][c]==1){
				visited[c] = true;
				traverse(graph, c, visited);
			}
		}
	}
	
	private boolean traverseCycle(int[][] graph, int curr, boolean[] visited){
		for(int c=1; c<graph.length; c++){
			if(graph[curr][c]==1){
				if(!visited[c]){
					visited[c] = true;
					boolean cycle = traverseCycle(graph, c, visited);
					if(cycle) return true;
				}else{
					return true; //is cyclic
				}
			}
		}
//		System.err.println("run out of the loop? means all graph[curr][c] = 0?");
		return false;
	}
	
	
	@Override
	public BFInstance decompile(Network network) {
		BFNetwork lcrfNetwork = (BFNetwork)network;
		BFInstance lcrfInstance = (BFInstance)lcrfNetwork.getInstance();
		BFInstance result = lcrfInstance.duplicate();
		
		int[] heads = new int[lcrfInstance.size()];
		long root = toNode_root(lcrfInstance.size());
		int rootIdx = Arrays.binarySearch(lcrfNetwork.getAllNodes(),root);
		
		for(int i=1;i<lcrfInstance.size();i++){
			int child_k = lcrfNetwork.getMaxPath(rootIdx)[0];
			long child = lcrfNetwork.getNode(child_k);
			int[] child_1_arr = NetworkIDMapper.toHybridNodeArray(child);
			int pos = child_1_arr[0];
			//System.err.println(Arrays.toString(child_1_arr));
			int headIndex = NetworkIDMapper.toHybridNodeArray(child)[1];
			heads[pos] = headIndex;
					
			rootIdx = child_k;
		}
		result.setPredHeads(heads);
		return result;
	}
	

	public BFNetwork compileLabeledInstances(int networkId, BFInstance inst, LocalNetworkParam param){
		BFNetwork lcrfNetwork = new BFNetwork(networkId, inst,param);
		
		long leaf = toNode_leaf();
		long[] children = new long[]{leaf};
		lcrfNetwork.addNode(leaf);
		BFInstance bfInst = (BFInstance)lcrfNetwork.getInstance();
		for(int i=1;i<lcrfNetwork.getInstance().size();i++){
			
			if(i==bfInst.getInput().get(i).getHeadIndex()){
				throw new RuntimeException(" current index and the head index cannot be the same");
			}
			long depInLinear = toNodeDepInLinear(i, bfInst.getInput().get(i).getHeadIndex());
			lcrfNetwork.addNode(depInLinear);
			long[] currentNodes = new long[]{depInLinear};
			lcrfNetwork.addEdge(depInLinear, children);
			children = currentNodes;
		}
		long root = toNode_root(bfInst.size());
		lcrfNetwork.addNode(root);
		lcrfNetwork.addEdge(root, children);
		
		lcrfNetwork.finalizeNetwork();
		lcrfNetwork.iniRemoveArr();
		if(!genericUnlabeledNetwork.contains(lcrfNetwork)){
			System.err.println("wrong");
		}
		return lcrfNetwork;
	}
	
	public BFNetwork compileUnlabeledInstances(int networkId, BFInstance inst, LocalNetworkParam param){
		
		long[] allNodes = genericUnlabeledNetwork.getAllNodes();
		long root = toNode_root(inst.size());
		int rootIdx = Arrays.binarySearch(allNodes, root);
		BFNetwork lcrfNetwork = new BFNetwork(networkId, inst,allNodes,genericUnlabeledNetwork.getAllChildren() , param, rootIdx+1);
		
		findAllNetworks(lcrfNetwork);
		return lcrfNetwork;
	}
	
	public void compileUnlabeledInstancesGeneric(){
		BFNetwork lcrfNetwork = new BFNetwork();
		
		long linearLeaf = toNode_leaf();
		long[] children = new long[]{linearLeaf};
		lcrfNetwork.addNode(linearLeaf);
		long[] currentNodes = new long[this._size];
		
		for(int i=1;i<_size;i++){
			currentNodes = new long[this._size];
			
			
			for(int headIdx = 0; headIdx < this._size;headIdx++){
				if(headIdx==i) { currentNodes[headIdx] = -1; continue;}
				long depInLinear = toNodeDepInLinear(i, headIdx);
				for(long child: children){
					if(child==-1) continue;
					if(lcrfNetwork.contains(child)){
						currentNodes[headIdx] = depInLinear;
						lcrfNetwork.addNode(depInLinear);
						lcrfNetwork.addEdge(depInLinear, new long[]{child});
					}
				}
			}
			
			
			long root = toNode_root(i+1);
			lcrfNetwork.addNode(root);
			for(long child:currentNodes){
				if(child==-1) continue;
				lcrfNetwork.addEdge(root, new long[]{child});
			}
			children = currentNodes;
			
		}
		lcrfNetwork.finalizeNetwork();
		genericUnlabeledNetwork =  lcrfNetwork;
		System.err.println("total number of nodes:"+genericUnlabeledNetwork.getAllNodes().length);
	}
	
	
	
	
	
}

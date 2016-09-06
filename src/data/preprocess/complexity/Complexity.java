package data.preprocess.complexity;

import java.util.ArrayList;
import java.util.HashSet;

public class Complexity {

	public static int N = 5;
	public static ArrayList<HashSet<Edge>> allPossible;
	
	public static void findAllPossibleStructures(){
		ArrayList<Integer> remains = new ArrayList<Integer>();
		for(int n=2; n<= N; n++)
			remains.add(n);
		allPossible = new ArrayList<HashSet<Edge>>();
		findNext(new HashSet<Edge>(), 1, remains);
		System.out.println(allPossible.size());
	}
	
	private static void findNext(HashSet<Edge> edges, int currNode,  ArrayList<Integer> remain){
		
		if(remain.size()==0 && edges.size()!=(N-1)) throw new RuntimeException("error");
		if(remain.size()!=0 && edges.size()==(N-1)) throw new RuntimeException("error");
		if(remain.size()==0){
			System.out.println(edges.toString());
//			Collections.sort(edges);
			if(allPossible.size()==0 && edges.size()==(N-1)) allPossible.add(edges);
			else{
				boolean contain = false;
				for(int e=0; e<allPossible.size(); e++){
					HashSet<Edge> inedges = allPossible.get(e);
					if(edges.size()==(N-1) && inedges.containsAll(edges)){
						contain = true;
						break;
					}
				}
				if(!contain){
//					System.err.println(edges.toString());
					allPossible.add(edges);
				}
			}
			
		}
		ArrayList<String> combin = new ArrayList<String>();
		gray("", remain.size(), combin);
		for(int c=0; c<combin.size(); c++){
			String valids = combin.get(c);
			@SuppressWarnings("unchecked")
			HashSet<Edge> currEdges = (HashSet<Edge>) edges.clone();
			ArrayList<Integer> currRemain = new ArrayList<Integer>();
			ArrayList<Integer> nextNodes = new ArrayList<Integer>();
			for(int v = 0; v < valids.length(); v++){
				if(valids.substring(v, v+1).equals("0")) {currRemain.add(remain.get(v)); continue;}
				currEdges.add(new Edge(currNode, remain.get(v)));
				nextNodes.add(remain.get(v));
			}
			for(int nextNode: nextNodes){
				findNext(currEdges, nextNode, currRemain);
			}
		}
		
	}
	
	
	
	
	
	// append reverse of order n gray code to prefix string, and print
    public static void yarg(String prefix, int n, ArrayList<String> combin) {
        if (n == 0) combin.add(prefix);
        else {
            gray(prefix + "1", n - 1, combin);
            yarg(prefix + "0", n - 1, combin);
        }
    }  

    // append order n gray code to end of prefix string, and print
    public static void gray(String prefix, int n, ArrayList<String> combin) {
        if (n == 0) combin.add(prefix);
        else {
            gray(prefix + "0", n - 1, combin);
            yarg(prefix + "1", n - 1, combin);
        }
    }  
    
    public static void main(String[] args) {
    	ArrayList<String> combin = new ArrayList<String>();
        gray("", 0, combin);
        System.out.println(combin.toString());
        
        findAllPossibleStructures();
    }


}

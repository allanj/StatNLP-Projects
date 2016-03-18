package tmp.experiments;

import java.util.HashMap;

public class TestRam {
	
	public static void main(String args[]){
		
		int size = 10000000;
		double[] arr = new double[size];
		
//		HashMap<Integer, Double> map = new HashMap<Integer, Double>();
//		long bRam = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		long bTime = System.currentTimeMillis();
		for(int i = 0; i<size; i++){
			arr[i] = (i-981) * 1000 + (i+982)/10000/10000;
//			map.put(i, Math.random());
		}
		long eTime = System.currentTimeMillis();
		System.gc();
		long eRam = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		System.gc();
		
		System.err.println((eTime-bTime)/1000.0+" seconds.");
		System.err.println((eRam)/1024.0/1024+" MB.");
		
//		System.err.println(map.size());
		
	}

}

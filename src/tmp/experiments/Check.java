package tmp.experiments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import com.statnlp.dag.ID;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class Check {
	
	public static void main(String args[]){
		
		
		
		Double x = null;
		
		System.err.println(x.hashCode());
		
		System.exit(1);
		
//		double oldWeight = -62.8442221017404;
//		double weight = -813.260480728427;
//		double uuu = Math.log(Math.exp(oldWeight)+Math.exp(weight));
//
//		double newWeight = Math.log1p(Math.exp(weight-oldWeight))+oldWeight;
//		
//		System.err.println(uuu);
//		System.err.println(newWeight);
//		System.exit(1);

		{
			long ram;
			
			int n = 100000000;
//			{
//				Random r = new Random(1234);
//				ArrayList<Long> arr = new ArrayList<Long>();
//				for(int k = 0; k<n; k++){
//					arr.add((long)k);
//				}
//				System.gc();
//				ram = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
//				System.err.println(ram/1024.0/1024);
//				System.err.println(arr.size());
//				
//				long time = System.currentTimeMillis();
//				for(int k = 0; k<10000; k++){
//					Collections.binarySearch(arr, r.nextLong());
//				}
//				time = System.currentTimeMillis() - time;
//				System.err.println(time+" ms");
//			}
			
//			{
//				Random r = new Random(1234);
//				long[] kkk = new long[n];
//				System.gc();
//				ram = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
//				System.err.println(ram/1024.0/1024);
//				System.err.println(kkk.length);
//				
//				long time = System.currentTimeMillis();
//				for(int k = 0; k<10000; k++){
//					Arrays.binarySearch(kkk, r.nextLong());
//				}
//				time = System.currentTimeMillis() - time;
//				System.err.println(time+" ms");
//			}
			
			{
				boolean[] kkk = new boolean[n];
				System.gc();
				ram = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
				System.err.println(ram/1024.0/1024);
				System.err.println(kkk.length);
			}
		}
		
		System.exit(1);
		
		double v1 = (Math.random()-.5)/10;
		double v2 = (Math.random()-.5)/10;
		double v3 = (Math.random()-.5)/10;
		double v4 = (Math.random()-.5)/10;
		
		double u1 = Math.exp(v1);
		double u2 = Math.exp(v2);
		double u3 = Math.exp(v3);
		double u4 = Math.exp(v4);
		
		double u = u1 + u2 + u3 + u4;
		
		System.err.println(u1+"="+u1/u);
		System.err.println(u2+"="+u2/u);
		System.err.println(u3+"="+u3/u);
		System.err.println(u4+"="+u4/u);
		
		System.exit(1);
		
		int n = 10000000;
//		ID[] ids = new ID[n];
//		for(int k = 0; k<n; k++){
//			ids[k] = new ID(new int[]{1,2,3,4,5});
//		}
		long[] kkk = new long[n];
		System.gc();
		long ram = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
		System.err.println(ram/1024.0/1024);
		
//		System.err.println(ids.length);
		System.err.println(kkk.length);
		
		System.exit(1);
		
		double score = Double.NEGATIVE_INFINITY;
		double inside = Double.NEGATIVE_INFINITY;
		double v = Math.log1p(Math.exp(score-inside))+inside;
		
		System.err.println(v);
		
		System.exit(1);
		
		System.err.println(Character.isLowerCase(')'));
		
		System.exit(1);
		
		System.err.println(Math.exp(-700));
		System.err.println(Math.log1p(Math.exp(-700)));
		
		System.exit(1);
		
		long bTime = System.currentTimeMillis();
		int size = 20;
		System.err.println(Math.pow(size, 5));
		for(int i1 = 0; i1<size; i1++){
			for(int i2 = 0; i2<size; i2++){
				for(int i3 = 0; i3<size; i3++){
					for(int i4 = 0; i4<size; i4++){
						for(int i5 = 0; i5<size; i5++){
							int[] array = new int[]{i1, i2, i3, i4, i5};
							long value = NetworkIDMapper.toHybridNodeID(array);
							NetworkIDMapper.toHybridNodeArray(value);
						}
					}
				}
			}
		}
		long eTime = System.currentTimeMillis();
		System.err.println(eTime-bTime+" ms");
		
	}

}
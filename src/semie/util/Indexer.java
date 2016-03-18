package semie.util;

public class Indexer {
	
	public static int encode(int bIndex, int eIndex){
		assert bIndex < 1000 && bIndex >= 0;
		assert eIndex < 1000 && eIndex >= 0;
		return bIndex*1000 + eIndex;
	}
	
	public static int[] decode(int key){
		assert key < 1001*1000 && key >= 0;
		return new int[]{key/1000, key%1000};
	}
	
}

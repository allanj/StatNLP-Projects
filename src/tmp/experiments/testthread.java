package tmp.experiments;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

public class TestThread {
	
	public static void main(String args[]) throws InterruptedException{
		
		ConcurrentHashMap<String, Integer> table = new  ConcurrentHashMap<String, Integer>();
		
		RunnableDemo R1 = new RunnableDemo("Thread-1");
		R1.start(table);
		
		RunnableDemo R2 = new RunnableDemo("Thread-2");
		R2.start(table);
		
//		Thread t = new Thread();
//		t.sleep(1000);
		
		System.err.println(table);

	}
	
}
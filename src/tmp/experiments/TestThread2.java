package tmp.experiments;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

public class TestThread2 {
	
	public static void main(String args[]) throws InterruptedException{
		
//		ConcurrentHashMap<String, Integer> table = new  ConcurrentHashMap<String, Integer>();
		
		RWDictionary dict = new RWDictionary();
		
		RunnableDemo2 R1 = new RunnableDemo2("Thread-1");
//		R1.start(table);
		R1.start(dict);
		
		RunnableDemo2 R2 = new RunnableDemo2("Thread-2");
//		R2.start(table);
		R2.start(dict);
		
//		Thread t = new Thread();
		Thread.sleep(1000);
		
		System.err.println(dict);

	}
	
}
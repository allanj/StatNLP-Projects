package tmp.experiments;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

public class RunnableDemo2 implements Runnable{
	
	private Thread t;
	private String threadName;
	private RWDictionary _dict;
//	public ConcurrentHashMap<String, Integer> _table;
//	private Lock lock = new Lock();
	
	public RunnableDemo2(String name){
		this.threadName = name;
		System.out.println("Creating "+this.threadName);
	}
	
	@Override
	public synchronized void run() {
		doit();
	}
	
	private synchronized void doit(){
		
		System.out.println("Running... "+threadName);
		String s = "x";
		for(int i = 1000; i>0; i--){
//			System.err.println(s);
//			double v = this._dict.get(s);
//			this._dict.put(s, v+i);
			this._dict.inc(s, i, threadName);
		}
		
		System.out.println("Thread "+threadName+" exiting.");
	}
	
	public void start(RWDictionary dict){
		this._dict = dict;
		if(t==null){
			t = new Thread(this, threadName);
			t.start();
		}
//	t.join();
	}
	
//	public void start(ConcurrentHashMap<String, Integer> table) {
//		this._table = table;
//		System.out.println("Starting "+threadName);
//		if(t==null){
//			t = new Thread(this, threadName);
//			t.start();
//		}
////		t.join();
//	}
	
}

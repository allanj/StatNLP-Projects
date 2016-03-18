package tmp.experiments;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

public class RunnableDemo implements Runnable{
	
	private Thread t;
	private String threadName;
	public ConcurrentHashMap<String, Integer> _table;
//	private Lock lock = new Lock();
	
	public RunnableDemo(String name){
		this.threadName = name;
		System.out.println("Creating "+this.threadName);
	}
	
	@Override
	public synchronized void run() {
		doit();
	}
	
	private synchronized void doit(){
		
		System.out.println("Running "+threadName);
		String s = "x";
		for(int i = 10000; i>0; i--){
//			System.err.println(s);
			if(!this._table.containsKey(s)){
				this._table.put(s, 0);
			}
			int old_v = this._table.get(s);
			this._table.put(s, old_v+i);
//				System.out.println("Thread:"+threadName+","+i);
//				Thread.sleep(50);
		}
		
		System.out.println("Thread "+threadName+" exiting.");
	}
	
	public void start(ConcurrentHashMap<String, Integer> table) {
		this._table = table;
		System.out.println("Starting "+threadName);
		if(t==null){
			t = new Thread(this, threadName);
			t.start();
		}
//		t.join();
	}
	
}

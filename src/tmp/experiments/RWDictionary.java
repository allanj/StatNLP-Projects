package tmp.experiments;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class RWDictionary {

	private final Map<String, Double> m = new TreeMap<String, Double>();
	private final ReentrantLock lock = new ReentrantLock();
//	private final Lock r = rwl.lock();
	
	public String toString(){
		return this.m.toString();
	}
	
//	public String[] allKeys() {
//		r.lock();
//		try {
//			return (String[]) m.keySet().toArray();
//		} finally {
//			r.unlock();
//		}
//	}
	
//	public double get(String key) {
//		System.err.println(">>????!!!!");
//		r.lock();
//		try {
//			if(!m.containsKey(key)){
//				return 0.0;
//			}
//			return m.get(key);
//		} finally {
//			r.unlock();
//		}
//	}
//	
//	public double put(String key, double value) {
//		System.err.println(">>????");
//		w.lock();
//		try {
//			return m.put(key, value);
//		} finally {
//			w.unlock();
//		}
//	}
	
	public double inc(String key, double value, String threadName) {
		
		System.err.println(">>1"+threadName);
		lock.lock();
		System.err.println(">>2"+threadName);
//		r.lock();
		try {
			if(!m.containsKey(key)){
				m.put(key, 0.0);
			}
			double value_old = m.get(key);
			return m.put(key, value_old + value);
		} finally {
//			r.unlock();
//			w.unlock();
			lock.unlock();
		}
	}
	
//	public void clear() {
//		w.lock();
//		try {
//			m.clear();
//		} finally {
//			w.unlock();
//		}
//	}
}
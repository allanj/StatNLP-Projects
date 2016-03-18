package semie.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class IOUtil {
	
	public static void writeHashMap3(HashMap<Integer, ArrayList<Integer>> map, ObjectOutputStream out)throws IOException{
		out.writeInt(map.size());
		
		Iterator<Integer> keys = map.keySet().iterator();
		while(keys.hasNext()){
			int key = keys.next();
			ArrayList<Integer> vals = map.get(key);
			out.writeInt(key);
			out.writeInt(vals.size());
			for(int i = 0; i<vals.size(); i++){
				out.writeInt(vals.get(i));
			}
		}
	}
	
	public static HashMap<Integer, ArrayList<Integer>> readHashMap3(ObjectInputStream in)throws IOException, ClassNotFoundException{
		HashMap<Integer, ArrayList<Integer>> map = new HashMap<Integer, ArrayList<Integer>>();
		
		int size = in.readInt();
		for(int i = 0; i<size; i++){
			int key = in.readInt();
			int subsize = in.readInt();
			ArrayList<Integer> vals = new ArrayList<Integer>();
			for(int j = 0; j<subsize; j++){
				vals.add(in.readInt());
			}
			map.put(key, vals);
		}
		
		return map;
	}
	
	public static void writeArray(Object[] array, ObjectOutputStream out)throws IOException{
		out.writeInt(array.length);
		for(Object s : array)
			out.writeObject(s);
	}
	
	public static void writeArray(String[] array, ObjectOutputStream out)throws IOException{
		out.writeInt(array.length);
		for(String s : array)
			out.writeUTF(s);
	}
	
	public static void writeArray(String[][] array, ObjectOutputStream out)throws IOException{
		out.writeInt(array.length);
		for(String[] a : array)
			writeArray(a, out);
	}
	
	public static String[] readStringArray(ObjectInputStream in)throws IOException{
		String[] array = new String[in.readInt()];
		for(int i = 0; i<array.length; i++)
			array[i] = in.readUTF();
		return array;
	}
	
	public static String[][] readStringArray2(ObjectInputStream in)throws IOException{
		String[][] array = new String[in.readInt()][];
		for(int i = 0; i<array.length; i++)
			array[i] = readStringArray(in);
		return array;
	}
	
	public static int[] readIntArray(ObjectInputStream in)throws IOException{
		int[] array = new int[in.readInt()];
		for(int i = 0; i < array.length; i++)
			array[i] = in.readInt();
		return array;
	}
	
	public static <T> void writeArrayList(ArrayList<T> list, ObjectOutputStream out)throws IOException{
		out.writeInt(list.size());
		for(int i = 0; i<list.size(); i++){
			out.writeObject(list.get(i));
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> ArrayList<T> readArrayList(ObjectInputStream in)throws IOException, ClassNotFoundException{
		int size = in.readInt();
		ArrayList<T> list = new ArrayList<T>();
		for(int i = 0; i<size; i++){
			list.add((T)in.readObject());
		}
		return list;
	}
	
	public static <T> void writeHashMap2(HashMap<T, Integer> map, ObjectOutputStream out) throws IOException{
		Iterator<T> keys = map.keySet().iterator();
		out.writeInt(map.size());
		while(keys.hasNext()){
			T key = keys.next();
			int v = map.get(key);
			out.writeObject(key);
			out.writeInt(v);
		}
	}
	
	public static <T> void writeHashMap(HashMap<Integer, T> map, ObjectOutputStream out) throws IOException{
		Iterator<Integer> keys = map.keySet().iterator();
		out.writeInt(map.size());
		while(keys.hasNext()){
			int key = keys.next();
			T t = map.get(key);
			out.writeInt(key);
			out.writeObject(t);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> HashMap<T, Integer> readHashMap2(ObjectInputStream in)throws IOException, ClassNotFoundException{
		HashMap<T, Integer> map = new HashMap<T, Integer>();
		int size = in.readInt();
		for(int i = 0; i<size; i++){
			T key = (T)in.readObject();
			map.put(key, in.readInt());
		}
		return map;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> HashMap<Integer, T> readHashMap(ObjectInputStream in)throws IOException, ClassNotFoundException{
		HashMap<Integer, T> map = new HashMap<Integer, T>();
		int size = in.readInt();
		for(int i = 0; i<size; i++){
			int key = in.readInt();
			map.put(key, (T) in.readObject());
		}
		return map;
	}
}

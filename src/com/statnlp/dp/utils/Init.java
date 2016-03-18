package com.statnlp.dp.utils;

import java.util.HashMap;
import java.util.Iterator;

import com.statnlp.hybridnetworks.NetworkConfig;

public class Init {

	
	public static String PARENT_IS = DPConfig.PARENT_IS;
	public static String OE = DPConfig.OE;
	public static String ONE = DPConfig.ONE;
	
	
	/**
	 * Initialize the type map.
	 * @return
	 */
	public static String[] initializeTypeMap(){
		NetworkConfig.typeMap = new HashMap<String, Integer>();
		NetworkConfig.typeMap.put("O", 0);
		NetworkConfig.typeMap.put("person", 1);  NetworkConfig.typeMap.put("gpe", 2);  
		NetworkConfig.typeMap.put("EMPTY", 4); 
		NetworkConfig.typeMap.put("organization", 3);
		String[] entities = new String[NetworkConfig.typeMap.size()];
		Iterator<String> iter = NetworkConfig.typeMap.keySet().iterator();
		while(iter.hasNext()){
			String entity = iter.next();
			entities[NetworkConfig.typeMap.get(entity)] = entity;
		}
		return entities;
	}
	
	public static String[] initializeUniqueModelTypeMap(String[] selectedEntities){
		NetworkConfig.typeMap = new HashMap<String, Integer>();
		int index = 0;
		NetworkConfig.typeMap.put(OE, index++);
		NetworkConfig.typeMap.put(ONE, index++);
		for(int i=0;i<selectedEntities.length;i++){
			NetworkConfig.typeMap.put(selectedEntities[i],index++);
		}
		NetworkConfig.typeMap.put(PARENT_IS+OE, index++);
		NetworkConfig.typeMap.put(PARENT_IS+ONE, index++);
		for(int i=0;i<selectedEntities.length;i++){
			NetworkConfig.typeMap.put(PARENT_IS+selectedEntities[i],index++);
		}
		String[] entities = new String[NetworkConfig.typeMap.size()/2];
		Iterator<String> iter = NetworkConfig.typeMap.keySet().iterator();
		while(iter.hasNext()){
			String entity = iter.next();
			if(!entity.startsWith(PARENT_IS))
				entities[NetworkConfig.typeMap.get(entity)] = entity;
		}
		return entities;
	}
	
	public static String[] initializeINCModelTypeMap(String[] selectedEntities){
		NetworkConfig.typeMap = new HashMap<String, Integer>();
		int index = 0;
		NetworkConfig.typeMap.put("OE", index++);
		NetworkConfig.typeMap.put("ONE", index++);
		for(int i=0;i<selectedEntities.length;i++){
			NetworkConfig.typeMap.put(selectedEntities[i],index++);
		}
		for(int m=0;m<=1;m++)
			for(int n=0;n<=1;n++){
				NetworkConfig.typeMap.put("pae:"+m+":"+n+":"+"OE", index++);
				NetworkConfig.typeMap.put("pae:"+m+":"+n+":"+"ONE", index++);
				for(int i=0;i<selectedEntities.length;i++)
					NetworkConfig.typeMap.put("pae:"+m+":"+n+":"+selectedEntities[i],index++);
			}
		
		
		String[] entities = new String[NetworkConfig.typeMap.size()/5];
		Iterator<String> iter = NetworkConfig.typeMap.keySet().iterator();
		while(iter.hasNext()){
			String entity = iter.next();
			if(!entity.startsWith("pae"))
				entities[NetworkConfig.typeMap.get(entity)] = entity;
		}
		return entities;
	}
	
	public static String[] initializeHyperEdgeModelTypeMap(String[] selectedEntities){
		NetworkConfig.typeMap = new HashMap<String, Integer>();
		int index = 0;
		NetworkConfig.typeMap.put("OE", index++);
		NetworkConfig.typeMap.put("ONE", index++);
		for(int i=0;i<selectedEntities.length;i++){
			NetworkConfig.typeMap.put(selectedEntities[i],index++);
		}
		String[] entities = new String[NetworkConfig.typeMap.size()];
		Iterator<String> iter = NetworkConfig.typeMap.keySet().iterator();
		while(iter.hasNext()){
			String entity = iter.next();
			entities[NetworkConfig.typeMap.get(entity)] = entity;
		}
		return entities;
	}
	
	

	
	
}

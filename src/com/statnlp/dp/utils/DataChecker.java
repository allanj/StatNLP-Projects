package com.statnlp.dp.utils;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.DependInstance;
import com.statnlp.hybridnetworks.NetworkConfig;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.UnnamedDependency;

public class DataChecker {
	
	
	public static void checkJoint(DependInstance[] insts, String[] entities){
		int totalNum = 0;
		int totalNotIncomplete = 0;
		for(DependInstance inst: insts){
			Sentence sent = inst.getInput();
			int start = 0; int end = -1;
			int currNum = 0;
			String prevEntity = "";
			for(int i=1;i<sent.length();i++){
				String e = sent.get(i).getEntity();
				e = e.equals("O")? e: e.substring(2, e.length());
				prevEntity = sent.get(i-1).getEntity();
				prevEntity = prevEntity.equals("O")? prevEntity:prevEntity.substring(2, prevEntity.length());
				//Need to fix the case of continuous two entity, start:inclusive, end:exclusive
				if(!e.equals(prevEntity)){
					if(!e.equals("O")){
						if(!prevEntity.equals("O")){
							end = i;
							currNum++;
							totalNum+=joinChecking( inst,  sent,  start,  end,  prevEntity);
							//debug:
							if((end>start+1) && !(sent.get(start).getHeadIndex()==end-1 || sent.get(end-1).getHeadIndex()==start))
								totalNotIncomplete++;
						}
						start = i;
					}else{
						end = i;
						currNum++;
						totalNum+=joinChecking( inst,  sent,  start,  end,  prevEntity);
						if((end>start+1)  && !(sent.get(start).getHeadIndex()==end-1 || sent.get(end-1).getHeadIndex()==start)){
							totalNotIncomplete++;
//							System.err.println(sent.toString());
//							System.exit(0);
						}
							
					}
				}
			}
			String lastE = sent.get(sent.length()-1).getEntity();
			lastE = lastE.equals("O")? lastE: lastE.substring(2, lastE.length());
			
			if(!lastE.equals("O")){
				currNum++;
				end = sent.length();
				totalNum+=joinChecking( inst,  sent,  start,  end,  prevEntity);
				if((end>start+1) && !(sent.get(start).getHeadIndex()==end-1 || sent.get(end-1).getHeadIndex()==start))
					totalNotIncomplete++;
			}
			
			//check the case that (B-e,I-e.....)(B-e,I-e,.....)
			int bc = 0;
			for(int k=1;k<sent.length();k++){
				if(sent.get(k).getEntity().startsWith("B-")){
					bc++;
				}
			}
			if(currNum!=bc){
				System.err.println("Current: "+currNum+", B number:"+bc);
				System.err.println(sent.toString());
				System.err.println();
				
			}
		}
		System.err.println("Total invalid:"+totalNum);
		int[] all = new int[entities.length];
		int[][] allOutside = new int[NetworkConfig.typeMap.size()/2][10];
		int[] total = new int[entities.length];
		for(DependInstance inst: insts){
			for(int i=0;i<all.length;i++){
				all[i]+=inst.unValidNum[i];
				total[i]+=inst.entityNum[i];
			}
			for(int e=0;e<allOutside.length;e++){
				for(int i=0;i<10;i++){
					allOutside[e][i] += inst.outsideHeads[e][i];
				}
			}
			
		}
		System.err.println("Total and Invalid number:");
		for(int i=0;i<all.length;i++){
			System.err.println(entities[i]+":"+all[i]+" out of "+ total[i]);
		}
		
		System.err.println("*****outside head****:");
		for(int e=0;e<allOutside.length;e++){
			if(entities[e].equals("O") || entities[e].equals("EMPTY")|| entities[e].equals("OE")|| entities[e].equals("ONE")) continue;
			for(int i=0;i<10;i++){
				System.err.println(i+":"+entities[e]+": outside head:"+allOutside[e][i]);
			}
		}
		
		System.err.println("******Total number of entity span that is not incomplete******");
		System.err.println("Total number of entity span that is not incomplete: "+totalNotIncomplete);

		
	}
	
	
	private static int  joinChecking(DependInstance inst, Sentence sent, int start, int end, String prevEntity){
		int totalNum = 0;
//		System.err.println(prevEntity);
		inst.entityNum[NetworkConfig.typeMap.get(prevEntity)]++;
		//for start, end-1
		boolean hInside = true;
		int outside = 0;
		for(int k=start+1;k<end-1;k++){
			if(!(sent.get(k).getHeadIndex()>=start && sent.get(k).getHeadIndex()<end)) {
				hInside = false;
				outside++;
			}
		}
//		if(outside==2 && prevEntity.equals("organization")){
//			System.err.println(sent.toString());
//		}
		inst.outsideHeads[NetworkConfig.typeMap.get(prevEntity)][outside]++;
		if(start!=end-1 && !checkInside(sent.get(start).getHeadIndex(), start, end) && !checkInside(sent.get(end-1).getHeadIndex(), start, end)){
			inst.outsideHeads[NetworkConfig.typeMap.get(prevEntity)][9]++;
			for(int k=start+1;k<end-1;k++){
				if(!(sent.get(k).getHeadIndex()>=start && sent.get(k).getHeadIndex()<end)) {
					inst.outsideHeads[NetworkConfig.typeMap.get(prevEntity)][8]++;
					break;
				}
			}
		}
		if(checkInside(sent.get(start).getHeadIndex(), start, end) || checkInside(sent.get(end-1).getHeadIndex(), start, end)){
			
		}else {
			hInside = false;
		}
		
		if(start==end-1) {hInside = true;}
		if(!hInside) {
//			if(sent.get(i-1).getEntity().equals("gpe")){
//				
//				System.err.println(sent.toString());
//				System.err.println(start+":"+end);System.err.println();
//			}
				
			totalNum++;
			inst.unValidNum[NetworkConfig.typeMap.get(prevEntity)]++;
		}
		return totalNum;
	}
	
	
	/**
	 * right exclusive
	 * @param head
	 * @param left
	 * @param right
	 * @return
	 */
	private static boolean checkInside(int head, int left, int right){
		
		if(head>=left && head<right) return true;
		return false;
	}


	
	public static boolean checkProjective(ArrayList<UnnamedDependency> list){
		for(int i=0;i<list.size()-1;i++){
			UnnamedDependency dependency_i = list.get(i);
			CoreLabel iHeadLabel = (CoreLabel)(dependency_i.governor());
			CoreLabel iModifierLabel = (CoreLabel)(dependency_i.dependent());
			int iSmallIndex = Math.min(iHeadLabel.sentIndex(), iModifierLabel.sentIndex());
			int iLargeIndex = Math.max(iHeadLabel.sentIndex(), iModifierLabel.sentIndex());
			for(int j=i+1;j<list.size();j++){
				UnnamedDependency dependency_j = list.get(j);
				CoreLabel jHeadLabel = (CoreLabel)(dependency_j.governor());
				CoreLabel jModifierLabel = (CoreLabel)(dependency_j.dependent());
				int jSmallIndex = Math.min(jHeadLabel.sentIndex(), jModifierLabel.sentIndex());
				int jLargeIndex = Math.max(jHeadLabel.sentIndex(), jModifierLabel.sentIndex());
				if(iSmallIndex<jSmallIndex && iLargeIndex<jLargeIndex && jSmallIndex<iLargeIndex) return false;
				if(iSmallIndex>jSmallIndex && jLargeIndex>iSmallIndex && iLargeIndex>jLargeIndex) return false;
			}
		}
		return true;
	}


	public static int[] countEntities(DependInstance[] insts, String[] entities){
		int[] total = new int[entities.length];
		for(DependInstance dpInst: insts){
			Sentence sent = dpInst.getInput();
			for(int i=1;i<sent.length();i++){
				if(sent.get(i).getEntity().startsWith("B-")){
					for(int j=0;j<entities.length;j++){
						if(sent.get(i).getEntity().endsWith(entities[j]))
							total[j]++;
					}
				}
			}
		}
		return total;
	}
}

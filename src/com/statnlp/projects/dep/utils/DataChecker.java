package com.statnlp.projects.dep.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.statnlp.commons.types.Sentence;
import com.statnlp.projects.dep.DependInstance;
import com.statnlp.projects.dep.commons.EntitySpan;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.UnnamedDependency;

public class DataChecker {
	
	
	/**
	 * Check all the instance whether they are incomplete or not.
	 * @param insts
	 * @param entities
	 */
	public static void checkJoint(DependInstance[] insts, String[] entities, HashMap<String, Integer> typeMap){
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
							totalNum+=joinChecking( inst,  sent,  start,  end,  prevEntity,  typeMap);
							//debug:
							if((end>start+1) && !(sent.get(start).getHeadIndex()==end-1 || sent.get(end-1).getHeadIndex()==start))
								totalNotIncomplete++;
						}
						start = i;
					}else{
						end = i;
						currNum++;
						totalNum+=joinChecking( inst,  sent,  start,  end,  prevEntity,typeMap);
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
				totalNum+=joinChecking( inst,  sent,  start,  end,  prevEntity, typeMap);
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
		int[][] allOutside = new int[typeMap.size()/2][10];
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
	
	
	private static int  joinChecking(DependInstance inst, Sentence sent, int start, int end, String prevEntity, HashMap<String, Integer> typeMap){
		int totalNum = 0;
//		System.err.println(prevEntity);
		inst.entityNum[typeMap.get(prevEntity)]++;
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
		inst.outsideHeads[typeMap.get(prevEntity)][outside]++;
		if(start!=end-1 && !checkInside(sent.get(start).getHeadIndex(), start, end) && !checkInside(sent.get(end-1).getHeadIndex(), start, end)){
			inst.outsideHeads[typeMap.get(prevEntity)][9]++;
			for(int k=start+1;k<end-1;k++){
				if(!(sent.get(k).getHeadIndex()>=start && sent.get(k).getHeadIndex()<end)) {
					inst.outsideHeads[typeMap.get(prevEntity)][8]++;
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
			inst.unValidNum[typeMap.get(prevEntity)]++;
		}
		return totalNum;
	}
	
	
	/**
	 * check the head is inside the span or not
	 * @param head
	 * @param left:inclusive
	 * @param right:exclusive
	 * @return
	 */
	private static boolean checkInside(int head, int left, int right){
		
		if(head>=left && head<right) return true;
		return false;
	}


	/**
	 * check wheter this list of dependency link is projective for one whole dependency structure
	 * @param list
	 * @return
	 */
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
	
	/**
	 * Using the heads only to check the projectiveness
	 * @param heads
	 * @return
	 */
	public static boolean checkProjective(List<Integer> heads){
		for (int i = 0; i < heads.size(); i++) {
			int ihead = heads.get(i);
			if (ihead == -1) continue;
			int iSmallIndex = Math.min(i, ihead);
			int iLargeIndex = Math.max(i, ihead);
			for (int j = 0; j < heads.size(); j++) {
				int jhead = heads.get(j);
				if (i==j || jhead == -1) continue;
				int jSmallIndex = Math.min(j, jhead);
				int jLargeIndex = Math.max(j, jhead);
				if(iSmallIndex < jSmallIndex && iLargeIndex < jLargeIndex && jSmallIndex < iLargeIndex) return false;
				if(iSmallIndex > jSmallIndex && jLargeIndex > iSmallIndex && iLargeIndex > jLargeIndex) return false;
			}
		}
		return true;
	}

	public static boolean checkIsTree(List<Integer> heads) {
		HashMap<Integer, List<Integer>> tree = new HashMap<Integer, List<Integer>>();
		for (int i = 0; i < heads.size(); i++) {
			int ihead = heads.get(i);
			if (ihead == -1) continue;
			if (tree.containsKey(ihead)) {
				tree.get(ihead).add(i);
			} else {
				List<Integer> children = new ArrayList<>();
				children.add(i);
				tree.put(ihead, children);
			}
		}
		boolean[] visited = new boolean[heads.size()];
		Arrays.fill(visited, false);
		visited[0] = true;
		traverse(visited, 0, tree);
		for(int i = 0; i < visited.length; i++)
			if (!visited[i])
				return false;
		return true;
	}
	
	
	private static void traverse(boolean[] visited, int parent, HashMap<Integer, List<Integer>> tree) {
		if (tree.containsKey(parent)) {
			for(int child: tree.get(parent)) {
				visited[child] = true;
				traverse(visited, child, tree);
			}
		}
		
	}
	
	/**
	 * Count the number of entities (all categories) 
	 * @param insts
	 * @param entities
	 * @return
	 */
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
	
	
	/**
	 * Similar to check Joint method, but only check whether its incomplete return a boolean value
	 * @param sent: just check for one sentence.
	 * @param tags
	 */
	public static ArrayList<EntitySpan> checkAllIncomplete(Sentence sent){
		int start = 0; int end = -1;
		ArrayList<EntitySpan> elist = new ArrayList<EntitySpan>();
		String prevEntity = "";
		for(int i=1;i<sent.length();i++){
			String e = sent.get(i).getEntity();
			e = e.equals("O")? e: e.substring(2);
			prevEntity = sent.get(i-1).getEntity();
			prevEntity = prevEntity.equals("O")? prevEntity:prevEntity.substring(2);
			//Need to fix the case of continuous two entity, start:inclusive, end:exclusive..Fixed by the reader already
			if(!e.equals(prevEntity)){
				if(!e.equals("O")){
					if(!prevEntity.equals("O")){
						end = i;
						if(notIncomplete(sent,start,end))
							elist.add(new EntitySpan(prevEntity,start,end-1));
					}
					start = i;
				}else{
					end = i;
					if(notIncomplete(sent,start,end)){
						elist.add(new EntitySpan(prevEntity,start,end-1));
					}
				}
			}
		}
		String lastE = sent.get(sent.length()-1).getEntity();
		lastE = lastE.equals("O")? lastE: lastE.substring(2);
		if(!lastE.equals("O")){
			end = sent.length();
			if(notIncomplete(sent,start,end))
				elist.add(new EntitySpan(lastE,start,end-1));
		}
		return elist;
	}
	
	/**
	 * check this entity is incomplete or not
	 * @param sent: sentence
	 * @param start: inclusive
	 * @param end: exlucsive
	 * @return
	 */
	private static boolean notIncomplete(Sentence sent, int start, int end){
//		if(end>start+1){
//			for(int x=start;x<end;x++){
//				if(sent.get(x).getHeadIndex()>=start || sent.get(x).getHeadIndex()<end)
//					System.err.println(sent.get(x).getDepLabel());
//			}
//		}
		return (end>start+1)  && !(sent.get(start).getHeadIndex()==end-1 || sent.get(end-1).getHeadIndex()==start);
	}

	
	
	public static void checkIncompeteSideType(DependInstance[] insts){
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		int numIsE = 0;
		int total  = 0;
		for(DependInstance inst:insts){
			ArrayList<UnnamedDependency> deps = inst.toDependencies(inst.getOutput());
			for(UnnamedDependency dep: deps){
				CoreLabel headLabel = (CoreLabel)dep.governor();
				CoreLabel moLabel = (CoreLabel)dep.dependent();
				String head = headLabel.ner().length()>2? headLabel.ner().substring(2):headLabel.ner();
				String mo = moLabel.ner().length()>2?moLabel.ner().substring(2):moLabel.ner();
				String x = head+","+mo;
				if(moLabel.ner().startsWith("B-")) total++;
				if(head.equals(mo) && !head.equals("O") ){
					for(int i= headLabel.sentIndex()+1;i<moLabel.sentIndex();i++){
						String e = inst.getInput().get(i).getEntity();
						e = e.length()>2?e.substring(2):e;
						if(!e.equals(head)){
							numIsE++;
//							System.err.println(headLabel.sentIndex());
//							System.err.println(moLabel.sentIndex());
//							System.err.println(inst.getInput().toString());
							System.err.println(inst.getInstanceId());
							break;
						}
					}
				}
				if(map.containsKey(x)){
					int num = map.get(x);
					map.put(x, num+1);
				}else{
					map.put(x, 1);
				}
			}
		}
		System.err.println(map.toString());
		System.err.println(numIsE +",total:"+total);
	}


}

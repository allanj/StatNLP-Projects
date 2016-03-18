/** Statistical Natural Language Processing System
    Copyright (C) 2014-2015  Lu, Wei

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * 
 */
package com.statnlp.sp.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import com.statnlp.commons.ml.clustering.KMeans;
import com.statnlp.commons.types.VectorWord;

/**
 * @author wei_lu
 *
 */
public class Word2VecReader {
	
	public static void main(String args[])throws IOException{
		
//		String filename = "data/geoquery/geo-word2vec-glove-Wiki+GigaWord";
//		String filename = "data/geoquery/geo-word2vec-glove-common-crawl-840B";
		String filename = "data/geoquery/geo-word2vec-glove-common-crawl-42B";
		ArrayList<VectorWord> vwords = toVectorWords(filename);
		
		VectorWord[] pts = new VectorWord[vwords.size()];
		for(int k = 0; k<vwords.size(); k++){
			VectorWord vword = vwords.get(k);
			pts[k] = vword;
		}
		vwords = null;
		
		int numClusters = 20;
		int numIterations = 100;
		KMeans km = new KMeans(pts, numClusters);
		km.run(numIterations);
		
		km.viewCluster(1);
		
//		System.err.println(vwords.size());
//		for(int k = 0; k<vwords.size(); k++){
//			VectorWord vword1 = vwords.get(k);
//			vword1.expNorm();
//		}
//		
//		for(int k = 0; k<vwords.size(); k++){
//			VectorWord vword1 = vwords.get(k);
//			vword1.getNewVec();
//		}
//		
//		for(int k = 0; k<vwords.size(); k++){
//			VectorWord vword1 = vwords.get(k);
////			System.err.println(vword1);
//			for(int i = k+1; i<vwords.size(); i++){
//				VectorWord vword2 = vwords.get(i);
//				double sim = vword1.sim(vword2);
//				System.err.println("sim("+vword1.getWord()+","+vword2.getWord()+")="+sim);
//				
////				double offset = -3.8205;
////				vword1.addOffset(offset);
////				vword2.addOffset(offset);
//				
//				double newSim = vword1.newSim(vword2);
//				System.err.println("newSim("+vword1.getWord()+","+vword2.getWord()+")="+newSim);
//				System.err.println(Arrays.toString(vword1.getNewVec()));
//				System.err.println(Arrays.toString(vword2.getNewVec()));
////				vword1.addOffset(-offset);
////				vword2.addOffset(-offset);
//				System.err.println();
//				if(sim>0.9){
//					System.exit(1);
//				}
//			}
//		}
		
	}
	
	private static ArrayList<VectorWord> toVectorWords(String filename) throws FileNotFoundException{
		ArrayList<VectorWord> vecwords = new ArrayList<VectorWord>();
		Scanner scan = new Scanner(new File(filename));
		while(scan.hasNextLine()){
			String line = scan.nextLine().trim();
			if(line.equals("")) continue;
			int bIndex = line.indexOf("[");
			int eIndex = line.indexOf("]");
			String word = line.substring(0, bIndex).trim();
			String[] nums = line.substring(bIndex+1, eIndex).split("\\s");
			double[] vec = new double[nums.length];
			for(int k = 0; k<vec.length; k++){
				vec[k] = Double.parseDouble(nums[k]);
			}
			VectorWord vectorword = new VectorWord(word, vec);
			vecwords.add(vectorword);
//			System.err.println(line);
		}
		scan.close();
		return vecwords;
	}
	
}

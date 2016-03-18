/** Statistical Natural Language Processing System
    Copyright (C) 2014  Lu, Wei

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
package com.statnlp.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.statnlp.commons.BitextInstance;
import com.statnlp.commons.Sentence;
import com.statnlp.commons.Word;

public class BitextReader {
	
	public static void main(String args[])throws IOException{
		ArrayList<BitextInstance> instances = BitextReader.read("data/IWSLT/CE/train/train.zh", "data/IWSLT/CE/train/train.en", -1, 20);
		System.err.println(instances.size());
	}
	
	public static ArrayList<BitextInstance> read(String srcFilename, String tgtFilename, int firstNInstances, int maxSentLength) throws IOException{
		
		ArrayList<BitextInstance> instances = new ArrayList<BitextInstance>();
		
		InputStreamReader in;
		BufferedReader scan;
		String line;

		in = new InputStreamReader(new FileInputStream(srcFilename), "UTF-8");
		scan = new BufferedReader(in);
		
		ArrayList<String> srcLines = new ArrayList<String>();
		line = scan.readLine();
		while(line!=null){
			srcLines.add(line);
			line = scan.readLine();
		}
		in.close();
		
		in = new InputStreamReader(new FileInputStream(tgtFilename), "UTF-8");
		scan = new BufferedReader(in);
		
		ArrayList<String> tgtLines = new ArrayList<String>();
		line = scan.readLine();
		while(line!=null){
			tgtLines.add(line);
			line = scan.readLine();
		}
		in.close();
		
		if(srcLines.size()!=tgtLines.size()){
			throw new RuntimeException("Oh, srcLines has "+srcLines.size()+";tgtLines has "+tgtLines.size());
		}
		
		double limit;
		if(firstNInstances==-1){
			limit = srcLines.size();
		} else {
			limit = Math.min(firstNInstances, srcLines.size());
		}
		
		if(maxSentLength == -1)
			maxSentLength = Integer.MAX_VALUE;
		
		for(int k = 0; k<limit; k++){
			Word[] srcWords = Word.toWords_forHALIGN(srcLines.get(k));
			Word[] tgtWords = Word.toWords_forHALIGN(tgtLines.get(k));
			if(srcWords.length <= maxSentLength && tgtWords.length <= maxSentLength){
				Sentence srcSentence = new Sentence("zh", srcWords);
				Sentence tgtSentence = new Sentence("en", tgtWords);
				BitextInstance inst = new BitextInstance(k, srcSentence, tgtSentence);
				instances.add(inst);
			}
		}
		
		return instances;
		
	}
	
}

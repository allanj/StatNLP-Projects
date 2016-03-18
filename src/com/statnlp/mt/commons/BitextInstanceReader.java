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
package com.statnlp.mt.commons;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public class BitextInstanceReader {
	
	private HashMap<SrcWord, SrcWord> srcMap;
	private HashMap<TgtWord, TgtWord> tgtMap;
	private ArrayList<SrcWord> srcList;
	private ArrayList<TgtWord> tgtList;
	
	public BitextInstanceReader(){
		this.srcMap = new HashMap<SrcWord, SrcWord>();
		this.tgtMap = new HashMap<TgtWord, TgtWord>();
		this.srcList = new ArrayList<SrcWord>();
		this.tgtList = new ArrayList<TgtWord>();
	}
	
	public BitextInstance[] readBitext(String src_filename, String tgt_filename, int numInstances) throws IOException{
		
		InputStreamReader in_src = new InputStreamReader(new FileInputStream(src_filename), "UTF-8");
		BufferedReader scan_src = new BufferedReader(in_src);
		
		InputStreamReader in_tgt = new InputStreamReader(new FileInputStream(tgt_filename), "UTF-8");
		BufferedReader scan_tgt = new BufferedReader(in_tgt);
		
		ArrayList<BitextInstance> instances = new ArrayList<BitextInstance>();
		
		int id = 1;
		String line_src;
		String line_tgt;
		while((line_src=scan_src.readLine())!=null){
			line_tgt = scan_tgt.readLine();
			
			String[] words_src = line_src.trim().split("\\s");
			String[] words_tgt = line_tgt.trim().split("\\s");
			
			SrcWord[] srcWords = new SrcWord[words_src.length];
			TgtWord[] tgtWords = new TgtWord[words_tgt.length];
			for(int k =0; k<words_src.length; k++){
				srcWords[k] = this.toSrcWord(words_src[k]);
			}
			for(int k =0; k<tgtWords.length; k++){
				tgtWords[k] = this.toTgtWord(words_tgt[k]);
			}
			
			BitextInstance inst = new BitextInstance(id++, 1.0, srcWords, tgtWords);
			instances.add(inst);
		}
		scan_src.close();
		scan_tgt.close();
		
		int size = numInstances < 0 ? instances.size() : numInstances < instances.size() ? numInstances : instances.size();
		
		BitextInstance[] insts = new BitextInstance[size];
		for(int k = 0; k<insts.length; k++){
			insts[k] = instances.get(k);
		}
		return insts;

	}
	
	private SrcWord toSrcWord(String s){
		SrcWord w = new SrcWord(s);
		if(this.srcMap.containsKey(w)){
			return this.srcMap.get(w);
		} else {
			w.setId(this.srcList.size());
			this.srcList.add(w);
			this.srcMap.put(w, w);
			return w;
		}
	}

	private TgtWord toTgtWord(String s){
		TgtWord w = new TgtWord(s);
		if(this.tgtMap.containsKey(w)){
			return this.tgtMap.get(w);
		} else {
			w.setId(this.tgtList.size());
			this.tgtList.add(w);
			this.tgtMap.put(w, w);
			return w;
		}
	}
}

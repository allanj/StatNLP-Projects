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
package com.statnlp.topic.commons;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public class TopicInstanceReader {
	
	private HashMap<TopicWordToken, TopicWordToken> wordMap;
	private ArrayList<TopicWordToken> wordList;
	
	public TopicInstanceReader(){
		this.wordMap = new HashMap<TopicWordToken, TopicWordToken>();
		this.wordList = new ArrayList<TopicWordToken>();
	}
	
	public TopicInstance[] readInstances(String filename) throws IOException{
		
		InputStreamReader in = new InputStreamReader(new FileInputStream(filename), "UTF-8");
		BufferedReader scan = new BufferedReader(in);
		
		ArrayList<TopicInstance> instances = new ArrayList<TopicInstance>();
		
		int id = 1;
		String line;
		while((line=scan.readLine())!=null){
			String[] words_src = line.trim().split("\\s");
			
			TopicWordToken[] srcWords = new TopicWordToken[words_src.length];
			for(int k =0; k<srcWords.length; k++){
				srcWords[k] = this.toTopicWord(words_src[k]);
			}
			
			TopicInstance inst = new TopicInstance(id++, 1.0, srcWords);
			instances.add(inst);
		}
		scan.close();
		
		TopicInstance[] insts = new TopicInstance[instances.size()];
		for(int k = 0; k<insts.length; k++){
			insts[k] = instances.get(k);
		}
		return insts;

	}
	
	private TopicWordToken toTopicWord(String s){
		TopicWordToken w = new TopicWordToken(s);
		if(this.wordMap.containsKey(w)){
			return this.wordMap.get(w);
		} else {
			w.setId(this.wordList.size());
			this.wordList.add(w);
			this.wordMap.put(w, w);
			return w;
		}
	}
	
}
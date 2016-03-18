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
package com.statnlp.cws;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

import com.statnlp.commons.types.Segment;
import com.statnlp.commons.types.WordToken;

/**
 * @author wei_lu
 *
 */
public class CWSReader {
	
	private CWSOutputTokenSet _outputSet;
	
	private HashMap<Integer, ArrayList<CWSOutputTokenList>> _lists;
	
	private HashMap<String, WordToken> _str2token;
	
	public static void main(String args[])throws IOException{
		
		String filename_train = "data/ctb5/ctb5.train";
		String filename_test = "data/ctb5/ctb5.test";
		
		CWSReader reader = new CWSReader();
		
		CWSInstance[] insts_train = reader.readTrain(filename_train);
		System.err.println(reader._str2token.size());
		CWSInstance[] insts_test = reader.readTest(filename_test);
		System.err.println(reader._str2token.size());
		
//		for(int k = 0; k<insts.size(); k++){
//			CWSInstance inst = insts.get(k);
//			System.err.println(inst.getInstanceId()+"\t"+Arrays.toString(inst.getInput()));
//			System.err.println(inst.getInstanceId()+"\t"+inst.size());
//			System.err.println(inst.getInstanceId()+"\t"+inst.getOutput());
//		}
//		
//		System.err.println(reader._lists.get(1).size());
//		System.err.println(reader._lists.get(2).size());
//		System.err.println(reader._lists.get(3).size());
		
	}
	
	public ArrayList<CWSOutputTokenList> getLists(int gram){
		return this._lists.get(gram);
	}
	
	public CWSReader(){
		this._outputSet = new CWSOutputTokenSet();
		this._str2token = new HashMap<String, WordToken>();
		CWSOutputToken._START = this._outputSet.toOutputToken("<START>");
	}
	
	public CWSInstance[] readTrain(String filename) throws FileNotFoundException{
		CWSInstance[] insts =  readData(filename);
		if(CWSNetworkConfig.CONSIDER_ONLY_PATTERNS_IN_THE_TRAINING_SET){
			this.recordLists(insts);
		}
		return insts;
	}

	public CWSOutputTokenSet getOutputTokenSet(){
		return this._outputSet;
	}
	
	private void recordLists(CWSInstance[] insts){
		this._lists = new HashMap<Integer, ArrayList<CWSOutputTokenList>>();
		
		for(int n = 1; n<=CWSNetworkConfig.TAG_GRAM; n++){
			
			this._lists.put(n, new ArrayList<CWSOutputTokenList>());
			
			for(int k = 0; k<insts.length; k++){
				CWSInstance inst = insts[k];
				CWSOutput output = inst.getOutput();
				ArrayList<Segment> segments = output.getSegments();
				
				ArrayList<CWSOutputToken> tags = new ArrayList<CWSOutputToken>();
				
				for(int j = 0; j<n; j++){
					tags.add(CWSOutputToken._START);
				}

				for(int i = 0; i<segments.size(); i++){
					Segment segment = segments.get(i);
					CWSOutputToken tag = output.getOutputBySegment(segment);
					@SuppressWarnings("unchecked")
					ArrayList<CWSOutputToken> tags_tmp = (ArrayList<CWSOutputToken>)tags.clone();
					tags_tmp.add(tag);
					tags_tmp.remove(0);
					CWSOutputTokenList list = new CWSOutputTokenList(tags_tmp);
					ArrayList<CWSOutputTokenList> lists = this._lists.get(n);
					if(!lists.contains(list)){
						lists.add(list);
					}
					tags = tags_tmp;
				}
			}
		}
		
	}
	
	public CWSInstance[] readTest(String filename) throws FileNotFoundException{
		CWSInstance[] insts = readData(filename);
		for(CWSInstance inst : insts){
			inst.removeOutput();
		}
		return insts;
	}
	
	private CWSInstance[] readData(String filename) throws FileNotFoundException{
		
		ArrayList<CWSInstance> insts = new ArrayList<CWSInstance>();
		Scanner scan = new Scanner(new File(filename));
		ArrayList<String[]> tokens = new ArrayList<String[]>();
		int id = 1;
		while(scan.hasNextLine()){
			String line = scan.nextLine().trim();
			if(line.equals("")){
				if(tokens.size()!=0){
					CWSInstance inst = toInstance(id++, tokens);
					insts.add(inst);
					tokens = new ArrayList<String[]>();
				}
			} else {
				String[] token = line.split("\\s");
				tokens.add(token);
			}
		}
		scan.close();
		
		if(tokens.size()!=0){
			CWSInstance inst = toInstance(id, tokens);
			insts.add(inst);
		}
		
		CWSInstance[] insts_r = new CWSInstance[insts.size()];
		for(int k = 0; k<insts.size(); k++){
			insts_r[k] = insts.get(k);
		}
		
		return insts_r;
	}
	
	private WordToken toWordToken(String word){
		if(this._str2token.containsKey(word)){
			return this._str2token.get(word);
		}
		WordToken wToken = new WordToken(word);
		wToken.setId(this._str2token.size());
		this._str2token.put(word, wToken);
		return wToken;
	}

	private CWSInstance toInstance(int id, ArrayList<String[]> tokens){
		WordToken[] inputs = new WordToken[tokens.size()];
		CWSOutput output = new CWSOutput();
		int bIndex = -1;
		int eIndex = -1;
		CWSOutputToken outputToken = null;
		for(int k = 0; k<inputs.length; k++){
			inputs[k] = this.toWordToken(tokens.get(k)[0]);
			String l = tokens.get(k)[1];
			if(l.startsWith("U-")){
				bIndex = k; eIndex=k+1;
				outputToken = this._outputSet.toOutputToken(l.split("\\-")[1]);
				output.addOutput(new Segment(bIndex, eIndex), outputToken); 
				bIndex = k+1;
			}
			else if(l.startsWith("B-")){
				bIndex = k;
			}
			else if(l.startsWith("L-")){
				eIndex = k+1;
				
				if(eIndex-bIndex<=CWSNetworkConfig.MAX_WORD_LEN){
					outputToken = this._outputSet.toOutputToken(l.split("\\-")[1]);
					output.addOutput(new Segment(bIndex, eIndex), outputToken); 
					bIndex = k+1;
				} else {
					if(CWSNetworkConfig.LONG_WORD_BLIOU){
						outputToken = this._outputSet.toOutputToken("B-"+l.split("\\-")[1]);
						output.addOutput(new Segment(bIndex, bIndex+1), outputToken);
						
						outputToken = this._outputSet.toOutputToken("I-"+l.split("\\-")[1]);
						for(int cIndex = bIndex+1; cIndex<eIndex-1; cIndex++){
							output.addOutput(new Segment(cIndex, cIndex+1), outputToken);
						}
						
						outputToken = this._outputSet.toOutputToken("L-"+l.split("\\-")[1]);
						output.addOutput(new Segment(eIndex-1, eIndex), outputToken);
						
						bIndex = k+1;
					} else {
						outputToken = this._outputSet.toOutputToken("U-"+l.split("\\-")[1]);
						for(int cIndex = bIndex; cIndex<=eIndex-1; cIndex++){
							output.addOutput(new Segment(cIndex, cIndex+1), outputToken); 
							bIndex = k+1;
						}
					}
				}
			}
		}
		CWSInstance inst = new CWSInstance(id, 1.0, inputs, output);
		return inst;
	}
	
}

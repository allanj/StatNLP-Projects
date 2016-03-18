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
package com.statnlp.chinese.preprocess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;

public class CTB5ToColumnFormat {
	
	public static void main(String args[]) throws IOException{
		
		BufferedWriter p_train = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("data/ctb5.train"), "UTF-8"));
		BufferedWriter p_dev = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("data/ctb5.dev"), "UTF-8"));
		BufferedWriter p_test = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("data/ctb5.test"), "UTF-8"));
		
		out = new PrintWriter(new OutputStreamWriter(System.out, "UTF8"));
		
		String p_name = "xx";
		
		String foldername = "data/ctb5/";
		File f = new File(foldername);
		String[] filenames = f.list();
		for(String filename : filenames){
			int bIndex = filename.indexOf("_");
			int eIndex = filename.indexOf(".");
			int id = Integer.parseInt(filename.substring(bIndex+1,eIndex));
			BufferedWriter p;
			if(1<=id && id<=270){
				p = p_train;
				p_name = "train";
			} else if(400<=id && id<=1151){
				p = p_train;
				p_name = "train";
			} else if(301<=id && id<=325){
				p = p_dev;
				p_name = "dev";
			} else {
				p = p_test;
				p_name = "test";
			}
			String form = toColumnFormat(foldername+filename, p_name);
			p.write(form);
			p.write('\n');
			p.flush();
		}
		p_train.close();
		p_dev.close();
		p_test.close();
		
		
		System.err.println(Arrays.toString(sents_count));
		System.err.println(Arrays.toString(words_count));
		System.err.println(_maxWordLen);
		System.err.println(_cLongerThanSix);
		System.err.println(Arrays.toString(_lenCount));
		double sum = 0;
		for(int k = 1; k<_lenCount.length; k++){
			int c = _lenCount[k];
			sum += c;
		}
		for(int k = 1; k<_lenCount.length; k++){
			int c = _lenCount[k];
			System.err.println(k+"\t"+c/sum);
		}
	}
	
	private static int[] sents_count = new int[3];
	private static int[] words_count = new int[3];
	
	private static int _maxWordLen = -1;
	private static int _cLongerThanSix = 0;
	
	private static PrintWriter out;
	
	private static int _lenCount[] = new int[25];

	private static String toColumnFormat(String filename, String p_name) throws IOException{
		
		int index0 = -1;
		if(p_name.equals("train")){
			index0 = 0;
		} else if(p_name.equals("dev")){
			index0 = 1;
		} else {
			index0 = 2;
		}
		
		StringBuilder sb = new StringBuilder();
		
//		Scanner scan = new Scanner(new File(filename));
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF8"));
		
		String line;
		while((line=in.readLine())!=null){
			line = line.trim();
			if(line.startsWith("<")){
				if(!line.endsWith(">")){
					throw new RuntimeException("line:"+line);
				}
			} else {
				sents_count[index0] ++;
				String[] tokens = line.split("\\s");
				
//				words_count[index0] += tokens.length;
				for(String token : tokens){
					int index = token.indexOf("_");
					String word = token.substring(0, index);
					String tag = token.substring(index+1);
					
					if(word.toCharArray().length > 0){
						if(word.toCharArray().length > _maxWordLen){
							_maxWordLen = word.length();
						}
//						System.err.println(word+"\t"+word.toCharArray().length);
						if(word.toCharArray().length==2){
							out.println("::"+word+"\t"+word.toCharArray().length);
							out.flush();
						}
						_cLongerThanSix++;
					}
					
					_lenCount[word.toCharArray().length]++;
					
					if(tag.equals("-NONE-")){
					} else {
						words_count[index0]++;
						char[] chars = word.toCharArray();
						if(chars.length==1){
							sb.append(word+"\tU-"+tag);
							sb.append('\n');
						} else {
							for(int k = 0; k<chars.length; k++){
								if(k==0){
									sb.append(chars[k]+"\tB-"+tag);
								} else if(k==chars.length-1){
									sb.append(chars[k]+"\tL-"+tag);
								} else{
									sb.append(chars[k]+"\tI-"+tag);
								}
								sb.append('\n');
							}
						}
//						sb.append(word+"\t"+tag);
//						sb.append('\n');
					}
				}
				sb.append('\n');
			}
		}
		
		return sb.toString();
	}
}

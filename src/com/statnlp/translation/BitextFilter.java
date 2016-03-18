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
package com.statnlp.translation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class BitextFilter {
	
	public static void main(String args[]) throws IOException{
		
		int WORD_LIMIT = 25;
		
		String src_old = "data/IWSLT/CE/train/train.zh";
		String tgt_old = "data/IWSLT/CE/train/train.en";
		
		BufferedReader in_src = new BufferedReader(new InputStreamReader(new FileInputStream(src_old), "UTF-8"));
		BufferedReader in_tgt = new BufferedReader(new InputStreamReader(new FileInputStream(tgt_old), "UTF-8"));
		
		File f = new File("data/IWSLT/CE/train-"+WORD_LIMIT+"/");
		f.mkdirs();
		
		String src_new = "data/IWSLT/CE/train-"+WORD_LIMIT+"/"+"train.zh";
		String tgt_new = "data/IWSLT/CE/train-"+WORD_LIMIT+"/"+"train.en";
		
		BufferedWriter out_src = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(src_new), "UTF-8"));
		BufferedWriter out_tgt = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tgt_new), "UTF-8"));
		
		String line_src;
		String line_tgt;
		
		int num_lines = 0;
		int num_lines_selected = 0;
		
		while((line_src=in_src.readLine())!=null){
			num_lines++;
			line_tgt = in_tgt.readLine();
			String[] tokens_src = line_src.split("\\s");
			String[] tokens_tgt = line_tgt.split("\\s");
			if(tokens_src.length <= WORD_LIMIT && tokens_tgt.length <= WORD_LIMIT){
				num_lines_selected ++;
				out_src.write(line_src);
				out_src.write('\n');
				out_src.flush();
				out_tgt.write(line_tgt);
				out_tgt.write('\n');
				out_tgt.flush();
			}
		}
		out_src.close();
		out_tgt.close();
		in_src.close();
		in_tgt.close();
		
		System.err.println("Done:"+num_lines_selected+"/"+num_lines+"="+((double)num_lines_selected/num_lines));
	}

}

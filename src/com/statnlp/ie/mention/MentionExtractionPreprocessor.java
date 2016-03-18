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
package com.statnlp.ie.mention;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class MentionExtractionPreprocessor {
	
	public static void main(String args[])throws IOException{
		
		String dirname;
		
		dirname = "data/ACE2004_all/";
		processDir(dirname);
		dirname = "data/ACE2005_all/";
		processDir(dirname);
		
//		dirname = "data/ACE2005/nw/fp1/";
//		processDir(dirname);
//		dirname = "data/ACE2005/wl/fp1/";
//		processDir(dirname);
//		dirname = "data/ACE2005/un/fp1/";
//		processDir(dirname);
//		dirname = "data/ACE2005/cts/fp1/";
//		processDir(dirname);
//		dirname = "data/ace2005_normalized/nw/";
//		processDir(dirname);
//		dirname = "data/ace2005_normalized/bc/";
//		processDir(dirname);
		
		System.err.println("Done!");
		
	}
	
	private static void processDir(String dirname) throws FileNotFoundException{
		
		PrintWriter p1 = new PrintWriter(new File(dirname+"train.txt"));
		PrintWriter p2 = new PrintWriter(new File(dirname+"test.txt"));
		
		Random rand = new Random(1234);
		File f = new File(dirname);
		String[] file_names = f.list();
		ArrayList<String> files = new ArrayList<String>();
		for(String file_name : file_names){
			if(file_name.endsWith(".sgm"))
				files.add(file_name);
		}
		Collections.sort(files);
		for(int k = 0; k<files.size(); k++){
			String file = files.get(k);
			if(rand.nextDouble()<0.7){
				p1.println(file);
				p1.flush();
			} else {
				p2.println(file);
				p2.flush();
			}
		}
		p1.close();
		p2.close();
		
	}

}

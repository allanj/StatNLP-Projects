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
package com.statnlp.ie.io;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.StringTokenizer;

public class DataReader_ProcessCONLL2003_FromColumnFormat {
	
	public static void main(String args[])throws IOException{
		
		String sets[] = new String[]{"train", "dev", "test"};
		
		for(String set : sets){
			
			String filename_in = "data/CONLL2003/CONLL2003-"+set.toUpperCase()+".txt";
			String filename_out = "data/CONLL2003/"+set+".data";
			
			Scanner scan = new Scanner(new File(filename_in));
			PrintWriter p = new PrintWriter(new File(filename_out));
			
			String line;
			
			int skipped = 0;
			
			boolean prev_empty = true;
			
			while(scan.hasNextLine()){
				line = scan.nextLine().trim();
				
				if(line.equals("O	0	0	O	-X-	-DOCSTART-	x	x	0")){
//					System.err.println("Skipped..");
					skipped++;
				}
				else if(line.equals("O	0	0	-X-	-X-	-DOCSTART-	x	x	0")){
//					System.err.println("Skipped..");
					skipped++;
				}
				else if(line.equals("")){
//					System.err.println();
					if(!prev_empty){
						p.println();
						p.flush();
					}
					prev_empty = true;
				}
				else {
					StringTokenizer st = new StringTokenizer(line);
					String[] tokens = new String[st.countTokens()];
					int k = 0;
					while(st.hasMoreTokens()){
						tokens[k++] = st.nextToken();
					}
//					System.err.println(tokens[5]+"\t"+tokens[4]+"\t"+tokens[0]);
					p.println(tokens[5]+"\t"+tokens[4]+"|"+tokens[3]+"\t"+tokens[0]);
					p.flush();
					prev_empty = false;
				}
			}
			
			p.close();
			
			System.err.println("Skipped:"+skipped);
			
			
		}
		
	}

}

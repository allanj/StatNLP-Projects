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
package com.statnlp.ie;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;


public class Survey {
	
	public static void main(String args[])throws IOException{
		Scanner scan = new Scanner(new File("data/ace.template"));
		
		HashSet<String> all_tokens = new HashSet<String>();
		while(scan.hasNextLine()){
			String line = scan.nextLine();
			System.err.println(line);
			String[] segs = line.split("\\s");
			for(int k = 0; k<segs.length; k++){
				String seg = segs[k];
				if(seg.equals("{") || seg.equals("}")){
				} else {
					int index = seg.indexOf("[");
					if(index==-1){
						all_tokens.add(seg);
					} else {
						String token = seg.substring(0, index);
						if(token.startsWith("Time")){
							all_tokens.add("Time");
						} else {
							all_tokens.add(token);
						}
					}
				}
			}
		}
		System.err.println(all_tokens.size());
	}

}

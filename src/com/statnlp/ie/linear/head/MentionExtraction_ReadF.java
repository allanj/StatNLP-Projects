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
package com.statnlp.ie.linear.head;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * @author wei_lu
 *
 */
public class MentionExtraction_ReadF {
	
	public static void main(String args[]) throws FileNotFoundException{
		
		double max_f = Double.NEGATIVE_INFINITY;
		Scanner scan = new Scanner(new File(args[0]));
		while(scan.hasNextLine()){
			String line = scan.nextLine();
			if(line.equals("-FULL-")){
				line = scan.nextLine();
				String[] scores = line.split("\\s");
				String[] f_str = scores[2].split(":");
				double f = Double.parseDouble(f_str[1]);
				if(f>max_f){
					max_f = f;
				}
			}
		}
		
		System.err.println(max_f);
		
		scan.close();
		
	}
	
}

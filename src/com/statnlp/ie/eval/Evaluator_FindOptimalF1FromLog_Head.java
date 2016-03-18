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
package com.statnlp.ie.eval;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class Evaluator_FindOptimalF1FromLog_Head {
	
	public static void main(String args[])throws IOException{
		
//		String filename = "results/ace2004-head.log";
		String filename = "logs/2004-head.log";
		
		Scanner scan = new Scanner(new File(filename));
		
		while(scan.hasNextLine()){
			String line = scan.nextLine();
			if(line.startsWith("lambda=")){
				break;
			}
		}
		
		double dev_f_best = -1;
		double dev_p_best = -1;
		double dev_r_best = -1;
		double test_f_best = -1;
		double test_p_best = -1;
		double test_r_best = -1;
		
		String line;
		while(scan.hasNextLine()){
			line = scan.nextLine();
			while(!line.equals("-FULL-")){
				line = scan.nextLine();
			}
			double dev_p = Double.parseDouble(scan.nextLine().substring(2));
			double dev_r = Double.parseDouble(scan.nextLine().substring(2));
			double dev_f = Double.parseDouble(scan.nextLine().substring(2));
			
			line = scan.nextLine();
			while(!line.equals("-FULL-")){
				line = scan.nextLine();
			}
			double test_p = Double.parseDouble(scan.nextLine().substring(2));
			double test_r = Double.parseDouble(scan.nextLine().substring(2));
			double test_f = Double.parseDouble(scan.nextLine().substring(2));
			
//			System.err.println("DEV:"+dev_p+"/"+dev_r+"/"+dev_f);
//			System.err.println("TEST:"+test_p+"/"+test_r+"/"+test_f);
			
			if(dev_f>dev_f_best){
				dev_f_best = dev_f;
				dev_p_best = dev_p;
				dev_r_best = dev_r;
				test_f_best = test_f;
				test_p_best = test_p;
				test_r_best = test_r;
			}
			
			line = scan.nextLine();
			
			System.err.println("dev:\n"+dev_p_best+"\n"+dev_r_best+"\n"+dev_f_best);
			System.err.println("test:\n"+test_p_best+"\n"+test_r_best+"\n"+test_f_best);
			System.err.println();
		}
		
		
	}

}

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

import java.io.IOException;

import com.statnlp.commons.WordUtil;

public class PreprocessData_CONLL2003 {
	
	public static void main(String args[])throws IOException{
		
		String input = "SINGAPORE 1996-08-29";
		
		String output = preprocess(input);
		
		System.err.println(output);
		
	}
	
	private static String preprocess(String line){
		
		StringBuilder sb = new StringBuilder();
		
		String[] tokens = line.split("\\s");
		for(int k = 0; k<tokens.length; k++){
			if(k!=0)
				sb.append(' ');
			String word = tokens[k];
			if(WordUtil.isMonth(word)){
				sb.append("*MONTH*");
			} else {
				sb.append(WordUtil.normalizeDigits(word));
			}
		}
		
		return sb.toString();
	}

}

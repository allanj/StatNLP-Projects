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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

//author: luwei. 2014.15.08 19:51. v1.0 

public class ExtractInputFeaturesBasedOnTemplate {
	
	public static void main(String args[])throws IOException{
		
		String template_filename = "temp";
		String data_filename = "dat";
		
		ExtractInputFeaturesBasedOnTemplate extractor = new ExtractInputFeaturesBasedOnTemplate();
		
		ArrayList<String> templates = extractor.readTemplates(template_filename);
		ArrayList<String[][]> dataList = extractor.readData(data_filename);
		
		System.err.println(dataList.size()+" sentences");
		
		HashMap<String, Integer> feature2id = new HashMap<String, Integer>();
		
		for(String[][] data : dataList){
			ArrayList<String> features = extractor.extract(data, templates);
			
			//here, this is what we want, the ids of the features associated with each instance.
			//where each instance is a position/row entry in the column data format.
			ArrayList<Integer> featureIds = new ArrayList<Integer>(); 
			for(String feature : features){
				if(feature2id.containsKey(feature)){
					featureIds.add(feature2id.get(feature));
				} else {
					int id = feature2id.size();
					featureIds.add(id);
					feature2id.put(feature, id);
				}
			}
			System.err.println(featureIds.toString());
		}
		
	}
	
	
	public ArrayList<String> readTemplates(String filename) throws FileNotFoundException{
		
		ArrayList<String> templates = new ArrayList<String>();
		
		Scanner scan = new Scanner(new File(filename));
		while(scan.hasNextLine()){
			String line = scan.nextLine().trim();
			if(line.startsWith("#") || line.equals("")){
			} else {
				templates.add(line);
			}
		}
		
		return templates;
		
	}
	
	//ok, make sure the last line of the data file is a blank line.
	public ArrayList<String[][]> readData(String filename) throws FileNotFoundException{
		
		ArrayList<String[][]> results = new ArrayList<String[][]>();
		Scanner scan = new Scanner(new File(filename));
		ArrayList<String> lines = new ArrayList<String>();
		
		while(scan.hasNextLine()){
			String line = scan.nextLine().trim();
			if(!line.equals("")){
				lines.add(line);
			} else {
				
				String[][] data = new String[lines.size()][];
				for(int k = 0; k<data.length; k++){
					data[k] = lines.get(k).split("\\s");
				}
				results.add(data);
				lines = new ArrayList<String>();
			}
		}
		
		return results;
		
	}
	
	private ArrayList<String> extract(String[][] data, ArrayList<String> templates){
		
		ArrayList<String> fs = new ArrayList<String>();
		for(String template : templates){
			ArrayList<String> v = this.extract(data, template);
			fs.addAll(v);
		}
		return fs;
		
	}
	
	public ArrayList<String> extract(String[][] data, String template){
		
		ArrayList<String> fs = new ArrayList<String>();
		for(int curr_row = 0; curr_row<data.length; curr_row++){
			String f = this.extract(data, template, curr_row);
			fs.add(f);
		}
		return fs;
		
	}
	
	
	//matrix: row - instance
	//matrix: col - type of input
	private String extract(String[][] data, String template, int curr_row){
		
		//U00:%x[-2,0]
		//U05:%x[-1,0]/%x[0,0]
		
		String[] f_t = template.split(":");
		String f_name = f_t[0];
		String[] f_values = f_t[1].split("\\/");
		
		String input_f = "";
		for(int k = 0; k<f_values.length; k++){
			String f_value = f_values[k];
//			String f_value : f_values;
			int bIndex = f_value.indexOf("[");
			int cIndex = f_value.indexOf(",",bIndex);
			int eIndex = f_value.lastIndexOf("]");
			
			int offset_col = Integer.parseInt(f_value.substring(bIndex+1,cIndex));
			int offset_row = Integer.parseInt(f_value.substring(cIndex+1,eIndex));
			
			if(k!=0){
				input_f += "/";
			}
			
			if(curr_row+offset_col>=0 && curr_row+offset_col<data.length){
				input_f += data[curr_row+offset_col][offset_row];
			} else {
				input_f += "";
			}
			
		}
		
		String f = f_name + ":" + input_f;
		
		return f;
		
	}
	
}
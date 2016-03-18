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
package com.statnlp.ie.event;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.StringTokenizer;

import com.statnlp.commons.AttributedWord;
import com.statnlp.commons.Word;
import com.statnlp.ie.types.IEManager;
import com.statnlp.ie.types.MentionType;

/**
 * @author wei_lu
 *
 */
public class EventReader {
	
	public static void main(String args[])throws IOException{
		
		String filename = "data/";
		
		IEManager manager = new IEManager();
		
		ArrayList<EventInstance> instances = readEvents(manager, filename);
		
	}
	
	public static ArrayList<EventInstance> readEvents(IEManager manager, String filename) throws FileNotFoundException{
		
		ArrayList<EventInstance> insts = new ArrayList<EventInstance>();
		
		Scanner scan = new Scanner(new File(filename));
		
		while(scan.hasNextLine()){
			
			String line;
			line = scan.nextLine();
			if(!line.startsWith("++++++")){
				throw new RuntimeException("Invalid format:"+line+". Expecting ++++++.");
			}
			String file_name = scan.nextLine();
			
			// --SENTENCES--
			line = scan.nextLine(); 
			if(!line.startsWith("--SENTENCE--")){
				throw new RuntimeException("Invalid format:"+line+". Expecting --SENTENCE--.");
			}
			line = scan.nextLine(); //String sent = line;
			String[] words = line.split("\\s");
			AttributedWord[] word_ids = new AttributedWord[words.length];
			for(int i = 0; i< words.length; i++){
				word_ids[i] = new AttributedWord(words[i]); //manager.toWord(words[i]);
			}
			
			// --EXPECTED EVENT--
			line = scan.nextLine(); 
			if(!line.startsWith("--EXPECTED EVENT--")){
				throw new RuntimeException("Invalid format:"+line+". Expecting --EXPECTED EVENT--.");
			}
			line = scan.nextLine();
			String[] event_types = line.split(":");
			Event event = manager.toEvent(event_types[0], event_types[1]);
			
			// --GOLD ANNOTATIONS--
			line = scan.nextLine();
			if(!line.startsWith("--GOLD ANNOTATIONS--")){
				throw new RuntimeException("Invalid format:"+line+". Expecting --GOLD ANNOTATIONS--.");
			}
			int num_gold_annotations = Integer.parseInt(scan.nextLine());
			for(int i = 0; i <num_gold_annotations; i++){
				String[] parts = scan.nextLine().split("\\s");
				String[] interval = parts[0].split(",");
				String[] arg_names = parts[1].split("\\|");
				int bIndex = Integer.parseInt(interval[0]);
				int eIndex = Integer.parseInt(interval[1]);
				EventRole argument = manager.toEventRole(event, arg_names[0]);
			}
			
			EventLabeledTextSpan span = new EventLabeledTextSpan(word_ids);
			
			String[] indices;
			
			// --SPAN--
			line = scan.nextLine(); 
			if(!line.startsWith("--SPAN--")){
				throw new RuntimeException("Invalid format:"+line+". Expecting --SPAN--.");
			}
			line = scan.nextLine(); 
			indices = line.split(",");
			int span_bIndex = Integer.parseInt(indices[0]);
			int span_eIndex = Integer.parseInt(indices[1]);
			span.setEventSpanIndices(new int[]{span_bIndex, span_eIndex});

			// --TRIGGER--
			line = scan.nextLine(); 
			if(!line.startsWith("--TRIGGER--")){
				throw new RuntimeException("Invalid format:"+line+". Expecting --TRIGGER--.");
			}
			line = scan.nextLine(); 
			indices = line.split(",");
			int trigger_bIndex = Integer.parseInt(indices[0]);
			int trigger_eIndex = Integer.parseInt(indices[1]);
			span.setEventTriggerIndices(new int[]{trigger_bIndex, trigger_eIndex});
			
			// --TAGS--
			line = scan.nextLine(); 
			if(!line.startsWith("--TAGS--")){
				throw new RuntimeException("Invalid format:"+line+". Expecting --TAGS--.");
			}
			line = scan.nextLine().trim();
			int num_tags = Integer.parseInt(line);
			int index;
			for(int i = 0; i<num_tags; i++){
				line = scan.nextLine().trim();
				index = line.indexOf(" ");
				int tag_type = Integer.parseInt(line.substring(0,index).trim());
				String tag_name = line.substring(index+1).trim();
				if(tag_type == 0){//these tags are assigned to each word
					line = scan.nextLine();
					Tag[][] tags =  toTags(line, manager, words.length);
					if(tag_name.equals("POS")){
						for(int k = 0; k<tags.length; k++){
							System.err.println(word_ids[k].getName());
							System.err.println(tags[k][0]);
						}
						System.exit(1);
					}
//					boolean added = span.addTags(tag_name, tags);
//					if(!added){
//						throw new RuntimeException("Length mismatch:"+line+"\n"+tags.length+"\n"+words.length);
//					}
				} else {
					throw new RuntimeException("Invalid format:"+line+". Expecting 0");
				}
			}
			
			line = scan.nextLine(); 
			if(!line.startsWith("--LABELS--")){
				throw new RuntimeException("Invalid format:"+line+". Expecting --LABELS--.");
			}
			line = scan.nextLine();
			index = line.indexOf(" ");
			int num_labels = Integer.parseInt(line.substring(0,index).trim());
			for(int i = 0; i<num_labels; i++){
				String[] parts = scan.nextLine().split("\\s");
				String[] interval = parts[0].split(",");
				int bIndex = Integer.parseInt(interval[0]);
				int eIndex = Integer.parseInt(interval[1]);
				MentionType type = manager.toMentionType(parts[1]);
			}
			
		}
		
	}
	
	private static Tag[][] toTags(String line, IEManager manager, int expected_num_tokens){
		ArrayList<String> tags_all = tokenize(line);
		if(tags_all.size()!=expected_num_tokens){
			System.err.println("expected:"+expected_num_tokens);
			System.err.println("found   :"+tags_all.size());
			System.err.println(line);
			System.exit(1);
		}
		Tag[][] res = new Tag[tags_all.size()][];
		for(int i = 0; i<tags_all.size(); i++){
			StringTokenizer st = new StringTokenizer(tags_all.get(i));
			ArrayList<String> tags_form1 = new ArrayList<String>();
			while(st.hasMoreTokens()){
				String tags_form_str = st.nextToken();
				if(!tags_form1.contains(tags_form_str) && !tags_form_str.equals("")){
					tags_form1.add(tags_form_str);
				}
			}
			Tag[] tags = new Tag[tags_form1.size()];
			for(int j =0; j<tags_form1.size(); j++){
				tags[j] = new Tag(tags_form1.get(j));
			}
			res[i] = tags;
		}
		return res;
	}
	
	private static ArrayList<String> tokenize(String line){
		ArrayList<String> arr = new ArrayList<String>();
		tokenize_helper(line, arr);
		return arr;
	}

	private static void tokenize_helper(String line, ArrayList<String> arr){
		int index = line.indexOf("|***|");
		if(index==-1){
			arr.add(line);
		} else {
			arr.add(line.substring(0, index).trim());
			tokenize_helper(line.substring(index+5).trim(), arr);
		}
	}
	
}

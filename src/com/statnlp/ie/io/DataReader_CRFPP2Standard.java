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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import com.statnlp.commons.AttributedWord;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.ie.types.IEManager;
import com.statnlp.ie.types.LabeledTextSpan;
import com.statnlp.ie.types.Mention;

public class DataReader_CRFPP2Standard {
	
	public static void main(String args[])throws IOException{
		
		String filename_template = "data/ace.template";
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		
		IEManager manager = EventExtractionReader.readIEManager(filename_template);
		
		Scanner scan;
		PrintWriter p;
		
		String filename_input;
		String filename_output;
		
		filename_input = "data/raw/CONLL2002/ned.train";
		filename_output = "data/CONLL2002/data/Dutch/mention-standard/FINE_TYPE/train.data";
		
		scan = new Scanner(new File(filename_input));
		p = new PrintWriter(new File(filename_output));
		
		process_CRFPP(scan, p, manager);
		
		scan.close();
		p.close();
		

		filename_input = "data/raw/CONLL2002/ned.testa";
		filename_output = "data/CONLL2002/data/Dutch/mention-standard/FINE_TYPE/dev.data";
		
		scan = new Scanner(new File(filename_input));
		p = new PrintWriter(new File(filename_output));
		
		process_CRFPP(scan, p, manager);
		
		scan.close();
		p.close();
		
		filename_input = "data/raw/CONLL2002/ned.testb";
		filename_output = "data/CONLL2002/data/Dutch/mention-standard/FINE_TYPE/test.data";
		
		scan = new Scanner(new File(filename_input));
		p = new PrintWriter(new File(filename_output));
		
		process_CRFPP(scan, p, manager);
		
		scan.close();
		p.close();
	}
	
	public static void main2(String args[])throws IOException{
		
		String filename_template = "data/ace.template";
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		
		IEManager manager = EventExtractionReader.readIEManager(filename_template);
		
		Scanner scan;
		PrintWriter p;
		
		String filename_input;
		String filename_output;

		filename_input = "data/raw/CONLL2002/esp.train";
		filename_output = "data/CONLL2002/data/Spanish/mention-standard/FINE_TYPE/train.data";
		
		scan = new Scanner(new File(filename_input));
		p = new PrintWriter(new File(filename_output));
		
		process_CRFPP(scan, p, manager);
		
		scan.close();
		p.close();
		

		filename_input = "data/raw/CONLL2002/esp.testa";
		filename_output = "data/CONLL2002/data/Spanish/mention-standard/FINE_TYPE/dev.data";
		
		scan = new Scanner(new File(filename_input));
		p = new PrintWriter(new File(filename_output));
		
		process_CRFPP(scan, p, manager);
		
		scan.close();
		p.close();
		
		filename_input = "data/raw/CONLL2002/esp.testb";
		filename_output = "data/CONLL2002/data/Spanish/mention-standard/FINE_TYPE/test.data";
		
		scan = new Scanner(new File(filename_input));
		p = new PrintWriter(new File(filename_output));
		
		process_CRFPP(scan, p, manager);
		
		scan.close();
		p.close();
	}
	
	public static void process_CRFPP(Scanner scan, PrintWriter p, IEManager manager)throws FileNotFoundException{
		
		String line;
		ArrayList<String> lines;
		
		lines = new ArrayList<String>();
		while(scan.hasNextLine()){
			line = scan.nextLine().trim();
			if(line.equals("")){
				String words[] = new String[lines.size()];
				String tags[] = new String[lines.size()];
				String labels[] = new String[lines.size()];
				AttributedWord[] aws = new AttributedWord[words.length];
				
				for(int k = 0; k<lines.size(); k++){
					String s = lines.get(k);
//					System.err.println(s);
					String[] fields = s.split("\\s");
					words[k] = fields[0];
					tags[k] = fields[1];
					
					aws[k] = new AttributedWord(words[k]);
					aws[k].addAttribute("POS", tags[k]);
					labels[k] = fields[2];
				}
				
				LabeledTextSpan span = new LabeledTextSpan(aws);
				
				int bIndex;
				int eIndex = -1;
				String t;
				for(int k = 0; k<labels.length; k++){
					if(labels[k].startsWith("B-")){
						bIndex = k;
						t = labels[k].substring(2);
						boolean closed = false;
						for(int j = k+1; j<labels.length; j++){
							if(!labels[j].equals("I-"+t)){
								eIndex = j;
								closed = true;
								break;
							}
						}
						if(!closed){
							eIndex = labels.length;
						}
						Mention m = new Mention(bIndex, eIndex, bIndex, eIndex, manager.toMentionType(t));
						span.label(bIndex, eIndex, m);
					}
				}
				
				p.println(span.toStandardFormat());
				p.flush();
				
//				System.err.println(span.getAllMentions());
//				System.exit(1);
				
				lines = new ArrayList<String>();
			} else {
				lines.add(line);
			}
		}
		
	}
	
	public static void process_withOverlapping(Scanner scan, PrintWriter p, IEManager manager) throws FileNotFoundException{
		
		int num_instances = 0;
		while(scan.hasNextLine()){
			String[] words = scan.nextLine().split("\\s");
			String[] tags = scan.nextLine().split("\\s");
			String annot = scan.nextLine();
			String[] annotations;
			if(!annot.equals("")){
				annotations = annot.split("\\|");
			} else {
				annotations = new String[0];
			}
			
			AttributedWord[] aws = new AttributedWord[words.length];
			for(int k = 0; k<words.length; k++){
				aws[k] = new AttributedWord(words[k]);
				aws[k].addAttribute("POS", tags[k]);
			}
			LabeledTextSpan span = new LabeledTextSpan(aws);
			for(int k = 0; k<annotations.length; k++){
				String[] annotation = annotations[k].split("\\s");
				String[] indices = annotation[0].split(",");
				int bIndex = Integer.parseInt(indices[0]);
				int eIndex = Integer.parseInt(indices[1]);
				int bIndex_head = Integer.parseInt(indices[2]);
				int eIndex_head = Integer.parseInt(indices[3]);
				String label = annotation[1];
				span.label(bIndex, eIndex, new Mention(bIndex, eIndex, bIndex_head, eIndex_head, manager.toMentionType(label)));
			}
			
			p.println(span.toCRFPP_format_withType_withOverlappings());
			p.flush();
			
			if(words.length!=tags.length){
				throw new RuntimeException("The lengths between words and tags are not the same!");
			}
			
			scan.nextLine();
			num_instances++;
		}
		System.err.println("There are "+num_instances+" instances.");
		
	}
	
	public static void process_noOverlapping(Scanner scan, PrintWriter p, IEManager manager) throws FileNotFoundException{
		
		int num_instances = 0;
		while(scan.hasNextLine()){
			String[] words = scan.nextLine().split("\\s");
			String[] tags = scan.nextLine().split("\\s");
			String annot = scan.nextLine();
			String[] annotations;
			if(!annot.equals("")){
				annotations = annot.split("\\|");
			} else {
				annotations = new String[0];
			}
			
			AttributedWord[] aws = new AttributedWord[words.length];
			for(int k = 0; k<words.length; k++){
				aws[k] = new AttributedWord(words[k]);
				aws[k].addAttribute("POS", tags[k]);
			}
			LabeledTextSpan span = new LabeledTextSpan(aws);
			for(int k = 0; k<annotations.length; k++){
				String[] annotation = annotations[k].split("\\s");
				String[] indices = annotation[0].split(",");
				int bIndex = Integer.parseInt(indices[0]);
				int eIndex = Integer.parseInt(indices[1]);
				int bIndex_head = Integer.parseInt(indices[2]);
				int eIndex_head = Integer.parseInt(indices[3]);
				String label = annotation[1];
				span.label(bIndex, eIndex, new Mention(bIndex, eIndex, bIndex_head, eIndex_head, manager.toMentionType(label)));
			}
			
			p.println(span.toCRFPP_format_withType());
			p.flush();
			
			if(words.length!=tags.length){
				throw new RuntimeException("The lengths between words and tags are not the same!");
			}
			
			scan.nextLine();
			num_instances++;
		}
		System.err.println("There are "+num_instances+" instances.");
		
	}
	
}
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import com.statnlp.commons.types.Segment;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.ie.types.IEConfig;
import com.statnlp.ie.types.IEManager;
import com.statnlp.ie.types.LabeledTextSpan;

public class DataReader_Raw2Standard_ACE {

	public static void main(String args[])throws IOException{
		
		IEConfig._MAX_MENTION_LENGTH = 6;
		
		int cap = IEConfig._MAX_MENTION_LENGTH;
		
		String filename_template = "data/ace.template";
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		
		IEManager manager = EventExtractionReader.readIEManager(filename_template);
		
		System.err.println("There are "+(manager.getMentionTemplate().getAllTypes().length)+" mention types.");
		
		for(String folder_name: new String[]{"ACE2005"}){
			for(String subfolder_name : new String[]{"English"}){
				File f = new File("data/"+folder_name+"/data/"+subfolder_name+"/mention-standard/"+IEConfig._type.name()+"/");
				f.mkdirs();
				
				PrintWriter p_train = new PrintWriter(new File("data/"+folder_name+"/data/"+subfolder_name+"/mention-standard/"+IEConfig._type.name()+"/train.data"));
				PrintWriter p_dev = new PrintWriter(new File("data/"+folder_name+"/data/"+subfolder_name+"/mention-standard/"+IEConfig._type.name()+"/dev.data"));
				PrintWriter p_test = new PrintWriter(new File("data/"+folder_name+"/data/"+subfolder_name+"/mention-standard/"+IEConfig._type.name()+"/test.data"));
				
				System.err.println("Processing:"+"data/"+folder_name+"/data/"+subfolder_name);
				ArrayList<ArrayList<LabeledTextSpan>> spans = process("data/"+folder_name+"/data/"+subfolder_name, manager);
				
				for(int k = 0; k<3; k++){
					
					ArrayList<LabeledTextSpan> spans_k  = spans.get(k);
					PrintWriter p;
					if(k==0)
						p = p_train;
					else if(k==1)
						p = p_dev;
					else
						p = p_test;
					
					int max_len_train = -1;
					int num_mentions_train = 0;
					double avg_len_train = 0;
					double avg_men_len_train = 0;
					double num_men_len_gt_cap_train = 0;
					int max_men_len_train = -1;
					int num_men_overlap_train = 0;
					int num_men_overlap_notNested_train = 0;
					double num_sents_overlap_train = 0;
					
					for(LabeledTextSpan span : spans_k){
						if(span.length()>max_len_train)
							max_len_train = span.length();
						num_mentions_train += span.countAllLabels();
						avg_len_train += span.length();
						num_men_overlap_train += span.countOverlappingSegments();
						num_men_overlap_notNested_train += span.countNested();
						if(span.countOverlappingSegments()!=0){
							num_sents_overlap_train++;
						}
						ArrayList<Segment> segs = span.getAllSegments();
						for(Segment seg : segs){
							avg_men_len_train+= seg.length();
							if(seg.length()>cap){
								num_men_len_gt_cap_train ++;
							}
							if(seg.length()>max_men_len_train){
								max_men_len_train = seg.length();
							}
						}
						p.println(span.toStandardFormat());
						p.flush();
					}
					
					System.err.println("=DATA SET=");
					System.err.println("# Sents:\t"+spans_k.size());
					System.err.println("Max Sent Len:\t"+max_len_train);
					System.err.println("Avg Sent Len:\t"+avg_len_train/spans_k.size());
					System.err.println("# Ments:\t"+num_mentions_train);
					System.err.println("Avg Ment Len:\t"+avg_men_len_train/num_mentions_train);
					System.err.println("Max Ment Len:\t"+max_men_len_train);
					System.err.println("# Ments (Len>"+IEConfig._MAX_MENTION_LENGTH+"):\t"+(num_men_len_gt_cap_train)+" ("+(num_men_len_gt_cap_train/num_mentions_train)+")");
					System.err.println("# Ments (Overlap):\t"+num_men_overlap_train);
					System.err.println("# Ments (Overlap, Not Nested):\t"+num_men_overlap_notNested_train);
					System.err.println("# Sents (Has Overlap):\t"+num_sents_overlap_train+" ("+num_sents_overlap_train/spans_k.size()+")");
					System.err.println();
					System.err.println();
					
				}
				
				p_train.close();
				p_test.close();
				p_dev.close();
			}
		}
		
	}
	
	private static ArrayList<ArrayList<LabeledTextSpan>> process(String dir_name, IEManager manager) throws IOException{
		
		String files_all = dir_name+"/_split/all.txt";
		
		ArrayList<LabeledTextSpan> spans_train = new ArrayList<LabeledTextSpan>();
		ArrayList<LabeledTextSpan> spans_dev = new ArrayList<LabeledTextSpan>();
		ArrayList<LabeledTextSpan> spans_test = new ArrayList<LabeledTextSpan>();
		
		ArrayList<ArrayList<String>> file_names = new ArrayList<ArrayList<String>>();
		file_names.add(new ArrayList<String>());
		file_names.add(new ArrayList<String>());
		file_names.add(new ArrayList<String>());
		
		Random rand = new Random(1);
		
		Scanner scan;
		
		int num_docs[] = new int[3];
		
		scan = new Scanner(new File(files_all));
		while(scan.hasNext()){
			String file_name = scan.nextLine();
			double v = rand.nextDouble();
			if(v<=0.1){
				System.err.print('T');
				ArrayList<LabeledTextSpan> spans = IEDocumentsReader.readLabeledSpansFromSingleFile(dir_name, file_name, manager);
				spans_test.addAll(spans);
				num_docs[2] ++;
				file_names.get(2).add(file_name);
			} else if(v<=0.2){
				System.err.print('D');
				ArrayList<LabeledTextSpan> spans = IEDocumentsReader.readLabeledSpansFromSingleFile(dir_name, file_name, manager);
				spans_dev.addAll(spans);
				num_docs[1] ++;
				file_names.get(1).add(file_name);
			} else {
				System.err.print('A');
				ArrayList<LabeledTextSpan> spans = IEDocumentsReader.readLabeledSpansFromSingleFile(dir_name, file_name, manager);
				spans_train.addAll(spans);
				num_docs[0] ++;
				file_names.get(0).add(file_name);
			}
		}
		scan.close();
		
		String fname;
		PrintWriter p;
		
		fname = dir_name+"/_split/train.txt";
		p = new PrintWriter(new File(fname));
		for(String f : file_names.get(0)){
			p.println(f);
			p.flush();
		}
		p.close();

		fname = dir_name+"/_split/dev.txt";
		p = new PrintWriter(new File(fname));
		for(String f : file_names.get(1)){
			p.println(f);
			p.flush();
		}
		p.close();

		fname = dir_name+"/_split/test.txt";
		p = new PrintWriter(new File(fname));
		for(String f : file_names.get(2)){
			p.println(f);
			p.flush();
		}
		p.close();
		
		System.err.println("Split:"+Arrays.toString(num_docs));
		
		System.err.println("#entities:"+IEDocumentsReader._num_entities);
		System.err.println("#mentions:"+IEDocumentsReader._num_mentions);
		System.err.println("#mentions_found:"+IEDocumentsReader._num_mentions_found);
		
		ArrayList<ArrayList<LabeledTextSpan>> results = new ArrayList<ArrayList<LabeledTextSpan>>();
		results.add(spans_train);
		results.add(spans_dev);
		results.add(spans_test);
		
		return results;
		
	}
	
}
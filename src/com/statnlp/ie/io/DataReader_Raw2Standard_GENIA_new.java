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
import java.util.HashMap;
import java.util.Scanner;

import com.statnlp.commons.AttributedWord;
import com.statnlp.commons.StringUtil;
import com.statnlp.commons.types.Segment;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.ie.types.IEConfig;
import com.statnlp.ie.types.IEDocument;
import com.statnlp.ie.types.IEManager;
import com.statnlp.ie.types.IESentence;
import com.statnlp.ie.types.LabeledTextSpan;
import com.statnlp.ie.types.Mention;
import com.statnlp.ie.types.MentionType;

public class DataReader_Raw2Standard_GENIA_new {

	public static void main(String args[])throws IOException{
		
		int cap = IEConfig._MAX_MENTION_LENGTH;
		
		String filename_template = "data/ace.template";
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		
		IEManager manager = EventExtractionReader.readIEManager(filename_template);
		
		System.err.println("There are "+(manager.getMentionTemplate().getAllTypes().length)+" mention types.");
		
		for(String folder_name: new String[]{"GENIA"}){
			for(String subfolder_name : new String[]{"v3.02"}){
				
//				File f = new File("data/mention-standard/"+IEConfig._type.name()+"/"+folder_name+"/"+subfolder_name+"/");
//				f.mkdirs();
//				PrintWriter p_train = new PrintWriter(new File("data/mention-standard/"+IEConfig._type.name()+"/"+folder_name+"/"+subfolder_name+"/train.data"));
//				PrintWriter p_test = new PrintWriter(new File("data/mention-standard/"+IEConfig._type.name()+"/"+folder_name+"/"+subfolder_name+"/test.data"));
				
				File f = new File("data/"+folder_name+"/data/"+subfolder_name+"/mention-standard/"+IEConfig._type.name()+"/");
				f.mkdirs();
				
				PrintWriter p_train = new PrintWriter(new File("data/"+folder_name+"/data/"+subfolder_name+"/mention-standard/"+IEConfig._type.name()+"/train.data"));
				PrintWriter p_test = new PrintWriter(new File("data/"+folder_name+"/data/"+subfolder_name+"/mention-standard/"+IEConfig._type.name()+"/test.data"));
				
				PrintWriter p_all = new PrintWriter(new File("data/"+folder_name+"/data/"+subfolder_name+"/mention-standard/"+IEConfig._type.name()+"/all.txt"));
				
				System.err.println("Processing:"+folder_name+"/"+subfolder_name);
				ArrayList<ArrayList<LabeledTextSpan>> spans = process_genia("data/"+folder_name+"/GENIAcorpus3.02.xml", manager);
				
				spans = improve_genia(spans, manager);
				
				ArrayList<LabeledTextSpan> spans_train = spans.get(0);
				ArrayList<LabeledTextSpan> spans_test  = spans.get(1);
				
				for(LabeledTextSpan span : spans_train){
					String line = span.toLine();
					p_all.println(line);
					p_all.flush();
				}
				p_all.close();
				
				int max_len_train = -1;
				int num_mentions_train = 0;
				double avg_len_train = 0;
				double avg_men_len_train = 0;
				double num_men_len_gt_cap_train = 0;
				int max_men_len_train = -1;
				int num_men_overlap_train = 0;
				int num_men_overlap_notNested_train = 0;
				double num_sents_overlap_train = 0;
				
				for(LabeledTextSpan span : spans_train){
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
					p_train.println(span.toStandardFormat_noPOS());
					p_train.flush();
				}
				p_train.close();
				
				int max_len_test = -1;
				int num_mentions_test = 0;
				double avg_len_test = 0;
				double avg_men_len_test = 0;
				double num_men_len_gt_cap_test = 0;
				int max_men_len_test = -1;
				int num_men_overlap_test = 0;
				int num_men_overlap_notNested_test = 0;
				double num_sents_overlap_test = 0;
				
				for(LabeledTextSpan span : spans_test){
					if(span.length()>max_len_test)
						max_len_test = span.length();
					num_mentions_test += span.countAllLabels();
					avg_len_test += span.length();
					num_men_overlap_test += span.countOverlappingSegments();
					num_men_overlap_notNested_test += span.countNested();
					if(span.countOverlappingSegments()!=0){
						num_sents_overlap_test++;
					}
					ArrayList<Segment> segs = span.getAllSegments();
					for(Segment seg : segs){
						avg_men_len_test+= seg.length();
						if(seg.length()>cap){
							num_men_len_gt_cap_test ++;
						}
						if(seg.length()>max_men_len_test){
							max_men_len_test = seg.length();
						}
					}
					p_test.println(span.toStandardFormat_noPOS());
					p_test.flush();
				}
				p_test.close();
				
				System.err.println("numInstancesTrain:\t"+spans_train.size());
				System.err.println("numInstancesTest :\t"+spans_test.size());
				System.err.println("maxLenTrain:\t"+max_len_train);
				System.err.println("maxLenTest :\t"+max_len_test);
				System.err.println("avgLenTrain:\t"+avg_len_train/spans_train.size());
				System.err.println("avgLenTest :\t"+avg_len_test/spans_test.size());
				System.err.println("numMentionsTrain:\t"+num_mentions_train);
				System.err.println("numMentionsTest :\t"+num_mentions_test);
				System.err.println("avgMentionLenTrain:\t"+avg_men_len_train/num_mentions_train);
				System.err.println("avgMentionLenTest :\t"+avg_men_len_test/num_mentions_test);
				System.err.println("gtCapMentionLenTrain:\t"+(num_mentions_train-num_men_len_gt_cap_train)+" ("+(1.0-num_men_len_gt_cap_train/num_mentions_train)+")");
				System.err.println("gtCapMentionLenTest :\t"+(num_mentions_test-num_men_len_gt_cap_test)+" ("+(1.0-num_men_len_gt_cap_test/num_mentions_test)+")");
				System.err.println("maxMentionLenTrain:\t"+max_men_len_train);
				System.err.println("maxMentionLenTest :\t"+max_men_len_test);
				System.err.println("numMentionOverlapTrain:\t"+num_men_overlap_train);
				System.err.println("numMentionOverlapTest :\t"+num_men_overlap_test);
				System.err.println("numMentionOverlapNotNestedTrain:\t"+num_men_overlap_notNested_train);
				System.err.println("numMentionOverlapNotNestedTest :\t"+num_men_overlap_notNested_test);
				System.err.println("numInstancesHasOverlapTrain:\t"+num_sents_overlap_train+" ("+num_sents_overlap_train/spans_train.size()+")");
				System.err.println("numInstancesHasOverlapTest :\t"+num_sents_overlap_test+" ("+num_sents_overlap_test/spans_test.size()+")");
				System.err.println();
			}
		}
		
	}
	
	
	private static ArrayList<ArrayList<LabeledTextSpan>> improve_genia(ArrayList<ArrayList<LabeledTextSpan>> spans, IEManager manager){
		
		ArrayList<ArrayList<LabeledTextSpan>> spans_list = new ArrayList<ArrayList<LabeledTextSpan>>();
		
		ArrayList<LabeledTextSpan> spans_train = spans.get(0);
//		ArrayList<LabeledTextSpan> spans_dev = spans.get(1);
		ArrayList<LabeledTextSpan> spans_test = spans.get(1);
		
		ArrayList<LabeledTextSpan> spans_train_improved = new ArrayList<LabeledTextSpan>();
//		ArrayList<LabeledTextSpan> spans_dev_improved = new ArrayList<LabeledTextSpan>();
		ArrayList<LabeledTextSpan> spans_test_improved = new ArrayList<LabeledTextSpan>();
		
		for(int k = 0; k<spans_train.size(); k++){
			LabeledTextSpan span = spans_train.get(k);
			ArrayList<LabeledTextSpan> spans_improved = improve_genia(span, manager);
//			System.err.println(span);
//			System.exit(1);
			spans_train_improved.addAll(spans_improved);
		}
		
//		for(int k = 0; k<spans_dev.size(); k++){
//			LabeledTextSpan span = spans_dev.get(k);
//			ArrayList<LabeledTextSpan> spans_improved = improve_genia(span, manager);
//			spans_dev_improved.addAll(spans_improved);
//		}
		
		for(int k = 0; k<spans_test.size(); k++){
			LabeledTextSpan span = spans_test.get(k);
			ArrayList<LabeledTextSpan> spans_improved = improve_genia(span, manager);
			spans_test_improved.addAll(spans_improved);
		}
		
		spans_list.add(spans_train_improved);
//		spans_list.add(spans_dev_improved);
		spans_list.add(spans_test_improved);
		
		System.err.println(spans_train_improved.size());
		System.err.println(spans_test_improved.size());
		
		return spans_list;
		
	}
	
	private static ArrayList<LabeledTextSpan> improve_genia(LabeledTextSpan span_old, IEManager manager){
		StringBuilder sb = new StringBuilder();
		
		ArrayList<String> words = new ArrayList<String>();
		
		for(int k = 0; k<span_old.length(); k++){
			String word = span_old.getWord(k).getName();
			words.add(word);
			if(k!=0)
				sb.append(' ');
			sb.append(word);
		}
		
		String sent = sb.toString();
		
		ArrayList<LabeledTextSpan> spans = new ArrayList<LabeledTextSpan>();
		
		IEDocument doc = new IEDocument("", sent);
		doc.toSentences();
		
		ArrayList<Mention> mentions = span_old.getAllMentions();
		for(int k = 0; k<mentions.size(); k++){
			Mention mention = mentions.get(k);
			Segment segment = mention.getSegment();
			
			int bIndex = segment.getBIndex();
			int eIndex = segment.getEIndex();
			
			int bOffset_mention=0;
			for(int i = 0; i<=bIndex-1; i++){
				bOffset_mention += words.get(i).length()+1;
			}
			
			int eOffset_mention=0;
			for(int i = 0; i<=eIndex-1; i++){
				eOffset_mention += words.get(i).length()+1;
			}
			eOffset_mention-=2;
			
			doc.annotate(bOffset_mention, eOffset_mention, bOffset_mention, eOffset_mention, (MentionType)mention.getSemanticTag());
		}
		
		ArrayList<IESentence> sentences = doc.getIESentences();
		
		System.err.println(sentences.size()+"<<<"+sent);
		
		for(int k = 0; k<sentences.size(); k++){
			IESentence sentence = sentences.get(k);
			LabeledTextSpan span = sentence.toLabeledTextSpan(manager);
			System.err.println(span.getAllMentions().size()+" # of mentions..");
			spans.add(span);
		}
		
		return spans;
	}
	
	
	//<sentence>The <cons lex="CD4_coreceptor" sem="G#protein_molecule">CD4 coreceptor</cons> interacts with <cons lex="non-polymorphic_region" sem="G#protein_domain_or_region">non-polymorphic regions</cons> of <cons lex="major_histocompatibility_complex_class_II_molecule" sem="G#protein_family_or_group">major histocompatibility complex class II molecules</cons> on <cons lex="antigen-presenting_cell" sem="G#cell_type">antigen-presenting cells</cons> and contributes to <cons lex="T_cell_activation" sem="G#other_name">T cell activation</cons>.</sentence>
	//TODO
	private static ArrayList<ArrayList<LabeledTextSpan>> process_genia(String xmlfile_name, IEManager manager) throws IOException{
		ArrayList<LabeledTextSpan> spans_train = new ArrayList<LabeledTextSpan>();
		ArrayList<LabeledTextSpan> spans_test = new ArrayList<LabeledTextSpan>();
		
		ArrayList<LabeledTextSpan> spans = null;
		
		Scanner scan = new Scanner(new File(xmlfile_name));
		
		int n = 0;
		String line;
		while(scan.hasNextLine()){
			line = scan.nextLine();
//			if(line.startsWith("<article>")){
//				n++;
//				System.err.println(n);
//				if(n<=1.0*2000)
//				{
//					spans = spans_train;
//				} else {
//					_test_mode = true;
//					spans = spans_test;
//				}
//			}
			if(line.indexOf("<sentence")!=-1){
				int bIndex = line.indexOf("<sentence");
				int eIndex = line.lastIndexOf("</sentence");
				line = line.substring(bIndex, eIndex+"</sentence>".length());
				
				n++;
				if(n<=0.9*(16791+1755)){
					spans = spans_train;
				} else {
					_test_mode = true;
					spans = spans_test;
				}
				LabeledTextSpan span = processLine(line, manager);
				spans.add(span);
			}
		}
		
		ArrayList<ArrayList<LabeledTextSpan>> results = new ArrayList<ArrayList<LabeledTextSpan>>();
		results.add(spans_train);
		results.add(spans_test);
		
		System.err.println("TRAIN:"+spans_train.size());
		System.err.println("TEST :"+spans_test.size());
		System.err.println("OK:");
		
		return results;
	}
	
	private static LabeledTextSpan processLine(String line, IEManager manager){
		line = line.trim();
		if(!line.startsWith("<sentence>") || !line.endsWith("</sentence>")){
			throw new RuntimeException("Error:"+line);
		}
		
		int bIndex = line.indexOf(">");
		int eIndex = line.lastIndexOf("<");
		line = line.substring(bIndex+1, eIndex).trim();
		
		String[] tokens = StringUtil.stripXMLTags(line).split("\\s");
		AttributedWord[] words = new AttributedWord[tokens.length];
		for(int k = 0; k<tokens.length; k++){
			AttributedWord word = new AttributedWord(tokens[k]);
			words[k] = word;
		}
		
		LabeledTextSpan span = new LabeledTextSpan(words);
		
		bIndex = 0;
		
		processRemaining(span, line, bIndex, manager);
		
		return span;
	}
	
	private static void processRemaining(LabeledTextSpan span, String line, int bIndex, IEManager manager){
		
		int startIndex = line.indexOf("<cons", bIndex);
		if(startIndex==-1)
			return;
		
		findNextNE(span, line, startIndex, manager);
		
		int endIndex = line.indexOf(">", startIndex);
		
		processRemaining(span, line, endIndex+1, manager);
		
	}
	
	private static boolean _test_mode = false;
	
	private static void findNextNE(LabeledTextSpan span, String line, int bIndex, IEManager manager){
		
		int startIndex = line.indexOf("<cons", bIndex);
		int semStartIndex = line.indexOf("sem=\"", startIndex)+"sem=\"".length();
		int semEndIndex = line.indexOf("\"", semStartIndex+"sem=\"".length());
		String sem = line.substring(semStartIndex, semEndIndex);
		int b_pos = StringUtil.numTokens(StringUtil.stripXMLTags(line.substring(0, startIndex)).trim());
		//.split("\\s").length;
		int e_pos = -1;
		int fromIndex = startIndex+1;
		int nested_count = 1;
		while(true){
			int index = line.indexOf("<", fromIndex);
			if(line.substring(index).startsWith("<cons")){
				nested_count++;
			} else{
				nested_count--;
			}
			if(nested_count==0){
				e_pos = StringUtil.numTokens(StringUtil.stripXMLTags(line.substring(0, index)).trim());
				//StringUtil.stripXMLTags(line.substring(0, index)).trim().split("\\s").length;
				System.err.println(b_pos+","+e_pos+"="+sem);
				if(b_pos == e_pos){
					System.err.println(Arrays.toString("".split("\\s")));
					System.err.println("".split("\\s").length);
					System.err.println(line);
					System.exit(1);
				}
//				span.label(b_pos, e_pos, new Mention(b_pos, e_pos, b_pos, e_pos, manager.toMentionType(sem)));
				String[] list = new String[]{"G#cell_line", "G#cell_type", "G#protein", "G#RNA", "G#DNA"};
				for(int k = 0; k<list.length; k++){
					if(sem.startsWith(list[k])){
						span.label(b_pos, e_pos, new Mention(b_pos, e_pos, b_pos, e_pos, manager.toMentionType(list[k])));
						if(_test_mode)
							num[k]++;
						break;
					} else if(sem.startsWith("(")){
						System.err.println(sem);
						String tokens[] = sem.split("\\s");
						boolean hasType = false;
						for(String token : tokens){
							if(token.startsWith(list[k])){
								hasType = true;
								break;
							}
						}
						if(hasType){
//							System.err.println("GOOD.");
							span.label(b_pos, e_pos, new Mention(b_pos, e_pos, b_pos, e_pos, manager.toMentionType(list[k])));
//							System.exit(1);
							if(_test_mode)
								num[k]++;
						}
//						
					}
				}
				break;
			}
			fromIndex = index+1;
		}
		
		System.err.println(Arrays.toString(num));
		
	}
	
	private static int num[] = new int[5];
	
//	private static ArrayList<ArrayList<LabeledTextSpan>> process(String dir_name, IEManager manager) throws IOException{
//		
//		String files_train = dir_name+"/train.txt";
//		String files_test  = dir_name+"/test.txt";
//		
//		ArrayList<LabeledTextSpan> spans_train = new ArrayList<LabeledTextSpan>();
//		ArrayList<LabeledTextSpan> spans_test = new ArrayList<LabeledTextSpan>();
//		
//		Scanner scan;
//
//		scan = new Scanner(new File(files_train));
//		while(scan.hasNext()){
//			String file_name = scan.nextLine();
//			ArrayList<LabeledTextSpan> spans = IEDocumentsReader.readLabeledSpansFromSingleFile(dir_name, file_name, manager);
//			spans_train.addAll(spans);
//		}
//		scan.close();
//		
//		scan = new Scanner(new File(files_test));
//		while(scan.hasNext()){
//			String file_name = scan.nextLine();
//			ArrayList<LabeledTextSpan> spans = IEDocumentsReader.readLabeledSpansFromSingleFile(dir_name, file_name, manager);
//			spans_test.addAll(spans);
//		}
//		scan.close();
//		
//		ArrayList<ArrayList<LabeledTextSpan>> results = new ArrayList<ArrayList<LabeledTextSpan>>();
//		results.add(spans_train);
//		results.add(spans_test);
//		
//		return results;
//		
//	}
	
}
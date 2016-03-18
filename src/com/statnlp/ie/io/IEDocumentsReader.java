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
import java.util.ArrayList;
import java.util.Scanner;

import com.statnlp.ie.types.IEDocument;
import com.statnlp.ie.types.IEManager;
import com.statnlp.ie.types.IESentence;
import com.statnlp.ie.types.LabeledTextSpan;
import com.statnlp.ie.types.MentionType;

public class IEDocumentsReader {
	
	public static void main(String args[])throws IOException{
		
		String dir_name = "data/ace2004_normalized/nwire/";
		IEManager manager = EventExtractionReader.readIEManager("data/ace.template");
		ArrayList<LabeledTextSpan> spans = readLabeledSpans(dir_name, manager);
		System.err.println("#Spans:"+spans.size());
		
	}
	
	public static ArrayList<LabeledTextSpan> readLabeledSpansFromSingleFile(String dir_name, String file_name, IEManager manager) throws IOException{
		
		ArrayList<LabeledTextSpan> spans = new ArrayList<LabeledTextSpan>();
		
		String doc_id = file_name.substring(0,file_name.length());
		IEDocument doc = readIEDocument(dir_name, doc_id);
		
		System.err.println(file_name);
		
		doc.toSentences();
		readAnnotations(doc, manager);
		
		ArrayList<IESentence> sents = doc.getIESentences();
		for(int k = 0; k<sents.size(); k++){
			LabeledTextSpan span = sents.get(k).toLabeledTextSpan(manager);
			spans.add(span);
		}
		
		return spans;
		
	}
	
	public static ArrayList<ArrayList<LabeledTextSpan>> readLabeledSpansGroupedByDocs(String dir_name, IEManager manager) throws IOException{
		
		ArrayList<ArrayList<LabeledTextSpan>> spanList = new ArrayList<ArrayList<LabeledTextSpan>>();
		
		File f = new File(dir_name);
		
		int n_files = 0;
		
		int max_len = 100;
		double count_ok = 0;
		double count_total = 0;
		
		String[] file_names = f.list();
		for(String file_name : file_names){
			if(file_name.endsWith(".sgm")){
				String doc_id = file_name.substring(0,file_name.length()-4);
				IEDocument doc = readIEDocument(dir_name, doc_id);
				ArrayList<LabeledTextSpan> spans = new ArrayList<LabeledTextSpan>();
				doc.toSentences();
				ArrayList<IESentence> sentences = doc.getIESentences();
//				System.err.println();
//				System.err.println("#sentences:"+sentences.size());
				for(int k = 0; k<sentences.size(); k++){
					int len = sentences.get(k).toString().split("\\s").length;
					if(len <= max_len){
						count_ok ++;
					}
					count_total++;
				}
				readAnnotations(doc, manager);
				n_files ++;
				
				ArrayList<IESentence> sents = doc.getIESentences();
				for(int k = 0; k<sents.size(); k++){
					LabeledTextSpan span = sents.get(k).toLabeledTextSpan(manager);
					spans.add(span);
				}
				spanList.add(spans);
			}
		}
		
		System.err.println("max_len="+max_len+"\tOK="+count_ok+"/"+count_total+"="+(count_ok/count_total));
		System.err.println(n_files);
		
		return spanList;
		
	}
	
	public static ArrayList<LabeledTextSpan> readLabeledSpans(String dir_name, IEManager manager) throws IOException{
		
		ArrayList<LabeledTextSpan> spans = new ArrayList<LabeledTextSpan>();
		File f = new File(dir_name);
		
		int n_files = 0;
		
		int max_len = 100;
		double count_ok = 0;
		double count_total = 0;
		
		String[] file_names = f.list();
		for(String file_name : file_names){
			if(file_name.endsWith(".sgm")){
				String doc_id = file_name.substring(0,file_name.length()-4);
				IEDocument doc = readIEDocument(dir_name, doc_id);
				doc.toSentences();
				ArrayList<IESentence> sentences = doc.getIESentences();
				System.err.println();
//				System.err.println("#sentences:"+sentences.size());
				for(int k = 0; k<sentences.size(); k++){
					int len = sentences.get(k).toString().split("\\s").length;
					if(len <= max_len){
						count_ok ++;
					}
					count_total++;
				}
				readAnnotations(doc, manager);
				n_files ++;
				
				ArrayList<IESentence> sents = doc.getIESentences();
				for(int k = 0; k<sents.size(); k++){
					LabeledTextSpan span = sents.get(k).toLabeledTextSpan(manager);
					spans.add(span);
				}
			}
		}
		
		System.err.println("max_len="+max_len+"\tOK="+count_ok+"/"+count_total+"="+(count_ok/count_total));
		System.err.println(n_files);
		
		return spans;		
	}
	
	public static int _num_entities = 0;
	public static int _num_mentions = 0;
	public static int _num_mentions_found = 0;
	
	private static int countEntities(IEDocument doc) throws FileNotFoundException{
		String doc_name = doc.getFileName();
		String doc_annotate_name = doc_name.substring(0, doc_name.lastIndexOf("."))+".apf.xml";
		
		int count = 0;
		Scanner scan = new Scanner(new File(doc_annotate_name));
		while(scan.hasNextLine()){
			String line = scan.nextLine().trim();
			if(line.equals("</entity_mention>")){
				count ++;
			}
		}
		_num_mentions += count;
		return count;
	}
	
	public static void readAnnotations(IEDocument doc, IEManager manager) throws FileNotFoundException{
		String doc_name = doc.getFileName();
		String doc_annotate_name = doc_name.substring(0, doc_name.lastIndexOf("."))+".apf.xml";
		int num_entities = countEntities(doc);
		int num_entities_found = 0;
		Scanner scan = new Scanner(new File(doc_annotate_name));
		while(scan.hasNextLine()){
			String line = scan.nextLine().trim();
			if(line.startsWith("<entity_mention")){
				throw new RuntimeException("Error! PPP:"+line);
			}
			if(line.startsWith("</entity_mention")){
				throw new RuntimeException("Error! TTT:"+line);
			}
			if(line.startsWith("<entity ID")){
				_num_entities ++;
				String[] tokens = line.split("\\s");
				String type_s = tokens[2].split("\"")[1];
				MentionType type = manager.toMentionType(type_s);
				while(!(line=scan.nextLine().trim()).equals("</entity>")){
//					if(line.equals("</entity_mention>")){
//						throw new RuntimeException("Error! 9x0"+line);
//					}
					if(line.startsWith("<entity_mention ")){
						
						line=scan.nextLine().trim();
						if(line.equals("</entity_mention>")){
							throw new RuntimeException("Error! 0x0"+line);
						}
						if(!line.equals("<extent>")){
							throw new RuntimeException("Error! 154"+line);
						}
						
						String v1, v2;
						
						line = scan.nextLine().trim();
						if(line.equals("</entity_mention>")){
							throw new RuntimeException("Error! 2x0"+line);
						}
						if(!line.startsWith("<charseq ")){
							throw new RuntimeException("Error! 161"+line);
						}
						
						tokens = line.split("\\s");
						v1 = tokens[1].split("\"")[1];
						v2 = tokens[2].split("\"")[1];
						int bOffset = Integer.parseInt(v1);
						int eOffset = Integer.parseInt(v2);
						line = scan.nextLine().trim(); //</extent>
						if(line.equals("</entity_mention>")){
							throw new RuntimeException("Error! 1x0"+line);
						}
						while(!line.equals("</extent>")){
							line = scan.nextLine().trim(); //</extent>
							if(line.equals("</entity_mention>")){
								throw new RuntimeException("Error! 170"+line);
							}
//							throw new RuntimeException("Error! 170"+line);
						}
//						System.err.println(line);
						line = scan.nextLine().trim(); //<head>
						if(line.equals("</entity_mention>")){
							throw new RuntimeException("Error! 180"+line);
						}
//						System.err.println(line);
						line = scan.nextLine().trim(); //<charseq 
						if(line.equals("</entity_mention>")){
							throw new RuntimeException("Error! 185"+line);
						}
//						System.err.println(line);
						if(!line.startsWith("<charseq ")){
							throw new RuntimeException("Error! 175"+line);
						}
						
						tokens = line.split("\\s");
						v1 = tokens[1].split("\"")[1];
						v2 = tokens[2].split("\"")[1];
						int bOffset_head = Integer.parseInt(v1);
						int eOffset_head = Integer.parseInt(v2);
						
						if(bOffset_head<bOffset || eOffset_head>eOffset){
							System.err.println(bOffset);
							System.err.println(eOffset);
							System.err.println(bOffset_head);
							System.err.println(eOffset_head);
							System.err.println(line);
							throw new RuntimeException("x");
						}
						
						num_entities_found++;
						_num_mentions_found++;
						boolean annotated = doc.annotate(bOffset, eOffset, bOffset_head, eOffset_head, type);
						if(!annotated){
							System.err.println("Waring: ignored one annotation because the annotation crosses sentence boundaries.");
						}
					}
				}
			}
		}
		scan.close();
		
		if(num_entities_found!=num_entities){
			System.err.println(num_entities_found);
			System.err.println(num_entities);
			throw new RuntimeException("Why??"+doc.getFileName());
		}
//		System.err.println("====");
	}
	
	public static IEDocument readIEDocument(String dir_name, String doc_id)throws IOException{
		String doc_name = dir_name+"/"+doc_id+".sgm";
		Scanner scan = new Scanner(new File(doc_name));
		String text = "";
		while(scan.hasNextLine()){
			String line = scan.nextLine();
			text += stripTags(line)+"\n";
		}
		scan.close();
		IEDocument doc = new IEDocument(doc_name, text);
		return doc;
	}
	
	public static IEDocument readIEDocument_CONLL2003(String dir_name, String doc_id)throws IOException{
		String doc_name = dir_name+"/"+doc_id+".txt";
		Scanner scan = new Scanner(new File(doc_name));
		String text = "";
		while(scan.hasNextLine()){
			String line = scan.nextLine();
			text += stripTags(line)+"\n";
		}
		scan.close();
		IEDocument doc = new IEDocument(doc_name, text);
		return doc;
	}
	
	private static String stripTags(String line){
		StringBuilder sb = new StringBuilder();
		char[] ch = line.toCharArray();
		boolean recording = true;
		for(char c : ch){
			if(c == '<'){
				recording = false;
				continue;
			} else if(c == '>'){
				recording = true;
				continue;
			}
			if(recording){
				sb.append(c);
			}
		}
		return sb.toString();
	}
	
}

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import com.statnlp.commons.AttributedWord;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.ie.io.EventExtractionReader;
import com.statnlp.ie.io.IEDocumentsReader;
import com.statnlp.ie.types.IEConfig;
import com.statnlp.ie.types.IEManager;
import com.statnlp.ie.types.LabeledTextSpan;
import com.statnlp.ie.types.Mention;
import com.statnlp.ie.types.UnlabeledTextSpan;

public class Evaluator_CRFPP_BIO {
	
	public static void main(String args[]) throws IOException{

		IEConfig._MAX_MENTION_LENGTH = 100000;
		
//		String filename_template = "data/ace.template";
//		
//		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
//		IEConfig._MAX_MENTION_LENGTH = 100000;
//		
//		IEManager manager = EventExtractionReader.readIEManager(filename_template);
		
		for(String folder: new String[]{"ACE2004", "ACE2005"}){
			for(String subfolder : new String[]{"English"}){
				for(String overlap : new String[]{"no_overlap"}){
					String filename_template = "data/"+folder+"/data/"+subfolder+"/template";
					
					NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
					
					IEManager manager = EventExtractionReader.readIEManager(filename_template);
					
//					System.err.println(folder+"/"+subfolder);
//					String test_corr = "data/"+folder+"/data/"+subfolder+"/mention-standard/"+IEConfig._type.name()+"/test.data";
//					Scanner scan = new Scanner(new File(test_corr));
//					ArrayList<LabeledTextSpan> spans = readSpans(scan, manager);
//					
//					String test_pred = "experiments/mention/lcrf/"+IEConfig._type.name()+"/"+folder+"/"+subfolder+"/"+overlap+"/output-test.data";
//					scan = new Scanner(new File(test_pred));
//					
//					eval(scan, spans, manager);
//					

					System.err.println(folder+"/"+subfolder);

					{
						String test_corr = "data/"+folder+"/data/"+subfolder+"/mention-standard/"+IEConfig._type.name()+"/dev.data";
						Scanner scan = new Scanner(new File(test_corr));
						ArrayList<LabeledTextSpan> spans = readSpans(scan, manager);
						
						String test_pred = "experiments/mention/lcrf/"+IEConfig._type.name()+"/"+folder+"/"+subfolder+"/"+overlap+"/output-dev.data";
						scan = new Scanner(new File(test_pred));
						
						eval(scan, spans, manager);
					}
					{
						String test_corr = "data/"+folder+"/data/"+subfolder+"/mention-standard/"+IEConfig._type.name()+"/test.data";
						Scanner scan = new Scanner(new File(test_corr));
						ArrayList<LabeledTextSpan> spans = readSpans(scan, manager);
						
						String test_pred = "experiments/mention/lcrf/"+IEConfig._type.name()+"/"+folder+"/"+subfolder+"/"+overlap+"/output-test.data";
						scan = new Scanner(new File(test_pred));
						
						eval(scan, spans, manager);
					}
				}
			}
		}
		
	}
	
	private static void eval(Scanner scan, ArrayList<LabeledTextSpan> spans, IEManager manager){
		
		String line;
		int id = 0;
		double[] v = new double[4];
		ArrayList<AttributedWord> words = new ArrayList<AttributedWord>();
		ArrayList<String> preds = new ArrayList<String>();
		while(scan.hasNextLine()){
			line = scan.nextLine();
			if(line.trim().equals("")){
				LabeledTextSpan span_corr = spans.get(id++);
				UnlabeledTextSpan span_pred = span_corr.removeLabels();
				toSpan(v, words, preds, span_pred, manager);
				words = new ArrayList<AttributedWord>();
				preds = new ArrayList<String>();
			} else {
				String[] parts = line.split("\\s");
//				System.err.println(">>"+line+"<<");
				String word_form = parts[0];
				String pos_tag = parts[1];
				String pred = parts[3];
				AttributedWord word = new AttributedWord(word_form);
				word.addAttribute("POS", pos_tag);
				words.add(word);
				preds.add(pred);
			}
		}
		System.err.println(v[0]);
		System.err.println(v[1]);
		System.err.println(v[2]);
		if(v[3]!=v[1]){
			throw new RuntimeException(v[3]+"\t"+v[1]);
		}
		
		double P = v[0]/v[1];
		double R = v[0]/v[2];
		
		System.err.println(""+P);
		System.err.println(""+R);
		System.err.println(""+2/(1/P+1/R));
		
	}
	
	private static void toSpan(double[] v, ArrayList<AttributedWord> words, ArrayList<String> preds, UnlabeledTextSpan span, IEManager manager){
		
		double b = 0;
		if(span.length()!=words.size()){
			throw new RuntimeException(span.length()+"\t"+words.size());
		}
		for(int k = 0; k<span.length(); k++){
			if(!span.getWord(k).equals(words.get(k))){
				throw new RuntimeException(span.getWord(k)+"\t"+words.get(k));
			}
		}
		
		for(int i = 0; i<preds.size(); i++){
			String pred = preds.get(i);
			if(pred.equals("O")){
				//do nothing.
			} else {
				
				String[] tags = pred.split("\\+");
				
				for(String tag : tags){
					if(tag.startsWith("B-")){
						b++;
						String type = tag.split("\\-")[1].trim();
						for(int j = i+1; j<=preds.size(); j++){
							if(j==preds.size()){
								span.label_predict(i, j, new Mention(i,j,i,j,manager.toMentionType(type)));
							} else {
								String tag_new = preds.get(j);
								if(tag_new.indexOf("I-"+type)<0){
									boolean f = span.label_predict(i, j, new Mention(i,j,i,j,manager.toMentionType(type)));
//									if(!f){
//										throw new RuntimeException("x"+i+","+j+"="+type+"\t"+span.toLine());
//									}
									break;
								}
							}
						}
					}
				}
				
			}
		}
		
		v[0] += span.countCorrect();
		v[1] += span.countPredicted();
		v[2] += span.countExpected();
		v[3] += b;
	}
	
	public static ArrayList<LabeledTextSpan> readSpans(Scanner scan, IEManager manager) throws FileNotFoundException{
		
		ArrayList<LabeledTextSpan> spans = new ArrayList<LabeledTextSpan>();
		
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
				span.label(bIndex, eIndex, new Mention(bIndex, eIndex, bIndex, eIndex, manager.toMentionType(label)));
			}
			
			spans.add(span);
			
			if(words.length!=tags.length){
				throw new RuntimeException("The lengths between words and tags are not the same!");
			}
			
			scan.nextLine();
			num_instances++;
		}
		System.err.println("There are "+num_instances+" instances.");
		
		return spans;
		
	}
	
	private static ArrayList<LabeledTextSpan> process(String dir_name, IEManager manager) throws IOException{
		
		String files_test  = dir_name+"/test.txt";
		
		ArrayList<LabeledTextSpan> spans_test = new ArrayList<LabeledTextSpan>();
		
		Scanner scan = new Scanner(new File(files_test));
		while(scan.hasNext()){
			String file_name = scan.nextLine();
			ArrayList<LabeledTextSpan> spans = IEDocumentsReader.readLabeledSpansFromSingleFile(dir_name, file_name, manager);
			spans_test.addAll(spans);
		}
		scan.close();
		
		return spans_test;
		
	}	
}

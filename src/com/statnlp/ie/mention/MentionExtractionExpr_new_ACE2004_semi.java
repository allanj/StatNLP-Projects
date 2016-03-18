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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

import com.statnlp.commons.AttributedWord;
import com.statnlp.commons.WordUtil;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.ie.flatsemi.FlatSemiConfig;
import com.statnlp.ie.flatsemi.FlatSemiFeatureManager;
import com.statnlp.ie.flatsemi.FlatSemiInstance;
import com.statnlp.ie.flatsemi.FlatSemiModel;
import com.statnlp.ie.io.EventExtractionReader;
import com.statnlp.ie.types.IEManager;
import com.statnlp.ie.types.LabeledTextSpan;
import com.statnlp.ie.types.Mention;
import com.statnlp.ie.types.UnlabeledTextSpan;

public class MentionExtractionExpr_new_ACE2004_semi {
	

	private static ArrayList<String> findFuncWords(ArrayList<LabeledTextSpan> spans_train){
		HashMap<String, Integer> wordMap = new HashMap<String, Integer>();
		ArrayList<String> words = new ArrayList<String>();
		for(int k = 0; k<spans_train.size(); k++){
			LabeledTextSpan span = spans_train.get(k);
			ArrayList<Mention> mentions = span.getAllMentions();
			for(Mention mention : mentions){
				int bIndex = mention.getSegment().getBIndex();
				int eIndex = mention.getSegment().getEIndex();
				for(int index = bIndex; index<eIndex; index++){
					String word = span.getWord(index).getName();
					if(!WordUtil.isAllLowerCase(word)){
						continue;
					}
					if(WordUtil.isNumber(word)){
						continue;
					}
					if(!WordUtil.isAllLetters(word)){
						continue;
					}
					if(WordUtil.isPunctuationMark(word)){
						continue;
					}
					if(!wordMap.containsKey(word)){
						wordMap.put(word, 1);
					} else {
						int oldCount = wordMap.get(word);
						wordMap.put(word, oldCount+1);
					}
				}
			}
		}
		
		Iterator<String> keys = wordMap.keySet().iterator();
		while(keys.hasNext()){
			String word = keys.next();
			int count = wordMap.get(word);
			if(count>=3){
				System.err.println(word);
				words.add(word);
			}
		}
		
		return words;
	}
	
	private static ArrayList<String> findFuncWords2(ArrayList<LabeledTextSpan> spans_train){
		HashMap<String, Integer> wordMap = new HashMap<String, Integer>();
		ArrayList<String> words = new ArrayList<String>();
		
		ArrayList<String> ments = new ArrayList<String>();
		
		for(int k = 0; k<spans_train.size(); k++){
			LabeledTextSpan span = spans_train.get(k);
			ArrayList<Mention> mentions = span.getAllMentions();
			for(Mention mention : mentions){
				int bIndex = mention.getSegment().getBIndex();
				int eIndex = mention.getSegment().getEIndex();
				String form = "";
				for(int index = bIndex; index<eIndex; index++){
					String word = span.getWord(index).getName();
					form += " " + word;
				}
				form = form.trim();
				if(!ments.contains(form)){
					ments.add(form);
				}
			}
		}

		for(int k = 0; k<ments.size(); k++){
			String[] ws = ments.get(k).split("\\s");
			for(String word : ws){
				if(!WordUtil.isAllLowerCase(word)){
					continue;
				}
				if(WordUtil.isNumber(word)){
					continue;
				}
				if(!WordUtil.isAllLetters(word)){
					continue;
				}
				if(WordUtil.isPunctuationMark(word)){
					continue;
				}
				if(!wordMap.containsKey(word)){
					wordMap.put(word, 1);
				} else {
					int oldCount = wordMap.get(word);
					wordMap.put(word, oldCount+1);
				}
			}
		}
		
		Iterator<String> keys = wordMap.keySet().iterator();
		while(keys.hasNext()){
			String word = keys.next();
			int count = wordMap.get(word);
			if(count>=3){
				System.err.println(word);
				words.add(word);
			}
		}
		
		return words;
	}
	
	public static void main(String args[])throws IOException, ClassNotFoundException{
		
		int num_itrs = 200;
		int maxLen = 6;
		
		FlatSemiConfig._MAX_LBFGS_ITRS = num_itrs;
		
		System.err.println("OK11Aug1654");
		
		long bTime_overall = System.currentTimeMillis();
		
		for(String folder : new String[]{"ACE2004"}){
			for(String subfolder : new String[]{"English"})
			{
				for(String c : new String[]{"0.01"}){
					
					NetworkConfig.L2_REGULARIZATION_CONSTANT = Double.parseDouble(c);
					
					String filename_template = "data/"+folder+"/data/"+subfolder+"/template";
					
					NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
					
					IEManager manager = EventExtractionReader.readIEManager(filename_template);
					
					GlobalNetworkParam param = new GlobalNetworkParam();
					
					FlatSemiFeatureManager fm = new FlatSemiFeatureManager(param);
					
					System.err.println(Arrays.toString(manager.getMentionTemplate().getAllTypes()));
					
					File f = new File("experiments/mention/new_model/"+FlatSemiConfig._type.name()+"/"+folder+"/"+subfolder+"/");
					f.mkdirs();
					
					String filename_output = "experiments/mention/new_model/"+FlatSemiConfig._type.name()+"/"+folder+"/"+subfolder+"/output.data";
					PrintWriter p = new PrintWriter(new File(filename_output));
					
					String filename_input;
					Scanner scan;
					
					filename_input = "data/"+folder+"/data/"+subfolder+"/mention-standard/"+FlatSemiConfig._type.name()+"/train.data";
					scan = new Scanner(new File(filename_input));
					ArrayList<LabeledTextSpan> spans_train = readTextSpans(scan, manager, true);
					
					filename_input = "data/"+folder+"/data/"+subfolder+"/mention-standard/"+FlatSemiConfig._type.name()+"/dev.data";
					scan = new Scanner(new File(filename_input));
					ArrayList<LabeledTextSpan> spans_dev = readTextSpans(scan, manager, false);
					
					filename_input = "data/"+folder+"/data/"+subfolder+"/mention-standard/"+FlatSemiConfig._type.name()+"/test.data";
					scan = new Scanner(new File(filename_input));
					ArrayList<LabeledTextSpan> spans_test = readTextSpans(scan, manager, false);
					
					int train_size = spans_train.size();
					
					ArrayList<FlatSemiInstance> instances = new ArrayList<FlatSemiInstance>();
					for(int k = 0; k<train_size; k++){
						MentionFlatSemiInstance inst = new MentionFlatSemiInstance(k+1, spans_train.get(k), manager.getMentionTemplate());
						instances.add(inst);
					}
					
					System.err.println("There are "+instances.size()+" training instances.");
					System.err.println("There are "+spans_test.size()+" test instances.");
					
					FlatSemiModel model = new FlatSemiModel(fm, manager.getMentionTemplate().getAllTypesExcludingStartAndFinish_arr(), maxLen);
					
					model.train(instances, num_itrs);
					
					ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("experiments/mention/new_model/"+FlatSemiConfig._type.name()+"/"+folder+"/"+subfolder+"/model.data.c="+c+".itrs="+num_itrs));
					out.writeObject(param);
					out.flush();
					out.close();
					
					System.err.println("saved.");
					
//					ObjectInputStream in = new ObjectInputStream(new FileInputStream("experiments/mention/new_model/"+FlatSemiConfig._type.name()+"/"+folder+"/"+subfolder+"/model.data."+args[0]+"."+num_itrs+".c="+c));
//					param = (NetworkParam)in.readObject();
//					in.close();
//					
//					System.err.println("read.");
					
					double count_corr;
					double count_pred;
					double count_corr_span;
					double count_expt;
					
					double P;
					double R;
					double F;
					
					count_corr = 0;
					count_corr_span = 0;
					count_pred = 0;
					count_expt = 0;
					
					for(int k = 0; k<train_size; k++){
						MentionFlatSemiInstance inst = new MentionFlatSemiInstance(k+1, spans_train.get(k), manager.getMentionTemplate());
						MentionFlatSemiInstance inst_unlabeled = inst.removeOutput();
						
						model.max(inst_unlabeled);
						
						UnlabeledTextSpan span = (UnlabeledTextSpan)inst_unlabeled.getSpan();
						count_corr += span.countCorrect();
						count_corr_span += span.countCorrect_span();
						count_pred += span.countPredicted();
						count_expt += span.countExpected();
						
						p.println(span);
						p.flush();
					}
					p.close();
					
					System.err.println(count_corr);
					System.err.println(count_corr_span);
					System.err.println(count_pred);
					System.err.println(count_expt);

					P = count_corr/count_pred;
					R = count_corr/count_expt;
					F = 2/(1/P+1/R);
					
					System.err.println("-FULL-");
					System.err.println("P:"+P);
					System.err.println("R:"+R);
					System.err.println("F:"+F);
					
					P = count_corr_span/count_pred;
					R = count_corr_span/count_expt;
					F = 2/(1/P+1/R);
					
					System.err.println("-SPAN ONLY-");
					System.err.println("P:"+P);
					System.err.println("R:"+R);
					System.err.println("F:"+F);
					
//					System.exit(1);
					
					double lambda = 0;
					
					//dirty trick..
//					int f1 = fm.getParam().toFeature("MENTION","MENTION");
//					double weight1 = fm.getParam().getWeight(f1);
//					int f2 = fm.getParam().toFeature("ONE_WORD", "ONE_WORD");
//					double weight2 = fm.getParam().getWeight(f2);
					
					for(int j = 0; j<1000; j++){
						
						System.err.println("lambda="+lambda);
						
//						fm.getParam().setWeight(f1, weight1+lambda);
						
//						System.err.println("mention penalty="+fm.getParam().getWeight(f1));
						
						count_corr = 0;
						count_corr_span = 0;
						count_pred = 0;
						count_expt = 0;
						
						for(int k = 0; k<spans_dev.size(); k++){
							MentionFlatSemiInstance inst = new MentionFlatSemiInstance(k+1, spans_dev.get(k), manager.getMentionTemplate());
							MentionFlatSemiInstance inst_unlabeled = inst.removeOutput();
							
							model.max(inst_unlabeled);
							
							UnlabeledTextSpan span = (UnlabeledTextSpan)inst_unlabeled.getSpan();
							count_corr += span.countCorrect();
							count_corr_span += span.countCorrect_span();
							count_pred += span.countPredicted();
							count_expt += span.countExpected();
							
							p.println(span);
							p.flush();
						}
						p.close();
						
						System.err.println(count_corr);
						System.err.println(count_corr_span);
						System.err.println(count_pred);
						System.err.println(count_expt);
						
						P = count_corr/count_pred;
						R = count_corr/count_expt;
						F = 2/(1/P+1/R);
						
						System.err.println("-FULL-");
						System.err.println("P:"+P);
						System.err.println("R:"+R);
						System.err.println("F:"+F);
						
						P = count_corr_span/count_pred;
						R = count_corr_span/count_expt;
						F = 2/(1/P+1/R);
						
						System.err.println("-SPAN ONLY-");
						System.err.println("P:"+P);
						System.err.println("R:"+R);
						System.err.println("F:"+F);
						
						

						count_corr = 0;
						count_corr_span = 0;
						count_pred = 0;
						count_expt = 0;
						
						for(int k = 0; k<spans_test.size(); k++){
							MentionFlatSemiInstance inst = new MentionFlatSemiInstance(k+1, spans_test.get(k), manager.getMentionTemplate());
							MentionFlatSemiInstance inst_unlabeled = inst.removeOutput();
							
							model.max(inst_unlabeled);
							
							UnlabeledTextSpan span = (UnlabeledTextSpan)inst_unlabeled.getSpan();
							count_corr += span.countCorrect();
							count_corr_span += span.countCorrect_span();
							count_pred += span.countPredicted();
							count_expt += span.countExpected();
							
							p.println(span);
							p.flush();
						}
						p.close();
						
						System.err.println(count_corr);
						System.err.println(count_corr_span);
						System.err.println(count_pred);
						System.err.println(count_expt);
						
						P = count_corr/count_pred;
						R = count_corr/count_expt;
						F = 2/(1/P+1/R);
						
						System.err.println("-FULL-");
						System.err.println("P:"+P);
						System.err.println("R:"+R);
						System.err.println("F:"+F);
						
						P = count_corr_span/count_pred;
						R = count_corr_span/count_expt;
						F = 2/(1/P+1/R);
						
						System.err.println("-SPAN ONLY-");
						System.err.println("P:"+P);
						System.err.println("R:"+R);
						System.err.println("F:"+F);
						
						lambda += 0.01;
					}
					
					long eTime_overall = System.currentTimeMillis();
					
					System.err.println("Overall time:"+(eTime_overall-bTime_overall)/1000.0+" secs.");
					
				}
				
			}
		}
		
	}
	
	public static ArrayList<LabeledTextSpan> readTextSpans(Scanner scan, IEManager manager, boolean isTrain) throws FileNotFoundException{
		
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
				int bIndex, eIndex, head_bIndex, head_eIndex;
				if(indices.length == 2){
					bIndex = Integer.parseInt(indices[0]);
					eIndex = Integer.parseInt(indices[1]);
					head_bIndex = Integer.parseInt(indices[0]);
					head_eIndex = Integer.parseInt(indices[1]);
				} else if(indices.length == 4){
					bIndex = Integer.parseInt(indices[0]);
					eIndex = Integer.parseInt(indices[1]);
					head_bIndex = Integer.parseInt(indices[2]);
					head_eIndex = Integer.parseInt(indices[3]);
				} else {
					throw new RuntimeException("The number of indices is "+indices.length);
				}
				String label = annotation[1];
				span.label(bIndex, eIndex, new Mention(bIndex, eIndex, head_bIndex, head_eIndex, manager.toMentionType(label)));
			}
			
			spans.add(span);
			
			if(words.length!=tags.length){
				throw new RuntimeException("The lengths between words and tags are not the same!");
			}
			
			scan.nextLine();
			num_instances++;
		}
		System.err.println("There are "+num_instances+" instances.");
		
//		if(isTrain){
//			ArrayList<String> funcwords = findFuncWords(spans);
//			WordUtil.setFunctionWords(funcwords);
//		}
//		
//		String ne_word_type = "NE_WORD_TYPE";
//		
//		for(LabeledTextSpan span : spans){
//			for(int i = 0; i<span.length(); i++){
//				AttributedWord word = span.getWord(i);
//				word.addAttribute(ne_word_type, WordUtil.getNEWordType(word.getForm()));
//			}
//		}
		
		for(LabeledTextSpan span : spans){
			span.expandAtt_WORD();
			span.expandAtt_POS();
//			span.expandAtt_BOW();
//			span.expandAtt_NER();
//			span.expandAtt_NE_WORD_TYPE();
		}
		
		return spans;
	}
	
}
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
package com.statnlp.ie.linear.latent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

import com.statnlp.commons.AttributedWord;
import com.statnlp.commons.WordUtil;
import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GenerativeNetworkModel;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.ie.mention.MentionLinearLatentInstance;
import com.statnlp.ie.io.EventExtractionReader;
import com.statnlp.ie.types.IEManager;
import com.statnlp.ie.types.LabeledTextSpan;
import com.statnlp.ie.types.Mention;
import com.statnlp.ie.types.UnlabeledTextSpan;

public class MentionLatentExtractionExpr_ACE {
	
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
	
	public static void main(String args[])throws IOException, ClassNotFoundException, InterruptedException{
		
//		IELinearLatentConfig._MAX_LBFGS_ITRS = 10;
		
		NetworkConfig._numThreads = Integer.parseInt(args[0]);
		
		String corpusName = args[1];
		//choose from ACE2004, ACE2005
		
		int num_itrs = 2000;
		
		System.err.println("OK20Jan1453");
		
		long bTime_overall = System.currentTimeMillis();
		
		for(String folder : new String[]{corpusName}){
			for(String subfolder : new String[]{"English"})
//			String subfolder = args[0];
			{
//				for(String c : new String[]{"0.1", "1", "0.01"}){
//				for(String c : new String[]{"0.01"}){
				for(String c : new String[]{"0.01"}){
					
					NetworkConfig.L2_REGULARIZATION_CONSTANT = Double.parseDouble(c);
					NetworkConfig.RANDOM_INIT_WEIGHT = true;
					NetworkConfig.RANDOM_INIT_FEATURE_SEED = 999;
					
					String filename_template = "data/"+folder+"/data/"+subfolder+"/template";
					
					NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
					
					IEManager manager = EventExtractionReader.readIEManager(filename_template);
					
					IELinearLatentFeatureManager fm = new IELinearLatentFeatureManager(new GlobalNetworkParam());
					
					System.err.println(Arrays.toString(manager.getMentionTemplate().getAllTypes()));
					
					File f = new File("experiments/mention/new_model/"+IELinearLatentConfig._type.name()+"/"+folder+"/"+subfolder+"/");
					f.mkdirs();
					
					String filename_input;
					Scanner scan;
					
					filename_input = "data/"+folder+"/data/"+subfolder+"/mention-standard/"+IELinearLatentConfig._type.name()+"/train.data";
					scan = new Scanner(new File(filename_input));
					ArrayList<LabeledTextSpan> spans_train = readTextSpans(scan, manager, true);

					int train_size = spans_train.size();
					
					IELinearLatentInstance[] instances = new IELinearLatentInstance[train_size];
					for(int k = 0; k<train_size; k++){
						MentionLinearLatentInstance inst = new MentionLinearLatentInstance(k+1, spans_train.get(k), manager.getMentionTemplate());
						instances[k] = inst;
					}
					
					IELinearLatentNetworkCompiler compiler = new IELinearLatentNetworkCompiler(manager.getMentionTemplate().getAllTypesExcludingStartAndFinish_arr());
					
					NetworkModel model = NetworkConfig.TRAIN_MODE_IS_GENERATIVE ? GenerativeNetworkModel.create(fm, compiler)
							: DiscriminativeNetworkModel.create(fm, compiler);
					
					model.train(instances, num_itrs);
					
					String filename_output = "experiments/mention/new_model/"+IELinearLatentConfig._type.name()+"/"+folder+"/"+subfolder+"/output.data";
					PrintWriter p = new PrintWriter(new File(filename_output));
					
					filename_input = "data/"+folder+"/data/"+subfolder+"/mention-standard/"+IELinearLatentConfig._type.name()+"/dev.data";
					scan = new Scanner(new File(filename_input));
					ArrayList<LabeledTextSpan> spans_dev = readTextSpans(scan, manager, false);
					
					filename_input = "data/"+folder+"/data/"+subfolder+"/mention-standard/"+IELinearLatentConfig._type.name()+"/test.data";
					scan = new Scanner(new File(filename_input));
					ArrayList<LabeledTextSpan> spans_test = readTextSpans(scan, manager, false);
					
					System.err.println("There are "+instances.length+" training instances.");
					System.err.println("There are "+spans_test.size()+" test instances.");
					
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
					
					MentionLinearLatentInstance[] instances_test;
					
					instances_test = new MentionLinearLatentInstance[train_size];
					for(int k = 0; k<train_size; k++){
						instances_test[k] = new MentionLinearLatentInstance(k+1, spans_train.get(k), manager.getMentionTemplate());
						instances_test[k].removeOutput();
						instances_test[k].setUnlabeled();
					}
					
					Instance[] instances_outputs;
					
					instances_outputs = model.decode(instances_test);
					for(int k = 0; k<train_size; k++){
						MentionLinearLatentInstance inst = (MentionLinearLatentInstance)instances_outputs[k];
						
						UnlabeledTextSpan span = inst.getInput();
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
					
//					P = count_corr/count_pred;
//					R = count_corr/count_expt;
//					F = 2/(1/P+1/R);
//					
//					System.err.println("-FULL-");
//					System.err.println("P:"+P);
//					System.err.println("R:"+R);
//					System.err.println("F:"+F);
					
					P = count_corr_span/count_pred;
					R = count_corr_span/count_expt;
					F = 2/(1/P+1/R);
					
					System.err.println("-SPAN ONLY-");
					System.err.println("P:"+P);
					System.err.println("R:"+R);
					System.err.println("F:"+F);
					
					double lambda = 0;
					
					//dirty trick..
					int f1 = fm.getParam_G().toFeature("MENTION_PENALTY", "MENTION","MENTION");
					double weight1 = fm.getParam_G().getWeight(f1);
					
					for(int j = 0; j<100; j++){
						
						System.err.println("lambda="+lambda);
						
						fm.getParam_G().setWeight(f1, weight1+lambda);
						
						System.err.println("mention penalty="+fm.getParam_G().getWeight(f1));
						
						{
							count_corr = 0;
							count_corr_span = 0;
							count_pred = 0;
							count_expt = 0;
							
							instances_test = new MentionLinearLatentInstance[spans_dev.size()];
							for(int k = 0; k<spans_dev.size(); k++){
								instances_test[k] = new MentionLinearLatentInstance(k+1, spans_dev.get(k), manager.getMentionTemplate());
								instances_test[k].removeOutput();
								instances_test[k].setUnlabeled();
							}
							
							instances_outputs = model.decode(instances_test);
							for(int k = 0; k<instances_outputs.length; k++){
								MentionLinearLatentInstance inst = (MentionLinearLatentInstance)instances_outputs[k];
								
								UnlabeledTextSpan span = inst.getInput();
								count_corr += span.countCorrect();
								count_corr_span += span.countCorrect_span();
								count_pred += span.countPredicted();
								count_expt += span.countExpected();
								
								p.println(span);
								p.flush();
							}
							p.close();
							
							System.err.println("==DEV SET==");
							System.err.println("#corr      ="+count_corr);
							System.err.println("#corr(span)="+count_corr_span);
							System.err.println("#pred      ="+count_pred);
							System.err.println("#expt      ="+count_expt);
							
//							P = count_corr/count_pred;
//							R = count_corr/count_expt;
//							F = 2/(1/P+1/R);
//							
//							System.err.println("-FULL-");
//							System.err.println("P:"+P);
//							System.err.println("R:"+R);
//							System.err.println("F:"+F);
							
							P = count_corr_span/count_pred;
							R = count_corr_span/count_expt;
							F = 2/(1/P+1/R);
							
							System.err.println("-SPAN ONLY-");
							System.err.println("P:"+P);
							System.err.println("R:"+R);
							System.err.println("F:"+F);
						}
						
						{
							count_corr = 0;
							count_corr_span = 0;
							count_pred = 0;
							count_expt = 0;
							
							instances_test = new MentionLinearLatentInstance[spans_test.size()];
							for(int k = 0; k<instances_test.length; k++){
								instances_test[k] = new MentionLinearLatentInstance(k+1, spans_test.get(k), manager.getMentionTemplate());
								instances_test[k].removeOutput();
								instances_test[k].setUnlabeled();
							}
							
							instances_outputs = model.decode(instances_test);
							for(int k = 0; k<instances_outputs.length; k++){
								MentionLinearLatentInstance inst = (MentionLinearLatentInstance)instances_outputs[k];
								
								UnlabeledTextSpan span = inst.getInput();
								count_corr += span.countCorrect();
								count_corr_span += span.countCorrect_span();
								count_pred += span.countPredicted();
								count_expt += span.countExpected();
								
								p.println(span);
								p.flush();
							}
							p.close();
							
							System.err.println("==TEST SET==");
							System.err.println("#corr      ="+count_corr);
							System.err.println("#corr(span)="+count_corr_span);
							System.err.println("#pred      ="+count_pred);
							System.err.println("#expt      ="+count_expt);
							
//							P = count_corr/count_pred;
//							R = count_corr/count_expt;
//							F = 2/(1/P+1/R);
//							
//							System.err.println("-FULL-");
//							System.err.println("P:"+P);
//							System.err.println("R:"+R);
//							System.err.println("F:"+F);
							
							P = count_corr_span/count_pred;
							R = count_corr_span/count_expt;
							F = 2/(1/P+1/R);
							
							System.err.println("-SPAN ONLY-");
							System.err.println("P:"+P);
							System.err.println("R:"+R);
							System.err.println("F:"+F);
						}
//						
						lambda += 0.1;
					}
//					
					
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
		
		if(isTrain){
			ArrayList<String> funcwords = findFuncWords(spans);
			WordUtil.setFunctionWords(funcwords);
		}
		
		String ne_word_type = "NE_WORD_TYPE";
		
		for(LabeledTextSpan span : spans){
			for(int i = 0; i<span.length(); i++){
				AttributedWord word = span.getWord(i);
				word.addAttribute(ne_word_type, WordUtil.getNEWordType(word.getName()));
			}
		}
		
		for(LabeledTextSpan span : spans){
			span.expandAtt_WORD();
			span.expandAtt_POS();
			span.expandAtt_BOW();
//			span.expandAtt_NER();
			span.expandAtt_NE_WORD_TYPE();
		}
		
		return spans;
	}
	
}
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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;

import com.statnlp.commons.AttributedWord;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.GlobalNetworkParam.TRAIN_MODE;
import com.statnlp.ie.linear.semi.IELinearSemiFeatureManager;
import com.statnlp.ie.linear.semi.IELinearSemiInstance;
import com.statnlp.ie.linear.semi.IELinearSemiModel;
import com.statnlp.ie.linear.semi.IELinearSemiConfig;
import com.statnlp.ie.io.EventExtractionReader;
import com.statnlp.ie.io.IEDocumentsReader;
import com.statnlp.ie.types.IEManager;
import com.statnlp.ie.types.LabeledTextSpan;
import com.statnlp.ie.types.Mention;
import com.statnlp.ie.types.UnlabeledTextSpan;

public class MentionExtractionExpr_semi {
	
	public static void main(String args[])throws IOException{
		
		long bTime_overall = System.currentTimeMillis();
		
		for(String folder : new String[]{"ACE2004"}){
			for(String subfolder : new String[]{"English"}){
				
				String filename_template = "data/ace.template";
				
				NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
				
				IEManager manager = EventExtractionReader.readIEManager(filename_template);
				
				GlobalNetworkParam param = new GlobalNetworkParam(TRAIN_MODE.DISCRIMINATIVE);
				
				IELinearSemiFeatureManager fm = new IELinearSemiFeatureManager(param);
				
				File f = new File("experiments/mention/new_model/"+IELinearSemiConfig._type.name()+"/"+folder+"/"+subfolder+"/");
				f.mkdirs();
				
				String filename_output = "experiments/mention/new_model/"+IELinearSemiConfig._type.name()+"/"+folder+"/"+subfolder+"/output.data";
				PrintWriter p = new PrintWriter(new File(filename_output));
				
				String filename_input;
				Scanner scan;
				
				filename_input = "data/"+folder+"/data/"+subfolder+"/mention-standard/"+IELinearSemiConfig._type.name()+"/train.data";
				scan = new Scanner(new File(filename_input));
				ArrayList<LabeledTextSpan> spans_train = readTextSpans(scan, manager);
				
				filename_input = "data/"+folder+"/data/"+subfolder+"/mention-standard/"+IELinearSemiConfig._type.name()+"/test.data";
				scan = new Scanner(new File(filename_input));
				ArrayList<LabeledTextSpan> spans_test = readTextSpans(scan, manager);
				
				int train_size = spans_train.size();
				
				ArrayList<IELinearSemiInstance> instances = new ArrayList<IELinearSemiInstance>();
				for(int k = 0; k<train_size; k++){
					MentionLinearSemiInstance inst = new MentionLinearSemiInstance(k+1, spans_train.get(k), manager.getMentionTemplate());
					instances.add(inst);
					
//					ArrayList<Mention> all_mentions = ((LabeledTextSpan)inst.getSpan()).getAllMentions();

//					//check
//					for(int i = 0; i<all_mentions.size(); i++){
//						Mention m1 = all_mentions.get(i);
//						for(int j = i+1; j<all_mentions.size(); j++){
//							Mention m2 = all_mentions.get(j);
//							if(m1.equals(m2)){
//								throw new RuntimeException("wowo..");
//							}
//						}
//					}
				}
				
				System.err.println("There are "+instances.size()+" training instances.");
				System.err.println("There are "+spans_test.size()+" test instances.");
				
				IELinearSemiModel model = new IELinearSemiModel(fm, manager.getMentionTemplate().getAllTypesExcludingStartAndFinish_arr());
				
				model.train(instances);
				
				double count_corr;
				double count_pred;
				double count_corr_span;
				double count_expt;
				double count_supported;
				
				double P;
				double R;
				double F;
				
				count_corr = 0;
				count_corr_span = 0;
				count_pred = 0;
				count_expt = 0;
				count_supported = 0;
				
				for(int k = 0; k<train_size; k++){
					MentionLinearSemiInstance inst = new MentionLinearSemiInstance(k+1, spans_train.get(k), manager.getMentionTemplate());
					MentionLinearSemiInstance inst_unlabeled = inst.removeOutput();
					
					model.max(inst_unlabeled);
					
					UnlabeledTextSpan span = (UnlabeledTextSpan)inst_unlabeled.getSpan();
					count_corr += span.countCorrect();
					count_corr_span += span.countCorrect_span();
					count_pred += span.countPredicted();
					count_expt += span.countExpected();
					count_supported += span.countSupported();
					
					p.println(span);
					p.flush();
				}
				p.close();

				System.err.println("CORR(STRONG):\t"+count_corr);
				System.err.println("CORR(WEAK):\t"+count_corr_span);
				System.err.println("PRED:\t"+count_pred);
				System.err.println("EXPET:\t"+count_expt);
				System.err.println("SUPPORTED:\t"+count_supported);

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
				count_supported = 0;
				
				for(int k = 0; k<spans_test.size(); k++){
					MentionLinearSemiInstance inst = new MentionLinearSemiInstance(k+1, spans_test.get(k), manager.getMentionTemplate());
					MentionLinearSemiInstance inst_unlabeled = inst.removeOutput();
					
					model.max(inst_unlabeled);
					
					UnlabeledTextSpan span = (UnlabeledTextSpan)inst_unlabeled.getSpan();
					count_corr += span.countCorrect();
					count_corr_span += span.countCorrect_span();
					count_pred += span.countPredicted();
					count_expt += span.countExpected();
					count_supported += span.countSupported();
					
					p.println(span);
					p.flush();
				}
				p.close();

				System.err.println("CORR(STRONG):\t"+count_corr);
				System.err.println("CORR(WEAK):\t"+count_corr_span);
				System.err.println("PRED:\t"+count_pred);
				System.err.println("EXPET:\t"+count_expt);
				System.err.println("SUPPORTED:\t"+count_supported);

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
				
				long eTime_overall = System.currentTimeMillis();
				
				System.err.println("Overall time:"+(eTime_overall-bTime_overall)/1000.0+" secs.");
				
			}
		}
		
	}
	
	public static ArrayList<LabeledTextSpan> readTextSpans(Scanner scan, IEManager manager) throws FileNotFoundException{
		
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
			span.expandAtt_POS();
			
			if(words.length!=tags.length){
				throw new RuntimeException("The lengths between words and tags are not the same!");
			}
			
			scan.nextLine();
			num_instances++;
		}
		System.err.println("There are "+num_instances+" instances.");
		
		return spans;
	}
	

	public static void main5(String args[])throws IOException{
		
		long bTime_overall = System.currentTimeMillis();
		
		String filename_template = "data/ace.template";
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		
		IEManager manager = EventExtractionReader.readIEManager(filename_template);
		
		GlobalNetworkParam param = new GlobalNetworkParam(TRAIN_MODE.DISCRIMINATIVE);
		
		IELinearSemiFeatureManager fm = new IELinearSemiFeatureManager(param);
		
		System.err.println("There are "+(manager.getMentionTemplate().getAllTypes().length)+" mentions.");
		
		long bTime = System.currentTimeMillis();
		
		String dir_name = "data/ace2004_normalized/nw/";
		
		ArrayList<ArrayList<LabeledTextSpan>> spanList = IEDocumentsReader.readLabeledSpansGroupedByDocs(dir_name, manager);
		Random rand = new Random(1234);
		ArrayList<LabeledTextSpan> spans_train = new ArrayList<LabeledTextSpan>();
		ArrayList<LabeledTextSpan> spans_test = new ArrayList<LabeledTextSpan>();
		
		for(int k = 0; k<spanList.size(); k++){
			ArrayList<LabeledTextSpan> spans = spanList.get(k);
			if(rand.nextDouble()<=.7){
				spans_train.addAll(spans);
				if(spans_train.size()>10)
					break;
			} else {
				spans_test.addAll(spans);
			}
		}
		
		int num_mentions;
		num_mentions = 0;
		for(int k = 0; k<spans_train.size(); k++){
			num_mentions += spans_train.get(k).countAllLabels();
		}
		System.err.println("##Train:"+spans_train.size()+".#mentions:"+num_mentions);
		
		num_mentions = 0;
		for(int k = 0; k<spans_test.size(); k++){
			num_mentions += spans_test.get(k).countAllLabels();
		}
		System.err.println("##Test:"+spans_test.size()+".#mentions:"+num_mentions);
//		System.exit(1);
		
		ArrayList<IELinearSemiInstance> instances = new ArrayList<IELinearSemiInstance>();
		for(int k = 0; k<spans_train.size(); k++){
			MentionLinearSemiInstance inst = new MentionLinearSemiInstance(k+1, spans_train.get(k), manager.getMentionTemplate());
			instances.add(inst);
		}
		
		IELinearSemiModel model = new IELinearSemiModel(fm, manager.getMentionTemplate().getAllTypesExcludingStartAndFinish_arr());
		
		model.train(instances);
		
		long eTime = System.currentTimeMillis();
		
		System.err.println((eTime-bTime)/1000.0+" secs.");

		double count_corr;
		double count_corr_span;
		double count_pred;
		double count_expt;
		
		double P;
		double R;
		double F;
		
		count_corr = 0;
		count_corr_span = 0;
		count_pred = 0;
		count_expt = 0;
		
		for(int k = 0; k<spans_train.size(); k++){
			MentionLinearSemiInstance inst = new MentionLinearSemiInstance(k+1, spans_train.get(k), manager.getMentionTemplate());
			MentionLinearSemiInstance inst_unlabeled = inst.removeOutput();
			
			model.max(inst_unlabeled);
			
			UnlabeledTextSpan span = (UnlabeledTextSpan)inst_unlabeled.getSpan();
			count_corr += span.countCorrect();
			count_corr_span += span.countCorrect_span();
			count_pred += span.countPredicted();
			count_expt += span.countExpected();
			
		}
		
		System.err.println(count_corr);
		System.err.println(count_corr_span);
		System.err.println(count_pred);
		System.err.println(count_expt);
		
		P = count_corr/count_pred;
		R = count_corr/count_expt;
		F = 2/(1/P+1/R);
		
		System.err.println("-FULL EVALUATION-");
		System.err.println("P:"+P);
		System.err.println("R:"+R);
		System.err.println("F:"+F);
		
		P = count_corr_span/count_pred;
		R = count_corr_span/count_expt;
		F = 2/(1/P+1/R);
		
		System.err.println("-SPAN ONLY EVALUATION-");
		System.err.println("P:"+P);
		System.err.println("R:"+R);
		System.err.println("F:"+F);
		
		count_corr = 0;
		count_corr_span = 0;
		count_pred = 0;
		count_expt = 0;
		
		for(int k = 0; k<spans_test.size(); k++){
			MentionLinearSemiInstance inst = new MentionLinearSemiInstance(k+1, spans_test.get(k), manager.getMentionTemplate());
			MentionLinearSemiInstance inst_unlabeled = inst.removeOutput();
			
			model.max(inst_unlabeled);
			
			UnlabeledTextSpan span = (UnlabeledTextSpan)inst_unlabeled.getSpan();
			count_corr += span.countCorrect();
			count_corr_span += span.countCorrect_span();
			count_pred += span.countPredicted();
			count_expt += span.countExpected();
			
		}

		System.err.println(count_corr);
		System.err.println(count_corr_span);
		System.err.println(count_pred);
		System.err.println(count_expt);
		
		P = count_corr/count_pred;
		R = count_corr/count_expt;
		F = 2/(1/P+1/R);
		
		System.err.println("-FULL EVALUATION-");
		System.err.println("P:"+P);
		System.err.println("R:"+R);
		System.err.println("F:"+F);
		
		P = count_corr_span/count_pred;
		R = count_corr_span/count_expt;
		F = 2/(1/P+1/R);
		
		System.err.println("-SPAN ONLY EVALUATION-");
		System.err.println("P:"+P);
		System.err.println("R:"+R);
		System.err.println("F:"+F);
		
		long eTime_overall = System.currentTimeMillis();
		
		System.err.println("Overall time:"+(eTime_overall-bTime_overall)/1000.0+" secs.");
		
	}
	
	public static void main3(String args[])throws IOException{
		
		long bTime_overall = System.currentTimeMillis();
		
		String event = "meet";
		String filename_template = "data/ace.template";
		String filename_train_data = "data/ace-"+event+".train.data";
		String filename_test_data = "data/ace-"+event+".test.data";
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		
		IELinearSemiFeatureManager fm = new IELinearSemiFeatureManager(new GlobalNetworkParam(TRAIN_MODE.DISCRIMINATIVE));
		
		IEManager manager = EventExtractionReader.readIEManager(filename_template);
		
		System.err.println("There are "+(manager.getMentionTemplate().getAllTypes().length)+" mentions.");
		
		long bTime = System.currentTimeMillis();
		
		ArrayList<LabeledTextSpan> spans = EventExtractionReader.readLabeledTextSpans_Mention(manager, filename_train_data);
		ArrayList<UnlabeledTextSpan> spans_u = EventExtractionReader.readUnlabeledTextSpans_Mention(manager, filename_test_data);
		
		System.err.println(spans.size()+"\t"+spans_u.size());
		
		HashSet<String> lines = new HashSet<String>();
		for(int k = 0; k<spans.size(); k++){
			String line = spans.get(k).toLine();
			lines.add(line);
		}
		int count_overlap = 0;
		for(int k = 0; k<spans_u.size(); k++){
			String line = spans_u.get(k).toLine();
			if(lines.contains(line)){
				count_overlap ++;
				spans_u.remove(k);
				k--;
			}
		}
		System.err.println(count_overlap+"\t"+spans.size()+"\t"+spans_u.size());

		count_overlap = 0;
		for(int k = 0; k<spans_u.size(); k++){
			String line = spans_u.get(k).toLine();
			if(lines.contains(line)){
				count_overlap ++;
			}
		}
		
		System.err.println(count_overlap+"\t"+spans.size()+"\t"+spans_u.size());
		
		ArrayList<IELinearSemiInstance> instances = new ArrayList<IELinearSemiInstance>();
		for(int k = 0; k<spans.size(); k++){
			MentionLinearSemiInstance inst = new MentionLinearSemiInstance(k+1, spans.get(k), manager.getMentionTemplate());
			instances.add(inst);
		}
		
		IELinearSemiModel model = new IELinearSemiModel(fm, manager.getMentionTemplate().getAllTypesExcludingStartAndFinish_arr());
		
		model.train(instances);
		
		long eTime = System.currentTimeMillis();
		
		System.err.println((eTime-bTime)/1000.0+" secs.");
		
		double count_corr = 0;
		double count_pred = 0;
		double count_expt = 0;
		
		
		for(int k = 0; k<spans_u.size(); k++){
			MentionLinearSemiInstance inst = new MentionLinearSemiInstance(k+1, spans_u.get(k), manager.getMentionTemplate());
			MentionLinearSemiInstance inst_unlabeled = inst.removeOutput();
			
			model.max(inst_unlabeled);
			
			UnlabeledTextSpan span = (UnlabeledTextSpan)inst_unlabeled.getSpan();
			count_corr += span.countCorrect();
			count_pred += span.countPredicted();
			count_expt += span.countExpected();
		}
		
		System.err.println(count_corr);
		System.err.println(count_pred);
		System.err.println(count_expt);
		
		double P = count_corr/count_pred;
		double R = count_corr/count_expt;
		double F = 2/(1/P+1/R);
		
		System.err.println("P:"+P);
		System.err.println("R:"+R);
		System.err.println("F:"+F);
		
		long eTime_overall = System.currentTimeMillis();
		
		System.err.println("Overall time:"+(eTime_overall-bTime_overall)/1000.0+" secs.");
	}
	
}
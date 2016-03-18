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
import java.util.Scanner;

import com.statnlp.commons.AttributedWord;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.ie.linear.IELinearConfig;
import com.statnlp.ie.linear.IELinearFeatureManager;
import com.statnlp.ie.linear.IELinearInstance;
import com.statnlp.ie.linear.IELinearModel;
import com.statnlp.ie.linear.MentionLinearInstance;
import com.statnlp.ie.io.EventExtractionReader;
import com.statnlp.ie.types.IEManager;
import com.statnlp.ie.types.LabeledTextSpan;
import com.statnlp.ie.types.Mention;
import com.statnlp.ie.types.UnlabeledTextSpan;

public class MentionExtractionExpr_new {
	
	public static void main(String args[])throws IOException{
		
		int num_itrs = 1000;
		
		long bTime_overall = System.currentTimeMillis();
		
		for(String folder : new String[]{"ACE2004"}){
			for(String subfolder : new String[]{"English"}){
				
				String filename_template = "data/ace.template";
				
				NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
				
				IEManager manager = EventExtractionReader.readIEManager(filename_template);
				
				GlobalNetworkParam param = new GlobalNetworkParam();
				
				IELinearFeatureManager fm = new IELinearFeatureManager(param);
				
				File f = new File("experiments/mention/new_model/"+IELinearConfig._type.name()+"/"+folder+"/"+subfolder+"/");
				f.mkdirs();
				
				String filename_output = "experiments/mention/new_model/"+IELinearConfig._type.name()+"/"+folder+"/"+subfolder+"/output.data";
				PrintWriter p = new PrintWriter(new File(filename_output));
				
				String filename_input;
				Scanner scan;
				
				filename_input = "data/"+folder+"/data/"+subfolder+"/mention-standard/"+IELinearConfig._type.name()+"/train.data";
				scan = new Scanner(new File(filename_input));
				ArrayList<LabeledTextSpan> spans_train = readTextSpans(scan, manager);
				
				filename_input = "data/"+folder+"/data/"+subfolder+"/mention-standard/"+IELinearConfig._type.name()+"/test.data";
				scan = new Scanner(new File(filename_input));
				ArrayList<LabeledTextSpan> spans_test = readTextSpans(scan, manager);
				
				int train_size = spans_train.size();
				
				ArrayList<IELinearInstance> instances = new ArrayList<IELinearInstance>();
				for(int k = 0; k<train_size; k++){
					MentionLinearInstance inst = new MentionLinearInstance(k+1, spans_train.get(k), manager.getMentionTemplate());
					instances.add(inst);
				}
				
				System.err.println("There are "+instances.size()+" training instances.");
				System.err.println("There are "+spans_test.size()+" test instances.");
				
				IELinearModel model = new IELinearModel(fm, manager.getMentionTemplate().getAllTypesExcludingStartAndFinish_arr());
				
				model.train(instances, num_itrs);
				
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
					MentionLinearInstance inst = new MentionLinearInstance(k+1, spans_train.get(k), manager.getMentionTemplate());
					inst.removeOutput();
					
					model.max(inst);
					
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
					MentionLinearInstance inst = new MentionLinearInstance(k+1, spans_test.get(k), manager.getMentionTemplate());
					inst.removeOutput();
					
					model.max(inst);
					
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
	
}
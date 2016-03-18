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
package com.statnlp.ie.linear;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;

import com.statnlp.commons.AttributedWord;
import com.statnlp.commons.WordUtil;
import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GenerativeNetworkModel;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.ie.types.IEManager;
import com.statnlp.ie.types.LabeledTextSpan;
import com.statnlp.ie.types.Mention;
import com.statnlp.ie.types.UnlabeledTextSpan;

public class MentionExtractionTuner {

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
	
	public static String _corpusName = "xxx";
	
	@SuppressWarnings("unchecked")
	public static void main(String args[])throws IOException, ClassNotFoundException, InterruptedException{
		
		NetworkConfig._numThreads = Integer.parseInt(args[0]);
		
		String corpusName = args[1];
		//choose from ACE2004, ACE2005, GENIA, CONLL2003
		
		_corpusName = corpusName;
		
//		String c = args[2];//Double.parseDouble(args[3]);
		String[] cv = args[2].split(",");//Double.parseDouble(args[3]);
		
		String subfolder = args[3];
		
		System.err.println("OK05Feb1034");
		
		long bTime_overall = System.currentTimeMillis();
		
		for(String folder : new String[]{corpusName}){

			for(String c : cv){

				String filename_model = "experiments/mention/model/"+IELinearConfig._type.name()+"/"+folder+"/"+subfolder+"/linear-"+c+".model";
				
				NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
				
				System.err.println("Loading Model...");
				ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename_model));
				GlobalNetworkParam param = (GlobalNetworkParam)in.readObject();
				IEManager manager = (IEManager)in.readObject();
				WordUtil._func_words = (HashSet<String>)in.readObject();
				in.close();
				
				System.err.println("Model loaded!");
				
//				System.err.println("Is locked?"+param.isLocked());
				
				FeatureManager fm;
				
				if(_corpusName.equals("GENIA")){
					fm = new IELinearFeatureManager_GENIA(param);
				} else {
					fm = new IELinearFeatureManager(param);
				}
				
				System.err.println(Arrays.toString(manager.getMentionTemplate().getAllTypes()));
				
				IELinearNetworkCompiler compiler = new IELinearNetworkCompiler(manager.getMentionTemplate().getAllTypesExcludingStartAndFinish_arr());
				
				NetworkModel model = NetworkConfig.TRAIN_MODE_IS_GENERATIVE ? GenerativeNetworkModel.create(fm, compiler)
						: DiscriminativeNetworkModel.create(fm, compiler);
				
				String filename_input;
				Scanner scan;
				
				filename_input = "data/"+folder+"/data/"+subfolder+"/mention-standard/"+IELinearConfig._type.name()+"/dev.data";
				scan = new Scanner(new File(filename_input));
				ArrayList<LabeledTextSpan> spans_dev = readTextSpans(scan, manager, false);
				
				File f = new File("experiments/mention/model/"+IELinearConfig._type.name()+"/"+folder+"/"+subfolder+"/");
				f.mkdirs();
				
				String filename_output = "experiments/mention/model/"+IELinearConfig._type.name()+"/"+folder+"/"+subfolder+"/output-dev.data";
				PrintWriter p = new PrintWriter(new File(filename_output));
				
				System.err.println("There are "+spans_dev.size()+" development instances.");
				
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
				
				MentionLinearInstance[] instances_test;
				Instance[] instances_outputs;
				
				double lambda = 0;
				
				//dirty trick..
				int f1 = fm.getParam_G().toFeature("MENTION_PENALTY", "MENTION","MENTION");
				double weight1 = fm.getParam_G().getWeight(f1);
				
				double bestF = Double.NEGATIVE_INFINITY;
				double bestMentionPenalty = weight1;
				
				for(int j = 0; j<1000; j++){
					
					System.err.println("lambda="+lambda);
					
					double MP = weight1+lambda;
					
					fm.getParam_G().setWeight(f1, MP);
					
					System.err.println("mention penalty="+fm.getParam_G().getWeight(f1));
					
					count_corr = 0;
					count_corr_span = 0;
					count_pred = 0;
					count_expt = 0;
					
					instances_test = new MentionLinearInstance[spans_dev.size()];
					for(int k = 0; k<spans_dev.size(); k++){
						instances_test[k] = new MentionLinearInstance(k+1, spans_dev.get(k), manager.getMentionTemplate());
						instances_test[k].removeOutput();
						instances_test[k].setUnlabeled();
					}
					
					long bTime = System.currentTimeMillis();
					instances_outputs = model.decode(instances_test);
					long time = System.currentTimeMillis() - bTime;
					
					double time_sec = time/1000.0;
					
					double numWords = 0;
					
					for(int k = 0; k<instances_outputs.length; k++){
						MentionLinearInstance inst = (MentionLinearInstance)instances_outputs[k];
						
						UnlabeledTextSpan span = inst.getInput();
						count_corr += span.countCorrect();
						count_corr_span += span.countCorrect_span();
						count_pred += span.countPredicted();
						count_expt += span.countExpected();
						
						p.println(span);
						p.flush();
						
						numWords += inst.getInput().length();
					}
					p.close();
					
					System.err.println("==DEV SET==");
					System.err.println("#corr      ="+count_corr);
					System.err.println("#corr(span)="+count_corr_span);
					System.err.println("#pred      ="+count_pred);
					System.err.println("#expt      ="+count_expt);
					System.err.println("#words/sec.="+(numWords/time_sec));
					System.err.println("#insts/sec.="+(instances_outputs.length/time_sec));
					
					P = count_corr/count_pred;
					R = count_corr/count_expt;
					F = 2/(1/P+1/R);
					
					System.err.println("-FULL-");
					System.err.println("Prec:"+P+"\t"+"Rec:"+R+"\t"+"F:"+F);
					
					P = count_corr_span/count_pred;
					R = count_corr_span/count_expt;
					F = 2/(1/P+1/R);
					
					System.err.println("-SPAN ONLY-");
					System.err.println("Prec:"+P+"\t"+"Rec:"+R+"\t"+"F:"+F);
					
					if(F>bestF){
						bestF = F;
						bestMentionPenalty = MP;
					} else if(F/bestF<0.8){
						break;
					}
					
					lambda += 0.01;
					
				}
				
				long eTime_overall = System.currentTimeMillis();
				System.err.println("Overall time:"+(eTime_overall-bTime_overall)/1000.0+" secs.");
				
				System.err.println("bestMentionPenalty="+bestMentionPenalty);
				
				PrintWriter p0 = new PrintWriter(new File("experiments/mention/model/"+IELinearConfig._type.name()+"/"+folder+"/"+subfolder+"/linear-"+c+".model.MP"));
				p0.println(bestMentionPenalty);
				p0.flush();
				p0.close();
				
			}
			
		}
		
	}
	
	private static void extractPrefixes(AttributedWord word){
		String form = word.getName();
		for(int k = 1; k<Math.min(form.length(), 5); k++){
			String prefix = form.substring(0, k);
			word.addAttribute("prefix-"+k, prefix);
		}
	}
	
	private static void extractSuffixes(AttributedWord word){
		String form = word.getName();
		for(int k = 1; k<Math.min(form.length(), 5); k++){
			String suffix = form.substring(form.length()-k, form.length());
			word.addAttribute("suffix-"+k, suffix);
		}
	}
	
	public static ArrayList<LabeledTextSpan> readTextSpans(Scanner scan, IEManager manager, boolean isTrain) throws FileNotFoundException{
		
		ArrayList<LabeledTextSpan> spans = new ArrayList<LabeledTextSpan>();
		
		int num_instances = 0;
		while(scan.hasNextLine()){
			String[] words = scan.nextLine().split("\\s");
			String[] tags;
			if(_corpusName.equals("GENIA")){
				tags = new String[words.length];
			} else {
				tags = scan.nextLine().split("\\s");
			}
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
				if(_corpusName.equals("CONLL2003")){
					String[] pos_chunk = tags[k].split("\\|");
					aws[k].addAttribute("POS", pos_chunk[0]);
					aws[k].addAttribute("Chunk", pos_chunk[1]);
					
					extractPrefixes(aws[k]);
					extractSuffixes(aws[k]);
				}
				else if(!_corpusName.equals("GENIA")){
					aws[k].addAttribute("POS", tags[k]);
				}
				
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
			
			if(!_corpusName.equals("GENIA")){
				if(words.length!=tags.length){
					throw new RuntimeException("The lengths between words and tags are not the same!");
				}
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
			if(_corpusName.equals("GENIA")){
				span.expandAtt_WORD_simple();
			} else{
				span.expandAtt_WORD();
				span.expandAtt_POS();
				span.expandAtt_BOW();
			}
//			span.expandAtt_NER();
			span.expandAtt_NE_WORD_TYPE();
		}
		
		return spans;
	}
	
}
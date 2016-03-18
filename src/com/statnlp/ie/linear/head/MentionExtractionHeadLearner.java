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
package com.statnlp.ie.linear.head;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

import com.statnlp.commons.AttributedWord;
import com.statnlp.commons.WordUtil;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GenerativeNetworkModel;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.ie.io.EventExtractionReader;
import com.statnlp.ie.types.IEManager;
import com.statnlp.ie.types.LabeledTextSpan;
import com.statnlp.ie.types.Mention;

public class MentionExtractionHeadLearner {
	
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
	
	public static void main(String args[])throws IOException, ClassNotFoundException, InterruptedException{
		
		NetworkConfig._numThreads = Integer.parseInt(args[0]);
		
		String[] corpusNames = args[1].split(",");
		//choose from ACE2004, ACE2005, GENIA, CONLL2003
		
		int num_itrs = Integer.parseInt(args[2]);
		
		String[] cv = args[3].split(",");//Double.parseDouble(args[3]);
		
		String[] subfolders = args[4].split(",");
		
		System.err.println("OK04Feb2234");
		
		long bTime_overall = System.currentTimeMillis();
		
		for(String folder : corpusNames){
			
			_corpusName = folder;
			
			for(String subfolder : subfolders)
			{
				for(String c : cv){
					
//					System.err.println("["+c+"]");
//					if(true) continue;
					
					NetworkConfig.L2_REGULARIZATION_CONSTANT = Double.parseDouble(c);
					
					String filename_template = "data/"+folder+"/data/"+subfolder+"/template";
					
					NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
					
					IEManager manager = EventExtractionReader.readIEManager(filename_template);
					
					IELinearHeadFeatureManager fm = new IELinearHeadFeatureManager(new GlobalNetworkParam());
					
					System.err.println(Arrays.toString(manager.getMentionTemplate().getAllTypes()));
					
					File f = new File("experiments/mention/model/"+IELinearHeadConfig._type.name()+"/"+folder+"/"+subfolder+"/");
					f.mkdirs();
					
					String filename_input;
					String filename_model;
					Scanner scan;
					
					filename_input = "data/"+folder+"/data/"+subfolder+"/mention-standard/"+IELinearHeadConfig._type.name()+"/train.data";
					filename_model = "experiments/mention/model/"+IELinearHeadConfig._type.name()+"/"+folder+"/"+subfolder+"/linear-head-"+c+".model";
					
					scan = new Scanner(new File(filename_input));
					ArrayList<LabeledTextSpan> spans_train = readTextSpans(scan, manager, true);

					int train_size = spans_train.size();
					
					IELinearHeadInstance[] instances = new IELinearHeadInstance[train_size];
					for(int k = 0; k<train_size; k++){
						MentionLinearHeadInstance inst = new MentionLinearHeadInstance(k+1, spans_train.get(k), manager.getMentionTemplate());
						instances[k] = inst;
					}
					
					IELinearHeadNetworkCompiler compiler = new IELinearHeadNetworkCompiler(manager.getMentionTemplate().getAllTypesExcludingStartAndFinish_arr());
					
					NetworkModel model = NetworkConfig.TRAIN_MODE_IS_GENERATIVE ? GenerativeNetworkModel.create(fm, compiler)
							: DiscriminativeNetworkModel.create(fm, compiler);
					
					model.train(instances, num_itrs);
					
					long eTime_overall = System.currentTimeMillis();
					System.err.println("Overall time:"+(eTime_overall-bTime_overall)/1000.0+" secs.");

					System.err.println("Learning completes. Overall time:"+(eTime_overall-bTime_overall)/1000.0+" secs.");
					
					System.err.println("Saving Model...");
					ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename_model));
					out.writeObject(fm.getParam_G());
					out.flush();
					out.writeObject(manager);
					out.flush();
					out.writeObject(WordUtil._func_words);
					out.flush();
					out.close();
					System.err.println("Model Saved.");
						
				}
				
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
			}
			span.expandAtt_BOW();
//			span.expandAtt_NER();
			span.expandAtt_NE_WORD_TYPE();
		}
		
		return spans;
	}
	
}
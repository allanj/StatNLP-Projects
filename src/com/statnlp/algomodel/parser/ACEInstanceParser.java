package com.statnlp.algomodel.parser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

import com.statnlp.algomodel.AlgoModel;
import com.statnlp.commons.AttributedWord;
import com.statnlp.commons.WordUtil;
import com.statnlp.ie.linear.IELinearInstance;
import com.statnlp.ie.linear.MentionLinearInstance;
import com.statnlp.ie.types.IEManager;
import com.statnlp.ie.types.LabeledTextSpan;
import com.statnlp.ie.types.Mention;
import com.statnlp.ui.ExpGlobal;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**

	 * It builds Instance 

 * @author Li Hao
 *
 */
public class ACEInstanceParser extends InstanceParser{
	
	
	IEManager manager = null;

	public static String _corpusName = "xxx";
	
	MaxentTagger tagger = null;


	public ACEInstanceParser(AlgoModel algomodel) {
		super(algomodel);
		/*
		String filename_template = (String) this.getParameters("filename_template");
		
		try {
			//manager = EventExtractionReader.readIEManager(filename_template);
			//this.setParameters("manager", manager);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
	}

	/**
	 * It builds Instance
	 */
	@Override
	public void BuildInstances()
			throws FileNotFoundException {
		
		manager = (IEManager) getParameters("manager");
		System.err.println(Arrays.toString(manager.getMentionTemplate().getAllTypes()));
		
		Scanner scan;
	
		String filename_input = (String) this.getParameters("filename_input");
		Boolean WithPOSTag = (Boolean)getParameters("withPOSTag");
		boolean withPOSTag = (WithPOSTag == null || WithPOSTag == true) ? true : false;
		
		if (!withPOSTag)
		{
			scan = new Scanner(new File(filename_input));
			filename_input = addPOSTags(scan);
		}
	
		scan = new Scanner(new File(filename_input));
		_corpusName = (String)this.getParameters("folder");
		ArrayList<LabeledTextSpan> spans = readTextSpans(scan, manager, true);

		int train_size = spans.size();
		
		this.algomodel.Instances = new IELinearInstance[train_size];
		for(int k = 0; k<train_size; k++){
			MentionLinearInstance inst = new MentionLinearInstance(k+1, spans.get(k), manager.getMentionTemplate());
			this.algomodel.Instances[k] = inst;
		}
		
		
		
	}
	
	/**
	 * Add POS tags for every line in a file and return the filename of new file with POS tags
	 * @param scan
	 * @return new filename
	 */
	public String addPOSTags(Scanner scan)
	{
		String input = "";
		
		while(scan.hasNextLine())
		{
			String line = scan.nextLine();
			line = line.trim();
			if (line.length() == 0)
				continue;
			
			String tag = applyPOSTags(line);
				input += line + "\n" + tag + "\n\n\n";
			
		}
				
		
		String filename_input = (String) algomodel.getParameters("filename_input");
		filename_input += ".withpostag";
		algomodel.setParameters("filename_input", filename_input);
		
		PrintWriter p;
		try {
			p = new PrintWriter(new File(filename_input));
			p.write(input);
			p.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return filename_input;		
		
	}
	
	
	private String applyPOSTags(String line)
	{
		if (tagger == null)
		{
			tagger = new MaxentTagger(ExpGlobal.POSTaggerTrainedFile);
		}
		
		String tag_output = "";
			 
		String[] tokens = line.split(" ");
			 
		for(String token : tokens)
		{
			String text_with_tag = tagger.tagString(token);
			int tag_pos = text_with_tag.lastIndexOf("_");
			tag_output += text_with_tag.substring(tag_pos + 1);
		}

		return tag_output;
			
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
}

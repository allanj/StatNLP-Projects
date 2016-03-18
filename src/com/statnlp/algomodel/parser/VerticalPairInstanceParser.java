package com.statnlp.algomodel.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import com.statnlp.algomodel.AlgoModel;
import com.statnlp.commons.ml.gm.linear.LinearInstance;
import com.statnlp.commons.ml.gm.linear.OutputTag;
import com.statnlp.commons.types.InputToken;
import com.statnlp.commons.types.OutputToken;
import com.statnlp.commons.types.WordToken;
import com.statnlp.hybridnetworks.NetworkConfig;

public class VerticalPairInstanceParser extends InstanceParser {
	
	public HashMap<String, Integer> wordsMap = new HashMap<String, Integer>();
	public ArrayList<String> words = new ArrayList<String>();
	
	public HashMap<String, Integer> tagsMap = new HashMap<String, Integer>();
	public ArrayList<String> tags = new ArrayList<String>();
	
	

	public VerticalPairInstanceParser(AlgoModel algomodel) {
		super(algomodel);
		NetworkConfig._numThreads = 1;
		clear();
	}

	@Override
	public void BuildInstances() throws FileNotFoundException {
		ArrayList<String> words_arr = new ArrayList<String>();
		ArrayList<String> tags_arr = new ArrayList<String>();
		
		ArrayList<LinearInstance> instances_arr = new ArrayList<LinearInstance>();
		
		
		String filename_input = (String) getParameters("filename_input");
		
		//training
		String line;
		Scanner scan = new Scanner(new File(filename_input));
		int id = 0;
		String tmp;
		String sentence = null;
		while(scan.hasNextLine())
		{
			words_arr.clear();
			tags_arr.clear();
			
			sentence = "";
			
			while(scan.hasNextLine())
			{
			
				
				line = scan.nextLine();
				if (line.trim().equals(""))
					break;
				
				String[] input = line.split("\t");
				if (input.length < 2)
				{
					tmp = input[0];
					input = new String[]{tmp, ""};
				}
				
				if (input.length >= 1 && input[0].trim().equals(""))
					break;
				
				words_arr.add(input[0]);
				if (!input[1].equals(""))
					tags_arr.add(input[1]);
				
				sentence += " " + input[0];
				
				
			}
			
			
			id++;
			
			String[] words = new String[words_arr.size()];
			InputToken[] input = new WordToken[words.length];
			for(int i = 0; i < words.length; i++)
			{
				words[i] = words_arr.get(i);//.toLowerCase();
				input[i] = new WordToken(words[i]);
				int observationID = addWords(input[i].getName());
				input[i].setId(observationID);
			}
			
			OutputToken[] output = new OutputTag[words.length];
			
			String[] tags = new String[tags_arr.size()];
			for(int i = 0; i < tags.length; i++)
			{
				tags[i] = tags_arr.get(i);
				output[i] = new OutputTag(tags[i]);
				int hiddenStateID = addTags(output[i].getName());
				output[i].setId(hiddenStateID);
			}
	
			LinearInstance instance = new LinearInstance(id, 1.0, input, output);
			instance.setLabeled();
			instances_arr.add(instance);
		}
		//sentence is not used actually.
		sentence.toString();
		scan.close();
		
		this.algomodel.Instances = new LinearInstance[instances_arr.size()];
		
		for(int i = 0; i < instances_arr.size(); i++)
		{
			this.algomodel.Instances[i] = instances_arr.get(i);
		}
		
	}
	
	
	int addTags(String hiddenState)
	{
		Integer index = this.tagsMap.get(hiddenState);
		if (index == null)
		{
			index = tags.size();
			this.tagsMap.put(hiddenState, index);
			this.tags.add(hiddenState);
		}
		
		return index;
	}
	
	
	int addWords(String observationState)
	{
		Integer index = this.wordsMap.get(observationState);
		if (index == null)
		{
			index = words.size();
			this.wordsMap.put(observationState, index);
			this.words.add(observationState);
		}
		return index;
	}
	
	void clear()
	{
		this.tags.clear();
		this.tagsMap.clear();
		
		this.words.clear();
		this.wordsMap.clear();
	}

}

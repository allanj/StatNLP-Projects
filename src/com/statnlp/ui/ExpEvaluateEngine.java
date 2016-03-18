package com.statnlp.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;

import com.statnlp.algomodel.AlgoModel;
import com.statnlp.commons.WordUtil;
import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GenerativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.ie.io.EventExtractionReader;
import com.statnlp.ie.linear.IELinearConfig;
import com.statnlp.ie.linear.IELinearFeatureManager;
import com.statnlp.ie.linear.IELinearFeatureManager_GENIA;
import com.statnlp.ie.linear.IELinearNetworkCompiler;
import com.statnlp.ie.linear.MentionExtractionEvaluator;
import com.statnlp.ie.linear.MentionLinearInstance;
import com.statnlp.ie.types.IEManager;
import com.statnlp.ie.types.LabeledTextSpan;
import com.statnlp.ie.types.SemanticTag;
import com.statnlp.ie.types.UnlabeledTextSpan;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;


public class ExpEvaluateEngine implements MVCModel {

	private static final boolean DEBUG = true && ExpGlobal.DEBUG;

	MVCViewer viewer = null;

	ExpEngine parent = null;

	String Status;
	
	MaxentTagger tagger = null;
	
	AlgoModel algomodel;

	boolean withTags = false;

	public ExpEvaluateEngine(ExpEngine ee)
	{
		this.parent = ee;
		if (tagger == null)
		{
			tagger = new MaxentTagger(ExpGlobal.POSTaggerTrainedFile);
		}
	}


	@Override
	public void setMVCViewer(MVCViewer viewer) {
		// TODO Auto-generated method stub
		this.viewer = viewer;

	}

	@Override
	public String getStatus() {
		// TODO Auto-generated method stub
		return Status;
	}

	@Override
	public void setStatus(String status) {
		// TODO Auto-generated method stub
		this.Status = status;
	}

	public void loadModel(ExpConfig ec) 
	{
		algomodel = AlgoModel.create(ec.algomodel.toString());
		String[] args = new String[6];
		args[0] = "1";
		args[1] = ec.corpusNames;
		args[2] = "";
		args[3] = ec.cv;
		args[4] = ec.subfolders;
		
		algomodel.LoadModel(args);
	}

	
	public String preProcess(String input, boolean withTags, ExpConfig ec)
	{

		String[] lines = splitLines(input);
		String eval = "";
		
		for(int i = 0; i < lines.length; i++)
		{
			String line = lines[i];
			line = line.trim();
			if (line.length() == 0)
				continue;
			
			//line = ee.evale.preprocess(line);
			if (!withTags)
			{
				String tag = addTag(line);
				eval += line + "\n" + tag + "\n\n\n";
			}
			else
			{
				String tag = lines[i + 1];
				String mention = lines[i + 2];
				eval += line + "\n" + tag + "\n" + mention + "\n\n";
				i = i + 3;
				
			}
			
		}
		
		if (DEBUG)
			System.out.println(eval);
		
		/*
		String filename_input = (String) algomodel.getParameters("filename_input");
		filename_input += ".demo";
		algomodel.setParameters("filename_input", filename_input);
		
		String filename_output = (String) algomodel.getParameters("filename_output");
		filename_output += ".demo";
		algomodel.setParameters("filename_output", filename_output);
		
		PrintWriter p;
		try {
			p = new PrintWriter(new File(filename_input));
			p.write(eval);
			p.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		return eval;		
		//return "";
	}
	
	public String[] splitLines(String text)
	{
		String[] lines = text.split("\n");
		return lines;
	}
	
	public String addTag(String eval)
	{
		String tag_output = "";
		 
		 String[] tokens = eval.split(" ");
		 
		  for(String token : tokens)
		  {
			  String text_with_tag = tagger.tagString(token);
			  int tag_pos = text_with_tag.lastIndexOf("_");
			  tag_output += text_with_tag.substring(tag_pos + 1);
		  }

		  return tag_output;
		
	}
	
	public void setHtmlOutputWidth(int width)
	{
		algomodel.setParameters("htmlOutputWidth", new Integer(width));
	}
	
	public void setWithPOSTag(boolean withtag)
	{
		this.withTags = withtag;
		algomodel.setParameters("withPOSTag", new Boolean(withtag));
	}


	public String[] evaluate(ExpConfig ec, String eval, boolean updateUI) throws FileNotFoundException, InterruptedException
	{
		System.out.println("eval:\n" + eval + "\n*******\n");
		String eval_ACE_format = this.preProcess(eval, withTags, ec);
		String[] args = new String[]{"1", "Demo", "", ec.cv, "Demo"};
		
		
		String filename_input =filename_input = "data\\Demo\\data\\Demo\\mention-standard\\"+IELinearConfig._type.name()+"\\test.data";
		PrintWriter p;
		p = new PrintWriter(new File(filename_input));
		p.write(eval);
		p.close();
		
		//(String) algomodel.getParameters("filename_input");
		//filename_input += ".demo";
		//algomodel.setParameters("filename_input", filename_input);
		
	
		
		
		algomodel.setDemoMode(true);
		algomodel.Evaluate(args);
		
		String[] result = new String[3];
		
		result[0] = (String) algomodel.getParameters("filename_output") + ".html";
		//result[1] = precision_recall.toString();
		//result[2] = status.toString();
		return result;

	}
	


}

package com.statnlp.ui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.statnlp.ui.visualize.type.VNode;

public class ExpGlobal {
	
	public static boolean DEBUG = true;
	
	public static String NLPBOX_CONFIG_FILE_NAME = "NLPBox.config";
	
	public static String EVALUATION_INPUT = "evaluation.txt";
	
	public static String EVALUATION_HTML_FILE_NAME = "evaluation.html";

	public static String getLogFileName(ExpConfig ec)
	{
		StringBuffer sb = new StringBuffer("");
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
		Date date = new Date();
		sb.append(ec.execution + "-");
		sb.append(ec.corpusNames + "-");
		sb.append(ec.Iteration + "-");
		sb.append(dateFormat.format(date));
		sb.append(".log");
		return sb.toString();
	}
	
	public static String getTestModelFileName = "experiments/mention/model/FINE_TYPE/ACE20041/English/linear-0.01.model";
	
	public static String POSTaggerTrainedFile = "models/english-left3words-distsim.tagger";
	
	
	
	public static class VisualizationState
	{
		public static int SELECT_ROOT = 5;
		
		public static int SELECT_EDGE = 6;
		
		public static int Select_Item_Type = SELECT_ROOT;
		
		public static int bIndex = 1;
		
		public static int MAX_NUM_NODE = 1000000;
		
		public static VNode linkStartNode;
		
		public static VNode linkEndNode;
		
		
		
	}
	
	
}

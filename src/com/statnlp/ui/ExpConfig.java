package com.statnlp.ui;

import java.io.PrintStream;

import com.statnlp.algomodel.AlgoGlobal;
import com.statnlp.algomodel.AlgoModel;
import com.statnlp.hybridnetworks.NetworkConfig;

public class ExpConfig {
	
	public static ExpConfig DEFAULT_CONFIG = resetDefault();
	
	public String execution = "";
	
	public AlgoGlobal.TaskMode taskmode;
	
	public AlgoModel.ALGOMODEL algomodel;
	
	public String Memory = "";
	
	public int Thread = 0;
	
	public String ThreadString = "";
	
	public String corpusNames = "";
	
	public String Iteration = "";
	
	public String cv = "";
	
	public String subfolders = "";
	
	public String parameters = "";
	
	public boolean CACHE_FEATURES_DURING_TRAINING = false;
	
	public boolean REBUILD_FOREST_EVERY_TIME = false;
	
	public boolean REDIRECT_LOG_FILE = false;
	
	public boolean SAVE_MODEL = false;
	
	public double L2_REGULARIZATION_CONSTANT = 0.0;
	
	public boolean TRAIN_MODE_IS_GENERATIVE = true;
	
	public PrintStream out = null;
	
	public PrintStream err = null;
	
	public ExpConfig()
	{
		
	}
	
	private static ExpConfig resetDefault()
	{
		ExpConfig ec = new ExpConfig();
		ec.execution = "com.statnlp.ie.linear.MentionExtractionLearner";
		ec.algomodel = AlgoModel.ALGOMODEL.MENTION_EXTRACTION;
		ec.taskmode = AlgoGlobal.TaskMode.TRAIN;
		ec.Memory = "1g";
		ec.Thread = NetworkConfig._numThreads;
		ec.ThreadString = String.valueOf(NetworkConfig._numThreads);
		ec.corpusNames = "";
		ec.Iteration = "2000";
		ec.cv = "";
		ec.subfolders = "";
		ec.CACHE_FEATURES_DURING_TRAINING = NetworkConfig._CACHE_FEATURES_DURING_TRAINING;
		ec.REBUILD_FOREST_EVERY_TIME = NetworkConfig.REBUILD_FOREST_EVERY_TIME;
		ec.REDIRECT_LOG_FILE = true;
		ec.SAVE_MODEL = true;
		ec.L2_REGULARIZATION_CONSTANT = 0.0;
		ec.TRAIN_MODE_IS_GENERATIVE = true;
		ec.out = null;
		ec.err = null;
		return ec;
	}
	
	public void applySetting()
	{
		/*NetworkConfig._numThreads = this.Thread;
		NetworkConfig._CACHE_FEATURES_DURING_TRAINING = this.CACHE_FEATURES_DURING_TRAINING;
		NetworkConfig.REBUILD_FOREST_EVERY_TIME = this.REBUILD_FOREST_EVERY_TIME;
		ExpOI.err = this.err;
		ExpOI.out = this.out;*/
	}
	
	public int validate()
	{
		if (this.algomodel == null)
			return 1;
		
		if (this.Memory == "")
			return 2;
		
		if (this.Thread == 0)
			return 3;
		
		if (this.corpusNames == "")
			return 4;
		
		if (this.Iteration == "")
			return 5;
		
		if (this.cv == "")
			return 6;
		
		if (this.subfolders == "")
			return 7;
		
			
		return 0;
		
	}
	
	public String toString()
	{
		StringBuffer sb = new StringBuffer("");
		sb.append(this.algomodel + " ");
		sb.append(this.taskmode + " ");
		sb.append(this.Memory + " ");
		sb.append(this.Thread + " ");
		sb.append(this.corpusNames + " ");
		sb.append(this.Iteration + " ");
		sb.append(this.cv + " ");
		sb.append(this.subfolders + " ");
		sb.append((this.CACHE_FEATURES_DURING_TRAINING ? "Cache_Feature " : ""));
		sb.append((this.REBUILD_FOREST_EVERY_TIME ? "Rebuild_Forest " : ""));
		sb.append((this.REDIRECT_LOG_FILE ? "Redirect_Logs_to_Files " : ""));
		sb.append((this.SAVE_MODEL ? "Save_Model " : ""));

		return sb.toString();
	}
	
	
	public void setPrintStream(PrintStream out, PrintStream err)
	{
		this.out = out;
		this.err = err;
	}
	

	
	

}

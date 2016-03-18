package com.statnlp.ui;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;

import com.statnlp.algomodel.AlgoGlobal;
import com.statnlp.ui.visualize.ExpVisualizationEngine;





public class ExpEngine extends Thread implements MVCModel{
	
	public static boolean DEBUG = true;
	
	ExpConfig ec;
	
	String Status = "";
	
	public boolean RUN_IN_CONCURRENT = false;
	
	DefaultListModel<ExpConfig> ecs = new DefaultListModel<ExpConfig>();
	
	public MVCViewer viewer = null;
	
	ExpTrainingDataEngine etde = new ExpTrainingDataEngine(this);
	
	ExpVisualizationEngine eve = new ExpVisualizationEngine(this);
	
	ExpEvaluateEngine evale = new ExpEvaluateEngine(this);
	
	public ExpTrainingDataEngine getExpTrainingDataEngine()
	{
		return etde;
	}
	
	
	public ExpEngine()
	{
		
	}
	
	public ExpEngine(MVCViewer viewer)
	{
		this.setMVCViewer(viewer);
	}
	
	public void addTask(ExpConfig ec)
	{
		this.ecs.addElement(ec);
	}
	
	@Override
	public void run()
	{
		if (this.RUN_IN_CONCURRENT)
		{
			
		}
		else
		{
			try {
				executeTasksInSeries();
			} catch (InterruptedException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	public void runSingleTraining(ExpConfig ec)
	{
		final ExpConfig expconfig = ec;
		
		updateStatus("Start Single Training...", "");
		
		new Thread(new Runnable() {
		    public void run() {
		    	
		    	try {
		    		
		    		executeTask(expconfig);
		    		
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    	
		    }
		}).start();
		
	}
	
	public void updateStatus(String status, String arg)
	{
		this.Status = status;
		this.viewer.updateMVCViewer(arg);
	}
	

	boolean executeTasksInSeries() throws InterruptedException, IOException
	{
		for (int i = 0; i < ecs.size(); i++)
		{
			ExpConfig ec = ecs.get(i);
			
			this.updateStatus("Execute the task:" + ec.toString(), "");
			ec.applySetting();
			
			if (this.DEBUG)
			{
				//JOptionPane.showMessageDialog(null, ec.toString());
			}
			boolean ret = executeTask(ec);
			Thread.sleep(1000);
		}
		
		this.endOfExperiment();
		
		return true;
	}
	
	void endOfExperiment()
	{
		if (DEBUG)
			System.out.println("All Task are completed!");
		
		this.ecs.clear();
		this.updateStatus("All tasks are completed!", "");
		this.viewer.endOfExperiment();
	}
	
	public ExpEngine copyThread()
	{
		ExpEngine ee = new ExpEngine();
		ee.ecs = this.ecs;
		ee.viewer = this.viewer;
		ee.eve = this.eve;
		return ee;
	}
	
	boolean executeTask(ExpConfig ec) throws IOException, InterruptedException
	{
		//String parameters = "-cp bin -Xmx2g com.statnlp.ie.linear.MentionExtractionLearner 8 ACE20041 2 0.01 English";
		
		//String workingDir = System.getProperty("user.dir");
		
		List<String> parameters = new ArrayList<String>();
		parameters.add("java");
		parameters.add("-cp");
		parameters.add("bin");
		parameters.add("-Xmx" + ec.Memory);
		parameters.add(AlgoGlobal.ALGO_MODEL_PATH);
		parameters.add(ec.algomodel.toString());
		parameters.add(ec.taskmode.toString());
		parameters.add(String.valueOf(ec.Thread));
		parameters.add(ec.corpusNames);
		parameters.add(String.valueOf(ec.Iteration));
		parameters.add(ec.cv);
		parameters.add(ec.subfolders);
		
		if (ec.SAVE_MODEL)
		{
			parameters.add("SAVE_MODEL");
		}
		
		if (ec.CACHE_FEATURES_DURING_TRAINING)
		{
			parameters.add("CACHE_FEATURES");
		}
		
		if (ec.REBUILD_FOREST_EVERY_TIME)
		{
			parameters.add("REBUILD_FOREST_EVERY_TIME");
		}
		
		if (ec.REDIRECT_LOG_FILE)
		{
			parameters.add("REDIRECT_LOG_FILE");
		}
		
		
		if (DEBUG)
			System.out.println("Parameter = " + parameters.toString());
		
		//ProcessBuilder pb = new ProcessBuilder("java.exe", "-cp", "bin", "-Xmx2g", "com.statnlp.ie.linear.MentionExtractionLearner", "8", "ACE20041", "2", "0.01" ,"English");
		ProcessBuilder pb = new ProcessBuilder(parameters);
		pb.redirectErrorStream(true);
		

	
		
		final ExpConfig current_ec = ec;
		
		final Process p = pb.start();
			
		new Thread(new Runnable() {
		    public void run() {
		        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		        String line = null; 
		        
		    	BufferedWriter bufferedWriter = null;
				
				if (current_ec.REDIRECT_LOG_FILE)
				{
					FileWriter fileWriter = null;
					try {
						fileWriter = new FileWriter(ExpGlobal.getLogFileName(current_ec));
						bufferedWriter = new BufferedWriter(fileWriter);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
		        
		        try {       		
		        	 // read the output from the command
		            //System.out.println("Here is the standard output of the command:\n");
		            while ((line = input.readLine()) != null) {
		                System.out.println(line);
		                
		                if (current_ec.REDIRECT_LOG_FILE && bufferedWriter != null)
		                {
		                	bufferedWriter.write(line);
		                	bufferedWriter.newLine();
		                }
		                
		            }
		        
		        } catch (IOException e) {
		            e.printStackTrace();
		        } 
		        
		        if (current_ec.REDIRECT_LOG_FILE)
		        {
		        	try {
		        		bufferedWriter.close();
		        	} catch (IOException e) {
		        		// TODO Auto-generated catch block
		        		e.printStackTrace();
		        	}
				}
		    }
		}).start();

		p.waitFor();
		this.updateStatus("This task is completed.", "");
		return true;
	}

	@Override
	public void setMVCViewer(MVCViewer viewer) {
		this.viewer = viewer;
	}

	@Override
	public String getStatus() {
		return Status;
	}

	@Override
	public void setStatus(String status) {
		this.Status = status;
	}
	

}

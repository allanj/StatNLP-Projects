package com.statnlp.ie.linear;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;

import com.statnlp.algomodel.AlgoModel;
import com.statnlp.algomodel.parser.ACEInstanceParser;
import com.statnlp.commons.WordUtil;
import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.ie.io.EventExtractionReader;
import com.statnlp.ie.types.IEManager;
import com.statnlp.ie.types.UnlabeledTextSpan;

public class MentionExtractionAlgoModel extends AlgoModel {
	
	public static String MODELNAME = "Mention_Extraction";
	public static String DEFAULT_HTMLOutputWidth = "100%";
	
	String folder = "";
	String subfolder = "";
	IEManager manager = null;
	
	String filename_input;
	String filename_model;
	String filename_template;
	String filename_output;
	
	
	String cv;
	double weight0;

	public MentionExtractionAlgoModel() {
		super();
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
	}

	@Override
	protected void initInstanceParser(AlgoModel algomodel) {
		this.parser = new ACEInstanceParser(algomodel);
	
	}

	@Override
	protected void initNetworkCompiler() {
		manager = (IEManager)getParameters("manager");
		System.err.println(Arrays.toString(manager.getMentionTemplate().getAllTypes()));
		
		compiler = new IELinearNetworkCompiler(manager.getMentionTemplate().getAllTypesExcludingStartAndFinish_arr());

	}

	@Override
	protected void initFeatureManager() {
		
		if(folder.equals("GENIA")){
			fm = new IELinearFeatureManager_GENIA(param);
		} else {
			fm = new IELinearFeatureManager(param);
		}
		
	}

	@Override
	protected void saveModel() throws IOException {
		File f = new File("experiments/mention/model/"+IELinearConfig._type.name()+"/"+folder+"/"+subfolder+"/");
		f.mkdirs();
		
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

	@SuppressWarnings("unchecked")
	@Override
	protected void loadModel() throws IOException {
		
		
		System.err.println("Loading Model...");
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename_model));
		try {
			param = (GlobalNetworkParam)in.readObject();
			manager = (IEManager)in.readObject();
			WordUtil._func_words = (HashSet<String>)in.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		in.close();
		setParameters("manager", manager);
		System.err.println("Model loaded!");

	}

	@Override
	public void initTraining(String[] args) {
		
		NetworkConfig._numThreads = Integer.parseInt(args[0]);
		folder = args[1];

		this.MaxIteration = Integer.parseInt(args[2]);
		
		cv = args[3];
		subfolder = args[4];
		
		NetworkConfig.L2_REGULARIZATION_CONSTANT = Double.parseDouble(cv);
		
		this.param = new GlobalNetworkParam();
		
		filename_input = "data/"+folder+"/data/"+subfolder+"/mention-standard/"+IELinearConfig._type.name()+"/train.data";
		filename_model = "experiments/mention/model/"+IELinearConfig._type.name()+"/"+folder+"/"+subfolder+"/linear-"+cv+".model";
		filename_template = "data/"+folder+"/data/"+subfolder+"/template";
		
		try {
			manager = EventExtractionReader.readIEManager(filename_template);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		setParameters("folder", folder);
		setParameters("subfolder", subfolder);
		setParameters("filename_input", filename_input);
		setParameters("filename_model", filename_model);
		setParameters("filename_template", filename_template);
		setParameters("manager", manager);
	}

	@Override
	public void initEvaluation(String[] args) {
		NetworkConfig._numThreads = Integer.parseInt(args[0]);
		folder = args[1];
		cv = args[3];
		subfolder = args[4];
		
		NetworkConfig.L2_REGULARIZATION_CONSTANT = Double.parseDouble(cv);
		
		this.param = null;
		
		filename_input = "data/"+folder+"/data/"+subfolder+"/mention-standard/"+IELinearConfig._type.name()+"/test.data";
		filename_model = "experiments/mention/model/"+IELinearConfig._type.name()+"/"+folder+"/"+subfolder+"/linear-"+cv+".model";
		filename_output = "experiments/mention/model/"+IELinearConfig._type.name()+"/"+folder+"/"+subfolder+"/output.data";

		
		setParameters("folder", folder);
		setParameters("subfolder", subfolder);
		setParameters("filename_input", filename_input);
		setParameters("filename_model", filename_model);
		setParameters("filename_output", filename_output);
		
		Scanner scan0;
		try {
			scan0 = new Scanner(new File("experiments/mention/model/"+IELinearConfig._type.name()+"/"+folder+"/"+subfolder+"/linear-"+cv+".model.MP"));
			weight0 = Double.parseDouble(scan0.nextLine().trim());
			scan0.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	@Override
	protected void evaluationResult(Instance[] instances_outputs) {
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
		
		double numWords = 0;
		
		double time_sec = this.timer/1000.0;
		
		Integer htmlOutputWidth = (Integer)getParameters("htmlOutputWidth");
		String htmlOutputWidth_Str = (htmlOutputWidth == null) ? DEFAULT_HTMLOutputWidth : htmlOutputWidth.toString();
		
		StringBuffer html_output = new StringBuffer("<html><div width=" + htmlOutputWidth_Str + " style='font-size: 120%'>");
		
		String filename_output = (String) getParameters("filename_output");
		PrintWriter p = null;
		try {
			p = new PrintWriter(new File(filename_output));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for(int k = 0; k<instances_outputs.length; k++){
			MentionLinearInstance inst = (MentionLinearInstance)instances_outputs[k];
			
			UnlabeledTextSpan span = inst.getInput();
			count_corr += span.countCorrect();
			count_corr_span += span.countCorrect_span();
			count_pred += span.countPredicted();
			count_expt += span.countExpected();
			
			html_output.append(span.toTreeHTMLFormat_predictionWithPOS(false, false));
			
			p.println(span.toStandardFormat_predictionWithPOS());
			p.flush();
			
			numWords += inst.getInput().length();
		}
		p.close();
		
		StringBuffer precision_recall = new StringBuffer();
		precision_recall.append("==TEST SET==\n");
		precision_recall.append("#corr      ="+count_corr+"\n");
		precision_recall.append("#corr(span)="+count_corr_span+"\n");
		precision_recall.append("#pred      ="+count_pred+"\n");
		precision_recall.append("#expt      ="+count_expt+"\n");
		precision_recall.append("#words/sec.="+(numWords/time_sec)+"="+numWords+"/"+time_sec+"\n");
		precision_recall.append("#insts/sec.="+(instances_outputs.length/time_sec)+"\n");
		
		P = count_corr/count_pred;
		R = count_corr/count_expt;
		F = 2/(1/P+1/R);

		precision_recall.append("-FULL-\n");
		precision_recall.append("Prec:"+P+"\t"+"Rec:"+R+"\t"+"F:"+F+"\n");
		
		P = count_corr_span/count_pred;
		R = count_corr_span/count_expt;
		F = 2/(1/P+1/R);

		precision_recall.append("-SPAN ONLY-\n");
		precision_recall.append("Prec:"+P+"\t"+"Rec:"+R+"\t"+"F:"+F+"\n");
		
		html_output.append("</div>");
		
		html_output.append("<div>");
		html_output.append(precision_recall.toString().replace("\n","<br>"));
		html_output.append("</div>");
		html_output.append("</html>");
		
		try {
			p = new PrintWriter(new File(filename_output + ".html"));
			p.write(html_output.toString());
			p.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}

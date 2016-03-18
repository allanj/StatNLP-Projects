package com.statnlp.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import com.statnlp.commons.AttributedWord;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GenerativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.LocalNetworkLearnerThread;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.ie.io.EventExtractionReader;
import com.statnlp.ie.linear.IELinearConfig;
import com.statnlp.ie.linear.IELinearFeatureManager;
import com.statnlp.ie.linear.IELinearFeatureManager_GENIA;
import com.statnlp.ie.linear.IELinearInstance;
import com.statnlp.ie.linear.IELinearNetworkCompiler;
import com.statnlp.ie.linear.MentionExtractionLearner;
import com.statnlp.ie.linear.MentionLinearInstance;
import com.statnlp.ie.types.IEManager;
import com.statnlp.ie.types.LabeledTextSpan;
import com.statnlp.ie.types.Mention;
import com.statnlp.ie.types.SemanticTag;

public class ExpTrainingDataEngine implements MVCModel{
	
	public static boolean DEBUG = true && ExpGlobal.DEBUG;
	
	MVCViewer viewer = null;

	ExpEngine parent = null;

	String Status;

	IELinearNetworkCompiler compiler;

	NetworkModel model;

	FeatureManager fm;

	GlobalNetworkParam param;

	SemanticTag[] tags;

	IEManager manager;

	Network[] networks;

	ArrayList<LabeledTextSpan> spans_train = null;

	ArrayList<String> words = null;

	int layout_width = 1024;

	int layout_height = 1024;

	double span_width = layout_width / 5.0;

	double span_height = 100.0;

	String _corpusName;

	public int network_size = 0;
	
	public ExpTrainingDataEngine(ExpEngine parent) {
		this.parent = parent;
	}

	@Override
	public void setMVCViewer(MVCViewer viewer) {
		this.viewer = viewer;
	}
	
	void loadData(File file) throws FileNotFoundException {

		if (_corpusName.equals("GENIA")) {
			fm = new IELinearFeatureManager_GENIA(new GlobalNetworkParam());
		} else {
			fm = new IELinearFeatureManager(new GlobalNetworkParam());
		}

		Scanner scan = new Scanner(file);
		spans_train = MentionExtractionLearner.readTextSpans(scan, manager,
				true);

		int train_size = spans_train.size();

		IELinearInstance[] instances = new IELinearInstance[train_size];
		for (int k = 0; k < train_size; k++) {
			MentionLinearInstance inst = new MentionLinearInstance(k + 1,
					spans_train.get(k), manager.getMentionTemplate());
			instances[k] = inst;
		}

		tags = manager.getMentionTemplate()
				.getAllTypesExcludingStartAndFinish_arr();

		for (int i = 0; i < tags.length; i++) {
			System.out.println("\t" + tags[i]);
		}
		// compiler = new IELinearNetworkCompiler(tags);

		compiler = new IELinearNetworkCompiler(manager.getMentionTemplate()
				.getAllTypesExcludingStartAndFinish_arr());

		model = NetworkConfig.TRAIN_MODE_IS_GENERATIVE ? GenerativeNetworkModel
				.create(fm, compiler) : DiscriminativeNetworkModel.create(fm,
				compiler);

		// model.train(instances, 0);
		for (int k = 0; k < instances.length; k++) {
			instances[k].setInstanceId(k + 1);
		}

		// create the threads.
		LocalNetworkLearnerThread learners = new LocalNetworkLearnerThread(0,
				fm, instances, compiler, -1);
		// distribute the works into different threads.
		// WARNING: must do the following sequentially..

		learners.touch();

		// finalize the features.
		fm.getParam_G().lockIt();

		networks = learners.getNetworks();

		network_size = networks.length;

		finishLoadingModel();
	}

	void loadData() throws InterruptedException, IOException {
		NetworkConfig._numThreads = 1;

		String c = "0.01";

		NetworkConfig.L2_REGULARIZATION_CONSTANT = Double.parseDouble(c);

		String folder = "ACE20041";

		_corpusName = folder;

		String subfolder = "English";

		String filename_template = "data/" + folder + "/data/" + subfolder
				+ "/template";

		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;

		manager = EventExtractionReader.readIEManager(filename_template);

		System.err.println(Arrays.toString(manager.getMentionTemplate()
				.getAllTypes()));

		// File f = new
		// File("experiments/mention/model/"+IELinearConfig._type.name()+"/"+folder+"/"+subfolder+"/");
		// f.mkdirs();

		String filename_input;
		// String filename_model;

		filename_input = "data/" + folder + "/data/" + subfolder
				+ "/mention-standard/" + IELinearConfig._type.name()
				+ "/train.data";
		// filename_model =
		// "experiments/mention/model/"+IELinearConfig._type.name()+"/"+folder+"/"+subfolder+"/linear-"+c+".model";

		File file = new File(filename_input);

		loadData(file);

	}

	public void loadData(ExpConfig ec) throws IOException {
		NetworkConfig._numThreads = 1;

		String c = ec.cv;

		NetworkConfig.L2_REGULARIZATION_CONSTANT = Double.parseDouble(c);

		String folder = ec.corpusNames;

		_corpusName = folder;

		String subfolder = ec.subfolders;

		String filename_template = "data/" + folder + "/data/" + subfolder
				+ "/template";

		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;

		manager = EventExtractionReader.readIEManager(filename_template);

		System.err.println(Arrays.toString(manager.getMentionTemplate()
				.getAllTypes()));

		String filename_input = "data/" + folder + "/data/" + subfolder
				+ "/mention-standard/" + IELinearConfig._type.name()
				+ "/train.data";

		File file = new File(filename_input);

		loadData(file);

	}

	public void loadTrainingData(ExpConfig ec) {
		final ExpConfig expconfig = ec;

		parent.updateStatus("Loading Training Data...", "");

		new Thread(new Runnable() {
			public void run() {

				try {
					loadData(expconfig);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}).start();

	}

	void finishLoadingModel() {
		this.setStatus("Loading is completed...");
		String status = this.getStatus();
		System.err.println(status);
		parent.updateStatus(status, "update_network_size");
	}

	@Override
	public String getStatus() {
		return Status;
	}

	@Override
	public void setStatus(String status) {
		this.Status = status;
	}
	

	
	public LabeledTextSpan getLabeledTextSpan(int network_index)
	{
		return this.spans_train.get(network_index);
		
	}
	

	public Network getNetwork(int network_index)
	{
		return this.networks[network_index];
	}

}

package com.statnlp.test;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.hybridnetworks.NetworkConfig.InferenceType;
import com.statnlp.projects.nndcrf.factorialCRFs.Entity;
import com.statnlp.projects.nndcrf.factorialCRFs.TFConfig;
import com.statnlp.projects.nndcrf.factorialCRFs.TFConfig.TASK;
import com.statnlp.projects.nndcrf.factorialCRFs.TFFeatureManager;
import com.statnlp.projects.nndcrf.factorialCRFs.TFInstance;
import com.statnlp.projects.nndcrf.factorialCRFs.TFNetworkCompiler;
import com.statnlp.projects.nndcrf.factorialCRFs.Tag;

import junit.framework.TestCase;

public class MeanFieldTest extends TestCase {

	/**
	 * The training data.
	 */
	protected TFInstance[] data;
	
	protected int maxIter;
	
	/**
	 * Some initialization here.
	 */
	protected void setUp(){
		WordToken[] wts = new WordToken[2];
		wts[0] = new WordToken("a", "NN", -1, "B-PER");
		wts[1] = new WordToken("b", "VB", -1, "O");
		Sentence sent = new Sentence(wts);
		TFInstance inst = new TFInstance(1, 1.0, sent);
		ArrayList<String> output = new ArrayList<>(2);
		output.add("B-PER");
		output.add("O");
		inst.setEntities(output);
		inst.setLabeled();
		data = new TFInstance[]{inst};
		//Initialize the two maps
		Entity.get("B-PER");
		Entity.get("O");
		Tag.get("NN");
		Tag.get("VB");
		Entity.lock();
		Tag.lock();
		//Setup the configuration
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig.CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = TFConfig.l2val;
		NetworkConfig.NUM_THREADS = 8;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = true;
		NetworkConfig.BUILD_FEATURES_FROM_LABELED_ONLY = false;
		NetworkConfig.NUM_STRUCTS = 2;
		NetworkConfig.INFERENCE = InferenceType.MEAN_FIELD;
		maxIter = 100;
		System.out.println(Entity.ENTS_INDEX.toString());
		System.out.println(Tag.TAGS_INDEX.toString());
	}
	
	public void testMeanField() throws InterruptedException{
		NetworkConfig.MF_ROUND = 3;
		GlobalNetworkParam param_g = new GlobalNetworkParam();
		TFFeatureManager fa = new TFFeatureManager(param_g, true, false, TASK.JOINT, 3);
		TFNetworkCompiler compiler = new TFNetworkCompiler(TASK.JOINT, false);
		NetworkModel model = DiscriminativeNetworkModel.create(fa, compiler);
		model.train(data, maxIter);
	}
	
}

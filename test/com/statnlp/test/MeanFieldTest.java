package com.statnlp.test;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkConfig.InferenceType;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.projects.nndcrf.factorialCRFs.Chunk;
import com.statnlp.projects.nndcrf.factorialCRFs.FCRFConfig.TASK;
import com.statnlp.projects.nndcrf.factorialCRFs.FCRFFeatureManager;
import com.statnlp.projects.nndcrf.factorialCRFs.FCRFInstance;
import com.statnlp.projects.nndcrf.factorialCRFs.FCRFNetworkCompiler;
import com.statnlp.projects.nndcrf.factorialCRFs.Tag;

import cern.colt.Arrays;
import junit.framework.TestCase;

public class MeanFieldTest extends TestCase {

	/**
	 * The training data.
	 */
	protected FCRFInstance[] data;
	
	protected int maxIter;
	
	/**
	 * Some initialization here.
	 */
	protected void setUp(){
		WordToken[] wts = new WordToken[2];
		wts[0] = new WordToken("a", "NN", -1, "B-PER");
		wts[1] = new WordToken("b", "VB", -1, "O");
		Sentence sent = new Sentence(wts);
		FCRFInstance inst = new FCRFInstance(1, 1.0, sent);
		ArrayList<String> output = new ArrayList<>(2);
		output.add("B-PER");
		output.add("O");
		inst.setChunks(output);
		inst.setLabeled();
		data = new FCRFInstance[]{inst};
		//Initialize the two maps
		Chunk.get("B-PER");
		Chunk.get("O");
		Tag.get("NN");
		Tag.get("VB");
		Chunk.lock();
		Tag.lock();
		//Setup the configuration
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig.CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = 0;
		NetworkConfig.NUM_THREADS = 8;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = true;
		NetworkConfig.BUILD_FEATURES_FROM_LABELED_ONLY = false;
		NetworkConfig.NUM_STRUCTS = 2;
		NetworkConfig.INFERENCE = InferenceType.MEAN_FIELD;
		maxIter = 100;
		System.out.println(Chunk.CHUNKS_INDEX.toString());
		System.out.println(Tag.TAGS_INDEX.toString());
	}
	
	public void testMeanField() throws InterruptedException{
		NetworkConfig.MAX_MF_UPDATES = 4;
		GlobalNetworkParam param_g = new GlobalNetworkParam();
		FCRFFeatureManager fa = new FCRFFeatureManager(param_g, true, false, TASK.JOINT, 3, false);
		FCRFNetworkCompiler compiler = new FCRFNetworkCompiler(TASK.JOINT, false, 2);
		NetworkModel model = DiscriminativeNetworkModel.create(fa, compiler);
		model.train(data, maxIter);
		/**Printing the features**/
		for (int w = 0; w < fa.getParam_G().getWeights().length; w++){
			System.out.println(w + "\t" + Arrays.toString(fa.getParam_G().getFeatureRep(w)));
		}
		/****/
	}
	
}

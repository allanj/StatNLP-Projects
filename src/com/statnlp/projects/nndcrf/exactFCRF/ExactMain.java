package com.statnlp.projects.nndcrf.exactFCRF;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import com.statnlp.commons.ml.opt.GradientDescentOptimizerFactory;
import com.statnlp.commons.ml.opt.OptimizerFactory;
import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.neural.NeuralConfig;
import com.statnlp.neural.NeuralConfigReader;

public class ExactMain {

	public static int trainNumber = -100;
	public static int testNumber = -100;
	public static int numIteration = -100;
	public static int numThreads = -100;
	public static String trainFile = "";
	public static String testFile = "";
	public static String output;
	public static String neural_config = "config/exactneural.config";
	public static OptimizerFactory optimizer = OptimizerFactory.getLBFGSFactory();
	public static boolean IOBESencoding = true;
	public static boolean npchunking = true;
	/** Cascaded CRF option. **/
	public static int windowSize = 5;
	public static String modelFile = "data/conll2000/model";
	/** The option to save model **/
	public static boolean saveModel = false;
	/** The option to use existing model **/
	public static boolean useExistingModel = false;
	public static int randomSeed = 1234;
	
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException{
		
		trainNumber = 2;
		testNumber = 2;
		numThreads = 5;
		numIteration = 200;
		trainFile = ExactConfig.CONLL_train;
		testFile = ExactConfig.CONLL_test;
		output = ExactConfig.exactOut;
		
		processArgs(args);
		
		List<ExactInstance> trainInstances = null;
		List<ExactInstance> testInstances = null;
		/***********DEBUG*****************/
		trainFile = "data/conll2000/train.txt";
//		trainNumber = 2;
		testFile = "data/conll2000/test.txt";;
//		testNumber = 2;
//		numIteration = 1000;   
//		testFile = trainFile;
//		NetworkConfig.MAX_MF_UPDATES = 6;
//		useJointFeatures = true;
//		task = TASK.JOINT;
		IOBESencoding = true;
//		saveModel = false;
		//modelFile = "data/conll2000/model";
//		useExistingModel = false;
		npchunking = true;
		ExactConfig.l2val = 0.01;
		NetworkConfig.AVOID_DUPLICATE_FEATURES = true;
		NetworkConfig.RANDOM_INIT_FEATURE_SEED = randomSeed;
//		cascade = true;
//		testFile = "data/conll2000/NP_chunk_final_prediction.txt";
//		npchunking = true;
//		testFile = "data/conll2000/POS_final_prediction.txt";
//		optimizer = OptimizerFactory.getGradientDescentFactoryUsingAdaM(0.0001, 0.9, 0.999, 10e-8);
		/***************************/
		
		System.err.println("[Info] trainingFile: "+trainFile);
		System.err.println("[Info] testFile: "+testFile);
		System.err.println("[Info] output: " + output);
		
		trainInstances = ExactReader.readCONLLData(trainFile, true, trainNumber, npchunking, IOBESencoding);
		testInstances = ExactReader.readCONLLData(testFile, false, testNumber, npchunking, false);
		
//		trainInstances = FCRFReader.readGRMMData("data/conll2000/conll2000.train1k.txt", true, -1);
//		testInstances = FCRFReader.readGRMMData("data/conll2000/conll2000.test1k.txt", false, -1);
		
		
		ChunkLabel.lock();
		TagLabel.lock();
		ExactConfig.concatExactLabel();
		ExactLabel.lock();
		
		
		System.err.println("chunk size:"+ChunkLabel.CHUNKS_INDEX.toString());
		System.err.println("tag size:"+TagLabel.TAGS.size());
		System.err.println("tag size:"+TagLabel.TAGS_INDEX.toString());
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig.CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = ExactConfig.l2val;
		NetworkConfig.NUM_THREADS = numThreads;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = true;
		NetworkConfig.BUILD_FEATURES_FROM_LABELED_ONLY = false;
		
		/***Neural network Configuration**/
//		NetworkConfig.USE_NEURAL_FEATURES = false; 
		if(NetworkConfig.USE_NEURAL_FEATURES)
			NeuralConfigReader.readConfig(neural_config);
		/****/
		
		GlobalNetworkParam param_g = null; 
		if(useExistingModel){
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(modelFile));
			param_g = (GlobalNetworkParam)in.readObject();
			in.close();
		}else{
			param_g = new GlobalNetworkParam(optimizer);
		}
		
		FeatureManager fa = null;
		fa = new ExactFeatureManager(param_g, windowSize);
//		fa = new GRMMFeatureManager(param_g, useJointFeatures);
		ExactNetworkCompiler compiler = new ExactNetworkCompiler(IOBESencoding);
		NetworkModel model = DiscriminativeNetworkModel.create(fa, compiler);
		ExactInstance[] ecrfs = trainInstances.toArray(new ExactInstance[trainInstances.size()]);
		
		if(!useExistingModel){
			if(NetworkConfig.USE_NEURAL_FEATURES){
				ExactInstance[] allInsts = new ExactInstance[trainInstances.size()+testInstances.size()];
				int i = 0;
		        for(; i<trainInstances.size(); i++) {
		        	allInsts[i] = trainInstances.get(i);
		        }
		        int lastId = allInsts[i-1].getInstanceId();
		        for(int j = 0; j<testInstances.size(); j++, i++) {
		        	allInsts[i] = testInstances.get(j);
		        	allInsts[i].setInstanceId(lastId+j+1);
		        	allInsts[i].setUnlabeled();
		        }
		        model.train(allInsts, trainInstances.size(), numIteration);
			}else{
				System.out.println("Training Instance size: " + ecrfs.length);
				model.train(ecrfs, numIteration);
			}
		}
		
		if(saveModel){
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(modelFile));
			out.writeObject(param_g);
			out.close();
		}
		
		Instance[] predictions = model.decode(testInstances.toArray(new ExactInstance[testInstances.size()]));
		/**Evaluation part**/
		ExactEval.evalFscore(predictions, output+".eval");
		ExactEval.evalPOSAcc(predictions);
		ExactEval.evalJointAcc(predictions, output);
	}

	
	/**
	 * Process the input argument
	 * @param args: arguments
	 */
	public static void processArgs(String[] args){
		if(args.length>1 && (args[0].equals("-h") || args[0].equals("help") || args[0].equals("-help") )){
			System.err.println("Factorial CRFs for Joint NP Chunking and POS tagging ");
			System.err.println("\t usage: java -jar fcrf.jar -trainNum -1 -testNum -1 -thread 5 -iter 100 -pipe true");
			System.err.println("\t put numTrainInsts/numTestInsts = -1 if you want to use all the training/testing instances");
			System.exit(0);
		}else{
			for(int i=0;i<args.length;i=i+2){
				switch(args[i]){
					case "-trainNum": trainNumber = Integer.valueOf(args[i+1]); break;
					case "-testNum": testNumber = Integer.valueOf(args[i+1]); break;
					case "-iter": numIteration = Integer.valueOf(args[i+1]); break;
					case "-thread": numThreads = Integer.valueOf(args[i+1]); break;
					case "-seed": randomSeed = Integer.valueOf(args[i+1]); break;
					case "-testFile": testFile = args[i+1]; break;
					case "-reg": ExactConfig.l2val = Double.valueOf(args[i+1]); break;
					case "-windows":ExactConfig.windows = args[i+1].equals("true")? true:false; break;
					case "-iobes": 		IOBESencoding = args[i+1].equals("true")? true:false; break;
					case "-npchunking": npchunking = args[i+1].equals("true")? true:false; break;
					case "-optim": 
						if(args[i+1].equals("lbfgs"))
							optimizer = GradientDescentOptimizerFactory.getLBFGSFactory();
						else if(args[i+1].equals("adagrad")) {
							//specify the learning rate also 
							if(args[i+2].startsWith("-")) {System.err.println("Please specify the learning rate for adagrad.");System.exit(0);}
							optimizer = GradientDescentOptimizerFactory.getGradientDescentFactoryUsingAdaGrad(Double.valueOf(args[i+2]));
							i=i+1;
						}else if(args[i+1].equals("adam")){
							if(args[i+2].startsWith("-")) {System.err.println("Please specify the learning rate for adam.");System.exit(0);}
							//default should be 1e-3
							optimizer = GradientDescentOptimizerFactory.getGradientDescentFactoryUsingAdaM(Double.valueOf(args[i+2]), 0.9, 0.999, 10e-8);
							i=i+1;
						}else{
							System.err.println("No optimizer named: "+args[i+1]+"found..");System.exit(0);
						}
						break;
					case "-wsize": 	 windowSize = Integer.valueOf(args[i+1]); break; //default: 5. the window size of neural feature.
					case "-mode": 	if (args[i+1].equals("train")){
										//train also test the file
										useExistingModel = false;
										saveModel = true;
										modelFile = args[i+2];
								  	}else if (args[i+1].equals("test")){
								  		useExistingModel = true;
										saveModel = false;
										modelFile = args[i+2];
								  	}else if (args[i+1].equals("train-only")){
								  		useExistingModel = false;
										saveModel = false;
										modelFile = args[i+2];
								  	}else{
										System.err.println("Unknown mode: "+args[i+1]+" found..");System.exit(0);
									}
									i = i + 1;
								break;
					case "-neural": if(args[i+1].equals("true")){ 
						NetworkConfig.USE_NEURAL_FEATURES = true;
						NetworkConfig.OPTIMIZE_NEURAL = true;  //false: optimize in neural network
						NetworkConfig.IS_INDEXED_NEURAL_FEATURES = false; //only used when using the senna embedding.
						NetworkConfig.REGULARIZE_NEURAL_FEATURES = true; //true means regularize in the crf part
						NeuralConfig.NUM_NEURAL_NETS = 1;
					}
					break;
					default: System.err.println("Invalid arguments :"+args[i]+", please check usage."); System.exit(0);
				}
			}
			System.err.println("[Info] trainNum: "+trainNumber);
			System.err.println("[Info] testNum: "+testNumber);
			System.err.println("[Info] numIter: "+numIteration);
			System.err.println("[Info] numThreads: "+numThreads);
			System.err.println("[Info] Regularization Parameter: "+ExactConfig.l2val);	
		}
	}
	
	
}

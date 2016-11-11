package com.statnlp.projects.nndcrf.factorialCRFs;

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
import com.statnlp.hybridnetworks.NetworkConfig.InferenceType;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.neural.NeuralConfig;
import com.statnlp.neural.NeuralConfigReader;
import com.statnlp.projects.nndcrf.factorialCRFs.TFConfig.TASK;

public class FCRFMain {

	public static String[] entities; 
	public static int trainNumber = -100;
	public static int testNumber = -100;
	public static int numIteration = -100;
	public static int numThreads = -100;
	public static String trainFile = "";
	public static String testFile = "";
	public static String nerOut;
	public static String posOut;
	public static String neural_config = "config/fcrfneural.config";
	public static OptimizerFactory optimizer = OptimizerFactory.getLBFGSFactory();
	public static boolean useJointFeatures = true;
	public static TASK task = TASK.JOINT;
	public static boolean IOBESencoding = true;
	public static boolean npchunking = true;
	public static boolean cascade = false;
	public static int windowSize = 5;
	public static String modelFile = "data/conll2000/model";
	public static boolean saveModel = false;
	public static boolean useExistingModel = false;
	
	public static void main(String[] args) throws IOException, InterruptedException{
		// TODO Auto-generated method stub
		
		trainNumber = 80;
		testNumber = 2;
		numThreads = 5;
		numIteration = 200;
		trainFile = TFConfig.CONLL_train;
		testFile = TFConfig.CONLL_test;
		nerOut = TFConfig.nerOut;
		posOut = TFConfig.posOut;
		
		processArgs(args);
	
		
		List<TFInstance> trainInstances = null;
		List<TFInstance> testInstances = null;
		/***********DEBUG*****************/
		trainFile = "data/conll2000/train.txt";
		trainNumber = 100;
		testFile = "data/conll2000/test.txt";;
		testNumber = 100;
		numIteration = 400;   
//		testFile = trainFile;
		NetworkConfig.MF_ROUND = 4;
		useJointFeatures = true;
		task = TASK.JOINT;
		IOBESencoding = true;
		saveModel = false;
		modelFile = "data/conll2000/model";
		useExistingModel = false;
//		cascade = true;
//		testFile = "data/conll2000/NP_chunk_final_prediction.txt";
//		npchunking = true;
//		testFile = "data/conll2000/POS_final_prediction.txt";
//		optimizer = OptimizerFactory.getGradientDescentFactoryUsingAdaM(0.0001, 0.9, 0.999, 10e-8);
		/***************************/
		
		System.err.println("[Info] trainingFile: "+trainFile);
		System.err.println("[Info] testFile: "+testFile);
		System.err.println("[Info] nerOut: "+nerOut);
		System.err.println("[Info] posOut: "+posOut);
		System.err.println("[Info] task: "+task.toString());
		
		trainInstances = TFReader.readCONLLData(trainFile, true, trainNumber, npchunking, IOBESencoding, task);
		boolean iobesOnTest = task==TASK.TAGGING && cascade? true:false;
		testInstances = TFReader.readCONLLData(testFile, false, testNumber, npchunking, iobesOnTest, task, cascade);
		
//		trainInstances = TFReader.readGRMMData("data/conll2000/conll2000.train1k.txt", true, -1);
//		testInstances = TFReader.readGRMMData("data/conll2000/conll2000.test1k.txt", false, -1);
		
		
		Entity.lock();
		Tag.lock();
		
		
		System.err.println("entity size:"+Entity.ENTS_INDEX.toString());
		System.err.println("tag size:"+Tag.TAGS.size());
		System.err.println("tag size:"+Tag.TAGS_INDEX.toString());
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig.CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = TFConfig.l2val;
		NetworkConfig.NUM_THREADS = numThreads;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = true;
		NetworkConfig.BUILD_FEATURES_FROM_LABELED_ONLY = false;
		NetworkConfig.NUM_STRUCTS = 2;
		NetworkConfig.INFERENCE = task==TASK.JOINT? InferenceType.MEAN_FIELD:InferenceType.FORWARD_BACKWARD;
		
		
		
		/***Neural network Configuration**/
		NetworkConfig.USE_NEURAL_FEATURES = false; 
		if(NetworkConfig.USE_NEURAL_FEATURES)
			NeuralConfigReader.readConfig(neural_config);
		NetworkConfig.OPTIMIZE_NEURAL = false;  //false: optimize in neural network
		NetworkConfig.IS_INDEXED_NEURAL_FEATURES = false; //only used when using the senna embedding.
		NetworkConfig.REGULARIZE_NEURAL_FEATURES = false; //true means regularize in the crf part
		NeuralConfig.NUM_NEURAL_NETS = 2;
		/****/
		
		
		GlobalNetworkParam param_g = null; 
		if(useExistingModel){
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(modelFile));
			try {
				param_g = (GlobalNetworkParam)in.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			in.close();
		}else{
			param_g = new GlobalNetworkParam(optimizer);
		}
		param_g = new GlobalNetworkParam(optimizer);
		
		FeatureManager fa = null;
		fa = new TFFeatureManager(param_g, useJointFeatures, cascade, task, windowSize);
//		fa = new GRMMFeatureManager(param_g);
		TFNetworkCompiler compiler = new TFNetworkCompiler(task,IOBESencoding);
		NetworkModel model = DiscriminativeNetworkModel.create(fa, compiler);
		TFInstance[] ecrfs = trainInstances.toArray(new TFInstance[trainInstances.size()]);
		
		if(!useExistingModel){
			if(NetworkConfig.USE_NEURAL_FEATURES){
				TFInstance[] allInsts = new TFInstance[trainInstances.size()+testInstances.size()];
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
				model.train(ecrfs, numIteration);
			}
		}
		
		if(saveModel){
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(modelFile));
			out.writeObject(param_g);
			out.close();
		}
		
		
		Instance[] predictions = model.decode(testInstances.toArray(new TFInstance[testInstances.size()]));
		/**Evaluation part**/
		if(task == TASK.NER || task == TASK.JOINT){
			TFEval.evalNER(predictions, nerOut);
			TFEval.evalSingleE(predictions);
		}
		if(task == TASK.TAGGING || task == TASK.JOINT){
			TFEval.evalPOS(predictions, posOut);
		}
		if(task == TASK.JOINT)
			TFEval.evalSingleJoint(predictions);
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
					case "-testFile": testFile = args[i+1]; break;
					case "-reg": TFConfig.l2val = Double.valueOf(args[i+1]); break;
					case "-windows":TFConfig.windows = args[i+1].equals("true")? true:false; break;
					case "-mfround":NetworkConfig.MF_ROUND = Integer.valueOf(args[i+1]);
									if(NetworkConfig.MF_ROUND==0) useJointFeatures = false;
									break;
					case "-task": 
						if(args[i+1].equals("ner"))  task = TASK.NER;
						else if (args[i+1].equals("tagging")) task  = TASK.TAGGING;
						else if (args[i+1].equals("joint"))  task  = TASK.JOINT;
						else throw new RuntimeException("Unknown task:"+args[i+1]+"?"); break;
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
					case "-cascade": cascade = args[i+1].equals("true")? true:false; break;
					case "-wsize": 	 windowSize = Integer.valueOf(args[i+1]); break; //default: 5. the window size of neural feature.
					case "-nerout":  nerOut = args[i+1]; break; //default: name is output/nerout
					case "-posout":  posOut = args[i+1]; break; //default: name is output/pos_out;
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
					default: System.err.println("Invalid arguments :"+args[i]+", please check usage."); System.exit(0);
				}
			}
			System.err.println("[Info] trainNum: "+trainNumber);
			System.err.println("[Info] testNum: "+testNumber);
			System.err.println("[Info] numIter: "+numIteration);
			System.err.println("[Info] numThreads: "+numThreads);
			System.err.println("[Info] Regularization Parameter: "+TFConfig.l2val);	
		}
	}
	
	
}

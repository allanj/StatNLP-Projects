package com.statnlp.dp.model.bruteforce;

import java.io.IOException;
import java.util.List;

import com.statnlp.commons.types.Instance;
import com.statnlp.dp.utils.DPConfig;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkModel;

public class BFMain {

	public static int trainNumber = -100;
	public static int testNumber = -100;
	public static int numIteration = -100;
	public static int numThreads = -100;
	public static String testFile = "";
	public static String nerOut;
	public static String dpOut;
	public static String nerRes;
	public static boolean isDev;
	
	public static void main(String[] args) throws IOException, InterruptedException{
		// TODO Auto-generated method stub
		
		trainNumber = 80;
		testNumber = 2;
		numThreads = 5;
		numIteration = 200;
		processArgs(args);
		String modelType = "brute-force";
		
		
		String middle = isDev? ".dev":".test";
		dpOut = DPConfig.data_prefix+modelType+middle+DPConfig.dp_res_suffix;
		nerOut = DPConfig.data_prefix+modelType+middle+DPConfig.ner_eval_suffix;
		nerRes = DPConfig.data_prefix+modelType+middle+DPConfig.ner_res_suffix;
		testFile = isDev? DPConfig.devPath:DPConfig.testingPath;
		System.err.println("[Info] trainingFile: "+DPConfig.trainingPath);
		System.err.println("[Info] testFile: "+testFile);
		System.err.println("[Info] nerOut: "+nerOut);
		System.err.println("[Info] nerRes: "+nerRes);
		
		List<BFInstance> trainInstances = null;
		List<BFInstance> testInstances = null;
		/***********DEBUG*****************/
		DPConfig.trainingPath = "data/semeval10t1/debug.conllx";
//		testFile="data/semeval10t1/ecrf.smalltest.txt";
		numThreads = 4;
		trainNumber = 50;
		testNumber = 50;
		testFile = DPConfig.trainingPath;
//		testFile = "data/semeval10t1/ecrf.test.part.txt";
		/***************************/
		
		trainInstances = BFReader.readData(DPConfig.trainingPath,true,trainNumber);
		testInstances = BFReader.readData(testFile,false,testNumber);
//		Formatter.ner2Text(trainInstances, "data/testRandom2.txt");
//		System.exit(0);
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig.CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = DPConfig.L2;
		NetworkConfig.NUM_THREADS = numThreads;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = false;
		
		BFFeatureManager fa = new BFFeatureManager(new GlobalNetworkParam());
		BFNetworkCompiler compiler = new BFNetworkCompiler();
		NetworkModel model = DiscriminativeNetworkModel.create(fa, compiler);
		BFInstance[] ecrfs = trainInstances.toArray(new BFInstance[trainInstances.size()]);
		model.train(ecrfs, numIteration);
//		Instance[] predictions = model.decode(testInstances.toArray(new BFInstance[testInstances.size()]));
//		BFEval.evalDP(predictions, dpOut);
	}

	
	
	public static void processArgs(String[] args){
		if(args[0].equals("-h") || args[0].equals("help") || args[0].equals("-help") ){
			System.err.println("Linear-Chain CRF Version: Joint DEPENDENCY PARSING and Entity Recognition TASK: ");
			System.err.println("\t usage: java -jar dpe.jar -trainNum -1 -testNum -1 -thread 5 -iter 100 -pipe true");
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
					case "-reg": DPConfig.L2 = Double.valueOf(args[i+1]); break;
					case "-dev":isDev = args[i+1].equals("true")?true:false; break;
					case "-windows":DPConfig.windows = true; break;
					default: System.err.println("Invalid arguments, please check usage."); System.exit(0);
				}
			}
			System.err.println("[Info] trainNum: "+trainNumber);
			System.err.println("[Info] testNum: "+testNumber);
			System.err.println("[Info] numIter: "+numIteration);
			System.err.println("[Info] numThreads: "+numThreads);
			System.err.println("[Info] Using development set??: "+isDev);
			System.err.println("[Info] Data type: "+DPConfig.dataType);
			System.err.println("[Info] Regularization Parameter: "+DPConfig.L2);
		}
	}
}

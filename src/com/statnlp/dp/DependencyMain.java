package com.statnlp.dp;

import java.io.IOException;

import com.statnlp.commons.types.Instance;
import com.statnlp.dp.utils.DPConfig;
import com.statnlp.dp.utils.Init;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkModel;

/**
 * Dependency Parsing
 * According to the paper written by: Jason Eisner
 * Paper: Three New Probabilistic Models for Dependency Parsing: An Exploration 
 * @author zhanming_jie
 * Remember to check the sentence length..because I already add one root.
 */
public class DependencyMain {
	
	public static String[] entities; 
	public static int trainNumber = 20;
	public static int testNumber = 20;
	public static int numIteration = 300;
	public static int numThreads = 5;
	public static String testFile = DPConfig.testingPath;
	public static boolean isPipe = true;
	public static String trainingPath = DPConfig.trainingPath;
	public static boolean isDev;
	
	public static void main(String[] args) throws InterruptedException, IOException {
	
		entities = Init.initializeTypeMap();
		processArgs(args);
		
		String middle = isDev? ".dev":".test";
		String modelType = "only";
		String dpOut = DPConfig.data_prefix+modelType+middle+DPConfig.dp_res_suffix;
		testFile = isDev? DPConfig.devPath:DPConfig.testingPath;
		
		if(isPipe) {
			testFile = isDev?DPConfig.ner2dp_ner_dev_input: DPConfig.ner2dp_ner_test_input;
			dpOut = DPConfig.data_prefix+middle+".pp.ner2dp.dp.res.txt";
		}
		System.err.println("[Info] DEBUG MODE: "+DPConfig.DEBUG);
		
		System.err.println("[Info] testFile: "+testFile);
		System.err.println("[Info] nerOut: "+dpOut);
		
		DependencyTransformer trans = new DependencyTransformer();
		DependInstance[] trainingInsts = DependencyReader.readInstance(trainingPath, true,trainNumber,entities,trans);
		
		DependInstance[] testingInsts = isPipe? DependencyReader.readFromPipeline(testFile,testNumber,trans): DependencyReader.readInstance(testFile, false,testNumber,entities,trans);
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig._CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig._numThreads = numThreads;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = 0.7;
		NetworkConfig._SEQUENTIAL_FEATURE_EXTRACTION = false;
		DependencyFeatureManager dfm = new DependencyFeatureManager(new GlobalNetworkParam(), isPipe);
		DependencyNetworkCompiler dnc = new DependencyNetworkCompiler();
		NetworkModel model = DiscriminativeNetworkModel.create(dfm, dnc);
		model.train(trainingInsts, numIteration); 
		
		/****************Evaluation Part**************/
		Instance[] predInsts = model.decode(testingInsts);
		Evaluator.evalDP(predInsts, dpOut);
		
	}
	
	
	public static void processArgs(String[] args){
		String usage = "\t usage: java -jar dp.jar -trainNum -1 -testNum -1 -thread 5 -iter 100 -train path -test path -pipe true";
		if(args[0].equals("-h") || args[0].equals("help") || args[0].equals("-help") ){
			System.err.println("Version: Only Dependency Parsing task TASK: ");
			System.err.println(usage);
			System.exit(0);
		}else{
			for(int i=0;i<args.length;i=i+2){
				switch(args[i]){
					case "-trainNum": trainNumber = Integer.valueOf(args[i+1]); break;
					case "-testNum": testNumber = Integer.valueOf(args[i+1]); break;
					case "-iter": numIteration = Integer.valueOf(args[i+1]); break;
					case "-thread": numThreads = Integer.valueOf(args[i+1]); break;
					case "-pipe": isPipe = args[i+1].equals("true")?true:false; break;
					case "-train":trainingPath = args[i+1];break;
					case "-test": testFile = args[i+1];break;
					case "-debug": DPConfig.DEBUG = args[i+1].equals("true")? true:false; break;
					case "-reg": DPConfig.L2 = Double.valueOf(args[i+1]); break;
					case "-dev":isDev = args[i+1].equals("true")?true:false; break;
					default: System.err.println("Invalid arguments, please check usage."); System.err.println(usage);System.exit(0);
				}
			}

			System.err.println("[Info] trainNum: "+trainNumber);
			System.err.println("[Info] testNum: "+testNumber);
			System.err.println("[Info] numIter: "+numIteration);
			System.err.println("[Info] numThreads: "+numThreads);
			System.err.println("[Info] is Pipeline: "+isPipe);
			System.err.println("[Info] train path: "+trainingPath);
			System.err.println("[Info] Using development set??: "+isDev);
			System.err.println("[Info] Regularization Parameter: "+DPConfig.L2);
			if(isPipe){
				System.err.println("[Info] *********PipeLine: from NER res to DP****");
			}
			
		}
	}

	
}

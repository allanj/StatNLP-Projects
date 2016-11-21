package com.statnlp.projects.dep;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.neural.NeuralConfigReader;
import com.statnlp.projects.dep.commons.DepLabel;
import com.statnlp.projects.dep.utils.DPConfig;
import com.statnlp.projects.dep.utils.Init;

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
	public static String testFile;
	public static boolean isPipe = false;
	public static String trainingPath;
	public static boolean isDev = false;
	public static HashSet<String> dataTypeSet;
	public static boolean topKinput = false;
	public static boolean labeledDep = false;
	public static int windowSize = 1; //for neural features
	public static boolean basicFeatures = true;
	
	public static String[] initializeTypeMap(){
		HashMap<String, Integer> typeMap = new HashMap<String, Integer>();
		typeMap.put("O", 0);
		typeMap.put("person", 1);  typeMap.put("gpe", 2);  
		typeMap.put("EMPTY", 5); 
		typeMap.put("organization", 3);
		typeMap.put("MISC", 4);
		String[] entities = new String[typeMap.size()];
		Iterator<String> iter = typeMap.keySet().iterator();
		while(iter.hasNext()){
			String entity = iter.next();
			entities[typeMap.get(entity)] = entity;
		}
		return entities;
	}
	
	public static void main(String[] args) throws InterruptedException, IOException {
	
		entities = initializeTypeMap();
		dataTypeSet = Init.iniOntoNotesData();
		
		testFile = DPConfig.testingPath;
		
		processArgs(args);
		DPConfig.writeWeight = false;
		
		trainingPath = DPConfig.trainingPath;
		
		String middle = isDev? ".dev":".test";
		String modelType = "dep";
		String dpOut = DPConfig.data_prefix+modelType+middle+DPConfig.dp_res_suffix;
		String topKDepOut = DPConfig.data_prefix+modelType+middle+DPConfig.dp_topk_res_suffix;
		testFile = isDev? DPConfig.devPath:DPConfig.testingPath;
		
		if(isPipe) {
			testFile = isDev?DPConfig.ner2dp_ner_dev_input: DPConfig.ner2dp_ner_test_input;
			if(topKinput)
				testFile = isDev?DPConfig.ner2dp_ner_dev_input:DPConfig.ner2dp_ner_topK_test_input;
			dpOut = DPConfig.data_prefix+middle+".pp.ner2dp.dp.res.txt";
		}
		/****Debug info****/
//		trainingPath = "data/semeval10t1/small.txt";
//		testFile = trainingPath;
		/****/
		System.err.println("[Info] DEBUG MODE: "+DPConfig.DEBUG);
		System.err.println("[Info] is pipeline: "+ isPipe);
		System.err.println("[Info] train path: "+trainingPath);
		System.err.println("[Info] testFile: "+testFile);
		System.err.println("[Info] depOut: "+dpOut);
		System.err.println("[Info] topKDepOut: "+topKDepOut);
		System.err.println("[Info] Regularization: "+DPConfig.L2);
		
		if(NetworkConfig.USE_NEURAL_FEATURES){
			System.err.println("[Info] Reading the neural configuration....");
			NeuralConfigReader.readConfig("config/dep.config");
		}
		
		
		DependencyTransformer trans = new DependencyTransformer();
		DependInstance[] trainingInsts = null;
		DependInstance[] testingInsts = null;
		trainingInsts = DependencyReader.readCoNLLX(trainingPath, true, trainNumber, trans, true); //true: check projective
		boolean checkTestProjective = isDev? true:false;
		testingInsts =   isPipe? DependencyReader.readFromPipeline(testFile,testNumber,trans, topKinput): 
								DependencyReader.readCoNLLX(testFile, false,testNumber,trans, checkTestProjective);   //false: not check the projective in testing
		System.err.println("[Info] Total number of dependency label:"+DepLabel.LABELS.size());
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig.CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig.NUM_THREADS = numThreads;
		//0.1 is the best after tunning the parameters
		NetworkConfig.L2_REGULARIZATION_CONSTANT = DPConfig.L2; //default is 0.1
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = true;
		System.err.println("[Info] Regularization Parameter: "+NetworkConfig.L2_REGULARIZATION_CONSTANT);
		
		
		
		DependencyFeatureManager dfm = new DependencyFeatureManager(new GlobalNetworkParam(), isPipe, labeledDep, windowSize, basicFeatures);
		DependencyNetworkCompiler dnc = new DependencyNetworkCompiler(labeledDep);
		NetworkModel model = DiscriminativeNetworkModel.create(dfm, dnc);
		
		DependInstance all_instances[] = new DependInstance[trainingInsts.length+testingInsts.length];
        int i = 0;
        for(; i<trainingInsts.length; i++) {
            all_instances[i] = trainingInsts[i];
        }
        int lastId = all_instances[i-1].getInstanceId();
        for(int j = 0; j<testingInsts.length; j++, i++) {
            all_instances[i] = testingInsts[j];
            all_instances[i].setInstanceId(lastId+j+1);
            all_instances[i].setUnlabeled();
        }
        if(NetworkConfig.USE_NEURAL_FEATURES){
			model.train(all_instances, trainingInsts.length, numIteration);
		}else{
			model.train(trainingInsts, numIteration);
		}
		
		/****************Evaluation Part**************/
		Instance[] predInsts = model.decode(testingInsts);
		Evaluator.evalDP(predInsts, dpOut, labeledDep);
		if(NetworkConfig._topKValue>1)
			Evaluator.outputTopKDep(predInsts, topKDepOut);
		
	}
	
	
	public static void processArgs(String[] args){
		String usage = "\t usage: java -jar dp.jar -trainNum -1 -testNum -1 -thread 5 -iter 100 -train path -test path -pipe false";
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
					case "-train":DPConfig.trainingPath = args[i+1];break;
					case "-test": DPConfig.testingPath = args[i+1];break;
					case "-debug": DPConfig.DEBUG = args[i+1].equals("true")? true:false; break;
					case "-reg": DPConfig.L2 = Double.valueOf(args[i+1]); break;
					case "-dev":isDev = args[i+1].equals("true")?true:false; break;
					case "-windows":DPConfig.windows = true; break;
					case "-comb": DPConfig.comb = true; break;
					case "-data":DPConfig.dataType=args[i+1];DPConfig.changeDataType(); break;
					case "-wpath":DPConfig.weightPath=args[i+1]; DPConfig.writeWeight = true; break;
					case "-topk":NetworkConfig._topKValue = Integer.valueOf(args[i+1]); break;
					case "-topkinput": topKinput = true; break;
					case "-las": labeledDep= args[i+1].equals("true")?true:false; break;
					case "-windowSize": windowSize = Integer.valueOf(args[i+1]); break;
					case "-neural": if(args[i+1].equals("true")){ 
										NetworkConfig.USE_NEURAL_FEATURES = true; 
										NetworkConfig.OPTIMIZE_NEURAL = true;  //not optimize in CRF..
										NetworkConfig.IS_INDEXED_NEURAL_FEATURES = false; //only used when using the senna embedding.
										NetworkConfig.REGULARIZE_NEURAL_FEATURES = true; // Regularized the neural features in CRF or not
									} break;
					case "-basicf": basicFeatures = args[i+1].equals("true") ? true : false; break;
					default: System.err.println("Invalid argument " + args[i] + ", please check usage."); System.err.println(usage);System.exit(0);
				}
			}

			if(DPConfig.comb){
				DPConfig.changeTrainingPath();
			}
			System.err.println("[Info] trainNum: "+trainNumber);
			System.err.println("[Info] testNum: "+testNumber);
			System.err.println("[Info] numIter: "+numIteration);
			System.err.println("[Info] numThreads: "+numThreads);
			System.err.println("[Info] is Pipeline: "+isPipe);
			System.err.println("[Info] Using development set??: "+isDev);
			if(isPipe){
				System.err.println("[Info] *********PipeLine: from NER res to DP****");
			}
			
		}
	}

	
}

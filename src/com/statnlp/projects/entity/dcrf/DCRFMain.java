package com.statnlp.projects.entity.dcrf;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.projects.dep.utils.Init;

public class DCRFMain {

	public static String[] entities; 
	public static int trainNumber = -100;
	public static int testNumber = -100;
	public static int numIteration = -100;
	public static int numThreads = -100;
	public static String testFile = "";
	public static String nerOut;
	public static String nerRes;
	public static String posOut;
	public static boolean isDev;
	public static String[] selectedEntities = {"person","organization","gpe","MISC"};
	public static HashSet<String> dataTypeSet;
	public static HashMap<String, Integer> entityMap;
	
	public static void initializeEntityMap(){
		entityMap = new HashMap<String, Integer>();
		int index = 0;
		entityMap.put("O", index++);  
		for(int i=0;i<selectedEntities.length;i++){
			entityMap.put("B-"+selectedEntities[i], index++);
			entityMap.put("I-"+selectedEntities[i], index++);
		}
		entities = new String[entityMap.size()];
		Iterator<String> iter = entityMap.keySet().iterator();
		while(iter.hasNext()){
			String entity = iter.next();
			entities[entityMap.get(entity)] = entity;
		}
	}

	
	
	public static void main(String[] args) throws IOException, InterruptedException{
		// TODO Auto-generated method stub
		
		trainNumber = 80;
		testNumber = 2;
		numThreads = 5;
		numIteration = 200;
		processArgs(args);
		dataTypeSet = Init.iniOntoNotesData();
		initializeEntityMap();
		String modelType = "Dyanmic CRF";
		
		
		String middle = isDev? ".dev":".test";
		nerOut = DCRFConfig.nerOut;
		nerRes = DCRFConfig.nerRes;
		posOut = DCRFConfig.posOut;
		testFile = isDev? DCRFConfig.CONLL_dev:DCRFConfig.CONLL_test;
		System.err.println("[Info] trainingFile: "+DCRFConfig.CONLL_train);
		System.err.println("[Info] testFile: "+testFile);
		System.err.println("[Info] nerOut: "+nerOut);
		System.err.println("[Info] nerRes: "+nerRes);
		
		List<DCRFInstance> trainInstances = null;
		List<DCRFInstance> testInstances = null;
		/***********DEBUG*****************/
//		DCRFConfig.CONLL_train = "dcrf/dat/small.txt";
//		testFile="data/semeval10t1/ecrf.smalltest.txt";
//		DPConfig.writeWeight = true;
//		DPConfig.weightPath = "data/semeval10t1/ecrfWeight.txt";
//		DPConfig.readWeight = false;
//		testFile = DPConfig.ecrftrain;
//		testFile = "data/semeval10t1/ecrf.test.part.txt";
		NetworkConfig.MAX_MARGINAL_DECODING = true;
		trainNumber = 100;
		testNumber = 100;
		testFile = DCRFConfig.CONLL_train;
		DCRFConfig.dataType = "conll";
		/***************************/
		
		DCRFViewer viewer = new DCRFViewer();
//		if(dataTypeSet.contains(DCRFConfig.dataType)){
//			trainInstances = DCRFReader.readCNN(DPConfig.ecrftrain, true, trainNumber, entityMap);
//			testInstances = DCRFReader.readCNN(testFile, false, testNumber, entityMap);
//		}else{
			trainInstances = DCRFReader.readCONLLData(DCRFConfig.CONLL_train,true,trainNumber);
			testInstances = DCRFReader.readCONLLData(testFile,false,testNumber);
//		}
		System.err.println("entity size:"+DEntity.ENTS_INDEX.toString());
		System.err.println("tag size:"+Tag.TAGS.size());
		System.err.println("tag size:"+Tag.TAGS_INDEX.toString());
//		Formatter.ner2Text(trainInstances, "data/testRandom2.txt");
//		System.exit(0);
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig.CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = 0.01;
		NetworkConfig.NUM_THREADS = numThreads;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = false;
		
		DCRFFeatureManager fa = new DCRFFeatureManager(new GlobalNetworkParam());
		DCRFNetworkCompiler compiler = new DCRFNetworkCompiler(viewer);
		NetworkModel model = DiscriminativeNetworkModel.create(fa, compiler);
		DCRFInstance[] ecrfs = trainInstances.toArray(new DCRFInstance[trainInstances.size()]);
		model.train(ecrfs, numIteration);
		Instance[] predictions = model.decode(testInstances.toArray(new DCRFInstance[testInstances.size()]));
		DCRFEval.evalNER(predictions, nerOut);
		DCRFEval.evalPOS(predictions, posOut);
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
					case "-ent": selectedEntities = args[i+1].split(","); break;
					case "-testFile": testFile = args[i+1]; break;
					case "-reg": DCRFConfig.l2val = Double.valueOf(args[i+1]); break;
					case "-dev":isDev = args[i+1].equals("true")?true:false; break;
					case "-windows":DCRFConfig.windows = true; break;
//					case "-comb": DPConfig.comb = true; break;
//					case "-data":DPConfig.dataType=args[i+1];DPConfig.changeDataType(); break;
					default: System.err.println("Invalid arguments, please check usage."); System.exit(0);
				}
			}
//			if(DPConfig.comb){
//				DPConfig.changeTrainingPath();
//			}
			System.err.println("[Info] trainNum: "+trainNumber);
			System.err.println("[Info] testNum: "+testNumber);
			System.err.println("[Info] numIter: "+numIteration);
			System.err.println("[Info] numThreads: "+numThreads);
			System.err.println("[Info] Using development set??: "+isDev);
			System.err.println("[Info] Selected Entities: "+Arrays.toString(selectedEntities));
//			System.err.println("[Info] Data type: "+DPConfig.dataType);
			System.err.println("[Info] Regularization Parameter: "+DCRFConfig.l2val);
		}
	}
}

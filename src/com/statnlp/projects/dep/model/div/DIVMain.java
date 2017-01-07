package com.statnlp.projects.dep.model.div;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.projects.dep.Evaluator;
import com.statnlp.projects.dep.Transformer;
import com.statnlp.projects.dep.model.ModelViewer;
import com.statnlp.projects.dep.utils.DPConfig;
import com.statnlp.projects.dep.utils.DPConfig.MODEL;
import com.statnlp.projects.dep.utils.Init;

/**
 * Additional Dependency parsing
 * @author zhanming_jie
 *
 */
public class DIVMain {
	
	public static String PARENT_IS = DPConfig.PARENT_IS;
	public static String OE = DPConfig.OE;
	public static String ONE = DPConfig.ONE;
	public static String[] entities; 
	public static int trainNumber = -1;
	public static int testNumber = -1;
	public static int numIteration = 100;
	public static int numThreads = 2;
	public static String trainingPath;
	public static String testingPath;
	public static String devPath;
	public static boolean isDev = true;
	public static String[] selectedEntities = {"person","organization","gpe","MISC"};
	public static HashSet<String> dataTypeSet;
	public static HashMap<String, Integer> typeMap;
	
	public static String[] initializeUniqueModelTypeMap(String[] selectedEntities){
		typeMap = new HashMap<String, Integer>();
		int index = 0;
		typeMap.put(OE, index++); //0
		typeMap.put(ONE, index++); //1
		for(int i=0;i<selectedEntities.length;i++){
			typeMap.put(selectedEntities[i],index++); //2,3,4,5,
		}
		typeMap.put(PARENT_IS+OE, index++);
		typeMap.put(PARENT_IS+ONE, index++);
		for(int i=0;i<selectedEntities.length;i++){
			typeMap.put(PARENT_IS+selectedEntities[i],index++);
		}
		String[] entities = new String[typeMap.size()/2];
		Iterator<String> iter = typeMap.keySet().iterator();
		while(iter.hasNext()){
			String entity = iter.next();
			if(!entity.startsWith(PARENT_IS))
				entities[typeMap.get(entity)] = entity;
		}
		return entities;
	}
	
	
	public static void main(String[] args) throws InterruptedException, IOException {
		
		
		processArgs(args);
		dataTypeSet = Init.iniOntoNotesData();
		entities = initializeUniqueModelTypeMap(selectedEntities);
		String modelType = MODEL.DIVIDED.name();
		DPConfig.currentModel = modelType;
		String middle = isDev? ".dev":".test";
		String dpRes = DPConfig.data_prefix+modelType+middle+DPConfig.dp_res_suffix; 
		String nerEval = DPConfig.data_prefix+modelType+middle+DPConfig.ner_eval_suffix;
		String jointRes = DPConfig.data_prefix+modelType+middle+DPConfig.joint_res_suffix;
		trainingPath = DPConfig.trainingPath;
		testingPath = DPConfig.testingPath;
		devPath = DPConfig.devPath;
		
		System.err.println("[Info] Current Model:"+modelType);
		/******Debug********/
//		trainingPath = "data/semeval10t1/small.txt";
//		testingPath = "data/semeval10t1/test.txt";
//		trainNumber = 1;
//		testNumber = 1;
//		numIteration = 40;
//		numThreads = 1;
//		testingPath = trainingPath;
//		DPConfig.readWeight = false;
//		DPConfig.writeWeight = false;
		/************/
		
		
		Transformer tran = new DIVTransformer();
		String decodePath = isDev?devPath:testingPath;
		System.err.println("[Info] train path: "+trainingPath);
		System.err.println("[Info] testFile: "+decodePath);
		System.err.println("[Info] dpRes: "+dpRes);
		System.err.println("[Info] ner eval: "+nerEval);
		System.err.println("[Info] joint Res: "+jointRes);
		DIVInstance[] trainingInsts = null;
		DIVInstance[] testingInsts = null;
		if(dataTypeSet.contains(DPConfig.dataType)){
			trainingInsts = DIVReader.readCNN(trainingPath, true,trainNumber,tran);
			testingInsts = DIVReader.readCNN(decodePath, false,testNumber,tran);
		}else{
			trainingInsts = DIVReader.readInstance(trainingPath, true,trainNumber,selectedEntities,tran, false);
			testingInsts = DIVReader.readInstance(decodePath, false,testNumber,selectedEntities,tran, false);
		}
//		Formatter.semevalToNER(trainingInsts, "data/"+DPConfig.dataType+"/ecrf.train.conllx");
//		Formatter.semevalToNER(testingInsts, "data/"+DPConfig.dataType+"/ecrf.test.conllx");
//		Formatter.semevalToText(devInsts, "data/"+DPConfig.dataType+"/proj/en.devel.txt");
//		Formatter.semevalToText(tInsts, "data/"+DPConfig.dataType+"/proj/en.test.txt");
//		System.exit(0);
//		System.err.println(testingInsts[0].getInput().toString());
//		DataChecker.checkJoint(trainingInsts, entities);
//		DataChecker.checkJoint(testingInsts, entities);
//		System.exit(0);
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig.CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig.NUM_THREADS = numThreads;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = DPConfig.L2; //DPConfig.L2;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = false;
		NetworkConfig.MAX_MARGINAL_DECODING = false;
		
		ModelViewer viewer = new ModelViewer(4,entities);
		FeatureManager dfm = new DIVFeatureManager_leafcopy(new GlobalNetworkParam(),entities);
		DIVNetworkCompiler dnc = new DIVNetworkCompiler(typeMap, viewer);
		NetworkModel model = DiscriminativeNetworkModel.create(dfm, dnc);
		model.train(trainingInsts, numIteration); 
		//DIVFeatureManager.pw.close();
		/****************Evaluation Part**************/
		System.err.println("*****Evaluation*****");
		Instance[] predInsts = model.decode(testingInsts);
		Evaluator.evalDP(predInsts, dpRes);
		Evaluator.evalNER(predInsts, nerEval);
		Evaluator.writeJointResult(predInsts, jointRes, modelType);
	}
	
	public static void processArgs(String[] args){
		String usage = "\t usage: java -jar divided.jar -trainNum -1 -testNum -1 -thread 5 -iter 100 -train path -test path";
		if(args[0].equals("-h") || args[0].equals("help") || args[0].equals("-help") ){
			System.err.println("UModel Version: Joint DEPENDENCY PARSING and Entity Recognition TASK: ");
			System.err.println(usage);
			System.err.println("\t put numTrainInsts/numTestInsts = -1 if you want to use all the training/testing instances");
			System.err.println("\t By default: trainNum=-1, testNum=-1, thread=2, iter=100");
			System.exit(0);
		}else{
			for(int i=0;i<args.length;i=i+2){
				switch(args[i]){
					case "-trainNum": trainNumber = Integer.valueOf(args[i+1]); break;
					case "-testNum": testNumber = Integer.valueOf(args[i+1]); break;
					case "-iter": numIteration = Integer.valueOf(args[i+1]); break;
					case "-thread": numThreads = Integer.valueOf(args[i+1]); break;
					case "-train":trainingPath = args[i+1];break;
					case "-test":testingPath = args[i+1];break;
					case "-ent": selectedEntities = args[i+1].split(","); break;
					case "-debug": DPConfig.DEBUG = args[i+1].equals("true")? true:false; break;
					case "-reg": DPConfig.L2 = Double.parseDouble(args[i+1]); break;
					case "-dev": isDev = args[i+1].equals("true")? true:false; break;
					case "-windows": DPConfig.windows = true; break;
					case "-comb": DPConfig.comb = true; break;
					case "-data":DPConfig.dataType=args[i+1];DPConfig.changeDataType(); break;
					case "-rw": DPConfig.weightPath=args[i+1]; DPConfig.readWeight = true;DPConfig.writeWeight = false; break;
					case "-ww":DPConfig.weightPath=args[i+1]; DPConfig.readWeight = false; DPConfig.writeWeight = true; break;
					default: System.err.println("Invalid arguments: "+args[i]+", please check usage."); System.err.println(usage);System.exit(0);
				}
			}
			if(DPConfig.comb){
				DPConfig.changeTrainingPath();
			}
			
			System.err.println("[Info] trainNum: "+trainNumber);
			System.err.println("[Info] testNum: "+testNumber);
			System.err.println("[Info] numIter: "+numIteration);
			System.err.println("[Info] numThreads: "+numThreads);
			
			System.err.println("[Info] Selected Entities: "+Arrays.toString(selectedEntities));
			System.err.println("[Info] Regularization Parameter: "+DPConfig.L2);
			System.err.println("[Info] Using development set??: "+isDev);
			
		}
	}

}
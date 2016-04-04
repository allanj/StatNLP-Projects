package com.statnlp.dp.model.divcopy;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import com.statnlp.commons.types.Instance;
import com.statnlp.dp.DependInstance;
import com.statnlp.dp.DependencyReader;
import com.statnlp.dp.Evaluator;
import com.statnlp.dp.Transformer;
import com.statnlp.dp.model.ModelViewer;
import com.statnlp.dp.utils.DPConfig;
import com.statnlp.dp.utils.DPConfig.MODEL;
import com.statnlp.dp.utils.Init;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkModel;

/**
 * Additional Dependency parsing
 * @author zhanming_jie
 *
 */
public class DCMain {
	
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
	
	public static void main(String[] args) throws InterruptedException, IOException {
		
		
		processArgs(args);
		dataTypeSet = Init.iniOntoNotesData();
		entities = Init.initializeUniqueModelTypeMap(selectedEntities);
		String modelType = MODEL.DIVCOPY.name();
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
		testingPath = "data/semeval10t1/test.txt";
		trainNumber = 500;
//		testNumber = -1;
//		numIteration = 1;
//		numThreads = 8;
//		testingPath = trainingPath;
		DPConfig.readWeight = true;
		DPConfig.writeWeight = false;
		/************/
		
		
		Transformer tran = new DCTransformer();
		String decodePath = isDev?devPath:testingPath;
		System.err.println("[Info] train path: "+trainingPath);
		System.err.println("[Info] testFile: "+decodePath);
		System.err.println("[Info] dpRes: "+dpRes);
		System.err.println("[Info] ner eval: "+nerEval);
		System.err.println("[Info] joint Res: "+jointRes);
		DependInstance[] trainingInsts = null;
		DependInstance[] testingInsts = null;
		if(dataTypeSet.contains(DPConfig.dataType)){
			trainingInsts = DependencyReader.readCNN(trainingPath, true,trainNumber,tran);
			testingInsts = DependencyReader.readCNN(decodePath, false,testNumber,tran);
		}else{
			trainingInsts = DependencyReader.readInstance(trainingPath, true,trainNumber,selectedEntities,tran, false);
			testingInsts = DependencyReader.readInstance(decodePath, false,testNumber,selectedEntities,tran, false);
		}
//		Formatter.semevalToText(trinInsts, "data/"+DPConfig.dataType+"/proj/en.train.txt");
//		Formatter.semevalToText(devInsts, "data/"+DPConfig.dataType+"/proj/en.devel.txt");
//		Formatter.semevalToText(tInsts, "data/"+DPConfig.dataType+"/proj/en.test.txt");
//		System.exit(0);
//		System.err.println(testingInsts[0].getInput().toString());
//		DataChecker.checkJoint(trainingInsts, entities);
//		DataChecker.checkJoint(testingInsts, entities);
//		System.exit(0);
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig._CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig._numThreads = numThreads;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = DPConfig.L2; //DPConfig.L2;
		NetworkConfig._SEQUENTIAL_FEATURE_EXTRACTION = false;
		
		
		ModelViewer viewer = new ModelViewer(4,entities);
		FeatureManager dfm = null;
		dfm = new DCFeatureManager(new GlobalNetworkParam(),entities);
		DCNetworkCompiler dnc = new DCNetworkCompiler(NetworkConfig.typeMap, viewer);
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

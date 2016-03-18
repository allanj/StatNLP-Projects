package com.statnlp.dp.model.rp;

import java.io.IOException;
import java.util.Arrays;

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
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkModel;

/**
 * Additional Dependency parsing
 * @author zhanming_jie
 *
 */
public class RPMain {
	
	public static String[] entities; 
	public static int trainNumber = -1;
	public static int testNumber = -1;
	public static int numIteration = 100;
	public static int numThreads = 2;
	public static String trainingPath = DPConfig.trainingPath;
	public static String testingPath = DPConfig.testingPath;
	public static String[] selectedEntities = {"person","organization","gpe"};

	
	
	public static void main(String[] args) throws InterruptedException, IOException {
		
		
		processArgs(args);
		entities = Init.initializeUniqueModelTypeMap(selectedEntities);
		String modelType = MODEL.RPM.name();
		String dpRes = DPConfig.data_prefix+modelType+".dp.res.txt"; 
		String nerEval = DPConfig.data_prefix+modelType+".ner.eval.txt"; 
		String jointRes = DPConfig.data_prefix+modelType+".joint.res.txt";
		
		System.err.println("[Info] Current Model:"+modelType);
		/******Debug********/
//		trainingPath = "data/semeval10t1/small.txt";
//		testingPath = "data/semeval10t1/small.txt";
//		trainNumber = -1;
//		testNumber = 100;
//		numIteration = 300;
//		numThreads = 4;
//		testingPath = trainingPath;
		/************/
		
		
		Transformer tran = new RPTransformer();
		DependInstance[] trainingInsts = DependencyReader.readInstance(trainingPath, true,trainNumber,selectedEntities,tran, false);
		
		DependInstance[] testingInsts = DependencyReader.readInstance(testingPath, false,testNumber,selectedEntities,tran, false);
//		System.err.println(testingInsts[9].getOutput().pennString());
//		DataChecker.checkJoint(trainingInsts, entities);
//		DataChecker.checkJoint(testingInsts, entities);
//		System.exit(0);
		
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig._CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig._numThreads = numThreads;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = DPConfig.L2;
		NetworkConfig._SEQUENTIAL_FEATURE_EXTRACTION = false;
		
		ModelViewer viewer = new ModelViewer(4,entities);
		RPFeatureManager dfm = new RPFeatureManager(new GlobalNetworkParam(),entities);
		RPNetworkCompiler dnc = new RPNetworkCompiler(NetworkConfig.typeMap, viewer);
		NetworkModel model = DiscriminativeNetworkModel.create(dfm, dnc);
		model.train(trainingInsts, numIteration); 
		
		/****************Evaluation Part**************/
		System.err.println("*****Evaluation*****");
		Instance[] predInsts = model.decode(testingInsts);
//		for(int i=0;i<predInsts.length;i++){
//			DependInstance inst = (DependInstance)predInsts[i];
//			System.err.println("instance Id:"+predInsts[i].getInstanceId());
//			System.err.println(inst.getPrediction().pennString());
//			System.err.println("********Splitting Line*********");
//		}
		Evaluator.evalDP(predInsts, dpRes);
		Evaluator.evalNER(predInsts, nerEval);
		Evaluator.writeJointResult(predInsts, jointRes, modelType);
	}
	
	public static void processArgs(String[] args){
		String usage = "\t usage: java -jar rpmodel.jar -trainNum -1 -testNum -1 -thread 5 -iter 100 -train path -test path";
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
					case "-reg": DPConfig.L2 = Double.valueOf(args[i+1]); break;
					default: System.err.println("Invalid arguments, please check usage."); System.err.println(usage);System.exit(0);
				}
			}
			System.err.println("[Info] trainNum: "+trainNumber);
			System.err.println("[Info] testNum: "+testNumber);
			System.err.println("[Info] numIter: "+numIteration);
			System.err.println("[Info] numThreads: "+numThreads);
			System.err.println("[Info] train path: "+trainingPath);
			System.err.println("[Info] test path: "+testingPath);
			System.err.println("[Info] Selected Entities: "+Arrays.toString(selectedEntities));
			System.err.println("[Info] Regularization Parameter: "+DPConfig.L2);
			
		}
	}

}

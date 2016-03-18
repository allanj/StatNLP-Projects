package com.statnlp.dp.model;

import java.io.IOException;

import com.statnlp.commons.types.Instance;
import com.statnlp.dp.DependInstance;
import com.statnlp.dp.DependencyReader;
import com.statnlp.dp.Evaluator;
import com.statnlp.dp.Transformer;
import com.statnlp.dp.utils.DPConfig;
import com.statnlp.dp.utils.DataChecker;
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
public class ADPMain {
	
	public static String[] entities; 
	public static int trainNumber = -100;
	public static int testNumber = -100;
	public static int numIteration = 100;
	public static int numThreads = -100;
	public static boolean nested = true;
	

	
	
	public static void main(String[] args) throws InterruptedException, IOException {
		
		
		processArgs(args);
		String trainingPath = DPConfig.trainingPath;
		String testingPath = DPConfig.testingPath;
		entities = Init.initializeTypeMap();
		
		String dpRes = DPConfig.data_prefix+"model."+nested+".dp.res.txt"; 
		String nerEval = DPConfig.data_prefix+"model."+nested+".ner.eval.txt"; 
		String jointRes = DPConfig.data_prefix+"model."+nested+".joint.res.txt";
		String modelType = DPConfig.MODEL.ADM.name();
		/******Debug********/
		trainingPath = "data/semeval10t1/small.txt";
//		testingPath = "data/semeval10t1/en.test.txt";
		trainNumber = -1;
		testNumber = -1;
//		numIteration = 10;
//		numThreads = 10;
//		testingPath = trainingPath;
		/************/
		
		
		nested = true;
		Transformer tran = nested? new MDPTransformer(): new ADPTransformer();
		DependInstance[] trainingInsts = DependencyReader.readInstance(trainingPath, true,trainNumber,entities,tran, true);
		
		DependInstance[] testingInsts = DependencyReader.readInstance(testingPath, false,testNumber,entities,tran, true);
		
		DataChecker.checkJoint(trainingInsts, entities);
		DataChecker.checkJoint(testingInsts, entities);
		System.exit(0);
		
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig._CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig._numThreads = numThreads;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = DPConfig.L2;
		NetworkConfig._SEQUENTIAL_FEATURE_EXTRACTION = false;
		ADPFeatureManager dfm = new ADPFeatureManager(new GlobalNetworkParam(),entities,nested);
		ADPNetworkCompiler dnc = new ADPNetworkCompiler(NetworkConfig.typeMap);
		NetworkModel model = DiscriminativeNetworkModel.create(dfm, dnc);
		model.train(trainingInsts, numIteration); 
		
		/****************Evaluation Part**************/
		System.err.println("*****Evaluation*****");
		Instance[] predInsts = model.decode(testingInsts);
		Evaluator.evalDP(predInsts, dpRes);
		Evaluator.evalNER(predInsts, nerEval);
		Evaluator.writeJointResult(predInsts, jointRes, modelType);
	}
	
	public static void processArgs(String[] args){
		if(args.length<8){
			System.err.println("Argument number are not following the rules. See the following example:");
			System.err.println("\t usage: java -jar adp.jar -trainNum -1 -testNum -1 -thread 5 -iter 100 -nested false");
			System.exit(0);
		}
		else if(args[0].equals("-h") || args[0].equals("help") || args[0].equals("-help") ){
			System.err.println("Brute Version: Joint DEPENDENCY PARSING and Entity Recognition TASK: ");
			System.err.println("\t usage: java -jar adp.jar -trainNum -1 -testNum -1 -thread 5 -iter 100 -nested true");
			System.err.println("\t put numTrainInsts/numTestInsts = -1 if you want to use all the training/testing instances");
			System.exit(0);
		}else{
			for(int i=0;i<args.length;i=i+2){
				switch(args[i]){
					case "-trainNum": trainNumber = Integer.valueOf(args[i+1]); break;
					case "-testNum": testNumber = Integer.valueOf(args[i+1]); break;
					case "-iter": numIteration = Integer.valueOf(args[i+1]); break;
					case "-thread": numThreads = Integer.valueOf(args[i+1]); break;
					case "-nested": nested = args[i+1].equals("true")?true:false; break;
				}
			}
			System.err.println("[Info] trainNum: "+trainNumber);
			System.err.println("[Info] testNum: "+testNumber);
			System.err.println("[Info] numIter: "+numIteration);
			System.err.println("[Info] numThreads: "+numThreads);
			System.err.println("[Info] Nested: "+nested);
			if(nested) System.err.println("[Info] ************Nested Version*********");
			else System.err.println("[Info] ************Not Nested Version*********");
			
			if(trainNumber==-100 || testNumber==-100 || numIteration==-100 || numThreads==-100 ){
				System.err.println("Some of the parameters are not set or not properly setting, please double check the following arguments");
				System.err.println("\t usage: java -jar bdp.jar -trainNum -1 -testNum -1 -thread 5 -iter 100");
				System.exit(0);
			}
		}
	}

}

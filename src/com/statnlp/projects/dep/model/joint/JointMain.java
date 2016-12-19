package com.statnlp.projects.dep.model.joint;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.projects.dep.utils.DPConfig;
import com.statnlp.projects.dep.utils.DPConfig.MODEL;

/**
 * Joint Dependency Parsing and NER.
 * @author allan_jie
 * @version 2.0
 */
public class JointMain {
	
	public static int trainNumber = -1;
	public static int testNumber = -1;
	public static int numIteration = 100;
	public static int numThreads = 2;
	public static String trainingPath;
	public static String testingPath;
	public static String devPath;
	public static boolean isDev = false;
	public static HashSet<String> dataTypeSet;
	
	public static void main(String[] args) throws InterruptedException, IOException, ClassNotFoundException {
		
		
		processArgs(args);
		String modelType = MODEL.HYPEREDGE.name();
		DPConfig.currentModel = modelType;
		String middle = isDev? ".dev":".test";
		String dpRes = DPConfig.data_prefix + modelType+middle+DPConfig.dp_res_suffix; 
		String nerEval = DPConfig.data_prefix + modelType+middle+DPConfig.ner_eval_suffix;
		String jointRes = DPConfig.data_prefix + modelType+middle+DPConfig.joint_res_suffix;
		trainingPath = DPConfig.trainingPath;
		testingPath = DPConfig.testingPath;
		devPath = DPConfig.devPath;
		
		System.err.println("[Info] Current Model:"+modelType);
		/******Debug********/
		trainingPath = "data/allanprocess/abc/train.conllx";
		testingPath = "data/allanprocess/abc/test.conllx";
//		trainNumber = 100;
//		testNumber = 100;
//		numIteration = 20;
//		numThreads = 8;
//		testingPath = trainingPath;
//		DPConfig.readWeight = true;
//		DPConfig.writeWeight = false;
		/************/
		//initialize the pruned map.
//		SemiPrune pruner = new SemiPrune(trainingPath, testingPath, -1, -1, numThreads);
//		Map<Integer, Map<Integer, Map<Integer, Set<Integer>>>> prunedMap = pruner.prune(0.1);
		ObjectInputStream in = new ObjectInputStream(new FileInputStream("data/allanprocess/abc/pruned"));
		@SuppressWarnings("unchecked")
		Map<Integer, Map<Integer, Map<Integer, Set<Integer>>>> prunedMap = (Map<Integer, Map<Integer, Map<Integer, Set<Integer>>>>)in.readObject();
		in.close();
		System.err.println("[Info] Pruned Map size: " + prunedMap.size());
		
		String decodePath = isDev?devPath:testingPath;
		System.err.println("[Info] train path: "+trainingPath);
		System.err.println("[Info] testFile: "+decodePath);
		System.err.println("[Info] dpRes: "+dpRes);
		System.err.println("[Info] ner eval: "+nerEval);
		System.err.println("[Info] joint Res: "+jointRes);
		JointInstance[] trainingInsts = JointReader.readCoNLLXData(trainingPath, true, trainNumber, true);;
		JointInstance[] testingInsts = JointReader.readCoNLLXData(decodePath, false, testNumber, false);
		Label.get(DPConfig.EMPTY);
		System.err.println("The label set: " + Label.Label_Index.toString());
		
		//debug
		Label.lock();
		
		//debug:
//		System.err.println("checking training");
//		Analyzer.checkMultiwordsHead(trainingInsts);
//		System.err.println("checking testing");
//		Analyzer.checkMultiwordsHead(testingInsts);
//		System.exit(0);
		
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig.CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig.NUM_THREADS = numThreads;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = DPConfig.L2; //DPConfig.L2;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = true;
		NetworkConfig.AVOID_DUPLICATE_FEATURES = true;
		
		JointFeatureManager hpfm = new JointFeatureManager(new GlobalNetworkParam());
		JointNetworkCompiler dnc = new JointNetworkCompiler(prunedMap);
		NetworkModel model = DiscriminativeNetworkModel.create(hpfm, dnc);
		model.train(trainingInsts, numIteration); 
		
		/****************Evaluation Part**************/
		System.err.println("*****Evaluation*****");
		Instance[] predInsts = model.decode(testingInsts);
//		HPEEval.evalDP(predInsts, dpRes);
		JointEval.evalNER(predInsts, nerEval);
//		HPEEval.writeJointResult(predInsts, jointRes, modelType);
	}
	
	public static void processArgs(String[] args){
		String usage = "\t usage: java -jar hyperedge.jar -trainNum -1 -testNum -1 -thread 5 -iter 100 -train path -test path";
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
					case "-debug": DPConfig.DEBUG = args[i+1].equals("true")? true:false; break;
					case "-reg": DPConfig.L2 = Double.parseDouble(args[i+1]); break;
					case "-dev": isDev = args[i+1].equals("true")? true:false; break;
					case "-windows": DPConfig.windows = true; break;
					case "-data":DPConfig.dataType=args[i+1];DPConfig.changeDataType(); break;
					case "-rw": DPConfig.weightPath=args[i+1]; DPConfig.readWeight = true;DPConfig.writeWeight = false; break;
					case "-ww":DPConfig.weightPath=args[i+1]; DPConfig.readWeight = false; DPConfig.writeWeight = true; break;
					default: System.err.println("Invalid arguments: "+args[i]+", please check usage."); System.err.println(usage);System.exit(0);
				}
			}
			System.err.println("[Info] trainNum: "+trainNumber);
			System.err.println("[Info] testNum: "+testNumber);
			System.err.println("[Info] numIter: "+numIteration);
			System.err.println("[Info] numThreads: "+numThreads);
			
			System.err.println("[Info] Regularization Parameter: "+DPConfig.L2);
			System.err.println("[Info] Using development set??: "+isDev);
			
		}
	}

}

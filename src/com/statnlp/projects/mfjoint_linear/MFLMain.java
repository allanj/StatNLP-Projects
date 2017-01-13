package com.statnlp.projects.mfjoint_linear;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkConfig.InferenceType;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.projects.mfjoint_linear.MFLConfig.MFLTASK;

public class MFLMain {

	
	public static String trainFile;
	public static String testFile;
	public static int trainNum = 50;
	public static int testNum = 20;
	public static int numThreads = 8;
	public static int numIterations = 5000;
	public static double l2 = 0.1;
	public static String modelFile = null;
	public static boolean saveModel = false;
	public static boolean readModel = false;
	public static String dataset = "allanprocess";
	public static String dataSection = "abc";
	public static MFLTASK task = MFLTASK.JOINT;
	public static boolean useJointFeatures = false;
	public static int maxSize = 150;
	public static String nerOut;
	public static String jointOutput;
	public static boolean iobes = true;
	public static boolean evaluation = false;
	public static boolean isPipe = false;
	
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
		
		processArgs(args);
		trainFile = "data/"+dataset+"/"+dataSection+"/train.conllx";
		testFile = "data/"+dataset+"/"+dataSection+"/test.conllx";
		modelFile = "data/"+dataset+"/"+dataSection+"/output/"+dataSection+".mfjoint.linear.mf"+NetworkConfig.MAX_MF_UPDATES+".reg"+l2+".model";
		nerOut = "data/"+dataset+"/"+dataSection+"/output/"+dataSection+".mfjoint.linear.mf"+NetworkConfig.MAX_MF_UPDATES+".ner.eval";
		jointOutput = "data/"+dataset+"/"+dataSection+"/output/"+dataSection+".mfjoint.linear.mf"+NetworkConfig.MAX_MF_UPDATES+".res";
		
		System.err.println("[Info] trainFile: " + trainFile);
		System.err.println("[Info] testFile: " + testFile);
		System.err.println("[Info] model file: " + modelFile);
		System.err.println("[Info] nerOut: " + nerOut);
		System.err.println("[Info] task: " + task.toString());
		System.err.println("[Info] #max-mf: " + NetworkConfig.MAX_MF_UPDATES);
		NetworkConfig.INFERENCE = task == MFLTASK.JOINT ? InferenceType.MEAN_FIELD : InferenceType.FORWARD_BACKWARD;
		System.err.println("[Info] inference type: " + NetworkConfig.INFERENCE.name());
		System.err.println("[Info] use joint features?: " + useJointFeatures);
		
		boolean checkProjective = task == MFLTASK.PARING || task == MFLTASK.JOINT ? true : false;
		MFLInstance[] trainInsts = MFLReader.readCoNLLXData(trainFile, true, trainNum, checkProjective, iobes);
		MFLInstance[] testInsts = MFLReader.readCoNLLXData(testFile, false, testNum, false, false);
		
		
		NetworkConfig.NUM_THREADS = numThreads;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = l2;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = true;
		NetworkConfig.AVOID_DUPLICATE_FEATURES = true;
		
		
		/***DEBUG configuration***/
//		MFLConfig.windows = true;
//		useJointFeatures = true;
//		NetworkConfig.MAX_MF_UPDATES = 30;
		/******/
		

		if (evaluation) {
			MFLReader.readResultFile(jointOutput, testInsts);
			MFLEval.evalDep(testInsts);
			MFLEval.evalNER(testInsts, nerOut);
			MFLEval.evalCombined(testInsts);
			return;
		}
		
		NetworkModel model = null;
		if (!readModel) {
			MFLNetworkCompiler compiler = new MFLNetworkCompiler(task, maxSize, iobes);
			MFLFeatureManager fm = new MFLFeatureManager(new GlobalNetworkParam(), useJointFeatures);
			model = DiscriminativeNetworkModel.create(fm, compiler);
			model.train(trainInsts, numIterations);
		} else {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(modelFile));
			model = (NetworkModel)in.readObject();
			in.close();
		}
		
		if (saveModel) {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(modelFile));
			out.writeObject(model);
			out.close();
		}
		
		Instance[] predictions = model.decode(testInsts);
		if (task == MFLTASK.PARING || task == MFLTASK.JOINT)
			MFLEval.evalDep(predictions);
		if ((task == MFLTASK.NER || task == MFLTASK.JOINT))
			MFLEval.evalNER(predictions, nerOut);
		if (task == MFLTASK.JOINT) {
			MFLEval.evalCombined(testInsts);
			MFLEval.writeJointResult(predictions, jointOutput);
		}
	}

	public static void processArgs(String[] args){
		if(args.length > 1 && (args[0].equals("-h") || args[0].equals("help") || args[0].equals("-help") )){
			System.err.println("Factorial CRFs for ");
			System.err.println("\t usage: java -jar fcrf.jar -trainNum -1 -testNum -1 -thread 5 -iter 100 -pipe true");
			System.err.println("\t put numTrainInsts/numTestInsts = -1 if you want to use all the training/testing instances");
			System.exit(0);
		}else{
			for(int i=0;i<args.length;i=i+2){
				switch(args[i]){
					case "-trainNum": trainNum = Integer.valueOf(args[i+1]); break;
					case "-testNum": testNum = Integer.valueOf(args[i+1]); break;
					case "-iter": numIterations = Integer.valueOf(args[i+1]); break;
					case "-thread": numThreads = Integer.valueOf(args[i+1]); break;
					case "-reg": l2 = Double.valueOf(args[i+1]); break;
					case "-windows": MFLConfig.windows = args[i+1].equals("true")? true:false; break;
					case "-mfround": NetworkConfig.MAX_MF_UPDATES = Integer.valueOf(args[i+1]);
									 useJointFeatures = true;
									 if(NetworkConfig.MAX_MF_UPDATES == 0) useJointFeatures = false;
									 break;
					case "-task": 
						if(args[i+1].equals("parsing"))  task = MFLTASK.PARING;
						else if (args[i+1].equals("ner")) task  = MFLTASK.NER;
						else if (args[i+1].equals("joint"))  task  = MFLTASK.JOINT;
						else if (args[i+1].equals("eval"))  evaluation = true;
						else throw new RuntimeException("Unknown task:"+args[i+1]+"?"); break;
					case "-saveModel": saveModel = args[i+1].equals("true")?true:false; break;
					case "-readModel": readModel = args[i+1].equals("true")?true:false; break;
					case "-iobes": iobes = args[i+1].equals("true")? true : false; break;
					case "-pipe": isPipe = args[i+1].equals("true")? true : false; break;
					case "-dataset": dataset = args[i+1]; break;
					case "-section": dataSection = args[i+1]; break;
					case "-trainFile": trainFile = args[i+1]; break;
					case "-testFile": testFile = args[i+1]; break;
					default: System.err.println("Invalid arguments :"+args[i]+", please check usage."); System.exit(0);
				}
			}
			System.err.println("[Info] trainNum: "+ trainNum);
			System.err.println("[Info] testNum: "+ testNum);
			System.err.println("[Info] numIter: "+ numIterations);
			System.err.println("[Info] numThreads: "+ numThreads);
			System.err.println("[Info] Regularization Parameter: "+ l2);	
		}
	}

}

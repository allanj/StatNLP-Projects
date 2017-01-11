package com.statnlp.projects.joint.mix;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkModel;

public class MixMain {

	public static String trainFile;
	public static String testFile;
	public static int trainNum = -1;
	public static int testNum = -1;
	public static int numThreads = 8;
	public static int numIterations = 5000;
	public static double l2 = 0.1;
	public static String modelFile = null;
	public static boolean saveModel = false;
	public static boolean readModel = false;
	public static String dataset = "ConnectedData";
	public static String dataSection = "pri";
	public static boolean useJointFeatures = false;
	public static int maxSize = 150;
	public static int maxSegmentLength = 8;
	public static String nerOut;
	
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
		
		processArgs(args);
		trainFile = "data/"+dataset+"/"+dataSection+"/train.conllx";
		testFile = "data/"+dataset+"/"+dataSection+"/test.conllx";
		modelFile = "data/"+dataset+"/"+dataSection+"/output/mfjoint.model";
		nerOut = "data/"+dataset+"/"+dataSection+"/output/ner.eval";
		
		System.err.println("[Info] trainFile: " + trainFile);
		System.err.println("[Info] testFile: " + testFile);
		System.err.println("[Info] model file: " + modelFile);
		System.err.println("[Info] nerOut: "+nerOut);
		System.err.println("[Info] use joint features?: " + useJointFeatures);
		
		/***DEBUG configuration***/
//		trainFile = "data/"+dataset+"/"+dataSection+"/train.conllx";
//		trainFile = "E:/Framework/data/ontonotes/nw/train.conllx";
//		testFile = "E:/Framework/data/ontonotes/nw/test.conllx";
//		testFile = trainFile;
		/****/
		
		MixInstance[] trainInsts = MixReader.readCoNLLXData(trainFile, true, trainNum, true);
		MixInstance[] testInsts = MixReader.readCoNLLXData(testFile, false, testNum, false);
		MixLabel.lock();
		System.err.println(MixLabel.Label_Index.toString());
		NetworkConfig.NUM_THREADS = numThreads;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = l2;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = true;
		NetworkConfig.AVOID_DUPLICATE_FEATURES = true;
		
		/***DEBUG configuration***/
//		System.exit(0);
//		MixConfig.windows = true;
//		useJointFeatures = true;
//		NetworkConfig.MAX_MF_UPDATES = 4;
		/******/
		
		NetworkModel model = null;
		if (!readModel) {
			MixNetworkCompiler compiler = new MixNetworkCompiler(maxSize, maxSegmentLength);
			MixFeatureManager fm = new MixFeatureManager(new GlobalNetworkParam());
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
		MixEval.evalDep(predictions);
		MixEval.evalNER(predictions, nerOut);
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
					case "-windows": MixConfig.windows = args[i+1].equals("true")? true:false; break;
					case "-saveModel": saveModel = args[i+1].equals("true")?true:false; break;
					case "-readModel": readModel = args[i+1].equals("true")?true:false; break;
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

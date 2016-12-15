package com.statnlp.projects.nndcrf.linear_pos;

import java.io.IOException;
import java.util.List;

import com.statnlp.commons.ml.opt.OptimizerFactory;
import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkConfig.ModelType;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.neural.NeuralConfigReader;

public class POSMain {

	public static int trainNumber = -100;
	public static int testNumber = -100;
	public static int numIteration = 100;
	public static int numThreads = 5;
	public static double adagrad_learningRate = 0.1;
	public static double l2 = 0.01;
	public static boolean cascade  = false; //by default it's not a cascaded CRF.
	public static boolean basicFeatures = true; //the simple word/caps features. default should be true;
	public static int windowSize = 5; //by default the neural feature window size is 5.
	
	//read the conll 2000 dataset.
	public static String trainPath = "data/conll2000/train.txt";
	public static String testFile = "data/conll2000/test.txt";
	public static String nerOut = "data/conll2000/output/pos_out.txt";
	public static String neural_config = "config/posneural.config";
	
	
	public static void main(String[] args) throws IOException, InterruptedException{
		
		processArgs(args);
		System.err.println("[Info] trainingFile: "+trainPath);
		System.err.println("[Info] testFile: "+testFile);
		System.err.println("[Info] nerOut: "+nerOut);
		
		List<POSInstance> trainInstances = null;
		List<POSInstance> testInstances = null;
		
		
		/**Debug information**/
//		trainPath = "data/conll2000/train.txt";
//		trainNumber = 100;
//		testFile = "data/conll2000/test.txt";;
//		testNumber = 100;
//		numIteration = 500;
		/***/
		
		
		trainInstances = POSReader.readCONLL2000Data(trainPath, true,  trainNumber);
		testInstances = POSReader.readCONLL2000Data(testFile, false, testNumber);
		NetworkConfig.CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = l2;
		NetworkConfig.NUM_THREADS = numThreads;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = true;
		
		GlobalNetworkParam gnp = new GlobalNetworkParam(OptimizerFactory.getLBFGSFactory());
		
		if(NetworkConfig.USE_NEURAL_FEATURES){
			NeuralConfigReader.readConfig(neural_config);
			//gnp =  new GlobalNetworkParam(OptimizerFactory.);
		}
		
		System.err.println("[Info] "+POS.POSLabels.size()+" POS tags: "+POS.POSLabels.toString());
		
		POSInstance all_instances[] = new POSInstance[trainInstances.size()+testInstances.size()];
        int i = 0;
        for(; i<trainInstances.size(); i++) {
            all_instances[i] = trainInstances.get(i);
        }
        int lastId = all_instances[i-1].getInstanceId();
        int maxSize = 0;
        for(int j = 0; j<testInstances.size(); j++, i++) {
            all_instances[i] = testInstances.get(j);
            all_instances[i].setInstanceId(lastId+j+1);
            all_instances[i].setUnlabeled();
            maxSize = Math.max(maxSize, all_instances[i].size());
        }
		System.err.println("max sentence size:"+maxSize);
		POSFeatureManager fa = new POSFeatureManager(gnp, basicFeatures, cascade, windowSize);
		POSNetworkCompiler compiler = new POSNetworkCompiler();
		NetworkModel model = DiscriminativeNetworkModel.create(fa, compiler);
		POSInstance[] ecrfs = trainInstances.toArray(new POSInstance[trainInstances.size()]);
		if(NetworkConfig.USE_NEURAL_FEATURES){
			model.train(all_instances, trainInstances.size(), numIteration);
		}else{
			model.train(ecrfs, numIteration);
		}
		Instance[] predictions = model.decode(testInstances.toArray(new POSInstance[testInstances.size()]));
		POSEval.evalPOS(predictions, nerOut);
	}

	
	
	public static void processArgs(String[] args){
		if(args[0].equals("-h") || args[0].equals("help") || args[0].equals("-help") ){
			System.err.println("Linear-Chain CRF for POS tagging task: ");
			System.err.println("\t usage: java -jar pos.jar -trainNum -1 -testNum -1 -thread 5 -iter 100 -pipe true");
			System.err.println("\t put numTrainInsts/numTestInsts = -1 if you want to use all the training/testing instances");
			System.exit(0);
		}else{
			for(int i=0;i<args.length;i=i+2){
				switch(args[i]){
					case "-trainNum": trainNumber = Integer.valueOf(args[i+1]); break;   //default: all 
					case "-testNum": testNumber = Integer.valueOf(args[i+1]); break;    //default:all
					case "-iter": numIteration = Integer.valueOf(args[i+1]); break;   //default:100;
					case "-thread": numThreads = Integer.valueOf(args[i+1]); break;   //default:5
					case "-testFile": testFile = args[i+1]; break;        
					case "-batch": NetworkConfig.USE_BATCH_TRAINING = true;
									NetworkConfig.BATCH_SIZE = Integer.valueOf(args[i+1]); break;
					case "-model": NetworkConfig.MODEL_TYPE = args[i+1].equals("crf")? ModelType.CRF:ModelType.SSVM;   break;
					case "-neural": if(args[i+1].equals("true")){ 
											NetworkConfig.USE_NEURAL_FEATURES = true; 
											NetworkConfig.IS_INDEXED_NEURAL_FEATURES = false; //only used when using the senna embedding.
											NetworkConfig.REGULARIZE_NEURAL_FEATURES = true; // Regularized the neural features in CRF or not
										}
									break;
					case "-reg": l2 = Double.valueOf(args[i+1]);  break;
					case "-lr": adagrad_learningRate = Double.valueOf(args[i+1]); break;
					case "-basicf": 	basicFeatures = args[i+1].equals("true")? true:false; break;
					case "-cascade": 	cascade = args[i+1].equals("true")? true:false; break; //default: false. means that using the POS tagging generated from the first CRF.
					case "-wsize": 	 	windowSize = Integer.valueOf(args[i+1]); break; //default: 5. the window size of neural feature.
					default: System.err.println("Invalid arguments "+args[i]+", please check usage."); System.exit(0);
				}
			}
			System.err.println("[Info] trainNum: "+trainNumber);
			System.err.println("[Info] testNum: "+testNumber);
			System.err.println("[Info] numIter: "+numIteration);
			System.err.println("[Info] numThreads: "+numThreads);
			System.err.println("[Info] Regularization Parameter: "+l2);
		}
	}
}

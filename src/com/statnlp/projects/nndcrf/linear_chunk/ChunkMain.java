package com.statnlp.projects.nndcrf.linear_chunk;

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

public class ChunkMain {

	public static int trainNumber = -100;
	public static int testNumber = -100;
	public static int numIteration = 100;
	public static int numThreads = 5;
	public static String MODEL = "ssvm";
	public static double adagrad_learningRate = 0.1;
	public static double l2 = 0.01;
	public static boolean IOBESencoding = true;
	public static boolean npchunking = true;
	public static boolean cascade  = false;
	public static int windowSize = 5; //by default the neural feature window size is 5.
	
	//read the conll 2000 dataset.
	public static String trainPath = "data/conll2000/train.txt";
	public static String testFile = "data/conll2000/test.txt";
	public static String nerOut = "data/conll2000/output/chunk_out.txt";
	public static String neural_config = "config/chunkneural.config";
	
	
	public static void main(String[] args) throws IOException, InterruptedException{
		
		processArgs(args);
		System.out.println("[Info] trainingFile: "+trainPath);
		System.out.println("[Info] testFile: "+testFile);
		System.out.println("[Info] nerOut: "+nerOut);
		
		List<ChunkInstance> trainInstances = null;
		List<ChunkInstance> testInstances = null;
		
		
		/**Debug information**/
//		testFile = trainPath;
		/***/
		
		
		trainInstances = ChunkReader.readCONLL2000Data(trainPath, true,  trainNumber, npchunking, IOBESencoding);
		testInstances = ChunkReader.readCONLL2000Data(testFile, false, testNumber, npchunking);
		NetworkConfig.CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = l2;
		NetworkConfig.NUM_THREADS = numThreads;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = true;
		
		GlobalNetworkParam gnp = new GlobalNetworkParam(OptimizerFactory.getLBFGSFactory());
		
		if(NetworkConfig.USE_NEURAL_FEATURES){
			NeuralConfigReader.readConfig(neural_config);
			//gnp =  new GlobalNetworkParam(OptimizerFactory.);
		}
		
		System.out.println("[Info] "+Chunk.ChunkLabels.size()+" ChunkLabels: "+Chunk.ChunkLabels.toString());
		
		ChunkInstance all_instances[] = new ChunkInstance[trainInstances.size()+testInstances.size()];
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
		System.out.println("max sentence size:"+maxSize);
		ChunkFeatureManager fa = new ChunkFeatureManager(gnp, cascade, windowSize);
		ChunkNetworkCompiler compiler = new ChunkNetworkCompiler(IOBESencoding);
		NetworkModel model = DiscriminativeNetworkModel.create(fa, compiler);
		ChunkInstance[] ecrfs = trainInstances.toArray(new ChunkInstance[trainInstances.size()]);
		if(NetworkConfig.USE_NEURAL_FEATURES){
			model.train(all_instances, trainInstances.size(), numIteration);
		}else{
			model.train(ecrfs, numIteration);
		}
		Instance[] predictions = model.decode(testInstances.toArray(new ChunkInstance[testInstances.size()]));
		ChunkEval.evalNER(predictions, nerOut);
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
					case "-trainNum": 	trainNumber = Integer.valueOf(args[i+1]); break;   //default: all 
					case "-testNum": 	testNumber = Integer.valueOf(args[i+1]); break;    //default:all
					case "-iter": 		numIteration = Integer.valueOf(args[i+1]); break;   //default:100;
					case "-thread": 	numThreads = Integer.valueOf(args[i+1]); break;   //default:5
					case "-testFile": 	testFile = args[i+1]; break;        
					case "-windows":	CConfig.windows = args[i+1].equals("true")? true:false; break;  //default: false (is using windows system to run the evaluation script)
					case "-batch": 		NetworkConfig.USE_BATCH_TRAINING = true;
										NetworkConfig.BATCH_SIZE = Integer.valueOf(args[i+1]); break;
					case "-model": 		NetworkConfig.MODEL_TYPE = args[i+1].equals("crf")? ModelType.CRF:ModelType.SSVM;   break;
					case "-neural": 	if(args[i+1].equals("true")){ 
											NetworkConfig.USE_NEURAL_FEATURES = true; 
											NetworkConfig.OPTIMIZE_NEURAL = true;  //false: optimize in neural network
											NetworkConfig.IS_INDEXED_NEURAL_FEATURES = false; //only used when using the senna embedding.
											NetworkConfig.REGULARIZE_NEURAL_FEATURES = true; // Regularized the neural features in CRF or not
										}
										break;
					case "-reg": 		l2 = Double.valueOf(args[i+1]);  break;
					case "-lr": 		adagrad_learningRate = Double.valueOf(args[i+1]); break; //enable only if using adagrad optimization
					case "-npchunking": npchunking = args[i+1].equals("true")? true:false; break;
					case "-iobes": 		IOBESencoding = args[i+1].equals("true")? true:false; break;
					case "-cascade": 	cascade = args[i+1].equals("true")? true:false; break; //means that using the POS tagging generated from the first CRF.
					case "-wsize": 	 	windowSize = Integer.valueOf(args[i+1]); break; //the window size of neural feature.
					default: 			System.err.println("Invalid arguments "+args[i]+", please check usage."); System.exit(0);
				}
			}
			System.out.println("[Info] trainNum: "+trainNumber);
			System.out.println("[Info] testNum: "+testNumber);
			System.out.println("[Info] numIter: "+numIteration);
			System.out.println("[Info] numThreads: "+numThreads);
			System.out.println("[Info] Regularization Parameter: "+l2);
		}
	}
}

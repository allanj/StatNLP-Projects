package com.statnlp.entity.semi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.ml.opt.OptimizerFactory;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;
import com.statnlp.entity.EntityChecker;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GenerativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkConfig.ModelType;
import com.statnlp.neural.NeuralConfigReader;
import com.statnlp.hybridnetworks.NetworkModel;

public class SemiCRFMain {
	
	
	public static int trainNum = 1000;
	public static int testNumber = -1;
	public static int numThread = 8;
	public static int numIterations = 5000;
	public static double AdaGrad_Learning_Rate = 0.1;
	public static double l2 = 0.01;
	public static boolean nonMarkov = false;
	public static String neuralConfig = "config/debug.config";
	public static boolean useAdaGrad = false;
	public static boolean depFeature = false;
	public static boolean useIncompleteSpan = false;
	public static boolean useDepNet = false;
	public static String modelFile = null;
//	public static String train_filename = "data/cnn/ecrf.train.MISC.txt";
//	public static String test_filename = "data/cnn/ecrf.test.MISC.txt";
//	public static String train_filename = "data/semeval10t1/ecrf.smalltest.txt";
//	public static String test_filename = "data/semeval10t1/ecrf.smalltest.txt";
	public static String train_filename = "data/semeval10t1/ecrf.train.MISC.txt";
	public static String test_filename = "data/semeval10t1/ecrf.test.MISC.txt";
	
	private static void processArgs(String[] args) throws FileNotFoundException{
		for(int i=0;i<args.length;i=i+2){
			switch(args[i]){
				case "-trainNum": trainNum = Integer.valueOf(args[i+1]); break;   //default: all 
				case "-testNum": testNumber = Integer.valueOf(args[i+1]); break;    //default:all
				case "-iter": numIterations = Integer.valueOf(args[i+1]); break;   //default:100;
				case "-thread": numThread = Integer.valueOf(args[i+1]); break;   //default:5
				case "-windows": SemiEval.windows = true; break;            //default: false (is using windows system to run the evaluation script)
				case "-batch": NetworkConfig.USE_BATCH_TRAINING = true;
								NetworkConfig.BATCH_SIZE = Integer.valueOf(args[i+1]); break;
				case "-model": NetworkConfig.MODEL_TYPE = args[i+1].equals("crf")? ModelType.CRF:ModelType.SSVM;   break;
				case "-neural": if(args[i+1].equals("true")){ 
										NetworkConfig.USE_NEURAL_FEATURES = true; 
										NetworkConfig.REGULARIZE_NEURAL_FEATURES = false;
										NetworkConfig.OPTIMIZE_NEURAL = false;  //not optimize in CRF..
										NetworkConfig.IS_INDEXED_NEURAL_FEATURES = false; //only used when using the senna embedding.
									}
								break;
				case "-neuralconfig":neuralConfig = args[i+1]; break;
				case "-reg": l2 = Double.valueOf(args[i+1]);  break;
				case "-lr": AdaGrad_Learning_Rate = Double.valueOf(args[i+1]); break;
				case "-adagrad": useAdaGrad = args[i+1].equals("true")? true:false;break;
				case "-nonmarkov": if(args[i+1].equals("true")) nonMarkov = true; else nonMarkov= false; break;
				case "-depf": if(args[i+1].equals("true")) depFeature = true; else depFeature= false; break;
				case "-useincom": useIncompleteSpan = args[i+1].equals("true")? true:false;break;
				case "-usedepnet": useDepNet = args[i+1].equals("true")? true:false;break;
				case "-modelPath": modelFile = args[i+1]; break;
				default: System.err.println("Invalid arguments, please check usage."); System.exit(0);
			}
		}
	}
	
	
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
		
		
		//always use conll data
//		train_filename = "data/semi/semi.train.txt";
//		test_filename = "data/semi/semi.test.txt";
		
		processArgs(args);
		String resEval = "data/semi/semi.eval.txt";
		String resRes = "data/semi/semi.res.txt";
		/**data is 0-indexed, network compiler is 1-indexed since we have leaf nodes.**/
		SemiCRFInstance[] trainInstances= readCoNLLData(train_filename, true,	trainNum);
		SemiCRFInstance[] testInstances	= readCoNLLData(test_filename, 	false,	testNumber);
	
		int maxSize = 0;
		int maxSpan = 0;
		for(SemiCRFInstance instance: trainInstances){
			maxSize = Math.max(maxSize, instance.size());
			for(Span span: instance.output){
				maxSpan = Math.max(maxSpan, span.end-span.start+1);
			}
		}
		for(SemiCRFInstance instance: testInstances){
			maxSize = Math.max(maxSize, instance.size());
		}
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig.CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = l2;
		NetworkConfig.NUM_THREADS = numThread;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = true;

		//modify this. and read neural config
		OptimizerFactory of = NetworkConfig.USE_NEURAL_FEATURES || NetworkConfig.MODEL_TYPE==ModelType.SSVM? 
				OptimizerFactory.getGradientDescentFactoryUsingAdaGrad(AdaGrad_Learning_Rate):OptimizerFactory.getLBFGSFactory();
		
		if(NetworkConfig.USE_NEURAL_FEATURES) NeuralConfigReader.readConfig(neuralConfig);
		if(useAdaGrad) of = OptimizerFactory.getGradientDescentFactoryUsingAdaGrad(AdaGrad_Learning_Rate);
		
		
		int size = trainInstances.length;
		
		System.err.println("Read.."+size+" instances.");
		
		SemiViewer sViewer = new SemiViewer();
		
		GlobalNetworkParam gnp = null;
		if(modelFile==null || !new File(modelFile).exists()){
			gnp = new GlobalNetworkParam(of);
		}else{
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(modelFile));
			gnp=(GlobalNetworkParam)in.readObject();
			in.close();
		}
		
		SemiCRFNetworkCompiler compiler = new SemiCRFNetworkCompiler(maxSize, maxSpan,sViewer, useDepNet);
		SemiCRFFeatureManager fm = new SemiCRFFeatureManager(gnp, nonMarkov, depFeature);
		NetworkModel model = NetworkConfig.TRAIN_MODE_IS_GENERATIVE ? GenerativeNetworkModel.create(fm, compiler) : DiscriminativeNetworkModel.create(fm, compiler);
		
		if(!new File(modelFile).exists()){
			model.train(trainInstances, numIterations);
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(modelFile));
			out.writeObject(fm.getParam_G());
			out.close();
		}
		
		Instance[] predictions = model.decode(testInstances);
		SemiEval.evalNER(predictions, resEval);
		SemiEval.writeNERResult(predictions, resRes);
	}
	
	/**
	 * Read data from file in a CoNLL format 0-index.
	 * @param fileName
	 * @param isLabeled
	 * @return
	 * @throws IOException
	 */
	private static SemiCRFInstance[] readCoNLLData(String fileName, boolean isLabeled, int number) throws IOException{
		InputStreamReader isr = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
		BufferedReader br = new BufferedReader(isr);
		ArrayList<SemiCRFInstance> result = new ArrayList<SemiCRFInstance>();
		ArrayList<WordToken> wts = new ArrayList<WordToken>();
		List<Span> output = new ArrayList<Span>();
		int instanceId = 1;
		int start = -1;
		int end = 0;
		Label prevLabel = null;
		while(br.ready()){
			String line = br.readLine().trim();
			if(line.length() == 0){

				end = wts.size()-1;
				if(start != -1){
					createSpan(output, start, end, prevLabel);
				}
				SemiCRFInstance instance = new SemiCRFInstance(instanceId, 1);
				WordToken[] wtArr = new WordToken[wts.size()];
				instance.input = new Sentence(wts.toArray(wtArr));
				instance.output = output;
				if(isLabeled){
					instance.setLabeled(); // Important!
				} else {
					instance.setUnlabeled();
				}
				if(useIncompleteSpan && EntityChecker.checkAllIncomplete(instance.input).size()>0){
					//do nothing. just don't add.
				}else{
					instanceId++;
					result.add(instance);
				}
				wts = new ArrayList<WordToken>();
				output = new ArrayList<Span>();
				prevLabel = null;
				start = -1;
				end = 0;
				if(result.size()==number)
					break;
			} else {
				String[] values = line.split("[\t ]");
				int index = Integer.valueOf(values[0]) - 1; //because it is starting from 1
				String word = values[1];
				wts.add(new WordToken(word, values[2], Integer.valueOf(values[4])-1, values[3]));
				String form = values[3];
				Label label = null;
				if(form.startsWith("B")){
					if(start != -1){
						end = index - 1;
						createSpan(output, start, end, prevLabel);
					}
					start = index;
					
					label = Label.get(form.substring(2));
					
				} else if(form.startsWith("I")){
					label = Label.get(form.substring(2));
				} else if(form.startsWith("O")){
					if(start != -1){
						end = index - 1;
						createSpan(output, start, end, prevLabel);
					}
					start = -1;
					createSpan(output, index, index, Label.get("O"));
					label = Label.get("O");
				}
				prevLabel = label;
			}
		}
		br.close();
		return result.toArray(new SemiCRFInstance[result.size()]);
	}
	
	private static void createSpan(List<Span> output, int start, int end, Label label){
		if(label==null){
			throw new RuntimeException("The label is null");
		}
		if(start>end){
			throw new RuntimeException("start cannot be larger than end");
		}
		if(label.form.startsWith("O")){
			for(int i=start; i<=end; i++){
				output.add(new Span(i, i, label));
			}
		} else {
			output.add(new Span(start, end, label));
		}
	}

}

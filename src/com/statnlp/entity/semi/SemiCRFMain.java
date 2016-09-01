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
import java.util.HashSet;
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
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.neural.NeuralConfigReader;

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
	public static boolean isTrain = true;
	public static String dataType = "abc";
	public static String testSuff = "test";
	public static String train_filename = "data/alldata/nbc/ecrf.train.MISC.txt";
	public static String test_filename = "data/alldata/nbc/ecrf.test.MISC.txt";
	public static String extention = "model0";
	/** true means using the predicted dependency features.. if not used dep features, this option does not matter**/
	public static boolean isPipe = false; 
	public static boolean ignore = false;
	
	
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
//				case "-useincom": useIncompleteSpan = args[i+1].equals("true")? true:false;break;
//				case "-usedepnet": useDepNet = args[i+1].equals("true")? true:false;break;
				case "-modelPath": modelFile = args[i+1]; break;
				case "-dev": testSuff = args[i+1].equals("true")? "dev":"test"; break;
				case "-data": dataType = args[i+1]; break;
				case "-ext": extention = args[i+1]; break;
				case "-mode": isTrain = args[i+1].equals("train")?true:false; break;
				case "-pipe": isPipe = args[i+1].equals("true")?true:false;break;
				case "-ignore": ignore = args[i+1].equals("true")?true:false;break;
				default: System.err.println("Invalid arguments, please check usage."); System.exit(0);
			}
		}
	}
	
	
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
		
		
		//always use conll data
//		train_filename = "data/semi/semi.train.txt";
//		test_filename = "data/semi/semi.test.txt";
		
		processArgs(args);
		System.out.println("[Info] using the predicted dependency?:"+isPipe);
		
		/**data is 0-indexed, network compiler is 1-indexed since we have leaf nodes.**/
//		train_filename = "data/semeval10t1/ecrf.train.MISC.txt";
//		test_filename = "data/semeval10t1/ecrf."+testSuff+".MISC.txt";
		/**Read the all data**/
		String prefix = "data/alldata/"+dataType+"/";
		train_filename = prefix+"train.conllx";
		test_filename = isPipe? prefix+"only.test.dp.res.txt":prefix+testSuff+".conllx";
		String depStruct = isPipe? "pred":"gold";
		boolean model1 = false;
		boolean model2 = false;
		if(extention.equals("model1")) { model1 = true; useDepNet = true; }
		else if(extention.equals("model2")) {model2 = true; useDepNet = true;}
		System.out.println("[Info] Current Model Extention:"+extention);
		System.out.println("[Info] Ignore those not fit in "+extention+":"+ignore);
		System.out.println("[Info] Current Dataset:"+dataType);
		String ign = ignore? "ignore":"noignore";
		String resEval = "data/alldata/"+dataType+"/output/semi."+extention+"."+depStruct+".depf-"+depFeature+"."+ign+".eval.txt";
		String resRes  = "data/alldata/"+dataType+"/output/semi."+extention+"."+depStruct+".depf-"+depFeature+"."+ign+".es.txt";
		
		System.out.println("[Info] Reading data:"+train_filename);
		System.out.println("[Info] Reading data:"+test_filename);
		SemiCRFInstance[] trainInstances = readCoNLLData(train_filename, true,	trainNum, false);
		SemiCRFInstance[] testInstances	 = readCoNLLData(test_filename, false,	testNumber, isPipe);
		if(model2){
			//print some information if using model 2
			int notConnected = 0;
			for(SemiCRFInstance inst: trainInstances){
				notConnected+=checkConnected(inst);
//				if(checkConnected(inst)>0)
//					System.out.println(inst.getInput().toString());
			}
			System.out.println("isgnore:"+ignore+" not connected entities in train:"+notConnected);
			notConnected = 0;
			for(SemiCRFInstance inst: testInstances){
				notConnected+=checkConnected(inst);
//				if(checkConnected(inst)>0)
//					System.out.println(inst.getInput().toString());
			}
			System.out.println("not connected entities in test:"+notConnected);
		}
		if(model1){
			//print some information if using model 2
			int incom = 0;
			for(SemiCRFInstance inst: trainInstances){
				incom+=EntityChecker.checkAllIncomplete(inst.input).size();
//				if(EntityChecker.checkAllIncomplete(inst.input).size()>0 && inst.size()<=15)
//					System.out.println(inst.input.toString()+"\n");
			}
			System.out.println("incomplete entities in train:"+incom);
			incom = 0;
			for(SemiCRFInstance inst: testInstances){
				incom+=EntityChecker.checkAllIncomplete(inst.input).size();
//				if(EntityChecker.checkAllIncomplete(inst.input).size()>0 && inst.size()<=15)
//					System.out.println(inst.input.toString()+"\n");
			}
			System.out.println("incomplete entities in test:"+incom);
		}
	
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
		if(isTrain || modelFile==null || modelFile.equals("") || !new File(modelFile).exists()){
			gnp = new GlobalNetworkParam(of);
		}else{
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(modelFile));
			gnp=(GlobalNetworkParam)in.readObject();
			in.close();
		}
		
		SemiCRFNetworkCompiler compiler = new SemiCRFNetworkCompiler(maxSize, maxSpan,sViewer, useDepNet, model1, model2, ignore);
		SemiCRFFeatureManager fm = new SemiCRFFeatureManager(gnp, nonMarkov, depFeature);
		NetworkModel model = NetworkConfig.TRAIN_MODE_IS_GENERATIVE ? GenerativeNetworkModel.create(fm, compiler) : DiscriminativeNetworkModel.create(fm, compiler);
		
		if(isTrain){
			model.train(trainInstances, numIterations);
			if(modelFile!=null && !modelFile.equals("") ){
				ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(modelFile));
				out.writeObject(fm.getParam_G());
				out.close();
			}
		}
		
		Instance[] predictions = model.decode(testInstances);
		SemiEval.evalNER(predictions, resEval);
		SemiEval.writeNERResult(predictions, resRes);
	}
	
	/**
	 * Read data from file in a CoNLL format 0-index.
	 * @param fileName
	 * @param isLabeled
	 * @param isPipe: true means read the predicted features. always set to false for reading training instances.
	 * @return
	 * @throws IOException
	 */
	private static SemiCRFInstance[] readCoNLLData(String fileName, boolean isLabeled, int number, boolean isPipe) throws IOException{
		if(isLabeled && isPipe) throw new RuntimeException("training instances always have the true dependency");
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
				instance.input.setRecognized();
				instance.output = output;
				//instance.leftDepRel = sent2LeftDepRel(instance.input);
				if(isLabeled){
					instance.setLabeled(); // Important!
				} else {
					instance.setUnlabeled();
				}
				if(isLabeled && ignore && extention.equals("model2") && checkConnected(instance)>0){
					//do nothing. just don't add, ignore those invalid.
					
				}else if(isLabeled && ignore && extention.equals("model1") && EntityChecker.checkAllIncomplete(instance.input).size()>0){
					//do nothing
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
				String depLabel = null;
				int headIdx = -1;
				if(!isPipe){
					depLabel = values[7];
					headIdx = Integer.valueOf(values[6])-1;
				}else{
					if(values.length<13) {br.close(); throw new RuntimeException("No predicted dependency label comes out?");}
					depLabel = values.length==13? values[12]: null;
					headIdx = Integer.valueOf(values[11])-1;
					
				}
				wts.add(new WordToken(word, values[2], headIdx, values[3], depLabel));
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

	
	private static int checkConnected(SemiCRFInstance inst){
		int number = 0;
		List<Span> output = inst.getOutput();
		Sentence sent = inst.getInput();
		for(Span span: output){
			int start = span.start;
			int end = span.end;
			Label label = span.label;
			if(label.equals(Label.get("O")) || start==end) continue;
			HashSet<Integer> set = new HashSet<Integer>();
			int times = 0;
			while(times<(end-start+1)){
				for(int pos = start; pos<=end; pos++){
					int headIdx = sent.get(pos).getHeadIndex();
					if(headIdx<start || headIdx>end) continue;
					if(set.size()==0 || set.contains(pos) || set.contains(headIdx)){
						set.add(pos); set.add(headIdx);
					}
				}
				times++;
			}
			if(set.size()!=(end-start+1)) {
				number++;
			}
		}
//		if(number>0)
//			System.out.println(sent.toString());
		return number;
	}

}

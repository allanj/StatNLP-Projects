package com.statnlp.entity.semi2d;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;
import com.statnlp.dp.utils.DPConfig;
import com.statnlp.entity.semi.Label;
import com.statnlp.entity.semi.SemiCRFInstance;
import com.statnlp.entity.semi.SemiEval;
import com.statnlp.entity.semi.Span;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GenerativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkModel;

/**
 * Semi CRF for named entity recognition. 
 * But each node has two directions. 
 * @author allanjie
 *
 */
public class S2DCRFMain {
	
	
	public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException, NoSuchFieldException, SecurityException, InterruptedException, IllegalArgumentException, IllegalAccessException{
		
		DPConfig.windows = true;
		
		String train_filename;
		String test_filename;
		SemiCRFInstance[] trainInstances; 
		SemiCRFInstance[] testInstances;
		
		//always use conll data
//		train_filename = DPConfig.ecrftrain;
//		test_filename = DPConfig.ecrftest;
		train_filename = "data/semi/semi.train.txt";
		test_filename = "data/semi/semi.test.txt";
		trainInstances = readCoNLLData(train_filename, true,-1);
		testInstances = readCoNLLData(test_filename, false,-1);
		
		String resEval = "data/semi/semi.eval.txt";
		String resRes = "data/semi/semi.res.txt";
		
		
		int maxSize = 0;
		int maxSpan = 0;
		for(SemiCRFInstance instance: trainInstances){
			maxSize = Math.max(maxSize, instance.size());
			for(Span span: instance.output){
				maxSpan = Math.max(maxSpan, span.end-span.start);
			}
		}
		for(SemiCRFInstance instance: testInstances){
			maxSize = Math.max(maxSize, instance.size());
		}
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig.CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = 0.01;
		NetworkConfig.NUM_THREADS = 38;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = false;
		
		int numIterations = 5000;
		
		int size = trainInstances.length;
		
		System.err.println("Read.."+size+" instances.");
		
		S2DViewer sViewer = new S2DViewer();
		S2DCRFFeatureManager fm = new S2DCRFFeatureManager(new GlobalNetworkParam());
		
		
		S2DNetworkCompiler compiler = new S2DNetworkCompiler(maxSize, maxSpan,sViewer);
		
		NetworkModel model = NetworkConfig.TRAIN_MODE_IS_GENERATIVE ? GenerativeNetworkModel.create(fm, compiler) : DiscriminativeNetworkModel.create(fm, compiler);
		
		model.train(trainInstances, numIterations);
		
		Instance[] predictions = model.decode(testInstances);
		SemiEval.evalNER(predictions, resEval);
		SemiEval.writeNERResult(predictions, resRes);
	}
	
	
	
	/**
	 * Read data from file in a CoNLL format 
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
				//System.err.println(output.toString());
				if(isLabeled){
					instance.setLabeled(); // Important!
				} else {
					instance.setUnlabeled();
				}
				instanceId++;
				result.add(instance);
				wts = new ArrayList<WordToken>();
				output = new ArrayList<Span>();
				prevLabel = null;
				start = -1;
				end = 0;
				if(result.size()==number)
					break;
			} else {
				String[] values = line.split("\t");
				int index = Integer.valueOf(values[0]) - 1; //because it is starting from 1
				String word = values[1];
				wts.add(new WordToken(word, values[2]));
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

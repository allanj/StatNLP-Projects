package com.statnlp.example.lcrf;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.statnlp.commons.crf.Label;
import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkModel;

public class LCRFMain {

	public static List<Label> allLabels;
	
	public static List<LCRFInstance> readData(String path, boolean setLabel, int number) throws IOException{
		allLabels = new ArrayList<Label>();
		BufferedReader br = RAWF.reader(path);
		String line = null;
		List<LCRFInstance> lcrfs = new ArrayList<LCRFInstance>();
		int index =1;
		ArrayList<String> words = new ArrayList<String>();
		ArrayList<Label> tags = new ArrayList<Label>();
		Map<String, Integer> labelId = new HashMap<String, Integer>();
		int labelID = 0;
		while((line = br.readLine())!=null){
			if(line.equals("")){
				LCRFInstance lcrfInstance = new LCRFInstance(index++,1.0);
				lcrfInstance.words = words;
				lcrfInstance.tags = tags;
				if(setLabel)
					lcrfInstance.setLabeled();
				else
					lcrfInstance.setUnlabeled();
				words = new ArrayList<String>();
				tags = new ArrayList<Label>();
				lcrfs.add(lcrfInstance);
				if(number!=-1 && lcrfs.size()==number) break;
				continue;
			}
			String[] values = line.split(" ");
			words.add(values[0]);
			if(labelId.containsKey(values[1]))
				tags.add(new Label(labelId.get(values[1]), values[1]));
			else{
				Label label = new Label(labelID, values[1]);
				allLabels.add(label);
				labelId.put(values[1], labelID);
				tags.add(label);
				labelID++;
			}
		}
		br.close();
		return lcrfs;
	}
	
	public static void main(String[] args) throws IOException, InterruptedException{
		
//		int trainNum = Integer.valueOf(args[0]);
//		int testNum = Integer.valueOf(args[1]);
//		int numThreads = Integer.valueOf(args[2]);
//		int iteration = Integer.valueOf(args[3]);
		
		int trainNum = 10;
		int testNum = 10;
		int numThreads = 5;
		int iteration = 50;
		// TODO Auto-generated method stub
		List<LCRFInstance> trainInstances = readData("data/train.txt",true,trainNum);
		System.out.println(trainInstances.size());
		List<LCRFInstance> testInstances = readData("data/test.txt",false,testNum);
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig.CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig.NUM_THREADS = numThreads;
		
		LCRFFeatureManager fa = new LCRFFeatureManager(new GlobalNetworkParam());
		LCRFNetworkCompiler compiler = new LCRFNetworkCompiler(allLabels);
		NetworkModel model = DiscriminativeNetworkModel.create(fa, compiler);
		
		model.train(trainInstances.toArray(new LCRFInstance[trainInstances.size()]), iteration);
		Instance[] predictions = model.decode(testInstances.toArray(new LCRFInstance[testInstances.size()]));
		
		int corr = 0;
		int total = 0;
		for(Instance instance: predictions){
			LCRFInstance lcrfInstance = (LCRFInstance)instance;
			List<Label> output = lcrfInstance.getOutput();
			List<Label> prediction = lcrfInstance.getPrediction();
			
			for(int i=0;i<output.size();i++){
				
				if(output.get(i).getTag().equals(prediction.get(i).getTag()))
					corr++;
				total++;
			}
		}
		System.out.println(corr+","+total);
		System.out.println("Accuracy:"+corr*1.0/total);
	}

}

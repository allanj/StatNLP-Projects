package com.statnlp.projects.dep.model.pipeline;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.projects.dep.DependInstance;
import com.statnlp.projects.dep.DependencyReader;
import com.statnlp.projects.dep.DependencyTransformer;
import com.statnlp.projects.entity.semi.SemiCRFInstance;
import com.statnlp.projects.entity.semi.SemiCRFMain;

/***
 * A pipeline approach starting from NER to dependency parsing
 * @author allanjie
 *
 */
public class IterativeModel {

	protected NetworkModel depModel;
	protected NetworkModel semiModel; 
	protected NetworkModel semiOnlyModel;
	protected String trainFile;
	protected String testFile;
	protected String nerOut;
	protected int testNumber = -1;
	protected ResultInstance[] results;
	//debug purpose..
	protected NetworkModel depOnlyModel;
	
	public IterativeModel(String depOnlyModelFile, String depModelFile, String semiModelFile, String semiNodepf, String testFile, int testNumber) {
		ObjectInputStream in;
		try {
			System.err.println("[Info] Reading dependency model...");
			in = new ObjectInputStream(new FileInputStream(depModelFile));
			try {
				depModel = (NetworkModel)in.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			in.close();
			System.err.println("[Info] Reading semiCRF only model...");
			in = new ObjectInputStream(new FileInputStream(semiNodepf));
			try {
				semiOnlyModel = (NetworkModel)in.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			in.close();
			System.err.println("[Info] Reading semiCRF model...");
			in = new ObjectInputStream(new FileInputStream(semiModelFile));
			try {
				semiModel = (NetworkModel)in.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			in.close();
			System.err.println("[Info] Reading depOnly model...");
			in = new ObjectInputStream(new FileInputStream(depOnlyModelFile));
			try {
				depOnlyModel = (NetworkModel)in.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.err.println("[Info] All models read..");
		results = Reader.readResults(testFile, testNumber);
		this.testFile = testFile;
		this.testNumber = testNumber;
	}
	
	public void setFiles(String trainFile, String nerOut) {
		this.trainFile = trainFile;
		this.nerOut = nerOut;
	}
	
	/**
	 * Initialization will obtain the NER result first/without the dependency feature
	 * using the semiOnly model
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private DependInstance[] init() throws InterruptedException, IOException {
		System.err.println("[Info] Initialization...");
		SemiCRFMain.readCoNLLData(trainFile, false,	-1);
		DependencyReader.readCoNLLX(trainFile, false, -1, new DependencyTransformer(), false);
		SemiCRFMain.readCoNLLData(testFile, false,	-1);
		DependencyReader.readCoNLLX(testFile, false, -1, new DependencyTransformer(), false);
		SemiCRFInstance[] testInstances	 = SemiCRFMain.readCoNLLData(testFile, false,	testNumber);
		NetworkIDMapper.setCapacity(new int[]{10000, 20, 100});
		Instance[] results = semiOnlyModel.decode(testInstances);
		Eval.evalNER(results, this.results, nerOut);
		return Converter.semiInst2DepInst(results);
	}
	
	private void debug() throws InterruptedException, IOException {
		System.err.println("[Info] Debugging with dep model with entity features...");
		DependInstance[] testInstances = DependencyReader.readCoNLLX(testFile, false,testNumber, new DependencyTransformer(), false);
		NetworkIDMapper.setCapacity(new int[]{500, 500, 5, 5, 100, 10});
		Instance[] results = depModel.decode(testInstances);
		Eval.evalDP(results, this.results);
	}
	
	/**
	 * Training means starting from the ner model.
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public void iterate(int maxIter) throws InterruptedException, IOException {
		debug();
		DependInstance[] testInsts = init();
		Instance[] semiRes = null;
		for (int it = 0; it < maxIter; it++) {
			NetworkIDMapper.setCapacity(new int[]{500, 500, 5, 5, 100, 10});
			System.err.println("[Info] Iteration " + (it + 1) + ":");
			Instance[] depRes = depModel.decode(testInsts);
			Eval.evalDP(depRes, this.results);
			SemiCRFInstance[] testSemiInsts =  Converter.depInst2SemiInst(depRes);
			NetworkIDMapper.setCapacity(new int[]{10000, 20, 100});
			semiRes = semiModel.decode(testSemiInsts);
			Eval.evalNER(semiRes, this.results, nerOut);
			System.err.println();
		}
	}
	
	public static void main(String[] args) throws InterruptedException, IOException {
		String data = args[0];
		NetworkConfig.NUM_THREADS = Integer.parseInt(args[1]);
		int testNumber = Integer.parseInt(args[2]);
		String depOnlyModelFile = "data/allanprocess/"+data+"/output/dep.test.noef.dep.model";
		String depModelFile = "data/allanprocess/"+data+"/output/dep.test.ef.dep.model";
		String semiModelFile = "data/allanprocess/"+data+"/output/semi.semi.gold.depf-true.noignore.model";
		String semiOnlyModelFile = "data/allanprocess/"+data+"/output/semi.semi.gold.depf-false.noignore.model";
		String testFile = "data/allanprocess/"+data+"/test.conllx";
		String trainFile = "data/allanprocess/"+data+"/train.conllx";
		String nerOut = "data/allanprocess/"+data+"/output/semi.pipe.eval.txt";
		IterativeModel model = new IterativeModel(depOnlyModelFile, depModelFile, semiModelFile, semiOnlyModelFile, testFile, testNumber);
		model.setFiles(trainFile, nerOut);
		int maxIter = 10;
		model.iterate(maxIter);
	}

}

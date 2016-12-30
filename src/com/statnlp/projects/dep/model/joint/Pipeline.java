package com.statnlp.projects.dep.model.joint;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.projects.dep.DependInstance;
import com.statnlp.projects.dep.Evaluator;
import com.statnlp.projects.dep.model.joint.stat.ResultInstance;
import com.statnlp.projects.entity.semi.SemiCRFInstance;
import com.statnlp.projects.entity.semi.SemiCRFMain;
import com.statnlp.projects.entity.semi.SemiEval;

/***
 * A pipeline approach starting from NER to dependency parsing
 * @author allanjie
 *
 */
public class Pipeline {

	protected NetworkModel depModel;
	protected NetworkModel semiModel; 
	protected NetworkModel semiOnlyModel; 
	protected String testFile;
	protected String nerOut;
	protected String dpOut;
	protected int testNumber = -1;
	protected ResultInstance[] results;
	
	public Pipeline(String depModelFile, String semiModelFile, String semiNodepf, String testFile) {
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(new FileInputStream(depModelFile));
			try {
				depModel = (NetworkModel)in.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			in.close();
			in = new ObjectInputStream(new FileInputStream(semiNodepf));
			try {
				semiOnlyModel = (NetworkModel)in.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			in.close();
			in = new ObjectInputStream(new FileInputStream(semiModelFile));
			try {
				semiModel = (NetworkModel)in.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		results = readResults(testFile);
	}
	
	/**
	 * Initialization will obtain the NER result first/without the dependency feature
	 * using the semiOnly model
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private DependInstance[] init() throws InterruptedException, IOException {
		SemiCRFInstance[] testInstances	 = SemiCRFMain.readCoNLLData(testFile, false,	testNumber);
		Instance[] results = semiOnlyModel.decode(testInstances);
		return semiInst2DepInst(results);
	}
	
	/**
	 * Training means starting from the ner model.
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public void train(int maxIter) throws InterruptedException, IOException {
		DependInstance[] testInsts = init();
		Instance[] semiRes = null;
		for (int it = 0; it < maxIter; it++) {
			System.err.println("Iteration " + (it + 1));
			Instance[] depRes = depModel.decode(testInsts);
			Evaluator.evalDP(testInsts, dpOut);
			SemiCRFInstance[] testSemiInsts =  depInst2SemiInst(depRes);
			semiRes = semiModel.decode(testSemiInsts);
			SemiEval.evalNER(semiRes, nerOut);
			System.err.println();
		}
		output(semiRes);
	}
	
	public void output(Instance[] semiRes) {
		//export CoNLLX format, for read conveniently.
	}
	
	private DependInstance[] semiInst2DepInst(Instance[] insts) {
		return null;
	}
	
	private SemiCRFInstance[] depInst2SemiInst(Instance[] insts) {
		return null;
	}
	
	
	private ResultInstance[] readResults(String testFile) throws IOException {
		
	}
	
	public static void main(String[] args) {
		
	}

}

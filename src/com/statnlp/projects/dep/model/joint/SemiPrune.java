package com.statnlp.projects.dep.model.joint;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Set;

import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.projects.entity.semi.SemiCRFFeatureManager;
import com.statnlp.projects.entity.semi.SemiCRFInstance;
import com.statnlp.projects.entity.semi.SemiCRFMain;
import com.statnlp.projects.entity.semi.SemiCRFNetworkCompiler;
import com.statnlp.projects.entity.semi.SemiLabel;
import com.statnlp.projects.entity.semi.SemiViewer;
import com.statnlp.projects.entity.semi.Span;

/**
 * A semi CRF for pruning the variables.
 * Note that for training and testing, configuration is different
 * @author allanjie
 *
 */
public class SemiPrune {

	private String trainFile;
	private String testFile;
	private int trainNum;
	private int testNum;
	private int numThreads;
	/**Run the semi until converge. **/
	private final int ITER = 10000;
	/**TODO: do we still need this L2 regularization for training?**/
	private double l2 = 0.1; 
	/**Now assume we dun have the depFeatures. But for training we have.**/
	private boolean depFeature = false;
	private Map<Integer, Map<Integer, Map<Integer, Set<Integer>>>> prunedMap;
	
	public SemiPrune(String trainFile, String testFile, int trainNum, int testNum, int numThreads, double L2) {
		this.trainFile = trainFile;
		this.testFile = testFile;
		this.trainNum = trainNum;
		this.testNum = testNum;
		this.numThreads = numThreads;
		this.l2 = L2;
	}
	
	public Map<Integer, Map<Integer, Map<Integer, Set<Integer>>>> prune(double prunedProb) throws IOException, InterruptedException {
		SemiCRFInstance[] trainInsts = SemiCRFMain.readCoNLLData(trainFile, true, trainNum);
		SemiCRFInstance[] testInsts = SemiCRFMain.readCoNLLData(testFile, false,	testNum);
		System.err.println("Labels from SemiCRFs: " + SemiLabel.LABELS.toString());
		int maxSize = 0;
		int maxSpan = 0;
		for(SemiCRFInstance instance: trainInsts){
			maxSize = Math.max(maxSize, instance.size());
			for(Span span: instance.output){
				maxSpan = Math.max(maxSpan, span.end-span.start+1);
			}
		}
		for(SemiCRFInstance instance: testInsts){
			maxSize = Math.max(maxSize, instance.size());
		}
		NetworkConfig.L2_REGULARIZATION_CONSTANT = l2;
		NetworkConfig.NUM_THREADS = numThreads;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = true;
		NetworkConfig.prunedProb = prunedProb;
		SemiCRFNetworkCompiler compiler = new SemiCRFNetworkCompiler(maxSize, maxSpan, new SemiViewer(), false, false, false, false);
		GlobalNetworkParam globalParam = new GlobalNetworkParam();	
		SemiCRFFeatureManager fm = new SemiCRFFeatureManager(globalParam, false, depFeature);
		NetworkModel model = DiscriminativeNetworkModel.create(fm, compiler, true);
		model.train(trainInsts, ITER);
		System.err.println("[Info] Train Map size: " + model.getGlobalPrunedMap().size());
		model.decode(testInsts);
		prunedMap = model.getGlobalPrunedMap();
		model = null;
		compiler = null;
		globalParam = null;
		fm = null;
		System.err.println("[Info] Global Map size: " + prunedMap.size());
		/**Counting number of spans**/
		int numSpans = 0;
		for (int instId: prunedMap.keySet()) {
			Map<Integer, Map<Integer, Set<Integer>>> instPrunedMap = prunedMap.get(instId);
			for (int leftIdx: instPrunedMap.keySet()) {
				Map<Integer, Set<Integer>> leftPrunedMap = instPrunedMap.get(leftIdx);
				for (int rightIdx: leftPrunedMap.keySet()) {
					numSpans += leftPrunedMap.get(rightIdx).size();
				}
			}
		}
		System.err.println("[Info] total number of spans: " + numSpans);
		return prunedMap;
	}
	
	private static void writeObject(ObjectOutputStream out, Map<Integer, Map<Integer, Map<Integer, Set<Integer>>>> prunedMap) throws IOException {
        out.writeObject(prunedMap);
        out.close();
    }
	
	public static void main(String... args) throws IOException, InterruptedException {
		String subsection = "abc";
		String trainFile = "data/allanprocess/"+subsection+"/train.conllx";
		String testFile = "data/allanprocess/"+subsection+"/test.conllx";
		int trainNum = -1;
		int testNum = -1;
		double L2 = 0.1;
		int numThreads = 35;
		double prunedProb = 0.001;
		SemiPrune pruner = new SemiPrune(trainFile, testFile, trainNum, testNum, numThreads, L2);
		writeObject(new ObjectOutputStream(new FileOutputStream("data/allanprocess/"+subsection+"/pruned")), pruner.prune(prunedProb));
		
	}
	
}

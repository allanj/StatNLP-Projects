package com.statnlp.projects.dep.model.joint;

import java.io.IOException;
import java.util.HashMap;
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
	private int numThread;
	/**Run the semi until converge. **/
	private final int ITER = 10000;
	/**TODO: do we still need this L2 regularization for training?**/
	private double l2 = 0.01; 
	/**Now assume we dun have the depFeatures. But for training we have.**/
	private boolean depFeature = false;
	private SemiCRFInstance[] trainInsts;
	private SemiCRFInstance[] testInsts;
	private SemiCRFNetworkCompiler compiler; 
	private SemiCRFFeatureManager fm;
	private NetworkModel model;
	private GlobalNetworkParam globalParam;

	public SemiPrune(String trainFile, String testFile, int trainNum, int testNum) {
		this.trainFile = trainFile;
		this.testFile = testFile;
		this.trainNum = trainNum;
		this.testNum = testNum;
	}
	
	public void init() throws IOException, InterruptedException {
		this.trainInsts = SemiCRFMain.readCoNLLData(trainFile, true, trainNum);
		this.testInsts = SemiCRFMain.readCoNLLData(testFile, false,	testNum);
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
		NetworkConfig.NUM_THREADS = numThread;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = true;
		this.compiler = new SemiCRFNetworkCompiler(maxSize, maxSpan, new SemiViewer(), false, false, false, false);
		globalParam = new GlobalNetworkParam();	
		this.fm = new SemiCRFFeatureManager(globalParam, false, depFeature);
		this.model = DiscriminativeNetworkModel.create(fm, compiler);
		model.train(trainInsts, ITER);
		
	}
	
	
	/**
	 * Return a map containing the (semi-CRFs) span information.
	 * Map<InstanceId, Map<LeftIndex, Set<SemiSpan>>>
	 * Note that the span in CRF is 0-indexed.
	 * @return
	 */
	private Map<Integer, Map<Integer, Set<Span>>> prune() {
		Map<Integer, Map<Integer, Set<Span>>> spanMap = new HashMap<>();
		return spanMap;
	}
	
}

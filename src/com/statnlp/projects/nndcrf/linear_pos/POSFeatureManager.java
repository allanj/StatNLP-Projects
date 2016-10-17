package com.statnlp.projects.nndcrf.linear_pos;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.neural.NeuralConfig;

public class POSFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 376931974939202432L;

	public enum FEATYPE {word, 
		word_l, 
		word_ll, 
		word_r, 
		word_rr, 
		cap, 
		cap_l, 
		cap_ll, 
		cap_r, 
		cap_rr,
		chunk, 
		chunk_l, 
		chunk_ll, 
		chunk_r, 
		chunk_rr, 
		transition, neural_1, neural_2};
	private String OUT_SEP = NeuralConfig.OUT_SEP; 
	private String IN_SEP = NeuralConfig.IN_SEP;
	
	private boolean cascade;
	private int windowSize;
	private boolean basicFeatures;
	
	/**
	 * The initialization of the feature manager.
	 * @param param_g: global parameter for weights
	 * @param basicFeatures: use basic features or not
	 * @param cascade: use the features from previous model or not
	 * @param windowSize: the window size for window features
	 */
	public POSFeatureManager(GlobalNetworkParam param_g, boolean basicFeatures, boolean cascade, int windowSize) {
		super(param_g);
		this.cascade = cascade;
		this.windowSize = windowSize;
		this.basicFeatures = basicFeatures;
	}
	
	
	
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		
		//the tag in the sentence is actually the caps id.
		
		POSInstance inst = ((POSInstance)network.getInstance());
		//int instanceId = inst.getInstanceId();
		Sentence sent = inst.getInput();
		long node = network.getNode(parent_k);
		int[] nodeArr = NetworkIDMapper.toHybridNodeArray(node);
		
		FeatureArray fa = null;
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		
		int pos = nodeArr[0]-1;
		int eId = nodeArr[1];
		if(pos<0 || pos > inst.size())
			return FeatureArray.EMPTY;
			
//		System.err.println(Arrays.toString(nodeArr) + Entity.get(eId).toString());
		int[] child = NetworkIDMapper.toHybridNodeArray(network.getNode(children_k[0]));
		int childEId = child[1];
		
		String lw = pos>0? sent.get(pos-1).getName():"<PAD>";
		String llw = pos==0? "<PAD>": pos==1? "<PAD>":sent.get(pos-2).getName();
		String rw = pos<sent.length()-1? sent.get(pos+1).getName():"<PAD>";
		String rrw = pos==sent.length()? "<PAD>":pos==sent.length()-1? "<PAD>": pos==sent.length()-2? "<PAD>":sent.get(pos+2).getName();
		String w = pos==inst.size()? "<PAD>":inst.getInput().get(pos).getName();
		
		String caps = capsF(w);
		String lcaps = capsF(lw);
		String llcaps = capsF(llw);
		String rcaps = capsF(rw);
		String rrcaps = capsF(rrw);
		
		//needs to be careful about using NP chunks or all chunks 
		String lchunk = pos>0? sent.get(pos-1).getEntity():"<PAD>";
		String llchunk = pos==0? "<PAD>": pos==1? "<PAD>":sent.get(pos-2).getEntity();
		String rchunk = pos<sent.length()-1? sent.get(pos+1).getEntity():"<PAD>";
		String rrchunk = pos==sent.length()? "<PAD>":pos==sent.length()-1? "<PAD>": pos==sent.length()-2? "<PAD>":sent.get(pos+2).getEntity();
		String chunk = pos==inst.size()? "<PAD>":inst.getInput().get(pos).getEntity();

		String t = POS.get(eId).getForm();
		
		if(basicFeatures){
			/**Simple word features**/
			featureList.add(this._param_g.toFeature(network, FEATYPE.word.name(), 	t,  w));
			featureList.add(this._param_g.toFeature(network, FEATYPE.word_l.name(), t,  lw));
			featureList.add(this._param_g.toFeature(network, FEATYPE.word_ll.name(),t,  llw));
			featureList.add(this._param_g.toFeature(network, FEATYPE.word_r.name(), t,  rw));
			featureList.add(this._param_g.toFeature(network, FEATYPE.word_rr.name(),t,  rrw));
			
			/**Simple shape features**/
			featureList.add(this._param_g.toFeature(network, FEATYPE.cap.name(), 	t,  caps));
			featureList.add(this._param_g.toFeature(network, FEATYPE.cap_l.name(), 	t,  lcaps));
			featureList.add(this._param_g.toFeature(network, FEATYPE.cap_ll.name(), t,  llcaps));
			featureList.add(this._param_g.toFeature(network, FEATYPE.cap_r.name(), 	t,  rcaps));
			featureList.add(this._param_g.toFeature(network, FEATYPE.cap_rr.name(),	t,  rrcaps));
		}
		
		/**Cascade approach using the predicted chunk from the first Model (CRF)**/
		if(cascade){
			featureList.add(this._param_g.toFeature(network, FEATYPE.chunk.name(), 	  t,  chunk));
			featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_l.name(),  t,  lchunk));
			featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_ll.name(), t,  llchunk));
			featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_r.name(),  t,  rchunk));
			featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_rr.name(), t,  rrchunk));
		}
		
		if(NetworkConfig.USE_NEURAL_FEATURES){
			if(windowSize==1)
				featureList.add(this._param_g.toFeature(network,FEATYPE.neural_1.name(), t,  w.toLowerCase()+OUT_SEP+caps));
			else if(windowSize==3)
				featureList.add(this._param_g.toFeature(network,FEATYPE.neural_1.name(), t,  lw.toLowerCase()+IN_SEP+w.toLowerCase()
																							+IN_SEP+rw.toLowerCase()+OUT_SEP+
																							lcaps+IN_SEP+caps+IN_SEP+rcaps));
			else if(windowSize==5)
				featureList.add(this._param_g.toFeature(network,FEATYPE.neural_1.name(), t,  llw.toLowerCase()+IN_SEP+
																							lw.toLowerCase()+IN_SEP+w.toLowerCase()
																							+IN_SEP+rw.toLowerCase()+IN_SEP+
																							rrw.toLowerCase()+OUT_SEP+
																							llcaps+IN_SEP+lcaps+IN_SEP+caps+IN_SEP+rcaps+IN_SEP+rrcaps));
			else throw new RuntimeException("Unknown window size: "+windowSize);
		}
		
		
		String lt = POS.get(childEId).getForm();
		featureList.add(this._param_g.toFeature(network,FEATYPE.transition.name(), t,  lt));
		
		
		
		ArrayList<Integer> finalList = new ArrayList<Integer>();
		for(int i=0;i<featureList.size();i++){
			if(featureList.get(i)!=-1)
				finalList.add(featureList.get(i));
		}
		int[] features = new int[finalList.size()];
		for(int i=0;i<finalList.size();i++) features[i] = finalList.get(i);
		if(features.length==0) return FeatureArray.EMPTY;
		fa = new FeatureArray(features);
		
		return fa;
	}
	
	private String capsF(String word){
		String cap = null;
		if(word.equals("<PAD>")) return "others";
		if(word.equals(word.toLowerCase())) cap = "all_lowercases";
		else if(word.equals(word.toUpperCase())) cap = "all_uppercases";
		else if(word.matches("[A-Z][a-z0-9]*")) cap = "first_upper";
		else if(word.matches("[a-z0-9]+[A-Z]+.*")) cap = "at_least_one";
		else cap = "others";
		return cap;
	}

}

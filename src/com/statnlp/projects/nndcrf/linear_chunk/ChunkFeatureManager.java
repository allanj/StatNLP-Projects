package com.statnlp.projects.nndcrf.linear_chunk;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.neural.NeuralConfig;

public class ChunkFeatureManager extends FeatureManager {

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
		chunk, neural_1};
	private String OUT_SEP = NeuralConfig.OUT_SEP; 
	private String IN_SEP = NeuralConfig.IN_SEP;
	
	public ChunkFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}
	
	
	
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		
		//the tag in the sentence is actually the caps id.
		
		ChunkInstance inst = ((ChunkInstance)network.getInstance());
		//int instanceId = inst.getInstanceId();
		Sentence sent = inst.getInput();
		long node = network.getNode(parent_k);
		int[] nodeArr = NetworkIDMapper.toHybridNodeArray(node);
		
		FeatureArray fa = null;
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		
		int pos = nodeArr[0]-1;
		int eId = nodeArr[1];
		if(pos<0 || pos >= inst.size() || eId==Chunk.ChunkLabels.size())
			return FeatureArray.EMPTY;
			
//		System.err.println(Arrays.toString(nodeArr) + Entity.get(eId).toString());
		int[] child = NetworkIDMapper.toHybridNodeArray(network.getNode(children_k[0]));
		int childEId = child[1];
//		int childPos = child[0]-1;
		
		String lw = pos>0? sent.get(pos-1).getName():"<PAD>";
		String lcaps = capsF(lw);
		String llw = pos==0? "<PAD>": pos==1? "<PAD>":sent.get(pos-2).getName();
		String llcaps = capsF(llw);
//		String llt = pos==0? "<PAD>": pos==1? "<PAD>":sent.get(pos-2).getTag();
//		String lt = pos>0? sent.get(pos-1).getTag():"<PAD>";
		String rw = pos<sent.length()-1? sent.get(pos+1).getName():"<PAD>";
		String rcaps = capsF(rw);
//		String rt = pos<sent.length()-1? sent.get(pos+1).getTag():"<PAD>";
		String rrw = pos==sent.length()-1? "<PAD>": pos==sent.length()-2? "<PAD>":sent.get(pos+2).getName();
		String rrcaps = capsF(rrw);
//		String rrt = pos==sent.length()-1? "<PAD>": pos==sent.length()-2? "<PAD>":sent.get(pos+2).getTag();
		
		String currWord = inst.getInput().get(pos).getName();
		String currCaps = capsF(currWord);
//		String currTag = inst.getInput().get(pos).getTag();
//		String childWord = childPos>=0? inst.getInput().get(childPos).getName():"STR";
//		String childTag = childPos>=0? inst.getInput().get(childPos).getTag():"STR";
		
		
		
		
		String currEn = Chunk.get(eId).getForm();
		
		
		/**Simple word features**/
		featureList.add(this._param_g.toFeature(network, FEATYPE.word.name(), 	currEn,  currWord));
		featureList.add(this._param_g.toFeature(network, FEATYPE.word_l.name(), currEn,  lw));
		featureList.add(this._param_g.toFeature(network, FEATYPE.word_ll.name(),currEn,  llw));
		featureList.add(this._param_g.toFeature(network, FEATYPE.word_r.name(), currEn,  rw));
		featureList.add(this._param_g.toFeature(network, FEATYPE.word_rr.name(),currEn,  rrw));
		
		/**Simple shape features**/
		featureList.add(this._param_g.toFeature(network, FEATYPE.cap.name(), 	currEn,  currCaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.cap_l.name(), 	currEn,  lcaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.cap_ll.name(), currEn,  llcaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.cap_r.name(), 	currEn,  rcaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.cap_rr.name(),	currEn,  rrcaps));
		
		
		/** Neural features if neural network is enabled**/
		if(NetworkConfig.USE_NEURAL_FEATURES){
//			featureList.add(this._param_g.toFeature(network,FEATYPE.neural_1.name(), currEn,  currWord));
			featureList.add(this._param_g.toFeature(network, FEATYPE.neural_1.name(), currEn, llw.toLowerCase()+IN_SEP+
																					lw.toLowerCase()+IN_SEP+
																					currWord.toLowerCase()+IN_SEP+
																					rw.toLowerCase()+IN_SEP+
																					rrw.toLowerCase()+OUT_SEP+
																					llcaps+IN_SEP+lcaps+IN_SEP+currCaps+IN_SEP+rcaps+IN_SEP+rrcaps));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.neural_1.name(), currEn, llw.toLowerCase()+IN_SEP+lw.toLowerCase()
//								+IN_SEP+currWord.toLowerCase()+IN_SEP+rw.toLowerCase()+IN_SEP+rrw.toLowerCase()));
		}
		
		
		
		
		/** transition feature. from the JMLR paper**/
		String prevEntity = Chunk.get(childEId).getForm();
		featureList.add(this._param_g.toFeature(network,FEATYPE.chunk.name(), currEn,  prevEntity));
					

		
		
		
		
		
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

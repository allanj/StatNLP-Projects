package com.statnlp.projects.nndcrf.exactFCRF;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureBox;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class ExactFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 376931974939202432L;

	private String UNK = "unk";
	
	private int windowSize;
	public enum FEATYPE {
		chunk_currWord,
		chunk_leftWord1,
		chunk_leftWord2,
		chunk_rightWord1,
		chunk_rightWord2,
		chunk_cap, 
		chunk_cap_l, 
		chunk_cap_ll, 
		chunk_cap_r, 
		chunk_cap_rr, 
		chunk_transition,
		tag_currWord,
		tag_leftWord1,
		tag_leftWord2,
		tag_rightWord1,
		tag_rightWord2,
		tag_cap, 
		tag_cap_l, 
		tag_cap_ll, 
		tag_cap_r, 
		tag_cap_rr, 
		tag_transition
		};
	
		
	public ExactFeatureManager(GlobalNetworkParam param_g, int windowSize) {
		super(param_g);
		this.windowSize = windowSize;
	}

	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		// TODO Auto-generated method stub
		ExactInstance inst = ((ExactInstance)network.getInstance());
		//int instanceId = inst.getInstanceId();
		Sentence sent = inst.getInput();
		long node = network.getNode(parent_k);
		int[] nodeArr = NetworkIDMapper.toHybridNodeArray(node);
		FeatureArray fa = null;
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		
		int pos = nodeArr[0]-1;
		int labelId = nodeArr[1];
		if(pos < 0 || pos > inst.size())
			return FeatureArray.EMPTY;
		if(pos == inst.size() || labelId == (ExactLabel.Labels.size())) return FeatureArray.EMPTY;
		
//		if (pos!=inst.size() && labelId == (ExactLabel.Labels.size()) ) return FeatureArray.EMPTY;
//		if (pos==inst.size() && labelId != (ExactLabel.Labels.size()) ) return FeatureArray.EMPTY;
		String label = ExactLabel.Labels_Index.get(labelId).getForm();
		String[] vals = label.split(ExactConfig.EXACT_SEP);
		
		int[] childArr = network.getNodeArray(children_k[0]);
		String childLabel = pos == 0 ? "O" + ExactConfig.EXACT_SEP + "STR" :  ExactLabel.Labels_Index.get(childArr[1]).getForm();
		String[] childVals = childLabel.split(ExactConfig.EXACT_SEP);
		addChunkFeatures(featureList, network, sent, pos, vals[0], childVals[0]);
		addPOSFeatures(featureList, network, sent, pos, vals[1], childVals[1]);
		addJointFeatures(featureList, network, sent, pos, label, childLabel);
		
		
		
		ArrayList<Integer> finalList = new ArrayList<Integer>();
		for (int i = 0; i < featureList.size(); i++) {
			if (featureList.get(i) != -1)
				finalList.add(featureList.get(i));
		}
		int[] features = new int[finalList.size()];
		for (int i = 0; i < finalList.size(); i++)
			features[i] = finalList.get(i);

		
		fa = new FeatureArray(FeatureBox.getFeatureBox(features, this.getParams_L()[network.getThreadId()]));
		return fa;
	}
	
	private void addChunkFeatures(ArrayList<Integer> featureList,Network network, Sentence sent, int pos, String  currEn, String childEn){
		String lw = pos>0? sent.get(pos-1).getName():UNK;
		String lcaps = capsF(lw);
		String llw = pos==0? UNK: pos==1? "unk":sent.get(pos-2).getName();
		String llcaps = capsF(llw);
		String rw = pos<sent.length()-1? sent.get(pos+1).getName():UNK;
		String rcaps = capsF(rw);
		String rrw = pos==sent.length()-1? UNK: pos==sent.length()-2? UNK:sent.get(pos+2).getName();
		String rrcaps = capsF(rrw);
		String currWord = sent.get(pos).getName();
		String currCaps = capsF(currWord);
		
		featureList.add(this._param_g.toFeature(network,FEATYPE.chunk_currWord.name(), 		currEn,	currWord));
		featureList.add(this._param_g.toFeature(network,FEATYPE.chunk_leftWord1.name(), 	currEn,	lw));
		featureList.add(this._param_g.toFeature(network,FEATYPE.chunk_leftWord2.name(), 	currEn,	llw));
		featureList.add(this._param_g.toFeature(network,FEATYPE.chunk_rightWord1.name(), 	currEn,	rw));
		featureList.add(this._param_g.toFeature(network,FEATYPE.chunk_rightWord2.name(), 	currEn,	rrw));
		
		featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_cap.name(), 		currEn,  currCaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_cap_l.name(), 	currEn,  lcaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_cap_ll.name(), 	currEn,  llcaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_cap_r.name(), 	currEn,  rcaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_cap_rr.name(),	currEn,  rrcaps));
		
		featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_transition.name(),	currEn,  childEn));
		
//			String llt = pos==0? "<PAD>": pos==1? "<PAD>":sent.get(pos-2).getTag();
//			String lt = pos>0? sent.get(pos-1).getTag():"<PAD>";
//			String rt = pos<sent.length()-1? sent.get(pos+1).getTag():"<PAD>";
//			String rrt = pos==sent.length()-1? "<PAD>": pos==sent.length()-2? "<PAD>":sent.get(pos+2).getTag();
//			String currTag = sent.get(pos).getTag();
//			featureList.add(this._param_g.toFeature(network, FEATYPE.tag_currWord.name(), 	currEn,  currTag));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.tag_leftWord1.name(),  currEn,  lt));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.tag_leftWord2.name(),  currEn,  llt));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.tag_rightWord1.name(), currEn,  rt));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.tag_rightWord2.name(), currEn,  rrt));
		
//		if(NetworkConfig.USE_NEURAL_FEATURES){
//			if(windowSize == 5)
//				featureList.add(this._param_g.toFeature(network, FEATYPE.neural_1.name(), currEn, llw.toLowerCase()+IN_SEP+
//																						lw.toLowerCase()+IN_SEP+
//																						currWord.toLowerCase()+IN_SEP+
//																						rw.toLowerCase()+IN_SEP+
//																						rrw.toLowerCase()));
//			else if(windowSize == 3)
//				featureList.add(this._param_g.toFeature(network, FEATYPE.neural_1.name(), currEn, lw.toLowerCase()+IN_SEP+
//						currWord.toLowerCase()+IN_SEP+
//						rw.toLowerCase()));
//			else if(windowSize == 1)
//				featureList.add(this._param_g.toFeature(network, FEATYPE.neural_1.name(), currEn, currWord.toLowerCase()));
//			else throw new RuntimeException("Unknown window size: "+windowSize);
//		}
	}

	private void addPOSFeatures(ArrayList<Integer> featureList, Network network, Sentence sent, int pos, String currTag, String prevTag){
		String lw = pos>0? sent.get(pos-1).getName():"<PAD>";
		String llw = pos==0? "<PAD>": pos==1? "<PAD>":sent.get(pos-2).getName();
		String rw = pos<sent.length()-1? sent.get(pos+1).getName():"<PAD>";
		String rrw = pos==sent.length()? "<PAD>":pos==sent.length()-1? "<PAD>": pos==sent.length()-2? "<PAD>":sent.get(pos+2).getName();
		String w = pos==sent.length()? "<PAD>":sent.get(pos).getName();
		
		String caps = capsF(w);
		String lcaps = capsF(lw);
		String llcaps = capsF(llw);
		String rcaps = capsF(rw);
		String rrcaps = capsF(rrw);
		
		
		featureList.add(this._param_g.toFeature(network,FEATYPE.tag_currWord.name(), 	currTag,	w));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tag_leftWord1.name(), 	currTag,	lw));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tag_leftWord2.name(), 	currTag,	llw));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tag_rightWord1.name(), 	currTag,	rw));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tag_rightWord2.name(), 	currTag,	rrw));
		
		featureList.add(this._param_g.toFeature(network, FEATYPE.tag_cap.name(), 	currTag,  caps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.tag_cap_l.name(), 	currTag,  lcaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.tag_cap_ll.name(), currTag,  llcaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.tag_cap_r.name(), 	currTag,  rcaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.tag_cap_rr.name(),	currTag,  rrcaps));
		
		featureList.add(this._param_g.toFeature(network, FEATYPE.tag_transition.name(),	currTag,  prevTag));
		
//			String lchunk = pos>0? sent.get(pos-1).getEntity():"<PAD>";
//			String llchunk = pos==0? "<PAD>": pos==1? "<PAD>":sent.get(pos-2).getEntity();
//			String rchunk = pos<sent.length()-1? sent.get(pos+1).getEntity():"<PAD>";
//			String rrchunk = pos==sent.length()? "<PAD>":pos==sent.length()-1? "<PAD>": pos==sent.length()-2? "<PAD>":sent.get(pos+2).getEntity();
//			String chunk = pos==sent.length()? "<PAD>":sent.get(pos).getEntity();
//			featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_currWord.name(),	currTag,  chunk));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_leftWord1.name(),   currTag,  lchunk));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_leftWord2.name(), 	currTag,  llchunk));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_rightWord1.name(),  currTag,  rchunk));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_rightWord2.name(), 	currTag,  rrchunk));
		
		
//		if(NetworkConfig.USE_NEURAL_FEATURES){
//			if(windowSize==1)
//				featureList.add(this._param_g.toFeature(network,FEATYPE.neural_1.name(), currTag,  w.toLowerCase()));
//			else if(windowSize==3)
//				featureList.add(this._param_g.toFeature(network,FEATYPE.neural_1.name(), currTag,  lw.toLowerCase()+IN_SEP+w.toLowerCase()
//																							+IN_SEP+rw.toLowerCase()));
//			else if(windowSize==5)
//				featureList.add(this._param_g.toFeature(network,FEATYPE.neural_1.name(), currTag,  llw.toLowerCase()+IN_SEP+
//																							lw.toLowerCase()+IN_SEP+w.toLowerCase()
//																							+IN_SEP+rw.toLowerCase()+IN_SEP+
//																							rrw.toLowerCase()));
//			else throw new RuntimeException("Unknown window size: "+windowSize);
//		}
	}
	
	private void addJointFeatures(ArrayList<Integer> featureList,Network network, Sentence sent, int pos, String  label, String prevLabel){
		String lw = pos>0? sent.get(pos-1).getName():UNK;
		String lcaps = capsF(lw);
		String llw = pos==0? UNK: pos==1? "unk":sent.get(pos-2).getName();
		String llcaps = capsF(llw);
		String rw = pos<sent.length()-1? sent.get(pos+1).getName():UNK;
		String rcaps = capsF(rw);
		String rrw = pos==sent.length()-1? UNK: pos==sent.length()-2? UNK:sent.get(pos+2).getName();
		String rrcaps = capsF(rrw);
		String currWord = sent.get(pos).getName();
		String currCaps = capsF(currWord);
		
		featureList.add(this._param_g.toFeature(network,FEATYPE.joint_currWord.name(), 		label,	currWord));
		featureList.add(this._param_g.toFeature(network,FEATYPE.joint_leftWord1.name(), 	label,	lw));
		featureList.add(this._param_g.toFeature(network,FEATYPE.joint_leftWord2.name(), 	label,	llw));
		featureList.add(this._param_g.toFeature(network,FEATYPE.joint_rightWord1.name(), 	label,	rw));
		featureList.add(this._param_g.toFeature(network,FEATYPE.joint_rightWord2.name(), 	label,	rrw));
		
		featureList.add(this._param_g.toFeature(network, FEATYPE.joint_cap.name(), 		label,  currCaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.joint_cap_l.name(), 	label,  lcaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.joint_cap_ll.name(), 	label,  llcaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.joint_cap_r.name(), 	label,  rcaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.joint_cap_rr.name(),	label,  rrcaps));
		
		featureList.add(this._param_g.toFeature(network, FEATYPE.joint_transition.name(),	currEn,  childEn));
		
//			String llt = pos==0? "<PAD>": pos==1? "<PAD>":sent.get(pos-2).getTag();
//			String lt = pos>0? sent.get(pos-1).getTag():"<PAD>";
//			String rt = pos<sent.length()-1? sent.get(pos+1).getTag():"<PAD>";
//			String rrt = pos==sent.length()-1? "<PAD>": pos==sent.length()-2? "<PAD>":sent.get(pos+2).getTag();
//			String currTag = sent.get(pos).getTag();
//			featureList.add(this._param_g.toFeature(network, FEATYPE.tag_currWord.name(), 	currEn,  currTag));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.tag_leftWord1.name(),  currEn,  lt));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.tag_leftWord2.name(),  currEn,  llt));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.tag_rightWord1.name(), currEn,  rt));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.tag_rightWord2.name(), currEn,  rrt));
		
//		if(NetworkConfig.USE_NEURAL_FEATURES){
//			if(windowSize == 5)
//				featureList.add(this._param_g.toFeature(network, FEATYPE.neural_1.name(), currEn, llw.toLowerCase()+IN_SEP+
//																						lw.toLowerCase()+IN_SEP+
//																						currWord.toLowerCase()+IN_SEP+
//																						rw.toLowerCase()+IN_SEP+
//																						rrw.toLowerCase()));
//			else if(windowSize == 3)
//				featureList.add(this._param_g.toFeature(network, FEATYPE.neural_1.name(), currEn, lw.toLowerCase()+IN_SEP+
//						currWord.toLowerCase()+IN_SEP+
//						rw.toLowerCase()));
//			else if(windowSize == 1)
//				featureList.add(this._param_g.toFeature(network, FEATYPE.neural_1.name(), currEn, currWord.toLowerCase()));
//			else throw new RuntimeException("Unknown window size: "+windowSize);
//		}
	}
	
	
	private String capsF(String word){
		String cap = null;
		if(word.equals(UNK)) return "others";
		if(word.equals(word.toLowerCase())) cap = "all_lowercases";
		else if(word.equals(word.toUpperCase())) cap = "all_uppercases";
		else if(word.matches("[A-Z][a-z0-9]*")) cap = "first_upper";
		else if(word.matches("[a-z0-9]+[A-Z]+.*")) cap = "at_least_one";
		else cap = "others";
		return cap;
	}
	
}

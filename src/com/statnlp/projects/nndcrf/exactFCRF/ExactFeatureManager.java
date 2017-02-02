package com.statnlp.projects.nndcrf.exactFCRF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureBox;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.neural.NeuralConfig;

public class ExactFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 376931974939202432L;

	private String IN_SEP = NeuralConfig.IN_SEP;
	private static HashSet<String> others = new HashSet<>(Arrays.asList("#STR#", "#END#", "#STR1#", "#END1#", "#str#", "#end#", "#str1#", "#end1#"));
	
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
		tag_transition, 
		joint_currWord,
		joint_leftWord1,
		joint_leftWord2,
		joint_rightWord1,
		joint_rightWord2,
		joint_cap, 
		joint_cap_l, 
		joint_cap_ll, 
		joint_cap_r, 
		joint_cap_rr,
		neural_1
		};
	
		
	public ExactFeatureManager(GlobalNetworkParam param_g, int windowSize) {
		super(param_g);
		this.windowSize = windowSize;
	}

	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		ExactInstance inst = ((ExactInstance)network.getInstance());
		//int instanceId = inst.getInstanceId();
		Sentence sent = inst.getInput();
		long node = network.getNode(parent_k);
		int[] nodeArr = NetworkIDMapper.toHybridNodeArray(node);
		FeatureArray fa = null;
		int threadId = network.getThreadId();
		
		int pos = nodeArr[0]-1;
		int labelId = nodeArr[1];
		if(pos < 0 || pos > inst.size())
			return FeatureArray.EMPTY;
		if(pos == inst.size() || labelId == (ExactLabel.Labels.size())) return FeatureArray.EMPTY;
		
		String label = ExactLabel.Labels_Index.get(labelId).getForm();
		String[] vals = label.split(ExactConfig.EXACT_SEP);
		
		int[] childArr = network.getNodeArray(children_k[0]);
		String childLabel = pos == 0 ? "O" + ExactConfig.EXACT_SEP + "STR" :  ExactLabel.Labels_Index.get(childArr[1]).getForm();
		String[] childVals = childLabel.split(ExactConfig.EXACT_SEP);
		
		ArrayList<Integer> c_wordList = new ArrayList<Integer>();
		ArrayList<Integer> c_capList = new ArrayList<Integer>();
		ArrayList<Integer> c_transitionList = new ArrayList<Integer>();
		ArrayList<Integer> c_neuralList = new ArrayList<Integer>();
		ArrayList<Integer> t_wordList = new ArrayList<Integer>();
		ArrayList<Integer> t_capList = new ArrayList<Integer>();
		ArrayList<Integer> t_transitionList = new ArrayList<Integer>();
		ArrayList<Integer> t_neuralList = new ArrayList<Integer>();
		
		ArrayList<Integer> j_wordList = new ArrayList<Integer>();
		ArrayList<Integer> j_neuralList = new ArrayList<Integer>();
		ArrayList<ArrayList<Integer>> bigList = new ArrayList<ArrayList<Integer>>();
		
		addChunkFeatures(network, sent, pos, vals[0], childVals[0], c_wordList, c_capList, c_transitionList, c_neuralList);
		addPOSFeatures(network, sent, pos, vals[1], childVals[1], t_wordList, t_capList, t_transitionList, t_neuralList);
		addJointFeatures(network, sent, pos, label, childLabel, j_wordList, j_neuralList);
		
		bigList.add(c_wordList); bigList.add(c_capList);
		bigList.add(c_transitionList); bigList.add(c_neuralList);
		bigList.add(t_wordList); bigList.add(t_capList);
		bigList.add(t_transitionList); bigList.add(t_neuralList);
		
		bigList.add(j_wordList);
		bigList.add(j_neuralList); 
		
		FeatureArray orgFa = new FeatureArray(FeatureBox.getFeatureBox(new int[]{}, this.getParams_L()[threadId]));
		fa = orgFa;
		for (int i = 0; i < bigList.size(); i++) {
			FeatureArray curr = addNext(fa, bigList.get(i), threadId);
			fa = curr;
		}
		return orgFa;
	}
	
	private FeatureArray addNext(FeatureArray fa, ArrayList<Integer> featureList, int threadId)  {
		ArrayList<Integer> finalList = new ArrayList<Integer>();
		for(int i=0;i<featureList.size();i++){
			if(featureList.get(i)!=-1)
				finalList.add( featureList.get(i) );
		}
		if(finalList.size()==0) return fa;
		else {
			int[] fs = new int[finalList.size()];
			for(int i = 0; i < fs.length; i++) fs[i] = finalList.get(i);
			FeatureArray curr = new FeatureArray(FeatureBox.getFeatureBox(fs, this.getParams_L()[threadId]));
			fa.next(curr);
			return curr;
		}
	}
	
	private void addChunkFeatures(Network network, Sentence sent, int pos, String currEn, String prevEn,
			ArrayList<Integer> wordList, ArrayList<Integer> capList, ArrayList<Integer> transitionList, ArrayList<Integer> neuralList){
		
		String lw = pos > 0? sent.get(pos-1).getName(): "#STR#";
		String lcaps = capsF(lw);
		String llw = pos == 0? "#STR1#": pos==1? "#STR#" : sent.get(pos-2).getName();
		String llcaps = capsF(llw);
		String rw = pos<sent.length()-1? sent.get(pos+1).getName():"#END#";
		String rcaps = capsF(rw);
		String rrw = pos == sent.length()-1? "#END1#": pos==sent.length()-2? "#END#":sent.get(pos+2).getName();
		String rrcaps = capsF(rrw);
		String currWord = sent.get(pos).getName();
		String currCaps = capsF(currWord);
		
		wordList.add(this._param_g.toFeature(network,FEATYPE.chunk_currWord.name(), 	currEn,	currWord.toLowerCase()));
		wordList.add(this._param_g.toFeature(network,FEATYPE.chunk_leftWord1.name(), 	currEn,	lw.toLowerCase()));
		wordList.add(this._param_g.toFeature(network,FEATYPE.chunk_leftWord2.name(), 	currEn,	llw.toLowerCase()));
		wordList.add(this._param_g.toFeature(network,FEATYPE.chunk_rightWord1.name(), 	currEn,	rw.toLowerCase()));
		wordList.add(this._param_g.toFeature(network,FEATYPE.chunk_rightWord2.name(), 	currEn,	rrw.toLowerCase()));
		
		capList.add(this._param_g.toFeature(network, FEATYPE.chunk_cap.name(), 		currEn,  currCaps));
		capList.add(this._param_g.toFeature(network, FEATYPE.chunk_cap_l.name(), 	currEn,  lcaps));
		capList.add(this._param_g.toFeature(network, FEATYPE.chunk_cap_ll.name(), 	currEn,  llcaps));
		capList.add(this._param_g.toFeature(network, FEATYPE.chunk_cap_r.name(), 	currEn,  rcaps));
		capList.add(this._param_g.toFeature(network, FEATYPE.chunk_cap_rr.name(),	currEn,  rrcaps));
		
		transitionList.add(this._param_g.toFeature(network, FEATYPE.chunk_transition.name(),	currEn,  prevEn));
		if(NetworkConfig.USE_NEURAL_FEATURES){
			if(windowSize == 5)
				neuralList.add(this._param_g.toFeature(network, FEATYPE.neural_1.name(), currEn, llw.toLowerCase()+IN_SEP+
																						lw.toLowerCase()+IN_SEP+
																						currWord.toLowerCase()+IN_SEP+
																						rw.toLowerCase()+IN_SEP+
																						rrw.toLowerCase()));
			else if(windowSize == 3)
				neuralList.add(this._param_g.toFeature(network, FEATYPE.neural_1.name(), currEn, lw.toLowerCase()+IN_SEP+
						currWord.toLowerCase()+IN_SEP+
						rw.toLowerCase()));
			else if(windowSize == 1)
				neuralList.add(this._param_g.toFeature(network, FEATYPE.neural_1.name(), currEn, currWord.toLowerCase()));
			else throw new RuntimeException("Unknown window size: "+windowSize);
		}
	}

	private void addPOSFeatures(Network network, Sentence sent, int pos, String currTag, String prevTag,
			ArrayList<Integer> wordList, ArrayList<Integer> capList, ArrayList<Integer> transitionList, ArrayList<Integer> neuralList){
		String lw = pos > 0? sent.get(pos-1).getName():"#STR#";
		String llw = pos==0? "#STR1#": pos==1? "#STR#":sent.get(pos-2).getName();
		String rw = pos<sent.length()-1? sent.get(pos+1).getName():"#END#";
		String rrw = pos==sent.length()-1? "#END1#": pos==sent.length()-2? "#END#":sent.get(pos+2).getName();
		String w = sent.get(pos).getName();
		
		String caps = capsF(w);
		String lcaps = capsF(lw);
		String llcaps = capsF(llw);
		String rcaps = capsF(rw);
		String rrcaps = capsF(rrw);
		
		
		wordList.add(this._param_g.toFeature(network,FEATYPE.tag_currWord.name(), 	currTag,	w.toLowerCase()));
		wordList.add(this._param_g.toFeature(network,FEATYPE.tag_leftWord1.name(), 	currTag,	lw.toLowerCase()));
		wordList.add(this._param_g.toFeature(network,FEATYPE.tag_leftWord2.name(), 	currTag,	llw.toLowerCase()));
		wordList.add(this._param_g.toFeature(network,FEATYPE.tag_rightWord1.name(), 	currTag,	rw.toLowerCase()));
		wordList.add(this._param_g.toFeature(network,FEATYPE.tag_rightWord2.name(), 	currTag,	rrw.toLowerCase()));
		
		capList.add(this._param_g.toFeature(network, FEATYPE.tag_cap.name(), 	currTag,  caps));
		capList.add(this._param_g.toFeature(network, FEATYPE.tag_cap_l.name(), 	currTag,  lcaps));
		capList.add(this._param_g.toFeature(network, FEATYPE.tag_cap_ll.name(), currTag,  llcaps));
		capList.add(this._param_g.toFeature(network, FEATYPE.tag_cap_r.name(), 	currTag,  rcaps));
		capList.add(this._param_g.toFeature(network, FEATYPE.tag_cap_rr.name(),	currTag,  rrcaps));
		
		transitionList.add(this._param_g.toFeature(network, FEATYPE.tag_transition.name(),	currTag,  prevTag));
		if(NetworkConfig.USE_NEURAL_FEATURES){
			if(windowSize==1)
				neuralList.add(this._param_g.toFeature(network,FEATYPE.neural_1.name(), currTag,  w.toLowerCase()));
			else if(windowSize==3)
				neuralList.add(this._param_g.toFeature(network,FEATYPE.neural_1.name(), currTag,  lw.toLowerCase()+IN_SEP+w.toLowerCase()
																							+IN_SEP+rw.toLowerCase()));
			else if(windowSize==5)
				neuralList.add(this._param_g.toFeature(network,FEATYPE.neural_1.name(), currTag,  llw.toLowerCase()+IN_SEP+
																							lw.toLowerCase()+IN_SEP+w.toLowerCase()
																							+IN_SEP+rw.toLowerCase()+IN_SEP+
																							rrw.toLowerCase()));
			else throw new RuntimeException("Unknown window size: "+windowSize);
		}
	}
	
	private void addJointFeatures(Network network, Sentence sent, int pos, String label, String prevLabel,
			ArrayList<Integer> wordList, ArrayList<Integer> neuralList){
		String lw = pos > 0? sent.get(pos-1).getName(): "#STR#";
		String llw = pos == 0? "#STR1#": pos==1? "#STR#" : sent.get(pos-2).getName();
		String rw = pos<sent.length()-1? sent.get(pos+1).getName():"#END#";
		String rrw = pos == sent.length()-1? "#END1#": pos==sent.length()-2? "#END#":sent.get(pos+2).getName();
		String currWord = sent.get(pos).getName();
		
		if(windowSize >= 1) {
			wordList.add(this._param_g.toFeature(network,FEATYPE.joint_currWord.name(), label,	currWord));
		}
		if (windowSize >= 3) {
			wordList.add(this._param_g.toFeature(network,FEATYPE.joint_leftWord1.name(), 	label,	lw));
			wordList.add(this._param_g.toFeature(network,FEATYPE.joint_rightWord1.name(), 	label,	rw));
		}
		if (windowSize >= 5) {
			wordList.add(this._param_g.toFeature(network,FEATYPE.joint_leftWord2.name(), 	label,	llw));
			wordList.add(this._param_g.toFeature(network,FEATYPE.joint_rightWord2.name(), 	label,	rrw));
		}
		
		if(NetworkConfig.USE_NEURAL_FEATURES){
			int njf = -1;
			if(windowSize==1) 
				njf = this._param_g.toFeature(network,FEATYPE.neural_1.name(), label,  currWord.toLowerCase());
			else if(windowSize==3)
				njf =  this._param_g.toFeature(network,FEATYPE.neural_1.name(), label,  lw.toLowerCase()+IN_SEP+currWord.toLowerCase()
																							+IN_SEP+rw.toLowerCase());
			else if(windowSize==5)
				njf = this._param_g.toFeature(network,FEATYPE.neural_1.name(), label,  llw.toLowerCase()+IN_SEP+
																							lw.toLowerCase()+IN_SEP+currWord.toLowerCase()
																							+IN_SEP+rw.toLowerCase()+IN_SEP+
																							rrw.toLowerCase());
			else throw new RuntimeException("Unknown window size: "+windowSize);
			if (njf != -1) {
				neuralList.add(njf);
			}
		}
	}
	
	
	private String capsF(String word){
		String cap = null;
		if(others.contains(word)) return "others";
		if(word.equals(word.toLowerCase())) cap = "all_lowercases";
		else if(word.equals(word.toUpperCase())) cap = "all_uppercases";
		else if(word.matches("[A-Z][a-z0-9]*")) cap = "first_upper";
		else if(word.matches("[a-z0-9]+[A-Z]+.*")) cap = "at_least_one";
		else cap = "others";
		return cap;
	}
	
}

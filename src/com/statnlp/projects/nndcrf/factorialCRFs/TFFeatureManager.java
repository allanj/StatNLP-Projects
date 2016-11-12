package com.statnlp.projects.nndcrf.factorialCRFs;

import java.util.ArrayList;
import java.util.Arrays;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.neural.NeuralConfig;
import com.statnlp.projects.nndcrf.factorialCRFs.TFConfig.TASK;
import com.statnlp.projects.nndcrf.factorialCRFs.TFNetworkCompiler.NODE_TYPES;

public class TFFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 376931974939202432L;

	private boolean useJointFeatures;
	private String OUT_SEP = NeuralConfig.OUT_SEP; 
	private String IN_SEP = NeuralConfig.IN_SEP;
	
	private int windowSize;
	private boolean cascade;
	private TASK task;
	public enum FEATYPE {
		entity_currWord,
		entity_leftWord1,
		entity_leftWord2,
		entity_rightWord1,
		entity_rightWord2,
		entity_cap, 
		entity_cap_l, 
		entity_cap_ll, 
		entity_cap_r, 
		entity_cap_rr, 
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
		joint,
		neural_1,
		neural_2
		};
	
		
	public TFFeatureManager(GlobalNetworkParam param_g, boolean useJointFeatures, boolean cascade, TASK task, int windowSize) {
		super(param_g);
		this.useJointFeatures = useJointFeatures; 
		this.cascade = cascade;
		this.task = task;
		this.windowSize = windowSize;
	}

	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		// TODO Auto-generated method stub
		TFInstance inst = ((TFInstance)network.getInstance());
		//int instanceId = inst.getInstanceId();
		Sentence sent = inst.getInput();
		long node = network.getNode(parent_k);
		int[] nodeArr = NetworkIDMapper.toHybridNodeArray(node);
		FeatureArray fa = null;
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		
		int pos = nodeArr[0]-1;
		int eId = nodeArr[2];
		if(pos<0 || pos > inst.size())
			return FeatureArray.EMPTY;
		
		
		
		if(nodeArr[1]==NODE_TYPES.ENODE.ordinal()){
			if(pos==inst.size() || eId==(Entity.ENTS.size()+Tag.TAGS.size())) return FeatureArray.EMPTY;
			addEntityFeatures(featureList, network, sent, pos, eId);
			//false: means it's NE structure
			if(useJointFeatures)
				addJointFeatures(featureList, network, sent, pos, eId, parent_k, children_k, false);
			
		}
		
		if(nodeArr[1]==NODE_TYPES.TNODE.ordinal()){
			if(pos!=inst.size() && eId==(Entity.ENTS.size()+Tag.TAGS.size())) return FeatureArray.EMPTY;
			addPOSFeatures(featureList, network, sent, pos, eId);
			if(useJointFeatures && pos != inst.size())
				addJointFeatures(featureList, network, sent, pos, eId, parent_k, children_k, true);
		}
		
		ArrayList<Integer> finalList = new ArrayList<Integer>();
		for(int i=0;i<featureList.size();i++){
			if(featureList.get(i)!=-1)
				finalList.add(featureList.get(i));
		}
		int[] features = new int[finalList.size()];
		for(int i=0;i<finalList.size();i++) features[i] = finalList.get(i);
		
		
		fa = new FeatureArray(features);
		return fa;
	}
	
	private void addEntityFeatures(ArrayList<Integer> featureList,Network network, Sentence sent, int pos, int eId){
		String lw = pos>0? sent.get(pos-1).getName():"<PAD>";
		String lcaps = capsF(lw);
		String llw = pos==0? "<PAD>": pos==1? "<PAD>":sent.get(pos-2).getName();
		String llcaps = capsF(llw);
		String rw = pos<sent.length()-1? sent.get(pos+1).getName():"<PAD>";
		String rcaps = capsF(rw);
		String rrw = pos==sent.length()-1? "<PAD>": pos==sent.length()-2? "<PAD>":sent.get(pos+2).getName();
		String rrcaps = capsF(rrw);
		String currWord = sent.get(pos).getName();
		String currEn = Entity.get(eId).getForm();
		String currCaps = capsF(currWord);
		
		featureList.add(this._param_g.toFeature(network,FEATYPE.entity_currWord.name(), 	currEn,	currWord));
		featureList.add(this._param_g.toFeature(network,FEATYPE.entity_leftWord1.name(), 	currEn,	lw));
		featureList.add(this._param_g.toFeature(network,FEATYPE.entity_leftWord2.name(), 	currEn,	llw));
		featureList.add(this._param_g.toFeature(network,FEATYPE.entity_rightWord1.name(), 	currEn,	rw));
		featureList.add(this._param_g.toFeature(network,FEATYPE.entity_rightWord2.name(), 	currEn,	rrw));
		
		featureList.add(this._param_g.toFeature(network, FEATYPE.entity_cap.name(), 	currEn,  currCaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.entity_cap_l.name(), 	currEn,  lcaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.entity_cap_ll.name(), 	currEn,  llcaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.entity_cap_r.name(), 	currEn,  rcaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.entity_cap_rr.name(),	currEn,  rrcaps));
		
		if(task == TASK.NER && cascade){
			String llt = pos==0? "<PAD>": pos==1? "<PAD>":sent.get(pos-2).getTag();
			String lt = pos>0? sent.get(pos-1).getTag():"<PAD>";
			String rt = pos<sent.length()-1? sent.get(pos+1).getTag():"<PAD>";
			String rrt = pos==sent.length()-1? "<PAD>": pos==sent.length()-2? "<PAD>":sent.get(pos+2).getTag();
			String currTag = sent.get(pos).getTag();
			featureList.add(this._param_g.toFeature(network, FEATYPE.tag_currWord.name(), 	currEn,  currTag));
			featureList.add(this._param_g.toFeature(network, FEATYPE.tag_leftWord1.name(),  currEn,  lt));
			featureList.add(this._param_g.toFeature(network, FEATYPE.tag_leftWord2.name(),  currEn,  llt));
			featureList.add(this._param_g.toFeature(network, FEATYPE.tag_rightWord1.name(), currEn,  rt));
			featureList.add(this._param_g.toFeature(network, FEATYPE.tag_rightWord2.name(), currEn,  rrt));
		}
		
		if(NetworkConfig.USE_NEURAL_FEATURES){
			if(windowSize == 5)
				featureList.add(this._param_g.toFeature(network, FEATYPE.neural_1.name(), currEn, llw.toLowerCase()+IN_SEP+
																						lw.toLowerCase()+IN_SEP+
																						currWord.toLowerCase()+IN_SEP+
																						rw.toLowerCase()+IN_SEP+
																						rrw.toLowerCase()+OUT_SEP+
																						llcaps+IN_SEP+lcaps+IN_SEP+currCaps+IN_SEP+rcaps+IN_SEP+rrcaps));
			else if(windowSize == 3)
				featureList.add(this._param_g.toFeature(network, FEATYPE.neural_1.name(), currEn, lw.toLowerCase()+IN_SEP+
						currWord.toLowerCase()+IN_SEP+
						rw.toLowerCase()+OUT_SEP+
						lcaps+IN_SEP+currCaps+IN_SEP+rcaps));
			else if(windowSize == 1)
				featureList.add(this._param_g.toFeature(network, FEATYPE.neural_1.name(), currEn, currWord.toLowerCase()+OUT_SEP+currCaps));
			
			else throw new RuntimeException("Unknown window size: "+windowSize);
		}
	}

	private void addPOSFeatures(ArrayList<Integer> featureList, Network network, Sentence sent, int pos, int tId){
		String currTag = Tag.get(tId).getForm();
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
		
		if(task == TASK.TAGGING && cascade){
			String lchunk = pos>0? sent.get(pos-1).getEntity():"<PAD>";
			String llchunk = pos==0? "<PAD>": pos==1? "<PAD>":sent.get(pos-2).getEntity();
			String rchunk = pos<sent.length()-1? sent.get(pos+1).getEntity():"<PAD>";
			String rrchunk = pos==sent.length()? "<PAD>":pos==sent.length()-1? "<PAD>": pos==sent.length()-2? "<PAD>":sent.get(pos+2).getEntity();
			String chunk = pos==sent.length()? "<PAD>":sent.get(pos).getEntity();
			featureList.add(this._param_g.toFeature(network, FEATYPE.entity_currWord.name(),	currTag,  chunk));
			featureList.add(this._param_g.toFeature(network, FEATYPE.entity_leftWord1.name(),   currTag,  lchunk));
			featureList.add(this._param_g.toFeature(network, FEATYPE.entity_leftWord2.name(), 	currTag,  llchunk));
			featureList.add(this._param_g.toFeature(network, FEATYPE.entity_rightWord1.name(),  currTag,  rchunk));
			featureList.add(this._param_g.toFeature(network, FEATYPE.entity_rightWord2.name(), 	currTag,  rrchunk));
		}
		
		
		if(NetworkConfig.USE_NEURAL_FEATURES){
			if(windowSize==1)
				featureList.add(this._param_g.toFeature(network,FEATYPE.neural_2.name(), currTag,  w.toLowerCase()+OUT_SEP+caps));
			else if(windowSize==3)
				featureList.add(this._param_g.toFeature(network,FEATYPE.neural_2.name(), currTag,  lw.toLowerCase()+IN_SEP+w.toLowerCase()
																							+IN_SEP+rw.toLowerCase()+OUT_SEP+
																							lcaps+IN_SEP+caps+IN_SEP+rcaps));
			else if(windowSize==5)
				featureList.add(this._param_g.toFeature(network,FEATYPE.neural_2.name(), currTag,  llw.toLowerCase()+IN_SEP+
																							lw.toLowerCase()+IN_SEP+w.toLowerCase()
																							+IN_SEP+rw.toLowerCase()+IN_SEP+
																							rrw.toLowerCase()+OUT_SEP+
																							llcaps+IN_SEP+lcaps+IN_SEP+caps+IN_SEP+rcaps+IN_SEP+rrcaps));
			else throw new RuntimeException("Unknown window size: "+windowSize);
		}
	}
	
	/**
	 * 
	 * @param featureList
	 * @param network
	 * @param sent
	 * @param pos
	 * @param paId
	 * @param parent_k
	 * @param children_k
	 * @param paTchildE: false means the current structure is NE structure.
	 */
	private void addJointFeatures(ArrayList<Integer> featureList, Network network, Sentence sent, int pos, int paId, int parent_k, int[] children_k, boolean paTchildE){
		//TFNetwork tfnetwork = (TFNetwork)network;
		if(children_k.length!=1)
			throw new RuntimeException("The joint features should only have one children also");
		String currLabel = paTchildE? Tag.get(paId).getForm():Entity.get(paId).getForm();
		int jointFeatureIdx = -1; 
		int[] arr = null;
		int nodeType = -1;
		if(!paTchildE){
			//current it's NE structure, need to refer to Tag node.
			nodeType = NODE_TYPES.TNODE.ordinal();
			for (int t = 0; t < Tag.TAGS_INDEX.size(); t++) {
				String tag = Tag.get(t).getForm();
				arr = new int[] { pos + 1, nodeType, t };
				long unlabeledDstNode = NetworkIDMapper.toHybridNodeID(arr);
				TFNetwork unlabeledNetwork = (TFNetwork) network.getUnlabeledNetwork();
				int unlabeledDstNodeIdx = Arrays.binarySearch(unlabeledNetwork.getAllNodes(), unlabeledDstNode);
				if (unlabeledDstNodeIdx >= 0) {
					jointFeatureIdx = this._param_g.toFeature(network, FEATYPE.joint.name(), currLabel + "&" + tag, "");
					//System.out.println("Joint Feature Index: " + jointFeatureIdx + " [" + currLabel + "&" + tag + "]");
					network.putJointFeature(parent_k, jointFeatureIdx, unlabeledDstNodeIdx);
					featureList.add(jointFeatureIdx);
				}
			}
			
		}else{
			//current it's POS structure, need to refer to Entity node
			nodeType = NODE_TYPES.ENODE.ordinal();
			for(int e=0; e<Entity.ENTS_INDEX.size(); e++){
				String entity = Entity.get(e).getForm();
				arr = new int[]{pos+1, nodeType, e};
				long unlabeledDstNode = NetworkIDMapper.toHybridNodeID(arr);
				TFNetwork unlabeledNetwork = (TFNetwork)network.getUnlabeledNetwork();
				int unlabeledDstNodeIdx = Arrays.binarySearch(unlabeledNetwork.getAllNodes(), unlabeledDstNode);
				if(unlabeledDstNodeIdx>=0){
					jointFeatureIdx = this._param_g.toFeature(network, FEATYPE.joint.name(), entity + "&" + currLabel, "");
					//System.out.println("Joint Feature Index: " + jointFeatureIdx + " [" + entity + "&" + currLabel + "]");
					featureList.add(jointFeatureIdx);
					network.putJointFeature(parent_k, jointFeatureIdx, unlabeledDstNodeIdx);
				}
			}
			
		}
			
		
	}
	
	
	private String capsF(String word){
		String cap = null;
		if(word.equals("<PAD>")||word.startsWith("STR")||word.startsWith("END")) return "others";
		if(word.equals(word.toLowerCase())) cap = "all_lowercases";
		else if(word.equals(word.toUpperCase())) cap = "all_uppercases";
		else if(word.matches("[A-Z][a-z0-9]*")) cap = "first_upper";
		else if(word.matches("[a-z0-9]+[A-Z]+.*")) cap = "at_least_one";
		else cap = "others";
		return cap;
	}
}

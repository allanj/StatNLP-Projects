package com.statnlp.projects.nndcrf.exactFCRF;

import java.util.ArrayList;
import java.util.Arrays;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureBox;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.neural.NeuralConfig;
import com.statnlp.projects.nndcrf.factorialCRFs.FCRFConfig.TASK;
import com.statnlp.projects.nndcrf.factorialCRFs.FCRFNetworkCompiler.NODE_TYPES;

public class ExactFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 376931974939202432L;

	private String OUT_SEP = NeuralConfig.OUT_SEP; 
	private String IN_SEP = NeuralConfig.IN_SEP;
	private String UNK = "unk";
	
	private int windowSize;
	private boolean cascade;
	private TASK task;
	private boolean iobes;
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
		joint1,
		joint2,
		joint3,
		joint4,
		joint5,
		neural_1
		};
	
		
	public ExactFeatureManager(GlobalNetworkParam param_g, int windowSize, boolean iobes) {
		super(param_g);
		this.windowSize = windowSize;
		this.iobes = iobes;
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
		ArrayList<Integer> jointFeatureList = new ArrayList<>();
		
		int pos = nodeArr[0]-1;
		int eId = nodeArr[2];
		if(pos<0 || pos > inst.size())
			return FeatureArray.EMPTY;
		
		
		if (nodeArr[1] == NODE_TYPES.ENODE.ordinal()){
			if(pos==inst.size() || eId==(ChunkLabel.CHUNKS.size()+TagLabel.TAGS.size())) return FeatureArray.EMPTY;
			addChunkFeatures(featureList, network, sent, pos, eId);
			//false: means it's NE structure
			if(useJointFeatures)
				addJointFeatures(jointFeatureList, network, sent, pos, eId, parent_k, children_k, false);
																
		} else if (nodeArr[1] == NODE_TYPES.TNODE.ordinal() ){//|| nodeArr[1] == NODE_TYPES.ROOT.ordinal()){
															//can uncomment this after debugging, we might need to end features for POS tagging	
			if (pos!=inst.size() && eId==(ChunkLabel.CHUNKS.size()+TagLabel.TAGS.size() )) return FeatureArray.EMPTY;
			if (pos==inst.size() && eId!=(ChunkLabel.CHUNKS.size()+TagLabel.TAGS.size() )) return FeatureArray.EMPTY;
			addPOSFeatures(featureList, network, sent, pos, eId);
			if(useJointFeatures && pos != inst.size())
				addJointFeatures(jointFeatureList, network, sent, pos, eId, parent_k, children_k, true);
		}
		
		ArrayList<Integer> finalList = new ArrayList<Integer>();
		for (int i = 0; i < featureList.size(); i++) {
			if (featureList.get(i) != -1)
				finalList.add(featureList.get(i));
		}
		int[] features = new int[finalList.size()];
		for (int i = 0; i < finalList.size(); i++)
			features[i] = finalList.get(i);

		ArrayList<Integer> joint = new ArrayList<>();
		for (int i = 0; i < jointFeatureList.size(); i++) {
			if (jointFeatureList.get(i) != -1)
				joint.add(jointFeatureList.get(i));
		}
		int[] jointFeaturesArr = new int[joint.size()];
		for (int i = 0; i < joint.size(); i++)
			jointFeaturesArr[i] = joint.get(i);
//		FeatureArray jointFa = new FeatureArray(jointFeaturesArr);
//		jointFa.setAlwaysChange();
//		
//		fa = new FeatureArray(features, jointFa);
		
		fa = new FeatureArray(FeatureBox.getFeatureBox(features, this.getParams_L()[network.getThreadId()]));
		FeatureArray jointFa =  new FeatureArray(FeatureBox.getFeatureBox(jointFeaturesArr, this.getParams_L()[network.getThreadId()]));
		jointFa.setAlwaysChange();
		fa.next(jointFa);
		
		return fa;
	}
	
	private void addChunkFeatures(ArrayList<Integer> featureList,Network network, Sentence sent, int pos, int eId){
		String lw = pos>0? sent.get(pos-1).getName():UNK;
		String lcaps = capsF(lw);
		String llw = pos==0? UNK: pos==1? "unk":sent.get(pos-2).getName();
		String llcaps = capsF(llw);
		String rw = pos<sent.length()-1? sent.get(pos+1).getName():UNK;
		String rcaps = capsF(rw);
		String rrw = pos==sent.length()-1? UNK: pos==sent.length()-2? UNK:sent.get(pos+2).getName();
		String rrcaps = capsF(rrw);
		String currWord = sent.get(pos).getName();
		String currEn = ChunkLabel.get(eId).getForm();
		String currCaps = capsF(currWord);
		
		featureList.add(this._param_g.toFeature(network,FEATYPE.chunk_currWord.name(), 		currEn,	currWord));
		featureList.add(this._param_g.toFeature(network,FEATYPE.chunk_leftWord1.name(), 	currEn,	lw));
		featureList.add(this._param_g.toFeature(network,FEATYPE.chunk_leftWord2.name(), 	currEn,	llw));
		featureList.add(this._param_g.toFeature(network,FEATYPE.chunk_rightWord1.name(), 	currEn,	rw));
		featureList.add(this._param_g.toFeature(network,FEATYPE.chunk_rightWord2.name(), 	currEn,	rrw));
//		
//		featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_cap.name(), 		currEn,  currCaps));
//		featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_cap_l.name(), 	currEn,  lcaps));
//		featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_cap_ll.name(), 	currEn,  llcaps));
//		featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_cap_r.name(), 	currEn,  rcaps));
//		featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_cap_rr.name(),	currEn,  rrcaps));
		
		if(task == TASK.CHUNKING && cascade){
//			String llt = pos==0? "<PAD>": pos==1? "<PAD>":sent.get(pos-2).getTag();
//			String lt = pos>0? sent.get(pos-1).getTag():"<PAD>";
//			String rt = pos<sent.length()-1? sent.get(pos+1).getTag():"<PAD>";
//			String rrt = pos==sent.length()-1? "<PAD>": pos==sent.length()-2? "<PAD>":sent.get(pos+2).getTag();
			String currTag = sent.get(pos).getTag();
			featureList.add(this._param_g.toFeature(network, FEATYPE.tag_currWord.name(), 	currEn,  currTag));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.tag_leftWord1.name(),  currEn,  lt));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.tag_leftWord2.name(),  currEn,  llt));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.tag_rightWord1.name(), currEn,  rt));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.tag_rightWord2.name(), currEn,  rrt));
		}
		
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

	private void addPOSFeatures(ArrayList<Integer> featureList, Network network, Sentence sent, int pos, int tId){
		String currTag = pos==sent.length()? "<PAD>":TagLabel.get(tId).getForm();
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
//			String lchunk = pos>0? sent.get(pos-1).getEntity():"<PAD>";
//			String llchunk = pos==0? "<PAD>": pos==1? "<PAD>":sent.get(pos-2).getEntity();
//			String rchunk = pos<sent.length()-1? sent.get(pos+1).getEntity():"<PAD>";
//			String rrchunk = pos==sent.length()? "<PAD>":pos==sent.length()-1? "<PAD>": pos==sent.length()-2? "<PAD>":sent.get(pos+2).getEntity();
			String chunk = pos==sent.length()? "<PAD>":sent.get(pos).getEntity();
			featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_currWord.name(),	currTag,  chunk));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_leftWord1.name(),   currTag,  lchunk));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_leftWord2.name(), 	currTag,  llchunk));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_rightWord1.name(),  currTag,  rchunk));
//			featureList.add(this._param_g.toFeature(network, FEATYPE.chunk_rightWord2.name(), 	currTag,  rrchunk));
		}
		
		
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
		String currLabel = paTchildE? TagLabel.get(paId).getForm():ChunkLabel.get(paId).getForm();
		int jf1, jf2, jf3, jf4, jf5;  
		int[] arr = null;
		int nodeType = -1;
		String lw = pos>0? sent.get(pos-1).getName():"<PAD>";
		String llw = pos==0? "<PAD>": pos==1? "<PAD>":sent.get(pos-2).getName();
		String rw = pos<sent.length()-1? sent.get(pos+1).getName():"<PAD>";
		String w = pos==sent.length()? "<PAD>":sent.get(pos).getName();
		String rrw = pos==sent.length()-1? "<PAD>": pos==sent.length()-2? "<PAD>":sent.get(pos+2).getName();
		if(!paTchildE){
			//current it's NE structure, need to refer to Tag node.
			nodeType = NODE_TYPES.TNODE.ordinal();
			for (int t = 0; t < TagLabel.TAGS_INDEX.size(); t++) {
				String tag = TagLabel.get(t).getForm();
				arr = new int[] { pos + 1, nodeType, t };
				long unlabeledDstNode = NetworkIDMapper.toHybridNodeID(arr);
				ExactNetwork unlabeledNetwork = (ExactNetwork) network.getUnlabeledNetwork();
				int unlabeledDstNodeIdx = Arrays.binarySearch(unlabeledNetwork.getAllNodes(), unlabeledDstNode);
				if (unlabeledDstNodeIdx >= 0) {
//					jf0 = this._param_g.toFeature(network, FEATYPE.e_joint.name(), currLabel + "&" + tag, "");
					jf1 = this._param_g.toFeature(network, FEATYPE.joint1.name(), currLabel + "&" + tag, w);
					jf2 = this._param_g.toFeature(network, FEATYPE.joint2.name(), currLabel + "&" + tag, lw);
					jf3 = this._param_g.toFeature(network, FEATYPE.joint3.name(), currLabel + "&" + tag, rw);
//					jf4 = this._param_g.toFeature(network, FEATYPE.joint4.name(), currLabel + "&" + tag, llw);
//					jf5 = this._param_g.toFeature(network, FEATYPE.joint5.name(), currLabel + "&" + tag, rrw);
//					if(jf0 != -1){
//						featureList.add(jf0); network.putJointFeature(parent_k, jf0, unlabeledDstNodeIdx);
//					}
					if(jf1 != -1){
						featureList.add(jf1); network.putJointFeature(parent_k, jf1, unlabeledDstNodeIdx);
					}
					if(jf2 != -1){
						featureList.add(jf2); network.putJointFeature(parent_k, jf2, unlabeledDstNodeIdx);
					}
					if(jf3 != -1){
						featureList.add(jf3); network.putJointFeature(parent_k, jf3, unlabeledDstNodeIdx);
					}
//					if(jf4 != -1){
//						featureList.add(jf4); network.putJointFeature(parent_k, jf4, unlabeledDstNodeIdx);
//					}
//					if(jf5 != -1){
//						featureList.add(jf5); network.putJointFeature(parent_k, jf5, unlabeledDstNodeIdx);
//					}
					
					if(NetworkConfig.USE_NEURAL_FEATURES){
						int njf = -1;
						if(windowSize==1) 
							njf = this._param_g.toFeature(network,FEATYPE.neural_1.name(), currLabel + "&" + tag,  w.toLowerCase());
						else if(windowSize==3)
							njf =  this._param_g.toFeature(network,FEATYPE.neural_1.name(), currLabel + "&" + tag,  lw.toLowerCase()+IN_SEP+w.toLowerCase()
																										+IN_SEP+rw.toLowerCase());
						else if(windowSize==5)
							njf = this._param_g.toFeature(network,FEATYPE.neural_1.name(), currLabel + "&" + tag,  llw.toLowerCase()+IN_SEP+
																										lw.toLowerCase()+IN_SEP+w.toLowerCase()
																										+IN_SEP+rw.toLowerCase()+IN_SEP+
																										rrw.toLowerCase());
						else throw new RuntimeException("Unknown window size: "+windowSize);
						if (njf != -1) {
							featureList.add(njf);
							network.putJointFeature(parent_k, njf, unlabeledDstNodeIdx);
						}
					}
				}
			}
			
		}else{
			//current it's POS structure, need to refer to chunk node
			nodeType = NODE_TYPES.ENODE.ordinal();
			for(int e=0; e<ChunkLabel.CHUNKS_INDEX.size(); e++){
				String chunk = ChunkLabel.get(e).getForm();
				if (this.iobes && pos == sent.length()-1 && (chunk.startsWith("B") || chunk.startsWith("I")) ) 
					continue;
				arr = new int[]{pos+1, nodeType, e};
				long unlabeledDstNode = NetworkIDMapper.toHybridNodeID(arr);
				ExactNetwork unlabeledNetwork = (ExactNetwork)network.getUnlabeledNetwork();
				int unlabeledDstNodeIdx = Arrays.binarySearch(unlabeledNetwork.getAllNodes(), unlabeledDstNode);
				if (unlabeledDstNodeIdx >= 0) {
//					jf0 = this._param_g.toFeature(network, FEATYPE.t_joint.name(), chunk + "&" + currLabel, "");
					jf1 = this._param_g.toFeature(network, FEATYPE.joint1.name(), chunk + "&" + currLabel, w);
					jf2 = this._param_g.toFeature(network, FEATYPE.joint2.name(), chunk + "&" + currLabel, lw);
					jf3 = this._param_g.toFeature(network, FEATYPE.joint3.name(), chunk + "&" + currLabel, rw);
//					jf4 = this._param_g.toFeature(network, FEATYPE.joint4.name(), chunk + "&" + currLabel, llw);
//					jf5 = this._param_g.toFeature(network, FEATYPE.joint5.name(), chunk + "&" + currLabel, rrw);
//					if(jf0 != -1){
//						featureList.add(jf0); network.putJointFeature(parent_k, jf0, unlabeledDstNodeIdx);
//					}
					if(jf1 != -1){
						featureList.add(jf1); network.putJointFeature(parent_k, jf1, unlabeledDstNodeIdx);
					}
					if(jf2 != -1){
						featureList.add(jf2); network.putJointFeature(parent_k, jf2, unlabeledDstNodeIdx);
					}
					if(jf3 != -1){
						featureList.add(jf3); network.putJointFeature(parent_k, jf3, unlabeledDstNodeIdx);
					}
//					if(jf4 != -1){
//						featureList.add(jf4); network.putJointFeature(parent_k, jf4, unlabeledDstNodeIdx);
//					}
//					if(jf5 != -1){
//						featureList.add(jf5); network.putJointFeature(parent_k, jf5, unlabeledDstNodeIdx);
//					}
					if(NetworkConfig.USE_NEURAL_FEATURES){
						int njf = -1;
						if(windowSize==1) 
							njf = this._param_g.toFeature(network,FEATYPE.neural_1.name(), chunk + "&" + currLabel,  w.toLowerCase());
						else if(windowSize==3)
							njf =  this._param_g.toFeature(network,FEATYPE.neural_1.name(), chunk + "&" + currLabel,  lw.toLowerCase()+IN_SEP+w.toLowerCase()
																										+IN_SEP+rw.toLowerCase());
						else if(windowSize==5)
							njf = this._param_g.toFeature(network,FEATYPE.neural_1.name(), chunk + "&" + currLabel,  llw.toLowerCase()+IN_SEP+
																										lw.toLowerCase()+IN_SEP+w.toLowerCase()
																										+IN_SEP+rw.toLowerCase()+IN_SEP+
																										rrw.toLowerCase());
						else throw new RuntimeException("Unknown window size: "+windowSize);
						if (njf != -1) {
							featureList.add(njf);
							network.putJointFeature(parent_k, njf, unlabeledDstNodeIdx);
						}
					}
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

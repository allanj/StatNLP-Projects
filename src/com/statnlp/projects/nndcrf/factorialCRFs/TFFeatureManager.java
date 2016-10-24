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
import com.statnlp.projects.dep.utils.Extractor;
import com.statnlp.projects.nndcrf.factorialCRFs.TFNetworkCompiler.NODE_TYPES;

public class TFFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 376931974939202432L;

	private boolean useJointFeatures;

	private String IN_SEP = NeuralConfig.IN_SEP;
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
		entity_joint,
		tag_joint,
		neural_1,
		neural_2};
	
		
//	public PrintWriter pw;
	public TFFeatureManager(GlobalNetworkParam param_g, boolean useJointFeatures) {
		super(param_g);
		this.useJointFeatures = useJointFeatures; 
//		try {
//			pw = RAWF.writer("data/dat/grmmtrain.txt");
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	//
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
		if(pos<0 || pos >= inst.size())
			return FeatureArray.EMPTY;
		
		int eId = nodeArr[2];
		//System.err.println(Arrays.toString(nodeArr));
		//int[] child = NetworkIDMapper.toHybridNodeArray(network.getNode(children_k[0]));
		
		//int childPos = child[0]-1;
		
		if(nodeArr[1]==NODE_TYPES.ENODE.ordinal()){
			//must be in the ne chain and not tag in e node
			addEntityFeatures(featureList, network, sent, pos, eId);
			//false: means it's NE structure
			if(useJointFeatures)
				addJointFeatures(featureList, network, sent, pos, eId, parent_k, children_k, false);
			
		}
		
		if(nodeArr[1]==NODE_TYPES.TNODE.ordinal()){
			addPOSFeatures(featureList, network, sent, pos, eId);
			if(useJointFeatures)
				addJointFeatures(featureList, network, sent, pos, eId, parent_k, children_k, true);
		}
		
		ArrayList<Integer> finalList = new ArrayList<Integer>();
		for(int i=0;i<featureList.size();i++){
			if(featureList.get(i)!=-1)
				finalList.add(featureList.get(i));
		}
		int[] features = new int[finalList.size()];
		for(int i=0;i<finalList.size();i++) features[i] = finalList.get(i);
		
		
		//if(features.length==0) return FeatureArray.EMPTY;
		fa = new FeatureArray(features);
		//printing the features.
		
		/****
		// Printing the features: for comparsion with GRMM only
		 
		if(network.getInstance().isLabeled() && network.getInstance().getInstanceId()>0){
			if(nodeArr[1]==NODE_TYPES.ENODE_HYP.ordinal()){
				pw.write(sent.get(pos).getTag()+" "+sent.get(pos).getEntity()+" ----");
				int[] fs = fa.getCurrent();
				for(int f : fs)
					pw.write(" "+f);
			}
			if(nodeArr[1]==NODE_TYPES.TNODE_HYP.ordinal()){
				int[] fs = fa.getCurrent();
				for(int f : fs)
					pw.write(" "+f);
				pw.write("\n");
				if(pos==sent.length()-1)
					pw.write("\n");
			}
		}
		***/
		return fa;
	}
	
	/**
	 * Temoparary not using yet.
	 * @param word
	 * @return
	 */
	@SuppressWarnings("unused")
	private String shape(String word){
		return Extractor.wordShapeOf(word);
	}
	
	private void addEntityFeatures(ArrayList<Integer> featureList,Network network, Sentence sent, int pos, int eId){
		
		if(eId!=(Entity.ENTS.size()+Tag.TAGS.size())){
			String lw = pos>0? sent.get(pos-1).getName():"STR";
			String l2w = lw.equals("STR")? "STR1":pos==1?"STR":sent.get(pos-2).getName();
			String rw = pos<sent.length()-1? sent.get(pos+1).getName():"END";
			String r2w = rw.equals("END")? "END1":pos==(sent.length()-2)? "END": sent.get(pos+2).getName();
			String currWord = sent.get(pos).getName();
			String currEn = Entity.get(eId).getForm();
			String currCaps = capsF(currWord);
			String lcaps = capsF(lw);
			String llcaps = capsF(l2w);
			String rcaps = capsF(rw);
			String rrcaps = capsF(r2w);
			
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity_currWord.name(), 	currEn,	currWord));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity_leftWord1.name(), 	currEn,	lw));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity_leftWord2.name(), 	currEn,	l2w));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity_rightWord1.name(), 	currEn,	rw));
			featureList.add(this._param_g.toFeature(network,FEATYPE.entity_rightWord2.name(), 	currEn,	r2w));

			featureList.add(this._param_g.toFeature(network, FEATYPE.entity_cap.name(), 	currEn,  currCaps));
			featureList.add(this._param_g.toFeature(network, FEATYPE.entity_cap_l.name(), 	currEn,  lcaps));
			featureList.add(this._param_g.toFeature(network, FEATYPE.entity_cap_ll.name(), 	currEn,  llcaps));
			featureList.add(this._param_g.toFeature(network, FEATYPE.entity_cap_r.name(), 	currEn,  rcaps));
			featureList.add(this._param_g.toFeature(network, FEATYPE.entity_cap_rr.name(),	currEn,  rrcaps));
			
			if(NetworkConfig.USE_NEURAL_FEATURES){
				featureList.add(this._param_g.toFeature(network, FEATYPE.neural_1.name(), Entity.get(eId).getForm(), lw.toLowerCase()+IN_SEP+
						currWord.toLowerCase()+IN_SEP+rw.toLowerCase()));
			}
		}
	}

	private void addPOSFeatures(ArrayList<Integer> featureList, Network network, Sentence sent, int pos, int tId){
		String currTag = tId==(Entity.ENTS.size()+Tag.TAGS.size())? "END":Tag.get(tId).getForm();
		String currWord = tId==(Entity.ENTS.size()+Tag.TAGS.size())? "END": sent.get(pos).getName();
		String prevWord = (pos-1)<0? "STR":sent.get(pos-1).getName();
		String p2w = prevWord.equals("STR")? "STR1":pos==1?"STR":sent.get(pos-2).getName();
		String nextWord = tId==(Entity.ENTS.size()+Tag.TAGS.size())? "END1": pos==(sent.length()-1)? "END": sent.get(pos+1).getName();
		String n2w = nextWord.equals("END1")? "END2": nextWord.equals("END")? "END1": pos==(sent.length()-2)? "END":sent.get(pos+2).getName();
		
		String currCaps = capsF(currWord);
		String lcaps = capsF(prevWord);
		String llcaps = capsF(p2w);
		String rcaps = capsF(nextWord);
		String rrcaps = capsF(n2w);
		
		
		featureList.add(this._param_g.toFeature(network,FEATYPE.tag_currWord.name(), 	currTag,	currWord));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tag_leftWord1.name(), 	currTag,	prevWord));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tag_leftWord2.name(), 	currTag,	p2w));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tag_rightWord1.name(), 	currTag,	nextWord));
		featureList.add(this._param_g.toFeature(network,FEATYPE.tag_rightWord2.name(), 	currTag,	n2w));
		
		featureList.add(this._param_g.toFeature(network, FEATYPE.tag_cap.name(), 	currTag,  currCaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.tag_cap_l.name(), 	currTag,  lcaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.tag_cap_ll.name(), currTag,  llcaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.tag_cap_r.name(), 	currTag,  rcaps));
		featureList.add(this._param_g.toFeature(network, FEATYPE.tag_cap_rr.name(),	currTag,  rrcaps));
		
		
		
		if(NetworkConfig.USE_NEURAL_FEATURES){
			featureList.add(this._param_g.toFeature(network, FEATYPE.neural_2.name(), currTag, prevWord.toLowerCase()+IN_SEP+
					currWord.toLowerCase()+IN_SEP+nextWord.toLowerCase() ));
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
		TFNetwork tfnetwork = (TFNetwork)network;
		if(children_k.length!=1)
			throw new RuntimeException("The joint features should only have one children also");
		String currLabel = paTchildE? Tag.get(paId).getForm():Entity.get(paId).getForm();
		int jointFeatureIdx = -1; 
		//the joint feature here is always string (entityLabel, tagLabel);
		
		
		int[] arr = null;
		int nodeType = -1;
		if(!paTchildE){
			//current it's NE structure, need to refer to Tag node.
			nodeType = NODE_TYPES.TNODE.ordinal();
			for(int t=0;t<Tag.TAGS_INDEX.size();t++){
				String tag =  Tag.get(t).getForm();
				jointFeatureIdx = this._param_g.toFeature(network, FEATYPE.entity_joint.name(), currLabel, tag);
				featureList.add(jointFeatureIdx);
				arr = new int[]{pos+1, nodeType, t, 0, 0};
				addDstNode(tfnetwork, jointFeatureIdx, parent_k, arr);
			}
			
		}else{
			//current it's POS structure, need to refer to Entity node
			nodeType = NODE_TYPES.ENODE.ordinal();
			for(int e=0;e<Entity.ENTS_INDEX.size(); e++){
				String entity = Entity.get(e).getForm();
				jointFeatureIdx = this._param_g.toFeature(network, FEATYPE.tag_joint.name(), currLabel, entity);
				featureList.add(jointFeatureIdx);
				arr = new int[]{pos+1, nodeType, e, 0, 0};
//				System.out.println("unlabel:"+jointFeatureIdx);
				addDstNode(tfnetwork, jointFeatureIdx, parent_k, arr);
			}
		}
			
		
	}
	
	private void addDstNode(TFNetwork network, int jointFeatureIdx, int parent_k, int[] dstArr){
		long unlabeledDstNode = NetworkIDMapper.toHybridNodeID(dstArr);
		TFNetwork unlabeledNetwork = (TFNetwork)network.getUnlabeledNetwork();
		int unlabeledDstNodeIdx = Arrays.binarySearch(unlabeledNetwork.getAllNodes(), unlabeledDstNode);
		network.putJointFeature(parent_k, jointFeatureIdx, unlabeledDstNodeIdx);
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

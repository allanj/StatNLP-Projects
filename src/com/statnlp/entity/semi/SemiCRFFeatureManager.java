package com.statnlp.entity.semi;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.utils.Extractor;
import com.statnlp.entity.semi.SemiCRFNetworkCompiler.NodeType;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.neural.NeuralConfig;

public class SemiCRFFeatureManager extends FeatureManager {
	
	private static final long serialVersionUID = 6510131496948610905L;
	private boolean depFeature;
	private int prefixSuffixLen = 3;
	//private int twoDirInsideLen = 3;

	public enum FeatureType{
		prev_word,
		prev_word_shape,
		prev_tag,
		next_word,
		next_word_shape,
		next_word_tag,
		segment,
		seg_len,
		start_word,
		start_tag,
		end_word,
		end_tag,
		words,
		word_tags,
		word_shapes,
		seg_pref,
		seg_suff,
		transition,
		neural,
		head_word,
		head_tag,
		dep_word_label,
		dep_tag_label,
		cheat
	}
	
	private String OUT_SEP = NeuralConfig.OUT_SEP; 
	private String IN_SEP = NeuralConfig.IN_SEP;
	private final boolean CHEAT = false;
	private boolean nonMarkovFeature;
	
	public SemiCRFFeatureManager(GlobalNetworkParam param_g, boolean nonMarkov, boolean depFeature) {
		super(param_g);
		this.nonMarkovFeature = nonMarkov;
		this.depFeature = depFeature;
		if(CHEAT)
			System.out.println("[Info] Using the cheat features now..");
	}
	
	@Override
	protected FeatureArray extract_helper(Network net, int parent_k, int[] children_k) {
		SemiCRFNetwork network = (SemiCRFNetwork)net;
		SemiCRFInstance instance = (SemiCRFInstance)network.getInstance();
		
		Sentence sent = instance.getInput();
		
		
		int[] parent_arr = network.getNodeArray(parent_k);
		int parentPos = parent_arr[0] - 1;
		
		NodeType parentType = NodeType.values()[parent_arr[2]];
		int parentLabelId = parent_arr[1];
		
		//since unigram, root is not needed
		if(parentType == NodeType.LEAF || parentType == NodeType.ROOT){
			return FeatureArray.EMPTY;
		}
//		System.out.println("isLabeled: "+network.getInstance().isLabeled()+" parent:"+parentPos+" "+parentLabelId);
//		System.out.println("instance size:"+sent.length());
		int[] child_arr = network.getNodeArray(children_k[0]);
		int childPos = child_arr[0] + 1 - 1;
		NodeType childType = NodeType.values()[child_arr[2]];
		int childLabelId = child_arr[1];

		if(CHEAT){
			//int instanceId = Math.abs(instance.getInstanceId());
			//int cheatFeature = _param_g.toFeature(network, FeatureType.cheat.name(), parentLabelId+"", instanceId+" "+parentPos);
			int cheatFeature = _param_g.toFeature(network, FeatureType.cheat.name(), "cheat", "cheat");
			return new FeatureArray(new int[]{cheatFeature});
		}
		
		FeatureArray fa = null;
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		
		int start = childPos;
		int end = parentPos;
		if(parentPos==0 || childType==NodeType.LEAF ) start = childPos;
		String currEn = Label.get(parentLabelId).getForm();
		
		String lw = start>0? sent.get(start-1).getName():"STR";
		String ls = start>0? shape(lw):"STR_SHAPE";
		String lt = start>0? sent.get(start-1).getTag():"STR";
		String rw = end<sent.length()-1? sent.get(end+1).getName():"END";
		String rt = end<sent.length()-1? sent.get(end+1).getTag():"END";
		String rs = end<sent.length()-1? shape(rw):"END_SHAPE";
		featureList.add(this._param_g.toFeature(network,FeatureType.prev_word.name(), 		currEn,	lw));
		featureList.add(this._param_g.toFeature(network,FeatureType.prev_word_shape.name(), currEn, ls));
		featureList.add(this._param_g.toFeature(network,FeatureType.prev_tag.name(), 		currEn, lt));
		featureList.add(this._param_g.toFeature(network,FeatureType.next_word.name(), 		currEn, rw));
		featureList.add(this._param_g.toFeature(network,FeatureType.next_word_shape.name(), currEn, rs));
		featureList.add(this._param_g.toFeature(network,FeatureType.next_word_tag.name(), 	currEn, rt));
		
		StringBuilder segPhrase = new StringBuilder(sent.get(start).getName());
		for(int pos=start+1;pos<=end; pos++){
			String w = sent.get(pos).getName();
			segPhrase.append(" "+w);
		}
		featureList.add(this._param_g.toFeature(network,FeatureType.segment.name(), currEn,	segPhrase.toString()));
		
		int lenOfSeg = end-start+1;
		featureList.add(this._param_g.toFeature(network,FeatureType.seg_len.name(), currEn, lenOfSeg+""));
		
		/** Start and end features. **/
		String startWord = sent.get(start).getName();
		String startTag = sent.get(start).getTag();
		featureList.add(this._param_g.toFeature(network,FeatureType.start_word.name(),	currEn,	startWord));
		featureList.add(this._param_g.toFeature(network,FeatureType.start_tag.name(),	currEn,	startTag));
		String endW = sent.get(end).getName();
		String endT = sent.get(end).getTag();
		featureList.add(this._param_g.toFeature(network,FeatureType.end_word.name(),	currEn,	endW));
		featureList.add(this._param_g.toFeature(network,FeatureType.end_tag.name(),		currEn,	endT));
		
		int insideSegLen = lenOfSeg; //Math.min(twoDirInsideLen, lenOfSeg);
		for(int i=0; i<insideSegLen; i++){
			featureList.add(this._param_g.toFeature(network,FeatureType.words.name()+":"+i,			currEn, sent.get(start+i).getName()));
			featureList.add(this._param_g.toFeature(network,FeatureType.words.name()+":-"+i,		currEn,	sent.get(start+lenOfSeg-i-1).getName()));
			/**the following 4 may not be really used**/
			featureList.add(this._param_g.toFeature(network,FeatureType.word_tags.name()+":"+i,		currEn, sent.get(start+i).getTag()));
			featureList.add(this._param_g.toFeature(network,FeatureType.word_tags.name()+":-"+i,	currEn,	sent.get(start+lenOfSeg-i-1).getTag()));
			featureList.add(this._param_g.toFeature(network,FeatureType.word_shapes.name()+":"+i,	currEn, shape(sent.get(start+i).getName())));
			featureList.add(this._param_g.toFeature(network,FeatureType.word_shapes.name()+":-"+i,	currEn,	shape(sent.get(start+lenOfSeg-i-1).getName())));
		}
		/** needs to be modified maybe ***/
		for(int i=0; i<prefixSuffixLen; i++){
			String prefix = segPhrase.substring(0, Math.min(segPhrase.length(), i+1));
			String suffix = segPhrase.substring(Math.max(segPhrase.length()-i-1, 0));
			featureList.add(this._param_g.toFeature(network,FeatureType.seg_pref.name()+"-"+i,	currEn,	prefix));
			featureList.add(this._param_g.toFeature(network,FeatureType.seg_suff.name()+"-"+i,	currEn,	suffix));
		}
		String prevEntity = Label.get(childLabelId).getForm();
		featureList.add(this._param_g.toFeature(network,FeatureType.transition.name(), prevEntity+"-"+currEn,	""));
		
		
		
		/** add non-markovian neural features **/
		if(NetworkConfig.USE_NEURAL_FEATURES && nonMarkovFeature){
			String position = null;
			if(start==0) position = "start";
			if(parentPos==sent.length()-1) position = "end";
			if(start!=0 && parentPos!=(sent.length()-1)) position = "inside";
			if(start==0 && parentPos==(sent.length()-1)) position = "cover";
			featureList.add(this._param_g.toFeature(network, FeatureType.neural.name(), currEn, lenOfSeg+OUT_SEP+position+OUT_SEP+startWord+IN_SEP+endW));
//			featureList.add(this._param_g.toFeature(network, FeatureType.neural.name(), currEn, lenOfSeg+OUT_SEP+position+OUT_SEP+startWord+IN_SEP+endW+OUT_SEP+startTag+IN_SEP+endT));
		}
		/**  End (non-markovian neural features)**/
		
		
		/**Add dependency features**/
		if(this.depFeature){
			for(int pos=start;pos<=end;pos++){
				String currWord = sent.get(pos).getName();
				String currTag = sent.get(pos).getTag();
				int currHeadIndex = sent.get(pos).getHeadIndex();
				String currHead = currHeadIndex>=0? sent.get(currHeadIndex).getName():"STR";
				String currHeadTag = currHeadIndex>=0? sent.get(currHeadIndex).getTag():"STR";
				featureList.add(this._param_g.toFeature(network,FeatureType.head_word.name(),	currEn, currWord+"& head:"+currHead));
				featureList.add(this._param_g.toFeature(network,FeatureType.head_tag.name(),	currEn,	currTag+"& head:"+currHeadTag));
			}
			
		}
		/**(END) add dependency features**/
		
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

	private String shape(String word){
		return Extractor.wordShapeOf(word);
	}
}

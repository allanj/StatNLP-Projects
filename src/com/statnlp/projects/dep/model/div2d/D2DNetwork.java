package com.statnlp.projects.dep.model.div2d;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.projects.dep.DependencyNetwork;
import com.statnlp.projects.dep.model.mhp.MHPMain;
import com.statnlp.projects.dep.utils.DPConfig;

public class D2DNetwork extends DependencyNetwork {

	
	private static final long serialVersionUID = 991556477287748391L;
	private static String OE = DPConfig.OE;
	private static String ONE = DPConfig.ONE;
	private static String PARENT_IS = DPConfig.PARENT_IS;
	
	
	public D2DNetwork() {
		// TODO Auto-generated constructor stub
	}

	public D2DNetwork(int networkId, D2DInstance inst, LocalNetworkParam param) {
		super(networkId, inst, param);
		// TODO Auto-generated constructor stub
	}

	public D2DNetwork(int networkId, D2DInstance inst, long[] nodes, int[][][] children, LocalNetworkParam param, int numNodes) {
		super(networkId, inst, nodes, children, param,numNodes);
	}
	
	public void remove(int k){
		
	}
	
	public boolean isRemoved(int k){
		return false;
	}
	
//	public double totalLossUpTo(int k, int[] child_k){
//		D2DInstance instance = (D2DInstance)this.getInstance();
//		Sentence sent = instance.getInput();
//		
//		int[] paArr = getNodeArray(k);
//		int left = paArr[0]- paArr[1];
//		int right = paArr[0];
//		int comp = paArr[2];
//		int direction = paArr[3];
//		int typeIdx = paArr[4];
//		
//		
//		double loss = 0;
//		if(child_k.length==0)
//			return 0.0;
//		
//		int[] carr = getNodeArray(child_k[0]);
//		if(child_k.length==1 && typeIdx==D2DMain.typeMap.get(PARENT_IS+OE) && carr[4]>1 && carr[4]<6 ){
//			double err = 0;
//			for(int i=left;i<=right;i++){
//				String e = sent.get(i).getEntity().length()<2? DPConfig.ONE: sent.get(i).getEntity().substring(2);
//				int eIdx = D2DMain.typeMap.get(e);
//				if(carr[4]!=eIdx)
//					err++;
//			}
//			return err;
//		}else{
//			for(int i=0;i<child_k.length;i++)
//				loss+=_loss[child_k[i]];
//			return loss;
//		}
//	}

}

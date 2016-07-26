package com.statnlp.dp.model.var;

import com.statnlp.hybridnetworks.TableLookupNetwork;

public class VarNetwork  extends TableLookupNetwork{

	
	private static final long serialVersionUID = -2332617292604031178L;
	
	private LinearNetwork lcrfNetwork;
	private TreeNetwork tcrfNetwork;
	
	public VarNetwork() {
		
	}
	
	
	public VarNetwork(LinearNetwork lcrfNetwork , TreeNetwork tcrfNetwork) {
		this.lcrfNetwork = lcrfNetwork;
		this.tcrfNetwork = tcrfNetwork;
	}


	public LinearNetwork getLcrfNetwork() {
		return lcrfNetwork;
	}


	public void setLcrfNetwork(LinearNetwork lcrfNetwork) {
		this.lcrfNetwork = lcrfNetwork;
	}


	public TreeNetwork getTcrfNetwork() {
		return tcrfNetwork;
	}


	public void setTcrfNetwork(TreeNetwork tcrfNetwork) {
		this.tcrfNetwork = tcrfNetwork;
	}
	
	

	
	
}

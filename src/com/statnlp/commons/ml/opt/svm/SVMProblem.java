package com.statnlp.commons.ml.opt.svm;

import java.io.Serializable;

public class SVMProblem implements Serializable {
	
	private static final long serialVersionUID = -8618197575405811592L;
	
	public int l;
	public double[] y;
	public SVMNode[][] x;
	
}

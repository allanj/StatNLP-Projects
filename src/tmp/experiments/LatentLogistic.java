package tmp.experiments;

import com.statnlp.commons.ml.opt.LBFGSOptimizer;

public class LatentLogistic {
	
	public static void main(String args[]){
		
		double x[] = new double[3];
		
		//(e^x1+e^x2)/(e^x1+e^x2+e^x3)
		
		double obj = Math.log(Math.exp(x[0])+Math.exp(x[1])) - Math.log(Math.exp(x[0])+Math.exp(x[1])+Math.exp(x[2]));
		System.err.println(obj);
		
		LBFGSOptimizer opt = new LBFGSOptimizer();
		
		opt.setObjective(getObj(x));
		opt.setGradients(getGradient(x));
		
	}
	
	private static double getObj(double x[]){
		return Math.log(Math.exp(x[0])+Math.exp(x[1])) - Math.log(Math.exp(x[0])+Math.exp(x[1])+Math.exp(x[2]));
	}
	
	private static double[] getGradient(double x[]){
		double g[] = new double [3];
		g[0] = x[0]+x[2] - Math.log(Math.exp(x[0])+Math.exp(x[2])) - Math.log(Math.exp(x[0])+Math.exp(x[1])+Math.exp(x[2]));
		g[0] = Math.exp(g[0]);
		
		g[1] = x[1]+x[2] - Math.log(Math.exp(x[0])+Math.exp(x[2])) - Math.log(Math.exp(x[0])+Math.exp(x[1])+Math.exp(x[2]));
		g[1] = Math.exp(g[1]);
		
		g[2] = x[3] - Math.log(Math.exp(x[0])+Math.exp(x[1])+Math.exp(x[2]));
		g[2] = Math.exp(g[2]);
		
		return g;
	}

}

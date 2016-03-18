package tmp.experiments;

import com.statnlp.commons.ml.opt.LBFGSOptimizer;

public class Convexity_check {
	
	public static void main(String args[]){
		
		LBFGSOptimizer opt = new LBFGSOptimizer();
		
		double x = 0;
		double y = 0;
		double z = 0;
		double[] vars = new double[]{x, y, z};
		
		while(true){
			opt(x, y, z, opt);
		}
	}
	
	private static void opt(double x, double y, double z, LBFGSOptimizer opt){
		
		double exp_x = Math.exp(x);
		double exp_y = Math.exp(y);
		double exp_z = Math.exp(z);
		double exp_xy = Math.exp(x+y);
		double exp_yz = Math.exp(y+z);
		double exp_xyz = Math.exp(x+y+z);
		
		double numerator = exp_xy + exp_yz;
		double denominator = exp_x + exp_y + exp_z + exp_xyz + numerator;
		
		double obj = Math.log(numerator) - Math.log(denominator);
		
		double[] gradients = new double[3];
		gradients[0] = 0;
		gradients[1] = 0;
		gradients[2] = 0;
		
		
		
	}

}

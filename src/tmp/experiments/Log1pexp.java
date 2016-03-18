package tmp.experiments;

import org.apache.commons.math3.util.FastMath;

public class Log1pexp {
	
	public static void main(String args[]){
		
		double v = -1000;
		System.err.println(log1pexp_approx(v));
		System.err.println(log1pexp(v));
		
		System.exit(1);
		
		long bTime, eTime;
		
		double[] xs = new double[10000000];
		for(int k = 0; k<xs.length; k++){
			xs[k] = Math.random()*10-5;
		}
		
		bTime = System.currentTimeMillis();
		for(int k = 0; k<xs.length; k++){
			double v1 = log1pexp_approx(xs[k]);
		}
		eTime = System.currentTimeMillis();
		System.err.println(eTime-bTime+" ms.");
		
		bTime = System.currentTimeMillis();
		for(int k = 0; k<xs.length; k++){
			double v1 = log1pexp(xs[k]);
		}
		eTime = System.currentTimeMillis();
		System.err.println(eTime-bTime+" ms.");
	}
	
	private static double log1pexp(double x){
		return Math.log1p(FastMath.exp(x));
	}
	
	private static double log1pexp_approx(double x){
		
		if(x <= -37) {
			return FastMath.exp(x);
		} else if( x <= 18 ){
			return Math.log1p(FastMath.exp(x));
		} else if( x <= 33.3 ){
			return x + FastMath.exp(-x);
		} else {
			return x;
		}
		
	}
}

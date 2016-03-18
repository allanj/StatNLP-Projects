package semie.util;

public class MathUtil {

	public static double square(double[] x){
		double v = 0;
		for(int i = 0; i<x.length; i++){
			v += x[i] * x[i];
		}
		return v;
	}
	
}

package data.preprocess;

import org.apache.commons.math3.stat.inference.TTest;

public class TTESTStat {

		public static void main(String[] args){
			double[] sample1 = { 1  , 2  , 3   ,4 , 3, 5, 6.1, 3.4, 2.9, 4.4};
			double[] sample2 = { 5.2, 4.2, 7.24,4 , 5, 6, 4.1, 5.9, 7.0, 8.0};
			double t_statistic;
			TTest ttest = new TTest();
			t_statistic = ttest.pairedTTest(sample1, sample2);
			System.out.println(Double.toString( t_statistic) );
		}

}

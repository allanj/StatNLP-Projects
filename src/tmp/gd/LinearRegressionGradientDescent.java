/** Statistical Natural Language Processing System
    Copyright (C) 2014-2015  Lu, Wei

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * 
 */
package tmp.gd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

/**
 * @author wei_lu
 *
 */
public class LinearRegressionGradientDescent {

	private static double _gamma = 0.1;
	private static Random r = new Random(1234);
	
	public static void main(String args[])throws IOException{
		
		String filename = "data/linreg.train";
		Scanner scan = new Scanner(new File(filename));
		
		int N = 20;
		
		ArrayList<Data> data_train = new ArrayList<Data>();
		
		String line;
		while(scan.hasNextLine()){
			line = scan.nextLine();
			String[] nums = line.split("\\s");
			double[] x = new double[nums.length-1];
			for(int k = 0; k<x.length; k++){
				x[k] = Double.parseDouble(nums[k]);
			}
			double y = Double.parseDouble(nums[x.length]);
			
			Data d = new Data(x,y);
			data_train.add(d);
		}
		scan.close();
		
		System.err.println("OK:"+data_train.size());
		
		double[] w = new double[N];

		String filename_test = "data/linreg.test";
		Scanner scan_test = new Scanner(new File(filename_test));
		
		ArrayList<Data> data_test = new ArrayList<Data>();
		
		while(scan_test.hasNextLine()){
			line = scan_test.nextLine();
			String[] nums = line.split("\\s");
			double[] x = new double[nums.length-1];
			for(int k = 0; k<x.length; k++){
				x[k] = Double.parseDouble(nums[k]);
			}
			double y = Double.parseDouble(nums[x.length]);
			
			Data d = new Data(x,y);
			data_test.add(d);
		}
		scan_test.close();
		
		for(int IT = 0; IT<3000; IT++){
			train(w, data_train);
			
			double obj_test = 0.0;
			for(int k = 0; k < data_test.size(); k++){
				obj_test += data_test.get(k).getObj(w);
			}
			System.err.println(obj_test);
		}
		
	}
	
	private static double getObj(double[] w, ArrayList<Data> train_data){
		double obj = 0.0;
		for(int k = 0; k<train_data.size(); k++){
			obj += train_data.get(k).getObj(w);
		}
//		if(Double.isNaN(obj)){
//			throw new RuntimeException("x");
//		}
		return obj;
	}
	
	private static int cnt = 0;
	
	private static void train(double[] w, ArrayList<Data> train_data){
		
		double obj = getObj(w, train_data);
		System.err.println("obj="+obj);
		
//		int idx = (cnt++)%train_data.size();
		int idx = r.nextInt(train_data.size());
		System.err.println(idx);
		
		double[] x = train_data.get(idx).getX();
		double y = train_data.get(idx).getY();
		
		double a = 0.0;
		for(int i = 0; i<x.length; i++){
			a += x[i] * w[i];
		}
		a = y - a;
		a *= _gamma;
		
		for(int k = 0; k<w.length; k++){
			w[k] += a * x[k];
		}
		
	}
	
	private static class Data{
		
		private double[] x;
		private double y;
		
		public Data(double[] x, double y){
			this.x = x;
			this.y = y;
		}
		
		public double getObj(double[] w){
			double obj = 0;
			double a = 0;
			for(int k = 0; k<w.length; k++){
				a += w[k] * x[k];
			}
			obj = Math.pow(y - a, 2)*0.5;
			return obj;
		}
		
		public double[] getX(){
			return x;
		}
		
		public double getY(){
			return y;
		}
		
	}
	
}
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

import java.io.IOException;
import java.util.Random;

/**
 * @author wei_lu
 *
 */
public class GenLinearRegression {
	
	public static void main(String args[])throws IOException{
		
		Random r = new Random(1234);
		
		int N = 20;
		int M = 10000;
		double[] w = new double[N];
		for(int i = 0; i<N; i++){
			w[i] = r.nextDouble()*10-5;
			System.out.print(w[i]+" ");
		}
		System.out.println();
		
		for(int D = 0; D<M; D++){
			double[] x = new double[N];
			double y = 0.0;
			for(int i = 0; i<N; i++){
				x[i] = r.nextDouble()-.5;
				y += w[i] * x[i];// + (r.nextDouble()-.5)/10;
				System.err.print(x[i]+" ");
			}
			System.err.println(y);
		}
		
	}

}
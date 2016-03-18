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
package com.statnlp.commons.ml.opt;

/**
 * @author wei_lu
 *
 */
public class GradientDescentOptimizer {
	
	private double _learningRate = 1E-10;
	private double[] _x;
	private double[] _g;
	
	private double _T = 1.0;
	
	public GradientDescentOptimizer(double learningRate){
		this._learningRate = learningRate;
		this._T = 1;
	}
	
	public double getLearningRate(){
		return this._learningRate;
	}
	
	public void setVariables(double[] x){
		System.err.println("x0="+x[0]);
		this._x = x;
	}
	
	public void setGradients(double[] g){
		System.err.println("g0="+g[0]);
		this._g = g;
	}
	
	public boolean optimize(){
//		this._learningRate *= 1/this._T;
		for(int k = 0; k<this._x.length; k++){
			this._x[k] -= this._learningRate * this._g[k];
		}
//		this._T++;
		return false;
	}
	
}

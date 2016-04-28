/** Statistical Natural Language Processing System
    Copyright (C) 2014-2016  Lu, Wei

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
package com.statnlp.commons.ml.opt;


import com.statnlp.commons.ml.opt.LBFGS.ExceptionWithIflag;

/**
 * @author wei_lu
 * Find out more about AdaGrad here: https://en.wikipedia.org/wiki/Stochastic_gradient_descent
 */
public class GradientDescentOptimizer implements Optimizer{
	
	private double _learningRate;
	private double[] _x;
	private double[] _g;
	private double _obj;
	private double prevGradients[];
	private double prevDelta[];
	
	public static enum AdaptiveMethod {
		NONE,
		ADAGRAD,
		ADADELTA,
	}
	
	private AdaptiveMethod adaptiveMethod;
	private double adadeltaPhi;
	private double adadeltaEps;
	
	public GradientDescentOptimizer(AdaptiveMethod adaptiveMethod, double learningRate, double adadeltaPhi, double adadeltaEps, int weightLength){
		this._learningRate = learningRate;
		this.prevGradients = new double[weightLength];
		this.prevDelta = new double[weightLength];
		this.adaptiveMethod = adaptiveMethod;
		this.adadeltaPhi = adadeltaPhi;
		this.adadeltaEps = adadeltaEps;
	}
	
	public double getLearningRate(){
		return this._learningRate;
	}
	
	@Override
	public void setVariables(double[] x){
//		System.err.println("x0="+x[0]);
		this._x = x;
	}
	
	@Override
	public void setObjective(double obj){
		this._obj = obj;
	}
	
	@Override
	public void setGradients(double[] g){
//		System.err.println("g0="+g[0]);
		this._g = g;
	}

	@Override
	public double getObjective() {
		return _obj;
	}

	@Override
	public double[] getVariables() {
		return _x;
	}

	@Override
	public double[] getGradients() {
		return _g;
	}
	
	public boolean optimize() throws ExceptionWithIflag{
		for(int k = 0; k<this._x.length; k++){
			if(adaptiveMethod == AdaptiveMethod.NONE){ // Normal (S)GD
				this._x[k] -= this._learningRate * this._g[k];
			}
			if(adaptiveMethod == AdaptiveMethod.ADAGRAD) { // based on http://www.jmlr.org/papers/volume12/duchi11a/duchi11a.pdf
				prevGradients[k] += this._g[k]*this._g[k];
				double updateCoef = this._learningRate;
				if(prevGradients[k]!=0.0){
					updateCoef /= Math.sqrt(prevGradients[k]);
				}
				this._x[k] -= updateCoef * this._g[k];
			} else if (adaptiveMethod == AdaptiveMethod.ADADELTA){ // based on http://www.matthewzeiler.com/pubs/googleTR2012/googleTR2012.pdf
				prevGradients[k] = adadeltaPhi*prevGradients[k] + (1-adadeltaPhi)*Math.pow(this._g[k], 2);
				double update = Math.sqrt(prevDelta[k]+adadeltaEps)/Math.sqrt(prevGradients[k]+adadeltaEps) * this._g[k];
				prevDelta[k] = adadeltaPhi*prevDelta[k] + (1-adadeltaPhi)*Math.pow(update, 2);
				this._x[k] -= update;
			}
		}
		return false;
	}
}

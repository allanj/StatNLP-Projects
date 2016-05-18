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

import com.statnlp.commons.ml.opt.GradientDescentOptimizer.AdaptiveMethod;

public abstract class OptimizerFactory {
	
	public static final double DEFAULT_LEARNING_RATE = 0.01;
	public static final double DEFAULT_ADADELTA_PHI = 0.95;
	public static final double DEFAULT_ADADELTA_EPS = 1e-6;
	
	OptimizerFactory() {}
	
	public static LBFGSOptimizerFactory getLBFGSFactory(){
		LBFGSOptimizerFactory factory = new LBFGSOptimizerFactory();
		return factory;
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with normal (S)GD procedure.<br>
	 * The default learning rate will be set to {@value #DEFAULT_LEARNING_RATE}.
	 * @param learningRate
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactory(){
		return new GradientDescentOptimizerFactory(AdaptiveMethod.NONE, DEFAULT_LEARNING_RATE, 0.0, 0.0);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with normal (S)GD procedure.<br>
	 * @param learningRate
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactory(double learningRate){
		return new GradientDescentOptimizerFactory(AdaptiveMethod.NONE, learningRate, 0.0, 0.0);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with ADAGRAD adaptive method.<br>
	 * The default learning rate will be set to {@value #DEFAULT_LEARNING_RATE}.
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingAdaGrad(){
		return new GradientDescentOptimizerFactory(AdaptiveMethod.ADAGRAD, DEFAULT_LEARNING_RATE, 0.0, 0.0);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with ADAGRAD adaptive method.
	 * @param learningRate
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingAdaGrad(double learningRate){
		return new GradientDescentOptimizerFactory(AdaptiveMethod.ADAGRAD, learningRate, 0.0, 0.0);
	}

	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with ADADELTA adaptive method.<br>
	 * By default the hyperparameters are set as follows:
	 * <ol>
	 * <li>phi = {@value #DEFAULT_ADADELTA_PHI}</li>
	 * <li>eps = {@value #DEFAULT_ADADELTA_EPS}</li>
	 * </ol>
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingAdaDelta(){
		return new GradientDescentOptimizerFactory(AdaptiveMethod.ADADELTA, 0.0, DEFAULT_ADADELTA_PHI, DEFAULT_ADADELTA_EPS);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with ADADELTA adaptive method.<br>
	 * The hyperparameters are set according to the passed values.
	 * @param phi
	 * @param eps
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingAdaDelta(double phi, double eps){
		return new GradientDescentOptimizerFactory(AdaptiveMethod.ADADELTA, 0.0, phi, eps);
	}
	
	
	public abstract Optimizer create(int numWeights);

}

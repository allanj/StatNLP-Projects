package com.statnlp.neural;

public abstract class NNCRFInterface {
	// The remote neural network
	protected RemoteNN nn;

	// Internal Neural weights and gradients
	protected double[] _nnWeights, _nnGrads;
	
	public NNCRFInterface(RemoteNN nn) {
		this.nn = nn;
		nn.setController(this);
	}
	
	// Step 1 & 2: INIT for initializing Internal Neural weights
	// (random initialization and other stuffs)
	public abstract void initializeInternalNeuralWeights();
	
	// Step 3: FORWARD's input (up-to-date Internal Neural weights for NN)
	public double[] getInternalNeuralWeights() {
		return this._nnWeights;
	}
	
	public double[] getInternalNeuralGradients() {
		return this._nnGrads;
	}
	
	// Step 5: FORWARD's return value (External Neural weights in CRF)
	public abstract void updateExternalNeuralWeights(double[] weights);
	
	// Step 7: BACKWARD's input (External Neural gradients for NN)
	public abstract double[] getExternalNeuralGradients();
	
	// Step 8: BACKWARD's return value (Internal Neural gradients)
	public abstract void setInternalNeuralGradients(double[] counts);
	
	// Step 9a: Optimizer's (LBFGS) input
	// (e.g., concatenated Weight and Gradient vectors of Non-Neural and Internal Neural)
	public abstract int getNonNeuralAndInternalNeuralSize();
	public abstract void getNonNeuralAndInternalNeuralWeights(double[] concatWeights, double[] concatGrads);
	
	// Step 9b: Optimizer's return value
	// (up-to-date Non-Neural and Internal Neural weights)
	public abstract void updateNonNeuralAndInternalNeuralWeights(double[] concatWeights);
	
	// wrapper to RemoteNN's forward
	public void forwardNetwork() {
		nn.forwardNetwork();
	}
		
	// wrapper to RemoteNN's backward
	public void backwardNetwork() {
		nn.backwardNetwork();
	}

}

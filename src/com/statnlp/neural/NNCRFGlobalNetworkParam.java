package com.statnlp.neural;

import java.util.Arrays;
import java.util.HashMap;

import com.statnlp.hybridnetworks.GlobalNetworkParam;

public class NNCRFGlobalNetworkParam extends NNCRFInterface {
	
	private GlobalNetworkParam param_G;
	
	// "input" and "output" vocab
	private HashMap<String, Integer> str2idxInput = new HashMap<String, Integer>();
	private HashMap<Integer, String> idx2strInput = new HashMap<Integer, String>();
	private HashMap<Integer, String> idx2strOutput = new HashMap<Integer, String>();
	
	// maps the index in the flattened ``external'' weights from the NN to corresponding feature index
	private int[] externalWeightIndex;
		
	// maps the index in the flattened ``internal'' weights in NN to corresponding feature index
	// Note: this is a trick because in single-layer linear NN, we know the ``internal'' weights
	// in NN directly correspond to the features.
	private int[] internalWeightIndex;
	
	// reference to External Neural features
	private HashMap<String, HashMap<String, Integer>> neuralFeatureIntMap;
	
	// number of NN features
	private int _nnSize = 0;
	
	// number of used NN features
	private int _usedNNSize = 0;
	
	// checks if i-th feature is a neural feature
	private boolean[] _isNNFeature;
	
	// prevent repeated new array allocations
	private double[] grads;
	private double[] notNNWeights, notNNCounts;

	public NNCRFGlobalNetworkParam(RemoteNN nn, GlobalNetworkParam param_G) {
		super(nn);
		this.param_G = param_G;
		this.neuralFeatureIntMap = param_G.getFeatureIntMap().get("neural");
	}
	
	@Override
	public void initializeInternalNeuralWeights() {
		int size = param_G.countFeatures();
		double[] seed = new double[size];
		// can do random init here
		initializeInternalNeuralWeights(seed);
	}
	
	public void initializeInternalNeuralWeights(double[] seed) {
		makeVocab();
		_nnSize = this.nn.initNetwork(idx2strInput.size(), neuralFeatureIntMap.size());
		_nnWeights = new double[_nnSize];
		_nnGrads = new double[_nnSize];
		for(int k = 0 ; k<this._nnSize; k++){
			int weightIdx = internalWeightIndex[k];
			if (weightIdx != -1) {
				this._nnWeights[k] = seed[weightIdx];
			} else {
				this._nnWeights[k] = 0.0;
			}
		}
	}
	
	@Override
	public void updateExternalNeuralWeights(double[] weights) {
		for (int i = 0; i < weights.length; i++) {
			if (externalWeightIndex[i] != -1) {
				param_G.overRideWeight(externalWeightIndex[i], weights[i]);
			}
		}
	}

	@Override
	public double[] getExternalNeuralGradients() {
		double[] counts = param_G.getCounts();
		if (grads == null) {
			grads = new double[_nnSize];
		}
		for (int i = 0; i < _nnSize; i++) {
			int idx = externalWeightIndex[i];
			if (idx != -1) {
				grads[i] = counts[idx];
			} else {
				grads[i] = 0.0;
			}
		}
		return grads;
	}
	
	@Override
	public void setInternalNeuralGradients(double[] counts) {
		for (int i = 0; i < _nnSize; i++) {
			this._nnGrads[i] += counts[i];
		}
	}
	
	public int getNonNeuralAndInternalNeuralSize() {
		int size = param_G.countFeatures();
		int numNotNN = size - _usedNNSize;
		return numNotNN+_nnSize;
	}
	
	public void getNonNeuralAndInternalNeuralWeights(double[] concatWeights, double[] concatCounts) {
		int size = param_G.countFeatures();
		int numNotNN = size - _usedNNSize;
		if (notNNWeights == null) {
			notNNWeights = new double[numNotNN];
			notNNCounts = new double[numNotNN];
		}
		int j = 0;
		double[] weights = param_G.getWeights();
		double[] counts = param_G.getCounts();
		for (int i = 0; i < size; i++) {
			if (!isNNFeature(i)) {
				notNNWeights[j] = weights[i];
				notNNCounts[j] = counts[i];
				j++;
			}
		}
		concatArray(concatWeights, this.notNNWeights, this._nnWeights);
		concatArray(concatCounts, this.notNNCounts, this._nnGrads);
	}
	
	public void updateNonNeuralAndInternalNeuralWeights(double[] concatWeights) {
		unpackArray(concatWeights, this.notNNWeights, this._nnWeights);
		int size = param_G.countFeatures();
		int j = 0;
		for (int i = 0; i < size; i++) {
			if (!isNNFeature(i)) {
				param_G.setWeight(i, notNNWeights[j]);
				j++;
			}
		}
	}
	
	private void makeVocab() {
		for (String output : neuralFeatureIntMap.keySet()) {
			if (!idx2strOutput.containsKey(output)) {
				idx2strOutput.put(idx2strOutput.size(), output);
			}
			for (String input : neuralFeatureIntMap.get(output).keySet()) {
				if (!str2idxInput.containsKey(input)) {
					str2idxInput.put(input, str2idxInput.size());
					idx2strInput.put(idx2strInput.size(), input);
				}
			}
		}
		externalWeightIndex = new int[idx2strOutput.size()*idx2strInput.size()];
		internalWeightIndex = new int[idx2strOutput.size()*idx2strInput.size()];
		for (int i = 0; i < idx2strInput.size(); i++) {
			String input = idx2strInput.get(i);
			for (int j = 0; j < idx2strOutput.size(); j++) {
				String output = idx2strOutput.get(j);
				Integer idx = neuralFeatureIntMap.get(output).get(input);
				if (idx != null) {
					externalWeightIndex[i*idx2strOutput.size()+j] = idx;
					internalWeightIndex[j*idx2strInput.size()+i] = idx;
					setNNFeature(idx);
					_usedNNSize++;
				} else {
					externalWeightIndex[i*idx2strOutput.size()+j] = -1;
					internalWeightIndex[j*idx2strInput.size()+i] = -1;
				}
			}
		}
	}
	
	public boolean isNNFeature(int f) {
		return _isNNFeature[f];
	}
	
	private synchronized void setNNFeature(int f){
		if (_isNNFeature == null) {
			_isNNFeature = new boolean[param_G.countFeatures()];
			Arrays.fill(_isNNFeature, false);
		}
		_isNNFeature[f] = true;
	}
	
	public synchronized void setNNCounts(double[] counts){
		for (int i = 0; i < counts.length; i++) {
			this._nnGrads[i] += counts[i];
		}
	}
	
	// helper functions
	private void unpackArray(double[] arr, double[] a, double[] b) {
		int m = a.length;
		int n = b.length;
		for (int i = 0; i < m; i++) a[i] = arr[i];
		for (int i = 0; i < n; i++) b[i] = arr[i+m];
	}
	
	private void concatArray(double[] ret, double[] a, double[] b) {
		for (int i = 0; i < a.length; i++) ret[i] = a[i];
		for (int i = 0; i < b.length; i++) ret[i+a.length] = b[i];
	}
}

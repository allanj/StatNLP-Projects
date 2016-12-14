package com.statnlp.neural;

import java.util.List;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.statnlp.neural.model.MultiLayerPerceptron;
import org.statnlp.neural.util.Config.WordEmbedding;
import org.statnlp.neural.util.INDArrayList;

public class NNBackend {
	private boolean DEBUG = false;
	
	// Reference to controller instance for updating weights and getting gradients
	private NNCRFInterface controller;
	
	private MultiLayerPerceptron mlp;
	private INDArray vocabINDArray;
	private double[] paramArrBuf;
	private double[] gradParamArrBuf;
	private int[] outputShape;
	
	public void setController(NNCRFInterface controller) {
		this.controller = controller;
	}
	
	public double[] initNetwork(int[] numInputList, int[] inputDimList, List<String> wordList,
						   String lang, WordEmbedding[] embeddingList, int[] embSizeList,
						   int outputDim, List<List<Integer>> vocab) {
		mlp = new MultiLayerPerceptron(NeuralConfig.NUM_LAYER, NeuralConfig.HIDDEN_SIZE,
				NeuralConfig.ACTIVATION, inputDimList, numInputList, embeddingList, 
				embSizeList, outputDim, true, wordList, NeuralConfig.EMBEDDING_PATH);
		double[][] vocabArr = new double[vocab.size()][vocab.get(0).size()];
		for (int r = 0; r < vocabArr.length; r++) {
			List<Integer> list = vocab.get(r);
			for (int c = 0; c < vocabArr[0].length; c++) {
				vocabArr[r][c] = list.get(c);
			}
		}
		vocabINDArray = new INDArrayList(Nd4j.create(vocabArr));
		return getNetworkParameters();
	}
	
	public double[] getNetworkParameters() {
		List<INDArray> params = mlp.getParameters();
		if (paramArrBuf == null) {
			int size = 0;
			for (INDArray p : params) {
				size += p.data().length();
			}
			paramArrBuf = new double[size];
		}
		
		int k = 0;
		for (INDArray p : params) {
			for (int i = 0 ; i < p.data().length(); i++) {
				paramArrBuf[k] = p.data().getDouble(i);
				k++;
			}
		}
		return paramArrBuf;
	}

	public double[] getNetworkGradParameters() {
		List<INDArray> gradParams = mlp.getGradParameters();
		if (gradParamArrBuf == null) {
			int size = 0;
			for (INDArray p : gradParams) {
				size += p.data().length();
			}
			gradParamArrBuf = new double[size];
		}
		
		int k = 0;
		for (INDArray p : gradParams) {
			for (int i = 0 ; i < p.data().length(); i++) {
				gradParamArrBuf[k] = p.data().getDouble(i);
				k++;
			}
		}
		return gradParamArrBuf;
	}
	
	public void setNetworkParameters(double[] parameters) {
		int k = 0;
		
		for (INDArray p : mlp.getParameters()) {
			for (int i = 0 ; i < p.data().length(); i++) {
				p.data().put(i, parameters[k]);
				k++;
			}
		}
	}
	
	public void forwardNetwork(boolean training) {
		double[] nnInternalWeights = controller.getInternalNeuralWeights();
		setNetworkParameters(nnInternalWeights);
		
		INDArray output = mlp.forward(vocabINDArray);
		if(outputShape == null) {
			outputShape = output.shape();
		}
		controller.updateExternalNeuralWeights(output.data().asDouble());
	}
	
	public void backwardNetwork() {
		double[] grad = controller.getExternalNeuralGradients();
		mlp.backward(vocabINDArray, Nd4j.create(grad, outputShape));
		controller.setInternalNeuralGradients(getNetworkGradParameters());
		
	}
	
}

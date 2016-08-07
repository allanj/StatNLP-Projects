package com.statnlp.neural;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.zeromq.ZMQ;

import com.statnlp.hybridnetworks.NetworkConfig;

public class RemoteNN {
	private boolean DEBUG = false;
	
	// Torch NN server information
	private ZMQ.Context context;
	private ZMQ.Socket requester;
	private String serverAddress = NeuralConfig.NEURAL_SERVER_PREFIX + NeuralConfig.NEURAL_SERVER_ADDRESS+":" + NeuralConfig.NEURAL_SERVER_PORT;
	
	// Reference to controller instance for updating weights and getting gradients
	private NNCRFInterface controller;
	
	public RemoteNN() {
		context = ZMQ.context(1);
		requester = context.socket(ZMQ.REQ);
		requester.connect(serverAddress);
	}
	
	public void setController(NNCRFInterface controller) {
		this.controller = controller;
	}
	
	public double[] initNetwork(List<Integer> numInputList, List<Integer> inputDimList,
						   List<String> embeddingList, List<Integer> embSizeList,
						   int outputDim, List<List<Integer>> vocab) {
		JSONObject obj = new JSONObject();
		obj.put("cmd", "init");
		obj.put("numInputList", numInputList);
		obj.put("inputDimList", inputDimList);
		obj.put("embedding", embeddingList);
		obj.put("embSizeList", embSizeList);
		obj.put("outputDim", outputDim);
		obj.put("numLayer", NeuralConfig.NUM_LAYER);
		obj.put("hiddenSize", NeuralConfig.HIDDEN_SIZE);
		obj.put("activation", NeuralConfig.ACTIVATION);
		obj.put("dropout", NeuralConfig.DROPOUT);
		obj.put("optimizer", NeuralConfig.OPTIMIZER);
		obj.put("learningRate", NeuralConfig.LEARNING_RATE);
		obj.put("vocab", vocab);

		String request = obj.toString();
		requester.send(request.getBytes(), 0);
		byte[] reply = requester.recv(0);
		double[] nnInternalWeights = null;
		if(NetworkConfig.OPTIMIZE_NEURAL) {
			JSONArray arr = new JSONArray(new String(reply));
			nnInternalWeights = new double[arr.length()];
			for (int i = 0; i < nnInternalWeights.length; i++) {
				nnInternalWeights[i] = arr.getDouble(i);
			}
		}
		if (DEBUG) {
			System.out.println("Init returns " + new String(reply));
		}
		return nnInternalWeights;
	}
	
	public void forwardNetwork(boolean training) {
		JSONObject obj = new JSONObject();
		obj.put("cmd", "fwd");
		obj.put("training", training);
		
		if(NetworkConfig.OPTIMIZE_NEURAL) {
			double[] nnInternalWeights = controller.getInternalNeuralWeights();
			JSONArray nnWeightsArr = new JSONArray();
			for (int i = 0; i < nnInternalWeights.length; i++) {
				nnWeightsArr.put(nnInternalWeights[i]);
			}
			obj.put("weights", nnWeightsArr);
		}
		
		String request = obj.toString();
		requester.send(request.getBytes(), 0);

		byte[] reply = requester.recv(0);
		JSONArray arr = new JSONArray(new String(reply));
		double[] nnExternalWeights = new double[arr.length()];
		for (int i = 0; i < nnExternalWeights.length; i++) {
			nnExternalWeights[i] = arr.getDouble(i);
		}
		controller.updateExternalNeuralWeights(nnExternalWeights);
		if (DEBUG) {
			System.out.println("Forward returns " + arr.toString());
		}
	}
	
	public void backwardNetwork() {
		JSONObject obj = new JSONObject();
		obj.put("cmd", "bwd");
		
		double[] grad = controller.getExternalNeuralGradients();
		JSONArray gradArr = new JSONArray();
		for (int i = 0; i < grad.length; i++) {
			gradArr.put(grad[i]);
		}
		obj.put("grad", gradArr);
		
		String request = obj.toString();
		requester.send(request.getBytes(), 0);
		
		byte[] reply = requester.recv(0);
		if(NetworkConfig.OPTIMIZE_NEURAL) {
			JSONArray grads = new JSONArray(new String(reply));
			double[] counts = new double[grads.length()];
			for (int i = 0; i < counts.length; i++) {
				counts[i] = grads.getDouble(i);
			}
			controller.setInternalNeuralGradients(counts);
		}
		
		if (DEBUG) {
			System.out.println("Backward returns " + new String(reply));
		}
	}
	
	public void cleanUp() {
		requester.close();
		context.term();
	}
	
}

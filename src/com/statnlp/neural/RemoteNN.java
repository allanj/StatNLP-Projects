package com.statnlp.neural;

import org.json.JSONArray;
import org.json.JSONObject;
import org.zeromq.ZMQ;

import com.statnlp.hybridnetworks.NetworkConfig;

public class RemoteNN {
	private boolean DEBUG = false;
	
	// Torch NN server information
	private ZMQ.Context context;
	private ZMQ.Socket requester;
	private String serverAddress = NetworkConfig.NEURAL_SERVER_ADDR;
	
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
	
	public int initNetwork(int inputSize, int outputSize) {
		JSONObject obj = new JSONObject();
		obj.put("cmd", "init");
		obj.put("inputVocabSize", inputSize);
		obj.put("outputDim", outputSize);

		String request = obj.toString();
		requester.send(request.getBytes(), 0);
		byte[] reply = requester.recv(0);
		JSONArray replyJSON = new JSONArray(new String(reply));
		int numWeights = replyJSON.getInt(0);
		if (DEBUG) {
			System.out.println("Init returns " + replyJSON.toString());
		}
		return numWeights;
	}
	
	public void forwardNetwork() {
		JSONObject obj = new JSONObject();
		obj.put("cmd", "fwd");
		
		double[] nnInternalWeights = controller.getInternalNeuralWeights();
		JSONArray nnWeightsArr = new JSONArray();
		for (int i = 0; i < nnInternalWeights.length; i++) {
			nnWeightsArr.put(nnInternalWeights[i]);
		}
		obj.put("weights", nnWeightsArr);
		
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
		JSONArray grads = new JSONArray(new String(reply));
		double[] counts = new double[grads.length()];
		for (int i = 0; i < counts.length; i++) {
			counts[i] = grads.getDouble(i);
		}
		controller.setInternalNeuralGradients(counts);
		if (DEBUG) {
			System.out.println("Backward returns " + grads.toString());
		}
	}
	
	public void cleanUp() {
		requester.close();
		context.term();
	}
	
}

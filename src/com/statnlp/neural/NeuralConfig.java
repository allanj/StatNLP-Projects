package com.statnlp.neural;

public class NeuralConfig {
	public static int NEURAL_SERVER_PORT = 9546;
	public static String NEURAL_SERVER_PREFIX = "tcp://";
	public static String NEURAL_SERVER_ADDRESS = "172.18.240.32";
	
	public static int WORD_EMBEDDING_SIZE = 100;
	public static int NUM_LAYER = 0;
	public static int HIDDEN_SIZE = 100;
	public static String ACTIVATION = "tanh";
	public static double DROPOUT = 0;
}

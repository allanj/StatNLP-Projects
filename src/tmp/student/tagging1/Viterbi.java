/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tmp.student.tagging1;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Hazar This class is written to make a POS tagging using the HMM and
 *         Viterbi algorithm
 */
public class Viterbi {

	private HMM theModel;

	/**
	 * Path_to_train_data: string that locate the train file Construct the model
	 * and train it
	 */
	public void train(String Path_to_train_data) {
		theModel = new HMM(Path_to_train_data);
//		theModel.printA();
//		theModel.printB();
	}

	/**
	 * myString: sentence with words separated by space to be tagged build a
	 * tagged sentence with the format of one token per line with token and tag
	 * separated by tab
	 */
	public String Predict(String myString) {

		// observation of size T
		String[] observation = (myString).split("\\s");
		// vetrbi path probability matrix
		double[][] pi = new double[theModel.numTags()][observation.length];
		// backpointers store the state that give the max probability
		int[][] backpointers = new int[theModel.numTags()][observation.length];

		// init-------------------------------------------------------
		// for each state s from 1 to N do
		// pi[s,1]=a(0->s)*b(s->o1)
		// backpointers[s,1] = 0
		for (int i = 0; i < theModel.numTags(); i++) {
			double a = theModel
					.MLE_Transition("*", theModel.getTagset().get(i));
			double b = theModel.MLE_Emission(theModel.getTagset().get(i),
					observation[0]);
			pi[i][0] = a * b;
			backpointers[i][0] = -1;
		}
		// recurcive--------------------------------------------------------------
		// for each time step t from 2 to T do
		// for each state s from 1 to N do
		// pi[s,t]=max pi[s`,t-1]* a(s`->s) * b(s->ot) ; s` from 1 to N
		// backpointers[s,t]=argmax pi[s`,t-1]* a(s`->s) * b(s->ot) ; s` from 1
		// to N
		for (int k = 1; k < observation.length; k++) {
			for (int v = 0; v < theModel.numTags(); v++) {
				double max = 0;
				int indexofmax = 0;
				for (int u = 0; u < theModel.numTags(); u++) {
					double a = theModel.MLE_Transition(theModel.getTagset()
							.get(u), theModel.getTagset().get(v));
					double b = theModel.MLE_Emission(theModel.getTagset()
							.get(v), observation[k]);
					double prob = pi[u][k - 1] * a * b;
					if (prob > max) {
						max = prob;
						indexofmax = u;
					}
				}
				pi[v][k] = max;
				backpointers[v][k] = indexofmax;
			}
		}
		// Utility.printPi(pi);
		// Utility.printback(backpointers);
		// terminal-----------------------------------------------------------
		// pi[Stop,T]=max pi[s,T]* a(S->STOP) ; s from 1 to N
		// backpointers[Stop,T]=max pi[s,T]* a(S->STOP) ; s from 1 to N
		double max = 0;
		int indexofmax = 0;
		for (int u = 0; u < theModel.numTags(); u++) {
			double a = theModel.MLE_Transition(theModel.getTagset().get(u),
					"STOP");
			double prob = pi[u][observation.length - 1] * a;
			if (prob > max) {
				max = prob;
				indexofmax = u;
			}
		}
		String result = "";
		result += theModel.getTagset().get(indexofmax);

		// backtrack--------------------------------------------
		// bachtrack path by following back pointers to states back in time from
		// backpointers[Stop,T]
		int path = indexofmax;
		for (int u = observation.length - 1; u >= 1; u--) {
			path = backpointers[path][u];
			result = theModel.getTagset().get(path) + " " + result;
		}
		// formating the final result---------------------------------------
		String[] postags = result.split("\\s");

		String finalresult = "";
		for (int k = 0; k < observation.length; k++) {
			finalresult += observation[k] + "\t" + postags[k] + "\n";
		}

		return finalresult;
	}

	/**
	 * Path_to_test_data: string that locate the test file Path_to_save_result:
	 * string that locate where to save the result read the test file sentences
	 * then tag them and write them out to the path
	 * 
	 */
	public void test(String Path_to_test_data, String Path_to_save_result) {

		ArrayList<String> test_dataset = new ArrayList<>();
		ArrayList<String> taged_dataset = new ArrayList<>();

		test_dataset = Utility.read_test_data(Path_to_test_data);

		for (String sen : test_dataset) {
			long time = System.currentTimeMillis();
			String tagged = Predict(sen);
			time = System.currentTimeMillis() - time;
			System.err.println(time/1000.0+" secs.");
			taged_dataset.add(tagged);
		}
		Utility.write_tagged_data(taged_dataset, Path_to_save_result);
	}

	public String getAccuracy(String Path_to_test_data,
			String Path_to_Golden_test_data) {
		double acc = 0;
		List<String> test_data = Utility.fileToLines(Path_to_test_data);
		List<String> golden_test_data = Utility
				.fileToLines(Path_to_Golden_test_data);

		int correct = 0;
		for (int i = 0; i < test_data.size(); i++)
			if (test_data.get(i).equals(golden_test_data.get(i)))
				correct++;
		acc = (double) correct / test_data.size();
		return "Model Accuracy is " + acc * 100 + "%";
	}
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tmp.student.pos.tagging;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 *
 * @author Hazar Reading and writing to files
 */
public class Utility {
	/**
	 * Path_to_test_data: String locating the test file returns an array list of
	 * sentences from the test file
	 */
	public static ArrayList<String> read_test_data(String Path_to_test_data) {
		ArrayList<String> test_dataset = new ArrayList<>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(
					Path_to_test_data));
			String line = "";
			String sentence = "";
			while ((line = br.readLine()) != null) {
				if (line.isEmpty()) {
					test_dataset.add(sentence.trim());
					sentence = "";
				}
				sentence += line + " ";
			}
			test_dataset.add(sentence.trim());
		} catch (Exception e) {
			System.err.print(e.getMessage());
		}
		return test_dataset;
	}

	/**
	 * taged_dataset: array list of tagged sentences write the tagged sentences
	 * to the path .\\project-data\\My_data\\test.out
	 */
	public static void write_tagged_data(ArrayList<String> taged_dataset) {
		try {
			PrintWriter writer = new PrintWriter(
					"project-data/My_data/test-2.out");
			for (String s : taged_dataset)
				writer.println(s);
			writer.close();
		} catch (Exception e) {
			System.err.print(e.getMessage());
		}
	}
}

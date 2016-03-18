/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tmp.student.tagging1;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
				String[] tokens = line.split("\\t");
				sentence += tokens[0] + " ";
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
	public static void write_tagged_data(ArrayList<String> taged_dataset,
			String Path_to_save_result) {
		try {
			PrintWriter writer = new PrintWriter(Path_to_save_result);
			for (String s : taged_dataset)
				writer.println(s);
			writer.close();
		} catch (Exception e) {
			System.err.print(e.getMessage());
		}
	}

	public static List<String> fileToLines(String filename) {
		List<String> lines = new LinkedList<String>();
		String line = "";
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			while ((line = in.readLine()) != null) {
				if (!line.isEmpty()) {
					String[] token = line.split("\\t");
					lines.add(token[1]);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lines;
	}

	public static void printPi(double[][] matrix) {
		System.out.println("---------------- pi ----------------------");
		for (double[] row : matrix)
			System.out.println(Arrays.toString(row));
		System.out.println("--------------------------------------");
	}

	public static void printback(int[][] matrix) {
		System.out.println("---------------- bk ----------------------");
		for (int[] row : matrix)
			System.out.println(Arrays.toString(row));
		System.out.println("--------------------------------------");
	}

}

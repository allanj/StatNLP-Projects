/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tmp.student.tagging1;

/**
 *
 * @author Hazar
 */
public class POSTagging {

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {

		Viterbi v = new Viterbi();

		// System.out.print(v.Predict(""));
		// -------------test case 1:
//		v.train("data/project-data/My_data/oct27.train");
//		v.train("project-data/My_data/train.in");
		v.train("data/project-data/luwei/train");
//		v.test("data/project-data/My_data/oct27.dev",
//				"data/project-data/My_data/oct27.dev.out");
//		v.test("project-data/My_data/test.in",
//				"project-data/My_data/test.out");
//		v.test("data/project-data/luwei/dev.in",
//				"data/project-data/luwei/dev.pred");
//		String acc = v.getAccuracy("data/project-data/My_data/oct27.dev",
//				"data/project-data/My_data/oct27.dev.out");
		String acc = v.getAccuracy("data/project-data/luwei/dev.pred",
				"data/project-data/luwei/dev.out");
		// ------------test case 2:
		// v.train(".\\project-data\\My_data\\oct27.train");
		// v.test(".\\project-data\\My_data\\oct27.test",".\\project-data\\My_data\\oct27.test.out");
		// String acc= v.getAccuracy(".\\project-data\\My_data\\oct27.test",
		// ".\\project-data\\My_data\\oct27.test.out");
		// --------------test case 3:
		// v.train(".\\project-data\\My_data\\oct27.train");
		// v.test(".\\project-data\\My_data\\dev.in",".\\project-data\\My_data\\mydev.out");
		// String acc= v.getAccuracy(".\\project-data\\My_data\\mydev.out",
		// ".\\project-data\\My_data\\dev.out");
		System.out.println(acc);
	}
}

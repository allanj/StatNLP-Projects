/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tmp.student.pos.tagging;

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
		// TODO code application logic here
		Viterbi v = new Viterbi();
		v.train("project-data/My_data/train.in");
		v.test("project-data/My_data/train.in");
		System.out.print(v.Predict("it feel like Summer"));

	}
}

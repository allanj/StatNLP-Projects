package com.statnlp.commons.ml.opt.svm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import com.statnlp.commons.ml.opt.svm.Solver.SolutionInfo;

public class TestSVM {
	
	public static void main(String args[])throws IOException{
		
//		int l = 10;
//		SVMNode[][] x = new SVMNode[l][];
//		byte [] y = new byte[l];
//		double [] y_d = new double[l];
//		for(int k = 0; k<l/2; k++){
//			y[k] = -1;
//			y_d[k] = -1.0;
//		}
//		for(int k = l/2; k<l; k++){
//			y[k] = +1;
//			y_d[k] = +1.0;
//		}
		double Cp = 1.0;
		double Cn = 1.0;
		double eps = 1E-5;
		int shrinking = 0;
		
		SolutionInfo si = new SolutionInfo();
		
//		SVMProblem prob = new SVMProblem();
//		prob.l = l;
//		prob.x = x;
//		prob.y = y_d;
		
		SVMProblem prob = readData("data/a1a.train");
		
		int l = prob.l;
		double[] y_d = prob.y;
		
		double [] alpha = new double[l];
		double [] p = new double[l];
		byte [] y = new byte[l];
		
		System.err.println("#alpha:"+alpha.length);
		
		for(int k = 0; k<alpha.length; k++){
			alpha[k] = 0.0;
		}
		for(int k = 0; k<p.length; k++){
			p[k] = -1.0;
		}
		for(int k = 0; k<l; k++){
			if(y_d[k]==1){
				y[k] = +1;
			} else {
				y[k] = -1;
			}
		}
		
		SVMParam param = new SVMParam();
		
		SVC_Q Q = new SVC_Q(prob, param, y);
		
		Solver solver = new Solver();
		solver.Solve(l, Q, p, y, alpha, Cp, Cn, eps, si, shrinking);
		
	}
	
	private static SVMProblem readData(String filename) throws FileNotFoundException{
		
		HashMap<String, Integer> f2id = new HashMap<String, Integer>();
		
		Scanner scan = new Scanner(new File(filename));
		
		ArrayList<SVMNode[]> nodeList = new ArrayList<SVMNode[]>();
		ArrayList<Double> labels = new ArrayList<Double>();
		
		while(scan.hasNextLine()){
			String[] tokens = scan.nextLine().trim().split("\\s");
			double label = Double.parseDouble(tokens[0]);
			SVMNode nodes[] = new SVMNode[tokens.length-1];
			for(int k = 1; k<tokens.length; k++){
				String token = tokens[k];
				int id = -1;
				if(f2id.containsKey(token)){
					id = f2id.get(token);
				} else {
					id = f2id.size();
					f2id.put(token, id);
				}
				SVMNode node = new SVMNode();
				node.index = id;
				node.value = 1.0;
				nodes[k-1] = node;
			}
			nodeList.add(nodes);
			labels.add(label);
		}
		
		SVMNode[][] nodes = new SVMNode[nodeList.size()][];
		double[] y_d = new double[labels.size()];
		
		for(int k = 0; k<nodes.length; k++){
			nodes[k] = nodeList.get(k);
			y_d[k] = labels.get(k);
		}
		
		SVMProblem prob = new SVMProblem();
		prob.l = nodes.length;
		prob.x = nodes;
		prob.y = y_d;
		
		return prob;
	}

}

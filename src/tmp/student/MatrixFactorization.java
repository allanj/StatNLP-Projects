package tmp.student;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

public class MatrixFactorization {

	private static int NUM_MOVIES = 4;
	private static int NUM_USERS = 5;
	private static int d = 2; // number of latent features
	private static int steps = 20000; // maximum number of steps for optimization
	private static double alpha = 0.0002; // learning rate
	private static double lambda = 0.02; // regularization parameter
	private static int[][] dataset; // (n x m) matrix : (users x movies)
	private static double[][] U = new double[NUM_USERS][d]; // (n x d) matrix represent users latent features
	private static double[][] V = new double[NUM_MOVIES][d];; // (m x d) matrix represent items latent features
	
	public static void main(String[] args) throws IOException {
		
		if(d > Math.min(NUM_USERS, NUM_MOVIES)){
			System.out.println("d should be < min(n,m)");
			System.exit(0);
		}
		loadDataset();
		// initialize movie feature vectors randomly
		for(int i = 0; i < NUM_MOVIES; i++)
			for(int j = 0; j < d; j++)
				V[i][j] = Math.random();
		// iterate until convergence (i.e. until no. of steps is reached OR error is less than a threshold)
		for(int  i = 0; i<steps; i++){
			iterate();
			double err = getErrorFuncValue();
			System.out.println("Iteration " + (i + 1) + " => Error: " + err);
			if(err < 0.001)
				break;
		}
		predict();
		
	}
	
	public static void predict(){
		double [][] pred = multiplicar(U, transpose(V));
		for(int i = 0; i < NUM_USERS; i++){
			for(int  j =0; j < NUM_MOVIES; j++)
				System.out.print(pred[i][j] + " ");
			System.out.println();
		}
	}
	
	public static void iterate(){
		double [][] VT = transpose(V);
		// step (1) : fix V and solve for U
		for(int i = 0; i < NUM_USERS; i++)
			for(int j = 0; j < NUM_MOVIES; j++ )
				if(dataset[i][j] != 0){
					double eij = dataset[i][j] - dotProduct(getRow(U, NUM_USERS, d, i), getRow(V, NUM_MOVIES, d, j));
					for(int k = 0; k < d; k++)
						U[i][k] = U[i][k] + alpha * (eij * VT[k][j] - lambda * U[i][k]);
				}
		
		// step (2) : fix U and solve for V
		for(int i = 0; i < NUM_USERS; i++)
			for(int j = 0; j < NUM_MOVIES; j++ )
				if(dataset[i][j] != 0){
					double eij = dataset[i][j] - dotProduct(getRow(U, NUM_USERS, d, i), getRow(V, NUM_MOVIES, d, j));
					for(int k = 0; k < d; k++)
						VT[k][j] = VT[k][j] + alpha * (eij * U[i][k] - lambda * VT[k][j]);
				}
		V = transpose(VT);
	}
	
	public static double getErrorFuncValue(){
		double[][] predicted  = multiplicar(U, transpose(V));
		double err = 0;
		// compute sum of squares error
		for(int i = 0; i < NUM_USERS; i++)
			for(int j = 0; j < NUM_MOVIES; j++)
				if(dataset[i][j] != 0)
					err += Math.pow(dataset[i][j] - predicted[i][j], 2)/2;
		// add first regularization for U
		double regularization = 0;
		for(int i = 0; i < NUM_USERS; i++)
			for(int j = 0; j < d; j++)
				regularization += Math.pow(U[i][j], 2);
		regularization = regularization*(lambda/2);
		err += regularization;
		// add second regularization for V
		regularization = 0;
		for(int i = 0; i < NUM_MOVIES; i++)
			for(int j = 0; j < d; j++)
				regularization += Math.pow(V[i][j], 2);
		regularization = regularization*(lambda/2);
		err += regularization;
		return err;
	}
	
	public static double[][] multiplicar(double[][] A, double[][] B) {
        int aRows = A.length;
        int aColumns = A[0].length;
        int bRows = B.length;
        int bColumns = B[0].length;
        if (aColumns != bRows) {
            throw new IllegalArgumentException("A:Rows: " + aColumns + " did not match B:Columns " + bRows + ".");
        }
        double[][] C = new double[aRows][bColumns];
        for (int i = 0; i < aRows; i++) { // aRow
            for (int j = 0; j < bColumns; j++) { // bColumn
                for (int k = 0; k < aColumns; k++) { // aColumn
                    C[i][j] += A[i][k] * B[k][j];
                }
            }
        }
        return C;
    }
	
	public static double[][] transpose(double [][] matrix){
		double [][] transpose = new double[matrix[0].length][matrix.length];
		for(int i = 0;i < matrix.length; i++)
			for(int  j = 0; j < matrix[0].length; j++)
				transpose[j][i] = matrix[i][j];
		return transpose;
	}
	
	public static double dotProduct(double [] v1, double [] v2){
		double sum = 0;
		if(v1.length != v2.length){
			System.out.println("Length doesn't match.");
			return sum;
		}
		for(int i = 0; i<v1.length;i++)
			sum += v1[i] * v2[i];
		return sum;
	}
	
	public static double[] getColumn(double[][] matrix, int nrow, int ncol, int c){
		if(c > ncol)
			return null;
		double[] column = new double[nrow];
		for(int i = 0; i<nrow;i++)
			column[i] = matrix[i][c];
		return column.clone();
	}
	
	public static double[] getRow(double[][] matrix, int nrow, int ncol, int r){
		if(r > nrow)
			return null;
		double[] row = new double[ncol];
		for(int i = 0; i<ncol;i++)
			row[i] = matrix[r][i];
		return row.clone();
	}
	
	private static void printDatatset() {
		for(int  i = 0; i<NUM_USERS;i++){
			for(int j = 0; j<NUM_MOVIES; j++)
				System.out.print(dataset[i][j] + " ");
			System.out.println();
		}
		
	}

	private static void loadDataset() throws IOException {
		dataset = new int[NUM_USERS][NUM_MOVIES];
		BufferedReader bf = new BufferedReader(new FileReader(new File("data/Input.txt")));
		String line = "";
		for(int  i = 0;(line = bf.readLine()) != null;i++){
			if(i>=NUM_USERS)
				break;
			StringTokenizer st = new StringTokenizer(line, ",");
			for(int j = 0; st.hasMoreTokens(); j++)
				dataset[i][j] = Integer.parseInt(st.nextToken());
		}
		
		bf.close();
	}
	
}

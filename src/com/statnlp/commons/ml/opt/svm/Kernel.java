package com.statnlp.commons.ml.opt.svm;


//code from LibSVM
//
// Kernel evaluation
//
// the static method k_function is for doing single kernel evaluation
// the constructor of Kernel prepares to calculate the l*l kernel matrix
// the member function get_Q is for getting one column from the Q Matrix
//

public abstract class Kernel extends QMatrix {
	
	private SVMNode[][] x;
	private final double[] x_square;
	
	// svm_parameter
	private final int kernel_type;
	private final int degree;
	private final double gamma;
	private final double coef0;
	
	public Kernel(int l, SVMNode[][] x_, SVMParam param) {
		
		this.kernel_type = param.kernel_type;
		this.degree = param.degree;
		this.gamma = param.gamma;
		this.coef0 = param.coef0;
		
		x = (SVMNode[][]) x_.clone();
		
		if (kernel_type == SVMParam.RBF) {
			x_square = new double[l];
			for (int i = 0; i < l; i++)
				x_square[i] = dot(x[i], x[i]);
		} else{
			x_square = null;
		}
		
	}

	abstract float[] get_Q(int column, int len);

	abstract double[] get_QD();

	public void swap_index(int i, int j) {
		
		do {
			SVMNode[] _ = x[i];
			x[i] = x[j];
			x[j] = _;
		} while (false);
		if (x_square != null){
			do {
				double _ = x_square[i];
				x_square[i] = x_square[j];
				x_square[j] = _;
			} while (false);
		}
		
	}

	private static double powi(double base, int times) {
		
		double tmp = base, ret = 1.0;

		for (int t = times; t > 0; t /= 2) {
			if (t % 2 == 1)
				ret *= tmp;
			tmp = tmp * tmp;
		}
		return ret;
		
	}

	public double kernel_function(int i, int j) {
		
		switch (kernel_type) {
		case SVMParam.LINEAR:
			return dot(x[i], x[j]);
		case SVMParam.POLY:
			return powi(gamma * dot(x[i], x[j]) + coef0, degree);
		case SVMParam.RBF:
			return Math.exp(-gamma
					* (x_square[i] + x_square[j] - 2 * dot(x[i], x[j])));
		case SVMParam.SIGMOID:
			return Math.tanh(gamma * dot(x[i], x[j]) + coef0);
		case SVMParam.PRECOMPUTED:
			return x[i][(int) (x[j][0].value)].value;
		default:
			return 0; // java
		}
		
	}
	
	public static double dot(SVMNode[] x, SVMNode[] y) {
		
		double sum = 0;
		int xlen = x.length;
		int ylen = y.length;
		int i = 0;
		int j = 0;
		while (i < xlen && j < ylen) {
			if (x[i].index == y[j].index) {
				sum += x[i++].value * y[j++].value;
			} else {
				if (x[i].index > y[j].index)
					++j;
				else
					++i;
			}
		}
		return sum;
		
	}

	static double k_function(SVMNode[] x, SVMNode[] y, SVMParam param) {
		
		switch (param.kernel_type) {
		case SVMParam.LINEAR:
			return dot(x, y);
		case SVMParam.POLY:
			return powi(param.gamma * dot(x, y) + param.coef0, param.degree);
		case SVMParam.RBF: {
			double sum = 0;
			int xlen = x.length;
			int ylen = y.length;
			int i = 0;
			int j = 0;
			while (i < xlen && j < ylen) {
				if (x[i].index == y[j].index) {
					double d = x[i++].value - y[j++].value;
					sum += d * d;
				} else if (x[i].index > y[j].index) {
					sum += y[j].value * y[j].value;
					++j;
				} else {
					sum += x[i].value * x[i].value;
					++i;
				}
			}

			while (i < xlen) {
				sum += x[i].value * x[i].value;
				++i;
			}

			while (j < ylen) {
				sum += y[j].value * y[j].value;
				++j;
			}

			return Math.exp(-param.gamma * sum);
		}
		case SVMParam.SIGMOID:
			return Math.tanh(param.gamma * dot(x, y) + param.coef0);
		case SVMParam.PRECOMPUTED:
			return x[(int) (y[0].value)].value;
		default:
			return 0; // java
		}
		
	}
	
}



class ONE_CLASS_Q extends Kernel {
	
	private final Cache cache;
	private final double[] QD;
	
	public ONE_CLASS_Q(SVMProblem prob, SVMParam param) {
		super(prob.l, prob.x, param);
		cache = new Cache(prob.l, (long) (param.cache_size * (1 << 20)));
		QD = new double[prob.l];
		for (int i = 0; i < prob.l; i++)
			QD[i] = kernel_function(i, i);
	}

	public float[] get_Q(int i, int len) {
		float[][] data = new float[1][];
		int start, j;
		if ((start = cache.get_data(i, data, len)) < len) {
			for (j = start; j < len; j++)
				data[0][j] = (float) kernel_function(i, j);
		}
		return data[0];
	}

	public double[] get_QD() {
		return QD;
	}

	public void swap_index(int i, int j) {
		cache.swap_index(i, j);
		super.swap_index(i, j);
		do {
			double _ = QD[i];
			QD[i] = QD[j];
			QD[j] = _;
		} while (false);
	}
	
}

class SVR_Q extends Kernel {
	
	private final int l;
	private final Cache cache;
	private final byte[] sign;
	private final int[] index;
	private int next_buffer;
	private float[][] buffer;
	private final double[] QD;
	
	public SVR_Q(SVMProblem prob, SVMParam param) {
		super(prob.l, prob.x, param);
		l = prob.l;
		cache = new Cache(l, (long) (param.cache_size * (1 << 20)));
		QD = new double[2 * l];
		sign = new byte[2 * l];
		index = new int[2 * l];
		for (int k = 0; k < l; k++) {
			sign[k] = 1;
			sign[k + l] = -1;
			index[k] = k;
			index[k + l] = k;
			QD[k] = kernel_function(k, k);
			QD[k + l] = QD[k];
		}
		buffer = new float[2][2 * l];
		next_buffer = 0;
	}
	
	@Override
	public void swap_index(int i, int j) {
		do {
			byte _ = sign[i];
			sign[i] = sign[j];
			sign[j] = _;
		} while (false);
		do {
			int _ = index[i];
			index[i] = index[j];
			index[j] = _;
		} while (false);
		do {
			double _ = QD[i];
			QD[i] = QD[j];
			QD[j] = _;
		} while (false);
	}
	
	@Override
	public float[] get_Q(int i, int len) {
		float[][] data = new float[1][];
		int j, real_i = index[i];
		if (cache.get_data(real_i, data, l) < l) {
			for (j = 0; j < l; j++)
				data[0][j] = (float) kernel_function(real_i, j);
		}

		// reorder and copy
		float buf[] = buffer[next_buffer];
		next_buffer = 1 - next_buffer;
		byte si = sign[i];
		for (j = 0; j < len; j++)
			buf[j] = (float) si * sign[j] * data[0][index[j]];
		return buf;
	}
	
	@Override
	public double[] get_QD() {
		return QD;
	}
	
}


//
// Q matrices for various formulations
//
class SVC_Q extends Kernel {
	
	private final byte[] y;
	private final Cache cache;
	private final double[] QD;
	
	public SVC_Q(SVMProblem prob, SVMParam param, byte[] y_) {
		super(prob.l, prob.x, param);
		y = (byte[]) y_.clone();
		cache = new Cache(prob.l, (long) (param.cache_size * (1 << 20)));
		QD = new double[prob.l];
		for (int i = 0; i < prob.l; i++) {
			QD[i] = kernel_function(i, i);
		}
	}
	
	public float[] get_Q(int i, int len) {
		float[][] data = new float[1][];
		int start, j;
		if ((start = cache.get_data(i, data, len)) < len) {
			for (j = start; j < len; j++){
				data[0][j] = (float) (y[i] * y[j] * kernel_function(i, j));
			}
		}
		return data[0];
	}
	
	public double[] get_QD() {
		return QD;
	}
	
	public void swap_index(int i, int j) {
		cache.swap_index(i, j);
		super.swap_index(i, j);
		
		do {
			byte _ = y[i];
			y[i] = y[j];
			y[j] = _;
		} while (false);
		
		do {
			double _ = QD[i];
			QD[i] = QD[j];
			QD[j] = _;
		} while (false);
	}
	
}
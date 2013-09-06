package edu.kaist.ir.math;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.kaist.ir.utils.ArrayUtils;
import edu.kaist.ir.utils.VectorUtils;

/**
 * @author Heung-Seon Oh
 * 
 */
public class ArrayMaths {
	public static final double LOGTOLERANCE = 30.0;

	public static void accumulate(double[] x) {
		double sum = 0;
		for (int i = 0; i < x.length; i++) {
			sum += x[i];
			x[i] = sum;
		}
	}

	public static double mean(double[] x) {
		return sum(x) / x.length;
	}

	public static double geometricMean(double[] x) {
		double logSum = 0;
		for (int i = 0; i < x.length; i++) {
			logSum += Math.log(x[i]);
		}
		return Math.exp(logSum / x.length);
	}

	public static double jensenShannonDivergence(double[] x1, double[] x2) {
		assert (x1.length == x2.length);
		double[] average = new double[x1.length];
		for (int i = 0; i < x1.length; ++i) {
			average[i] += (x1[i] + x2[i]) / 2;
		}
		return (klDivergence(x1, average) + klDivergence(x2, average)) / 2;
	}

	public static double variance(double[] x, double mean) {
		double ret = 0;
		for (int i = 0; i < x.length; i++) {
			double diff = x[i] - mean;
			ret += diff * diff;
		}
		ret /= x.length;
		return ret;
	}

	/**
	 * Maths.java in mallet
	 * 
	 * @param x1
	 * @param x2
	 * @return
	 */
	public static double klDivergence(double[] x1, double[] x2) {
		double ret = 0;

		for (int i = 0; i < x1.length; ++i) {
			if (x1[i] == 0) {
				continue;
			}
			if (x2[i] == 0) {
				return Double.POSITIVE_INFINITY;
			}
			ret += x1[i] * Math.log(x1[i] / x2[i]);
		}
		return ret * MyMath.LOG_2_OF_E; // moved this division out of the loop
										// -DM
	}

	public static double entropy(double[] x) {
		double ret = 0;
		for (int i = 0; i < x.length; i++) {
			if (x[i] > 0 && x[i] < 1) {
				ret += x[i] * MyMath.log2(x[i]);
			}
		}
		return -ret;
	}

	public static double covariance(double[] x1, double[] x2) {
		double mean1 = mean(x1);
		double mean2 = mean(x2);
		double ret = 0;
		for (int i = 0; i < x1.length; i++) {
			ret += (x1[i] - mean1) * (x2[i] - mean2);
		}
		return ret;
	}

	public static double[] accumulateTo(double[] x) {
		double[] ret = new double[x.length];
		double sum = 0;
		for (int i = 0; i < x.length; i++) {
			sum += x[i];
			ret[i] = sum;
		}
		return ret;
	}

	public static void add(double[] x, double inrement) {
		for (int i = 0; i < x.length; i++) {
			x[i] += inrement;
		}
	}

	public static void add(double[] x1, double[] x2) {
		for (int i = 0; i < x1.length; i++) {
			x1[i] += x2[i];
		}
	}

	public static void addAfterExp(double[] x1, double[] x2) {
		for (int i = 0; i < x1.length; i++) {
			x1[i] += Math.exp(x2[i]);
		}
	}

	public static double[] addAfterExpTo(double[] x1, double[] x2) {
		double[] ret = new double[x1.length];
		for (int i = 0; i < x1.length; i++) {
			ret[i] = x1[i] + Math.exp(x2[i]);
		}
		return ret;
	}

	public static void addAfterLog(double[] x1, double[] x2) {
		for (int i = 0; i < x1.length; i++) {
			x1[i] += Math.log(x2[i]);
		}
	}

	public static double[] addAfterLogTo(double[] x1, double[] x2) {
		double[] ret = new double[x1.length];
		for (int i = 0; i < x1.length; i++) {
			ret[i] = x1[i] + Math.log(x2[i]);
		}
		return ret;
	}

	public static void addAfterScale(double[] x1, double[] x2, double factor) {
		addAfterScale(x1, x2, 1, factor);
	}

	public static void addAfterScale(double[] x1, double[] x2, double factor1, double factor2) {
		for (int i = 0; i < x1.length; i++) {
			x1[i] = factor1 * x1[i] + factor2 * x2[i];
		}
	}

	public static double[] addAfterScaleTo(double[] x1, double[] x2, double factor) {
		double[] ret = ArrayUtils.copy(x1);
		addAfterScale(x1, x2, factor);
		return ret;
	}

	public static void addTo(double[] x, double a) {
		double[] ret = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			ret[i] = x[i] + a;
		}
	}

	public static double[] addTo(double[] x1, double[] x2) {
		double[] ret = new double[x1.length];
		for (int i = 0; i < x1.length; i++) {
			ret[i] = x1[i] + x2[2];
		}
		return ret;
	}

	public static int argMax(double[] x) {
		int ret = -1;
		double max = Double.MIN_VALUE;
		for (int i = 0; i < x.length; i++) {
			if (x[i] > max) {
				ret = i;
				max = x[i];
			}
		}
		return ret;
	}

	public static int argMax(int[] x) {
		int ret = -1;
		double max = Double.MIN_VALUE;
		for (int i = 0; i < x.length; i++) {
			if (x[i] > max) {
				ret = i;
				max = x[i];
			}
		}
		return ret;
	}

	public static int argMin(double[] x) {
		int ret = -1;
		double min = Double.MAX_VALUE;
		for (int i = 0; i < x.length; i++) {
			if (x[i] < min) {
				ret = i;
				min = x[i];
			}
		}
		return ret;
	}

	public static int argMin(int[] x) {
		int ret = -1;
		double min = Double.MAX_VALUE;
		for (int i = 0; i < x.length; i++) {
			if (x[i] < min) {
				ret = i;
				min = x[i];
			}
		}
		return ret;
	}

	public static int[] between(double[] x, double min, double max) {
		List<Integer> ret = new ArrayList<Integer>();
		for (int i = 0; i < x.length; i++) {
			double value = x[i];
			if (value > min && value < max) {
				ret.add(i);
			}
		}
		return ArrayUtils.integerArray(ret);
	}

	public static double cosine(double[] x1, double[] x2) {
		double ret = 0;
		double norm1 = 0;
		double norm2 = 0;
		double dotProduct = 0;

		for (int i = 0; i < x1.length; i++) {
			dotProduct += x1[i] * x2[i];
			norm1 += x1[i] * x1[i];
			norm2 += x2[i] * x2[i];
		}

		norm1 = Math.sqrt(norm1);
		norm2 = Math.sqrt(norm2);

		if (norm1 > 0 && norm2 > 0) {
			ret = dotProduct / (norm1 * norm2);
		}

		if (ret > 1) {
			ret = 1;
		} else if (ret < 0) {
			ret = 0;
		}

		return ret;
	}

	public static void divide(double[] x1, double[] x2) {
		for (int i = 0; i < x1.length; i++) {
			x1[i] /= x2[i];
		}
	}

	public static double[] divideTo(double[] x1, double[] x2) {
		double[] ret = new double[x1.length];
		for (int i = 0; i < x1.length; i++) {
			ret[i] = x1[i] / x2[i];
		}
		return ret;
	}

	public static double dotProduct(double[] x1, double[] x2) {
		double ret = 0;
		for (int i = 0; i < x1.length; i++) {
			ret += x1[i] * x2[i];
		}
		return ret;
	}

	public static double euclideanDistance(double[] x1, double[] x2) {
		return Math.sqrt(euclideanDistanceSquared(x1, x2));
	}

	public static double euclideanDistanceSquared(double[] x1, double[] x2) {
		double ret = 0;
		for (int i = 0; i < x1.length; i++) {
			double diff = x1[i] - x2[i];
			ret += diff * diff;
		}
		return ret;
	}

	public static void exp(double[] x) {
		for (int i = 0; i < x.length; i++) {
			x[i] = Math.exp(x[i]);
		}
	}

	public static void expAfterScale(double[] x, double factor) {
		for (int i = 0; i < x.length; i++) {
			x[i] = Math.exp(factor * x[i]);
		}
	}

	public static double[] expAfterScaleTo(double[] x, double factor) {
		double[] ret = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			ret[i] = Math.exp(factor * x[i]);
		}
		return ret;
	}

	public static double[] expTo(double[] x) {
		double[] ret = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			ret[i] = Math.exp(x[i]);
		}
		return ret;
	}

	/**
	 * http://introcs.cs.princeton.edu/java/97data/LinearRegression.java.html
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static double[] linearRegression(double[] x, double[] y) {
		int n = x.length;

		// first pass: read in data, compute xbar and ybar
		double sumx = 0.0, sumy = 0.0, sumx2 = 0.0;

		for (int i = 0; i < n; i++) {
			sumx += x[i];
			sumx2 += x[i] * x[i];
			sumy += y[i];
		}

		double xbar = sumx / n;
		double ybar = sumy / n;

		// second pass: compute summary statistics
		double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
		for (int i = 0; i < n; i++) {
			xxbar += (x[i] - xbar) * (x[i] - xbar);
			yybar += (y[i] - ybar) * (y[i] - ybar);
			xybar += (x[i] - xbar) * (y[i] - ybar);
		}
		double beta1 = xybar / xxbar;
		double beta0 = ybar - beta1 * xbar;

		// print results
		System.out.println("y   = " + beta1 + " * x + " + beta0);

		boolean print = false;

		if (print) {
			// analyze results
			int df = n - 2;
			double rss = 0.0; // residual sum of squares
			double ssr = 0.0; // regression sum of squares
			for (int i = 0; i < n; i++) {
				double fit = beta1 * x[i] + beta0;
				rss += (fit - y[i]) * (fit - y[i]);
				ssr += (fit - ybar) * (fit - ybar);
			}
			double R2 = ssr / yybar;
			double svar = rss / df;
			double svar1 = svar / xxbar;
			double svar0 = svar / n + xbar * xbar * svar1;
			System.out.println("R^2                 = " + R2);
			System.out.println("std error of beta_1 = " + Math.sqrt(svar1));
			System.out.println("std error of beta_0 = " + Math.sqrt(svar0));
			svar0 = svar * sumx2 / (n * xxbar);
			System.out.println("std error of beta_0 = " + Math.sqrt(svar0));

			System.out.println("SSTO = " + yybar);
			System.out.println("SSE  = " + rss);
			System.out.println("SSR  = " + ssr);
		}

		double[] ret = new double[2];
		ret[0] = beta0;
		ret[1] = beta1;
		return ret;
	}

	public static void log(double[] x) {
		for (int i = 0; i < x.length; i++) {
			x[i] = Math.log(x[i]);
		}
	}

	public static double[] logTo(double[] x) {
		double[] ret = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			ret[i] = Math.log(x[i]);
		}
		return ret;
	}

	public static void main(String[] args) {
		System.out.println("process begins.");

		System.out.println(ArrayUtils.IntegerList(ArrayUtils.randomIntegerArray(20, 5, 10)));

		System.out.println("process ends.");
	}

	public static double max(double[] x) {
		return x[argMax(x)];
	}

	public static double min(double[] x) {
		return x[argMin(x)];
	}

	public static int min(int[] x) {
		return x[argMin(x)];
	}

	public static void multiply(double[][] x1, double[] x2, double[] y) {
		for (int i = 0; i < x1.length; i++) {
			y[i] = dotProduct(x1[i], x2);
		}
	}

	public static void multiply(double[][] x1, double[][] x2, double[][] y) {
		int rowDimOfA = x1.length;
		int rowDimOfB = x1[0].length;
		int colDimOfB = x2[0].length;

		double[] rowOfA; // row i of A
		double[] columnOfB = new double[colDimOfB]; // column j of B

		for (int j = 0; j < colDimOfB; j++) {
			for (int k = 0; k < rowDimOfB; k++) {
				columnOfB[k] = x2[k][j];
			}
			for (int i = 0; i < rowDimOfA; i++) {
				rowOfA = x1[i];
				double value = 0;
				for (int k = 0; k < rowDimOfB; k++) {
					value += rowOfA[k] * columnOfB[k];
				}
				y[i][j] = value;
			}
		}
	}

	public static double[] multiplyTo(double[][] x1, double[] x2) {
		double[] ret = new double[x1.length];
		multiply(x1, x2, ret);
		return ret;
	}

	/**
	 * http://introcs.cs.princeton.edu/java/95linear/
	 * 
	 * @param x1
	 * @param x2
	 * @return
	 */
	public static double[][] multiplyTo(double[][] x1, double[][] x2) {
		int rowDimOfA = x1.length;
		int colDimOfB = x2[0].length;
		double[][] ret = new double[rowDimOfA][colDimOfB];
		multiply(x1, x2, ret);
		return ret;
	}

	public static void normalize(double[] x) {
		scale(x, 1f / sum(x));
	}

	public static void normalize(double[] x, double high, double low) {
		double max = max(x);
		double min = min(x);

		for (int i = 0; i < x.length; i++) {
			double value = x[i];
			double newValue = low + ((high - low) / (max - min)) * (value - min);
			x[i] = newValue;
		}
	}

	/**
	 * SloppyMath.java in Stanford
	 * 
	 * @param x
	 *            log values
	 */
	public static void normalizeLogProb(double[] x) {
		double logSum = sumLogProb(x);
		if (Double.isNaN(logSum)) {
			throw new RuntimeException("Bad log-sum");
		}
		if (logSum == 0.0)
			return;
		for (int i = 0; i < x.length; i++) {
			x[i] -= logSum;
		}
	}

	/**
	 * SloppyMath.java in Stanford
	 * 
	 * @param x
	 *            log values
	 * @return
	 */
	public static double[] normalizeLogProbTo(double[] x) {
		double logSum = sumLogProb(x);
		if (Double.isNaN(logSum)) {
			throw new RuntimeException("Bad log-sum");
		}

		double[] ret = ArrayUtils.copy(x);
		if (logSum != 0) {
			for (int i = 0; i < x.length; i++) {
				ret[i] -= logSum;
			}
		}
		return ret;
	}

	public static double[] normalizeTo(double[] x) {
		return scaleTo(x, 1f / sum(x));
	}

	public static double normL2(double[] x) {
		return Math.sqrt(dotProduct(x, x));
	}

	public static int[] over(double[] x, double cutoff, boolean includeCutoff) {
		List<Integer> ret = new ArrayList<Integer>();
		for (int i = 0; i < x.length; i++) {
			double value = x[i];

			if (value > cutoff) {
				ret.add(i);
			}

			if (includeCutoff && value == cutoff) {
				ret.add(i);
			}
		}
		return ArrayUtils.integerArray(ret);
	}

	public static void pointwiseMultiply(double[] x1, double[] x2) {
		for (int i = 0; i < x1.length; i++) {
			x1[i] *= x2[i];
		}
	}

	public static void pointwiseMultiply(double[][] x1, double[][] x2) {
		for (int i = 0; i < x1.length; i++) {
			for (int j = 0; j < x1[i].length; j++) {
				x1[i][j] *= x2[i][j];
			}
		}
	}

	public static double[] pointwiseMultiplyTo(double[] x1, double[] x2) {
		double[] ret = new double[x1.length];
		for (int i = 0; i < x1.length; i++) {
			ret[i] = x1[i] * x2[i];
		}
		return ret;
	}

	public static int[] sample(double[] x, int sampleSize) {
		int[] ret = new int[sampleSize];
		Random random = new Random();

		double[] values = accumulateTo(x);
		double sum = values[values.length - 1];

		scale(values, 1f / sum);

		for (int i = 0; i < sampleSize; i++) {
			double randomValue = random.nextDouble();

			for (int j = 0; j < x.length; j++) {
				double value = values[j];
				if (randomValue <= value) {
					ret[i] = j;
					break;
				}
			}
		}

		return ret;
	}

	public static void scale(double[] x, double factor) {
		for (int i = 0; i < x.length; i++) {
			x[i] *= factor;
		}
	}

	public static double[] scaleTo(double[] x, double factor) {
		double[] ret = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			ret[i] = x[i] * factor;
		}
		return ret;
	}

	public static void simpleLinearRegression() {
		int MAXN = 1000;
		int n = 0;
		double[] x = new double[MAXN];
		double[] y = new double[MAXN];

		// first pass: read in data, compute xbar and ybar
		double sumx = 0.0, sumy = 0.0, sumx2 = 0.0;
		// while (!StdIn.isEmpty()) {
		// x[n] = StdIn.readDouble();
		// y[n] = StdIn.readDouble();
		sumx += x[n];
		sumx2 += x[n] * x[n];
		sumy += y[n];
		n++;
		// }
		double xbar = sumx / n;
		double ybar = sumy / n;

		// second pass: compute summary statistics
		double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
		for (int i = 0; i < n; i++) {
			xxbar += (x[i] - xbar) * (x[i] - xbar);
			yybar += (y[i] - ybar) * (y[i] - ybar);
			xybar += (x[i] - xbar) * (y[i] - ybar);
		}
		double beta1 = xybar / xxbar;
		double beta0 = ybar - beta1 * xbar;

		// print results
		System.out.println("y   = " + beta1 + " * x + " + beta0);

		// analyze results
		int df = n - 2;
		double rss = 0.0; // residual sum of squares
		double ssr = 0.0; // regression sum of squares
		for (int i = 0; i < n; i++) {
			double fit = beta1 * x[i] + beta0;
			rss += (fit - y[i]) * (fit - y[i]);
			ssr += (fit - ybar) * (fit - ybar);
		}
		double R2 = ssr / yybar;
		double svar = rss / df;
		double svar1 = svar / xxbar;
		double svar0 = svar / n + xbar * xbar * svar1;
		System.out.println("R^2                 = " + R2);
		System.out.println("std error of beta_1 = " + Math.sqrt(svar1));
		System.out.println("std error of beta_0 = " + Math.sqrt(svar0));
		svar0 = svar * sumx2 / (n * xxbar);
		System.out.println("std error of beta_0 = " + Math.sqrt(svar0));

		System.out.println("SSTO = " + yybar);
		System.out.println("SSE  = " + rss);
		System.out.println("SSR  = " + ssr);
	}

	/**
	 * Maths.java in mallet
	 * 
	 * Returns the difference of two doubles expressed in log space, that is,
	 * 
	 * <pre>
	 *    sumLogProb = log (e^a - e^b)
	 *               = log e^a(1 - e^(b-a))
	 *               = a + log (1 - e^(b-a))
	 * </pre>
	 * 
	 * By exponentiating <tt>b-a</tt>, we obtain better numerical precision than
	 * we would if we calculated <tt>e^a</tt> or <tt>e^b</tt> directly.
	 * <p>
	 * Returns <tt>NaN</tt> if b > a (so that log(e^a - e^b) is undefined).
	 */
	public static double subtractLogProb(double a, double b) {
		if (b == Double.NEGATIVE_INFINITY)
			return a;
		else
			return a + Math.log(1 - Math.exp(b - a));
	}

	public static double sum(double[] x) {
		double ret = 0;
		for (int i = 0; i < x.length; i++) {
			ret += x[i];
		}
		return ret;
	}

	/**
	 * Maths.java in mallet
	 * 
	 * Returns the sum of two doubles expressed in log space, that is,
	 * 
	 * <pre>
	 *    sumLogProb = log (e^a + e^b)
	 *               = log e^a(1 + e^(b-a))
	 *               = a + log (1 + e^(b-a))
	 * </pre>
	 * 
	 * By exponentiating <tt>b-a</tt>, we obtain better numerical precision than
	 * we would if we calculated <tt>e^a</tt> or <tt>e^b</tt> directly.
	 * <P>
	 * Note: This function is just like
	 * {@link cc.mallet.fst.Transducer#sumNegLogProb sumNegLogProb} in
	 * <TT>Transducer</TT>, except that the logs aren't negated.
	 */
	public static double sumLogProb(double a, double b) {
		if (a == Double.NEGATIVE_INFINITY)
			return b;
		else if (b == Double.NEGATIVE_INFINITY)
			return a;
		else if (b < a)
			return a + Math.log(1 + Math.exp(b - a));
		else
			return b + Math.log(1 + Math.exp(a - b));
	}

	/**
	 * Below from Stanford NLP package, SloppyMath.java
	 * 
	 * Sums an array of numbers log(x1)...log(xn). This saves some of the
	 * unnecessary calls to Math.log in the two-argument version.
	 * <p>
	 * Note that this implementation IGNORES elements of the x array that are
	 * more than LOGTOLERANCE (currently 30.0) less than the maximum element.
	 * <p>
	 * Cursory testing makes me wonder if this is actually much faster than
	 * repeated use of the 2-argument version, however -cas.
	 * 
	 * @param x
	 *            An array log(x1), log(x2), ..., log(xn)
	 * @return log(x1+x2+...+xn)
	 */
	public static double sumLogProb(double[] x) {
		double max = Double.NEGATIVE_INFINITY;
		int len = x.length;
		int maxIndex = 0;

		for (int i = 0; i < len; i++) {
			if (x[i] > max) {
				max = x[i];
				maxIndex = i;
			}
		}

		boolean anyAdded = false;
		double intermediate = 0.0;
		double cutoff = max - LOGTOLERANCE;

		for (int i = 0; i < maxIndex; i++) {
			if (x[i] >= cutoff) {
				anyAdded = true;
				intermediate += Math.exp(x[i] - max);
			}
		}
		for (int i = maxIndex + 1; i < len; i++) {
			if (x[i] >= cutoff) {
				anyAdded = true;
				intermediate += Math.exp(x[i] - max);
			}
		}

		if (anyAdded) {
			return max + Math.log(1.0 + intermediate);
		} else {
			return max;
		}
	}

	public static void unitVector(double[] x) {
		scale(x, normL2(x));
	}

	public static double[] unitVectorTo(double[] x) {
		return scaleTo(x, normL2(x));
	}

	public double sumAfterLogProb(double[] x) {
		return sumLogProb(logTo(x));
	}
}

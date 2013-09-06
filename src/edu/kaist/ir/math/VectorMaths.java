package edu.kaist.ir.math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import edu.kaist.ir.matrix.DenseMatrix;
import edu.kaist.ir.matrix.DenseVector;
import edu.kaist.ir.matrix.Matrix;
import edu.kaist.ir.matrix.SparseMatrix;
import edu.kaist.ir.matrix.SparseVector;
import edu.kaist.ir.matrix.Vector;
import edu.kaist.ir.utils.ArrayUtils;

/**
 * @author Heung-Seon Oh
 * 
 */
public class VectorMaths {

	public static void accumulate(Vector x) {
		double sum = 0;
		for (int i = 0; i < x.size(); i++) {
			sum += x.valueAtLoc(i);
			x.setAtLoc(i, sum);
		}
		x.setSum(sum);
	}

	public static Vector accumulateTo(Vector x) {
		Vector ret = x.copy();
		accumulate(ret);
		return ret;
	}

	public static void add(Vector x1, Vector x2) {
		addAfterScale(x1, x2, 1, 1);
	}

	public static double geometricMean(Vector x) {
		double logSum = 0;
		for (int i = 0; i < x.size(); i++) {
			int index = x.indexAtLoc(i);
			double value = x.valueAtLoc(index);
			logSum += Math.log(value);
		}
		return Math.exp(logSum / x.size());
	}

	public static void addAfterScale(Vector x1, Vector x2, double factor) {
		addAfterScale(x1, x2, 1, factor);
	}

	public static void addAfterScale(Vector x1, Vector x2, double factor1, double factor2) {
		if (x1.dim() != x2.dim()) {
			new IllegalArgumentException("different dimension");
		}

		if (!isSparse(x1) && !isSparse(x2)) {
			double sum = x1.sum();
			for (int i = 0; i < x1.dim(); i++) {
				double value1 = x1.value(i);
				double value2 = x2.value(i);
				double newValue = factor1 * value1 + factor2 * value2;
				double diff = newValue - value1;
				sum += diff;
				x1.set(i, newValue);
			}
			x1.setSum(sum);
		} else if (!isSparse(x1) && isSparse(x2)) {
			double sum = x1.sum();
			for (int i = 0; i < x2.size(); i++) {
				int index2 = x2.indexAtLoc(i);
				double value2 = x2.valueAtLoc(i);
				double value1 = x1.value(index2);
				double newValue = factor1 * value1 + factor2 * value2;
				double diff = newValue - value1;
				sum += diff;
				x1.set(index2, newValue);
			}
			x1.setSum(sum);
		} else if (isSparse(x1) && !isSparse(x2)) {
			throw new UnsupportedOperationException();
		} else {
			throw new UnsupportedOperationException();
		}
	}

	public static void addAfterScale(Vector x1, Vector x2, double factor1, double factor2, DenseVector y) {
		if (x1.dim() != x2.dim() && x2.dim() != y.dim()) {
			new IllegalArgumentException("different dimension");
		}

		double sum = 0;
		int i = 0, j = 0;
		while (i < x1.size() && j < x2.size()) {
			int index1 = x1.indexAtLoc(i);
			int index2 = x2.indexAtLoc(j);
			double value1 = factor1 * x1.valueAtLoc(i);
			double value2 = factor2 * x2.valueAtLoc(j);
			if (index1 == index2) {
				y.set(index1, value1 + value2);
				sum += (value1 + value2);
				i++;
				j++;
			} else if (index1 > index2) {
				y.set(index2, value2);
				sum += value2;
				j++;
			} else if (index1 < index2) {
				y.set(index1, value1);
				sum += value1;
				i++;
			}
		}
		y.setSum(sum);
	}

	public static Vector addAfterScaleTo(Vector x1, Vector x2, double factor) {
		Vector ret = x1.copy();
		addAfterScale(ret, x2, factor);
		return ret;
	}

	public static Vector addAfterScaleTo(Vector x1, Vector x2, double factor1, double factor2) {
		Vector ret = x1.copy();
		addAfterScale(ret, x2, factor1, factor2);
		return ret;
	}

	public static Vector addTo(Vector x1, Vector x2) {
		Vector ret = x1.copy();
		add(ret, x2);
		return ret;
	}

	public static double average(Vector x) {
		return x.sum() / x.size();
	}

	public static void bm25(List<SparseVector> xs) {
		System.out.println("weight by bm25.");
		double k_1 = 1.2d;
		double k_3 = 8d;
		double b = 0.75d;
		double avgDocLen = 0;

		int maxId = 0;
		for (int i = 0; i < xs.size(); i++) {
			SparseVector x = xs.get(i);
			if (x.size() == 0) {
				continue;
			}

			avgDocLen += x.sum();

			int id = xs.get(i).indexAtLoc(x.size() - 1);
			if (id > maxId) {
				maxId = id;
			}
		}

		avgDocLen /= xs.size();

		DenseVector term_docFreq = new DenseVector(maxId + 1);

		for (int i = 0; i < xs.size(); i++) {
			SparseVector x = xs.get(i);
			for (int j = 0; j < x.size(); j++) {
				int termId = x.indexAtLoc(j);
				term_docFreq.increment(termId, 1);
			}
		}

		for (int i = 0; i < xs.size(); i++) {
			SparseVector x = xs.get(i);
			double docLen = x.sum();
			double norm = 0;
			for (int j = 0; j < x.size(); j++) {
				int termId = x.indexAtLoc(j);
				double tf = x.valueAtLoc(j);
				double term1 = (tf * (k_1 + 1)) / (tf + k_1 * (1 - b + b * (docLen / avgDocLen)));

				double docFreq = term_docFreq.value(termId);
				double numDocs = xs.size();
				double term2 = Math.log((numDocs - docFreq + 0.5) / (docFreq + 0.5));
				double weight = term1 * term2;

				x.setAtLoc(j, weight);
				norm += weight * weight;
			}
			x.scale(1f / norm);
		}

	}

	public static double cosine(double dotProduct, double norm1, double norm2) {
		double ret = 0;
		if (norm1 > 0 && norm2 > 0) {
			ret = dotProduct / (norm1 * norm2);
		}
		if (ret > 1) {
			ret = 1;
		}
		if (ret < 0) {
			ret = 0;
		}
		return ret;
	}

	public static double cosine(Vector x1, Vector x2, boolean normalizeBefore) {
		if (x1.dim() != x2.dim()) {
			new IllegalArgumentException("different dimension");
		}

		double norm1 = 0;
		double norm2 = 0;
		double dotProduct = 0;

		int i = 0, j = 0;
		while (i < x1.size() && j < x2.size()) {
			int index1 = x1.indexAtLoc(i);
			int index2 = x2.indexAtLoc(j);

			double value1 = normalizeBefore ? x1.probAtLoc(i) : x1.valueAtLoc(i);
			double value2 = normalizeBefore ? x2.probAtLoc(j) : x2.valueAtLoc(j);

			if (index1 == index2) {
				dotProduct += (value1 * value2);
				norm1 += value1 * value1;
				norm2 += value2 * value2;
				i++;
				j++;
			} else if (index1 > index2) {
				norm2 += value2 * value2;
				j++;
			} else if (index1 < index2) {
				norm1 += value1 * value1;
				i++;
			}
		}

		norm1 = Math.sqrt(norm1);
		norm2 = Math.sqrt(norm2);
		double cosine = cosine(dotProduct, norm1, norm2);
		return cosine;
	}

	public static void distribute(Vector x, double sum) {
		double portionSum = 0;
		for (int i = 0; i < x.size(); i++) {
			int index = x.indexAtLoc(i);
			double prob = x.sum() == 1 ? x.valueAtLoc(i) : x.probAtLoc(i);
			double portion = sum * prob;
			x.setAtLoc(i, portion);
			portionSum += portion;
		}
		x.setSum(portionSum);
	}

	public static Vector distributeTo(Vector x, double sum) {
		Vector ret = x.copy();
		distribute(x, sum);
		return ret;
	}

	public static double dotProduct(Vector x1, Vector x2, boolean normalizeBefore) {
		if (x1.dim() != x2.dim()) {
			new IllegalArgumentException("different dimension");
		}
		double dotProduct = 0;
		if (isSparse(x1) && isSparse(x2)) {
			int i = 0, j = 0;
			while (i < x1.size() && j < x2.size()) {
				int index1 = x1.indexAtLoc(i);
				int index2 = x1.indexAtLoc(j);
				double value1 = normalizeBefore ? x1.probAtLoc(i) : x1.valueAtLoc(i);
				double value2 = normalizeBefore ? x2.probAtLoc(j) : x2.valueAtLoc(j);
				if (index1 == index2) {
					dotProduct += (value1 * value2);
					i++;
					j++;
				} else if (index1 > index2) {
					j++;
				} else if (index1 < index2) {
					i++;
				}
			}
		} else if (!isSparse(x1) && !isSparse(x2)) {
			for (int i = 0; i < x1.dim(); i++) {
				dotProduct += x1.value(i) * x2.value(i);
			}
		} else {
			DenseVector dense = null;
			SparseVector sparse = null;

			if (isSparse(x1)) {
				dense = (DenseVector) x2;
				sparse = (SparseVector) x1;
			} else {
				dense = (DenseVector) x1;
				sparse = (SparseVector) x2;
			}

			for (int i = 0; i < sparse.size(); i++) {
				int index2 = sparse.indexAtLoc(i);
				double value2 = sparse.valueAtLoc(i);
				double value1 = dense.value(index2);
				dotProduct += value1 * value2;
			}
		}
		return dotProduct;
	}

	public static double entropy(Vector x) {
		double ret = 0;
		for (int i = 0; i < x.size(); i++) {
			double prob = x.sum() == 1 ? x.valueAtLoc(i) : x.probAtLoc(i);
			if (prob > 0 && prob < 1) {
				ret += prob * MyMath.log2(prob);
			}
		}
		return ret;
	}

	public static double euclideanDistance(Vector x1, Vector x2, boolean normalizeBefore) {
		double ret = 0;
		int i = 0, j = 0;
		while (i < x1.size() && j < x2.size()) {
			int index1 = x1.indexAtLoc(i);
			int index2 = x1.indexAtLoc(j);
			double value1 = normalizeBefore ? x1.probAtLoc(i) : x1.valueAtLoc(i);
			double value2 = normalizeBefore ? x2.probAtLoc(i) : x2.valueAtLoc(j);
			double diff = 0;
			if (index1 == index2) {
				diff = value1 - value2;
				i++;
				j++;
			} else if (index1 > index2) {
				diff = -value2;
				j++;
			} else if (index1 < index2) {
				diff = value1;
				i++;
			}
			ret += diff * diff;
		}
		ret = Math.sqrt(ret);
		return ret;
	}

	public static boolean isSparse(Matrix x) {
		return x instanceof SparseMatrix;
	}

	public static boolean isSparse(Vector x) {
		return x instanceof SparseVector;
	}

	public static double jsDivergence(Vector x1, Vector x2) {
		return lambdaDivergence(x1, x2, 0.5);
	}

	public static double klDivergence(Vector x1, Vector x2, boolean symmetric) {
		double ret = 0;
		int i = 0, j = 0;

		while (i < x1.size() && j < x2.size()) {
			int index1 = x1.indexAtLoc(i);
			int index2 = x2.indexAtLoc(j);

			if (index1 == index2) {
				double value1 = x1.sum() == 1 ? x1.valueAtLoc(i) : x1.probAtLoc(i);
				double value2 = x2.sum() == 1 ? x2.valueAtLoc(j) : x2.probAtLoc(j);
				if (value1 != value2 && value1 > 0 && value2 > 0) {
					double div = 0;
					if (symmetric) {
						div = value1 * Math.log(value1 / value2) + value2 * Math.log(value2 / value1);
					} else {
						div = value1 * Math.log(value1 / value2);
					}
					ret += div;
				}
				i++;
				j++;
			} else if (index1 > index2) {
				j++;
			} else if (index1 < index2) {
				i++;
			}
		}

		return ret;
	}

	public static double lambdaDivergence(Vector x1, Vector x2, double lambda) {
		double ret = 0;
		int i = 0, j = 0;

		while (i < x1.size() && j < x2.size()) {
			int index1 = x1.indexAtLoc(i);
			int index2 = x2.indexAtLoc(j);

			if (index1 == index2) {
				double value1 = x1.sum() == 1 ? x1.valueAtLoc(i) : x1.probAtLoc(i);
				double value2 = x2.sum() == 1 ? x2.valueAtLoc(j) : x2.probAtLoc(j);
				if (value1 != value2 && value1 > 0 && value2 > 0) {
					double value3 = lambda * value1 + (1 - lambda) * value2;
					double term1 = value1 * Math.log(value1 / value3);
					double term2 = value2 * Math.log(value2 / value3);
					double div = lambda * term1 + (1 - lambda) * term2;
					ret += div;
				}
				i++;
				j++;
			} else if (index1 > index2) {
				j++;
			} else if (index1 < index2) {
				i++;
			}
		}

		return ret;
	}

	public static void main(String[] args) {
		System.out.println("process begins.");

		int[] indexes = { 0, 2, 3, 4, 5 };
		double[] values = { 2, 2, 2, 2, 2 };

		int[] indexes2 = { 1, 3, 4, 7, 8 };
		double[] values2 = { 2, 2, 0, 0, 1, };

		Vector x1 = new SparseVector(indexes, values, -1);
		Vector x2 = new SparseVector(indexes2, values2, -1);

		x1.summation();
		x2.summation();

		pointwiseMultiply(x1, x2);

		System.out.println(x1.toString());
		System.out.println(x2.toString());
		System.out.println();

		addAfterScale(x1, x2, 0.5, -0.5);

		// System.out.println(cosine(x1, x2, true));

		System.out.println(x1.toString());
		System.out.println(x2.toString());

		// dist1.summation();
		// dist2.summation();

		System.out.println("process ends.");

	}

	public static double mean(Vector x) {
		return x.sum() / x.size();
	}

	public static void multiply(Matrix x1, Matrix x2, Matrix y) {
		if (x1.colDim() != x2.rowDim()) {
			new IllegalArgumentException("different dimension");
		}
		if (x1.rowDim() != y.rowDim() || x2.colDim() != y.colDim()) {
			new IllegalArgumentException("different dimension");
		}

		if (!isSparse(x1) && !isSparse(x2)) {
			DenseMatrix m1 = (DenseMatrix) x1;
			DenseMatrix m2 = (DenseMatrix) x2;
			DenseMatrix m3 = (DenseMatrix) y;

			for (int j = 0; j < x2.colDim(); j++) {
				Vector column = x2.column(j);
				for (int i = 0; i < x1.rowDim(); i++) {
					Vector row = x1.row(i);
					double dotProduct = dotProduct(row, column, false);
					y.set(i, j, dotProduct);
				}
			}
		} else {

		}
	}

	public static void multiply(Matrix x1, Vector x2, Vector y) {
		if (x1.colDim() != x2.dim() && x2.dim() != y.dim()) {
			new IllegalArgumentException("different dimension");
		}

		if (!isSparse(x2) && isSparse(y)) {
			new IllegalArgumentException("different type");
		}

		if (!isSparse(x1) && !isSparse(x2)) {
			DenseMatrix m1 = (DenseMatrix) x1;
			DenseVector m2 = (DenseVector) x2;
			DenseVector m3 = (DenseVector) y;
			double sum = 0;
			for (int i = 0; i < m1.rowDim(); i++) {
				double dotProduct = dotProduct(m1.row(i), m2, false);
				m3.set(i, dotProduct);
				sum += dotProduct;
			}
			m3.setSum(sum);
		} else if (isSparse(x1) && isSparse(x2)) {
			SparseMatrix m1 = (SparseMatrix) x1;
			SparseVector m2 = (SparseVector) x2;
			SparseVector m3 = (SparseVector) y;
			double sum = 0;
			for (int i = 0; i < m1.rowSize(); i++) {
				int rowId = m1.indexAtRowLoc(i);
				Vector row = m1.vectorAtRowLoc(i);
				double dotProduct = dotProduct(row, m2, false);
				m3.setAtLoc(i, rowId, dotProduct);
				sum += dotProduct;
			}
			m3.setSum(sum);
		}
	}

	public static Vector multiplyTo(Matrix x1, Vector x2) {
		Vector ret = null;
		if (isSparse(x1)) {
			ret = new SparseVector(x1.rowSize(), x2.label());
			ret.setDim(x1.rowDim());
			ret.setIndexes(ArrayUtils.copy(x1.rowIndexes()));
		} else {
			ret = new DenseVector(x1.rowDim(), x2.label());
		}
		multiply(x1, x2, ret);
		return ret;
	}

	public static double normL1(Vector x, boolean normalizeBefore) {
		double ret = 0;
		for (int i = 0; i < x.size(); i++) {
			double value = normalizeBefore ? x.probAtLoc(i) : x.valueAtLoc(i);
			ret += Math.abs(value);
		}
		return ret;
	}

	public static double normL2(Vector x, boolean normalizeBefore) {
		double ret = 0;
		for (int i = 0; i < x.size(); i++) {
			double value = normalizeBefore ? x.probAtLoc(i) : x.valueAtLoc(i);
			ret += (value * value);
		}
		ret = Math.sqrt(ret);
		return ret;
	}

	public static void pointwiseMultiply(Matrix x1, Matrix x2) {
		if (x1.rowDim() != x2.rowDim() || x1.colDim() != x2.colDim()) {
			new IllegalArgumentException("different dimension");
		}

		if (isSparse(x1) && isSparse(x2)) {
			SparseMatrix m1 = (SparseMatrix) x1;
			SparseMatrix m2 = (SparseMatrix) x2;
			for (int i = 0; i < m1.rowSize(); i++) {
				int rowId = m1.indexAtRowLoc(i);
				Vector row1 = m1.vectorAtRowLoc(i);
				Vector row2 = m2.rowAlways(rowId);

				if (row2 == null) {
					row1.setAll(0);
				} else {
					pointwiseMultiply(row1, row2);
				}
			}
		} else if (!isSparse(x1) && !isSparse(x2)) {
			DenseMatrix m1 = (DenseMatrix) x1;
			DenseMatrix m2 = (DenseMatrix) x2;
			for (int i = 0; i < m1.rowDim(); i++) {
				pointwiseMultiply(m1.row(i), m2.row(i));
			}
		} else if (!isSparse(x1) && isSparse(x2)) {
			DenseMatrix m1 = (DenseMatrix) x1;
			SparseMatrix m2 = (SparseMatrix) x2;

			for (int i = 0; i < m1.rowDim(); i++) {
				Vector row1 = m1.row(i);
				Vector row2 = m2.rowAlways(i);
				if (row2 == null) {
					row1.setAll(0);
				} else {
					pointwiseMultiply(row1, row2);
				}
			}
		} else if (isSparse(x1) && !isSparse(x2)) {
			SparseMatrix m1 = (SparseMatrix) x1;
			DenseMatrix m2 = (DenseMatrix) x2;

			for (int i = 0; i < m1.rowSize(); i++) {
				int rowId = m1.indexAtRowLoc(i);
				Vector row1 = m1.vectorAtRowLoc(i);
				Vector row2 = m2.row(rowId);
				pointwiseMultiply(row1, row2);
			}
		}
	}

	public static void pointwiseMultiply(Vector x1, Vector x2) {
		if (x1.dim() != x2.dim()) {
			new IllegalArgumentException("different dimension");
		}

		double sum = 0;
		int i = 0, j = 0;
		while (i < x1.size() && j < x2.size()) {
			int index1 = x1.indexAtLoc(i);
			int index2 = x2.indexAtLoc(j);
			double value1 = x1.valueAtLoc(i);
			double value2 = x2.valueAtLoc(j);
			if (index1 == index2) {
				x1.setAtLoc(i, value1 * value2);
				sum += x1.valueAtLoc(i);
				i++;
				j++;
			} else if (index1 > index2) {
				j++;
			} else if (index1 < index2) {
				x1.setAtLoc(i, 0);
				i++;
			}
		}
		x1.setSum(sum);
	}

	public static SparseVector rank(Vector x) {
		SparseVector ret = null;
		if (isSparse(x)) {
			ret = (SparseVector) x.copy();
			ret.sortByValue();
			for (int i = 0; i < ret.size(); i++) {
				ret.setAtLoc(i, i + 1);
			}
			ret.setSum(0);
		} else {
			SparseVector m = ((DenseVector) x).toSparseVector();
			m.sortByValue();

			ret = m.copy();
			for (int i = 0; i < m.size(); i++) {
				ret.setAtLoc(i, i + 1);
			}
			ret.setSum(0);
		}
		return ret;
	}

	public static int[] sample(DenseVector x, int sampleSize) {
		return ArrayMaths.sample(x.values(), sampleSize);
	}

	public static int[] sample(Vector x, int sampleSize) {
		Random radom = new Random();
		double[] accValues = new double[x.size()];
		double valueSum = 0;

		for (int i = 0; i < x.size(); i++) {
			valueSum += x.valueAtLoc(i);
			accValues[i] = valueSum;
		}

		int[] ret = new int[sampleSize];
		for (int i = 0; i < sampleSize; i++) {
			double randomValue = radom.nextDouble();

			if (x.sum() != 1) {
				randomValue *= x.sum();
			}

			for (int j = 0; j < x.size(); j++) {
				double accValue = accValues[j];
				if (randomValue <= accValue) {
					ret[i] = x.indexAtLoc(j);
					break;
				}
			}
		}
		return ret;
	}

	public static void scale(List<Vector> xs, double upper, double lower) {
		int max_index = Integer.MIN_VALUE;

		for (int i = 0; i < xs.size(); i++) {
			Vector x = xs.get(i);
			for (int j = 0; j < x.size(); j++) {
				int index = x.indexAtLoc(j);
				if (index > max_index) {
					max_index = index;
				}
			}
		}

		double[] feature_max = new double[max_index + 1];
		double[] feature_min = new double[max_index + 1];

		Arrays.fill(feature_max, Double.MIN_VALUE);
		Arrays.fill(feature_min, Double.MAX_VALUE);

		for (int i = 0; i < xs.size(); i++) {
			Vector x = xs.get(i);
			for (int j = 0; j < x.size(); j++) {
				int index = x.indexAtLoc(j);
				double value = x.valueAtLoc(j);
				feature_max[index] = Math.max(value, feature_max[index]);
				feature_min[index] = Math.min(value, feature_min[index]);
			}
		}

		for (int i = 0; i < xs.size(); i++) {
			Vector x = xs.get(i);
			for (int j = 0; j < x.size(); j++) {
				int index = x.indexAtLoc(j);
				double value = x.valueAtLoc(j);

				double max = feature_max[index];
				double min = feature_min[index];

				if (max == min) {

				} else if (value == min) {

				} else if (value == max) {

				} else {
					value = lower + (upper - lower) * (value - min) / (max - min);
				}
				x.setAtLoc(j, index, value);
			}
		}
	}

	public static void subtract(Vector x1, Vector x2) {
		addAfterScale(x1, x2, 1, -1);
	}

	public static void subtractAfterScale(Vector x1, Vector x2, double factor1, double factor2) {
		addAfterScale(x1, x2, factor1, -factor2);
	}

	public static void tfidf(List<SparseVector> xs) {
		System.out.println("weight by tfidf.");
		int maxId = 0;

		for (int i = 0; i < xs.size(); i++) {
			SparseVector x = xs.get(i);
			if (x.size() == 0) {
				continue;
			}

			int id = xs.get(i).indexAtLoc(x.size() - 1);
			if (id > maxId) {
				maxId = id;
			}
		}

		DenseVector term_docFreq = new DenseVector(maxId + 1);

		for (int i = 0; i < xs.size(); i++) {
			SparseVector x = xs.get(i);
			for (int j = 0; j < x.size(); j++) {
				int termId = x.indexAtLoc(j);
				term_docFreq.increment(termId, 1);
			}
		}

		for (int i = 0; i < xs.size(); i++) {
			SparseVector x = xs.get(i);
			double norm = 0;
			for (int j = 0; j < x.size(); j++) {
				int termId = x.indexAtLoc(j);
				double count = x.valueAtLoc(j);
				double docFreq = term_docFreq.value(termId);
				double numDocs = xs.size();
				double tf = 1 + (count == 0 ? 0 : Math.log(count));
				double idf = Math.log((numDocs + 1) / docFreq);
				double tfidf = tf * idf;
				x.setAtLoc(j, tfidf);
				norm += tfidf * tfidf;
			}
			x.scale(1f / norm);
		}
	}

	public static List<SparseVector> tfidfTo(List<SparseVector> xs) {
		List<SparseVector> ret = new ArrayList<SparseVector>();
		for (SparseVector x : xs) {
			ret.add(x.copy());
		}
		tfidf(ret);
		return ret;
	}

	public static void unitVector(Vector x) {
		double norm = normL2(x, false);
		double sum = 0;
		for (int i = 0; i < x.size(); i++) {
			double value = x.valueAtLoc(i);
			value /= norm;
			x.setAtLoc(i, value);
			sum += value;
		}
		x.setSum(sum);
	}

	public static double variance(Vector x) {
		return variance(x, mean(x));
	}

	public static double variance(Vector x, double mean) {
		double ret = 0;
		for (int i = 0; i < x.size(); i++) {
			double diff = x.valueAtLoc(i) - mean;
			ret += diff * diff;
		}
		ret /= x.size();
		return ret;
	}

}

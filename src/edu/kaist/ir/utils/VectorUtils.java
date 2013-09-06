package edu.kaist.ir.utils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import edu.kaist.ir.matrix.DenseMatrix;
import edu.kaist.ir.matrix.DenseVector;
import edu.kaist.ir.matrix.SparseMatrix;
import edu.kaist.ir.matrix.SparseVector;

public class VectorUtils {
	public static SparseVector freqOfFreq(DenseVector x) {
		Counter<Integer> counter = new Counter<Integer>();
		for (int i = 0; i < x.size(); i++) {
			int freq = (int) x.value(i);
			counter.incrementCount(freq, 1);
		}
		SparseVector ret = toSparseVector(counter);
		ret.setLabel(x.label());
		return ret;
	}

	public static void subVector(SparseVector x, int[] subset) {
		List<Integer> indexList = new ArrayList<Integer>();
		List<Double> valueList = new ArrayList<Double>();
		double sum = 0;

		for (int index : subset) {
			int loc = x.location(index);
			if (loc < 0) {
				continue;
			}

			double value = x.valueAtLoc(loc);
			indexList.add(index);
			valueList.add(value);
			sum += value;
		}

		int[] indexes = ArrayUtils.integerArray(indexList);
		double[] values = ArrayUtils.doubleArray(valueList);

		x.setIndexes(indexes);
		x.setValues(values);
		x.setSum(sum);
	}

	public static void copyValues(DenseVector x, DenseVector y) {
		ArrayUtils.copy(x.values(), y.values());
		y.setSum(x.sum());
	}

	public static SparseVector subVectorTo(SparseVector x, int[] subset) {
		List<Integer> indexList = new ArrayList<Integer>();
		List<Double> valueList = new ArrayList<Double>();

		for (int i = 0; i < subset.length; i++) {
			int index = subset[i];
			int loc = x.location(index);
			if (loc < 0) {
				continue;
			}

			double value = x.valueAtLoc(loc);
			indexList.add(index);
			valueList.add(value);
		}

		return VectorUtils.toSparseVector(indexList, valueList);
	}

	public static Counter<String> toCounter(Counter<Integer> x, Indexer<String> indexer) {
		Counter<String> ret = new Counter<String>();
		for (int index : x.keySet()) {
			double value = x.getCount(index);
			String obj = indexer.getObject(index);
			if (obj == null) {
				continue;
			}
			ret.setCount(obj, value);
		}
		return ret;
	}

	public static Counter<String> toCounter(DenseVector x, Indexer<String> indexer) {
		Counter<String> ret = new Counter<String>();
		for (int i = 0; i < x.size(); i++) {
			double value = x.value(i);
			String obj = indexer.getObject(i);
			ret.setCount(obj, value);
		}
		return ret;
	}

	public static Counter<Integer> toCounter(SparseVector x) {
		Counter<Integer> ret = new Counter<Integer>();
		for (int i = 0; i < x.size(); i++) {
			int index = x.indexAtLoc(i);
			if (index < 0) {
				continue;
			}
			double value = x.valueAtLoc(i);
			ret.setCount(index, value);
		}
		return ret;
	}

	public static Counter<String> toCounter(SparseVector x, Indexer<String> indexer) {
		Counter<String> ret = new Counter<String>();

		for (int i = 0; i < x.size(); i++) {
			int index = x.indexAtLoc(i);
			if (index < 0) {
				continue;
			}
			double value = x.valueAtLoc(i);
			String obj = indexer.getObject(index);

			ret.setCount(obj, value);
		}
		return ret;
	}

	public static DenseMatrix toDenseMatrix(CounterMap<String, String> counterMap, Indexer<String> rowIndexer, Indexer<String> colIndexer) {
		DenseMatrix ret = new DenseMatrix(rowIndexer.size(), colIndexer.size());

		for (String key1 : counterMap.keySet()) {
			int rowId = rowIndexer.indexOf(key1);
			DenseVector vector = ret.row(rowId);
			double sum = 0;

			Counter<String> counter = counterMap.getCounter(key1);
			for (String key2 : counter.keySet()) {
				int colId = colIndexer.indexOf(key2);
				double value = counter.getCount(key2);
				vector.set(colId, value);
				sum += value;
			}
			vector.setSum(sum);
		}
		ret.rowSummation();
		return ret;
	}

	public static DenseVector toDenseVector(Counter<String> counter, Indexer<String> indexer) {
		DenseVector ret = new DenseVector(indexer.size(), -1);
		double sum = 0;
		for (String key : counter.keySet()) {
			int keyId = indexer.indexOf(key);
			double value = counter.getCount(key);
			ret.set(keyId, value);
			sum += value;
		}
		ret.setSum(sum);
		return ret;
	}

	public static IndexedList<Integer, SparseVector> toIndexedList(List<SparseVector> xs) {
		IndexedList<Integer, SparseVector> ret = new IndexedList<Integer, SparseVector>();
		for (int i = 0; i < xs.size(); i++) {
			SparseVector vector = xs.get(i);
			ret.put(vector.label(), vector);
		}
		return ret;
	}

	public static String toRankedString(SparseVector x, Indexer<String> indexer) {
		StringBuffer sb = new StringBuffer();

		x.sortByValue();
		x.reverse();

		for (int i = 0; i < x.size(); i++) {
			int index = x.indexAtLoc(i);
			int rank = (int) x.valueAtLoc(i);
			sb.append(String.format(" %s:%d", indexer.getObject(index), rank));
		}
		return sb.toString().trim();
	}

	public static SparseVector toSparseVector(Counter<Integer> x) {
		List<Integer> indexList = new ArrayList<Integer>();
		List<Double> valueList = new ArrayList<Double>();
		for (Entry<Integer, Double> entry : x.entrySet()) {
			int index = entry.getKey();
			double value = entry.getValue();
			indexList.add(index);
			valueList.add(value);
		}
		return toSparseVector(indexList, valueList);
	}

	public static SparseVector toSparseVector(Counter<String> x, Indexer<String> indexer) {
		List<Integer> indexList = new ArrayList<Integer>();
		List<Double> valueList = new ArrayList<Double>();

		for (Entry<String, Double> entry : x.entrySet()) {
			String key = entry.getKey();
			double value = entry.getValue();
			int index = indexer.indexOf(key);

			if (index < 0) {
				continue;
			}

			indexList.add(index);
			valueList.add(value);
		}

		return toSparseVector(indexList, valueList);
	}

	public static SparseVector toSparseVector(List<Integer> indexList, List<Double> valueList) {
		SparseVector ret = new SparseVector(indexList.size());
		double sum = 0;
		for (int i = 0; i < indexList.size(); i++) {
			int index = indexList.get(i);
			double value = valueList.get(i);
			ret.setAtLoc(i, index, value);
			sum += value;
		}
		ret.sortByIndex();
		ret.setSum(sum);
		return ret;
	}

	public static SparseMatrix toSpasreMatrix(CounterMap<Integer, Integer> counterMap) {
		int[] indexes = new int[counterMap.keySet().size()];
		SparseVector[] rowVectors = new SparseVector[indexes.length];
		int loc = 0;

		for (int index : counterMap.keySet()) {
			indexes[loc] = index;
			rowVectors[loc] = toSparseVector(counterMap.getCounter(index));
			rowVectors[loc].setLabel(index);
			loc++;
		}

		SparseMatrix ret = new SparseMatrix(-1, -1, -1, indexes, rowVectors);
		ret.sortByRowIndex();
		return ret;
	}

	public static SparseMatrix toSpasreMatrix(CounterMap<String, String> counterMap, Indexer<String> rowIndexer, Indexer<String> colIndexer) {
		int[] rowIndexes = new int[counterMap.keySet().size()];
		SparseVector[] rowVectors = new SparseVector[rowIndexes.length];
		int loc = 0;

		for (String key1 : counterMap.keySet()) {
			int index1 = rowIndexer.indexOf(key1);
			SparseVector rowVec = toSparseVector(counterMap.getCounter(key1), colIndexer);
			rowVec.setLabel(index1);
			rowVec.setDim(colIndexer.size());
			rowIndexes[loc] = index1;
			rowVectors[loc] = rowVec;
			loc++;
		}
		SparseMatrix ret = new SparseMatrix(rowIndexer.size(), colIndexer.size(), -1, rowIndexes, rowVectors);
		ret.sortByRowIndex();
		return ret;
	}

	public static String toSVMFormat(SparseVector x, NumberFormat nf) {
		if (nf == null) {
			nf = NumberFormat.getInstance();
			nf.setMinimumFractionDigits(8);
			nf.setGroupingUsed(false);
		}

		StringBuffer sb = new StringBuffer();
		sb.append(x.label());
		for (int i = 0; i < x.size(); i++) {
			sb.append(String.format(" %d:%s", x.indexAtLoc(i), nf.format(x.valueAtLoc(i))));
		}
		return sb.toString();
	}
}

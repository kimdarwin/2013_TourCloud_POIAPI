package edu.kaist.ir.matrix;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.kaist.ir.io.IOUtils;
import edu.kaist.ir.utils.Counter;
import edu.kaist.ir.utils.CounterMap;

public class SparseMatrix implements Matrix {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3542638642565119292L;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

	public static SparseMatrix read(File inputFile) throws Exception {
		System.out.printf("read [%s].\n", inputFile.getPath());
		ObjectInputStream ois = IOUtils.openObjectInputStream(inputFile);
		SparseMatrix ret = readStream(ois);
		ois.close();
		return ret;
	}

	public static List<SparseMatrix> readList(File inputFile) throws Exception {
		System.out.printf("read [%s].\n", inputFile.getPath());
		List<SparseMatrix> ret = new ArrayList<SparseMatrix>();

		ObjectInputStream ois = IOUtils.openObjectInputStream(inputFile);
		int size = ois.readInt();
		for (int i = 0; i < size; i++) {
			SparseMatrix mat = readStream(ois);
			ret.add(mat);
		}
		ois.close();
		System.out.printf("read [%d] matrices.\n", ret.size());
		return ret;
	}

	public static SparseMatrix readStream(ObjectInputStream ois) throws Exception {
		int rowDim = ois.readInt();
		int colDim = ois.readInt();
		int label = ois.readInt();
		int rowSize = ois.readInt();

		int[] rowIndexes = new int[rowSize];
		SparseVector[] rowVectors = new SparseVector[rowSize];

		for (int i = 0; i < rowSize; i++) {
			rowIndexes[i] = ois.readInt();
			rowVectors[i] = SparseVector.readStream(ois);
		}
		SparseMatrix ret = new SparseMatrix(rowDim, colDim, label, rowIndexes, rowVectors);
		return ret;
	}

	public static void write(File outputFile, List<SparseMatrix> mats) throws Exception {
		System.out.printf("write to [%s].\n", outputFile.getPath());

		ObjectOutputStream oos = IOUtils.openObjectOutputStream(outputFile);
		oos.writeInt(mats.size());
		for (int i = 0; i < mats.size(); i++) {
			SparseMatrix vector = mats.get(i);
			vector.write(oos);
		}
		oos.close();

		System.out.printf("write [%d] matrices.\n", mats.size());
	}

	private int rowDim;

	private int colDim;

	private int label;

	private int[] rowIds;

	private SparseVector[] rows;

	public SparseMatrix(int rowDim, int colDim, int label, int[] rowIndexes, SparseVector[] rows) {
		this.rowDim = rowDim;
		this.colDim = colDim;
		this.label = label;
		this.rowIds = rowIndexes;
		this.rows = rows;
		sortByRowIndex();
	}

	public SparseMatrix(int rowDim, int colDim, int label, Map<Integer, SparseVector> entries) {
		this.rowDim = rowDim;
		this.colDim = colDim;
		this.label = label;

		setEntries(entries);
	}

	public SparseMatrix(Map<Integer, SparseVector> entries) {
		this(-1, -1, -1, entries);
	}

	public int colDim() {
		return colDim;
	}

	public SparseVector column(int colId) {
		List<Integer> indexList = new ArrayList<Integer>();
		List<Double> valueList = new ArrayList<Double>();

		for (int i = 0; i < rowSize(); i++) {
			int index = rowIds[i];
			SparseVector row = rows[i];
			int loc = row.location(colId);
			if (loc < 0) {
				continue;
			}
			double value = row.valueAtLoc(loc);
			indexList.add(index);
			valueList.add(value);
		}

		int[] indexes = new int[indexList.size()];
		double[] values = new double[valueList.size()];
		double sum = 0;
		for (int i = 0; i < indexList.size(); i++) {
			indexes[i] = indexList.get(i);
			values[i] = valueList.get(i);
			sum += values[i];
		}
		SparseVector ret = new SparseVector(indexes, values, label(), rowDim());
		ret.setSum(sum);
		return ret;
	}

	public SparseVector columnSums() {
		Counter<Integer> counter = new Counter<Integer>();

		for (int i = 0; i < rowSize(); i++) {
			SparseVector row = rows[i];
			for (int j = 0; j < row.size(); j++) {
				int colId = row.indexAtLoc(j);
				double value = row.valueAtLoc(j);
				counter.incrementCount(colId, value);
			}
		}
		return new SparseVector(counter, label(), rowDim());
	}

	public SparseMatrix copy() {
		int[] newRowIndexes = new int[rowIds.length];
		SparseVector[] newRowVectors = new SparseVector[rowIds.length];
		for (int i = 0; i < rowIds.length; i++) {
			newRowIndexes[i] = rowIds[i];
			newRowVectors[i] = rows[i].copy();
		}
		return new SparseMatrix(rowDim(), colDim(), label(), newRowIndexes, newRowVectors);
	}

	public Map<Integer, SparseVector> entries() {
		Map<Integer, SparseVector> ret = new HashMap<Integer, SparseVector>();
		for (int i = 0; i < rowIds.length; i++) {
			ret.put(rowIds[i], rows[i]);
		}
		return ret;
	}

	public int indexAtRowLoc(int loc) {
		return rowIds[loc];
	}

	public int label() {
		return label;
	}

	public int locationAtRow(int rowId) {
		return Arrays.binarySearch(rowIds, rowId);
	}

	public void normalizeColumns() {
		SparseVector col_sum = columnSums();

		for (int i = 0; i < rowIds.length; i++) {
			SparseVector row = rows[i];
			for (int j = 0; j < row.size(); j++) {
				int colId = row.indexAtLoc(j);
				double value = row.valueAtLoc(j);
				double sum = col_sum.value(colId);
				if (sum != 0) {
					row.setAtLoc(j, colId, value / sum);
				}
			}
		}
	}

	public void normalizeRows() {
		for (Vector row : rows) {
			row.normalizeAfterSummation();
		}
	}

	private int qPartition(int low, int high) {
		// First element
		// int pivot = a[low];

		// Middle element
		// int middle = (low + high) / 2;

		int i = low - 1;
		int j = high + 1;

		// ascending order
		int randomIndex = (int) (Math.random() * (high - low)) + low;
		int pivotValue = rowIds[randomIndex];

		while (i < j) {
			i++;
			while (rowIds[i] < pivotValue) {
				i++;
			}

			j--;
			while (rowIds[j] > pivotValue) {
				j--;
			}

			if (i < j) {
				swapRows(i, j);
			}
		}
		return j;
	}

	private void qSort(int low, int high) {
		if (low >= high)
			return;
		int p = qPartition(low, high);
		qSort(low, p);
		qSort(p + 1, high);
	}

	private void quicksort() {
		qSort(0, rowIds.length - 1);
	}

	public Vector row(int rowId) {
		int loc = locationAtRow(rowId);
		if (loc < 0) {
			throw new IllegalArgumentException("not found");
		}
		return rows[loc];
	}

	public SparseVector rowAlways(int rowId) {
		SparseVector ret = null;
		int loc = locationAtRow(rowId);
		if (loc > -1) {
			ret = rows[loc];
		}
		return ret;
	}

	public int rowDim() {
		return rowDim;
	}

	public int[] rowIndexes() {
		return rowIds;
	}

	public int rowSize() {
		return rowIds.length;
	}

	public void rowSummation() {
		for (Vector row : rows) {
			row.summation();
		}
	}

	public SparseVector rowSums() {
		SparseVector ret = new SparseVector(rowSize());
		ret.setDim(rowDim());

		double totalSum = 0;
		for (int i = 0; i < rowIds.length; i++) {
			int rowId = rowIds[i];
			SparseVector row = rows[i];
			double sum = row.sum();
			ret.setAtLoc(i, rowId, sum);
			totalSum += sum;
		}
		ret.setSum(totalSum);
		return ret;
	}

	@Override
	public void set(int rowId, int colId, double value) {
		int rowLoc = locationAtRow(rowId);
		if (rowLoc > -1) {
			SparseVector row = (SparseVector) vectorAtRowLoc(rowLoc);
			int colLoc = row.location(colId);
			if (colLoc > -1) {
				row.setAtLoc(colLoc, value);
			}
		}
	}

	public void setAll(double value) {
		for (int i = 0; i < rows.length; i++) {
			rows[i].setAll(value);
		}
	}

	@Override
	public void setColDim(int colDim) {
		this.colDim = colDim;
	}

	public void setEntries(Map<Integer, SparseVector> entries) {
		rowIds = new int[entries.keySet().size()];
		rows = new SparseVector[rowIds.length];
		int loc = 0;

		for (int row : entries.keySet()) {
			SparseVector rowVec = entries.get(row);
			rowIds[loc] = row;
			rows[loc] = rowVec;
			loc++;
		}
		sortByRowIndex();
	}

	public void setLabel(int label) {
		this.label = label;
	}

	public void setRow(int loc, int rowId, SparseVector x) {
		rowIds[loc] = rowId;
		rows[loc] = x;
	}

	public void setRow(int rowId, SparseVector x) {
		int loc = locationAtRow(rowId);
		if (loc > -1) {
			setVectorAtRowLoc(loc, x);
		}
	}

	@Override
	public void setRow(int rowId, Vector x) {
		int rowLoc = locationAtRow(rowId);
		if (rowLoc > -1) {
			setVectorAtRowLoc(rowLoc, x);
		}

	}

	@Override
	public void setRowDim(int rowDim) {
		this.rowDim = rowDim;
	}

	public void setVectorAtRowLoc(int loc, Vector x) {
		rows[loc] = (SparseVector) x;
	}

	public void sortByRowIndex() {
		quicksort();
	}

	private void swapRows(int i, int j) {
		int temp1 = rowIds[i];
		int temp2 = rowIds[j];
		rowIds[i] = temp2;
		rowIds[j] = temp1;

		SparseVector temp3 = rows[i];
		SparseVector temp4 = rows[j];

		rows[i] = temp4;
		rows[j] = temp3;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("[label:\t%d]\n", label()));
		sb.append(String.format("[row dim:\t%d]\n", rowDim()));
		sb.append(String.format("[col dim:\t%d]\n", colDim()));
		for (int i = 0; i < rowIds.length && i < 15; i++) {
			sb.append(String.format("%dth: %s\n", i + 1, rows[i]));
		}
		return sb.toString().trim();
	}

	public SparseMatrix transpose() {
		CounterMap<Integer, Integer> counterMap = new CounterMap<Integer, Integer>();

		for (int i = 0; i < rows.length; i++) {
			int rowIndex = rowIds[i];
			SparseVector row = rows[i];
			for (int j = 0; j < row.size(); j++) {
				int colIndex = row.indexAtLoc(j);
				double value = row.valueAtLoc(j);
				counterMap.incrementCount(colIndex, rowIndex, value);
			}
		}

		int[] rowIds = new int[counterMap.keySet().size()];
		SparseVector[] rows = new SparseVector[rowIds.length];
		int loc = 0;

		for (int rowId : counterMap.keySet()) {
			Counter<Integer> col_value = counterMap.getCounter(rowId);

			int[] ids = new int[col_value.keySet().size()];
			double[] values = new double[ids.length];

			int loc2 = 0;
			for (Entry<Integer, Double> entry : col_value.entrySet()) {
				int colId = entry.getKey();
				double value = entry.getValue();
				ids[loc2] = colId;
				values[loc2] = value;
				loc2++;
			}

			SparseVector row = new SparseVector(ids, values, rowId, rowDim());
			row.sortByIndex();
			rows[loc++] = row;
		}

		return new SparseMatrix(colDim(), rowDim(), label(), rowIds, rows);
	}

	public double value(int rowId, int colId) {
		return row(rowId).value(colId);
	}

	public double valueAlways(int rowId, int colId) {
		double ret = 0;
		int rowLoc = locationAtRow(rowId);
		if (rowLoc > -1) {
			ret = rows[rowLoc].valueAlways(colId);
		}
		return ret;
	}

	public Vector vectorAtRowLoc(int loc) {
		return rows[loc];
	}

	public void write(File outputFile) throws Exception {
		System.out.printf("write to [%s].\n", outputFile.getPath());
		ObjectOutputStream oos = IOUtils.openObjectOutputStream(outputFile);
		write(oos);
		oos.close();
	}

	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeInt(rowDim());
		oos.writeInt(colDim());
		oos.writeInt(label());
		oos.writeInt(rowSize());

		for (int i = 0; i < rowSize(); i++) {
			oos.writeInt(indexAtRowLoc(i));
			vectorAtRowLoc(i).write(oos);
		}
	}
}

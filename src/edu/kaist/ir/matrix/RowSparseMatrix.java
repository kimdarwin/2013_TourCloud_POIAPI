package edu.kaist.ir.matrix;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.kaist.ir.io.IOUtils;

public class RowSparseMatrix implements Matrix {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3542638642565119292L;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

	public static RowSparseMatrix read(File inputFile) throws Exception {
		System.out.printf("read [%s].\n", inputFile.getPath());
		ObjectInputStream ois = IOUtils.openObjectInputStream(inputFile);
		RowSparseMatrix ret = readStream(ois);
		ois.close();
		return ret;
	}

	public static RowSparseMatrix readStream(ObjectInputStream ois) throws Exception {
		int rowDim = ois.readInt();
		int colDim = ois.readInt();
		int label = ois.readInt();

		int[] rowIndexes = IOUtils.readIntegerArray(ois);
		DenseVector[] rowVectors = new DenseVector[rowIndexes.length];
		for (int i = 0; i < rowVectors.length; i++) {
			rowVectors[i] = DenseVector.readStream(ois);
		}
		RowSparseMatrix ret = new RowSparseMatrix(rowDim, colDim, label, rowIndexes, rowVectors);
		return ret;
	}

	private int rowDim;

	private int colDim;

	private int label;

	private int[] rowIds;

	private DenseVector[] rows;

	public RowSparseMatrix(int rowDim, int colDim, int label, int[] rowIds, DenseVector[] rows) {
		this.rowDim = rowDim;
		this.colDim = colDim;
		this.label = label;
		this.rowIds = rowIds;
		this.rows = rows;
	}

	public RowSparseMatrix(int rowDim, int colDim, int label, Map<Integer, DenseVector> entries) {
		this.rowDim = rowDim;
		this.colDim = colDim;
		this.label = label;
		setEntries(entries);
	}

	public RowSparseMatrix(Map<Integer, DenseVector> entries) {
		this(-1, -1, -1, entries);
	}

	public int colDim() {
		return colDim;
	}

	public SparseVector column(int colId) {
		int[] indexes = new int[rowIds.length];
		double[] values = new double[rowIds.length];
		double sum = 0;

		for (int i = 0; i < rowIds.length; i++) {
			indexes[i] = rowIds[i];
			values[i] = rows[i].value(colId);
			sum += values[i];
		}
		SparseVector ret = new SparseVector(indexes, values, colId, rowDim);
		ret.setSum(sum);
		return ret;
	}

	public DenseVector columnSummation() {
		DenseVector ret = new DenseVector(colDim, -1);
		double sum = 0;
		for (int i = 0; i < rows.length; i++) {
			DenseVector row = rows[i];
			for (int j = 0; j < row.size(); j++) {
				double value = row.value(j);
				ret.increment(j, value);
				sum += value;
			}
		}
		ret.setSum(sum);
		return ret;
	}

	public double max() {
		double max = -Double.MAX_VALUE;
		for (int i = 0; i < rows.length; i++) {
			double value = rows[i].max();
			if (max < value) {
				max = value;
			}
		}
		return max;
	}

	public double min() {
		double min = Double.MAX_VALUE;
		for (int i = 0; i < rows.length; i++) {
			double value = rows[i].min();
			if (min > value) {
				min = value;
			}
		}
		return min;
	}

	public RowSparseMatrix copy() {
		int[] newRowIds = new int[rowIds.length];
		DenseVector[] newRows = new DenseVector[rowIds.length];
		for (int i = 0; i < rowIds.length; i++) {
			newRowIds[i] = rowIds[i];
			newRows[i] = rows[i].copy();
		}
		return new RowSparseMatrix(rowDim, colDim, label, newRowIds, newRows);
	}

	public void incrementAll(double value) {
		for (int i = 0; i < rows.length; i++) {
			rows[i].incrementAll(value);
		}
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
		DenseVector col_sum = columnSummation();

		for (int i = 0; i < rowIds.length; i++) {
			DenseVector row = rows[i];
			for (int j = 0; j < row.size(); j++) {
				double value = row.value(j);
				double sum = col_sum.value(j);
				if (sum != 0) {
					row.set(j, value / sum);
				}
			}
		}
	}

	public void normalizeRows() {
		for (DenseVector row : rows) {
			row.summation();
			row.normalize();
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

	public DenseVector row(int rowId) {
		int loc = locationAtRow(rowId);
		if (loc < 0) {
			throw new IllegalArgumentException("not found");
		}
		return rows[loc];
	}

	public DenseVector rowAlways(int row) {
		DenseVector ret = null;
		int loc = locationAtRow(row);
		if (loc > -1) {
			ret = vectorAtRowLoc(loc);
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
		for (DenseVector row : rows) {
			row.summation();
		}
	}

	public SparseVector rowSums() {
		SparseVector ret = new SparseVector(rowSize(), -1);
		ret.setDim(rowDim);
		double sum = 0;
		for (int i = 0; i < rowIds.length; i++) {
			int rowId = rowIds[i];
			DenseVector row = rows[i];
			sum += row.sum();
			ret.setAtLoc(i, rowId, row.sum());
		}
		ret.setSum(sum);
		return ret;
	}

	public void setAll(double value) {
		for (int i = 0; i < rows.length; i++) {
			rows[i].setAll(value);
		}
	}

	public void setAtRowLoc(int loc, int row, DenseVector vector) {
		rowIds[loc] = row;
		rows[loc] = vector;
	}

	public void setEntries(Map<Integer, DenseVector> entries) {
		rowIds = new int[entries.keySet().size()];
		rows = new DenseVector[rowIds.length];
		int loc = 0;
		for (int row : entries.keySet()) {
			rowIds[loc] = row;
			rows[loc] = entries.get(row);
			loc++;
		}

		sortByRowIndex();
	}

	public void setLabel(int label) {
		this.label = label;
	}

	public void setRow(int row, DenseVector vector) {
		int loc = locationAtRow(row);
		if (loc < 0) {

		}
		rowIds[loc] = row;
		rows[loc] = vector;
	}

	public void sortByRowIndex() {
		quicksort();
	}

	private void swapRows(int i, int j) {
		int temp1 = rowIds[i];
		int temp2 = rowIds[j];
		rowIds[i] = temp2;
		rowIds[j] = temp1;

		DenseVector temp3 = rows[i];
		DenseVector temp4 = rows[j];

		rows[i] = temp4;
		rows[j] = temp3;
	}

	public SparseMatrix toSparseMatrix() {
		int[] newRowIndexes = new int[rowSize()];
		SparseVector[] newRowVectors = new SparseVector[rowSize()];

		for (int i = 0; i < rows.length; i++) {
			newRowIndexes[i] = rowIds[i];
			newRowVectors[i] = rows[i].toSparseVector();
		}

		SparseMatrix ret = new SparseMatrix(rowDim, colDim, label, newRowIndexes, newRowVectors);
		return ret;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("[label:\t%d]\n", label));
		sb.append(String.format("[row, col:\t%d, %d]\n", rowDim, colDim));
		sb.append(String.format("[rows in entries:\t%d]\n", rowIds.length));

		for (int i = 0; i < rowIds.length && i < 15; i++) {
			sb.append(String.format("%dth: %s\n", i + 1, rows[i]));
		}

		return sb.toString().trim();
	}

	public RowSparseMatrix transposeTo() {
		Map<Integer, Map<Integer, Double>> col2row = new HashMap<Integer, Map<Integer, Double>>();

		for (int i = 0; i < rows.length; i++) {
			int rowId = rowIds[i];
			DenseVector row = rows[i];
			for (int j = 0; j < row.size(); j++) {
				double value = row.value(j);
				if (value == 0) {
					continue;
				}

				Map<Integer, Double> innerMap = col2row.get(j);
				if (innerMap == null) {
					innerMap = new HashMap<Integer, Double>();
					col2row.put(j, innerMap);
				}
				innerMap.put(rowId, value);
			}
		}

		int[] rowIds = new int[col2row.keySet().size()];
		DenseVector[] rows = new DenseVector[rowIds.length];
		int loc = 0;

		for (int colIndex : col2row.keySet()) {
			Map<Integer, Double> row_value = col2row.get(colIndex);
			rowIds[loc] = colIndex;

			DenseVector row = new DenseVector(colDim, colIndex);

			for (Entry<Integer, Double> entry : row_value.entrySet()) {
				int rowIndex = entry.getKey();
				double value = entry.getValue();
				row.set(rowIndex, value);
			}
			rows[loc++] = row;
		}

		return new RowSparseMatrix(colDim, rowDim, label, rowIds, rows);
	}

	public DenseVector vectorAtRowLoc(int loc) {
		return rows[loc];
	}

	public void write(File outputFile) throws Exception {
		System.out.printf("write to [%s].\n", outputFile.getPath());
		ObjectOutputStream oos = IOUtils.openObjectOutputStream(outputFile);
		write(oos);
		oos.close();
	}

	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeInt(rowDim);
		oos.writeInt(colDim);
		oos.writeInt(label);
		IOUtils.write(oos, rowIds);
		for (int i = 0; i < rows.length; i++) {
			rows[i].write(oos);
		}
	}

	@Override
	public Vector columnSums() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setVectorAtRowLoc(int loc, Vector x) {
		// TODO Auto-generated method stub

	}

	@Override
	public void set(int rowId, int colId, double value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setColDim(int colDim) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setRow(int rowId, Vector x) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setRowDim(int rowDim) {
		// TODO Auto-generated method stub

	}

}

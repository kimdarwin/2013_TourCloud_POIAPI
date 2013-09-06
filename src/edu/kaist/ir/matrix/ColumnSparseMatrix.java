package edu.kaist.ir.matrix;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.kaist.ir.io.IOUtils;
import edu.kaist.ir.utils.Counter;
import edu.kaist.ir.utils.CounterMap;

public class ColumnSparseMatrix implements Matrix {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3542638642565119292L;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

	public static ColumnSparseMatrix read(File inputFile) throws Exception {
		System.out.printf("read [%s].\n", inputFile.getPath());
		ObjectInputStream ois = IOUtils.openObjectInputStream(inputFile);
		ColumnSparseMatrix ret = readStream(ois);
		ois.close();
		return ret;
	}

	public static List<ColumnSparseMatrix> readList(File inputFile) throws Exception {
		System.out.printf("read [%s].\n", inputFile.getPath());
		List<ColumnSparseMatrix> ret = new ArrayList<ColumnSparseMatrix>();

		ObjectInputStream ois = IOUtils.openObjectInputStream(inputFile);
		int size = ois.readInt();
		for (int i = 0; i < size; i++) {
			ColumnSparseMatrix mat = readStream(ois);
			ret.add(mat);
		}
		ois.close();
		System.out.printf("read [%d] matrices.\n", ret.size());
		return ret;
	}

	public static ColumnSparseMatrix readStream(ObjectInputStream ois) throws Exception {
		int colDim = ois.readInt();
		int label = ois.readInt();
		int rowSize = ois.readInt();

		SparseVector[] rows = new SparseVector[rowSize];

		for (int i = 0; i < rowSize; i++) {
			rows[i] = SparseVector.readStream(ois);
		}
		ColumnSparseMatrix ret = new ColumnSparseMatrix(colDim, label, rows);
		return ret;
	}

	public static void write(File outputFile, List<ColumnSparseMatrix> mats) throws Exception {
		System.out.printf("write to [%s].\n", outputFile.getPath());

		ObjectOutputStream oos = IOUtils.openObjectOutputStream(outputFile);
		oos.writeInt(mats.size());
		for (int i = 0; i < mats.size(); i++) {
			ColumnSparseMatrix vector = mats.get(i);
			vector.write(oos);
		}
		oos.close();

		System.out.printf("write [%d] matrices.\n", mats.size());
	}

	private int colDim;

	private int label;

	private SparseVector[] rows;

	public ColumnSparseMatrix(int colDim, int label, SparseVector[] rows) {
		this.colDim = colDim;
		this.label = label;
		this.rows = rows;
	}

	public int colDim() {
		return colDim;
	}

	public DenseVector column(int colId) {
		DenseVector ret = new DenseVector(rowDim(), colId);

		for (int i = 0; i < rowSize(); i++) {
			SparseVector row = rows[i];
			int loc = row.location(colId);
			if (loc < 0) {
				continue;
			}
			double value = row.valueAtLoc(loc);
			ret.increment(i, value);
		}
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

	public ColumnSparseMatrix copy() {
		SparseVector[] newRows = new SparseVector[rows.length];
		for (int i = 0; i < rows.length; i++) {
			newRows[i] = rows[i].copy();
		}
		return new ColumnSparseMatrix(colDim(), label(), newRows);
	}

	public int indexAtRowLoc(int loc) {
		return loc;
	}

	public int label() {
		return label;
	}

	public int locationAtRow(int rowId) {
		return rowId;
	}

	public void normalizeColumns() {
		SparseVector col_sum = columnSums();

		for (int i = 0; i < rows.length; i++) {
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

	public SparseVector row(int rowId) {
		return rows[rowId];
	}

	public SparseVector rowAlways(int rowId) {
		return rows[rowId];
	}

	public int rowDim() {
		return rows.length;
	}

	public int[] rowIndexes() {
		throw new UnsupportedOperationException("unsupported");
	}

	public int rowSize() {
		return rowDim();
	}

	public void rowSummation() {
		for (Vector row : rows) {
			row.summation();
		}
	}

	public DenseVector rowSums() {
		DenseVector ret = new DenseVector(rowDim(), -1);
		for (int i = 0; i < rows.length; i++) {
			SparseVector row = rows[i];
			double sum = row.sum();
			ret.increment(i, sum);
		}
		return ret;
	}

	@Override
	public void set(int rowId, int colId, double value) {
		int colLoc = rows[rowId].location(colId);
		if (colLoc > -1) {
			rows[rowId].setAtLoc(colLoc, value);
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

	public void setLabel(int label) {
		this.label = label;
	}

	public void setRow(int loc, int rowId, SparseVector x) {
		if (loc != rowId) {
			throw new IllegalArgumentException();
		}
		setRow(rowId, x);
	}

	@Override
	public void setRow(int rowId, Vector x) {
		rows[rowId] = (SparseVector) x;

	}

	@Override
	public void setRowDim(int rowDim) {
		throw new UnsupportedOperationException();
	}

	public void setVectorAtRowLoc(int loc, Vector x) {
		rows[loc] = (SparseVector) x;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("[label:\t%d]\n", label()));
		sb.append(String.format("[row dim:\t%d]\n", rowDim()));
		sb.append(String.format("[col dim:\t%d]\n", colDim()));
		for (int i = 0; i < rows.length && i < 15; i++) {
			sb.append(String.format("%dth: %s\n", i + 1, rows[i]));
		}
		return sb.toString().trim();
	}

	public ColumnSparseMatrix transpose() {
		CounterMap<Integer, Integer> counterMap = new CounterMap<Integer, Integer>();

		for (int i = 0; i < rows.length; i++) {
			SparseVector row = rows[i];
			for (int j = 0; j < row.size(); j++) {
				int colIndex = row.indexAtLoc(j);
				double value = row.valueAtLoc(j);
				counterMap.incrementCount(colIndex, i, value);
			}
		}

		SparseVector[] rows = new SparseVector[colDim()];
		int loc = 0;

		for (int colId : counterMap.keySet()) {
			Counter<Integer> counter = counterMap.getCounter(colId);

			int[] ids = new int[counter.keySet().size()];
			double[] values = new double[ids.length];

			int loc2 = 0;
			for (Entry<Integer, Double> entry : counter.entrySet()) {
				int rowId = entry.getKey();
				double value = entry.getValue();
				ids[loc2] = rowId;
				values[loc2] = value;
				loc2++;
			}

			SparseVector newRow = new SparseVector(ids, values, colId, rowDim());
			newRow.sortByIndex();
			rows[loc++] = newRow;
		}

		return new ColumnSparseMatrix(colDim(), label(), rows);
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
		oos.writeInt(colDim());
		oos.writeInt(label());
		oos.writeInt(rowSize());

		for (int i = 0; i < rowSize(); i++) {
			rows[i].write(oos);
		}
	}
}

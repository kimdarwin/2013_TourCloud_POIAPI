package edu.kaist.ir.matrix;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

import edu.kaist.ir.io.IOUtils;

public class IntegerSparseMatrix implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3542638642565119292L;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

	public static IntegerSparseMatrix read(File inputFile) throws Exception {
		System.out.printf("read [%s].\n", inputFile.getPath());
		ObjectInputStream ois = IOUtils.openObjectInputStream(inputFile);
		IntegerSparseMatrix ret = readStream(ois);
		ois.close();
		return ret;
	}

	public static IntegerSparseMatrix readStream(ObjectInputStream ois) throws Exception {
		int[] rowIndexes = IOUtils.readIntegerArray(ois);
		int[][] rowVectors = IOUtils.readIntegerMatrix(ois);
		IntegerSparseMatrix ret = new IntegerSparseMatrix(rowIndexes, rowVectors);
		return ret;
	}

	private int[] rowIndexes;

	private int[][] rowVectors;

	public IntegerSparseMatrix(int[] rowIndexes, int[][] rowVectors) {
		this.rowIndexes = rowIndexes;
		this.rowVectors = rowVectors;
	}

	public int indexAtRowLoc(int loc) {
		return rowIndexes[loc];
	}

	public int locationAtRow(int row) {
		return Arrays.binarySearch(rowIndexes, row);
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
		int pivotValue = rowIndexes[randomIndex];

		while (i < j) {
			i++;
			while (rowIndexes[i] < pivotValue) {
				i++;
			}

			j--;
			while (rowIndexes[j] > pivotValue) {
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
		qSort(0, rowIndexes.length - 1);
	}

	public int[] row(int row) {
		int loc = locationAtRow(row);
		if (loc > -1) {
			throw new IllegalArgumentException("not found");
		}
		return rowVectors[loc];
	}

	public int[] rowIndexes() {
		return rowIndexes;
	}

	public int rowSize() {
		return rowIndexes.length;
	}

	public void setAtRowLoc(int loc, int[] vector) {
		rowVectors[loc] = vector;
	}

	public void setRow(int loc, int row, int[] vector) {
		rowIndexes[loc] = row;
		rowVectors[loc] = vector;
	}

	public void sortByRowIndex() {
		quicksort();
	}

	private void swapRows(int i, int j) {
		int temp1 = rowIndexes[i];
		int temp2 = rowIndexes[j];
		rowIndexes[i] = temp2;
		rowIndexes[j] = temp1;

		int[] temp3 = rowVectors[i];
		int[] temp4 = rowVectors[j];

		rowVectors[i] = temp4;
		rowVectors[j] = temp3;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < rowIndexes.length && i < 20; i++) {
			int rowIndex = rowIndexes[i];
			int[] rowVector = rowVectors[i];
			sb.append(String.format("%dth %d (%d) ->", i + 1, rowIndex, rowVector.length));
			for (int j = 0; j < rowVector.length && j < 20; j++) {
				sb.append(String.format(" %d", rowVector[j]));
			}
			sb.append("\n");
		}
		return sb.toString().trim();
	}

	public int[] vectorAtRowLoc(int loc) {
		return rowVectors[loc];
	}

	public void write(File outputFile) throws Exception {
		System.out.printf("write to [%s].\n", outputFile.getPath());
		ObjectOutputStream oos = IOUtils.openObjectOutputStream(outputFile);
		write(oos);
		oos.close();
	}

	public void write(ObjectOutputStream oos) throws Exception {
		IOUtils.write(oos, rowIndexes);
		IOUtils.write(oos, rowVectors);
	}
}

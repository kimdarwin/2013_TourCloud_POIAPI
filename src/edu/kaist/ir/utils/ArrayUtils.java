package edu.kaist.ir.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ArrayUtils {

	public static int[] array(Integer[] x) {
		int[] ret = new int[x.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = x[i].intValue();
		}
		return ret;
	}

	public static double[] copy(double[] x) {
		double[] ret = new double[x.length];
		System.arraycopy(x, 0, ret, 0, x.length);
		return ret;
	}

	public static void copy(double[] x, double[] y) {
		System.arraycopy(x, 0, y, 0, x.length);
	}

	public static int[] copy(int[] x) {
		int[] ret = new int[x.length];
		System.arraycopy(x, 0, ret, 0, x.length);
		return ret;
	}

	public static void copy(int[] x, int[] y) {
		System.arraycopy(x, 0, y, 0, x.length);
	}

	public static double[] doubleArray(Collection<Double> x) {
		double[] ret = new double[x.size()];
		int loc = 0;
		Iterator<Double> iter = x.iterator();
		while (iter.hasNext()) {
			ret[loc++] = iter.next();
		}
		return ret;
	}

	public static List<Double> doubleList(double[] array) {
		List<Double> ret = new ArrayList<Double>();
		for (int i = 0; i < array.length; i++) {
			ret.add(array[i]);
		}
		return ret;
	}

	public static int[] integerArray(Collection<Integer> x) {
		int[] ret = new int[x.size()];
		int loc = 0;
		Iterator<Integer> iter = x.iterator();
		while (iter.hasNext()) {
			ret[loc++] = iter.next();
		}
		return ret;
	}

	public static List<Integer> IntegerList(int[] array) {
		List<Integer> ret = new ArrayList<Integer>();
		for (int i = 0; i < array.length; i++) {
			ret.add(array[i]);
		}
		return ret;
	}

	public static Set<Integer> integerSet(int[] array) {
		Set<Integer> ret = new HashSet<Integer>();
		for (int value : array) {
			ret.add(value);
		}

		return ret;
	}

	public static Integer[] objectIntegerArray(int[] x) {
		Integer[] ret = new Integer[x.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = x[i];
		}
		return ret;
	}

	public static double[] randomDoubleArray(int size, double min, double max) {
		double[] ret = new double[size];
		Random random = new Random();
		double range = max - min;
		for (int i = 0; i < size; i++) {
			ret[i] = range * random.nextDouble() + min;
		}
		return ret;
	}

	// public static int[] toArray(Collection<Integer> x) {
	// int[] ret = new int[x.size()];
	// Iterator<Integer> it = x.iterator();
	// int loc = 0;
	// while (it.hasNext()) {
	// ret[loc++] = it.next();
	// }
	// return ret;
	// }

	public static int[] randomIntegerArray(int size, int min, int max) {
		int[] ret = new int[size];
		Random random = new Random();
		double range = max - min + 1;
		for (int i = 0; i < size; i++) {
			ret[i] = (int) (range * random.nextDouble()) + min;
		}
		return ret;
	}

	public static void reverse(int[] x) {
		int mid = x.length / 2;
		for (int i = 0; i < mid; i++) {
			swap(x, i, x.length - 1 - i);
		}
	}

	public static int[] sequenceIntegerArray(int size) {
		return sequenceIntegerArray(0, size);
	}

	public static int[] sequenceIntegerArray(int start, int end) {
		int size = end - start;
		int[] ret = new int[size];
		for (int i = start; i < end; i++) {
			ret[i] = i;
		}
		return ret;
	}

	public static void swap(double[] x, int index1, int index2) {
		double value1 = x[index1];
		double value2 = x[index2];
		x[index1] = value2;
		x[index2] = value1;
	}

	public static void swap(int[] x, int index1, int index2) {
		int value1 = x[index1];
		int value2 = x[index2];
		x[index1] = value2;
		x[index2] = value1;
	}
}

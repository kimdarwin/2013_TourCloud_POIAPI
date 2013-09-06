package edu.kaist.ir.matrix;

import java.io.File;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Set;

public interface Vector extends Serializable {

	public int argMax();

	public int argMin();

	public Vector copy();

	public int dim();

	public void increment(int index, double value);

	public void incrementAll(double value);

	public void incrementAtLoc(int loc, double value);

	public void incrementAtLoc(int loc, int index, double value);

	public int indexAtLoc(int loc);

	public int[] indexes();

	public void keepAbove(double cutoff);

	public void keepTopN(int topN);

	public int label();

	public int location(int index);

	public double max();

	public double min();

	public void normalize();

	public void normalizeAfterSummation();

	public double prob(int index);

	public double probAlways(int index);

	public double probAtLoc(int loc);

	public void prune(final Set<Integer> toRemove);

	public void pruneExcept(final Set<Integer> toKeep);

	public void scale(double factor);

	public void scale(int index, double factor);

	public void scaleAtLoc(int loc, double factor);

	public void set(int index, double value);

	public void setAll(double value);

	public void setAtLoc(int loc, double value);

	public void setAtLoc(int loc, int index, double value);

	public void setDim(int dim);

	public void setIndexes(int[] indexes);

	public void setLabel(int label);

	public void setSum(double sum);

	public void setValues(double[] values);

	public int size();

	public int sizeOfNonzero();

	public double sum();

	public void summation();

	public double value(int index);

	public double valueAlways(int index);

	public double valueAtLoc(int loc);

	public double[] values();

	public void write(File outputFile) throws Exception;

	public void write(ObjectOutputStream oos) throws Exception;

}

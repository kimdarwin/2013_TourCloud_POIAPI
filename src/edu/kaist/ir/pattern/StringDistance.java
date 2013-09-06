package edu.kaist.ir.pattern;


public interface StringDistance {
	/**
	 * Find the distance between s and t. Larger values indicate more similar
	 * strings.
	 */
	public double score(Sequence s, Sequence t);

	/** Preprocess a string for distance computation */
	public Sequence prepare(String s);

	/** Explain how the distance was computed. */
	public String explainScore(Sequence s, Sequence t);
}

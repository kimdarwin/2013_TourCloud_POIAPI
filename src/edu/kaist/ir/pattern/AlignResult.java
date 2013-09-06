package edu.kaist.ir.pattern;

import java.util.List;

import edu.kaist.ir.pattern.AffineGapAligner.Markup;

public class AlignResult {

	private MemoMatrix scoreMatrix;
	private Sequence src;
	private Sequence tar;
	private List<Markup> markups;

	private double score;
	private double numMatches;

	public AlignResult(MemoMatrix scoreMatrix, Sequence src, Sequence tar, List<Markup> markups, double score, double numMatches) {
		super();
		this.scoreMatrix = scoreMatrix;
		this.src = src;
		this.tar = tar;
		this.markups = markups;
		this.score = score;
		this.numMatches = numMatches;
	}

	public List<Markup> getMarkups() {
		return markups;
	}

	public double getNumMatches() {
		return numMatches;
	}

	public double getScore() {
		return score;
	}

	public MemoMatrix getScoreMatrix() {
		return scoreMatrix;
	}

	public Sequence getSource() {
		return src;
	}

	public Sequence getTarget() {
		return tar;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < markups.size(); i++) {
			String s = String.format("%d\t%s\t%s\t%s", i + 1, src.get(i), markups.get(i).getMarker(), tar.get(i));
			sb.append(s + "\n");
		}

		return sb.toString().trim();
	}

}

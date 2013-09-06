package edu.kaist.ir.pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Heung-Seon Oh
 * 
 *         class for affined gap alignment.
 * 
 */
public class AffineGapAligner {

	public enum Direction {
		LEFT, UP, DIAGONAL, BOUNDARY;

		public static Direction getPreviousPosition(MemoMatrix scoreMatrix, int i, int j) {
			Direction ret = Direction.BOUNDARY;

			if (i == 1 && j > 1) {
				ret = Direction.UP;
			} else if (i > 1 && j == 1) {
				ret = Direction.LEFT;
			} else {
				double leftScore = scoreMatrix.get(i - 1, j);
				double upScore = scoreMatrix.get(i, j - 1);
				double diagonalScore = scoreMatrix.get(i - 1, j - 1);
				double score = MemoMatrix.max3(leftScore, diagonalScore, upScore);

				if (score == diagonalScore) {
					ret = Direction.DIAGONAL;
				} else if (score == upScore) {
					ret = Direction.UP;
				} else if (score == leftScore) {
					ret = Direction.LEFT;
				}
			}

			return ret;
		}
	}

	public enum Markup {
		MATCH("="), UNMATCH("!="), SIMILAR(":=");

		private String marker;

		private Markup(String marker) {
			this.marker = marker;
		}

		String getMarker() {
			return marker;
		}
	}

	// a set of three linked distance matrices
	protected class MatrixTrio extends MemoMatrix {
		protected class InsertSMatrix extends MemoMatrix {
			public InsertSMatrix(Sequence s, Sequence t) {
				super(s, t);
			}

			public double compute(int i, int j) {
				if (i == 0 || j == 0)
					return 0;
				return max3(lowerBound, m.get(i - 1, j) + openGapScore, is.get(i - 1, j) + extendGapScore);
			}
		}

		protected class InsertTMatrix extends MemoMatrix {
			public InsertTMatrix(Sequence s, Sequence t) {
				super(s, t);
			}

			public double compute(int i, int j) {
				if (i == 0 || j == 0)
					return 0;
				return max3(lowerBound, m.get(i, j - 1) + openGapScore, it.get(i, j - 1) + extendGapScore);
			}
		}

		protected MemoMatrix m;

		protected InsertSMatrix is;

		protected InsertTMatrix it;

		public MatrixTrio(Sequence s, Sequence t) {
			super(s, t);
			is = new InsertSMatrix(s, t);
			it = new InsertTMatrix(s, t);
			m = this;
		}

		public double compute(int i, int j) {
			if (i == 0 || j == 0)
				return 0;
			double matchScore = strMatchScore.matchScore(sAt(i), tAt(j));
			return max4(lowerBound, m.get(i - 1, j - 1) + matchScore, is.get(i - 1, j - 1) + matchScore, it.get(i = 1, j - 1) + matchScore);
		}

	}

	static public void main(String[] argv) {
		AffineGapAligner aligner = new AffineGapAligner();

		// String[] strs = { "William W. ‘Don’t call me Dubya’ Cohen",
		// "William W. Cohen" };
		// Sequence s = Wrapper.getStringSequence(strs[0]);
		// Sequence t = Wrapper.getStringSequence(strs[1]);

		// String[] strs = { "i love new york but la .",
		// "i love boston but much more la ." };
		// String[] strs = { "근처/N 에/j 볼케이노/N 하/V 는/e 극장/N 알려주/V 어/e",
		// "근처/N 에/j 황산벌/N 제일/Z 빨리/Z 하/V 는/e 극장/N 찾아주/V 어/e" };
		// String[] strs = { "볼케이노/N 하/V 는/e 극장/N",
		// "근처/N 에/j 볼케이노/N 하/V 는/e 극장/N 알려주/V 어/e" };
		String[] strs = { "볼케이노/N 하/V 는/e 극장/N", "이번/N 주/N 토요일/N 에/j 부당거래/N 하/V 는/e 극장/N 중/N 예매율/N 이/j 가장/Z 적/V 은/e 극장/N 검색/N" };
		// String[] strs = { "근처/N 에/j 볼케이노/N 하/V 는/e 극장/N 알려주/V 어/e",
		// "근처/N 에/j 문화상품권/N 되/V 는/e 극장/N 알려주/V 어/e" };
		// String[] strs = { "근처/N 에/j 볼케이노/N 하/V 는/e 극장/N 알려주/V 어/e",
		// "근처/N 에/j 황산벌/N 제일/Z 빨리/Z 하/V 는/e 극장/N 찾아주/V 어/e" };
		Sequence s = Wrapper.getArraySequence(strs[0]);
		Sequence t = Wrapper.getArraySequence(strs[1]);
		AlignResult ar = aligner.align(s, t);

		System.out.println(ar.toString());

	}

	private StringMatchScore strMatchScore;

	private double openGapScore;

	private double extendGapScore;

	private double lowerBound;

	public AffineGapAligner() {
		this(StringMatchScore.DIST_21, 2, 1, -Double.MAX_VALUE);
		// this(StringMatchScore.DIST_21, 2, 1, 0);
	}

	public AffineGapAligner(StringMatchScore strMatchScore, double openGapScore, double extendGapScore, double lowerBound) {
		this.strMatchScore = strMatchScore;
		this.openGapScore = openGapScore;
		this.extendGapScore = extendGapScore;
		this.lowerBound = lowerBound;
	}

	public AlignResult align(Sequence s, Sequence t) {
		// System.out.println(explainScore(s, t));
		MatrixTrio scoreMatrix = new MatrixTrio(s, t);
		score(scoreMatrix);
		AlignResult ar = traceback(scoreMatrix);
		return ar;
	}

	public String explainScore(Sequence s, Sequence t) {
		MatrixTrio scoreMatrix = new MatrixTrio(s, t);
		double d = score(scoreMatrix);
		return scoreMatrix.toString() + "\nScore = " + d;
	}

	private double score(MatrixTrio scoreMatrix) {
		Sequence s = scoreMatrix.getSourceSequence();
		Sequence t = scoreMatrix.getTargetSequence();

		double best = -Double.MAX_VALUE;
		for (int i = 0; i <= s.length(); i++) {
			for (int j = 0; j <= t.length(); j++) {
				double score = scoreMatrix.get(i, j);
				best = Math.max(best, score);
			}
		}
		return best;
	}

	public double score(Sequence s, Sequence t) {
		return score(new MatrixTrio(s, t));
	}

	private AlignResult traceback(MemoMatrix scoreMatrix) {
		Sequence s = scoreMatrix.getSourceSequence();
		Sequence t = scoreMatrix.getTargetSequence();

		// boolean[] srcFlags = new boolean[s.length() + 1];
		// boolean[] tarFlags = new boolean[t.length() + 1];
		//
		int i = s.length();
		int j = t.length();

		List<String> src = new ArrayList<String>();
		List<String> tar = new ArrayList<String>();
		List<Markup> markups = new ArrayList<Markup>();
		final String GAP = "#G";

		double bestScore = -Double.MAX_VALUE;

		while (i > 1 && j > 1) {
			String si = scoreMatrix.sAt(i);
			String tj = scoreMatrix.tAt(j);
			double score = scoreMatrix.get(i, j);

			if (score > bestScore) {
				bestScore = score;
			}

			Direction from = Direction.getPreviousPosition(scoreMatrix, i, j);

			// System.out.println(String.format("[%d, %d, %d, %s] [%s, %s]", i,
			// j, (int) score, from, si, tj));

			String temp_si = si;
			String temp_tj = tj;

			if (from == Direction.UP) {
				temp_si = GAP;
				j--;
			} else if (from == Direction.LEFT) {
				temp_tj = GAP;
				i--;
			} else if (from == Direction.DIAGONAL) {
				i--;
				j--;
			}

			// if (temp_si.equals(temp_tj)) {
			// if (srcFlags[temp_i]) {
			// temp_si = GAP;
			// } else if (tarFlags[temp_j]) {
			// temp_tj = GAP;
			// }
			// }

			String new_si = temp_si;
			String new_tj = temp_tj;
			Markup markup = Markup.UNMATCH;

			if (new_si.equals(new_tj)) {
				markup = Markup.MATCH;
			}

			src.add(new_si);
			tar.add(new_tj);
			markups.add(markup);
		}

		if (i == 1 && j == 1) {
			String si = s.get(--i);
			String tj = t.get(--j);
			src.add(si);
			tar.add(tj);
			Markup markup = Markup.UNMATCH;
			if (si.equals(tj)) {
				markup = Markup.MATCH;
			}
			markups.add(markup);
		} else if (i == 1 && j > 1) {
			while (j > 0) {
				i--;
				j--;
				String si = i >= 0 ? s.get(i) : GAP;
				String tj = t.get(j);
				// System.out.println(String.format("%s, %s", si, tj));
				src.add(si);
				tar.add(tj);
				Markup markup = Markup.UNMATCH;
				if (si.equals(tj)) {
					markup = Markup.MATCH;
				}
				markups.add(markup);
			}
		} else if (i > 1 && j == 1) {
			while (i > 0) {
				i--;
				j--;
				String si = s.get(i);
				String tj = j >= 0 ? t.get(j) : GAP;
				src.add(si);
				tar.add(tj);
				Markup markup = Markup.UNMATCH;
				if (si.equals(tj)) {
					markup = Markup.MATCH;
				}
				markups.add(markup);

			}
		}

		Collections.reverse(src);
		Collections.reverse(tar);
		Collections.reverse(markups);

		assert (src.size() == markups.size() && tar.size() == markups.size());

		// System.out.println(src.size());
		// System.out.println(tar.size());
		// System.out.println(markups.size());

		double numMatches = 0;
		for (Markup m : markups) {
			if (m == Markup.MATCH) {
				numMatches++;
			}
		}

		Sequence ss = Wrapper.getArraySequence(src.toArray(new String[src.size()]));
		Sequence tt = Wrapper.getArraySequence(tar.toArray(new String[tar.size()]));

		// for (int k = 0; k < src.size(); k++) {
		// System.out.println(String.format("%s\t%s\t%s", src.get(k),
		// markups.get(k).getMarker(), tar.get(k)));
		// }

		return new AlignResult(scoreMatrix, ss, tt, markups, bestScore, numMatches);
	}
}

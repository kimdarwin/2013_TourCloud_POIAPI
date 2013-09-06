package edu.kaist.ir.pattern;

import java.io.Serializable;

import edu.kaist.ir.utils.StrUtils;

public abstract class StringMatchScore implements Serializable {

	private static final long serialVersionUID = 9155306272218852126L;

	public static StringMatchScore DIST_01 = new StringMatchScore() {

		private static final long serialVersionUID = 1243003111656604493L;

		public double matchScore(String c, String c1) {
			return !c.equals(c1) ? -1D : 0.0D;
		}

	};

	public static StringMatchScore DIST_21 = new StringMatchScore() {

		private static final long serialVersionUID = 6373091949733613157L;

		public double matchScore(String c, String c1) {
			return !c.equals(c1) ? -1D : 2D;
		}

	};

	public static StringMatchScore DIST_211 = new StringMatchScore() {

		private static final long serialVersionUID = 6373091949733613157L;

		public double matchScore(String c, String c1) {
			String[] two1 = StrUtils.split2Two("/", c);
			String[] two2 = StrUtils.split2Two("/", c1);

			String word1 = two1[0];
			String tag1 = two1[1];

			String word2 = two2[0];
			String tag2 = two2[1];

			double ret = 0;

			if (c.equals(c1)) {
				ret = 2;
			} else {
				if (word1.equals(word2) || tag1.equals(tag2)) {
					ret = 1;
				} else {
					ret = -1;
				}
			}
			return ret;
		}

	};

	public StringMatchScore() {
	}

	public abstract double matchScore(String c, String c1);

}
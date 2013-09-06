package edu.kaist.ir.poi.ext;

import java.util.Comparator;

import edu.kaist.ir.utils.Pair;

public class PairUtils {

	public static class PairComparator implements Comparator<Pair<Integer, Integer>> {
		private boolean compareSecond;

		public PairComparator(boolean compareSecond) {
			this.compareSecond = compareSecond;
		}

		@Override
		public int compare(Pair<Integer, Integer> arg0, Pair<Integer, Integer> arg1) {
			int ret = -1;

			if (compareSecond) {
				ret = arg0.getSecond() - arg1.getSecond();
			} else {
				ret = arg0.getFirst() - arg1.getFirst();
			}
			return ret;
		}
	}

}

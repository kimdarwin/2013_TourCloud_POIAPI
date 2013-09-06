package edu.kaist.ir.pattern;

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

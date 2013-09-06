package edu.kaist.ir.pattern;

import edu.kaist.ir.utils.StrUtils;

/**
 * @author Heung-Seon Oh
 * @version 1.2
 * @date 2009. 6. 6
 * 
 */
public class ArraySequence implements Sequence {
	private String[] seq;

	public ArraySequence(String[] seq) {
		this.seq = seq;
	}

	public String get(int i) {
		return seq[i];
	}

	@Override
	public int length() {
		return seq.length;
	}

	@Override
	public String toString() {
		return StrUtils.join(" ", seq);
	}

}

package edu.kaist.ir.pattern;

import java.util.List;

import edu.kaist.ir.utils.StrUtils;

/**
 * @author Heung-Seon Oh
 * @version 1.2
 * @date 2009. 6. 6
 * 
 */
public class ListSequence implements Sequence {
	private List<String> src;

	public ListSequence(List<String> s) {
		this.src = s;
	}

	public String get(int i) {
		return src.get(i);
	}

	@Override
	public int length() {
		return src.size();
	}

	@Override
	public String toString() {
		return StrUtils.join(" ", src);
	}

}

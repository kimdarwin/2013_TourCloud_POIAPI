package edu.kaist.ir.poi.ext;

import edu.kaist.ir.utils.StrUtils;

/**
 * @author Heung-Seon Oh
 * 
 *         This class holds information of a text segment.
 * 
 */
public class Segment {
	private String str;

	private int start;

	private int end;

	public Segment(String str, int start, int end) {
		this.str = str;
		this.start = start;
		this.end = end;
	}

	public int end() {
		return end;
	}

	public int start() {
		return start;
	}

	public Character[] toCharacters() {
		return StrUtils.toCharacters(str);
	}

	public String toString() {
		return String.format("%s, [%d-%d]", str, start, end);
	}
}
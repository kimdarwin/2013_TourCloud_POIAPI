package edu.kaist.ir.pattern;


/**
 * @author Heung-Seon Oh
 * @version 1.2
 * @date 2009. 6. 6
 * 
 */
public class StringSequence implements Sequence {
	private String str;

	public StringSequence(String str) {
		this.str = str;
	}

	@Override
	public String get(int i) {
		return new String(str.charAt(i) + "");
	}

	@Override
	public int length() {
		return str.length();
	}

	@Override
	public String toString() {
		return str;
	}

}

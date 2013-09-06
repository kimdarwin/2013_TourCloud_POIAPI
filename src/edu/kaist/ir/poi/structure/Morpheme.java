package edu.kaist.ir.poi.structure;

/**
 * 
 * 
 * @author Heung-Seon Oh
 * 
 *         This class holds information of a morpheme
 * 
 */
public class Morpheme {

	private String text;

	private String tag;

	public Morpheme(String text, String tag) {
		this.text = text;
		this.tag = tag;
	}

	/**
	 * Returns morpheme tag .
	 * 
	 * @return
	 */
	public String tag() {
		return tag;
	}

	/**
	 * Returns morpheme text
	 * 
	 * @return
	 */
	public String text() {
		return text;
	}

	public String toString() {
		return String.format("%s/%s", text, tag);
	}

}

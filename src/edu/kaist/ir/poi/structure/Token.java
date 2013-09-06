package edu.kaist.ir.poi.structure;

/**
 * 
 * @author Heung-Seon Oh
 * 
 *         This class holds information for Token.
 * 
 *         Token consists of a original token text and a sequence of morphemes
 *         and corresponding tags.
 * 
 */
public class Token {

	private String text;

	private String[] morphemes;

	private String[] tags;

	public Token(String text, String[] morphemes, String[] tags) {
		this.text = text;
		this.morphemes = morphemes;
		this.tags = tags;
	}

	public String[] morphemes() {
		return morphemes;
	}

	public String[] tags() {
		return tags;
	}

	public String text() {
		return text;
	}

	public String taggedText() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < morphemes.length; i++) {
			sb.append(String.format("%s/%s", morphemes[i], tags[i]));
			if (i != morphemes.length - 1) {
				sb.append("+");
			}
		}
		return sb.toString();
	}

	public String morphemeText() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < morphemes.length; i++) {
			sb.append(String.format("%s", morphemes[i]));
			if (i != morphemes.length - 1) {
				sb.append("+");
			}
		}
		return sb.toString();
	}

	public String tagText() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < morphemes.length; i++) {
			sb.append(String.format("%s", tags[i]));
			if (i != morphemes.length - 1) {
				sb.append("+");
			}
		}
		return sb.toString();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer(text);
		if (morphemes != null) {
			sb.append("=>");
			sb.append(taggedText());
		}
		return sb.toString();
	}

}

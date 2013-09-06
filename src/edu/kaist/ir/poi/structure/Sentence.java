package edu.kaist.ir.poi.structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.kaist.ir.poi.ext.PairUtils;
import edu.kaist.ir.utils.IndexedList;
import edu.kaist.ir.utils.Pair;

/**
 * @author Heung-Seon Oh
 * 
 *         This class holds information of a sentence
 * 
 */
public class Sentence {

	// original text
	private String text;

	private Token[] tokens;

	private IndexedList<Pair<Integer, Integer>, POI> poiAnnotations;

	public Sentence(String text) {
		this.text = text;
	}

	public Sentence(String[] words) {
		tokens = new Token[words.length];
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < words.length; i++) {
			tokens[i] = new Token(words[i], null, null);
			sb.append(words[i] + " ");
		}
		text = sb.toString().trim();
	}

	public Sentence(Token[] tokens) {
		this.tokens = tokens;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < tokens.length; i++) {
			sb.append(tokens[i].text());
			if (i != tokens.length - 1) {
				sb.append(" ");
			}
		}
		this.text = sb.toString().trim();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Sentence other = (Sentence) obj;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		return result;
	}

	public String[] morphemes() {
		List<String> ret = new ArrayList<String>();
		for (int i = 0; i < tokens.length; i++) {
			for (String morpheme : tokens[i].morphemes()) {
				ret.add(morpheme);
			}
		}
		return ret.toArray(new String[ret.size()]);
	}

	public String morphemeText() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < tokens.length; i++) {
			sb.append(tokens[i].morphemeText() + " ");
		}
		return sb.toString().trim();
	}

	public int[] morphemLocations(int start, int end) {
		String[] morphemes = morphemes();

		int[] tokenStarts = new int[morphemes.length];
		int[] tokenEnds = new int[morphemes.length];

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < morphemes.length; i++) {
			String morpheme = morphemes[i];
			tokenStarts[i] = sb.length();
			sb.append(morpheme);
			tokenEnds[i] = sb.length();
			if (i != tokens.length - 1) {
				sb.append(" ");
			}
		}

		int startLoc = -1;
		int endLoc = -1;

		for (int i = 0; i < tokenStarts.length; i++) {
			int tokenStart = tokenStarts[i];
			int tokenEnd = tokenEnds[i];

			if (tokenStart <= start && tokenEnd >= start) {
				startLoc = i;
			}

			if (tokenStart <= end && tokenEnd >= end) {
				endLoc = i;
			}
		}

		for (int i = startLoc; i <= endLoc; i++) {
			System.out.println(morphemes[i]);
		}

		return new int[] { startLoc, endLoc };
	}

	/**
	 * retuns POI annotations
	 * 
	 * Each annotation consists of (start, end) of matched string in a sentence
	 * and a corresponding POI path.
	 * 
	 * @return
	 */
	public IndexedList<Pair<Integer, Integer>, POI> poiAnnotations() {
		return poiAnnotations;
	}

	/**
	 * 
	 * 
	 * @param poiAnnotations
	 */
	public void setPoiAnnotations(IndexedList<Pair<Integer, Integer>, POI> poiAnnotations) {
		this.poiAnnotations = poiAnnotations;
	}

	public String taggedText() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < tokens.length; i++) {
			sb.append(tokens[i].taggedText() + " ");
		}
		return sb.toString().trim();
	}

	public String[] tags() {
		List<String> ret = new ArrayList<String>();
		for (int i = 0; i < tokens.length; i++) {
			for (String morpheme : tokens[i].tags()) {
				ret.add(morpheme);
			}
		}
		return ret.toArray(new String[ret.size()]);
	}

	public String tagText() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < tokens.length; i++) {
			sb.append(tokens[i].tagText() + " ");
		}
		return sb.toString().trim();
	}

	public String text() {
		return text;
	}

	public int[] tokenIndexes(int start, int end) {
		int[] tokenStarts = new int[tokens.length];
		int[] tokenEnds = new int[tokens.length];

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < tokens.length; i++) {
			Token token = tokens[i];
			tokenStarts[i] = sb.length();
			sb.append(token.morphemeText());
			tokenEnds[i] = sb.length();
			if (i != tokens.length - 1) {
				sb.append(" ");
			}
		}

		int startLoc = -1;
		int endLoc = -1;

		for (int i = 0; i < tokenStarts.length; i++) {
			int tokenStart = tokenStarts[i];
			int tokenEnd = tokenEnds[i];

			if (tokenStart <= start && tokenEnd >= start) {
				startLoc = i;
			}

			if (tokenStart <= end && tokenEnd >= end) {
				endLoc = i;
			}
		}

		for (int i = startLoc; i <= endLoc; i++) {
			System.out.println(tokens[i].morphemeText());
		}

		return new int[] { startLoc, endLoc };
	}

	public Token[] tokens() {
		return tokens;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer(text);

		if (poiAnnotations != null) {
			int numSpans = 0;

			List<Pair<Integer, Integer>> spans = new ArrayList<Pair<Integer, Integer>>(poiAnnotations.keySet());
			Collections.sort(spans, new PairUtils.PairComparator(false));

			for (int i = 0; i < spans.size(); i++) {
				Pair<Integer, Integer> span = spans.get(i);
				int start = span.getFirst();
				int end = span.getSecond();
				String poiStr = text.substring(start, end);
				List<POI> pois = poiAnnotations.get(span);

				sb.append(String.format("\n->%dth string:\t%s [%d-%d]", ++numSpans, poiStr, start, end));

				for (int j = 0; j < pois.size(); j++) {
					sb.append(String.format("\n%dth poi:\t%s", j + 1, pois.get(j)));
				}
			}
		}
		return sb.toString();
	}
}

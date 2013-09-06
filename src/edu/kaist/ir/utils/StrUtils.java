package edu.kaist.ir.utils;

import java.lang.Character.UnicodeBlock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.kaist.ir.math.ArrayMaths;

/**
 * 
 * @author Heung-Seon Oh
 * @version 1.0
 * @date 2009. 12. 8
 * 
 */

public class StrUtils {
	public static boolean find(String text, Pattern p) {
		return p.matcher(text).find();
	}

	public static boolean find(String text, String regex) {
		return find(text, Pattern.compile(regex));
	}

	/**
	 * Strings.java in mallet
	 * 
	 * 
	 * @param s
	 * @param t
	 * @param normalize
	 * @return
	 */
	public static double editDistance(String s, String t, boolean normalize) {
		int n = s.length();
		int m = t.length();
		int d[][]; // matrix
		int i; // iterates through s
		int j; // iterates through t
		char s_i; // ith character of s
		char t_j; // jth character of t
		int cost; // cost

		if (n == 0)
			return m;
		if (m == 0)
			return n;

		d = new int[n + 1][m + 1];

		for (i = 0; i <= n; i++)
			d[i][0] = i;

		for (j = 0; j <= m; j++)
			d[0][j] = j;

		for (i = 1; i <= n; i++) {
			s_i = s.charAt(i - 1);

			for (j = 1; j <= m; j++) {
				t_j = t.charAt(j - 1);

				cost = (s_i == t_j) ? 0 : 1;

				d[i][j] = ArrayMaths.min(new int[] { d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost });
			}
		}

		int longer = (n > m) ? n : m;
		double ret = normalize ? (double) d[n][m] / longer : (double) d[n][m];
		return ret;
	}

	public static Character[] toCharacters(String text) {
		Character[] ret = new Character[text.length()];
		for (int i = 0; i < text.length(); i++) {
			ret[i] = new Character(text.charAt(i));
		}
		return ret;
	}

	public static List<String> ngrams(String text, int ngramOrder) {
		List<String> ret = new ArrayList<String>();
		for (int i = 0; i < text.length() - ngramOrder + 1; i++) {
			String ngram = text.substring(i, i + ngramOrder);
			ret.add(ngram);
		}
		return ret;
	}

	public static String reverse(String text) {
		return new StringBuffer(text).reverse().toString();
	}

	public static List<Matcher> getMatchers(String text, Pattern p) {
		List<Matcher> ret = new ArrayList<Matcher>();
		boolean found = false;
		int loc = 0;
		do {
			Matcher m = p.matcher(text);
			found = m.find(loc);
			if (found) {
				ret.add(m);
				loc = m.end();
			}
		} while (found);
		return ret;
	}

	public static String join(String glue, List<String> list) {
		return join(glue, list, 0, list.size());
	}

	public static String join(String glue, List<String> list, int start) {
		return join(glue, list, start, list.size());
	}

	public static String join(String glue, List<String> list, int start, int end) {
		StringBuffer sb = new StringBuffer();

		if (end > list.size()) {
			end = list.size();
		}

		for (int i = start; i < end; i++) {
			sb.append(list.get(i).toString() + (i == end - 1 ? "" : glue));
		}
		return sb.toString();
	}

	public static String join(String glue, String[] array) {
		return join(glue, array, 0, array.length);
	}

	public static String join(String glue, String[] array, int start) {
		return join(glue, array, start, array.length);
	}

	public static String join(String glue, String[] array, int start, int end) {
		StringBuffer sb = new StringBuffer();
		if (start < 0) {
			start = 0;
		}

		if (end > array.length) {
			end = array.length;
		}

		for (int i = start; i < end; i++) {
			sb.append((array[i] == null ? "null" : array[i]) + (i == end - 1 ? "" : glue));
		}
		return sb.toString();
	}

	public static String join(String glue, String[] array, int[] indexList) {
		List<String> list = new ArrayList<String>();
		for (int index : indexList) {
			list.add(array[index]);
		}
		return join(glue, list);
	}

	// public static String normalize(String text, String regex) {
	// return text.replaceAll(regex, " ").trim();
	// }

	public static String normalizeNumbers(String text) {
		return text.replaceAll("\\d+", " ");
	}

	public static String normalizePunctuations(String text) {
		return text.replaceAll("\\p{Punct}+", " ");
	}

	public static String normalizeSpaces(String text) {
		return normalizeSpaces(text, false);
	}

	public static String normalizeUndefinedChars(String text) {
		return text.replaceAll("[^\\p{Punct}\\w\\s가-힣]+", " ");
	}

	public static String normalizeSpaces(String text, boolean exceptNewLines) {
		String ret = null;

		if (exceptNewLines) {
			StringBuffer sb = new StringBuffer();
			for (String line : text.split("\n")) {
				line = line.trim();
				if (line.equals("")) {
					continue;
				}
				sb.append(line.replaceAll("[\\s]+", " ") + "\n");
			}
			ret = sb.toString().trim();
		} else {
			ret = text.replaceAll("[\\s]+", " ");
		}
		return ret;
	}

	public static String separateBracket(String text) {
		StringBuffer sb = new StringBuffer();
		sb.append(text.charAt(0));

		for (int i = 1; i < text.length(); i++) {
			char prevCh = text.charAt(i - 1);
			char currCh = text.charAt(i);
			if (prevCh == '(' || prevCh == ')' || prevCh == '[' || prevCh == ']' || prevCh == '{' || prevCh == '}' || prevCh == '<' || prevCh == '>') {
				sb.append(currCh);
			}
		}

		return sb.toString();
	}

	public static String separateKorean(String text) {
		int[] types = new int[text.length()];

		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			Character.UnicodeBlock unicodeBlock = Character.UnicodeBlock.of(ch);

			if (UnicodeBlock.HANGUL_SYLLABLES.equals(unicodeBlock)

			|| UnicodeBlock.HANGUL_COMPATIBILITY_JAMO.equals(unicodeBlock)

			|| UnicodeBlock.HANGUL_JAMO.equals(unicodeBlock)) {
				types[i] = 1;
			} else if (Character.isWhitespace(ch)) {
				types[i] = 2;
			}
		}

		StringBuffer sb = new StringBuffer();

		sb.append(text.charAt(0));

		for (int i = 1; i < text.length(); i++) {
			if ((types[i - 1] == 1 && types[i] == 0) || types[i - 1] == 0 && types[i] == 1) {
				sb.append(' ');
				sb.append(text.charAt(i));
			} else {
				sb.append(text.charAt(i));
			}
		}

		return sb.toString();
	}

	public static List<String> split(String text) {
		return split("[\\s]+", text);
	}

	public static List<String> split(String delimiter, String text) {
		List<String> ret = new ArrayList<String>();
		for (String tok : text.split(delimiter)) {
			ret.add(tok);
		}
		return ret;
	}

	public static String[][] split(String[] array, int[] indexList) {
		Set<Integer> set = new HashSet<Integer>();
		for (int index : indexList) {
			set.add(index);
		}
		List<Object> list1 = new ArrayList<Object>();
		List<Object> list2 = new ArrayList<Object>();

		for (int i = 0; i < array.length; i++) {
			if (set.contains(i)) {
				list1.add(array[i]);
			} else {
				list2.add(array[i]);
			}
		}

		String[][] ret = new String[2][];
		ret[0] = list1.toArray(new String[list1.size()]);
		ret[1] = list2.toArray(new String[list2.size()]);
		return ret;
	}

	public static String[] split2Two(String delimiter, String text) {
		String[] ret = null;
		int idx = text.lastIndexOf(delimiter);

		if (idx > -1) {
			ret = new String[2];
			ret[0] = text.substring(0, idx);
			ret[1] = text.substring(idx + 1);
		}
		return ret;
	}

	public static String substring(String text, String startText, String endText) {
		int start = text.indexOf(startText) + startText.length();
		int end = text.indexOf(endText);
		return text.substring(start, end);
	}

	public static String[] toArray(Collection<String> collection) {
		String[] ret = new String[collection.size()];
		Iterator<String> iter = collection.iterator();
		int loc = 0;
		while (iter.hasNext()) {
			ret[loc++] = iter.next();
		}
		return ret;
	}

	public static List<String> toList(String[] array) {
		List<String> ret = new ArrayList<String>();
		for (String str : array) {
			ret.add(str);
		}
		return ret;
	}

	public static String toString(Object[] array, String delimiter) {
		StringBuffer sb = new StringBuffer();
		String separator = delimiter == null ? "\n" : delimiter;
		for (int i = 0; i < array.length; i++) {
			sb.append(array[i].toString() + (i == array.length - 1 ? "" : separator));
		}
		return sb.toString();
	}

};
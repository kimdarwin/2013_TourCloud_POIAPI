package edu.kaist.ir.poi.structure;

import java.util.ArrayList;
import java.util.List;

import edu.kaist.ir.utils.StrUtils;

/**
 * @author Heung-Seon Oh
 * 
 */
public class Document {

	/**
	 * Create document from raw text.
	 * 
	 * Sentences are detected by a sequence of new line characters ("\n").
	 * 
	 * @param text
	 * @return
	 */
	public static Document createFromRawText(String text) {
		String[] lines = text.split("[\n]+");
		List<Sentence> sents = new ArrayList<Sentence>();

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			Sentence sent = new Sentence(line.split("\\p{Space}"));
			sents.add(sent);
		}

		Document ret = new Document(text);
		ret.setSentences(sents);
		return ret;
	}

	/**
	 * Create document from text morphologically analyzed by Hannanum Tagger
	 * (http://semanticweb.kaist.ac.kr/home/index.php/HanNanum).
	 * 
	 * Input text is separated by "\n\n" where each split indicates a sentence.
	 * 
	 * A sentence is formed below.
	 * 
	 * token\tmorpheme1/tag1+morpheme2/tag2 ...
	 * 
	 * 
	 * 
	 * @param text
	 * @return
	 */
	public static Document createFromTaggedText(String text) {
		List<Sentence> sents = new ArrayList<Sentence>();

		for (String line : text.split("\n")) {
			List<Token> tokens = new ArrayList<Token>();

			for (String line2 : line.split("\n")) {
				String[] parts = line2.split("\t");

				List<String> morphemes = new ArrayList<String>();
				List<String> tags = new ArrayList<String>();

				String tokenText = parts[0];
				String morphemeStr = parts[1];

				for (String tok : morphemeStr.split("\\+")) {
					String[] two = StrUtils.split2Two("/", tok);
					String morphem = two[0];
					String tag = two[1];

					morphemes.add(morphem);
					tags.add(tag);
				}

				Token token = new Token(tokenText, morphemes.toArray(new String[morphemes.size()]), tags.toArray(new String[tags.size()]));
				tokens.add(token);

			}
			sents.add(new Sentence(tokens.toArray(new Token[tokens.size()])));
		}

		Document ret = new Document(text);
		ret.setSentences(sents);
		return ret;
	}

	public static Document createFromTaggedText2(String text) throws Exception {
		List<Sentence> sents = new ArrayList<Sentence>();

		for (String sent : text.split("\n\n\n")) {
			List<Token> tokens = new ArrayList<Token>();

			for (String line : sent.split("\n\n")) {
				String[] parts = line.split("\n\t");

				String tokenText = parts[0];
				String morphemeStr = parts[1];

				List<String> morphemes = new ArrayList<String>();
				List<String> tags = new ArrayList<String>();

				for (String tok : morphemeStr.split("\\+")) {
					String[] two = StrUtils.split2Two("/", tok);
					String morphem = two[0];
					String tag = two[1];

					morphemes.add(morphem);
					tags.add(tag);
				}

				Token token = new Token(tokenText, morphemes.toArray(new String[morphemes.size()]), tags.toArray(new String[tags.size()]));
				tokens.add(token);
			}
			sents.add(new Sentence(tokens.toArray(new Token[tokens.size()])));
		}

		Document ret = new Document(sents);
		return ret;
	}

	private String text;

	private List<Sentence> sents;

	public Document(String text) {
		this.text = text;
	}

	public Document(List<Sentence> sents) {
		this.sents = sents;

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < sents.size(); i++) {
			Sentence sent = sents.get(i);
			sb.append(sent.text());

			if (i != sents.size() - 1) {
				sb.append("\n");
			}
		}

		text = sb.toString();
	}

	public void addSentence(Sentence sent) {
		sents.add(sent);
	}

	public Sentence sentence(int i) {
		return sents.get(i);
	}

	public List<Sentence> sentences() {
		return sents;
	}

	public int sentSize() {
		return sents.size();
	}

	public void setSentences(List<Sentence> sents) {
		this.sents = sents;
	}

	public String text() {
		return text;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < sents.size(); i++) {
			sb.append(String.format("%dth:\t%s\n", i + 1, sents.get(i).toString()));
		}
		return sb.toString().trim();
	}

}

package edu.kaist.ir.wiki;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.jhu.nlp.wikipedia.WikiPage;

/**
 * For internal use only -- Used by the {@link WikiPage} class. Can also be used
 * as a stand alone class to parse wiki formatted text.
 * 
 * @author Delip Rao
 * 
 */
public class MyWikiTextParser {

	public class InfoBox {

		String infoBoxWikiText;

		InfoBox(String s) {
			infoBoxWikiText = null;
			infoBoxWikiText = s;
		}

		public String dumpRaw() {
			return infoBoxWikiText;
		}
	}

	private String wikiText = null;
	private List<String> pageCats = null;
	private List<String> pageLinks = null;
	private boolean redirect = false;
	private String redirectString = null;
	private boolean stub = false;
	private boolean disambiguation = false;

	private final static Pattern REDIRECT_PATTERN = Pattern.compile("#REDIRECT\\s+\\[\\[(.*?)\\]\\]", Pattern.CASE_INSENSITIVE);
	private final static Pattern STUB_PATTERN = Pattern.compile("\\-stub\\}\\}");
	private final static Pattern DISAMB_TEMPLATE_PATTERN = Pattern.compile("\\{\\{disambig\\}\\}");
	private final static Pattern CATEGORY_PATTERN = Pattern.compile("\\[\\[분류:(.*?)\\]\\]", Pattern.MULTILINE);
	private final static Pattern LINKS_PATTERN = Pattern.compile("\\[\\[(.*?)\\]\\]", Pattern.MULTILINE);

	private InfoBox infoBox = null;

	public MyWikiTextParser(String wtext) {
		wikiText = wtext;
		Matcher matcher = REDIRECT_PATTERN.matcher(wikiText);
		if (matcher.find()) {
			redirect = true;
			if (matcher.groupCount() == 1)
				redirectString = matcher.group(1);
		}
		matcher = STUB_PATTERN.matcher(wikiText);
		stub = matcher.find();
		matcher = DISAMB_TEMPLATE_PATTERN.matcher(wikiText);
		disambiguation = matcher.find();
	}

	public List<String> getCategories() {
		if (pageCats == null)
			parseCategories();
		return pageCats;
	}

	public String getFirstPharagraph() {
		String plainText = getPlainText();
		StringBuffer sb = new StringBuffer();
		for (String line : plainText.split("\n")) {
			if (line.startsWith("==")) {
				break;
			}
			sb.append(line + "\n");
		}
		return sb.toString().trim();
	}

	public String getFirstPharagraph(String plainText) {
		StringBuffer sb = new StringBuffer();
		for (String line : plainText.split("\n")) {
			if (line.startsWith("==")) {
				break;
			}
			sb.append(line + "\n");
		}
		return sb.toString().trim();
	}

	public String getDefinition() {
		String paragraph = getFirstPharagraph();
		StringBuffer sb = new StringBuffer();

		for (String line : paragraph.split("\n")) {
			if (line.startsWith("|") || line.startsWith("}}") || line.startsWith("{{")) {
				continue;
			}
			sb.append(line + "\n");
		}

		return sb.toString();
	}

	/**
	 * Parse the Infobox template (i.e. parsing a string starting with
	 * &quot;{{Infobox&quot; and ending with &quot;}}&quot;)
	 * 
	 * @return <code>null</code> if the Infobox template wasn't found.
	 */
	public InfoBox getInfoBox() {
		// parseInfoBox is expensive. Doing it only once like other parse*
		// methods
		if (infoBox == null)
			infoBox = parseInfoBox();
		return infoBox;
	}

	public List<String> getLinks() {
		if (pageLinks == null)
			parseLinks();
		return pageLinks;
	}

	public String getPlainText() {

		// String text = wikiText.replaceAll("&gt;", ">");
		// text = text.replaceAll("&lt;", "<");
		// text = text.replaceAll("<ref>.*?</ref>", " ");
		// text = text.replaceAll("</?.*?>", " ");
		// text = text.replaceAll("\\{\\{.*?\\}\\}", " ");
		// text = text.replaceAll("\\[\\[.*?:.*?\\]\\]", " ");
		// text = text.replaceAll("\\[\\[(.*?)\\]\\]", "$1");
		// text = text.replaceAll("\\s(.*?)\\|(\\w+\\s)", " $2");
		// text = text.replaceAll("\\[.*?\\]", " ");
		// text = text.replaceAll("\\'+", "");

		String text = wikiText.replaceAll("&gt;", ">");
		text = text.replaceAll("\r", "");
		text = text.replaceAll("&lt;", "<");
		text = text.replaceAll("(?m:<ref.*>.*?</ref>)", " ");
		text = text.replaceAll("(?m:</?.*?>)", " ");
		text = text.replaceAll("(?ms:<!--.*?-->)", " ");
		// text = text.replaceAll("(?ms:\\{\\{.*?\\}\\})", " ");

		Pattern p = Pattern.compile("(\\{\\{|\\}\\})");
		Matcher m = p.matcher(text);
		boolean found = m.find();

		List<String> items = new ArrayList<String>();
		List<Integer> locs = new ArrayList<Integer>();
		StringBuffer sb = new StringBuffer();

		while (found) {
			String str = m.group();
			items.add(m.group());
			locs.add(m.start());
			// System.out.println(++jj + ": " + m.group());
			m.appendReplacement(sb, "  ");
			found = m.find();
		}
		m.appendTail(sb);

		sb = new StringBuffer(text);

		Stack<Integer> stack = new Stack<Integer>();

		for (int i = 0; i < items.size(); i++) {
			String item1 = items.get(i);

			if (stack.size() == 0) {
				stack.push(i);
			} else {
				int top = stack.peek();
				String item2 = items.get(top);
				if (item1.equals(item2)) {
					stack.push(i);
				} else {
					stack.pop();
					int start = locs.get(top);
					int end = locs.get(i) + 2;

					// System.out.println(i + 1 + ":" + sb.substring(start,
					// end));
					StringBuffer sb2 = new StringBuffer();
					for (int j = 0; j < end - start; j++) {
						sb2.append(" ");
					}

					sb = sb.replace(start, end, sb2.toString());
				}
			}
		}

		text = sb.toString();
		text = text.replaceAll("\\[\\[.*?:.*?\\]\\]", " ");
		text = text.replaceAll("\\'+", "");

		sb = new StringBuffer();

		for (String line : text.split("[\n]+")) {
			line = line.trim();
			if (line.equals("")) {
				continue;
			}

			line = line.replaceAll("[\\s]+", " ");
			sb.append(line + "\n");
		}

		text = sb.toString().trim();

		return text;
	}

	public String getRedirectText() {
		return redirectString;
	}

	public String getText() {
		return wikiText;
	}

	public String getTranslatedTitle(String languageCode) {
		Pattern translatePattern = Pattern.compile("^\\[\\[" + languageCode + ":(.*?)\\]\\]$", Pattern.MULTILINE);
		Matcher matcher = translatePattern.matcher(wikiText);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}

	public boolean isDisambiguationPage() {
		return disambiguation;
	}

	public boolean isRedirect() {
		return redirect;
	}

	/**
	 * Strip wiki formatting characters from the given wiki text.
	 * 
	 * @return
	 */
	// public String getPlainText() {
	// String text = wikiText.replaceAll("&gt;", ">");
	// text = text.replaceAll("&lt;", "<");
	// text = text.replaceAll("<ref>.*?</ref>", " ");
	// text = text.replaceAll("</?.*?>", " ");
	// text = text.replaceAll("\\{\\{.*?\\}\\}", " ");
	// text = text.replaceAll("\\[\\[.*?:.*?\\]\\]", " ");
	// text = text.replaceAll("\\[\\[(.*?)\\]\\]", "$1");
	// text = text.replaceAll("\\s(.*?)\\|(\\w+\\s)", " $2");
	// text = text.replaceAll("\\[.*?\\]", " ");
	// text = text.replaceAll("\\'+", "");
	// return text;
	// }

	public boolean isStub() {
		return stub;
	}

	private void parseCategories() {
		pageCats = new ArrayList<String>();
		// Pattern catPattern = Pattern.compile("\\[\\[Category:(.*?)\\]\\]",
		// Pattern.MULTILINE);
		Matcher matcher = CATEGORY_PATTERN.matcher(wikiText);
		while (matcher.find()) {
			String[] temp = matcher.group(1).split("\\|");
			pageCats.add(temp[0]);
		}
	}

	/**
	 * Parse the Infobox template (i.e. parsing a string starting with
	 * &quot;{{Infobox&quot; and ending with &quot;}}&quot;)
	 * 
	 * @return <code>null</code> if the Infobox template wasn't found.
	 */
	private InfoBox parseInfoBox() {
		String INFOBOX_CONST_STR = "{{Infobox";
		int startPos = wikiText.indexOf(INFOBOX_CONST_STR);
		if (startPos < 0)
			return null;
		int bracketCount = 2;
		int endPos = startPos + INFOBOX_CONST_STR.length();

		if (endPos >= wikiText.length()) {
			return null;
		}
		for (; endPos < wikiText.length(); endPos++) {
			switch (wikiText.charAt(endPos)) {
			case '}':
				bracketCount--;
				break;
			case '{':
				bracketCount++;
				break;
			default:
			}
			if (bracketCount == 0)
				break;
		}
		String infoBoxText;
		if (endPos >= wikiText.length()) {
			infoBoxText = wikiText.substring(startPos);
		} else {
			infoBoxText = wikiText.substring(startPos, endPos + 1);
		}
		infoBoxText = stripCite(infoBoxText); // strip clumsy {{cite}} tags
		// strip any html formatting
		infoBoxText = infoBoxText.replaceAll("&gt;", ">");
		infoBoxText = infoBoxText.replaceAll("&lt;", "<");
		infoBoxText = infoBoxText.replaceAll("<ref.*?>.*?</ref>", " ");
		infoBoxText = infoBoxText.replaceAll("</?.*?>", " ");

		return new InfoBox(infoBoxText);
	}

	private void parseLinks() {
		pageLinks = new ArrayList<String>();

		Matcher matcher = LINKS_PATTERN.matcher(wikiText);
		while (matcher.find()) {
			String[] temp = matcher.group(1).split("\\|");
			if (temp == null || temp.length == 0)
				continue;
			String link = temp[0];
			if (link.contains(":") == false) {
				pageLinks.add(link);
			}
		}
	}

	private String stripCite(String text) {
		String CITE_CONST_STR = "{{cite";
		int startPos = text.indexOf(CITE_CONST_STR);
		if (startPos < 0)
			return text;
		int bracketCount = 2;
		int endPos = startPos + CITE_CONST_STR.length();
		for (; endPos < text.length(); endPos++) {
			switch (text.charAt(endPos)) {
			case '}':
				bracketCount--;
				break;
			case '{':
				bracketCount++;
				break;
			default:
			}
			if (bracketCount == 0)
				break;
		}
		text = text.substring(0, startPos - 1) + text.substring(endPos);
		return stripCite(text);
	}

}

package edu.kaist.ir.wiki;

import java.io.File;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kr.ac.kaist.swrc.jhannanum.hannanum.Workflow;
import kr.ac.kaist.swrc.jhannanum.hannanum.WorkflowFactory;
import kr.ac.kaist.swrc.jhannanum.plugin.MajorPlugin.MorphAnalyzer.ChartMorphAnalyzer.ChartMorphAnalyzer;
import kr.ac.kaist.swrc.jhannanum.plugin.MajorPlugin.PosTagger.HmmPosTagger.HMMTagger;
import kr.ac.kaist.swrc.jhannanum.plugin.SupplementPlugin.MorphemeProcessor.UnknownMorphProcessor.UnknownProcessor;
import kr.ac.kaist.swrc.jhannanum.plugin.SupplementPlugin.PlainTextProcessor.InformalSentenceFilter.InformalSentenceFilter;
import kr.ac.kaist.swrc.jhannanum.plugin.SupplementPlugin.PlainTextProcessor.SentenceSegmentor.SentenceSegmentor;
import edu.jhu.nlp.wikipedia.PageCallbackHandler;
import edu.jhu.nlp.wikipedia.WikiPage;
import edu.jhu.nlp.wikipedia.WikiXMLParser;
import edu.jhu.nlp.wikipedia.WikiXMLParserFactory;
import edu.kaist.ir.io.IOUtils;
import edu.kaist.ir.io.TextFileReader;
import edu.kaist.ir.io.TextFileWriter;
import edu.kaist.ir.poi.ext.Path;
import edu.kaist.ir.tree.trie.Node;
import edu.kaist.ir.tree.trie.Trie;
import edu.kaist.ir.utils.Counter;
import edu.kaist.ir.utils.CounterMap;
import edu.kaist.ir.utils.CounterUtils;
import edu.kaist.ir.utils.IndexedSet;
import edu.kaist.ir.utils.StopWatch;
import edu.kaist.ir.utils.StrUtils;

public class DataHandler {

	class LocationHandler implements PageCallbackHandler {

		TextFileWriter writer;

		private int numDocs;

		private StopWatch stopWatch;

		private IndexedSet<String, String> bigram_loc;

		private Set<String> locSet;

		private Pattern proPat;

		public LocationHandler(TextFileWriter writer) {
			this.writer = writer;
			numDocs = 0;
			stopWatch = new StopWatch();
			stopWatch.start();

			bigram_loc = new IndexedSet<String, String>();
			locSet = new HashSet<String>();

			String[] provinces = { "서울", "인천", "부산", "대구", "대전", "광주", "울산", "경기", "충청", "경상", "전라", "강원", "제주" };
			proPat = Pattern.compile("(" + StrUtils.join("|", provinces) + ")");

			try {
				for (String line : IOUtils.readLines(new File("data/poi.txt"))) {
					String[] parts = line.split("\t");
					for (int i = 0; i < 3; i++) {
						String loc = parts[i];
						for (String tok : loc.split(" ")) {
							for (String ngram : StrUtils.ngrams(tok, 2)) {
								bigram_loc.put(ngram, loc);
							}
							locSet.add(loc);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		protected void finalize() throws Throwable {
			super.finalize();

		}

		@Override
		public void process(WikiPage page) {
			// System.out.println("1-----------------------------");
			// System.out.println(page.getWikiText());
			// System.out.println("2-----------------------------");
			// System.out.println(page.getText());
			// System.out.println("3-----------------------------");

			if (++numDocs % 10000 == 0) {
				System.out.printf("[%d, %s]\n", numDocs, stopWatch.stop());
			}

			String title = page.getTitle().trim();

			// if (!title.equals("강원도 (남)")) {
			// return;
			// }

			String wikiText = page.getWikiText();
			MyWikiTextParser parser = new MyWikiTextParser(wikiText);
			String plainText = parser.getPlainText();
			List<String> topics = parser.getCategories();
			String topicStr = StrUtils.join("|", topics);

			// System.out.println(plainText);

			if (parser.isDisambiguationPage() || parser.isRedirect()) {
				return;
			}

			boolean found = false;
			if (locSet.contains(title)) {
				found = true;
			} else {
				for (String ngram : StrUtils.ngrams(title, 2)) {
					if (bigram_loc.containsKey(ngram)) {
						found = true;
						break;
					}
				}
			}

			// if (!title.contains("문화동")) {
			// return;
			// }

			if (!found) {
				return;
			}

			// System.out.println(title);

			// if (!title.equals("Abraham Lincoln")) {s
			// return;
			// }

			if (title.contains("대학교") || title.contains(":") || title.contains("국회의원")) {
				return;
			}

			if (title.toLowerCase().contains("list of")) {
				return;
			}

			if (!StrUtils.find(wikiText, "행정( )?구역")) {
				return;
			}

			if (!StrUtils.find(wikiText, proPat)) {
				return;
			}

			// if (StrUtils.find(wikiText, "(조선인민|공화국)")) {
			// return;
			// }

			int startYears = -1;
			int endYears = -1;

			for (int i = 0; i < topics.size(); i++) {
				String topic = topics.get(i);
				Matcher m = Pattern.compile("([\\d]+)년 폐지").matcher(topic);
				if (m.find()) {
					int temp = Integer.parseInt(m.group(1));
					if (endYears > temp) {
						endYears = temp;
					}
				}

				m = Pattern.compile("([\\d]+)년 설치").matcher(topic);
				if (m.find()) {
					int temp = Integer.parseInt(m.group(1));
					if (temp > startYears) {
						startYears = temp;
					}
				}
			}

			if (startYears < 0) {
				return;
			}

			if (endYears > 0 && endYears < 2005) {
				return;
			}

			StringBuffer sb = new StringBuffer();
			sb.append(String.format("NO:\t%d\n", numDocs));
			sb.append(String.format("TITLE:\t%s\n", title));
			sb.append(String.format("LOCATION:\t%s\n", ""));
			sb.append(String.format("TOPIC:\t%s\n", topicStr));
			sb.append(String.format("TEXT:\n%s", plainText));

			String output = sb.toString().trim();
			output = StrUtils.normalizeUndefinedChars(output);
			writer.write(output + "\n\n");

		}
	}

	class PoiHandler implements PageCallbackHandler {

		TextFileWriter writer;

		private int numDocs;

		private StopWatch stopWatch;

		private Set<String> poiSet;

		public PoiHandler(TextFileWriter writer) {
			this.writer = writer;
			numDocs = 0;
			stopWatch = new StopWatch();
			stopWatch.start();

			poiSet = new HashSet<String>();

			try {
				for (String line : IOUtils.readLines(new File("data/poi.txt"))) {
					String[] parts = line.split("\t");
					poiSet.add(parts[3]);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		protected void finalize() throws Throwable {
			super.finalize();

		}

		@Override
		public void process(WikiPage page) {
			// System.out.println("1-----------------------------");
			// System.out.println(page.getWikiText());
			// System.out.println("2-----------------------------");
			// System.out.println(page.getText());
			// System.out.println("3-----------------------------");

			if (++numDocs % 1000 == 0) {
				System.out.printf("[%d, %s]\n", numDocs, stopWatch.stop());
			}

			if (page.isDisambiguationPage() || page.isRedirect()) {
				return;
			}

			String title = page.getTitle().trim();

			if (!poiSet.contains(title)) {
				return;
			}

			// if (!title.equals("Abraham Lincoln")) {s
			// return;
			// }

			String wikiText = page.getWikiText();

			if (title.toLowerCase().contains("list of")) {
				return;
			}

			MyWikiTextParser parser = new MyWikiTextParser(wikiText);
			String plainText = parser.getPlainText();
			// String paragraph = parser.getFirstPharagraph(plainText);

			String paragraph = plainText;

			// String definition = parser.getDefinition();

			// System.out.println("-> " + title);
			// System.out.println("-> " + wikiText);
			// System.out.println("-> " + paragraph);
			// System.out.println();

			String paragraph2 = paragraph.toLowerCase();
			title = title.replaceAll("\\(.+\\)", "").trim();
			boolean hasAllTitleTerms = true;

			for (String term : title.split(" ")) {
				if (!paragraph2.contains(term.toLowerCase())) {
					hasAllTitleTerms = false;
					break;
				}
			}

			if (paragraph.length() == 0 || !hasAllTitleTerms) {
				return;
			}

			String topics = StrUtils.join("|", parser.getCategories());

			StringBuffer sb = new StringBuffer();
			sb.append(String.format("NO:\t%d\n", numDocs));
			sb.append(String.format("TITLE:\t%s\n", title));
			sb.append(String.format("TOPIC:\t%s\n", topics));
			sb.append(String.format("TEXT:\n%s", paragraph));

			writer.write(sb.toString().trim() + "\n\n");

		}
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		DataHandler dh = new DataHandler();
		// dh.extractPOIs();
		// dh.extractLocationCandidates();
		// dh.makeTermVectors();
		dh.mapLocationToWiki();
		System.out.println("process ends.");
	}

	private String cleanNoise(String text) {
		StringBuffer sb = new StringBuffer();

		// text = text.replace("|", "\\|");

		Pattern p = Pattern.compile("\\{[^\\{\\}]+\\}", Pattern.MULTILINE);
		Matcher m = null;

		for (int i = 0; i < 2; i++) {
			m = p.matcher(text);
			sb = new StringBuffer();

			while (m.find()) {
				m.appendReplacement(sb, "");
			}
			m.appendTail(sb);
			text = sb.toString();
		}

		p = Pattern.compile("\\[\\[([^\\[\\]]+)\\]\\]");
		m = p.matcher(text);
		sb = new StringBuffer();

		while (m.find()) {
			String group = m.group(1);
			String[] parts = group.split("\\|");
			m.appendReplacement(sb, parts[parts.length - 1]);
		}
		m.appendTail(sb);

		text = sb.toString();
		text = text.replaceAll("\\([^\\(\\)]+\\)", "");

		// sb = new StringBuffer();
		//
		// for (String line : text.split("\n+")) {
		// if (line.contains("=")) {
		// continue;
		// }
		//
		// line = StrUtils.normalizePunctuations(line);
		// line = StrUtils.normalizeSpaces(line);
		// sb.append(line + "\n");
		// }
		//
		// text = sb.toString().trim();

		return text;
	}

	public void extractLocationCandidates() throws Exception {
		File inputFile = new File(Path.PROJECT_DIR, "wiki/kowiki-20120828-pages-articles.xml.bz2");
		File outputFile = new File(Path.PROJECT_DIR, "wiki/wiki_location_candidate.txt");

		TextFileWriter writer = new TextFileWriter(outputFile);
		WikiXMLParser wxsp = WikiXMLParserFactory.getSAXParser(inputFile.getPath());
		wxsp.setPageCallback(new LocationHandler(writer));
		wxsp.parse();
		writer.close();
	}

	public void extractPOIs() throws Exception {
		File inputFile = new File(Path.PROJECT_DIR, "wiki/kowiki-20120828-pages-articles.xml.bz2");
		File outputFile = new File(Path.PROJECT_DIR, "wiki/poi.txt");

		TextFileWriter writer = new TextFileWriter(outputFile);
		WikiXMLParser wxsp = WikiXMLParserFactory.getSAXParser(inputFile.getPath());
		wxsp.setPageCallback(new PoiHandler(writer));
		wxsp.parse();
		writer.close();
	}

	public void makeTermVectors() throws Exception {
		String[] inputFileNames = { "wiki_location_candidate.txt", "wiki_poi.txt" };
		String[] outputFileNames = { "wiki_location_candidate_term.txt", "wiki_poi_term.txt" };

		for (int i = 0; i < inputFileNames.length - 1; i++) {
			File inputFile = new File(Path.PROJECT_DIR + "wiki", inputFileNames[i]);
			File ouptutFile = new File(Path.PROJECT_DIR + "wiki", outputFileNames[i]);
			makeTermVectors(inputFile, ouptutFile);
		}
	}

	private void makeTermVectors(File inputFile, File outputFile) throws Exception {
		// Workflow workflow = new Workflow();
		// workflow.appendPlainTextProcessor(new SentenceSegmentor(), null);
		// workflow.appendPlainTextProcessor(new InformalSentenceFilter(),
		// null);
		//
		// workflow.setMorphAnalyzer(new ChartMorphAnalyzer(),
		// "conf/plugin/MajorPlugin/MorphAnalyzer/ChartMorphAnalyzer.json");
		// workflow.appendMorphemeProcessor(new UnknownProcessor(), null);
		// workflow.setPosTagger(new HMMTagger(),
		// "conf/plugin/MajorPlugin/PosTagger/HmmPosTagger.json");
		// workflow.activateWorkflow(false);

		Workflow workflow = WorkflowFactory.getPredefinedWorkflow(WorkflowFactory.WORKFLOW_HMM_POS_TAGGER);
		workflow.activateWorkflow(false);

		TextFileReader reader = new TextFileReader(inputFile);
		TextFileWriter writer = new TextFileWriter(outputFile);

		Set<String> provinceSet = new HashSet<String>();
		String[] provinces = { "서울특별시", "인천광역시", "부산광역시", "대구광역시", "대전광역시", "광주광역시", "울산광역시", "경기도", "충청남도", "충청북도", "경상남도", "경상북도", "전라남도", "전라북도", "강원도",
				"제주특별자치도" };
		for (String name : provinces) {
			provinceSet.add(name);
		}

		while (reader.hasNext()) {
			reader.print(1000);
			List<String> lines = reader.getNextLines();
			String title = lines.get(1).split("\t")[1];

			StringBuffer sb = new StringBuffer();
			for (int i = 5; i < lines.size(); i++) {
				String line = lines.get(i);
				sb.append(line);
				if (i != lines.size() - 1) {
					sb.append("\n");
				}
			}

			String text = StrUtils.join("\n", lines, 5);
			// text = StrUtils.normalizeUndefinedChars(text);
			text = cleanNoise(text);

			if (text.length() == 0) {
				continue;
			}

			try {
				text = cleanNoise(text);
				workflow.analyze(text);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.printf("error: %s\n", title);
				continue;
			}

			String res = workflow.getResultOfDocument().trim();

			Counter<String> counter = nounCounts(res);

			if (title.equals("강원도 (남)")) {
				System.out.println(title);
				System.out.println(counter.toString());
			}

			if (counter.totalCount() < 5) {
				continue;
			}

			NumberFormat nf = NumberFormat.getInstance();
			nf.setMinimumFractionDigits(0);
			nf.setGroupingUsed(false);

			String output = title + "\t" + CounterUtils.toString(" ", counter, nf);
			writer.write(output + "\n");

			// System.out.println(output);

			// System.out.println(res);
			// System.out.println();

		}
		reader.printLast();
		reader.close();
		writer.close();
	}

	public void mapLocationToWiki() throws Exception {
		IndexedSet<String, String> bigram_loc = new IndexedSet<String, String>();
		CounterMap<String, String> counterMap = new CounterMap<String, String>();
		TextFileReader reader = new TextFileReader(new File(Path.PROJECT_DIR, "wiki/wiki_location_candidate_term.txt"));
		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.split("\t");
			if (parts.length != 2) {
				continue;
			}

			String loc = parts[0];
			String countStr = parts[1];

			Counter<String> counter = CounterUtils.counter(countStr);
			counterMap.setCounter(loc, counter);

			for (String tok : loc.split(" ")) {
				for (String bigram : StrUtils.ngrams(tok, 2)) {
					bigram_loc.put(bigram, loc);
				}
			}
		}
		reader.close();

		counterMap.tfidf();

		Set<String> provinceSet = new HashSet<String>();
		String[] provinces = { "서울특별시", "인천광역시", "부산광역시", "대구광역시", "대전광역시", "광주광역시", "울산광역시", "경기도", "충청남도", "충청북도", "경상남도", "경상북도", "전라남도", "전라북도", "강원도",
				"제주특별자치도" };
		for (String name : provinces) {
			provinceSet.add(name);
		}

		Trie<String> trie = new Trie<String>();
		for (String line : IOUtils.readLines(new File("data/poi.txt"))) {
			String[] parts = line.split("\t");
			parts[0] = parts[0].replaceAll("서울", "서울특별시");
			parts[0] = parts[0].replaceAll("인천", "인천광역시");
			parts[0] = parts[0].replaceAll("부산", "부산광역시");
			parts[0] = parts[0].replaceAll("대구", "대구광역시");
			parts[0] = parts[0].replaceAll("울산", "울산광역시");
			parts[0] = parts[0].replaceAll("대전", "대전광역시");
			parts[0] = parts[0].replaceAll("광주", "광주광역시");
			parts[0] = parts[0].replaceAll("제주도", "제주특별자치도");
			String path = StrUtils.join("-", parts, 0, parts.length - 1);
			trie.insert(path.split("-"));
		}

		List<Node<String>> nodes = trie.allNodes();
		CounterMap<String, String> ret = new CounterMap<String, String>();

		Workflow workflow = WorkflowFactory.getPredefinedWorkflow(WorkflowFactory.WORKFLOW_HMM_POS_TAGGER);
		workflow.activateWorkflow(false);

		for (int i = 0; i < nodes.size(); i++) {
			Node<String> node = nodes.get(i);
			String currentLoc = node.key();
			String path = node.keyPath("-");

			workflow.analyze(path.replace("-", " "));

			Counter<String> counter1 = new Counter<String>();
			for (String loc : path.split("-")) {
				for (String tok : loc.split(" ")) {
					for (String ngram : StrUtils.ngrams(tok, 2)) {
						counter1.incrementCount(ngram, 1);
					}
				}
			}

			Counter<String> counter2 = nounCounts(workflow.getResultOfDocument().trim());

			// for (String loc : path.split("-")) {
			// counter1.incrementCount(loc, 1);
			// }

			// String temp = currentLoc.replaceAll(" \\([^\\(\\)]+\\)", "");

			if (counterMap.containsKey(currentLoc)) {
				Counter<String> counter = counterMap.getCounter(currentLoc);
				ret.setCounter(path + "\t" + currentLoc, counter);
			} else {
				Set<String> locSet = new HashSet<String>();
				for (String tok : currentLoc.split(" ")) {
					for (String ngram : StrUtils.ngrams(tok, 2)) {
						if (bigram_loc.containsKey(ngram)) {
							for (String loc : bigram_loc.get(ngram)) {
								locSet.add(loc);
							}
						}
					}
				}

				if (locSet.size() == 0) {
					continue;
				}

				if (currentLoc.equals("강원도")) {
					System.out.println();
				}

				Counter<String> loc_cosine = new Counter<String>();

				for (String loc : locSet) {
					Counter<String> counter3 = new Counter<String>();
					for (String tok : loc.split(" ")) {
						for (String ngram : StrUtils.ngrams(tok, 2)) {
							counter3.incrementCount(ngram, 1);
						}
					}

					Counter<String> counter4 = counterMap.getCounter(loc);

					double cosine1 = CounterUtils.cosine(counter1, counter3);
					double cosine2 = CounterUtils.cosine(counter2, counter4);
					loc_cosine.setCount(loc, Math.exp(cosine1) * Math.exp(cosine2));
				}

				loc_cosine.normalize();

				String loc = loc_cosine.argMax();

				// if (loc_cosine.getCount(loc) >= 0.2) {
				ret.setCounter(path + "\t" + loc, counterMap.getCounter(loc));
				// }
				// System.out.println(path);
				// System.out.println(loc_cosine);
				// System.out.println();
			}
		}

		TextFileWriter writer = new TextFileWriter(new File(Path.PROJECT_DIR, "wiki/wiki_location_term.txt"));

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(6);
		nf.setGroupingUsed(false);

		for (String path : new TreeSet<String>(ret.keySet())) {
			Counter<String> counter = ret.getCounter(path);
			String[] parts = path.split("\t");
			parts[0] = parts[0].replaceAll("서울특별시", "서울");
			parts[0] = parts[0].replaceAll("인천광역시", "인천");
			parts[0] = parts[0].replaceAll("부산광역시", "부산");
			parts[0] = parts[0].replaceAll("대구광역시", "대구");
			parts[0] = parts[0].replaceAll("울산광역시", "울산");
			parts[0] = parts[0].replaceAll("대전광역시", "대전");
			parts[0] = parts[0].replaceAll("광주광역시", "광주");
			parts[0] = parts[0].replaceAll("제주특별자치도", "제주도");
			String output = parts[0] + "\t" + parts[1] + "\t" + CounterUtils.toString(" ", counter, nf);
			writer.write(output + "\n");
		}
		writer.close();

	}

	private Counter<String> nounCounts(String res) {
		Counter<String> ret = new Counter<String>();
		for (String sent : res.split("\n\n\n")) {
			for (String line : sent.split("\n\n")) {
				String[] splits = line.split("\n\t");
				String[] parts = splits[1].split("\\+");
				for (String part : parts) {
					String[] two = StrUtils.split2Two("/", part);

					if (two == null || two.length != 2) {
						continue;
					}

					String morpheme = two[0];
					String tag = two[1];

					if (morpheme.length() == 1 || StrUtils.find(morpheme, "[\\d]+") || StrUtils.find(morpheme, "\\p{Punct}+")) {
						continue;
					}

					if (tag.startsWith("n")) {
						ret.incrementCount(morpheme, 1);
					}
				}
			}
		}

		return ret;
	}

	public void show() {
		TextFileReader reader = new TextFileReader(new File(Path.PROJECT_DIR, "wiki/kowiki-20120828-pages-articles.xml"));
		while (reader.hasNext()) {
			if (reader.getNumLines() > 200) {
				break;
			}
			String line = reader.next();
			System.out.println(line);
		}
		reader.close();

	}

}

package edu.kaist.ir.poi.ext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kr.ac.kaist.swrc.jhannanum.hannanum.Workflow;
import kr.ac.kaist.swrc.jhannanum.plugin.MajorPlugin.MorphAnalyzer.ChartMorphAnalyzer.ChartMorphAnalyzer;
import kr.ac.kaist.swrc.jhannanum.plugin.MajorPlugin.PosTagger.HmmPosTagger.HMMTagger;
import kr.ac.kaist.swrc.jhannanum.plugin.SupplementPlugin.MorphemeProcessor.UnknownMorphProcessor.UnknownProcessor;
import kr.ac.kaist.swrc.jhannanum.plugin.SupplementPlugin.PlainTextProcessor.InformalSentenceFilter.InformalSentenceFilter;
import kr.ac.kaist.swrc.jhannanum.plugin.SupplementPlugin.PlainTextProcessor.SentenceSegmentor.SentenceSegmentor;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.jsoup.Jsoup;

import edu.kaist.ir.io.IOUtils;
import edu.kaist.ir.io.TextFileReader;
import edu.kaist.ir.io.TextFileWriter;
import edu.kaist.ir.poi.structure.Document;
import edu.kaist.ir.poi.structure.POI;
import edu.kaist.ir.poi.structure.Sentence;
import edu.kaist.ir.utils.Counter;
import edu.kaist.ir.utils.IndexedList;
import edu.kaist.ir.utils.Pair;
import edu.kaist.ir.utils.StrUtils;

/**
 * This class is in charge of several major text processing.
 * 
 * @author Heung-Seon Oh
 * 
 */
public class DataHandler {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		DataHandler dh = new DataHandler();
		// dh.extractTextFromXLS1();
		// dh.extractTextFromXLS2();
		// dh.extractTextFromBlogs();
		// dh.extractPoiEntries();
		// dh.tagPoiEntries();

		// dh.tagBlogs();
		// dh.filterBlogs();
		dh.getStatistics();
		// dh.test();
		System.out.println("process ends.");
	}

	public void getStatistics() throws Exception {
		File outputDir = new File("data/raw");
		IOUtils.deleteFilesUnder(outputDir);

		List<File> files = IOUtils.getFilesUnder(new File("D:/Test_Collection/6_testcollection_filter_2_tagged"));

		POIExtractor ext = new POIExtractor(1, 5, true, 2, false);
		ext.index(new File("data/poi_tagged.txt"));

		int numDocs = 0;
		Counter<String> poiCounts = new Counter<String>();
		Counter<String> mentionCounts = new Counter<String>();
		Counter<String> ambCounts = new Counter<String>();

		for (int i = 0; i < files.size(); i++) {
			File inputFile = files.get(i);
			File outputFile = new File(outputDir, inputFile.getName());

			String text = IOUtils.readText(inputFile);
			Document doc = null;

			try {
				doc = Document.createFromTaggedText2(text);
			} catch (Exception e) {
				// e.printStackTrace();
				inputFile.delete();
				continue;
			}

			int numNouns = 0;

			for (int j = 0; j < doc.sentSize(); j++) {
				Sentence sent = doc.sentence(j);
				String[] tags = sent.tags();
				for (int k = 0; k < tags.length; k++) {
					if (tags[k].startsWith("n")) {
						numNouns++;
					}
				}
			}

			if (numNouns < 50) {
				inputFile.delete();
				continue;
			}

			ext.extract(doc);

			numDocs++;

			for (Sentence sent : doc.sentences()) {
				IndexedList<Pair<Integer, Integer>, POI> poiAnno = sent.poiAnnotations();
				for (Pair<Integer, Integer> pair : poiAnno.keySet()) {
					List<POI> poiList = poiAnno.get(pair);
					int x = 0;
					for (POI poi : poiList) {
						poiCounts.incrementCount(poi.toString(), 1);
						x++;
					}
					if(x>1)
						ambCounts.incrementCount("fuck", 1);
					String mention = sent.text().substring(pair.getFirst(), pair.getSecond());
					mentionCounts.incrementCount(mention, 1);
				}
			}
		}

		StringBuffer sb = new StringBuffer();
		sb.append(String.format("doc size:\t%d\n", numDocs));
		sb.append(String.format("# of POIs:\t%d\n", (int) poiCounts.totalCount()));
		sb.append(String.format("# of POI types:\t%d\n", poiCounts.keySet().size()));
		sb.append(String.format("# of mentions:\t%d\n", (int) mentionCounts.totalCount()));
		sb.append(String.format("# of ambiguous:\t%d\n", (int) ambCounts.totalCount()));
		sb.append(String.format("POI/doc:\t%f\n", poiCounts.totalCount() / numDocs));
		sb.append(String.format("mention/doc:\t%f\n", mentionCounts.totalCount() / numDocs));
		sb.append(String.format("POI/mention:\t%f\n", poiCounts.totalCount() / mentionCounts.totalCount()));
		sb.append(String.format("mention/POI:\t%f\n", mentionCounts.totalCount() / poiCounts.totalCount()));

		Counter<String> topLevelCounts = new Counter<String>();
		Counter<String> categoryCounts = new Counter<String>();
		int index = 0;
		List<String> lines = IOUtils.readLines(new File("data/poi_category.txt"), "UTF-8", Integer.MAX_VALUE);
		//BufferedReader inFile = new BufferedReader(new InputStreamReader(new FileInputStream(new File("data/poi_category.txt")), "UTF-8"));
		//System.out.println(lines.size());
		
		for (String poi : poiCounts.keySet()) {
			double count = poiCounts.getCount(poi);
			String top = poi.split("-")[0];
			topLevelCounts.incrementCount(top, count);
			categoryCounts.incrementCount(lines.get(index), count);
			index++;
		}

		sb.append(topLevelCounts.toString() + "\n");
		sb.append(categoryCounts.toString() + "\n");
		System.out.println(sb);

	}

	public void test() throws Exception {
		String text = IOUtils.readText(new File(Path.PROJECT_DIR, "naver_2005math[23399683].txt"));

		Pattern p = Pattern.compile("&[^&;]+;");
		Matcher m = p.matcher(text);

		Set<String> set = new HashSet<String>();
		StringBuffer sb = new StringBuffer();

		while (m.find()) {
			String str = m.group();
			set.add(str);
			m.appendReplacement(sb, " ");
		}
		m.appendTail(sb);

		System.out.println(set);
	}

	public void tagBlogs() throws Exception {
		File outputDir = new File(Path.PROJECT_DIR, "test collection/tagged");
		// IOUtils.deleteFilesUnder(outputDir);

		List<File> files = IOUtils.getFilesUnder(new File(Path.PROJECT_DIR, "test collection/raw"));

		Workflow workflow = new Workflow();
		workflow.appendPlainTextProcessor(new SentenceSegmentor(), null);
		workflow.appendPlainTextProcessor(new InformalSentenceFilter(), null);

		workflow.setMorphAnalyzer(new ChartMorphAnalyzer(), "conf/plugin/MajorPlugin/MorphAnalyzer/ChartMorphAnalyzer.json");
		workflow.appendMorphemeProcessor(new UnknownProcessor(), null);
		workflow.setPosTagger(new HMMTagger(), "conf/plugin/MajorPlugin/PosTagger/HmmPosTagger.json");
		workflow.activateWorkflow(false);

		File visitFile = new File(Path.PROJECT_DIR, "visit_file.txt");
		Set<String> visited = new HashSet<String>();

		if (visitFile.exists()) {
			visited = IOUtils.readSet(visitFile);
		}

		for (int i = 0; i < files.size(); i++) {
			File inputFile = files.get(i);
			File outputFile = new File(outputDir, inputFile.getName());

			if (visited.contains(inputFile.getPath())) {
				continue;
			}

			String text = IOUtils.readText(inputFile);
			// System.out.println(text);

			text = StrUtils.normalizeUndefinedChars(text);

			int idx = text.indexOf("Content:");
			text = text.substring(idx + "Content:".length()).trim();

			// text = text.replaceAll("[\n]+", "\n").trim();

			// String[] lines = text.split("\n");
			// text = StrUtils.join("\n", lines, 7);

			String res = "";

			try {
				workflow.analyze(text);
				res = workflow.getResultOfDocument().trim();
			} catch (Exception e) {
				e.printStackTrace();
				visited.add(inputFile.getPath());
				IOUtils.write(visitFile, StrUtils.join("\n", new ArrayList<String>(visited)));
				return;
			}

			visited.add(inputFile.getPath());

			// System.out.println(text);

			// System.out.println(toSindiFormat(inputFile.getName(), doc));

			IOUtils.write(outputFile, IOUtils.UTF_8, false, res);
			//
		}

		IOUtils.write(visitFile, StrUtils.join("\n", new ArrayList<String>(visited)));
	}

	public void filterBlogs() throws Exception {
		File outputDir = new File(Path.PROJECT_DIR, "test collection/filtered");
		IOUtils.deleteFilesUnder(outputDir);

		List<File> files = IOUtils.getFilesUnder(new File(Path.PROJECT_DIR, "test collection/tagged"));

		POIExtractor ext = new POIExtractor(2, 5, true, 2, false);
		ext.index(new File("data/poi_tagged.txt"));

		POIDisambiguator disam = new POIDisambiguator();
		disam.readWikiData(new File(Path.PROJECT_DIR, "wiki/wiki_location_term.txt"), ext.poiIndexer());

		for (int i = 0; i < files.size(); i++) {
			File inputFile = files.get(i);
			File outputFile = new File(outputDir, inputFile.getName());

			String text = IOUtils.readText(inputFile);
			Document doc = null;

			try {
				doc = Document.createFromTaggedText2(text);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}

			int numNouns = 0;

			for (int j = 0; j < doc.sentSize(); j++) {
				Sentence sent = doc.sentence(j);
				String[] tags = sent.tags();
				for (int k = 0; k < tags.length; k++) {
					if (tags[k].startsWith("n")) {
						numNouns++;
					}
				}
			}

			if (numNouns < 50) {
				continue;
			}

			ext.extract(doc);

			int numPOIs = 0;

			for (Sentence sent : doc.sentences()) {
				IndexedList<Pair<Integer, Integer>, POI> poiAnno = sent.poiAnnotations();
				if (poiAnno.keySet().size() > 0) {
					numPOIs++;
				}
			}

			if (numPOIs < 10) {
				continue;
			}

			// CounterMap<String, String> mention_poi_score =
			// disam.predict(doc);
			// disam.disambiguate(doc, mention_poi_score);

			// System.out.println(doc.toString());
			// System.out.println(mention_poi_score);
			// System.out.println();

			String res = toSindiFormat(inputFile.getName(), doc);

			// System.out.println(toSindiFormat(inputFile.getName(), doc));

			//IOUtils.write(outputFile, IOUtils.UTF_8, false, res);

			//
		}
	}

	private String toSindiFormat(String docId, Document doc) {
		StringBuffer sb = new StringBuffer();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append(String.format("<document id=\"%s\" origId=\"DEFAULT_DID_00001\">\n", docId));

		int numPOIs = 0;

		for (int i = 0; i < doc.sentSize(); i++) {
			Sentence sent = doc.sentence(i);

			String sentText = sent.text();
			sentText = sentText.replaceAll("\n", "&#10;");
			sentText = sentText.replaceAll(">", "&gt;");
			sentText = sentText.replaceAll("<", "&lt;");
			sentText = sentText.replaceAll("\"", "&quot;");
			sentText = sentText.replaceAll("'", "&apos;");
			sentText = sentText.replaceAll("&", "&amp;");

			sb.append(String.format("<sentence id=\"DEFAULT.d1.s%d\" text=\"%s\">\n", i, sentText));
			IndexedList<Pair<Integer, Integer>, POI> poiAnno = sent.poiAnnotations();

			if (poiAnno.keySet().size() > 0) {
				List<Pair<Integer, Integer>> indexPairs = new ArrayList<Pair<Integer, Integer>>(poiAnno.keySet());
				Collections.sort(indexPairs, new PairUtils.PairComparator(false));

				for (Pair<Integer, Integer> indexPair : indexPairs) {
					List<POI> pois = poiAnno.get(indexPair);
					for (POI poi : pois) {
						int start = indexPair.getFirst();
						int end = indexPair.getSecond();
						String str = sent.text().substring(start, end);
						String poiStr = poi.toString();
						sb.append(String.format(

						"    <entity charOffset=\"%d-%d\" id=\"DEFAULT.d1.s%d.e0\" origId=\"%d\" type=\"%s\" nn=\"%s\"/>\n",

						start, end - 1, i, numPOIs, poiStr, str

						));
					}
				}
			}

			sb.append("</sentence>\n");
		}

		sb.append("</document>");
		return sb.toString();
	}

	public void filterNaverBlogs2() throws Exception {
		File outputDir = new File(Path.PROJECT_DIR, "test collection/filtered");
		IOUtils.deleteFilesUnder(outputDir);

		List<File> files = IOUtils.getFilesUnder(new File(Path.PROJECT_DIR, "test collection/raw"));

		POIExtractor ext = new POIExtractor(2, 5, true, 2, false);
		ext.index(new File("data/poi_tagged.txt"));

		Workflow workflow = new Workflow();
		workflow.appendPlainTextProcessor(new SentenceSegmentor(), null);
		workflow.appendPlainTextProcessor(new InformalSentenceFilter(), null);

		// workflow.setMorphAnalyzer(new ChartMorphAnalyzer(),
		// "conf/plugin/MajorPlugin/MorphAnalyzer/ChartMorphAnalyzer.json");
		// workflow.appendMorphemeProcessor(new UnknownProcessor(), null);
		// workflow.setPosTagger(new HMMTagger(),
		// "conf/plugin/MajorPlugin/PosTagger/HmmPosTagger.json");
		workflow.activateWorkflow(false);

		for (int i = 0; i < files.size(); i++) {
			File inputFile = files.get(i);
			File outputFile = new File(outputDir, inputFile.getName());

			String text = IOUtils.readText(inputFile);
			// System.out.println(text);

			text = text.replaceAll("[\n]+", "\n").trim();

			workflow.analyze(text);
			String res = workflow.getResultOfDocument().trim();

			Document doc = Document.createFromRawText(text);
			ext.extract(doc);

			int numPOIs = 0;

			for (Sentence sent : doc.sentences()) {
				IndexedList<Pair<Integer, Integer>, POI> poiAnno = sent.poiAnnotations();
				if (poiAnno.keySet().size() > 0) {
					numPOIs++;
				}
			}

			if (numPOIs > 10) {
				// System.out.println(text);

				// System.out.println(doc.toString());
				// System.out.println();

				IOUtils.write(outputFile, IOUtils.UTF_8, false, text);
			}

			//
		}
	}

	public void extractPoiEntries() {
		TextFileReader reader = new TextFileReader(new File("data/20120621 KTO_POI_with_Field.txt"));
		TextFileWriter writer = new TextFileWriter(new File("data/poi.txt"));
		while (reader.hasNext()) {
			String line = reader.next();

			if (reader.getNumLines() == 1) {
				continue;
			}

			String[] parts = line.split("\t");
			String poiStr = parts[2];
			String loc1 = parts[6];
			String loc2 = parts[7];
			String loc3 = parts[8];
			String output = StrUtils.join("\t", new String[] { loc1, loc2, loc3, poiStr });
			writer.write(output + "\n");
		}
		reader.close();
		writer.close();
	}

	public void tagPOS() throws Exception {
		Workflow workflow = new Workflow();
		workflow.appendPlainTextProcessor(new SentenceSegmentor(), null);
		workflow.appendPlainTextProcessor(new InformalSentenceFilter(), null);

		workflow.setMorphAnalyzer(new ChartMorphAnalyzer(), "conf/plugin/MajorPlugin/MorphAnalyzer/ChartMorphAnalyzer.json");
		workflow.appendMorphemeProcessor(new UnknownProcessor(), null);
		workflow.setPosTagger(new HMMTagger(), "conf/plugin/MajorPlugin/PosTagger/HmmPosTagger.json");
		workflow.activateWorkflow(false);

		List<File> files = IOUtils.getFilesUnder(new File("data/sample/"));
		File outputDir = new File("data/sample_pos");

		for (int i = 0; i < files.size(); i++) {
			File file = files.get(i);
			if (!file.getName().endsWith(".txt")) {
				continue;
			}
			String text = IOUtils.readText(file);
			workflow.analyze(text);
			String res = workflow.getResultOfDocument().trim();

			StringBuffer sb = new StringBuffer();

			for (String sent : res.split("\n\n\n")) {
				String[] parts = sent.split("\n\n");
				for (String part : parts) {
					String[] segs = part.split("\n\t");
					sb.append(String.format("%s\t%s\n", segs[0], segs[1]));
				}
				sb.append("\n");
			}

			IOUtils.write(new File(outputDir, file.getName()), sb.toString().trim());
		}
	}

	public void tagPoiEntries() throws Exception {
		Workflow workflow = new Workflow();
		workflow.appendPlainTextProcessor(new SentenceSegmentor(), null);
		workflow.appendPlainTextProcessor(new InformalSentenceFilter(), null);

		workflow.setMorphAnalyzer(new ChartMorphAnalyzer(), "conf/plugin/MajorPlugin/MorphAnalyzer/ChartMorphAnalyzer.json");
		workflow.appendMorphemeProcessor(new UnknownProcessor(), null);
		workflow.setPosTagger(new HMMTagger(), "conf/plugin/MajorPlugin/PosTagger/HmmPosTagger.json");
		workflow.activateWorkflow(false);

		TextFileReader reader = new TextFileReader(new File("data/poi.txt"));
		TextFileWriter writer = new TextFileWriter(new File("data/poi_tagged.txt"));

		while (reader.hasNext()) {
			String line = reader.next();

			if (reader.getNumLines() == 1) {
				continue;
			}

			String[] inputs = line.split("\t");
			// entity = entity.replaceAll("\\([^\\(\\)]+\\)", "");
			// String[] outputs = new String[inputs.length];
			List<String> outputs = new ArrayList<String>();

			for (int i = 0; i < inputs.length; i++) {
				String input = inputs[i];
				if (i == 3) {
					workflow.analyze(input);
					String tagged = workflow.getResultOfDocument().trim();
					tagged = tagged.replaceAll("\n\t", "\t");

					String[] slits = tagged.split("[\\n]+");
					for (int j = 0; j < slits.length; j++) {
						slits[j] = slits[j].replaceAll("\t", "=>");
					}

					outputs.add(input);
					outputs.add(StrUtils.join(" ", slits));
				} else {
					outputs.add(input);
				}

			}

			// System.out.println(StrUtils.join("\t", inputs));
			// System.out.println(StrUtils.join("\t", outputs));
			// System.out.println();

			String res = StrUtils.join("\t", outputs);
			writer.write(res + "\n");
		}

		reader.close();
		writer.close();
	}

	public void extractTextFromXLS1() throws Exception {
		for (File file : IOUtils.getFilesUnder(new File(Path.PROJECT_DIR, "sample/xls"))) {
			if (!file.getName().contains("여행후기")) {
				continue;
			}

			String outputFileName = file.getPath().replace(".xls", ".txt");
			outputFileName = outputFileName.replace("xls", "text");
			TextFileWriter writer = new TextFileWriter(outputFileName);

			POIFSFileSystem fs = new POIFSFileSystem(new FileInputStream(file));
			HSSFWorkbook wb = new HSSFWorkbook(fs);
			HSSFSheet sheet = wb.getSheetAt(0);
			for (int i = 0; i < sheet.getLastRowNum(); i++) {
				HSSFRow row = sheet.getRow(i);
				StringBuffer sb = new StringBuffer();
				for (int j = 0; j < row.getLastCellNum(); j++) {
					HSSFCell cell = row.getCell(j);
					String text = cell.toString();
					text = StringEscapeUtils.unescapeHtml3(text);

					Pattern p = Pattern.compile("<[^<>]+>");
					Matcher m = p.matcher(text);
					boolean found = m.find();
					StringBuffer sb2 = new StringBuffer();

					while (found) {
						m.appendReplacement(sb2, "\n");
						found = m.find();
					}
					m.appendTail(sb2);

					sb.append(sb2.toString().trim() + "\t");
				}

				String text = sb.toString().trim();
				String[] parts = text.split("\t");

				if (parts.length != 2) {
					continue;
				}

				String title = parts[0];
				String content = parts[1];

				StringBuffer sb3 = new StringBuffer();
				for (String line : content.split("[\\n]+")) {
					line = line.replaceAll("&nbsp;", " ");
					// line = line.trim();
					// line = line.replaceAll("\\r", "");
					line = StrUtils.normalizeSpaces(line, false).trim();

					// if (line.length() == 1) {
					//
					// if(line.equals(" ")){
					// System.out.printf("[%d]\n", line.length());
					// }
					//
					// System.out.printf("[%s]\n", line.charAt(0));
					// }

					if (line.equals("") || line.equals(" ")) {
						continue;
					}
					sb3.append(line + "\n");
				}
				content = sb3.toString().trim();

				writer.write("TITLE:\n");
				writer.write(title + "\n");
				writer.write("CONTENT:\n");
				writer.write(content + "\n\n");

			}
		}
	}

	public void extractTextFromXLS2() throws Exception {
		for (File file : IOUtils.getFilesUnder(new File(Path.PROJECT_DIR, "sample/xls"))) {
			if (!file.getName().contains("상품평")) {
				continue;
			}

			String outputFileName = file.getPath().replace(".xls", ".txt");
			outputFileName = outputFileName.replace("xls", "text");
			TextFileWriter writer = new TextFileWriter(outputFileName);

			POIFSFileSystem fs = new POIFSFileSystem(new FileInputStream(file));
			HSSFWorkbook wb = new HSSFWorkbook(fs);
			HSSFSheet sheet = wb.getSheetAt(0);
			for (int i = 0; i < sheet.getLastRowNum(); i++) {
				HSSFRow row = sheet.getRow(i);
				StringBuffer sb = new StringBuffer();
				for (int j = 0; j < row.getLastCellNum(); j++) {
					HSSFCell cell = row.getCell(j);
					String text = cell.toString();
					text = StringEscapeUtils.unescapeHtml3(text);

					Pattern p = Pattern.compile("<[^<>]+>");
					Matcher m = p.matcher(text);
					boolean found = m.find();
					StringBuffer sb2 = new StringBuffer();

					while (found) {
						m.appendReplacement(sb2, "\n");
						found = m.find();
					}
					m.appendTail(sb2);

					sb.append(sb2.toString().trim() + "\t");
				}

				String text = sb.toString().trim();
				String[] parts = text.split("\t");

				if (parts.length != 6) {
					continue;
				}

				String code = parts[0];
				String title = parts[1];

				String review = parts[2];
				String country = parts[3];
				String city = parts[4];
				String schedule = parts[5];

				StringBuffer sb3 = new StringBuffer();
				for (String line : schedule.split("[\\n]+")) {
					line = line.replaceAll("&nbsp;", " ");
					// line = line.trim();
					// line = line.replaceAll("\\r", "");
					line = StrUtils.normalizeSpaces(line, false).trim();

					// if (line.length() == 1) {
					//
					// if(line.equals(" ")){
					// System.out.printf("[%d]\n", line.length());
					// }
					//
					// System.out.printf("[%s]\n", line.charAt(0));
					// }

					if (line.equals("") || line.equals(" ")) {
						continue;
					}
					sb3.append(line + "\n");
				}
				schedule = sb3.toString().trim();

				writer.write("TITLE:\n");
				writer.write(title + "\n");
				writer.write("REVIEW:\n");
				writer.write(review + "\n");
				writer.write("SCHEDULE:\n");
				writer.write(schedule + "\n\n");

			}
		}
	}

	public void extractTextFromBlogs() throws Exception {
		File dataDir = new File(Path.PROJECT_DIR, "POI/blogs");
		File outputFile = new File(Path.PROJECT_DIR, "POI/blogs.txt");

		TextFileWriter writer = new TextFileWriter(outputFile);

		CleanerProperties props = new CleanerProperties();

		// set some properties to non-default values
		// props.setTranslateSpecialEntities(true);
		// props.setTransResCharsToNCR(true);
		props.setOmitComments(true);
		props.setOmitDeprecatedTags(true);
		props.setOmitDoctypeDeclaration(true);
		props.setOmitHtmlEnvelope(true);
		props.setOmitUnknownTags(true);
		props.setOmitXmlDeclaration(true);
		props.setAdvancedXmlEscape(false);
		// props.setTransResCharsToNCR(true);

		HtmlCleaner cleaner = new HtmlCleaner(props);

		List<File> files = IOUtils.getFilesUnder(dataDir);

		for (int i = 0; i < files.size(); i++) {
			System.out.println(i);

			if (i == 2957 || i == 9954 || i == 11007 || i == 12131 || i == 13434 || i == 18170) {
				continue;
			}

			File file = files.get(i);
			String fileName = file.getName();
			fileName = IOUtils.getFileName(fileName);

			// String htmlText = IOUtils.readText(file, "euc-kr");
			String text = Jsoup.parse(file, "MS949").text();

			// TagNode tagNode = cleaner.clean(new StringReader(htmlText));
			// text = tagNode.getText(300).toString();

			StringBuffer sb = new StringBuffer();

			for (String line : text.split("\n")) {
				line = line.replaceAll("[\\s]+", " ");
				line = line.trim();

				if (line.equals("")) {
					continue;
				}

				if (line.equals(" ")) {
					continue;
				}

				sb.append(line + "\n");
			}
			text = sb.toString().trim();

			if (text.length() < 1000) {
				continue;
			}

			// fileName = IOUtils.getFileName(fileName);
			// int id = Integer.parseInt(fileName);
			// fileName = String.format("%s.txt", new
			// DecimalFormat("000000").format(id));

			writer.write(String.format("[File Name:\t%s]\n", fileName));
			writer.write(text + "\n\n");
		}
		writer.close();
	}

}

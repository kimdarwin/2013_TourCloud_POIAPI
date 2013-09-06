package edu.kaist.ir.poi.ext;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.kaist.ir.io.IOUtils;
import edu.kaist.ir.io.TextFileReader;
import edu.kaist.ir.poi.structure.Document;
import edu.kaist.ir.poi.structure.POI;
import edu.kaist.ir.poi.structure.Sentence;
import edu.kaist.ir.poi.structure.Token;
import edu.kaist.ir.tree.trie.Node;
import edu.kaist.ir.tree.trie.Trie;
import edu.kaist.ir.utils.IndexedList;
import edu.kaist.ir.utils.Indexer;
import edu.kaist.ir.utils.Pair;
import edu.kaist.ir.utils.StrUtils;

/**
 * @author Heung-Seon Oh
 * 
 *         This class is in charge of extracting POIs which are defined in a POI
 *         lexicon. The implementation is based on
 *         "An Efficient Trie-based Method for Approximate Entity Extraction with Edit-Distance Constraints"
 *         , ICDE'12
 * 
 */
public class POIExtractor {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		POIExtractor ext = new POIExtractor(2, 5, true, 2, true);
		ext.index(new File("data/poi_tagged.txt"));

		POIDisambiguator disam = new POIDisambiguator();
		disam.readWikiData(new File("wiki/wiki_location_term.txt"), ext.poiIndexer());

		List<File> files = IOUtils.getFilesUnder(new File("D:/Test_Collection/5_testcollection_filter_2_tagged"));

		String outfile_dir = "D:/Test_Collection/7_testcollection_filter_2_final/";
		for (int i = 0; i < files.size(); i++) {
			File file = files.get(i);
			if (!file.getName().endsWith(".txt") || file.getPath().contains("svn")) {
				continue;
			}
			
			String text = IOUtils.readText(file);
			
			Document doc = Document.createFromTaggedText2(text);

			// ext.extract1(text);
			// ext.extract(doc);
			// ext.extract3(doc);

			ext.extract(doc);
			List sentList = doc.sentences();
			for (int j = 0; j < sentList.size(); j++) {
				Sentence sent = (Sentence) sentList.get(j);
				if (sent.poiAnnotations().size() > 0) {
					IndexedList<Pair<Integer, Integer>, POI> indexList = sent.poiAnnotations();
					Iterator<Pair<Integer, Integer>> iter = indexList.keySet().iterator();

					while (iter.hasNext()) {
						Pair<Integer, Integer> p = iter.next();
						//System.out.println("he: " + sent.text().substring(p.getFirst(), p.getSecond()));
						//System.out.println("ha: " + indexList.get(p));
					}
				}
			}
			
			//System.out.println(file.getCanonicalPath());
			//System.out.println(file.getCanonicalPath().lastIndexOf('\\'));
			System.out.println(file.getCanonicalPath());
			// file name
			String outfile_name = file.getCanonicalPath().substring((file.getCanonicalPath().lastIndexOf('\\')+1));
			// file path
			String outfile_path;
			if((file.getCanonicalPath().lastIndexOf('_')+1)==file.getCanonicalPath().lastIndexOf('[')){
				outfile_path = outfile_dir + file.getCanonicalPath().substring((file.getCanonicalPath().lastIndexOf("r_")+2),(file.getCanonicalPath().lastIndexOf('['))) + "/";
			} else if(file.getCanonicalPath().contains("tour_stylist")){
				outfile_path = outfile_dir + "tour_stylist/";
			} else if(file.getCanonicalPath().contains("record")){
				outfile_path = outfile_dir + "cafe/";
			} else {
				outfile_path = outfile_dir + file.getCanonicalPath().substring((file.getCanonicalPath().lastIndexOf('_')+1),(file.getCanonicalPath().lastIndexOf('['))) + "/";	
			}
			
			//System.out.println(outfile_path);
			File temp = new File(outfile_path);
			if(!temp.isDirectory()){
				temp.mkdir();
			}
			//System.out.println(doc.toString());
			BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outfile_path+outfile_name), "UTF-8"));
			fw.write(doc.toString());
			fw.close();
			//System.out.println(disam.predict(doc));
			// save as file
			
		}

		System.out.println("process ends.");
	}

	private Trie<Character> segmentTrie;

	private int minEditDist;

	private int minPartitionLen;

	private Indexer<POI> poiIndexer;

	private boolean partitionToken;

	private boolean debug;

	private int minSegmentLen;

	/**
	 * 
	 * 
	 * @param minEditDist
	 *            accept a text segment as POI if the edit distance is less than
	 *            minEditDist
	 * @param minPartitionLen
	 *            partition POI into a set of segments if POI is long than
	 *            minPartitionLen
	 * @param partitionToken
	 *            partition each token of POI after splitting with white spaces
	 * @param minSegmentLen
	 *            reject a segment if its length is less than minSegmentLen
	 * 
	 * @param debug
	 */
	public POIExtractor(int minEditDist, int minPartitionLen, boolean partitionToken, int minSegmentLen, boolean debug) {
		this.minEditDist = minEditDist;
		this.minPartitionLen = minPartitionLen;
		this.partitionToken = partitionToken;
		this.minSegmentLen = minSegmentLen;
		this.debug = debug;
	}

	/**
	 * Create POI indexer.
	 * 
	 * @param inputFile
	 * @return POI indexer which maps each POI to an integer
	 */
	private Indexer<POI> createPoiIndexer(File inputFile) {
		Indexer<POI> ret = new Indexer<POI>();
		TextFileReader reader = new TextFileReader(inputFile);
		while (reader.hasNext()) {
			String line = reader.next();

			if (reader.getNumLines() == 1) {
				continue;
			}

			String[] parts = line.split("\t");

			if (parts[3].contains("+")) {
				continue;
			}

			String[] parts2 = parts[4].split(" ");

			Token[] tokens = new Token[parts2.length];

			for (int i = 0; i < parts2.length; i++) {
				String[] parts3 = parts2[i].split("=>");
				String tokenText = parts3[0];
				if (tokenText.contains("(") && tokenText.contains(")")) {
					tokenText = tokenText.replaceAll("\\([^\\(\\)]+\\)", "").trim();
				}

				String[] parts4 = parts3[1].split("\\+");

				List<String> morphemes = new ArrayList<String>();
				List<String> tags = new ArrayList<String>();

				// try {
				for (int j = 0; j < parts4.length; j++) {
					String[] two = parts4[j].split("/");
					morphemes.add(two[0]);
					tags.add(two[1]);
				}
				// } catch (Exception e) {
				//
				// }

				Token token = new Token(tokenText, StrUtils.toArray(morphemes), StrUtils.toArray(tags));
				tokens[i] = token;
			}

			POI poi = new POI(new String[] { parts[0], parts[1], parts[2] }, new Sentence(tokens));
			ret.add(poi);
		}
		reader.close();
		return ret;
	}

	/**
	 * do SEARCH and EXTENSION.
	 * 
	 * This method is to extract POIs and store them in each sentence.
	 * 
	 * They can be accessed by calling poiAnnotations method in Sentence class.
	 * 
	 * @param text
	 */
	public void extract(Document doc) {
		Node<Character> root = segmentTrie.root();

		for (int i = 0; i < doc.sentSize(); i++) {
			Sentence sent = doc.sentence(i);
			String line = sent.text();

			// String line2 = sent.taggedText();
			// String line3 = sent.morphemeText();

			// StringBuffer sb = new StringBuffer();
			// sb.append(String.format("%dth sentence\n", i + 1));
			// sb.append(String.format("%s\n", line));

			IndexedList<Pair<Integer, Integer>, POI> poiAnnotations = new IndexedList<Pair<Integer, Integer>, POI>();

			for (int j = 0; j < line.length(); j++) {
				Node<Character> node = root;
				int k = j + 1;

				/*
				 * Find a node list which matches sub-string.
				 * 
				 * Accept only when a last node is a leaf.
				 */

				for (int m = j; m < line.length(); m++) {
					Character key = line.charAt(m);
					// System.out.printf("[%s, %d]\n", key.toString(), m);
					if (node.hasChild(key)) {
						node = node.child(key);
						k = m + 1;
					} else {
						break;
					}
				}

				if (!node.isLeaf()) {
					continue;
				}

				if (k - j < minSegmentLen) {
					continue;
				}

				String matchedSegment = line.substring(j, k);
				int matchedSegLen = matchedSegment.length();

				// if (matchedSegment.equals("용천사")) {
				// System.out.println();
				// }

				// sb.append(String.format("matched segment: %s [%d-%d]\n",
				// matchedSegment, j, k));

				boolean found = false;

				Set<Integer> poiIds = (Set<Integer>) node.data();

				for (int poiId : poiIds) {
					POI poi = poiIndexer.getObject(poiId);
					String poiStr = poi.text();

					int start = poiStr.indexOf(matchedSegment);
					int end = start + matchedSegLen;

					int rightIndexInPoi = poiStr.length();
					int leftIndexInPoi = 0;

					// if (entity.contains("설악산")) {
					// System.out.println();
					// }

					int rightDiff = rightIndexInPoi - end;
					int leftDiff = start - leftIndexInPoi;

					String leftInPoi = poiStr.substring(leftIndexInPoi, start);
					String rightInPoi = poiStr.substring(end, rightIndexInPoi);

					int rightIndexInSent = Math.min(k + rightDiff, line.length());
					int leftIndexInSent = Math.max(j - leftDiff, 0);

					String leftInSent = line.substring(leftIndexInSent, j);
					String rightInSent = line.substring(k, rightIndexInSent);

					double leftEditDist = StrUtils.editDistance(leftInSent, leftInPoi, false);

					if (leftEditDist > minEditDist) {
						continue;
					}

					double rightEditDist = StrUtils.editDistance(rightInSent, rightInPoi, false);

					if (rightEditDist > minEditDist) {
						continue;
					}

					double editDist = leftEditDist + rightEditDist;

					if (editDist > minEditDist) {
						continue;
					}

					// String poiStr = line.substring(leftIndexInSent,
					// rightIndexInSent);
					// String poiPath = poi.keyPath("-");

					Pair<Integer, Integer> indexPair = new Pair<Integer, Integer>(leftIndexInSent, rightIndexInSent);
					poiAnnotations.put(new Pair<Integer, Integer>(leftIndexInSent, rightIndexInSent), poi);

					// sb.append(String.format("matched context: %s|%s|%s\n",
					// leftInSent, matchedSegment, rightInSent));
					// sb.append(String.format("poi: %s\n", poi.toString()));
					// sb.append(String.format("path: %s\n", poiPath));

					found = true;
				}

				// if (found && debug) {
				// System.out.println(sb.toString());
				// System.out.println();
				// }
			}

			sent.setPoiAnnotations(poiAnnotations);

			// if (poiAnnotations.size() > 0) {
			// System.out.println(sent.toString());
			// System.out.println();
			// }

			// System.out.println(sent.toString());
			// System.out.println();

			// removeDuplications(sent);

			// refineMapping(line, string_candidate);
		}
	}

	/**
	 * Construct a trie-based index where each node represents a Korean
	 * character from POI entries.
	 * 
	 * The format of input file should follow below format.
	 * 
	 * 도\t광역시\t구\t동\tPOI\tPOI tagged
	 * 
	 * POI tagged is a result of applying morpological analysis.
	 * 
	 * In POI tagged, each token consists of a sequence of pairs of morpheme and
	 * tags.
	 * 
	 * Token=>morpheme1/tag1+morphem2+tag2 ...
	 * 
	 * @param inputFile
	 */
	public void index(File inputFile) {
		System.out.printf("index [%s].\n", inputFile.getPath());

		poiIndexer = createPoiIndexer(inputFile);

		segmentTrie = new Trie<Character>();

		Partitioner partioner = new Partitioner(minEditDist, partitionToken, minPartitionLen);

		for (POI poi : poiIndexer.getObjects()) {
			String poiStr = poi.text();
			int poiId = poiIndexer.indexOf(poi);
			Segment[] segments = partioner.partition(poiStr);

			for (int i = 0; i < segments.length; i++) {
				Segment segment = segments[i];
				Node<Character> node = segmentTrie.insert(segment.toCharacters());
				Set<Integer> posting = (Set<Integer>) node.data();

				if (posting == null) {
					posting = new HashSet<Integer>();
					node.setData(posting);
				}
				posting.add(poiId);
			}
		}
	}

	public Indexer<POI> poiIndexer() {
		return poiIndexer;
	}
}

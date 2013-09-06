package edu.kaist.ir.poi.ext;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kaist.ir.io.IOUtils;
import edu.kaist.ir.io.TextFileReader;
import edu.kaist.ir.poi.structure.Document;
import edu.kaist.ir.poi.structure.Sentence;
import edu.kaist.ir.poi.structure.Token;
import edu.kaist.ir.tree.trie.Node;
import edu.kaist.ir.tree.trie.Trie;
import edu.kaist.ir.utils.IndexedList;
import edu.kaist.ir.utils.Indexer;
import edu.kaist.ir.utils.Pair;
import edu.kaist.ir.utils.StrUtils;

/**
 * @modified by Kangwook Lee (origin: POIExtractor - Heung-Seon Oh)
 * 
 * 
 */
public class POICounter {

//	 public static void convert() throws Exception {
//		 for (File file : IOUtils.getFilesUnder(new File("data/sample"))) {
//			 List<String> lines = IOUtils.readLines(file, "EUC_KR",
//					 Integer.MAX_VALUE);
//			 String text = StrUtils.join("\n", lines);
//			 IOUtils.write(file, "UTF-8", false, text);
//		 }
//	 }

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		//convert();

		POICounter ext = new POICounter(2, 5, true);
		ext.index(new File("data/20120621 KTO_POI_with_Field.txt"));

		// args[0] = the absolute path for each file
		// File file = new File(args[0]);
		
		int result = 0;
		int[] numPOI;
		numPOI = new int[60];
		for(int n=0; n < 60; n++){
			numPOI[n] = 0;
		}
		for (File file : IOUtils.getFilesUnder(new File("D:/3_testcollection_filter_1"))) {
			List<String> lines = IOUtils.readLines(file);
			String text = StrUtils.join("\n", lines, 8);

			int counter = 0;

			List<Sentence> sents = new ArrayList<Sentence>();
			List<Token> tokens = new ArrayList<Token>();
			List<String> morphemes = new ArrayList<String>();
			List<String> tags = new ArrayList<String>();

			morphemes.add("fuck");
			tags.add("fuck");

			Token token = new Token(text, morphemes.toArray(new String[morphemes.size()]), tags.toArray(new String[tags.size()]));

			tokens.add(token);
			sents.add(new Sentence(tokens.toArray(new Token[tokens.size()])));

			StringBuffer sb = new StringBuffer();
			for (Sentence sent : sents) {
				sb.append(sent.text() + "\n");
			}

			Document doc = new Document(sb.toString());
			doc.setSentences(sents);
			
			counter += ext.extract(doc);
			
			if(counter==0) {
				if(file.delete()) {
					System.out.println(file.getName() + " has no POI. deleted.");	
				} else {
					System.err.println(file.getName() + " has no POI. not deleted.");
				}
//				System.out.println("delete");
				// delete function
				result++;
			} else {
				System.out.println(file.getName() + " has " + counter + " POIs.");
				numPOI[counter]++;
			}
		}
		System.out.println("process ends. " + result + " files has been deleted.");
		for(int n = 0; n < 60; n++)
			System.out.println("number of files which have " + n + " POIs is " + numPOI[n]);
	}

	private Trie<Character> trie;

	private int minEditDist;

	private int minPartitionLen;

	private int minEntityLen;

	private int maxEntityLen;

	private Indexer<Node<String>> poiIndexer;

	private boolean partitionToken;

	public POICounter(int editDist, int minPartitionLen, boolean partitionToken) {
		this.minEditDist = editDist;
		this.minPartitionLen = minPartitionLen;
		this.partitionToken = partitionToken;
	}

	public int extract(Document doc) {
		int minValidLen = minEntityLen - minEditDist;
		int maxValidLen = maxEntityLen + minEditDist;

		int counter = 0;

		if (minValidLen <= 0) {
			minValidLen = 2;
		}

		Node<Character> root = trie.root();

		for (int i = 0; i < doc.sentSize(); i++) {
			Sentence sent = doc.sentence(i);
			String line = sent.text();

			if (maxValidLen >= line.length()) {
				maxValidLen = maxEntityLen;
			}

			// System.out.printf("%dth sentence\n", i + 1);
			// System.out.printf("%s\n", line);

			IndexedList<Pair<Integer, Integer>, String> string_candidate = new IndexedList<Pair<Integer, Integer>, String>();

			for (int j = 0; j < line.length(); j++) {
				Node<Character> node = root;
				int k = j + 1;

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

				if (k - j < 2) {
					continue;
				}

				String matchedSegment = line.substring(j, k);
				int matchedSegLen = matchedSegment.length();
				int offset = matchedSegLen + minEditDist;

				Set<Integer> poiNodes = (Set<Integer>) node.data();

				StringBuffer sb = new StringBuffer();
				sb.append(String.format("matched segment: %s [%d-%d]\n", matchedSegment, j, k));

				boolean found = false;

				for (int poiId : poiNodes) {
					Node<String> poiNode = poiIndexer.getObject(poiId);
					String poi = poiNode.key();

					int start = poi.indexOf(matchedSegment);
					int end = start + matchedSegLen;

					int rightIndexInEntity = Math.min(end + offset, poi.length());
					int leftIndexInEntity = Math.max(start - offset, 0);

					// if (entity.contains("�ㅼ븙��)) {
					// System.out.println();
					// }

					int rightDiff = rightIndexInEntity - end;
					int leftDiff = start - leftIndexInEntity;

					String leftInEntity = poi.substring(leftIndexInEntity, start);
					String rightInEntity = poi.substring(end, rightIndexInEntity);
					String reversedLeftInEntity = StrUtils.reverse(leftInEntity);

					int rightIndexInSent = Math.min(k + rightDiff, line.length());
					int leftIndexInSent = Math.max(j - leftDiff, 0);

					String leftInSent = line.substring(leftIndexInSent, j);
					String rightInSent = line.substring(k, rightIndexInSent);
					String reversedLeftInSent = StrUtils.reverse(leftInSent);

					// if(matchedSegment.equals("媛묒궗") &&
					// leftInEntity.length() == 0 && rightInEntity.length()
					// == 0){
					// System.out.println();
					// }

					double ed1 = StrUtils.editDistance(reversedLeftInSent, reversedLeftInEntity, false);

					if (ed1 > minEditDist) {
						continue;
					}

					double ed2 = StrUtils.editDistance(rightInSent, rightInEntity, false);

					if (ed2 > minEditDist) {
						continue;
					}

					double ed = ed1 + ed2;

					if (ed > minEditDist) {
						continue;
					}

					String poiStr = line.substring(leftIndexInSent, rightIndexInSent);
					String entityPath = poiNode.keyPath("-");
					string_candidate.put(new Pair<Integer, Integer>(leftIndexInSent, rightIndexInSent), entityPath);

					sb.append(String.format("poi: %s|%s|%s\n", leftInSent, matchedSegment, rightInSent));
					// sb.append(String.format("poi: %s, %d-%d\n", poiStr,
					// leftIndexInSent, rightIndexInSent));
					sb.append(String.format("path: %s\n", entityPath));

					found = true;
					counter++;
				}

				if (found) {
					// System.out.println(sb.toString());
					// System.out.println();
				}
				return counter;
			}

	//		sent.setPoiAnnotations(string_candidate);

			// System.out.println(sent.toString());
			// System.out.println();

			// removeDuplications(sent);

			// refineMapping(line, string_candidate);
		}
		return counter;
	}

	public void index(File inputFile) {
		System.out.printf("index [%s].\n", inputFile.getPath());

		Trie<String> _trie = new Trie<String>();
		poiIndexer = new Indexer<Node<String>>();

		TextFileReader reader = new TextFileReader(inputFile);
		while (reader.hasNext()) {
			String line = reader.next();

			if (reader.getNumLines() == 1) {
				continue;
			}

			String[] parts = line.split("\t");
			String entity = parts[2];
			String loc1 = parts[6];
			String loc2 = parts[7];
			String loc3 = parts[8];
			entity = entity.replaceAll("\\([^\\(\\)]+\\)", "");

			// if (entity.contains("�좎슫��)) {
			// System.out.println(entity);
			// }

			_trie.insert(new String[] { loc1, loc2, loc3, entity });
		}

		// _trie.insert(new String[] { "N", "N", "N", "�ㅼ븙�� });
		reader.close();

		for (Node<String> leafNode : _trie.leafNodes()) {
			// System.out.println(leafNode.toString());
			poiIndexer.add(leafNode);
		}

		trie = new Trie<Character>();

		Partitioner partioner = new Partitioner(minEditDist, partitionToken, minPartitionLen);

		minEntityLen = Integer.MAX_VALUE;
		maxEntityLen = -Integer.MAX_VALUE;

		for (Node<String> entityNode : poiIndexer.getObjects()) {
			String entity = entityNode.key();
			int entityId = poiIndexer.indexOf(entityNode);

			if (entity.length() > maxEntityLen) {
				maxEntityLen = entity.length();
			}

			if (entity.length() < minEntityLen) {
				minEntityLen = entity.length();
			}

			Segment[] segments = partioner.partition(entity);

			for (int i = 0; i < segments.length; i++) {
				Segment segment = segments[i];
				Node<Character> node = trie.insert(segment.toCharacters());
				Set<Integer> entitySet = (Set<Integer>) node.data();

				if (entitySet == null) {
					entitySet = new HashSet<Integer>();
					node.setData(entitySet);
				}
				entitySet.add(entityId);
			}
		}
		reader.close();

		// System.out.println(trie.toString());
	}

}

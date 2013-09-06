package edu.kaist.ir.poi.ext;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.kaist.ir.io.TextFileReader;
import edu.kaist.ir.math.VectorMaths;
import edu.kaist.ir.matrix.DenseMatrix;
import edu.kaist.ir.matrix.DenseVector;
import edu.kaist.ir.matrix.Vector;
import edu.kaist.ir.poi.structure.Document;
import edu.kaist.ir.poi.structure.POI;
import edu.kaist.ir.poi.structure.Sentence;
import edu.kaist.ir.poi.structure.Token;
import edu.kaist.ir.tree.trie.Node;
import edu.kaist.ir.tree.trie.Trie;
import edu.kaist.ir.utils.Counter;
import edu.kaist.ir.utils.CounterMap;
import edu.kaist.ir.utils.CounterUtils;
import edu.kaist.ir.utils.IndexedList;
import edu.kaist.ir.utils.Indexer;
import edu.kaist.ir.utils.Pair;
import edu.kaist.ir.utils.StrUtils;
import edu.kaist.ir.utils.VectorUtils;

/**
 * @author Heung-Seon Oh
 * 
 *         This class determines POI of an extracted mention when there are
 *         several candidate POIs for a mention.
 * 
 *         The implementation is based on "Collective Entity Linking in Web
 *         Text: A Graph-Based Method" at SIGIR'11.
 * 
 * 
 * 
 */
public class POIDisambiguator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

	private CounterMap<String, String> wikiData;

	private CounterMap<String, String> label_term_weight;

	private IndexedList<String, String> mention_path;

	public POIDisambiguator() {

	}

	/**
	 * Compute the weight between a mention in a document and a POI.
	 * 
	 * A weight is defined as exp(cosine(mention, POI))
	 * 
	 * For example,
	 * 
	 * Mention = 불갑사, POI=전라남도-영광군-불갑면-불갑사
	 * 
	 * @param labelIndexer
	 * @param label_term_weight
	 * @return
	 */
	private CounterMap<Integer, Integer> compatibleEdges(Indexer<String> labelIndexer, DenseMatrix label_term_weight) {
		CounterMap<Integer, Integer> ret = new CounterMap<Integer, Integer>();

		for (String mention : mention_path.keySet()) {
			int labelId1 = labelIndexer.indexOf(mention);

			DenseVector x1 = label_term_weight.row(labelId1);

			for (String path : mention_path.get(mention)) {
				String[] parts = path.split("-");
				for (int i = parts.length - 1; i < parts.length; i++) {
					String subPath = StrUtils.join("-", parts, 0, i + 1);
					int labelId2 = labelIndexer.indexOf("l=" + subPath);
					DenseVector x2 = label_term_weight.row(labelId2);
					double cosine = VectorMaths.cosine(x1, x2, false);
					ret.setCount(labelId1, labelId2, Math.exp(cosine));
				}
			}
		}
		return ret;
	}

	/**
	 * Compute scores for POIs of each mention.
	 * 
	 * The core of this method is random-walk on referent graph.
	 * 
	 * @return
	 */
	private CounterMap<String, String> computeScores() {

		/*
		 * Construct indexers for term and labels.
		 * 
		 * Here a label is a mention or a sub-POI path.
		 */
		Indexer<String> labelIndexer = new Indexer<String>();
		Indexer<String> termIndexer = new Indexer<String>();

		for (String label : label_term_weight.keySet()) {
			labelIndexer.add(label);
			Counter<String> term_weight = label_term_weight.getCounter(label);
			for (String term : term_weight.keySet()) {
				termIndexer.add(term);
			}
		}

		/*
		 * 
		 * Construct a weight matrix for labels
		 */

		DenseMatrix weightMat = VectorUtils.toDenseMatrix(label_term_weight, labelIndexer, termIndexer);

		/*
		 * Compute initial scores only for mentions in labels
		 */

		DenseVector label_initScore = VectorUtils.toDenseVector(initialMentionScores(), labelIndexer);
		DenseVector label_oldScore = label_initScore.copy();
		DenseVector label_score = label_initScore.copy();

		/*
		 * Construct a referent graph by utilizing compatible and semantic
		 * related edges.
		 * 
		 * Note that referent matrix should be filled by (label2, label1) to
		 * avoid transpose operation.
		 */

		CounterMap<Integer, Integer> CEWeights = compatibleEdges(labelIndexer, weightMat);
		CounterMap<Integer, Integer> SRWeights = semanticRelatedEdges(labelIndexer, weightMat);
		DenseMatrix referentGraph = new DenseMatrix(labelIndexer.size(), labelIndexer.size());

		for (int labelId1 : CEWeights.keySet()) {
			Counter<Integer> counter = CEWeights.getCounter(labelId1);
			counter.normalize();
			for (int labelId2 : counter.keySet()) {
				double weight = counter.getCount(labelId2);
				referentGraph.set(labelId2, labelId1, weight);
			}
		}

		for (int labelId1 : SRWeights.keySet()) {
			Counter<Integer> counter = SRWeights.getCounter(labelId1);
			counter.normalize();
			for (int labelId2 : counter.keySet()) {
				double weight = counter.getCount(labelId2);
				referentGraph.set(labelId2, labelId1, weight);
			}
		}

		// Vector column = referentGraph.columnSums();
		// System.out.println(column.toString());

		double lambda = 0.1;
		double threshold = 0.00001;
		int maxIter = 20;

		/*
		 * do random-walk
		 */

		for (int i = 0; i < maxIter; i++) {
			label_score.setAll(0);
			VectorMaths.multiply(referentGraph, label_oldScore, label_score);
			VectorMaths.addAfterScale(label_score, label_initScore, 1 - lambda, lambda);
			label_score.normalize();

			double diff = VectorMaths.euclideanDistance(label_score, label_oldScore, false);

			if (diff < threshold) {
				break;
			}

			// System.out.println(VectorUtils.toCounter(label_score,
			// labelIndexer));

			VectorUtils.copyValues(label_score, label_oldScore);
		}

		CounterMap<String, String> ret = new CounterMap<String, String>();
		for (int mentionId : CEWeights.keySet()) {
			String mention = labelIndexer.getObject(mentionId);
			Counter<Integer> counter = CEWeights.getCounter(mentionId);
			for (int pathId : counter.keySet()) {
				String path = labelIndexer.getObject(pathId).substring(2);
				double ceWeight = counter.getCount(pathId);
				double prob = label_score.value(pathId);
				double score = ceWeight * prob;
				ret.setCount(mention, path, score);
			}
		}
		ret.normalize();

		// System.out.println(ret);

		return ret;

	}

	public CounterMap<String, String> predict(Document doc) {
		prepare(doc);
		return computeScores();
	}

	public void disambiguate(Document doc) {
		disambiguate(doc, predict(doc));
	}

	public void disambiguate(Document doc, CounterMap<String, String> mention_poi_score) {
		for (int i = 0; i < doc.sentSize(); i++) {
			Sentence sent = doc.sentence(i);
			IndexedList<Pair<Integer, Integer>, POI> poiAnno = sent.poiAnnotations();
			for (Pair<Integer, Integer> indexPair : poiAnno.keySet()) {
				String metion = sent.text().substring(indexPair.getFirst(), indexPair.getSecond());
				List<POI> pois = poiAnno.get(indexPair);
				String prediction = mention_poi_score.getCounter(metion).argMax();

				Iterator<POI> iter = pois.iterator();
				while (iter.hasNext()) {
					POI poi = iter.next();
					if (!poi.toString().equals(prediction)) {
						iter.remove();
					}
				}
			}
		}
	}

	/**
	 * Compute initial scores for mentions.
	 * 
	 * POIs have zero scores.
	 * 
	 * @return
	 */
	private Counter<String> initialMentionScores() {
		Counter<String> ret = new Counter<String>();
		for (String label : label_term_weight.keySet()) {
			double weightSum = label_term_weight.getCounter(label).totalCount();
			if (label.startsWith("l=")) {
				ret.setCount(label, 0);
			} else {
				ret.setCount(label, weightSum);
			}
		}
		ret.normalize();
		return ret;
	}

	/**
	 * prepare environments to further processing.
	 * 
	 * Extracted mentions and corresponding locations are encoded as TFIDF term
	 * vectors.
	 * 
	 * The context information of locations are extracted from Wikipedia
	 * articles.
	 * 
	 * 
	 * @param doc
	 */
	private void prepare(Document doc) {
		label_term_weight = new CounterMap<String, String>();
		mention_path = new IndexedList<String, String>();

		for (int i = 0; i < doc.sentSize(); i++) {
			Sentence sent = doc.sentence(i);
			IndexedList<Pair<Integer, Integer>, POI> poiAnno = sent.poiAnnotations();
			Iterator<Pair<Integer, Integer>> iter = poiAnno.keySet().iterator();

			while (iter.hasNext()) {
				Pair<Integer, Integer> indexPair = iter.next();
				int start = indexPair.getFirst();
				int end = indexPair.getSecond();
				String mention = sent.toString().substring(start, end);

				// int[] tokenIndexes = sent.tokenIndexes(start, end);
				// int tokenStart = tokenIndexes[0];
				// int tokenEnd = tokenIndexes[1];
				Token[] tokens = sent.tokens();

				for (int j = 0; j < tokens.length; j++) {
					Token token = tokens[j];
					// System.out.println(tokens[j].toString());

					String[] morphemes = token.morphemes();
					String[] tags = token.tags();

					for (int k = 0; k < morphemes.length; k++) {
						String morpheme = morphemes[k];
						String tag = tags[k];

						if (morpheme.length() == 1 || StrUtils.find(morpheme, "\\d+") || !tag.startsWith("n")) {
							continue;
						}
						label_term_weight.incrementCount(mention, morpheme, 1);
					}
				}

				if (label_term_weight.containsKey(mention)) {
					List<POI> pois = poiAnno.get(indexPair);
					for (POI poi : pois) {
						String path = poi.toString();
						mention_path.put(mention, path);
					}
				} else {
					// 문서로 부터 context 정보를 생성하지 못했을 경우에 annotation에서 삭제.
					iter.remove();
				}
			}
		}

		label_term_weight.tfidf();

		Set<String> pathSet = new HashSet<String>();
		for (String mention : mention_path.keySet()) {
			for (String path : mention_path.get(mention)) {
				pathSet.add(path);
			}
		}

		for (String path : pathSet) {
			String[] parts = path.split("-");
			for (int i = 0; i < parts.length; i++) {
				// String currentLocation = parts[i];
				String subPath = StrUtils.join("-", parts, 0, i + 1);
				String label = String.format("l=%s", subPath);

				if (!wikiData.containsKey(subPath)) {
					label_term_weight.setCounter(label, new Counter<String>());
				} else {
					Counter<String> counter = wikiData.getCounter(subPath);
					label_term_weight.setCounter(label, counter);
				}
			}
		}

	}

	/**
	 * Read context information of locations which appear in Wikipedia articles.
	 * 
	 * The file format is below:
	 * 
	 * POI Path\tWiki Location\tTerm1:Count1 Term2:Count2
	 * 
	 * @param inputFile
	 */
	public void readWikiData(File inputFile, Indexer<POI> poiIndexer) {
		wikiData = new CounterMap<String, String>();
		TextFileReader reader = new TextFileReader(inputFile);
		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.split("\t");
			String path = parts[0];

			path = path.replaceAll("서울특별시", "서울");
			path = path.replaceAll("인천광역시", "인천");
			path = path.replaceAll("부산광역시", "부산");
			path = path.replaceAll("대구광역시", "대구");
			path = path.replaceAll("울산광역시", "울산");
			path = path.replaceAll("대전광역시", "대전");
			path = path.replaceAll("광주광역시", "광주");
			path = path.replaceAll("제주특별자치도", "제주도");

			String wikiLoc = parts[1];
			String countStr = parts[2];
			Counter<String> term_count = CounterUtils.counter(countStr);
			wikiData.setCounter(path, term_count);
		}
		reader.close();
	}

	/**
	 * Compute weights for all combinations between sub POI paths.
	 * 
	 * A weight is defined as exp(cosine(sub-POI1, sub-POI2))*distance(sub-POI1,
	 * sub-POI2)
	 * 
	 * POI=전라남도-영광군-불갑면-불갑사
	 * 
	 * weight(전라남도-영광군-불갑면-불갑사,전라남도-영광군-불갑면), weight(전라남도-영광군-불갑면-불갑사,전라남도-영광군)
	 * weight(전라남도-영광군-불갑면-불갑사,전라남도),
	 * 
	 * @param labelIndexer
	 * @param weightMat
	 * @return
	 */
	private CounterMap<Integer, Integer> semanticRelatedEdges(Indexer<String> labelIndexer, DenseMatrix weightMat) {
		Trie<String> trie = new Trie<String>();
		for (String mention : mention_path.keySet()) {
			for (String path : mention_path.get(mention)) {
				trie.insert(path.split("-"));
			}
		}

		List<Node<String>> nodes = trie.allNodes();

		CounterMap<Integer, Integer> ret = new CounterMap<Integer, Integer>();

		for (int i = 0; i < nodes.size(); i++) {
			Node<String> node1 = nodes.get(i);
			String path1 = "l=" + node1.keyPath("-");
			int labelId1 = labelIndexer.indexOf(path1);
			DenseVector vector1 = weightMat.row(labelId1);

			for (int j = 0; j < nodes.size(); j++) {
				if (i == j) {
					continue;
				}

				Node<String> node2 = nodes.get(j);
				String path2 = "l=" + node2.keyPath("-");
				int labelId2 = labelIndexer.indexOf(path2);
				DenseVector vector2 = weightMat.row(labelId2);
				double cosine = VectorMaths.cosine(vector1, vector2, false);

				// if (cosine > 0) {
				// System.out.println(path1);
				// System.out.println(path2);
				// System.out.println();
				// }
				int dist = node1.distance(node2);
				ret.setCount(labelId1, labelId2, Math.exp(cosine) / dist);
			}
		}

		return ret;

	}

}

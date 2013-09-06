package edu.kaist.ir.poi.dis.graph;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import edu.kaist.ir.io.IOUtils;
import edu.kaist.ir.math.VectorMaths;
import edu.kaist.ir.matrix.DenseMatrix;
import edu.kaist.ir.matrix.DenseVector;
import edu.kaist.ir.poi.ext.POIExtractor;
import edu.kaist.ir.poi.structure.Document;
import edu.kaist.ir.poi.structure.POI;
import edu.kaist.ir.poi.structure.Sentence;
import edu.kaist.ir.utils.Counter;
import edu.kaist.ir.utils.CounterMap;
import edu.kaist.ir.utils.IndexedList;
import edu.kaist.ir.utils.Pair;

/**
 * 
 * @author Eunyoung Kim
 * 
 *         This class is in charge of POI disambiguation based on graph
 *         approach, especially random walk 기본적인 플랫폼은 'Collective Entity
 *         Linking:a graph-based method' by Xianpei Han et al. [SIGIR 2011] 을
 *         기반으로 합니다. POI가 이 논문의 entity에 대응됩니다.
 * 
 *         initial score 및 edge weight를 정하는 데 'An efficient location extraction
 *         algorithm by leveraging web contextual information' by T Qin. [GIS
 *         2010] 를 활용했습니다. mention node는 초기값으로 term frequency 값을 갖고 POI to POI
 *         edge weight는 POI Hierarchy에서 두 POI 노드간의 거리에 기반한 식 (alpha ^ distance)
 *         mention to POI는 not determined. 이 논문에서처럼 web을 이용한 prior 값을 활용할 수도 있을
 *         것임
 * 
 */

public class GraphDisambiguation {
	private DenseVector scoreVector;
	private DenseMatrix transitionMatrix;
	private HashMap<String, List<POI>> mentionEntityMap;
	private CounterMap<String, POI> scoreMap;
	private ArrayList<String> mentionList;
	private ArrayList<Integer> mentionIndexList;
	private HashMap<Integer, ArrayList<Integer>> indexMap;

	// public GraphDisambiguation(Vector v, Matrix m)
	// {
	// scoreVector = v;
	// transitionMatrix = m;
	// }

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		POIExtractor ext = new POIExtractor(2, 5, true, 2, true);
		ext.index(new File("data/poi_tagged.txt"));

		//List<File> files = IOUtils.getFilesUnder(new File("data/sample_pos"));
		List<File> files = IOUtils.getFilesUnder(new File("data/output_raw"));

		for (int i = 0; i < files.size(); i++) {
			File file = files.get(i);
			if (!file.getName().endsWith(".txt") || file.getPath().contains("svn")) {
				continue;
			}

			GraphDisambiguation g = new GraphDisambiguation();

			String text = IOUtils.readText(file);
			//Document doc = Document.createFromTaggedText(text);
			Document doc = Document.createFromRawText(text);
			

			ext.extract(doc);
			g.setUp(doc);
			//System.out.println(g.scoreVector);
			//System.out.println(g.transitionMatrix);


			g.randomWalk(1);
			g.getScoreVector().toString(Integer.MAX_VALUE, true, true, null);

			//CounterMap<String, POI> ctmap = g.getScoreMap();
			///System.out.println(ctmap);
			String outdir = "output_graph_acc";
			String outfile = file.toString().replace("output_raw",outdir);
			output(outfile, g.getHighestScoreMap());
			
			//break;
		}

		System.out.println("process ends.");

	}
	public static void output(String outfile, HashMap<String,POI> hsmap) throws IOException{
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outfile),"utf-8"));
		Iterator<String> iter = hsmap.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			POI poi = hsmap.get(key);
			//out.write("<P>"+poi+"</P>\n");
			out.write(poi+"\n");
			//System.out.println(key + " -> " + poi);
		}
		
		out.close();
	}

	/**
	 * This method should be called after POI extraction method
	 * 
	 * This method sets initial score vector and transition matrix for random
	 * walk There are two types of node: mention(text) node, entity(POI) node
	 * Currently, only mention nodes have initial score value based on their
	 * term frequency in the document The weight of edge from mention node to
	 * entity node is 1, from one entity node to another entity node depends on
	 * distance between two nodes in POI Hierarchy (alpha^distance, alpha = 0.5)
	 * Finally, initial score vector is normalized and transition matrix is
	 * row-normalized.
	 * 
	 * @param doc
	 */
	public void setUp(Document doc) {
		mentionEntityMap = new HashMap<String, List<POI>>();
		scoreMap = new CounterMap<String, POI>();
		mentionList = new ArrayList<String>();
		ArrayList<Integer> mentionFreqList = new ArrayList<Integer>();

		List sentList = doc.sentences();
		for (int j = 0; j < sentList.size(); j++) {
			Sentence sent = (Sentence) sentList.get(j);
			if (sent.poiAnnotations().size() > 0) {
				IndexedList<Pair<Integer, Integer>, POI> poiList = sent.poiAnnotations();
				Iterator<Pair<Integer, Integer>> iter = poiList.keySet().iterator();
				while (iter.hasNext()) {
					Pair<Integer, Integer> p = iter.next();
					String mention = sent.text().substring(p.getFirst(), p.getSecond());
					List<POI> poi = poiList.get(p);
					//System.out.println(p + ": " + sent.text().substring(p.getFirst(), p.getSecond()));
					//System.out.println(poiList.get(p));

					if (!mentionEntityMap.containsKey(mention)) {
						mentionEntityMap.put(mention, poi);
						scoreMap.setCounter(mention, new Counter<POI>(poi));
						mentionList.add(mention);
						mentionFreqList.add(1);
					} else {
						int idx = mentionList.indexOf(mention);
						mentionFreqList.set(idx, mentionFreqList.get(idx) + 1);
					}
				}
			}
		}
		// System.out.println(doc.toString());

		// Set indexMap
		mentionIndexList = new ArrayList<Integer>();
		indexMap = new HashMap<Integer, ArrayList<Integer>>();
		int index = 0;

		for (int i = 0; i < mentionList.size(); i++) {
			String mention = mentionList.get(i);
			int mentionIndex = index++;
			mentionIndexList.add(mentionIndex);
			ArrayList<Integer> indexList = new ArrayList<Integer>();

			for (int j = 0; j < mentionEntityMap.get(mention).size(); j++)
				indexList.add(index++);

			indexMap.put(mentionIndex, indexList);
		}

		double[] vec = new double[index];
		double[][] mat = new double[index][index];

		//System.out.println("index: " + index);

		// Set initial score vector
		// Set edge weight from text to entity node
		Iterator<Integer> iter2 = indexMap.keySet().iterator();
		while (iter2.hasNext()) {
			Integer key = iter2.next();

			int idx = mentionIndexList.indexOf(key);
			vec[key] = mentionFreqList.get(idx); // tf

			ArrayList<Integer> indexList = indexMap.get(key);
			for (int i = 0; i < indexList.size(); i++)
				mat[key][indexList.get(i)] = 1; // default
		}

		// Set edge weight btw entity node (based on node distance)
		for (int i = 0; i < mentionList.size() - 1; i++) // key index
		{
			List<POI> entityListA = mentionEntityMap.get(mentionList.get(i));

			for (int j = i + 1; j < mentionList.size(); j++) {
				// if (i == j)
				// continue;

				List<POI> entityListB = mentionEntityMap.get(mentionList.get(j));

				for (int k = 0; k < entityListA.size(); k++) {
					for (int l = 0; l < entityListB.size(); l++) {
						int distance = entityListA.get(k).distance(entityListB.get(l));
						// double weight = Math.pow(0.1, distance);
						double weight = Math.pow(0.5, distance);
						mat[indexMap.get(mentionIndexList.get(i)).get(k)][indexMap.get(mentionIndexList.get(j)).get(l)] = weight;
						mat[indexMap.get(mentionIndexList.get(j)).get(l)][indexMap.get(mentionIndexList.get(i)).get(k)] = weight;
					}
				}
			}
		}

		scoreVector = new DenseVector(vec);
		transitionMatrix = new DenseMatrix(mat);

		scoreVector.normalizeAfterSummation();
		transitionMatrix.normalizeRows();
//		System.out.println(scoreVector);
//		System.out.println(transitionMatrix);
		//System.out.println(mentionList);
		//System.out.println("Complete set up process");
	}

	/**
	 * This method perform random walk until the score vector(s) is converged
	 * Convergence condition is (s(t+1) - s(t) <= threshold)
	 * 
	 * @param threshold
	 * 
	 */
	public void randomWalk(double threshold) {
		// Matrix beforeScoreVector1 = new Matrix(new Vector(1));
		DenseVector beforeScoreVector = scoreVector.copy();
		DenseVector afterScoreVector;
		transitionMatrix.transpose();
		VectorMaths vm = new VectorMaths();

		// int cnt = 0;

		while (true) {
			afterScoreVector = (DenseVector) VectorMaths.multiplyTo(transitionMatrix, beforeScoreVector);
			DenseVector diffVector = afterScoreVector.copy();
			VectorMaths.subtract(diffVector, beforeScoreVector);
			double diffValue = VectorMaths.normL1(diffVector, false); // two
																		// norm?

			if (diffValue <= threshold)
				break;

			beforeScoreVector = afterScoreVector;

			// if (cnt++ % 10 == 0)
			// System.out.println("diffValue: " + diffValue + "\n" +
			// diffVector);
		}

		scoreVector = afterScoreVector;
	}

	/**
	 * This method perform random walk for n iterations
	 * 
	 * @param nIter
	 *            : # of iteration
	 * @return score vector
	 */

	public void randomWalk(int nIter) {
		DenseVector beforeScoreVector = scoreVector.copy();
		DenseVector afterScoreVector = scoreVector.copy();
		transitionMatrix.transpose();

		for (int i = 0; i < nIter; i++) {
			afterScoreVector = (DenseVector) VectorMaths.multiplyTo(transitionMatrix, beforeScoreVector);
			beforeScoreVector = afterScoreVector;
		}

		scoreVector = afterScoreVector;
	}

	public DenseVector getScoreVector() {
		return scoreVector;
	}

	/**
	 * [mention, (POI, random walk score)]
	 * 
	 * @return
	 */
	public CounterMap<String, POI> getScoreMap() {
		for (int i = 0; i < mentionList.size(); i++) {
			String mention = mentionList.get(i);
			int mentionIndex = mentionIndexList.get(i);
			List<POI> poiList = mentionEntityMap.get(mention);
			ArrayList<Integer> entityIndexList = indexMap.get(mentionIndex);

			for (int j = 0; j < poiList.size(); j++) {
				scoreMap.setCount(mention, poiList.get(j), scoreVector.value(entityIndexList.get(j)));
			}
		}

		return scoreMap;
	}

	/**
	 * get the disambiguated POI for each mention [mention, POI with the highest
	 * random walk score]
	 * 
	 * @return
	 */
	public HashMap<String, POI> getHighestScoreMap() {
		HashMap<String, POI> hsmap = new HashMap<String, POI>();

		Iterator<String> iter = scoreMap.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			POI maxPOI = scoreMap.getCounter(key).argMax();
			double score = scoreMap.getCounter(key).getCount(maxPOI);
			//System.out.println(maxPOI+": "+score);
			if(score>0.3)
			{hsmap.put(key, maxPOI);

			//System.out.println(key + " -> " + maxPOI);
			}
		}

		return hsmap;
	}

	// public HashMap poiExtract(File file) throws Exception
	// {
	// POIExtractor ext = new POIExtractor(2, 5, true);
	// ext.index(new File("data/20120621 KTO_POI_with_Field.txt"));
	//
	// String text = IOUtils.readText(file);
	//
	// List<Sentence> sents = new ArrayList<Sentence>();
	//
	// for (String line : text.split("\n\n")) {
	// List<Token> tokens = new ArrayList<Token>();
	//
	// for (String line2 : line.split("\n")) {
	// String[] parts = line2.split("\t");
	//
	// List<String> morphemes = new ArrayList<String>();
	// List<String> tags = new ArrayList<String>();
	//
	// String tokenText = parts[0];
	// String morphemeStr = parts[1];
	//
	// for (String tok : morphemeStr.split("\\+")) {
	// String[] two = StrUtils.split2Two("/", tok);
	// String morphem = two[0];
	// String tag = two[1];
	//
	// morphemes.add(morphem);
	// tags.add(tag);
	// }
	//
	// Token token = new Token(tokenText, morphemes.toArray(new
	// String[morphemes.size()]), tags.toArray(new String[tags.size()]));
	// tokens.add(token);
	//
	// }
	// sents.add(new Sentence(tokens.toArray(new Token[tokens.size()])));
	// }
	//
	// StringBuffer sb = new StringBuffer();
	// for (Sentence sent : sents) {
	// sb.append(sent.text() + "\n");
	// }
	//
	// Document doc = new Document(sb.toString());
	// doc.setSentences(sents);
	//
	// // ext.extract1(text);
	// // ext.extract2(doc);
	// return ext.extract3(doc);
	//
	// // System.out.println(doc.toString());
	// }
}

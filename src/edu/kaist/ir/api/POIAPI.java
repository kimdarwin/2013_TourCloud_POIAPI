package edu.kaist.ir.api;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.tree.DefaultMutableTreeNode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Application;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.Server;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;

import edu.kaist.ir.io.TextFileReader;
import edu.kaist.ir.poi.dis.graph.GraphDisambiguation;
import edu.kaist.ir.poi.dis.tree.Location;
import edu.kaist.ir.poi.ext.Partitioner;
import edu.kaist.ir.poi.ext.Segment;
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

public class POIAPI extends Application {
	/*Test examples: 

	1. POI extract
	http://chihaya.kaist.ac.kr:8111/poiextract?infile=http://chihaya.kaist.ac.kr/data/input/naver_anndam[100106176733].txt
	2. POI highlight
	http://chihaya.kaist.ac.kr:8111/highlight?infile=http://chihaya.kaist.ac.kr/data/input/naver_anndam[100106176733].txt
	*/
		
	
	static POIAPI ext;
	//Server Settings
	static final int serverport = 8111;
	static final String serverurlport = "http://chihaya.kaist.ac.kr:"+serverport;
	static final String poisearchurl = "http://search.naver.com/search.naver?&query=";
	//Parameters
	static final int MaxEditDistance = 1; 	//POI 후보 탐지 시 오타 허용 범위 (높일수록 정확율이 하락)
	private static float power_t = 0.17f; 	//주제 지역 탐지 시 Power값의 threshold (높일수록 재현율이 하락)
	private static float spread_t = 0.1f;	//주제 지역 탐지 시 Spread값의 threshold (높일수록 재현율이 하락)
	private static int loc_thres = 0; 		//지명이 x번 이상 나타난 문서에만 주제 지역 필터링 적용(-1로 할 시 재현율 대폭 하락)
	
	private DefaultMutableTreeNode root= new DefaultMutableTreeNode();
	private HashSet<String> locs = new HashSet<String>();
	

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		ext = new POIAPI(MaxEditDistance, 5, true, 2, true);
		ext.index(new File("data/poi_tagged.txt"));

		Server server = new Server(Protocol.HTTP,serverport);
		//server.setNext(new POIAPI2(1, 5, true, 2, true));
		server.setNext(ext);
		server.start();
		/*
		// Create a new Restlet component and add a HTTP server connector to it
        Component component = new Component();
        component.getServers().add(Protocol.HTTP, 8184);

        // Then attach it to the local host
        component.getDefaultHost().attach("/poiextract", poiextract.class);

        // Now, let's start the component!
        // Note that the HTTP server connector is also automatically started.
        component.start();*/
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
	public POIAPI(int minEditDist, int minPartitionLen, boolean partitionToken, int minSegmentLen, boolean debug) {
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
	/*//@Get
    public String toSsegsgsegtring2() {
		return "";
		
	}*/
    public Restlet poiextract2 = new Restlet() {
    	@Override
    	public void handle(Request req, Response resp) {
    		
    		Form r = req.getResourceRef().getQueryAsForm();
    		String infile = r.getFirstValue("infile"); 

    		GraphDisambiguation g = new GraphDisambiguation();
    		URL url = null;
    		try {
				url = new URL(infile);
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    		String text="";
    		BufferedReader in = null;
			try {
				in = new BufferedReader(new InputStreamReader(url.openStream(),"UTF-8"));
			} catch (UnsupportedEncodingException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
    		String line="";
    		try {
				while((line=in.readLine())!=null){
				text = text+"\n"+line;
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    		try {
				in.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    		Document doc = null;
    		try {
    			doc = Document.createFromRawText(text);
    		} catch (Exception e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}

    		ext.extract(doc);
    		g.setUp(doc);
			//System.out.println(g.scoreVector);
			//System.out.println(g.transitionMatrix);
			g.randomWalk(1);
			g.getScoreVector().toString(Integer.MAX_VALUE, true, true, null);
			//String outdir = "output_graph_acc";
			//String outfile = file.toString().replace("output_raw",outdir);
			//g.output(outfile, g.getHighestScoreMap());
			String cont="";
			HashMap<String, POI> highpois = g.getHighestScoreMap();
			HashSet<String> highpoiname = new HashSet<String>();
			Iterator<String> iter2 = highpois.keySet().iterator();
			while(iter2.hasNext()) {
				String poi = iter2.next();
				highpoiname.add(highpois.get(poi).toString());
			}
    		List sentList = doc.sentences();
    		JSONArray pois = new JSONArray();
    		for (int j = 0; j < sentList.size(); j++) {
    			Sentence sent = (Sentence) sentList.get(j);
    			if (sent.poiAnnotations().size() > 0) {
    				IndexedList<Pair<Integer, Integer>, POI> indexList = sent.poiAnnotations();
    				Iterator<Pair<Integer, Integer>> iter = indexList.keySet().iterator();

    				while (iter.hasNext()) {
    					Pair<Integer, Integer> p = iter.next();
    					String rawpoi = indexList.get(p).get(0).toString();
    					if(!highpoiname.contains(rawpoi)) continue;
    					JSONObject poi = new JSONObject ();
    					    					
    					try {
    						String poiid="0";
    						String poiname = indexList.get(p).get(0).toString().replace("[","").replace("]","");
    						String urlpoiname = URLEncoder.encode(poiname);
							int startidx = p.getFirst();
							int endidx = p.getSecond();
							String poiurl = poisearchurl+urlpoiname;

    						poi.put("sentidx",j+1);
    						poi.put("poiname",poiname);
    						poi.put("poiurl", poiurl);
							poi.put("startidx",startidx);
							poi.put("endidx",endidx);
							poi.put("poiid",poiid);
							
							pois.put(poi);
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
    					break;
    					//System.out.println("he: " + sent.text().substring(p.getFirst(), p.getSecond()));
    					//System.out.println("ha: " + indexList.get(p));
    				}
    			}
    		}
    		//System.out.println(pois.toString());
    		
    		String message = null;
			try {
				message = pois.toString(4);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		resp.setEntity(message,MediaType.TEXT_PLAIN);
    	}
    };
    public Restlet highlight2 = new Restlet() {
    	@Override
    	public void handle(Request req, Response resp) {
    		Form r = req.getResourceRef().getQueryAsForm();
    		String infile = r.getFirstValue("infile"); 
    		GraphDisambiguation g = new GraphDisambiguation();
    		URL url = null;
    		try {
				//url = new URL("http://chihaya.kaist.ac.kr/data/input/naver_2005math[23399683].txt");
    			url = new URL(infile);
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    		String text="";
    		BufferedReader in = null;
			try {
				in = new BufferedReader(new InputStreamReader(url.openStream(),"UTF-8"));
			} catch (UnsupportedEncodingException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
    		String line="";
    		try {
				while((line=in.readLine())!=null){
				text = text+"\n"+line;
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    		try {
				in.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    		Document doc = null;
    		try {
    			doc = Document.createFromRawText(text);
    		} catch (Exception e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}

    		ext.extract(doc);
    		
    		g.setUp(doc);
			//System.out.println(g.scoreVector);
			//System.out.println(g.transitionMatrix);
			g.randomWalk(1);
			g.getScoreVector().toString(Integer.MAX_VALUE, true, true, null);
			//String outdir = "output_graph_acc";
			//String outfile = file.toString().replace("output_raw",outdir);
			//g.output(outfile, g.getHighestScoreMap());
			String cont="";
			HashMap<String, POI> highpois = g.getHighestScoreMap();
			HashSet<String> highpoiname = new HashSet<String>();
			Iterator<String> iter2 = highpois.keySet().iterator();
			//System.out.println("size: "+highpoiname.size());
			while(iter2.hasNext()) {
				String poi = iter2.next();
				highpoiname.add(highpois.get(poi).toString());
//				System.out.println("high: "+highpois.get(poi).toString());
			}
    		
    		List<Sentence> sentList = doc.sentences();
    		JSONArray pois = new JSONArray();
    		
    		for (int j = 0; j < sentList.size(); j++) {
    			Sentence sent = (Sentence) sentList.get(j);
    			String sen = sent.text();
    			if (sent.poiAnnotations().size() > 0) {
    				IndexedList<Pair<Integer, Integer>, POI> indexList = sent.poiAnnotations();
    				Iterator<Pair<Integer, Integer>> iter = indexList.keySet().iterator();

    				while (iter.hasNext()) {
    					Pair<Integer, Integer> p = iter.next();
    					String rawpoi = indexList.get(p).get(0).toString();
    					if(!highpoiname.contains(rawpoi)) continue;
    						
    					JSONObject poi = new JSONObject ();
    					    					
    					try {
    						String poiid = "0";
    						//String poiname = indexList.get(p).get(0).toString().replace("[","").replace("]","");
    						String poiname = indexList.get(p).get(0).toString();
    						String urlpoiname = URLEncoder.encode(poiname);
							int startidx = p.getFirst();
							int endidx = p.getSecond();
							String poiurl = poisearchurl+urlpoiname;

    						poi.put("sentidx",j+1);
    						poi.put("poiname",poiname);
    						poi.put("poiurl", poiurl);
							poi.put("startidx",startidx);
							poi.put("endidx",endidx);
							poi.put("poiid",poiid);
							
							pois.put(poi);
							String ment = sen.substring(startidx,endidx);
							String link = "<a href=\""+poiurl+"\">"+ment+"</a>";
							//System.out.println(ment);
							//System.out.println(link);
							sen=sen.replace(ment, link);
							cont=cont+"<br>"+sen;
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
    					break;
    					//System.out.println("he: " + sent.text().substring(p.getFirst(), p.getSecond()));
    					//System.out.println("ha: " + indexList.get(p));
    				}
    			}
    		}
    		//System.out.println(pois.toString());
    		
    		/*String message = null;
			try {
				message = pois.toString(4);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
    		//resp.setEntity(cont,MediaType.TEXT_PLAIN);
    		resp.setEntity(cont,MediaType.TEXT_HTML);
    	}
    };
    public Restlet poiextract = new Restlet() {
    	@Override
    	public void handle(Request req, Response resp) {
    		
    		Form r = req.getResourceRef().getQueryAsForm();
    		String infile = r.getFirstValue("infile"); 
    		System.out.println("Infile: "+infile);
    		GraphDisambiguation g = new GraphDisambiguation();
    		URL url = null;
    		try {
				url = new URL(infile);
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    		String text="";
    		BufferedReader in = null;
			try {
				in = new BufferedReader(new InputStreamReader(url.openStream(),"UTF-8"));
			} catch (UnsupportedEncodingException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
    		String line="";
    		try {
				while((line=in.readLine())!=null){
				text = text+"\n"+line;
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    		try {
				in.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    		Document doc = null;
    		try {
    			doc = Document.createFromRawText(text);
    		} catch (Exception e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}

ext.extract(doc);
    		
    		g.setUp(doc);
			//System.out.println(g.scoreVector);
			//System.out.println(g.transitionMatrix);
			g.randomWalk(1);
			g.getScoreVector().toString(Integer.MAX_VALUE, true, true, null);
			//String outdir = "output_graph_acc";
			//String outfile = file.toString().replace("output_raw",outdir);
			//g.output(outfile, g.getHighestScoreMap());
			String cont="";
			HashMap<String, POI> highpois = g.getHighestScoreMap();
			HashSet<String> highpoiname = new HashSet<String>();
			Iterator<String> iter2 = highpois.keySet().iterator();
			while(iter2.hasNext()) {
				String poi = iter2.next();
				highpoiname.add(highpois.get(poi).toString());
				//System.out.println(highpois.get(poi).toString());
			}
    		
    	//주제지역
			HashSet<String> themelocs = new HashSet<String>();
			//System.out.println(text);
			String cont2=text;
			
			
			//트리초기화
			for(Enumeration<DefaultMutableTreeNode> e = root.breadthFirstEnumeration();e.hasMoreElements();){
				DefaultMutableTreeNode n = e.nextElement();
				if(n.toString()==null) continue;

				Location node = (Location)n.getUserObject();
				if(node==null) continue;
				//System.out.println(node.loc);
				node.cnt=0;
			}
			
			Iterator iter3 = locs.iterator();
			int totalcnt=0;
			while(iter3.hasNext()){
				String loc = (String) iter3.next();
				
				if(loc==null) continue;
				if(loc.equals("")) continue;
				//System.out.println(loc);
				final String regex = loc;
				//Pattern p = Pattern.compile(regex);
				Pattern p = Pattern.compile("\\b"+regex);
				Matcher m = p.matcher(cont2);
				int mcnt=0;
				while(m.find()) {
					mcnt++;
					totalcnt++;
				}
				//System.out.println("totalcnt: "+totalcnt);
				if(mcnt>0){
					//System.out.println(regex + " "+mcnt);
				//out2.write(regex+"\t"+mcnt+"\n");
				
				for(Enumeration<DefaultMutableTreeNode> e = root.breadthFirstEnumeration();e.hasMoreElements();){
					DefaultMutableTreeNode n = e.nextElement();
					if(n.toString()==null) continue;
					
					if(n.toString().equals(loc)){
						Location node = (Location)n.getUserObject();	
						if(node==null) continue;
						node.cnt+=mcnt;
					}
				}
				}
			}
				
				//spread, power 계산 시작
				for(Enumeration<DefaultMutableTreeNode> e = root.breadthFirstEnumeration();e.hasMoreElements();){
					DefaultMutableTreeNode n = e.nextElement();
					if(n.toString()==null) continue;
					Location node = (Location)n.getUserObject();
					if(node==null) continue;
					node.p=(float)node.cnt/totalcnt;
					//System.out.println(node.loc + " "+node.p);
				}
				//spread계산
				for(Enumeration<DefaultMutableTreeNode> e = root.breadthFirstEnumeration();e.hasMoreElements();){
					DefaultMutableTreeNode n = e.nextElement();
					if(n.toString()==null) continue;
					int cnum = n.getChildCount();
					float cpowersum = 0.0f;
					for(int l=0;l<cnum;l++){
						DefaultMutableTreeNode c = (DefaultMutableTreeNode) n.getChildAt(l);
						Location child = (Location)c.getUserObject();
						cpowersum+=child.p;
					}
					float cpowerentsum=0.0f;
					for(int l=0;l<cnum;l++){
						DefaultMutableTreeNode c = (DefaultMutableTreeNode) n.getChildAt(l);
						Location child = (Location)c.getUserObject();
						float cpowernor=(float)child.p/cpowersum;
						cpowerentsum-=(cpowernor * Math.log(cpowernor)); 
						
					}

					float spread = (float) (cpowerentsum / Math.log(cnum));
					Location node = (Location)n.getUserObject();
					if(node==null) continue;
					node.s=spread;
					
				}
				for(Enumeration<DefaultMutableTreeNode> e = root.breadthFirstEnumeration();e.hasMoreElements();){
					DefaultMutableTreeNode n = e.nextElement();
					if(n.toString()==null) continue;
					Location node = (Location)n.getUserObject();
					if(node==null) continue;
					if(node.p>=power_t) {
						themelocs.add(node.loc);
						//System.out.println(node.loc);
					}else if(node.s>spread_t) {
						themelocs.add(node.loc);
						//System.out.println(node.loc);
					}
					//System.out.println(node.loc + "\t"+node.p + "\t"+node.s);
				}
			
			
		//주제지역 끝
			List<Sentence> sentList = doc.sentences();
			JSONArray pois = new JSONArray();
    		
    		for (int j = 0; j < sentList.size(); j++) {
    			Sentence sent = (Sentence) sentList.get(j);
    			String sen = sent.text();
    			if (sent.poiAnnotations().size() > 0) {
    				IndexedList<Pair<Integer, Integer>, POI> indexList = sent.poiAnnotations();
    				Iterator<Pair<Integer, Integer>> iter = indexList.keySet().iterator();

    				while (iter.hasNext()) {
    					int flag=0;
    					Pair<Integer, Integer> p = iter.next();
    					List<POI> idx = indexList.get(p);
    					for(int k = 0 ; k <idx.size();k++){
    						String rawpoi = indexList.get(p).get(k).toString();
    					
    					//System.out.println(rawpoi);
    					if(!highpoiname.contains(rawpoi)) {
    						System.out.println("Regared through Graph: "+rawpoi);
    						continue;
    					}
    					String locs[] = rawpoi.split("-");
    					if(locs[1].length()>2 && locs[1].endsWith("시")) locs[1]=locs[1].substring(0,locs[1].length()-1);
    					else if(locs[1].length()>2 && locs[1].endsWith("군")) locs[1]=locs[1].substring(0,locs[1].length()-1);
    					if(totalcnt>loc_thres)
    					if(!(themelocs.contains(locs[0])||themelocs.contains(locs[1])||themelocs.contains(locs[2]))){
				
    						System.out.println("Regared through Theme Location: "+ rawpoi);
    						continue;
    						
    					}
    						//System.out.println("Found!!");
    						
    						
    					JSONObject poi = new JSONObject ();
    					    					
    					try {
    						String poiid="0";
    						String poiname = indexList.get(p).get(k).toString().replace("[","").replace("]","");
    						String urlpoiname = URLEncoder.encode(poiname);
							int startidx = p.getFirst();
							int endidx = p.getSecond();
							String poiurl = poisearchurl+urlpoiname;

    						poi.put("sentidx",j+1);
    						poi.put("poiname",poiname);
    						poi.put("poiurl", poiurl);
							poi.put("startidx",startidx);
							poi.put("endidx",endidx);
							poi.put("poiid",poiid);
							
							pois.put(poi);
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
    					flag=1;
    					if(flag==1) break;
    					//System.out.println("he: " + sent.text().substring(p.getFirst(), p.getSecond()));
    					//System.out.println("ha: " + indexList.get(p));
    				}
    			}
    		}
    		}
    		//System.out.println(pois.toString());
    		
    		String message = null;
			try {
				message = pois.toString(4);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		resp.setEntity(message,MediaType.TEXT_PLAIN);
    	}
    };
    public Restlet highlight = new Restlet() {
    	@Override
    	public void handle(Request req, Response resp) {
    		Form r = req.getResourceRef().getQueryAsForm();
    		String infile = r.getFirstValue("infile"); 
    		System.out.println("Infile: "+infile);
    		GraphDisambiguation g = new GraphDisambiguation();
    		URL url = null;
    		try {
				//url = new URL("http://chihaya.kaist.ac.kr/data/input/naver_2005math[23399683].txt");
    			url = new URL(infile);
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    		String text="";
    		BufferedReader in = null;
			try {
				in = new BufferedReader(new InputStreamReader(url.openStream(),"UTF-8"));
			} catch (UnsupportedEncodingException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
    		String line="";
    		try {
				while((line=in.readLine())!=null){
				text = text+"\n"+line;
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    		try {
				in.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    		Document doc = null;
    		try {
    			doc = Document.createFromRawText(text);
    		} catch (Exception e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}

    		ext.extract(doc);
    		
    		g.setUp(doc);

//			System.out.println(g.getScoreVector().toString(Integer.MAX_VALUE, true, true, null));
    		g.randomWalk(1);

			g.getScoreVector().toString(Integer.MAX_VALUE, true, true, null);
			String cont="";
			HashMap<String, POI> highpois = g.getHighestScoreMap();
			HashSet<String> highpoiname = new HashSet<String>();
			Iterator<String> iter2 = highpois.keySet().iterator();
			while(iter2.hasNext()) {
				String poi = iter2.next();
				highpoiname.add(highpois.get(poi).toString());
			}
    		
    	//주제지역
			
				HashSet<String> themelocs = new HashSet<String>();
			String cont2=text;
			//트리초기화
			for(Enumeration<DefaultMutableTreeNode> e = root.breadthFirstEnumeration();e.hasMoreElements();){
				DefaultMutableTreeNode n = e.nextElement();
				if(n.toString()==null) continue;

				Location node = (Location)n.getUserObject();
				if(node==null) continue;
				node.cnt=0;
			}
			
			Iterator iter3 = locs.iterator();
			int totalcnt=0;
			while(iter3.hasNext()){
				String loc = (String) iter3.next();
				
				if(loc==null) continue;
				if(loc.equals("")) continue;
				final String regex = loc;
				Pattern p = Pattern.compile("\\b"+regex);
				Matcher m = p.matcher(cont2);
				int mcnt=0;
				while(m.find()) {
					mcnt++;
					totalcnt++;
				}
				if(mcnt>0){
					//System.out.println(regex + " "+mcnt);
				
				for(Enumeration<DefaultMutableTreeNode> e = root.breadthFirstEnumeration();e.hasMoreElements();){
					DefaultMutableTreeNode n = e.nextElement();
					if(n.toString()==null) continue;
					
					if(n.toString().equals(loc)){
						Location node = (Location)n.getUserObject();	
						if(node==null) continue;
						node.cnt+=mcnt;
					}
				}
				}
			}
				
				//spread, power 계산 시작
				for(Enumeration<DefaultMutableTreeNode> e = root.breadthFirstEnumeration();e.hasMoreElements();){
					DefaultMutableTreeNode n = e.nextElement();
					if(n.toString()==null) continue;
					Location node = (Location)n.getUserObject();
					if(node==null) continue;
					node.p=(float)node.cnt/totalcnt;
				}
				//spread계산
				for(Enumeration<DefaultMutableTreeNode> e = root.breadthFirstEnumeration();e.hasMoreElements();){
					DefaultMutableTreeNode n = e.nextElement();
					if(n.toString()==null) continue;
					int cnum = n.getChildCount();
					float cpowersum = 0.0f;
					for(int l=0;l<cnum;l++){
						DefaultMutableTreeNode c = (DefaultMutableTreeNode) n.getChildAt(l);
						Location child = (Location)c.getUserObject();
						cpowersum+=child.p;
					}
					float cpowerentsum=0.0f;
					for(int l=0;l<cnum;l++){
						DefaultMutableTreeNode c = (DefaultMutableTreeNode) n.getChildAt(l);
						Location child = (Location)c.getUserObject();
						float cpowernor=(float)child.p/cpowersum;
						cpowerentsum-=(cpowernor * Math.log(cpowernor)); 
						
					}

					float spread = (float) (cpowerentsum / Math.log(cnum));
					Location node = (Location)n.getUserObject();
					if(node==null) continue;
					node.s=spread;
					
					
				}
				for(Enumeration<DefaultMutableTreeNode> e = root.breadthFirstEnumeration();e.hasMoreElements();){
					DefaultMutableTreeNode n = e.nextElement();
					if(n.toString()==null) continue;
					Location node = (Location)n.getUserObject();
					if(node==null) continue;
					if(node.p>=power_t) {
						themelocs.add(node.loc);
						System.out.println("Theme location added: "+node.loc + " "+node.p+ " "+node.s);
					}else if(node.s>spread_t) {
						themelocs.add(node.loc);
						System.out.println("Theme location added: "+node.loc + " "+node.p+ " "+node.s);
					}
				}
			
			
		//주제지역 끝
			List<Sentence> sentList = doc.sentences();
			JSONArray pois = new JSONArray();
    		
    		for (int j = 0; j < sentList.size(); j++) {
    			Sentence sent = (Sentence) sentList.get(j);
    			String sen = sent.text();
    			if (sent.poiAnnotations().size() > 0) {
    				IndexedList<Pair<Integer, Integer>, POI> indexList = sent.poiAnnotations();
    				Iterator<Pair<Integer, Integer>> iter = indexList.keySet().iterator();

    				while (iter.hasNext()) {
    					int flag=0;
    					Pair<Integer, Integer> p = iter.next();
    					List<POI> idx = indexList.get(p);
    					for(int k = 0 ; k <idx.size();k++){
    						String rawpoi = indexList.get(p).get(k).toString();
    					
    					if(!highpoiname.contains(rawpoi)) {
    						continue;
    					}
    					String locs[] = rawpoi.split("-");
    					if(locs[1].length()>2 && locs[1].endsWith("시")) locs[1]=locs[1].substring(0,locs[1].length()-1);
    					else if(locs[1].length()>2 && locs[1].endsWith("군")) locs[1]=locs[1].substring(0,locs[1].length()-1);

    					if(totalcnt>loc_thres)
    					if(!(themelocs.contains(locs[0])||themelocs.contains(locs[1])||themelocs.contains(locs[2]))){
    						
    						System.out.println("Regarded by Theme location: "+ rawpoi);
    						continue;
    						
    					}
    						
    						
    					JSONObject poi = new JSONObject ();
    					    					
    					try {
    						String poiid = "0";
    						String poiname = indexList.get(p).get(k).toString();
    						String urlpoiname = URLEncoder.encode(poiname);
							int startidx = p.getFirst();
							int endidx = p.getSecond();
							String poiurl = poisearchurl+urlpoiname;

    						poi.put("sentidx",j+1);
    						poi.put("poiname",poiname);
    						poi.put("poiurl", poiurl);
							poi.put("startidx",startidx);
							poi.put("endidx",endidx);
							poi.put("poiid",poiid);
							
							pois.put(poi);
							String ment = sent.text().substring(startidx,endidx);
							String link = "<a href=\""+poiurl+"\">"+ment+"</a>";
							sen=sen.replace(ment, link);
							flag=1;
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
    					if(flag==1) break;
    				}
    					
    					
    				}

    			}
    			cont=cont+"<br>"+sen;
    		}

    		resp.setEntity(cont,MediaType.TEXT_HTML);
    	}
    };
    
    @Override
    public Restlet createInboundRoot() {
    	try {
			init();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	Router router = new Router();
//    	router.attach("http://localhost:8111/poiextract",this.poiextract);
//    	router.attach("http://localhost:8111/highlight",this.highlight);
    	router.attach(serverurlport+"/poiextract",this.poiextract);
    	router.attach(serverurlport+"/highlight",this.highlight);

    	return router;
    }
	/** 
     * 초기화 메소드 
	 */
	public void init () throws IOException {
		String infile = "data\\(20121025) KTO_POI_with_Field-unicode.txt";
		String outfile = "data\\gazetteer.txt";
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(infile),"unicode"));
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outfile),"utf-8"));
		String line;
		
		
		while((line=in.readLine())!=null){
			String[] tok = line.split("\t");
			if(tok.length<9) continue;
			String state =tok[6];
			String city = tok[7];
			if(city.length()>2 && city.endsWith("시")) city=city.substring(0,city.length()-1);
			else if(city.length()>2 && city.endsWith("군")) city=city.substring(0,city.length()-1);
			String town = tok[8];
			int sflag=0;
			int cflag=0;
			int tflag=0;
			//트리 구성	
			DefaultMutableTreeNode s=null;
			for(Enumeration<DefaultMutableTreeNode> e = (Enumeration<DefaultMutableTreeNode>)root.children();e.hasMoreElements();){
				s = e.nextElement();
				if(s.getUserObject().toString().equals(state)) { sflag=1; break; }
			}
			if(sflag==0) root.add(new DefaultMutableTreeNode(new Location(state)));
			for(Enumeration<DefaultMutableTreeNode> e = root.children();e.hasMoreElements();){
				s = e.nextElement();
				if(s.getUserObject().toString().equals(state)) break;
			}
			DefaultMutableTreeNode c=null;
			for(Enumeration<DefaultMutableTreeNode> e = s.children();e.hasMoreElements();){
				c = e.nextElement();
				if(c.getUserObject().toString().equals(city)) { cflag=1; break; }
			}
			if(cflag==0) s.add(new DefaultMutableTreeNode(new Location(city)));
			for(Enumeration<DefaultMutableTreeNode> e = s.children();e.hasMoreElements();){
				c = e.nextElement();
				if(c.getUserObject().toString().equals(city)) break;
			}
			DefaultMutableTreeNode t=null;
			for(Enumeration<DefaultMutableTreeNode> e = c.children();e.hasMoreElements();){
				t = e.nextElement();
				if(t.getUserObject().toString().equals(town)) { tflag=1; break; }
			}
			if(tflag==0) c.add(new DefaultMutableTreeNode(new Location(town)));
			for(Enumeration<DefaultMutableTreeNode> e = c.children();e.hasMoreElements();){
				t = e.nextElement();
				if(t.getUserObject().toString().equals(town)) break;
			}
		}
		
		in.close();
		
		
		for(Enumeration<DefaultMutableTreeNode> e = root.breadthFirstEnumeration();e.hasMoreElements();){
			DefaultMutableTreeNode n = e.nextElement();
			locs.add(n.toString());

			out.write(n.toString()+"\n");
		}
		out.close();
	}
   
}

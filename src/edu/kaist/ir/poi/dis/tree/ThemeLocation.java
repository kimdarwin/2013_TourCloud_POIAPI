package edu.kaist.ir.poi.dis.tree;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.tree.DefaultMutableTreeNode;
/**
 * 
 * @author Kyung-min Kim
 * 구현 논문: Detecting Geographic Locations from Web Resources, GIR’05
 * 문서에 언급된 지명들을 지명사전 트리 구조에 카운팅한 후,
 * 엔트로피 기반 알고리즘으로 일정 점수 이상의 대표 지명들을 추출함.
 * 설정된 임계치: power=0.3 이상 or spread=0.2 이상일 시 추출.
 * 
 * API:
 * 한 문서에서 대표 지명 추출
 * Input: 파일, Output: 파일
 * ex) extractThemeLocationsFromFile("data/sample/1.txt","data/sample/out/1.txt");
 * 
 * 주어진 디렉토리의 모든 문서에서 대표 지명 추출
 * Input: 디렉토리, Output: 디렉토리
 * ex)	extractThemeLocationsFromDir("data/sample","data/sample/out");
 */

public class ThemeLocation {
	private static DefaultMutableTreeNode root= new DefaultMutableTreeNode();
	private static HashSet<String> locs = new HashSet<String>();
	
	//private static float power_t = 0.3f;
	//private static float spread_t = 0.2f;
	private static float power_t = 0.1f;
	private static float spread_t = 0.1f;
	
	/** 
     * 테스트용 main 
	 * @param args
	 * 			InputDir OutputDir
	 */
	public static void main(String[] args) throws IOException {
		init();
		//extractThemeLocationsFromFile("data/sample/1.txt","data/sample/out/1.txt");
		
		//아래3줄을 실행
		String indir = "C:\\Users\\hellcat\\Desktop\\kist\\어노테이션\\분석\\output_raw";
		String outdir = "C:\\Users\\hellcat\\Desktop\\kist\\어노테이션\\분석\\output_raw_loc_0.1";
		extractThemeLocationsFromDir(indir,outdir);
		
		//extractThemeLocationsFromDir(args[0],args[1]);
		//args[1] = "data\\sample", args[2] = "data\\sample\\out";		
	}
	
	/** 
     * 초기화 메소드 
	 */
	public static void init () throws IOException {
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
	/** 
     *  주어진 디렉토리의 모든 파일에서 대표지명 추출 메소드.
	 * @param indir
	 * 			Input directory.
	 * @param outdir
	 * 			Output directory.
	 */
	public static void extractThemeLocationsFromDir(String indir, String outdir) throws IOException{
		
		String docdir = indir;
		File[] files = new File(docdir).listFiles();
		for(int i=0;i<files.length;i++){


			if(!files[i].toString().contains(".txt")) continue;
			String locfile = files[i].toString().replace(docdir,outdir);
			extractThemeLocationsFromFile(files[i].toString(),locfile);
			
		}
	}
	/** 
     *  한 파일에서 대표지명 추출 메소드.
	 * @param infile
	 * 			Input file.
	 * @param outfile
	 * 			Output file.
	 */
	public static void extractThemeLocationsFromFile(String infile, String outfile) throws IOException{
			//한 파일에서 대표지명 추출 메소드.
			BufferedReader in2 = new BufferedReader(new InputStreamReader(new FileInputStream(infile),"utf-8"));

			
			String locfile = outfile;
			BufferedWriter out2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(locfile),"utf-8"));
			String line2;
			String cont2="";
			while((line2=in2.readLine())!=null){
				cont2+=line2+"\n";
			}
			in2.close();

			
			//트리초기화
			for(Enumeration<DefaultMutableTreeNode> e = root.breadthFirstEnumeration();e.hasMoreElements();){
				DefaultMutableTreeNode n = e.nextElement();
				if(n.toString()==null) continue;
				Location node = (Location)n.getUserObject();
				node.cnt=0;
			}
			
			Iterator iter = locs.iterator();
			int totalcnt=0;
			while(iter.hasNext()){
				String loc = (String) iter.next();
				if(loc==null) continue;
				final String regex = loc;
				Pattern p = Pattern.compile(regex);
				Matcher m = p.matcher(cont2);
				int mcnt=0;
				while(m.find()) {
					mcnt++;
					totalcnt++;
				}
				if(mcnt>0){
				out2.write(regex+"\t"+mcnt+"\n");
				
				for(Enumeration<DefaultMutableTreeNode> e = root.breadthFirstEnumeration();e.hasMoreElements();){
					DefaultMutableTreeNode n = e.nextElement();
					if(n.toString()==null) continue;
					
					if(n.toString().equals(loc)){
						Location node = (Location)n.getUserObject();	
						node.cnt+=mcnt;
					}
				}
				}
			}
				out2.write("\n");
				//spread, power 계산 시작
				for(Enumeration<DefaultMutableTreeNode> e = root.breadthFirstEnumeration();e.hasMoreElements();){
					DefaultMutableTreeNode n = e.nextElement();
					if(n.toString()==null) continue;
					Location node = (Location)n.getUserObject();
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
					node.s=spread;
				}
				for(Enumeration<DefaultMutableTreeNode> e = root.breadthFirstEnumeration();e.hasMoreElements();){
					DefaultMutableTreeNode n = e.nextElement();
					if(n.toString()==null) continue;
					Location node = (Location)n.getUserObject();
					if(node.p>power_t) {
						out2.write(node.loc+"\t"+node.p+"\t"+node.s+"\n");
					}else if(node.s>spread_t) {
						out2.write(node.loc+"\t"+node.p+"\t"+node.s+"\n");
					}
				}
				
			
			out2.close();
		
	}


}

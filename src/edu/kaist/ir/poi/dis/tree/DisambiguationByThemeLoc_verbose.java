package edu.kaist.ir.poi.dis.tree;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
public class DisambiguationByThemeLoc_verbose {

	
	public static void main(String[] args) throws IOException {
		String indir = "input\\testcollection_intersection3";
		String ofile = "output\\themeloc_output.txt";
		String lfile = "input\\themelocmap.txt";
		String stopfile = "input\\stopwords.txt";
		
		File[] dirs = new File(indir).listFiles();
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ofile),"utf-8"));
		BufferedReader in2 = new BufferedReader(new InputStreamReader(new FileInputStream(lfile),"utf-8"));
		String line2;
		HashSet<String> stops = new HashSet<String>();
		BufferedReader in3 = new BufferedReader(new InputStreamReader(new FileInputStream(stopfile),"utf-8"));
		
		while((line2=in3.readLine())!=null){
			stops.add(line2);
		}
		in3.close();
		
		HashMap<String,HashSet<String>> locmap = new HashMap<String,HashSet<String>>();
		while((line2=in2.readLine())!=null){
			String tok[] = line2.split("\t");
			String f = tok[0];
			String locs[];
			if(tok.length<2) locs=null;
			else{
				locs=tok[1].split(" ");
			locmap.put(f,new HashSet<String>());
			
			for(String loc:locs){
				locmap.get(f).add(loc);
			}
		}
		}
		in2.close();
		
		
		for(File file:dirs){
			//File[] files = dir.listFiles();
			//for(File file:files){
				if(file.getName().contains(".bak")) continue;
				if(file.getName().contains("_bak")) continue;
				System.out.println(file.getName());
				//String odir = file.toString().replace(indir,outdir).replace(file.getName(),"");
			//	String odir = outdir;
				//new File(odir).mkdirs();
				//String ofile = odir+"\\"+file.getName();
				
				BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file),"utf-8"));
				
				String line;
				//String locs="";
				int flag = 0;
				int cnt=0;
				HashSet<String> tlocs = locmap.get(file.getName());
				
				
				while((line=in.readLine())!=null) {
					cnt++;
//					String[] tok = line.split("\t");
//					if(tok==null) continue;
//					if(tok[0]==null) continue;
//					if(!tok[0].equals("")) locs += tok[0]+ " "; 
//					if(line.equals("\n")) continue;
//					if(line.startsWith("->\t")) continue;
					if(line.startsWith("->")) {flag=0; cnt=0;}
					if(!line.startsWith("candidate:")) continue;
					line = file.getName()+"\t"+line;
					String cand = line.replace("candidate:","").replace("<P>","").replace("</P>","");
					if(line.contains("<P>")){
						line = line+"\t1";
					}else line = line+"\t0";
					
					if(tlocs!=null){ 
					Iterator<String> iter = tlocs.iterator();
					while(iter.hasNext()){
						String tl = iter.next();
						if(line.contains(tl)&&flag==0) {
							int nostop=1;
							Iterator<String> iter2 = stops.iterator(); 
							while(iter2.hasNext()){
								String stopsufix=iter2.next();
								//System.out.println(stopsufix);
								if(cand.endsWith(stopsufix)){
									//System.out.println(cand);
									nostop=0;
								}
							}
							if(nostop==1){
								System.out.println(line.replace("candidate:", ""));
								line = line+"\t"+"1"+"\t"+tl;
							flag=1;}
						}
						//System.out.println(tl);
					}
					}
					out.write(cnt+"\t"+line+"\n");
				}
				//out.write(file.getName()+"\t"+locs+"\n");
				in.close();
				
				
			//}
	}
			out.close();		
		}

}

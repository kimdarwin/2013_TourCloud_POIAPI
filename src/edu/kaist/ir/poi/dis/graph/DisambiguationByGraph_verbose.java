package edu.kaist.ir.poi.dis.graph;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
public class DisambiguationByGraph_verbose {


	public static void main(String[] args) throws IOException {
		String indir = "input\\testcollection_intersection3";
		String ofile = "output\\graph_output.txt";
		String lfile = "input\\graphmap.txt";
		String stopfile = "input\\stopwords.txt";
		
		File[] dirs = new File(indir).listFiles();
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ofile),"utf-8"));
		BufferedReader in2 = new BufferedReader(new InputStreamReader(new FileInputStream(lfile),"utf-8"));
		String line2;
		HashMap<String,HashSet<String>> locmap = new HashMap<String,HashSet<String>>();
		HashSet<String> stops = new HashSet<String>();
		BufferedReader in3 = new BufferedReader(new InputStreamReader(new FileInputStream(stopfile),"utf-8"));
		
		while((line2=in3.readLine())!=null){
			stops.add(line2);
		}
		in3.close();
		
		while((line2=in2.readLine())!=null){
			String tok[] = line2.split("\t");
			String f = tok[0];
			String locs[];
			if(tok.length<2) locs=null;
			else{
				locs=tok[1].split("\\*");
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
				String locs="";
				int flag=0;
				int cnt=0;
				while((line=in.readLine())!=null) {
//					String[] tok = line.split("\t");
//					if(tok==null) continue;
//					if(tok[0]==null) continue;
//					if(!tok[0].equals("")) locs += tok[0]+ " "; 
//					if(line.equals("\n")) continue;
//					if(line.startsWith("->\t")) continue;
					if(line.startsWith("->")) {flag=0; cnt=0;} 
					cnt++;
					if(!line.startsWith("candidate:")) continue;
					String cand = line.replace("candidate:","").replace("<P>","").replace("</P>","");
					line = file.getName()+"\t"+line;
					if(line.contains("<P>")){
						line = line+"\t1";
					}else line = line+"\t0";
					HashSet<String> tlocs = locmap.get(file.getName());
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
								System.out.println(line.replace("candidate:",""));
							line = line+"\t"+"1"+"\t"+tl;
							flag=1;
							}
						}
						//System.out.println(tl);
					}
					}
					out.write(cnt+"\t"+line+"\n");
				}
				//out.write(file.getName()+"\t"+locs+"\n");
				in.close();
				
				
			
	}
			out.close();		
		}

}

package edu.kaist.ir.testcollection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;

public class GenerateAnnotationMap {
	static HashMap<String,HashMap<String,Integer>> pois = new HashMap<String,HashMap<String,Integer>>();
	static int num_agree=3;
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		String indir = "C:\\Users\\hellcat\\Desktop\\kist\\어노테이션\\태깅통합\\output_최";
		//String outdir = "C:\\Users\\hellcat\\Desktop\\kist\\어노테이션\\태깅통합\\output_intersection3";
		String outdir = "C:\\Users\\hellcat\\Desktop\\kist\\어노테이션\\태깅통합\\output_intersection3";
		//String outdir = "C:\\Users\\hellcat\\Desktop\\kist\\어노테이션\\태깅통합\\output_union";
		extractThemeLocationsFromDir(indir,outdir);
		indir = "C:\\Users\\hellcat\\Desktop\\kist\\어노테이션\\태깅통합\\output_구";
		extractThemeLocationsFromDir(indir,outdir);
		indir = "C:\\Users\\hellcat\\Desktop\\kist\\어노테이션\\태깅통합\\output_박";
		extractThemeLocationsFromDir(indir,outdir);
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
			//String locfile = files[i].toString().replace(docdir,outdir);
			String locfile = outdir+".txt";
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
			//String indir = infile.replace(new File(infile).getName(),"");
			String infilename = new File(infile).getName();
			String locfile = outfile;
			BufferedWriter out2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(locfile),"utf-8"));
			String line2;
			String cont2="";
			while((line2=in2.readLine())!=null){
				//cont2+=line2+"\n";
				if(line2.contains("<P>")){
					String p = line2.replace("candidate:", "").replace("<P>","").replace("</P>","").trim();
					if(pois.containsKey(infilename)){
						if(pois.get(infilename).containsKey(p)){
							int cnt=pois.get(infilename).get(p)+1;
							pois.get(infilename).remove(p);
							pois.get(infilename).put(p,cnt);
						}
						else {
							pois.get(infilename).put(p,1);
						}
					}
					else {
						pois.put(infilename,new HashMap<String,Integer>());
						pois.get(infilename).put(p,1);
					}
				}
			}
			in2.close();

			//out2.write(node.loc+"\t"+node.p+"\t"+node.s+"\n");
			Iterator<String> iter = pois.keySet().iterator();
			while(iter.hasNext()){
				String f = iter.next();
				Iterator<String> piter = pois.get(f).keySet().iterator();
				String poilist = "";
				while(piter.hasNext()){
					String poi = piter.next();
					if(pois.get(f).get(poi)>=num_agree){
					poilist = poi+"*"+poilist;
					}
				}
				out2.write(f+"\t"+poilist+"\n");
			}
		
				
			System.out.println(outfile);
			out2.close();
		
	}
}

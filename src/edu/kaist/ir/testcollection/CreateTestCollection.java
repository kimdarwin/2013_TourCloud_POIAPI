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
import java.util.HashSet;
import java.util.Iterator;

public class CreateTestCollection {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		//String indir = "C:\\Users\\kgm86\\Desktop\\kist\\어노테이션\\피드백\\구영모\\Output";
		String indir = "C:\\Users\\hellcat\\Desktop\\kist\\어노테이션\\태깅통합\\output_구";
		//String outdir = "C:\\Users\\kgm86\\Desktop\\kist\\어노테이션\\분석\\output_raw";
		//new File(outdir).mkdirs();
		
		File[] dirs = new File(indir).listFiles();
		//String ofile = "C:\\Users\\kgm86\\Desktop\\kist\\어노테이션\\분석\\구-output.txt";
		String ofile = "C:\\Users\\hellcat\\Desktop\\kist\\어노테이션\\태깅통합\\testcollection_union";
		//BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ofile),"utf-8"));
		
//		String lfile = "C:\\Users\\kgm86\\Desktop\\kist\\어노테이션\\분석\\locmap.txt";
		//String lfile = "C:\\Users\\kgm86\\Desktop\\kist\\어노테이션\\분석\\locmap_acc.txt";
		String lfile = "C:\\Users\\hellcat\\Desktop\\kist\\어노테이션\\태깅통합\\output_union.txt";
		BufferedReader in2 = new BufferedReader(new InputStreamReader(new FileInputStream(lfile),"utf-8"));
		String line2;
		HashMap<String,HashSet<String>> locmap = new HashMap<String,HashSet<String>>();
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
				System.out.println(loc);
			}
		}
		}
		in2.close();
		
		
		for(File file:dirs){
			//File[] files = dir.listFiles();
			//for(File file:files){
				if(file.getName().contains(".bak")) continue;
				if(file.getName().contains("_bak")) continue;
				//String odir = file.toString().replace(indir,outdir).replace(file.getName(),"");
			//	String odir = outdir;
				//new File(odir).mkdirs();
				//String ofile = odir+"\\"+file.getName();
				
				BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file),"utf-8"));
				String outfile = file.toString().replace(indir,ofile);
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outfile),"utf-8"));
				String line;
				String locs="";
				while((line=in.readLine())!=null) {

					if(line.startsWith("candidate:")){
					HashSet<String> tlocs = locmap.get(file.getName());
					if(tlocs!=null){ 
					Iterator<String> iter = tlocs.iterator();
					while(iter.hasNext()){
						String tl = iter.next();
						//if(line.contains(tl)) line = line+"\t"+"1"+"\t"+tl;
						if(line.contains(tl)) line = line.replace("<P>","").replace("</P>","").replace(tl,"<P>"+tl+"</P>");
						//System.out.println(tl);
					}
					out.write(line+"\n");
					}
					else{
					
						out.write(line+"\n");
					}
					}
					else{
					out.write(line+"\n");
					}
				//}
				//out.write(file.getName()+"\t"+locs+"\n");
				
				
				
			}
				in.close();
				out.close();		
	}
			
		}

}

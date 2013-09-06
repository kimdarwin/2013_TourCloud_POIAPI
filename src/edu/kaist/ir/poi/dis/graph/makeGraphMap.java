package edu.kaist.ir.poi.dis.graph;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class makeGraphMap {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		String indir = "f:\\kkm\\bak\\Documents and Settings\\김경민\\workspace\\2012_TourCloud\\data\\output_graph_0.4";
		//String outdir = "C:\\Users\\kgm86\\Desktop\\kist\\어노테이션\\분석\\output_raw";
		//new File(outdir).mkdirs();
		
		File[] files = new File(indir).listFiles();
		String ofile = "C:\\Users\\hellcat\\Desktop\\kist\\어노테이션\\분석\\graphmap.txt";
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ofile),"utf-8"));
//		for(File dir:dirs){
//			File[] files = dir.listFiles();
			for(File file:files){
				if(file.getName().contains(".bak")) continue;
				if(file.getName().contains("_bak")) continue;
				//String odir = file.toString().replace(indir,outdir).replace(file.getName(),"");
			//	String odir = outdir;
				//new File(odir).mkdirs();
				//String ofile = odir+"\\"+file.getName();
				
				BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file),"utf-8"));
				
				String line;
				String locs="";
				while((line=in.readLine())!=null) {
					String[] tok = line.split("\t");
					if(tok==null) continue;
					if(tok[0]==null) continue;
					///if(tok.length<3) continue;
					if(!tok[0].equals("")) locs += tok[0]+ "*"; 
//					if(line.equals("\n")) continue;
//					if(line.startsWith("->\t")) continue;
//					if(line.startsWith("candidate:")) continue;
					
				}
				out.write(file.getName()+"\t"+locs+"\n");
				in.close();
				
				
			}
			
			out.close();		
		}
	
	}
	

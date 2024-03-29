package edu.kaist.ir.io;

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

public class null_ThemeLoc_nodupanswer_nostopwords {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		String indir = "C:\\Users\\hellcat\\Desktop\\kist\\어노테이션\\태깅통합\\testcollection_union";
		//String indir = "C:\\Users\\hellcat\\Desktop\\kist\\어노테이션\\태깅통합\\testcollection_intersection3";
		//String indir = "C:\\Users\\kgm86\\Desktop\\kist\\어노테이션\\피드백\\구영모\\Output";
		//String outdir = "C:\\Users\\kgm86\\Desktop\\kist\\어노테이션\\분석\\output_raw";
		//new File(outdir).mkdirs();
		
		File[] dirs = new File(indir).listFiles();
		//String ofile = "C:\\Users\\kgm86\\Desktop\\kist\\어노테이션\\분석\\구-output.txt";
		//String ofile = "C:\\Users\\hellcat\\Desktop\\kist\\어노테이션\\평가\\intersection3-output_acc_nodup.txt";
		String ofile = "C:\\Users\\hellcat\\Desktop\\kist\\어노테이션\\평가\\union-output_acc_nodup.txt";
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ofile),"utf-8"));
		
//		String lfile = "C:\\Users\\kgm86\\Desktop\\kist\\어노테이션\\분석\\locmap.txt";
		String lfile = "C:\\Users\\hellcat\\Desktop\\kist\\어노테이션\\분석\\locmap_acc.txt";
		BufferedReader in2 = new BufferedReader(new InputStreamReader(new FileInputStream(lfile),"utf-8"));
		String line2;
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
				//String odir = file.toString().replace(indir,outdir).replace(file.getName(),"");
			//	String odir = outdir;
				//new File(odir).mkdirs();
				//String ofile = odir+"\\"+file.getName();
				
				BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file),"utf-8"));
				
				String line;
				//String locs="";
				int flag = 0;
				HashSet<String> tlocs = locmap.get(file.getName());
				
				
				while((line=in.readLine())!=null) {
//					String[] tok = line.split("\t");
//					if(tok==null) continue;
//					if(tok[0]==null) continue;
//					if(!tok[0].equals("")) locs += tok[0]+ " "; 
//					if(line.equals("\n")) continue;
//					if(line.startsWith("->\t")) continue;
					if(line.startsWith("->")) flag=0;
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
							
							//if(nostop==1){
							line = line+"\t"+"1"+"\t"+tl;
							flag=1;//}
						}
						//System.out.println(tl);
					}
					}
					out.write(line+"\n");
				}
				//out.write(file.getName()+"\t"+locs+"\n");
				in.close();
				
				
			//}
	}
			out.close();		
		}

}

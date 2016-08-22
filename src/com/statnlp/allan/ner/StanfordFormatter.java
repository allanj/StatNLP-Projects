package com.statnlp.allan.ner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import com.statnlp.commons.crf.RAWF;

public class StanfordFormatter {

	public StanfordFormatter() {
		// TODO Auto-generated constructor stub
	}
	
	public static void ontoNotes2conllx(String file, String out, boolean lowercase) throws IOException{
		BufferedReader reader = RAWF.reader(file);
		PrintWriter pw = RAWF.writer(out);
		String line = null;
		while((line = reader.readLine())!=null){
			if(line.equals("")) {pw.write("\n"); continue;}
			String[] vals = line.split(" ");
			String word = vals[1];
			if(lowercase) word = vals[1].toLowerCase();
			pw.write(vals[0]+"\t"+word+"\t_\t"+vals[2]+"\t"+vals[2]+"\t_\t"+vals[4]+"\t"+vals[5]+"\t_\t_\t"+vals[3]+"\n");
		}
		//pw.write("\n");
		pw.close();
		reader.close();
	}
	
	public static void main(String[] args) throws IOException{
		String[] types = new String[]{"abc","cnn","mnb","nbc","pri","voa"};
		String[] files = new String[]{"train","dev","test"};
		String suffix = ".conllx";
		boolean lowercase = false;
		if(lowercase)
			suffix = ".lowercase.conllx";
		for(String type: types){
			for(String file: files){
				ontoNotes2conllx("data/alldata/"+type+"/"+file+".output", "data/alldata/"+type+"/"+file+suffix, lowercase);
			}
		}
		
	}

}

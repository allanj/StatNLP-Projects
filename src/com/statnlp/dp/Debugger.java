package com.statnlp.dp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import com.statnlp.commons.crf.RAWF;

public class Debugger {

	public Debugger() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws IOException{
		BufferedReader br = RAWF.reader("data/ijcai-final/model.false.ner.eval.txt");
		PrintWriter pw = RAWF.writer("data/ijcai-final/model.false.ner.eval.combined.txt");
		String line = null;
		String prev="";
		while((line = br.readLine())!=null){
			String[] values = line.split(" ");
			if(line.equals("")){
				prev = "";
				pw.write("\n");
			}else{
				if(!values[3].equals("O") && values[3].substring(2, values[3].length()).equals(prev)){
					pw.write(values[0]+" "+values[1]+" "+values[2]+" "+"I-"+prev+"\n");
				}else{
					pw.write(line+"\n");
				}
				prev = values[3].equals("O")? "O":values[3].substring(2, values[3].length());
			}
		}
		br.close();
		pw.close();
	}
}

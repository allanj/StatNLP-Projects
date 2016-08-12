package com.statnlp.dp.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.DependInstance;
import com.statnlp.entity.lcr.ECRFInstance;

public class Formatter {

	
	public static void conll2003(ArrayList<Sentence> sents, String output){
		int enLen = -1;
		int count = 0;
		try{
			PrintWriter pw = RAWF.writer(output);
			for(Sentence sent: sents){
				//because i==0 is the root
				for(int i=1;i<sent.length();i++){
					String word = sent.get(i).getName();
					String pos = sent.get(i).getTag();
					String ner = sent.get(i).getEntity();
					if(!ner.equals("O") && ner.equals(sent.get(i-1).getEntity())){
						count++;
					}else {
						enLen = Math.max(enLen, count);
						count = 0;
					}
					if(!ner.equals("O")) ner = "I-"+ner.substring(0,3);
					pw.write(word+" "+pos+" O "+ner+"\n");
				}
				pw.write("\n");
			}
			pw.close();
			System.err.println("Max entity Len:"+enLen);
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	public static void semeval10ToMST(DependInstance[] insts, String output){
		try{
			PrintWriter pw = RAWF.writer(output);
			for(DependInstance inst: insts){
				Sentence sent = inst.getInput();
				pw.write(sent.get(1).getName());
				for(int i=2;i<sent.length();i++){
					String word = sent.get(i).getName();
					pw.write("\t"+word);
				}
				pw.write("\n");
				
				pw.write(sent.get(1).getTag());
				for(int i=2;i<sent.length();i++){
					String tag = sent.get(i).getTag();
					pw.write("\t"+tag);
				}
				pw.write("\n");
				
				pw.write(sent.get(1).getHeadIndex()+"");
				for(int i=2;i<sent.length();i++){
					int head = sent.get(i).getHeadIndex();
					pw.write("\t"+head);
				}
				pw.write("\n");
				
				
				pw.write("\n");
				
			}
			pw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	
	/**
	 * Convert the semeval data to ner format which is the input of linear-chain NER
	 * @param insts
	 * @param output
	 */
	public static void semevalToNER(DependInstance[] insts, String output){
		try {
			PrintWriter pw = RAWF.writer(output);
			for(DependInstance inst:insts){
				Sentence sent = inst.getInput();
				for(int i=1;i<sent.length();i++){
					pw.write(i+"\t"+sent.get(i).getName()+"\t"+sent.get(i).getTag()+"\t"+sent.get(i).getEntity()+"\t"+sent.get(i).getHeadIndex()+"\n");
				}
				pw.write("\n");
			}
			pw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void semevalToText(DependInstance[] insts, String output){
		try {
			PrintWriter pw = RAWF.writer(output);
			for(DependInstance inst:insts){
				Sentence sent = inst.getInput();
				for(int i=1;i<sent.length();i++){
					pw.write(i+" "+sent.get(i).getName()+" "+sent.get(i).getTag()+" "+sent.get(i).getEntity()+" "+sent.get(i).getHeadIndex()+"\n");
				}
				pw.write("\n");
			}
			pw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void ner2Text(List<ECRFInstance> insts, String output){
		try {
			PrintWriter pw = RAWF.writer(output);
			for(ECRFInstance inst:insts){
				Sentence sent = inst.getInput();
				for(int i=0;i<sent.length();i++){
					pw.write((i+1)+"\t"+sent.get(i).getName()+"\t"+sent.get(i).getTag()+"\t"+sent.get(i).getEntity()+"\t"+sent.get(i).getHeadIndex()+"\n");
				}
				pw.write("\n");
			}
			pw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
	}
	

}

package data.preprocess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.statnlp.commons.types.Sentence;

public class OntoNotesProcess {

	
	public static String[] datasets = {"bc","bn","mz","nw","tc","wb"};
	public static HashSet<String> dataNames;
	
	public static String[] fileType = {"train","development","test"};
	public static String filePrefix = "F:/phd/data/ontonotes-release-5.0_LDC2013T19/ontonotes-release-5.0_LDC2013T19/ontonotes-release-5.0/data/files/data/english/annotations/bn";
	
	public static void process() throws IOException, InterruptedException{
		dataNames = new HashSet<String>();
		for(String file: datasets) dataNames.add(file);
		
		
		for(int f=0;f<fileType.length;f++){
			File file = new File(filePrefix);
			String[] names = file.list();
			for(String data: names){
				if(dataNames.contains(data)){
					File subFile = new File(filePrefix+"/"+data); //the folder that abc/cnn/mnb
					String[] subNames = subFile.list();
					ArrayList<Sentence> sents = new ArrayList<Sentence>();
					for(String numberFolder: subNames){
						File theNumber = new File(filePrefix+"/"+data+"/"+numberFolder); //abc/00/
						if(theNumber.isDirectory()){
							String[] textFileList = theNumber.list();  //abc/00/0001.parse.. something like this.
//							System.out.println(Arrays.toString(textFileList));
							for(String textFile: textFileList){
//								if(textFile.endsWith(".onf"))
//									processTheONFFiles(filePrefix+"/"+data+"/"+numberFolder+"/"+textFile, sents);
								if(textFile.endsWith(".name")){
									String[] codes = textFile.split("\\.");
									String parseFile = codes[0]+".parse";
									processNameFiles(filePrefix+"/"+data+"/"+numberFolder+"/"+textFile, filePrefix+"/"+data+"/"+numberFolder+"/"+parseFile, sents);
								}
							}
						}
					}
					System.out.println("[Info] Finishing dataset:"+data);
					//print these sentences.
					printConll(data,sents);
				}
			}
		}
		
		
	}
	
}

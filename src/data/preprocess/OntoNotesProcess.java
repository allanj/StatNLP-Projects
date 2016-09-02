package data.preprocess;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Sentence;

public class OntoNotesProcess {

	
	public static String[] datasets = {"bc","bn","mz","nw","tc","wb"};
	public static HashSet<String> dataNames;
	
	public static String[] fileType = {"train","development","test"};
	public static String filePrefix = "F:/phd/data/conll-formatted-ontonotes-5.0/data";
	public static String outputPrefx = "E:/Framework/data/ontonotes/";
	
	public static void process() throws IOException, InterruptedException{
		dataNames = new HashSet<String>();
		for(String file: datasets) dataNames.add(file);
		
		
		for(int f=0;f<fileType.length;f++){
			String currPrefix = filePrefix+"/"+fileType[f]+"/data/english/annotations";
			File file = new File(currPrefix);
			String[] names = file.list();
			for(String newstype: names){
				if(dataNames.contains(newstype)){
					File subFile = new File(currPrefix+"/"+newstype); //the folder that bc/bn/nw
					String[] subNames = subFile.list();
					ArrayList<Sentence> sents = new ArrayList<Sentence>();
					for(String program: subNames){
						File programFolder = new File(currPrefix+"/"+newstype+"/"+program); //abc/cctc/cnn/so on
						String[] numFileList = programFolder.list();  //cnn/00 ,01,02, 
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
					System.out.println("[Info] Finishing dataset:"+data);
					//print these sentences.
					printConll(data,sents, fileType[f]);
				}
			}
		}
		
		
	}
	
	private static void printConll(String datasetName, ArrayList<Sentence> sents, String fileType) throws IOException{
		PrintWriter pw = RAWF.writer(outputPrefx+"/"+datasetName+"/"+fileType+".conllx");
		System.out.println("dataset:"+datasetName+" type:"+fileType+" size:"+sents.size());
		for(Sentence sent: sents){
			for(int i=1; i<sent.length(); i++)
				pw.write(i+"\t"+sent.get(i).getName()+"\t_\t"+sent.get(i).getTag()+"\t"+sent.get(i).getTag()+"\t_\t"+sent.get(i).getHeadIndex()+"\t"+sent.get(i).getDepLabel()+"\t_\t_\t"+sent.get(i).getEntity()+"\n");
			pw.write("\n");
		}
	
		pw.close();
	}
	
}

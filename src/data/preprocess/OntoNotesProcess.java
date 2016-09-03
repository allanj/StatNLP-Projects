package data.preprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;

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
						String[] numFolderList = programFolder.list();  //cnn/00 ,01,02,
						for(String numFolderName: numFolderList){
							File numFolder = new File(currPrefix+"/"+newstype+"/"+program+"/"+numFolderName); //abc/00 folder
							String[] textFileList = numFolder.list();
							for(String textFile: textFileList){  //the textfile inside the number folder
								if(textFile.endsWith("_conll")){
									processNameFile(currPrefix+"/"+newstype+"/"+program+"/"+numFolderName+"/"+textFile, sents);
								}
							}
						}
						
					}
					System.out.println("[Info] Finishing "+fileType[f]+" dataset:"+newstype);
					//print these sentences. write to Files
					printConll(newstype,sents, fileType[f]);
				}
			}
		}
		
		
	}
	
	private static void processNameFile(String filePath, ArrayList<Sentence> sents) throws IOException{
		BufferedReader reader = RAWF.reader(filePath);
		String line = null;
		String prevEntity = "O";
		while((line = reader.readLine())!=null){
			if(line.startsWith("#end")) break;
			if(line.startsWith("#")) continue;
			String[] vals = line.split("\\s+");
			//first-step convert to dep structure
			if(vals.length!=13) throw new RuntimeException("split length not equal to 13:\n"+filePath+"\n"+line);
			if(line.equals("")){
				//finish a sentence
				prevEntity = "O";
			}
			String word = vals[2];
			String pos = vals[4]; 
			if(pos.equals("XX")) throw new RuntimeException("No POS tag:\n"+filePath);
			String parseBit = vals[5];
			
			WordToken wt = new WordToken(vals[2]);
			/***later check if all documents here have the entity**/
			
			
		}
		
		reader.close();
	}
	
	
	/**
	 * Write the sentence to files and save as conllx format
	 * @param datasetName: news type
	 * @param sents: the sentences read from original file
	 * @param fileType: train/test/development
	 * @throws IOException
	 */
	private static void printConll(String datasetName, ArrayList<Sentence> sents, String fileType) throws IOException{
		if(fileType.equals("development")) fileType = "dev";
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

package tmp.experiments;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.StringTokenizer;

public class Count {
	
	public static void main(String args[])throws IOException{
		
		Scanner scan = new Scanner(new File("data/GENIA/GENIAcorpus3.02.xml"));
		
		int k = 0;
		String line;
		while(scan.hasNextLine()){
			line = scan.nextLine().trim();
			
			String[] tokens = line.split("\\s");
			for(String token : tokens){
				if(token.indexOf("</sentence")!=-1){
					k++;
				}
			}
		}
		System.err.println(k);
		
	}

}

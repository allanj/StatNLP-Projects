package tmp.experiments;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.StringTokenizer;

import com.statnlp.commons.StringUtil;

public class Search {
	
	public static void main(String args[])throws IOException{
		
		Scanner scan = new Scanner(new File("data/GENIA/GENIAcorpus3.02.xml"));
		
//		int m = 0;
		int n = 0;
		int k = 0;
		String line;
		while(scan.hasNextLine()){
			line = scan.nextLine().trim();
			int bIndex = -1;
			int eIndex = -1;
			bIndex = line.indexOf("<sentence");
			eIndex = line.lastIndexOf("</sentence");
			if(bIndex!=-1 && eIndex!=-1){
				line = line.substring(bIndex, eIndex);
				if(line.indexOf(". ")!=-1 && line.indexOf(".")!=line.lastIndexOf(".") && line.indexOf("...")==-1 
						&& line.indexOf("1.")==-1 && line.indexOf("2.")==-1 && line.indexOf("2.")==-1){
					System.err.println(line);
					System.exit(1);
				}
			}
//			line = StringUtil.stripXMLTags(line);
//			m += line.split("\\s").length;
			if(line.startsWith("<sentence")){
				n++;
//				if(n>0.9*18364+44)
				{
					StringTokenizer st = new StringTokenizer(line,"<>");
					while(st.hasMoreTokens()){
						String token = st.nextToken();
						if(token.equals("/cons")){
							System.err.println(token);
							k++;
						}
					}
				}
			} else if(line.indexOf("<sentence")!=-1 && line.indexOf("<sentence")!=line.lastIndexOf("<sentence")){
				System.err.println(line);
				System.exit(1);
			}
		}
		System.err.println(k);
		System.err.println(n);
//		System.err.println(m);
		
	}

}

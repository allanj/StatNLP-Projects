package tmp.experiments;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

public class CheckGenia {
	
	public static void main(String args[])throws IOException{
		
		String filename1 = "data/GENIA/data/v3.02/mention-standard/FINE_TYPE/test.data";
		String filename2 = "data/GENIA/data/v3.02/mention-standard/FINE_TYPE/test-old.data";
		
		Scanner scan1, scan2;
		
		scan1 = new Scanner(new File(filename1));
		scan2 = new Scanner(new File(filename2));
		
		for(int k = 0; k<200; k++){
			String sent1 = scan1.nextLine();
			String sent2 = scan2.nextLine();
			String annotation1 = scan1.nextLine();
			String annotation2 = scan2.nextLine();
			
			String[] words1 = sent1.split("\\s");
			String[] words2 = sent2.split("\\s");
			
			String[] ants1 = annotation1.split("\\|");
			String[] ants2 = annotation2.split("\\|");

			String[] v1 = new String[ants1.length];
			for(int i=0; i<ants1.length; i++){
				if(ants1[i].length()==0) continue;
				String[] ant1 = ants1[i].split(",");
				int bIndex = Integer.parseInt(ant1[0]);
				int eIndex = Integer.parseInt(ant1[1]);
				
				StringBuilder sb = new StringBuilder();
				for(int index = bIndex; index<eIndex; index++){
					sb.append(" "+words1[index]);
				}
				v1[i] = sb.toString().trim();
			}
//			System.err.println("----------------");
			String[] v2 = new String[ants2.length];
			for(int i=0; i<ants2.length; i++){
				if(ants2[i].length()==0) continue;
				String[] ant2 = ants2[i].split(",");
				int bIndex = Integer.parseInt(ant2[0]);
				int eIndex = Integer.parseInt(ant2[1]);

				StringBuilder sb = new StringBuilder();
				for(int index = bIndex; index<eIndex; index++){
					sb.append(" "+words2[index]);
				}
				v2[i] = sb.toString().trim();
			}
//			System.err.println("================");
			
//			if(Arrays.equals(v1, v2)){
//				System.err.println("OK");
//			} else {
//				System.err.println("FAILED");
//				
//				System.err.println("L1:\t"+sent1);
//				System.err.println("L2:\t"+sent2);
//				
//				System.err.println(Arrays.toString(v1));
//				System.err.println(Arrays.toString(v2));
//			}
			
			scan1.nextLine();
			scan2.nextLine();
		}
		
	}

}

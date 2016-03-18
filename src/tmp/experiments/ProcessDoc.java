package tmp.experiments;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class ProcessDoc {
	
	public static void main(String args[]) throws FileNotFoundException{
		
		Scanner scan = new Scanner(new File("data/simple/a1a.train.txt.back"));
		
		String line;
		while(scan.hasNextLine()){
			line = scan.nextLine();
			int index = line.indexOf(" ");
			String pred = line.substring(0, index);
			String fs = line.substring(index+1);
			System.err.println(randomClass() +" "+fs);
		}
		
	}
	
	private static String randomClass(){
		double r = Math.random();
		
		if(r<0.2){
			return "A";
		} else if(r<0.4){
			return "B";
		} else if(r<0.6){
			return "C";
		} else if(r<0.8){
			return "D";
		} else {
			return "E";
		}
	}
	
}

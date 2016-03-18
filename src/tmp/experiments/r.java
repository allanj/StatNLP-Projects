package tmp.experiments;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

class r{
	public static void main(String args[]) throws FileNotFoundException{
		
		int c = 0;
		Scanner scan = new Scanner(new File("data/ACE2005/data/English/mention-standard/FINE_TYPE/test.data"));
		String line;
		while(scan.hasNextLine()){
			scan.nextLine();
			scan.nextLine();
			line = scan.nextLine();
			StringTokenizer st = new StringTokenizer(line, "|");
			HashSet<String> tokens = new HashSet<String>();
			while(st.hasMoreTokens()){
				String ts[] = st.nextToken().split("\\s");
				String v[] = ts[0].split(",");
				String t = ts[1];
				String token = v[0] + v[1] + t;
				tokens.add(token);
			}
			c += tokens.size();
			scan.nextLine();
		}
		
		System.err.println(c);

	}
}
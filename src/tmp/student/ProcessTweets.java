/** Statistical Natural Language Processing System
    Copyright (C) 2014  Lu, Wei

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package tmp.student;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Scanner;

/**
 * @author Wei Lu (luwei@statnlp.com)
 * 14 Nov, 2014 2014 8:22:04 pm
 */
public class ProcessTweets {
	
	public static void main(String args[]) throws FileNotFoundException{
		

		Random r = new Random(1234);
		
		File f = new File("/Users/wei_lu/Dropbox/Research/Work-in-progress/STATNLP/data/linear/twitter/all.data");
		
		Scanner scan = new Scanner(f);
		
		ArrayList<String> tokens = new ArrayList<String>();
		ArrayList<String> tags = new ArrayList<String>();
		
		ArrayList<Tweet> ts = new ArrayList<Tweet>();
		
		ArrayList<Tweet> ts_train = new ArrayList<Tweet>();
		ArrayList<Tweet> ts_dev = new ArrayList<Tweet>();
		ArrayList<Tweet> ts_test = new ArrayList<Tweet>();
		ArrayList<Tweet> ts_test2 = new ArrayList<Tweet>();
		
		while(scan.hasNextLine()){
			String line = scan.nextLine().trim();
			System.err.println(line);
			if(line.equals("")){
				Tweet t = new Tweet(tokens, tags);
				ts.add(t);
				tokens = new ArrayList<String>();
				tags = new ArrayList<String>();
				
				double v = r.nextDouble();
				if(v<=0.1){
					ts_dev.add(t);
				} else if(v <= 0.2){
					ts_test.add(t);
				} else if(v <= 0.8){
					ts_train.add(t);
				} else {
					ts_test2.add(t);
				}
				
			} else {
				String[] token_tag = line.split("\\s");
				tokens.add(token_tag[0]);
				tags.add(token_tag[1]);
			}
		}
		
		System.err.println(ts.size()+" tweets.");
		
		scan.close();
		
		

		File f_train, f_dev_in, f_dev_out, f_test_in, f_test_out, f_test2_in, f_test2_out;
		PrintWriter p_train, p_dev_in, p_dev_out, p_test_in, p_test_out, p_test2_in, p_test2_out;
		
		f_train = new File("/Users/wei_lu/Dropbox/Research/Work-in-progress/STATNLP/data/linear/twitter/project/train");
		p_train = new PrintWriter(f_train);
		
		f_dev_in = new File("/Users/wei_lu/Dropbox/Research/Work-in-progress/STATNLP/data/linear/twitter/project/dev.in");
		p_dev_in = new PrintWriter(f_dev_in);
		
		f_dev_out = new File("/Users/wei_lu/Dropbox/Research/Work-in-progress/STATNLP/data/linear/twitter/project/dev.out");
		p_dev_out = new PrintWriter(f_dev_out);
		
		f_test_in = new File("/Users/wei_lu/Dropbox/Research/Work-in-progress/STATNLP/data/linear/twitter/project/test.in");
		p_test_in = new PrintWriter(f_test_in);
		
		f_test_out = new File("/Users/wei_lu/Dropbox/Research/Work-in-progress/STATNLP/data/linear/twitter/project/test.out");
		p_test_out = new PrintWriter(f_test_out);
		
		f_test2_in = new File("/Users/wei_lu/Dropbox/Research/Work-in-progress/STATNLP/data/linear/twitter/project/test2.in");
		p_test2_in = new PrintWriter(f_test2_in);
		
		f_test2_out = new File("/Users/wei_lu/Dropbox/Research/Work-in-progress/STATNLP/data/linear/twitter/project/test2.out");
		p_test2_out = new PrintWriter(f_test2_out);
		
		Collections.shuffle(ts_train);
		Collections.shuffle(ts_dev);
		Collections.shuffle(ts_test);
		Collections.shuffle(ts_test2);
		
		System.err.println(ts_train.size());
		System.err.println(ts_dev.size());
		System.err.println(ts_test.size());
		System.err.println(ts_test2.size());
		
		for(Tweet t : ts_train){
			p_train.println(t.toOutFormat());
			p_train.flush();
		}
		for(Tweet t : ts_dev){
			p_dev_in.println(t.toInFormat());
			p_dev_in.flush();
			p_dev_out.println(t.toOutFormat());
			p_dev_out.flush();
		}
		for(Tweet t : ts_test){
			p_test_in.println(t.toInFormat());
			p_test_in.flush();
			p_test_out.println(t.toOutFormat());
			p_test_out.flush();
		}

		for(Tweet t : ts_test2){
			p_test2_in.println(t.toInFormat());
			p_test2_in.flush();
			p_test2_out.println(t.toOutFormat());
			p_test2_out.flush();
		}
		
		p_train.close();
		p_dev_in.close();
		p_test_in.close();
		p_test2_in.close();
		p_dev_out.close();
		p_test_out.close();
		p_test2_out.close();
	}

}

class Tweet{
	
	private ArrayList<String> tokens;
	private ArrayList<String> tags;
	
	public Tweet(ArrayList<String> tokens, ArrayList<String> tags){
		this.tokens = tokens;
		this.tags = tags;
	}
	
	public ArrayList<String> getTokens(){
		return this.tokens;
	}
	
	public ArrayList<String> getTags(){
		return this.tags;
	}
	
	public String toOutFormat(){
		StringBuilder sb = new StringBuilder();
		
		for(int k = 0; k<tokens.size(); k++){
			sb.append(tokens.get(k));
			sb.append('\t');
			sb.append(tags.get(k));
			sb.append('\n');
		}
		
		return sb.toString();
	}
	
	public String toInFormat(){
		StringBuilder sb = new StringBuilder();
		
		for(int k = 0; k<tokens.size(); k++){
			sb.append(tokens.get(k));
			sb.append('\n');
		}
		
		return sb.toString();
	}
	
}

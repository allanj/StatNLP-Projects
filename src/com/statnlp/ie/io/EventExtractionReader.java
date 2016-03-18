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
package com.statnlp.ie.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import com.statnlp.commons.AttributedWord;
import com.statnlp.commons.WordUtil;
import com.statnlp.ie.event.Event;
import com.statnlp.ie.event.EventRole;
import com.statnlp.ie.event.EventTemplate;
import com.statnlp.ie.types.IEManager;
import com.statnlp.ie.types.LabeledTextSpan;
import com.statnlp.ie.types.Mention;
import com.statnlp.ie.types.MentionType;
import com.statnlp.ie.types.UnlabeledTextSpan;

public class EventExtractionReader {
	
	private static ArrayList<String> tokenize(String line){
		ArrayList<String> results = new ArrayList<String>();
		int bIndex = 0;
		while(true){
			int eIndex = line.indexOf("|***|", bIndex);
			if(eIndex==-1){
				String token = line.substring(bIndex).trim();
				results.add(token);
				break;
			} else {
				String token = line.substring(bIndex, eIndex).trim();
				results.add(token);
				bIndex = eIndex+"|***|".length();
			}
		}
		return results;
	}
	
	public static ArrayList<LabeledTextSpan> readLabeledTextSpans_Event(IEManager manager, String filename) throws FileNotFoundException{
		ArrayList<LabeledTextSpan> spans = new ArrayList<LabeledTextSpan>();
		Scanner scan = new Scanner(new File(filename));
		while(scan.hasNextLine()){
			String line = scan.nextLine();
			if(line.startsWith("++++++")){
				while(!(line=scan.nextLine()).startsWith("--SENTENCE--"));
				String[] tokens = scan.nextLine().trim().split("\\s");
				AttributedWord[] words = new AttributedWord[tokens.length];
				for(int k = 0; k<tokens.length; k++)
					words[k] = new AttributedWord(tokens[k]);
				LabeledTextSpan span = new LabeledTextSpan(words);
				while(!(line=scan.nextLine()).startsWith("--EXPECTED EVENT--"));
				String[] eventNames = scan.nextLine().trim().split(":");
				Event event = manager.toEvent(eventNames[0], eventNames[1]);
				while(!(line=scan.nextLine()).startsWith("--GOLD ANNOTATIONS--"));
				int num_lines = Integer.parseInt(scan.nextLine());
				for(int k = 0; k<num_lines; k++){
					String[] info = scan.nextLine().split("\\s");
					String[] indices = info[0].split("\\,");
					String roleName = info[1].split("\\|")[0];
					EventRole role = manager.toEventRole(event, roleName);
					int bIndex = Integer.parseInt(indices[0]);
					int eIndex = Integer.parseInt(indices[1]);
					//note that we discard longer mentions.
					span.label(bIndex, eIndex, new Mention(bIndex, eIndex, bIndex, eIndex, role));
				}
				while(!(line=scan.nextLine().trim()).equals("0 POS"));
				ArrayList<String> pos_tags = tokenize(scan.nextLine().trim());
				if(words.length!=pos_tags.size())
					throw new RuntimeException("There are "+words.length+" words, but there are "+pos_tags.size()+" pos tags.");
				for(int k = 0; k<pos_tags.size(); k++)
					words[k].addAttribute("POS", pos_tags.get(k));
				
				for(int k = 0; k<words.length; k++){
					AttributedWord word = words[k];
					String curr = word.getName();
					
				}
				
				spans.add(span);
			}
		}
		return spans;
	}

	public static ArrayList<UnlabeledTextSpan> readUnlabeledTextSpans_Event(IEManager manager, String filename) throws FileNotFoundException{
		ArrayList<LabeledTextSpan> spans = readLabeledTextSpans_Event(manager, filename);
		ArrayList<UnlabeledTextSpan> results = new ArrayList<UnlabeledTextSpan>();
		for(LabeledTextSpan span : spans)
			results.add(span.removeLabels());
		return results;
	}
	
	public static ArrayList<LabeledTextSpan> readLabeledTextSpans_Mention(IEManager manager, String filename) throws FileNotFoundException{
		ArrayList<LabeledTextSpan> spans = new ArrayList<LabeledTextSpan>();
		Scanner scan = new Scanner(new File(filename));
		while(scan.hasNextLine()){
			String line = scan.nextLine();
			if(line.startsWith("++++++")){
				while(!(line=scan.nextLine()).startsWith("--SENTENCE--"));
				String[] tokens = scan.nextLine().trim().split("\\s");
				AttributedWord[] words = new AttributedWord[tokens.length];
				for(int k = 0; k<tokens.length; k++)
					words[k] = new AttributedWord(tokens[k]);
				LabeledTextSpan span = new LabeledTextSpan(words);
				while(!(line=scan.nextLine().trim()).equals("0 POS"));
				ArrayList<String> pos_tags = tokenize(scan.nextLine().trim());
//				if(words.length!=pos_tags.size())
//					throw new RuntimeException("There are "+words.length+" words, but there are "+pos_tags.size()+" pos tags.");
				for(int k = 0; k<pos_tags.size(); k++)
					words[k].addAttribute("POS", pos_tags.get(k));
				while(!(line=scan.nextLine()).startsWith("--LABELS--"));
				int num_lines = Integer.parseInt(scan.nextLine().split("\\s")[0]);
				for(int k = 0; k<num_lines; k++){
					String[] info = scan.nextLine().split("\\s");
					String[] indices = info[0].split("\\,");
					String type = info[1];
					MentionType tag = manager.toMentionType(type);
					int bIndex = Integer.parseInt(indices[0]);
					int eIndex = Integer.parseInt(indices[1]);
					//note that we discard longer mentions.
					span.label(bIndex, eIndex, new Mention(bIndex, eIndex, bIndex, eIndex, tag));
				}
				
				spans.add(span);
				span.expandAtt_POS();
			}
		}
		return spans;
	}
	
	public static ArrayList<UnlabeledTextSpan> readUnlabeledTextSpans_Mention(IEManager manager, String filename) throws FileNotFoundException{
		ArrayList<LabeledTextSpan> spans = readLabeledTextSpans_Mention(manager, filename);
		ArrayList<UnlabeledTextSpan> results = new ArrayList<UnlabeledTextSpan>();
		for(LabeledTextSpan span : spans)
			results.add(span.removeLabels());
		return results;
	}
	
	public static IEManager readIEManager(String filename) throws IOException{
		
		IEManager manager = new IEManager();
		
		Scanner scan = new Scanner(new File(filename));
		while(scan.hasNextLine()){
			String line = scan.nextLine();
			
			int bIndex = line.indexOf("{");
			int eIndex = line.lastIndexOf("}");
			String[] eventNames = line.substring(0, bIndex).trim().split(":");
			Event event = manager.toEvent(eventNames[0], eventNames[1]);
			
			String[] roles_with_mentions = line.substring(bIndex+1, eIndex).trim().split("\\s");
			EventRole[] roles = new EventRole[roles_with_mentions.length];
			
			for(int i = 0; i<roles.length; i++){
				String role_with_mentions = roles_with_mentions[i];
				bIndex = role_with_mentions.indexOf("[");
				eIndex = role_with_mentions.lastIndexOf("]");
				String roleName = role_with_mentions.substring(0, bIndex).trim();
				String[] mentions = role_with_mentions.substring(bIndex+1, eIndex).trim().split("\\|");
				MentionType[] types = new MentionType[mentions.length];
				for(int k = 0; k<mentions.length; k++)
					types[k] = manager.toMentionType(mentions[k]);
				roles[i] = manager.toEventRole(event, roleName);
				roles[i].setCompatibleTypes(types);
			}
			
			EventTemplate template = new EventTemplate(manager, event, roles);
			manager.addEventTemplate(template);
		}
		scan.close();
		
		manager.finalize();
		
		return manager;
		
	}
	
}
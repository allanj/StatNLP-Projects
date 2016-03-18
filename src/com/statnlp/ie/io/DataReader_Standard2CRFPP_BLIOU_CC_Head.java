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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import com.statnlp.commons.AttributedWord;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.ie.types.IEConfig;
import com.statnlp.ie.types.IEManager;
import com.statnlp.ie.types.LabeledTextSpan;
import com.statnlp.ie.types.Mention;
import com.statnlp.ie.types.SemanticTag;

public class DataReader_Standard2CRFPP_BLIOU_CC_Head {
	

	public static void main(String args[])throws IOException{
		
		for(String folder : new String[]{"ACE2004", "ACE2005"}){
			for(String subfolder : new String[]{"English"}){
				for(String set : new String[]{"train", "test", "dev"}){
					
					for(String overlap : new String[]{"cascaded_head"}){
						String filename_template = "data/"+folder+"/data/"+subfolder+"/template";
						
						NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
						
						IEManager manager = EventExtractionReader.readIEManager(filename_template);
						
						ArrayList<SemanticTag> all_tags = manager.getMentionTemplate().getAllTypesExcludingStartAndFinish();
						
						File f = new File("experiments/mention/lcrf-bliou/"+IEConfig._type.name()+"/"+folder+"/"+subfolder+"/"+overlap+"/");
						f.mkdirs();
						
						String filename_input = "data/"+folder+"/data/"+subfolder+"/mention-standard/"+IEConfig._type.name()+"/"+set+".data";
						
						for(int tag_index = 0; tag_index<=all_tags.size()*2; tag_index++){
							
							String filename_output = "experiments/mention/lcrf-bliou/"+IEConfig._type.name()+"/"+folder+"/"+subfolder+"/"+overlap+"/"+set+"-"+tag_index+".data";
							
							Scanner scan = new Scanner(new File(filename_input));
							PrintWriter p = new PrintWriter(new File(filename_output));
							
							process_cascaded(scan, p, manager, tag_index);
							
							scan.close();
							p.close();
							
						}
						
					}
					
				}
			}
		}
		
	}
	
	public static void process_cascaded(Scanner scan, PrintWriter p, IEManager manager, int tag_index) throws FileNotFoundException{
		
		int num_instances = 0;
		while(scan.hasNextLine()){
			String[] words = scan.nextLine().split("\\s");
			String[] tags = scan.nextLine().split("\\s");
			String annot = scan.nextLine();
			String[] annotations;
			if(!annot.equals("")){
				annotations = annot.split("\\|");
			} else {
				annotations = new String[0];
			}
			
			ArrayList<SemanticTag> all_tags = manager.getMentionTemplate().getAllTypesExcludingStartAndFinish();
			
			AttributedWord[] aws = new AttributedWord[words.length];
			for(int k = 0; k<words.length; k++){
				aws[k] = new AttributedWord(words[k]);
				aws[k].addAttribute("POS", tags[k]);
			}
			LabeledTextSpan span = new LabeledTextSpan(aws);
			for(int k = 0; k<annotations.length; k++){
				String[] annotation = annotations[k].split("\\s");
				String[] indices = annotation[0].split(",");
				int bIndex = Integer.parseInt(indices[0]);
				int eIndex = Integer.parseInt(indices[1]);
				int bIndex_head = Integer.parseInt(indices[2]);
				int eIndex_head = Integer.parseInt(indices[3]);
				String label = annotation[1];
				span.label(bIndex, eIndex, new Mention(bIndex, eIndex, bIndex_head, eIndex_head, manager.toMentionType(label)));
			}
			
			p.println(span.toCRFPP_format_withType_BLIOU_head(all_tags, tag_index));
			p.flush();
			
			if(words.length!=tags.length){
				throw new RuntimeException("The lengths between words and tags are not the same!");
			}
			
			scan.nextLine();
			num_instances++;
		}
		System.err.println("There are "+num_instances+" instances.");
		
	}
	
}
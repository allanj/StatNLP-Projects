package semie.types;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * The document reader.
 * @author luwei
 * @version 1.0
 */

public class DocumentReader{
	
	public static Manager readStandardFormat(String eventspan_name, String event_name) throws FileNotFoundException{
		Manager manager = new Manager();
		readEventTemplate(manager, event_name);
		readStandardFormat(manager, eventspan_name);
		
		ArrayList<EventSpan> allEventSpans = manager.getAllEventSpans();
		for(int i = 0; i<allEventSpans.size(); i++){
			EventSpan eventspan = allEventSpans.get(i);
			Lattice lattice = eventspan.getLattice();
			lattice.label_postprocess();
			lattice.map_phrases(manager);
		}
		
		return manager;
	}

	/**
	 * The standard format is as follows:<br>
	 * <code>
	 *  ++++++<br>
	 *  file_name<br>
	 *  --SENTENCE--<br>
	 *  word_1 word_2 ... word_n<br>
	 *  --EXPECTED EVENTS--<br>
	 *  event__type:event_subtype (name of the event)<br>
	 *  --GOLD ANNOTATIONS--<br>
	 *  num_lines_of_gold_annotations<br>
	 *  i,j argument_tag|//from most general to most specific<br>
	 *  ...<br>
	 *  --TAGS--<br>
	 *  num_lines_of_tags<br>
	 *  tag_type tag_name  (0: normal 1-line tag; >0: multiple-line, SRL-annotation style tags)<br>
	 *  tag_1 |***| tag_n<br>
	 *  --LABELS--<br>
	 *  num_lines_of_such_labels<br> 
	 *  label_name<br>
	 *  i,j label_tag<br>
	 *  ...<br>
	 *  </code>
	 * @throws FileNotFoundException 
	 */
	public static void readStandardFormat(Manager manager, String event_span_name) throws FileNotFoundException{
		
		Scanner scan = new Scanner(new File(event_span_name));
		
		while(scan.hasNextLine()){
			
			String line;
			line = scan.nextLine();
			if(!line.startsWith("++++++")){
				throw new RuntimeException("Invalid format:"+line+". Expecting ++++++.");
			}
			String file_name = scan.nextLine();
			
			// --SENTENCES--
			line = scan.nextLine(); 
			if(!line.startsWith("--SENTENCE--")){
				throw new RuntimeException("Invalid format:"+line+". Expecting --SENTENCE--.");
			}
			line = scan.nextLine(); //String sent = line;
			String[] words = line.split("\\s");
			Word[] word_ids = new Word[words.length];
			for(int i = 0; i< words.length; i++){
				word_ids[i] = manager.toWord(words[i]);
			}

			// --EXPECTED EVENT--
			line = scan.nextLine(); 
			if(!line.startsWith("--EXPECTED EVENT--")){
				throw new RuntimeException("Invalid format:"+line+". Expecting --EXPECTED EVENT--.");
			}
			line = scan.nextLine();
			String[] event_types = line.split(":");
			Event event = manager.getEvent(new String[]{event_types[0], event_types[1]});
			
			// --GOLD ANNOTATIONS--
			line = scan.nextLine();
			if(!line.startsWith("--GOLD ANNOTATIONS--")){
				throw new RuntimeException("Invalid format:"+line+". Expecting --GOLD ANNOTATIONS--.");
			}
			int num_gold_annotations = Integer.parseInt(scan.nextLine());
			EventAnnotation gold_annotation = new EventAnnotation();
			for(int i = 0; i <num_gold_annotations; i++){
				String[] parts = scan.nextLine().split("\\s");
				String[] interval = parts[0].split(",");
				String[] arg_names = parts[1].split("\\|");
				int bIndex = Integer.parseInt(interval[0]);
				int eIndex = Integer.parseInt(interval[1]);
				Role argument = manager.toRole(arg_names);
				gold_annotation.annotateInterval(bIndex, eIndex, argument);
			}
			
			EventSpan span = manager.toEventSpan(file_name, word_ids, gold_annotation);
			span.setEvent(event);
			
			String[] indices;
			
			// --SPAN--
			line = scan.nextLine(); 
			if(!line.startsWith("--SPAN--")){
				throw new RuntimeException("Invalid format:"+line+". Expecting --SPAN--.");
			}
			line = scan.nextLine(); 
			indices = line.split(",");
			int span_bIndex = Integer.parseInt(indices[0]);
			int span_eIndex = Integer.parseInt(indices[1]);
			span.setSpanIndices(new int[]{span_bIndex, span_eIndex});

			// --TRIGGER--
			line = scan.nextLine(); 
			if(!line.startsWith("--TRIGGER--")){
				throw new RuntimeException("Invalid format:"+line+". Expecting --TRIGGER--.");
			}
			line = scan.nextLine(); 
			indices = line.split(",");
			int trigger_bIndex = Integer.parseInt(indices[0]);
			int trigger_eIndex = Integer.parseInt(indices[1]);
			span.setTriggerIndices(new int[]{trigger_bIndex, trigger_eIndex});
			
			// --TAGS--
			line = scan.nextLine(); 
			if(!line.startsWith("--TAGS--")){
				throw new RuntimeException("Invalid format:"+line+". Expecting --TAGS--.");
			}
			line = scan.nextLine().trim();
			int num_tags = Integer.parseInt(line);
			int index;
			for(int i = 0; i<num_tags; i++){
				line = scan.nextLine().trim();
				index = line.indexOf(" ");
				int tag_type = Integer.parseInt(line.substring(0,index).trim());
				String tag_name = line.substring(index+1).trim();
				if(tag_type == 0){//these tags are assigned to each word
					line = scan.nextLine();
					Tag[][] tags =  toTags(line, manager, words.length);
					boolean added = span.addTags(tag_name, tags);
					if(!added){
						throw new RuntimeException("Length mismatch:"+line+"\n"+tags.length+"\n"+words.length);
					}
				} else {
					throw new RuntimeException("Invalid format:"+line+". Expecting 0");
				}
			}
			
			line = scan.nextLine(); 
			if(!line.startsWith("--LABELS--")){
				throw new RuntimeException("Invalid format:"+line+". Expecting --LABELS--.");
			}
			Lattice lattice = new Lattice(span);
			line = scan.nextLine();
			index = line.indexOf(" ");
			int num_labels = Integer.parseInt(line.substring(0,index).trim());
			@SuppressWarnings("unused")
			//currently not in use. assume type labels only...
			int label_name = manager.indexStr(line.substring(index+1).trim());
			for(int i = 0; i<num_labels; i++){
				String[] parts = scan.nextLine().split("\\s");
				String[] interval = parts[0].split(",");
				int bIndex = Integer.parseInt(interval[0]);
				int eIndex = Integer.parseInt(interval[1]);
				Type type = manager.toType(parts[1]);
				lattice.label(bIndex, eIndex, type);
			}
			span.setLattice(lattice);
//			lattice.label_postprocess();
//			lattice.map_phrases(manager);
		}
		
	}
	
	private static Tag[][] toTags(String line, Manager manager, int expected_num_tokens){
		ArrayList<String> tags_all = tokenize(line);
		if(tags_all.size()!=expected_num_tokens){
			System.err.println("expected:"+expected_num_tokens);
			System.err.println("found   :"+tags_all.size());
			System.err.println(line);
			System.exit(1);
		}
		Tag[][] res = new Tag[tags_all.size()][];
		for(int i = 0; i<tags_all.size(); i++){
			StringTokenizer st = new StringTokenizer(tags_all.get(i));
			ArrayList<String> tags_form1 = new ArrayList<String>();
			while(st.hasMoreTokens()){
				String tags_form_str = st.nextToken();
				if(!tags_form1.contains(tags_form_str) && !tags_form_str.equals("")){
					tags_form1.add(tags_form_str);
				}
			}
			Tag[] tags = new Tag[tags_form1.size()];
			for(int j =0; j<tags_form1.size(); j++){
				tags[j] = manager.toTag(tags_form1.get(j));
			}
			res[i] = tags;
		}
		return res;
	}
	
	private static ArrayList<String> tokenize(String line){
		ArrayList<String> arr = new ArrayList<String>();
		tokenize_helper(line, arr);
		return arr;
	}

	private static void tokenize_helper(String line, ArrayList<String> arr){
		int index = line.indexOf("|***|");
		if(index==-1){
			arr.add(line);
		} else {
			arr.add(line.substring(0, index).trim());
			tokenize_helper(line.substring(index+5).trim(), arr);
		}
	}
	
	public static Manager readACEBatch(String directory_name, String event_name) throws FileNotFoundException{
		Manager manager = new Manager();
		readEventTemplate(manager, event_name);
		File d = new File(directory_name);
		String[] file_names = d.list();
		Arrays.sort(file_names);
		for(String file_name : file_names){
//			System.err.println(file_name);
			if(file_name.endsWith("sgm")){
				String file_id = file_name.substring(0, file_name.length()-4);
				String doc_name = directory_name+"/"+file_name;
				String xml_name = directory_name+"/"+file_id+".apf.xml";
				read(doc_name, xml_name, event_name, manager);
			}
		}
		
		ArrayList<EventSpan> allEventSpans = manager.getAllEventSpans();
		for(int i = 0; i<allEventSpans.size(); i++){
			EventSpan eventspan = allEventSpans.get(i);
			Lattice lattice = eventspan.getLattice();
//			manager.indexLattice(lattice);
			lattice.label_postprocess();
			lattice.map_phrases(manager);
		}
		
//		System.exit(1);
		
		return manager;
	}
	
	public static void read(String doc_name, String xml_name, String event_name, Manager manager) throws FileNotFoundException{
		
		/*
		String doc_name = "/Users/luwei/corpora/ace/ace2005_normalized/nw/XIN_ENG_20030423.0011.sgm";
		String xml_name = "/Users/luwei/corpora/ace/ace2005_normalized/nw/XIN_ENG_20030423.0011.apf.xml";
		String event_name = "/Users/luwei/corpora/ace/ace2005_normalized/nw/ace.ontology-coarse.txt";
		*/
		
		Document doc = readACE(doc_name);
		
		ArrayList<Mention> mentions = annotateACE_mentions(xml_name);
		
		/*
		for(Mention mention : mentions){
			int bIndex = mention.getBIndex();
			int eIndex = mention.getEIndex();
			System.err.println(mention+"\t"+doc.getText().substring(bIndex, eIndex));
		}
		*/
		
		annotateACE_events(manager, mentions, doc, xml_name);
		
	}
	
	private static class Mention implements Comparable<Mention>{
		
		private int _bIndex;
		private int _eIndex;
		private String _type;
		
		public Mention(int bIndex, int eIndex, String type){
			this._bIndex = bIndex;
			this._eIndex = eIndex;
			this._type = type;
		}
		
		public int getBIndex(){
			return this._bIndex;
		}
		
		public int getEIndex(){
			return this._eIndex;
		}
		
		public String getType(){
			return this._type;
		}
		
		public int compareTo(Mention m){
			if(this._bIndex != m._bIndex){
				return this._bIndex - m._bIndex;
			}
			if(this._eIndex != m._eIndex){
				return this._eIndex - m._eIndex;
			}
			return 0;
		}
		
		public boolean equals(Object o){
			if(o instanceof Mention){
				Mention m = (Mention)o;
				if(this.compareTo(m)!=0){
					return false;
				}
				if(this._type.equals(m._type)){
					return true;
				}
				return false;
			}
			return false;
		}
		
		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("["+this._bIndex+","+this._eIndex+")"+this._type);
			return sb.toString();
		}
		
	}
	
	/**
	 * Example event template:
	 * <code>
	 * Business:End-Org { Org[ORG] Place[FAC|GPE] Time-At-Beginning[TME] }<br>
	 * </code>
	 */
	public static void readEventTemplate(Manager manager, String event_struct)throws FileNotFoundException{
		
		Scanner scan = new Scanner(new File(event_struct));
		
		while(scan.hasNextLine()){
			String line = scan.nextLine();
			int bIndex = line.indexOf("{");
			int eIndex = line.indexOf("}");
			
			String event_name = line.substring(0, bIndex).trim();
			String event_types[] = event_name.split(":");
			String arguments = line.substring(bIndex+1, eIndex).trim();
			StringTokenizer st = new StringTokenizer(arguments);
			Role[] args = new Role[st.countTokens()];
			int k = 0;
			while(st.hasMoreTokens()){
				String argument = st.nextToken();
				bIndex = argument.indexOf("[");
				eIndex = argument.indexOf("]");
				String arg_name = argument.substring(0,bIndex);
				//String[] arg_names = arg_name.split(":");//separately by :
				String[] arg_names = new String[]{arg_name, arg_name+"_IN_"+event_name};
				String type_names = argument.substring(bIndex+1, eIndex);
				StringTokenizer st1 = new StringTokenizer(type_names,"|");
				Type[] types = new Type[st1.countTokens()];
				int t = 0;
				while(st1.hasMoreTokens()){
					Type type = manager.toType(st1.nextToken());
					types[t++] = type;
				}
				Role arg = manager.toRole(arg_names);
				arg.setTypes(types);
//				System.err.println(arg+"\t"+arg.getTypes());
				args[k++] = arg;
			}
			manager.toEvent(event_types, args);//for now, forget about the triggers...
		}
	}
	
	private static int[] getOffsets(String line){

		int bIndex_start, eIndex_start;
		int bIndex_end, eIndex_end;

		bIndex_start = line.indexOf("START=\"");
		eIndex_start = line.indexOf("\"", bIndex_start+7);
		bIndex_end = line.indexOf("END=\"");
		eIndex_end = line.indexOf("\"", bIndex_end+5);
		
		int startOffset = Integer.parseInt(line.substring(bIndex_start+7, eIndex_start));
		int endOffset = Integer.parseInt(line.substring(bIndex_end+5, eIndex_end))+1;
		
		return new int[]{startOffset, endOffset};
	}
	
	private static int countTokens(String line){
		StringTokenizer st = new StringTokenizer(line.trim());
		return st.countTokens();
	}
	
	public static void annotateACE_events(Manager manager, ArrayList<Mention> allMentions, Document document, String xml_name)throws FileNotFoundException{
		
		Scanner xml_scan = new Scanner(new File(xml_name), "UTF-8");
		
		while(xml_scan.hasNextLine()){
			String line = xml_scan.nextLine().trim();
			if(line.startsWith("<event ID")){
				int bIndex_type;
				int eIndex_type;

				bIndex_type = line.indexOf(" TYPE=\"");
				eIndex_type = line.indexOf("\"",bIndex_type+7);
				String type = line.substring(bIndex_type+7, eIndex_type);
				
				bIndex_type = line.indexOf("SUBTYPE=\"");
				eIndex_type = line.indexOf("\"",bIndex_type+9);
				String subtype = line.substring(bIndex_type+9, eIndex_type);
				
				Event event = manager.getEvent(new String[]{type, subtype});
				
				while(!(line=xml_scan.nextLine().trim()).startsWith("</event>")){
					if(line.startsWith("<event_mention ID")){
						//one modification: we consider event span at sentence level
						//thus, i changed to read ldc_scope instead
						StringBuffer sb;
						
						sb = new StringBuffer();
						while(!(line=xml_scan.nextLine().trim()).startsWith("<extent>")){
							//do nothing...
						}
						while(!(line=xml_scan.nextLine().trim()).startsWith("</extent>")){
							sb.append(line);
						}
						int[] offsets_extent = getOffsets(sb.toString());
						
						sb = new StringBuffer();
						while(!(line=xml_scan.nextLine().trim()).startsWith("<ldc_scope>")){
							//do nothing...
						}
						while(!(line=xml_scan.nextLine().trim()).startsWith("</ldc_scope>")){
							sb.append(line);
						}
						int[] offsets_sentence = getOffsets(sb.toString());
						
						sb = new StringBuffer();
						while(!(line=xml_scan.nextLine().trim()).startsWith("<anchor>")){
							//do nothing...
						}
						while(!(line=xml_scan.nextLine().trim()).startsWith("</anchor>")){
							sb.append(line);
						}
						int[] offsets_anchor = getOffsets(sb.toString());
						
						String sentence = document.getText().substring(offsets_sentence[0], offsets_sentence[1]);
						
						int extent_bIndex = countTokens(document.getText().substring(offsets_sentence[0], offsets_extent[0]));
						int extent_eIndex = countTokens(document.getText().substring(offsets_sentence[0], offsets_extent[1]));
						if(extent_bIndex == extent_eIndex){
							extent_bIndex -= 1;
						}
						
						int anchor_bIndex = countTokens(document.getText().substring(offsets_sentence[0], offsets_anchor[0]));
						int anchor_eIndex = countTokens(document.getText().substring(offsets_sentence[0], offsets_anchor[1]));
						if(anchor_bIndex == anchor_eIndex){
							anchor_bIndex -= 1;
						}
						
						StringTokenizer st = new StringTokenizer(sentence);
						
						Word[] words = new Word[st.countTokens()];
						int k = 0;
						while(st.hasMoreTokens()){
							String token = st.nextToken();
							Word word = manager.toWord(token);
							words[k]=word;
							k++;
						}
						
						//now, arguments..
						//<event_mention_argument
						StringBuffer sb_gold_info = new StringBuffer();
						int num_gold_info = 0;
						
						while(!(line=xml_scan.nextLine().trim()).startsWith("</event_mention>"))
						{
							if(line.startsWith("<event_mention_argument")){
								int bIndex = line.indexOf("ROLE=\"");
								int eIndex = line.indexOf("\"", bIndex+6);
								String role = line.substring(bIndex+6, eIndex);
								
								line = xml_scan.nextLine();//extent
								line = xml_scan.nextLine();//charseq
								
								int bIndex_start_arg = line.indexOf("START=\"");
								int eIndex_start_arg = line.indexOf("\"", bIndex_start_arg+7);
								int startIndex_arg = Integer.parseInt(line.substring(bIndex_start_arg+7, eIndex_start_arg));

								int bIndex_end_arg = line.indexOf("END=\"");
								int eIndex_end_arg = line.indexOf("\"", bIndex_end_arg+5);
								int endIndex_arg = Integer.parseInt(line.substring(bIndex_end_arg+5, eIndex_end_arg))+1;
								
								String s1 = document.getText().substring(offsets_sentence[0], startIndex_arg);
								String s2 = document.getText().substring(offsets_sentence[0], endIndex_arg);
								
								StringTokenizer st1 = new StringTokenizer(s1);
								StringTokenizer st2 = new StringTokenizer(s2);
								if(st1.countTokens() != st2.countTokens()){
									sb_gold_info.append(st1.countTokens()+","+st2.countTokens()+" "+role+"|"+role+"_IN_"+type+":"+subtype+"\n");
								} else {
									sb_gold_info.append((st1.countTokens()-1)+","+st2.countTokens()+" "+role+"|"+role+"_IN_"+type+":"+subtype+"\n");
								}
								num_gold_info++;
							}
						}
						
						@SuppressWarnings("unused")
						int gold_information = manager.indexStr((num_gold_info+"\n"+sb_gold_info.toString().trim()).trim()+"\n--SPAN--\n"+extent_bIndex+","+extent_eIndex+"\n--TRIGGER--\n"+anchor_bIndex+","+anchor_eIndex);
						EventSpan span = manager.toEventSpan(xml_name, words, null);
						span.setEvent(event);
						
						Lattice lattice = new Lattice(span);
						for(int i = 0; i<allMentions.size(); i++){
							Mention mention = allMentions.get(i);
							int bIndex = mention.getBIndex();
							int eIndex = mention.getEIndex();
							Type t = manager.toType(mention.getType());
							
							if(offsets_sentence[0] <= bIndex && eIndex <= offsets_sentence[1]){
								String s1 = document.getText().substring(offsets_sentence[0], bIndex);
								String s2 = document.getText().substring(offsets_sentence[0], eIndex);
								StringTokenizer st1 = new StringTokenizer(s1);
								StringTokenizer st2 = new StringTokenizer(s2);
								if(st1.countTokens() != st2.countTokens()){
									lattice.label(st1.countTokens(), st2.countTokens(), t);
								} else {
									lattice.label(st1.countTokens()-1, st2.countTokens(), t);
								}
							}
						}
						span.setLattice(lattice);
					}
				}
			}
		}
		
	}
	
	public static ArrayList<Mention> annotateACE_mentions(String xml_name)throws FileNotFoundException{
		
		ArrayList<Mention> mentions = new ArrayList<Mention>();
		
		Scanner xml_scan = new Scanner(new File(xml_name), "UTF-8");
		
		while(xml_scan.hasNextLine()){
			String line = xml_scan.nextLine().trim();
			if(line.startsWith("<entity ID")){
				int bIndex_type = line.indexOf("TYPE=\"");
				int eIndex_type = line.indexOf("\"",bIndex_type+6);
				String type = line.substring(bIndex_type+6, eIndex_type);
				
				while(!(line=xml_scan.nextLine().trim()).startsWith("</entity>")){
					if(line.startsWith("<extent>")){
						line = xml_scan.nextLine();
						int bIndex_start = line.indexOf("START=\"");
						int eIndex_start = line.indexOf("\"", bIndex_start+7);
						int startIndex = Integer.parseInt(line.substring(bIndex_start+7, eIndex_start));

						int bIndex_end = line.indexOf("END=\"");
						int eIndex_end = line.indexOf("\"", bIndex_end+5);
						int endIndex = Integer.parseInt(line.substring(bIndex_end+5, eIndex_end))+1;
						
						Mention mention = new Mention(startIndex, endIndex, type);
//						if(xml_name.equals("corpora/ace/ace2005_normalized/nw//AFP_ENG_20030413.0098.apf.xml") && startIndex == 1752){
//							System.err.println(mention);
//						}
						int idx = Collections.binarySearch(mentions, mention);
						if(idx>=0){
							Mention mention_old = mentions.get(idx);
							if(mention_old.equals(mention)){
								System.err.println("[WARN]you have duplicated annotations --- ignored!!"+mention_old+"\t"+mention+"\t"+idx);
							} else {
								System.err.println("[WARN]you have inconsistent annotatins?!"+mention_old+"\t"+mention+"\t"+idx);
								mentions.add(idx, mention);
							}
						} else {
							mentions.add(-1-idx, mention);
//							mentions.add(mention);
						}
					}
				}
			}
			if(line.startsWith("<value ID")){
				int bIndex_type = line.indexOf("TYPE=\"");
				int eIndex_type = line.indexOf("\"",bIndex_type+6);
				String type = line.substring(bIndex_type+6, eIndex_type);
				
//				if(type.equals("Numeric")){
//					System.err.println(line);
//					System.exit(1);
//				}
				
				while(!(line=xml_scan.nextLine().trim()).startsWith("</value>")){
					if(line.startsWith("<extent>")){
						line = xml_scan.nextLine();
						int bIndex_start = line.indexOf("START=\"");
						int eIndex_start = line.indexOf("\"", bIndex_start+7);
						int startIndex = Integer.parseInt(line.substring(bIndex_start+7, eIndex_start));

						int bIndex_end = line.indexOf("END=\"");
						int eIndex_end = line.indexOf("\"", bIndex_end+5);
						int endIndex = Integer.parseInt(line.substring(bIndex_end+5, eIndex_end))+1;
						
						Mention mention = new Mention(startIndex, endIndex, type);
						int idx = Collections.binarySearch(mentions, mention);
						if(idx>=0){
							Mention mention_old = mentions.get(idx);
							if(mention_old.equals(mention)){
								System.err.println("[WARN]you have duplicated annotations --- ignored!!"+mention_old+"\t"+mention+"\t"+idx);
							} else {
								System.err.println("[WARN]you have inconsistent annotatins?!"+mention_old+"\t"+mention);
								mentions.add(idx, mention);
							}
						} else {
							mentions.add(-1-idx, mention);
//							mentions.add(mention);
						}
					}
				}
			}
			if(line.startsWith("<timex2 ID")){
				String type = "TME";
				
				while(!(line=xml_scan.nextLine().trim()).startsWith("</timex2>")){
					if(line.startsWith("<extent>")){
						line = xml_scan.nextLine();
						int bIndex_start = line.indexOf("START=\"");
						int eIndex_start = line.indexOf("\"", bIndex_start+7);
						int startIndex = Integer.parseInt(line.substring(bIndex_start+7, eIndex_start));

						int bIndex_end = line.indexOf("END=\"");
						int eIndex_end = line.indexOf("\"", bIndex_end+5);
						int endIndex = Integer.parseInt(line.substring(bIndex_end+5, eIndex_end))+1;
						
						Mention mention = new Mention(startIndex, endIndex, type);
						int idx = Collections.binarySearch(mentions, mention);
						if(idx>=0){
							Mention mention_old = mentions.get(idx);
							if(mention_old.equals(mention)){
								System.err.println("[WARN]you have duplicated annotations --- ignored!!"+mention_old+"\t"+mention+"\t"+idx);
							} else {
								System.err.println("[WARN]you have inconsistent annotatins?!"+mention_old+"\t"+mention);
								mentions.add(idx, mention);
							}
						} else {
							mentions.add(-1-idx, mention);
//							mentions.add(mention);
						}
					}
				}
			}
		}
		
		return mentions;
	}
	
	public static Document readACE(String sgm_name) throws FileNotFoundException{
		Scanner sgm_scan = new Scanner(new File(sgm_name), "UTF-8");
		
		String docid=null;
		String text=null;

		StringBuffer sb = new StringBuffer();
		while(sgm_scan.hasNextLine()){
			String line = sgm_scan.nextLine();
			if(line.startsWith("<DOCID>") && line.endsWith("</DOCID>")){
				int bIndex = line.indexOf(">");
				int eIndex = line.lastIndexOf("<");
				docid = line.substring(bIndex+1, eIndex).trim();
			}
			
			char[] chs = line.toCharArray();
			boolean in = true;
			for(int i = 0; i<chs.length; i++){
				if(chs[i]=='<'){
					in = false;
				}else if(chs[i]=='>'){
					in = true;
					continue;
				}
				if(in){
					sb.append(chs[i]);
				}
			}
			sb.append('\n');
		}
		text = sb.toString();
		
		return new ACEDocument(docid, text);
	}
	
}

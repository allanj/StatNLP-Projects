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
package com.statnlp.ie.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import com.statnlp.commons.AttributedWord;
import com.statnlp.commons.types.Segment;
import com.statnlp.commons.types.TextSpan;

public class LabeledTextSpan extends TextSpan{
	
	private static final long serialVersionUID = 3611022441919260189L;
	
	public HashMap<Segment, ArrayList<Mention>> _map_supported;
	//the mentions which are ignored due to certain constraints, such as the max length of a mention.
	public HashMap<Segment, ArrayList<Mention>> _map_not_supported;
	
//	private ArrayList<Segment> _all_segs;
	private ArrayList<Mention> _all_mentions;
	
	public LabeledTextSpan(AttributedWord[] words) {
		super(words);
		this._map_supported = new HashMap<Segment, ArrayList<Mention>>();
		this._map_not_supported = new HashMap<Segment, ArrayList<Mention>>();
	}
	
	public ArrayList<Mention> getAllMentions(){
		if(this._all_mentions != null){
			return this._all_mentions;
		}
		
		this._all_mentions = new ArrayList<Mention>();
		
		Iterator<Segment> segs;
		
		segs = this._map_supported.keySet().iterator();
		while(segs.hasNext()){
			Segment seg = segs.next();
			this._all_mentions.addAll(this._map_supported.get(seg));
		}
		segs = this._map_not_supported.keySet().iterator();
		while(segs.hasNext()){
			Segment seg = segs.next();
			this._all_mentions.addAll(this._map_not_supported.get(seg));
		}
		
		Collections.sort(this._all_mentions);
		
		return this._all_mentions;
	}
	
	public int countNested(){
		int num_overlap = 0;
		ArrayList<Segment> segments = this.getAllSegments();
		for(int i = 0; i<segments.size(); i++){
			Segment segment1 = segments.get(i);
			for(int j = 0; j<segments.size(); j++){
				if(i==j)
					continue;
				Segment segment2 = segments.get(j);
				if(segment1.nestedWith(segment2)){
					num_overlap++;
					break;
				}
			}
		}
		return num_overlap;
	}
	
	public int countOverlappingSegments(){
		int num_overlap = 0;
		ArrayList<Segment> segments = this.getAllSegments();
		for(int i = 0; i<segments.size(); i++){
			Segment segment1 = segments.get(i);
			for(int j = 0; j<segments.size(); j++){
				if(i==j)
					continue;
				Segment segment2 = segments.get(j);
				if(!segment1.noOverlapWith(segment2)){
					num_overlap++;
					break;
				}
				if(segment1.overlapsWith(segment2) && segment1.noOverlapWith(segment2)){
					throw new RuntimeException(segment1+"\t"+segment2);
				}
			}
		}
		return num_overlap;
	}
	
	public int countAllSegments(){
		return this.getAllSegments().size();
	}
	
	public ArrayList<Segment> getAllSegments(){
//		if(this._all_segs!=null)
//			return this._all_segs;
		
		ArrayList<Segment> segments = new ArrayList<Segment>();
		Iterator<Segment> seg_keys;
		
		seg_keys = this._map_supported.keySet().iterator();
		while(seg_keys.hasNext()){
			Segment seg = seg_keys.next();
			segments.add(seg);
		}
		seg_keys = this._map_not_supported.keySet().iterator();
		while(seg_keys.hasNext()){
			Segment seg = seg_keys.next();
			segments.add(seg);
		}
		Collections.sort(segments);
//		this._all_segs = segments;
		return segments;
	}
	
	public int countAllLabels(){
		return this.countAllSupportedLabels() + this.countAllNotSupportedLabels();
	}
	
	public int countAllSupportedLabels(){
		int size = 0;
		Iterator<Segment> segs = this._map_supported.keySet().iterator();
		while(segs.hasNext()){
			Segment seg = segs.next();
			size += this._map_supported.get(seg).size();
		}
		return size;
	}
	
	public int countAllNotSupportedLabels(){
		int size = 0;
		Iterator<Segment> segs = this._map_not_supported.keySet().iterator();
		while(segs.hasNext()){
			Segment seg = segs.next();
			size += this._map_not_supported.get(seg).size();
		}
		return size;
	}
	

	public String toStandardFormat_noPOS(){
		StringBuilder sb = new StringBuilder();
		
		for(int k = 0; k<this.length(); k++){
			AttributedWord word = this.getWord(k);
			if(k!=0)
				sb.append(' ');
			sb.append(word.getName());
		}
		sb.append('\n');
//		for(int k = 0; k<this.length(); k++){
//			AttributedWord word = this.getWord(k);
//			if(k!=0)
//				sb.append(' ');
//			sb.append(word.getAttribute("POS").get(0));
//		}
//		sb.append('\n');
		
		ArrayList<Segment> segs = this.getAllSegments();
		boolean start = true;
		for(int k = 0; k<segs.size(); k++){
			Segment seg = segs.get(k);
			if(this._map_supported.containsKey(seg)){
				ArrayList<Mention> mentions = this._map_supported.get(seg);
				for(Mention mention : mentions){
					if(start){
						start = false;
					} else {
						sb.append("|");
					}
					sb.append(seg.getBIndex()+","+seg.getEIndex()+","+mention.getHeadSegment().getBIndex()+","+mention.getHeadSegment().getEIndex()+" "+mention.getSemanticTag().getName());
				}
			}
			else if(this._map_not_supported.containsKey(seg)){
				ArrayList<Mention> mentions = this._map_not_supported.get(seg);
				for(Mention mention : mentions){
					if(start){
						start = false;
					} else {
						sb.append("|");
					}
					sb.append(seg.getBIndex()+","+seg.getEIndex()+","+mention.getHeadSegment().getBIndex()+","+mention.getHeadSegment().getEIndex()+" "+mention.getSemanticTag().getName());
//					sb.append(seg.getBIndex()+","+seg.getEIndex()+" "+mention.getTag().getNames()[0]);
				}
			}
		}
		sb.append('\n');
		
		return sb.toString();
		
	}
	
	public String toStandardFormat(){
		StringBuilder sb = new StringBuilder();
		
		for(int k = 0; k<this.length(); k++){
			AttributedWord word = this.getWord(k);
			if(k!=0)
				sb.append(' ');
			sb.append(word.getName());
		}
		sb.append('\n');
		for(int k = 0; k<this.length(); k++){
			AttributedWord word = this.getWord(k);
			if(k!=0)
				sb.append(' ');
			sb.append(word.getAttribute("POS").get(0));
		}
		sb.append('\n');
		
		ArrayList<Segment> segs = this.getAllSegments();
		boolean start = true;
		for(int k = 0; k<segs.size(); k++){
			Segment seg = segs.get(k);
			if(this._map_supported.containsKey(seg)){
				ArrayList<Mention> mentions = this._map_supported.get(seg);
				for(Mention mention : mentions){
					if(start){
						start = false;
					} else {
						sb.append("|");
					}
					sb.append(seg.getBIndex()+","+seg.getEIndex()+","+mention.getHeadSegment().getBIndex()+","+mention.getHeadSegment().getEIndex()+" "+mention.getSemanticTag().getName());
				}
			}
			else if(this._map_not_supported.containsKey(seg)){
				ArrayList<Mention> mentions = this._map_not_supported.get(seg);
				for(Mention mention : mentions){
					if(start){
						start = false;
					} else {
						sb.append("|");
					}
					sb.append(seg.getBIndex()+","+seg.getEIndex()+","+mention.getHeadSegment().getBIndex()+","+mention.getHeadSegment().getEIndex()+" "+mention.getSemanticTag().getName());
//					sb.append(seg.getBIndex()+","+seg.getEIndex()+" "+mention.getTag().getNames()[0]);
				}
			}
		}
		sb.append('\n');
		
		return sb.toString();
		
	}
	
	public class IFeature implements Comparable<IFeature>{
		private String _form;
		private int _id;
		public IFeature(String form){
			this._form = form;
		}
		@Override
		public boolean equals(Object o){
			if(o instanceof IFeature){
				IFeature f = (IFeature)o;
				return this._form.equals(f._form);
			}
			return false;
		}
		@Override
		public int hashCode(){
			return this._form.hashCode();
		}
		@Override
		public int compareTo(IFeature f) {
			return this._id - f._id;
		}
		public void setId(int id){
			this._id = id;
		}
		public int getId(){
			return this._id;
		}
	}
	
	private IFeature toFeature(String s){
		if(featureMap.containsKey(s)){
			return featureMap.get(s);
		}
		IFeature f = new IFeature(s);
		f.setId(featureMap.size()+1);
		featureMap.put(s, f);
		return f;
	}
	
	private void addFeature(String s, HashMap<IFeature, Integer> features){
		IFeature f = this.toFeature(s);
		
		if(features.containsKey(f)){
			int oldCount = features.get(f);
			features.put(f, oldCount+1);
		} else {
			features.put(f, 1);
		}
		
	}
	
	public String toFeatureLine(int bIndex, int eIndex){
		
		HashMap<IFeature, Integer> features = new HashMap<IFeature, Integer>();
		
		StringBuilder sb = new StringBuilder();
		for(int index = bIndex; index<eIndex; index++){
			
			AttributedWord word = this.getWord(index);
			Iterator<String> atts = word.getAttributes().iterator();
			while(atts.hasNext()){
				String att_name = atts.next();
				String att_value = word.getAttribute(att_name).get(0);
				
				String f = att_name+":"+att_value;
				this.addFeature(f, features);
			}
			
		}
		
		ArrayList<IFeature> fs = new ArrayList<IFeature>();
		Iterator<IFeature> f_keys = features.keySet().iterator();
		while(f_keys.hasNext()){
			fs.add(f_keys.next());
		}
		Collections.sort(fs);
		
		for(int k = 0; k<fs.size(); k++){
			IFeature f = fs.get(k);
			if(k!=0)
				sb.append(' ');
			sb.append(f.getId()+":"+features.get(f));
		}
		
		return sb.toString();
		
	}
	
	private static HashMap<String, IFeature> featureMap;
	
	public static void resetSVMFeatureCache(){
		featureMap = new HashMap<String, IFeature>();
	}
	
	public String toCRFPP_format_withType_BLIOU_withOverlappings(){
		
		ArrayList<Mention> segs = this.toTaggedSegments_sorted_withOverlappings();
		
		StringBuilder sb = new StringBuilder();
		
		for(int k = 0; k<this.length(); k++){
			AttributedWord word = this.getWord(k);
			sb.append(word.getName());
			sb.append('\t');
			sb.append(word.getAttribute("POS").get(0));
			sb.append('\t');
			ArrayList<String> tags = new ArrayList<String>();
			
			for(int i = 0; i<segs.size(); i++){
				Mention ts = segs.get(i);
				if(k==ts.getSegment().getBIndex()){
					if(ts.length()==1){
						tags.add("U-"+ts.getSemanticTag().getName());
					} else {
						tags.add("B-"+ts.getSemanticTag().getName());
					}
				} else if(k>ts.getSegment().getBIndex() && k<ts.getSegment().getEIndex()){
					tags.add("I-"+ts.getSemanticTag().getName());
				}
			}
			Collections.sort(tags);
			if(tags.size()==0){
				sb.append("O");
			} else {
				for(int i = 0; i<tags.size(); i++){
					if(i!=0) sb.append("+");
					sb.append(tags.get(i));
				}
			}
			sb.append('\n');
		}
		
		return sb.toString();
		
	}
	
	public String toCRFPP_format_withType_withOverlappings(){
		
		ArrayList<Mention> segs = this.toTaggedSegments_sorted_withOverlappings();
		
		StringBuilder sb = new StringBuilder();
		
		for(int k = 0; k<this.length(); k++){
			AttributedWord word = this.getWord(k);
			sb.append(word.getName());
			sb.append('\t');
			sb.append(word.getAttribute("POS").get(0));
			sb.append('\t');
			ArrayList<String> tags = new ArrayList<String>();
			
			for(int i = 0; i<segs.size(); i++){
				Mention ts = segs.get(i);
				if(k==ts.getSegment().getBIndex()){
					tags.add("B-"+ts.getSemanticTag().getName());
				} else if(k>ts.getSegment().getBIndex() && k<ts.getSegment().getEIndex()){
					tags.add("I-"+ts.getSemanticTag().getName());
				}
			}
			Collections.sort(tags);
			if(tags.size()==0){
				sb.append("O");
			} else {
				for(int i = 0; i<tags.size(); i++){
					if(i!=0) sb.append("+");
					sb.append(tags.get(i));
				}
			}
			sb.append('\n');
		}
		
		return sb.toString();
		
	}

	public String toCRFPP_format_withType_BLIOU(){
		
		ArrayList<Mention> segs = this.toTaggedSegments_sorted_all_no_overlap();
		StringBuilder sb = new StringBuilder();
		
		for(int k = 0; k<this.length(); k++){
			AttributedWord word = this.getWord(k);
			sb.append(word.getName());
			sb.append('\t');
			sb.append(word.getAttribute("POS").get(0));
			sb.append('\t');
			boolean partOfSeg = false;
			for(int i = 0; i<segs.size(); i++){
				Mention ts = segs.get(i);
				if(k==ts.getSegment().getBIndex()){
					if(ts.length()==1){
						sb.append("U-"+ts.getSemanticTag().getName());
						partOfSeg = true;
						break;
					} else {
						sb.append("B-"+ts.getSemanticTag().getName());
						partOfSeg = true;
						break;
					}
				} else if(k==ts.getSegment().getEIndex()-1){
					sb.append("L-"+ts.getSemanticTag().getName());
					partOfSeg = true;
					break;
				} else if(k > ts.getSegment().getBIndex() && k<ts.getSegment().getEIndex()-1){
					sb.append("I-"+ts.getSemanticTag().getName());
					partOfSeg = true;
					break;
				}
			}
			if(!partOfSeg){
				sb.append("O");
			}
			sb.append('\n');
		}
		
		return sb.toString();
		
	}

	public String toCRFPP_format_withType_BLIOU_head(ArrayList<SemanticTag> tags, int tag_index){
		
		ArrayList<Mention> mentions = this.toMentions_sorted_withOverlappings();
		
		StringBuilder sb = new StringBuilder();
		
		for(int k = 0; k<this.length(); k++){
			AttributedWord word = this.getWord(k);
			sb.append(word.getName());
			sb.append('\t');
			sb.append(word.getAttribute("POS").get(0));
			sb.append('\t');
			
			for(int j = 0; j<(tag_index+1)/2; j++){
				SemanticTag tag = tags.get(j);
				
				boolean partOfSeg = false;
				boolean partOfSeg_head = false;
				for(int i = 0; i<mentions.size(); i++){
					Mention mention = mentions.get(i);
					Segment seg = mention.getSegment();
					Segment seg_head = mention.getHeadSegment();
					
					if(!mention.getSemanticTag().equals(tag)){
						continue;
					}
					
//					if(tag_index%2==0)
					{
						if(k==seg.getBIndex()){
							if(mention.length()==1){
								sb.append("U-"+mention.getSemanticTag().getName());
								partOfSeg = true;
							} else {
								sb.append("B-"+mention.getSemanticTag().getName());
								partOfSeg = true;
							}
						} else if(k==seg.getEIndex()-1){
							sb.append("L-"+mention.getSemanticTag().getName());
							partOfSeg = true;
						} else if(k > seg.getBIndex() && k<seg.getEIndex()-1){
							sb.append("I-"+mention.getSemanticTag().getName());
							partOfSeg = true;
						}
					}
					
//					if(tag_index%2==1 && j == tag_index-1)
					if(j!=(tag_index+1)/2-1 || tag_index%2==0)
					{
						if(k==seg_head.getBIndex()){
							if(mention.length()==1){
								sb.append("\t");
								sb.append("HU-"+mention.getSemanticTag().getName());
								partOfSeg_head = true;
							} else {
								sb.append("\t");
								sb.append("HB-"+mention.getSemanticTag().getName());
								partOfSeg_head = true;
							}
						} else if(k==seg_head.getEIndex()-1){
							sb.append("\t");
							sb.append("HL-"+mention.getSemanticTag().getName());
							partOfSeg_head = true;
						} else if(k > seg_head.getBIndex() && k<seg_head.getEIndex()-1){
							sb.append("\t");
							sb.append("HI-"+mention.getSemanticTag().getName());
							partOfSeg_head = true;
						}
					}
					
					if(partOfSeg){
						break;
					}
					
				}

//				if(tag_index%2==0)
				{
					if(!partOfSeg){
						sb.append("O");
					}
				} 

//				if(tag_index%2==1 && j == tag_index/2-1)
				if(j!=(tag_index+1)/2-1 || tag_index%2==0)
				{
					if(!partOfSeg_head){
						sb.append("\t");
						sb.append("HO");
					}
				}
				
				sb.append("\t");
			}
			
			sb.append('\n');
		}
		
		return sb.toString();
		
	}
	
	public String toCRFPP_format_withType_BLIOU(ArrayList<SemanticTag> tags, int tag_index){
		
		ArrayList<Mention> segs = this.toTaggedSegments_sorted_withOverlappings();
		StringBuilder sb = new StringBuilder();
		
		for(int k = 0; k<this.length(); k++){
			AttributedWord word = this.getWord(k);
			sb.append(word.getName());
			sb.append('\t');
			sb.append(word.getAttribute("POS").get(0));
			sb.append('\t');
			
			for(int j = 0; j<tag_index; j++){
				SemanticTag tag = tags.get(j);
				
				boolean partOfSeg = false;
				for(int i = 0; i<segs.size(); i++){
					Mention ts = segs.get(i);
					if(!ts.getSemanticTag().equals(tag)){
						continue;
					}
					
					if(k==ts.getSegment().getBIndex()){
						if(ts.length()==1){
							sb.append("U-"+ts.getSemanticTag().getName());
							partOfSeg = true;
							break;
						} else {
							sb.append("B-"+ts.getSemanticTag().getName());
							partOfSeg = true;
							break;
						}
					} else if(k==ts.getSegment().getEIndex()-1){
						sb.append("L-"+ts.getSemanticTag().getName());
						partOfSeg = true;
						break;
					} else if(k > ts.getSegment().getBIndex() && k<ts.getSegment().getEIndex()-1){
						sb.append("I-"+ts.getSemanticTag().getName());
						partOfSeg = true;
						break;
					}
				}
				if(!partOfSeg){
					sb.append("O");
				}
				
				sb.append("\t");
			}
			
			sb.append('\n');
		}
		
		return sb.toString();
		
	}
	
	public String toCRFPP_format_withType(){
		
		ArrayList<Mention> segs = this.toTaggedSegments_sorted_all_no_overlap();
		StringBuilder sb = new StringBuilder();
		
		for(int k = 0; k<this.length(); k++){
			AttributedWord word = this.getWord(k);
			sb.append(word.getName());
			sb.append('\t');
			sb.append(word.getAttribute("POS").get(0));
			sb.append('\t');
			boolean partOfSeg = false;
			for(int i = 0; i<segs.size(); i++){
				Mention ts = segs.get(i);
				if(k==ts.getSegment().getBIndex()){
					sb.append("B-"+ts.getSemanticTag().getName());
					partOfSeg = true;
					break;
				} else if(k>ts.getSegment().getBIndex() && k<ts.getSegment().getEIndex()){
					sb.append("I-"+ts.getSemanticTag().getName());
					partOfSeg = true;
					break;
				}
			}
			if(!partOfSeg){
				sb.append("O");
			}
			sb.append('\n');
		}
		
		return sb.toString();
		
	}
	
	public String toCRFPP_format(){
		
		ArrayList<Mention> segs = this.toTaggedSegments_sorted_all_no_overlap();
		StringBuilder sb = new StringBuilder();
		
		for(int k = 0; k<this.length(); k++){
			AttributedWord word = this.getWord(k);
			sb.append(word.getName());
			sb.append('\t');
			sb.append(word.getAttribute("POS").get(0));
			sb.append('\t');
			boolean partOfSeg = false;
			for(int i = 0; i<segs.size(); i++){
				Mention ts = segs.get(i);
				if(k==ts.getSegment().getBIndex()){
					sb.append("B");
					partOfSeg = true;
					break;
				} else if(k>ts.getSegment().getBIndex() && k<ts.getSegment().getEIndex()){
					sb.append("I");
					partOfSeg = true;
					break;
				}
			}
			if(!partOfSeg){
				sb.append("O");
			}
			sb.append('\n');
		}
		
		return sb.toString();
		
	}
	
	public static int _ignored = 0;
	
	public ArrayList<Mention> toMentions_sorted_all_no_overlap(){
		ArrayList<Mention> mentions_result = new ArrayList<Mention>();
		Iterator<Segment> segments;
		segments = this._map_supported.keySet().iterator();
		while(segments.hasNext()){
			Segment segment = segments.next();
			ArrayList<Mention> mentions = this._map_supported.get(segment);
			for(Mention mention : mentions){
				mentions_result.add(mention);
			}
		}
		
		segments = this._map_not_supported.keySet().iterator();
		while(segments.hasNext()){
			Segment segment = segments.next();
			ArrayList<Mention> mentions = this._map_not_supported.get(segment);
			for(Mention mention : mentions){
				mentions_result.add(mention);
			}
		}
		
		for(int k = 0; k<mentions_result.size(); k++){
			Mention mention1 = mentions_result.get(k);
			for(int i = k+1; i<mentions_result.size(); i++){
				Mention mention2 = mentions_result.get(i);
				if(mention1.getSegment().overlapsWith(mention2.getSegment())){
					if(mention1.length()>mention2.length()){
						mentions_result.remove(k);
						k--;
						break;
					} else {
						mentions_result.remove(i);
						i--;
					}
				}
			}
		}
		
		HashMap<Segment, Mention> seg2mentions = new HashMap<Segment, Mention>();
		ArrayList<Segment> segs = new ArrayList<Segment>();
		for(int k = 0; k<mentions_result.size(); k++){
			Mention mention = mentions_result.get(k);
			seg2mentions.put(mention.getSegment(), mention);
			segs.add(mention.getSegment());
		}
		
		Collections.sort(segs);
		
		ArrayList<Mention> mentions = new ArrayList<Mention>();
		for(int k = 0; k<segs.size(); k++){
			Segment seg = segs.get(k);
			mentions.add(seg2mentions.get(seg));
		}
		
		return mentions;
	}
	
	public ArrayList<Mention> toTaggedSegments_sorted_all_no_overlap(){
		ArrayList<Mention> tagged_segs = new ArrayList<Mention>();
		Iterator<Segment> segments;
		segments = this._map_supported.keySet().iterator();
		while(segments.hasNext()){
			Segment segment = segments.next();
			ArrayList<Mention> mentions = this._map_supported.get(segment);
			for(Mention mention : mentions){
				tagged_segs.add(mention);
			}
		}
		
		segments = this._map_not_supported.keySet().iterator();
		while(segments.hasNext()){
			Segment segment = segments.next();
			ArrayList<Mention> mentions = this._map_not_supported.get(segment);
			for(Mention mention : mentions){
				tagged_segs.add(mention);
			}
		}
		
		Collections.sort(tagged_segs);
		
		for(int k = 0; k<tagged_segs.size(); k++){
			Mention ts = tagged_segs.get(k);
			if(ts.getSegment().getBIndex() == -1 || ts.getSegment().getBIndex() == this.length()){
				tagged_segs.remove(k);
				k--;
			} else if(k!=0){
				Mention ts_prev = tagged_segs.get(k-1);
				if(ts.getSegment().getBIndex()==ts_prev.getSegment().getBIndex()){
					tagged_segs.remove(k-1);
					k--;
					_ignored++;
				}
				else if(ts.getSegment().getBIndex()<ts_prev.getSegment().getEIndex()){
					tagged_segs.remove(k);
					k--;
					_ignored++;
				}
			}
		}
		
		return tagged_segs;
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<Mention> toMentions_sorted_withOverlappings(){
		ArrayList<Mention> results = new ArrayList<Mention>();
		Iterator<Segment> segments;

		ArrayList<Segment> all_segments = new ArrayList<Segment>();
		
		segments = this._map_supported.keySet().iterator();
		while(segments.hasNext()){
			Segment segment = segments.next();
			all_segments.add(segment);
		}
		
		segments = this._map_not_supported.keySet().iterator();
		while(segments.hasNext()){
			Segment segment = segments.next();
			all_segments.add(segment);
		}
		
		Collections.sort(all_segments);
		
		for(int k = 0; k<all_segments.size(); k++){
			Segment segment = all_segments.get(k);
			ArrayList<Mention> mentions;
			if(this._map_supported.containsKey(segment)){
				mentions = this._map_supported.get(segment);
			} else {
				mentions = this._map_not_supported.get(segment);
			}
			mentions = (ArrayList<Mention>)mentions.clone();
			for(int i = 0; i<mentions.size(); i++){
				Mention m1 = mentions.get(i);
				for(int j = i+1; j<mentions.size(); j++){
					Mention m2 = mentions.get(j);
					if(m1.getSemanticTag().equals(m2.getSemanticTag())){
						mentions.remove(j);
						j--;
					}
				}
			}
			results.addAll(mentions);
		}
		
		return results;
	}
	
	public ArrayList<Mention> toTaggedSegments_sorted_withOverlappings(){
		ArrayList<Mention> tagged_segs = new ArrayList<Mention>();
		Iterator<Segment> segments;

		segments = this._map_supported.keySet().iterator();
		while(segments.hasNext()){
			Segment segment = segments.next();
			ArrayList<Mention> mentions = this._map_supported.get(segment);
			for(Mention mention : mentions){
				tagged_segs.add(mention);
			}
		}

		segments = this._map_not_supported.keySet().iterator();
		while(segments.hasNext()){
			Segment segment = segments.next();
			ArrayList<Mention> mentions = this._map_not_supported.get(segment);
			for(Mention mention : mentions){
				tagged_segs.add(mention);
			}
		}
		
		Collections.sort(tagged_segs);
		return tagged_segs;
	}
	
	public ArrayList<Mention> toTaggedSegments_sorted(){
		ArrayList<Mention> tagged_segs = new ArrayList<Mention>();
		Iterator<Segment> segments = this._map_supported.keySet().iterator();
		while(segments.hasNext()){
			Segment segment = segments.next();
			ArrayList<Mention> mentions = this._map_supported.get(segment);
			for(Mention mention : mentions){
				tagged_segs.add(mention);
			}
		}
		Collections.sort(tagged_segs);
		return tagged_segs;
	}
	
	public UnlabeledTextSpan removeLabels(){
		return new UnlabeledTextSpan(this);
	}
	
	public void label(int bIndex, int eIndex, Mention m){
		HashMap<Segment, ArrayList<Mention>> map = eIndex-bIndex<= IEConfig._MAX_MENTION_LENGTH ? this._map_supported : this._map_not_supported;
		Segment seg = new Segment(bIndex, eIndex);
		if(!map.containsKey(seg))
			map.put(seg, new ArrayList<Mention>());
		ArrayList<Mention> mentions = map.get(seg);
		if(!mentions.contains(m)){
			mentions.add(m);
		}
		
		//check
		for(int i = 0; i<mentions.size(); i++)
		{
			Mention m1 = mentions.get(i);
			Mention m2 = m;
			if(m1.getSegment().equals(m2.getSegment())
					&& m1.getSemanticTag().equals(m2.getSemanticTag())){
				if(!m1.equals(m2)){
					System.err.println("[Warning] You have two mentions of the same type that share the same boundary, but have different heads.");
					for(int index=bIndex; index<eIndex; index++){
						System.err.print(this.getWord(index).getName()+" ");
					}
					System.err.println();
					
					//remove the mention with a smaller head seg
					
					Segment h_seg1 = m1.getHeadSegment();
					Segment h_seg2 = m2.getHeadSegment();
					
					Segment h_seg = null;
					
					if(h_seg1.compareTo(h_seg2)<0){
						h_seg = h_seg2;
						for(int index=h_seg.getBIndex(); index<h_seg.getEIndex(); index++){
							System.err.print(this.getWord(index).getName()+" ");
						}
						System.err.println();
						System.err.println("removed the first.");
						mentions.remove(i);
						break;
					} else if(h_seg1.compareTo(h_seg2)>0){
						h_seg = h_seg1;
						for(int index=h_seg.getBIndex(); index<h_seg.getEIndex(); index++){
							System.err.print(this.getWord(index).getName()+" ");
						}
						System.err.println();
						System.err.println("removed the second.");
						Mention m3 = mentions.get(mentions.size()-1);
						if(!m2.equals(m3)){
							throw new RuntimeException("Not possible!!!");
						} else {
							mentions.remove(mentions.size()-1);
							break;
						}
					} else {
						throw new RuntimeException("Not possible.");
					}
					
					
					
//					System.err.println(this.toString());
//					throw new RuntimeException("x");
//					System.err.println(m1.toString()+"\t"+m2.toString());
				}
			}
		}
	}

	public ArrayList<Mention> getLabels(int bIndex, int eIndex){
		return this._map_supported.get(new Segment(bIndex, eIndex));
	}

	public ArrayList<Mention> getLabels_all(int bIndex, int eIndex){
		ArrayList<Mention> results = new ArrayList<Mention>();
		ArrayList<Mention> labels_a = this._map_supported.get(new Segment(bIndex, eIndex));
		ArrayList<Mention> labels_b = this._map_not_supported.get(new Segment(bIndex, eIndex));
		if(labels_a!=null){
			results.addAll(labels_a);
		}
		if(labels_b!=null){
			results.addAll(labels_b);
		}
		return results;
	}
	
}
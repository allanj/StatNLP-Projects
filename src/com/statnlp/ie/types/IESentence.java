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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.statnlp.commons.AttributedWord;

import LBJ2.nlp.Sentence;

public class IESentence implements Serializable{
	
	private static final long serialVersionUID = -1035778497823525061L;
	
	private IEDocument _doc;
	private int _bOffset;
	private int _eOffset;
	private String _sentence_orignal;
	private Sentence _s;
	private IEWord[] _words;
	
	private HashMap<Span_T, ArrayList<Mention>> _span2Types;
	private HashMap<Span_T, ArrayList<Mention>> _intv2Types;
	
	public LabeledTextSpan toLabeledTextSpan(IEManager manager){
		
		AttributedWord[] words = new AttributedWord[this._words.length];
		for(int k = 0; k<words.length; k++){
			words[k] = new AttributedWord(this._words[k].getForm());
			words[k].addAttribute("POS", this._words[k].getPosTag());
		}
		LabeledTextSpan span = new LabeledTextSpan(words);
		
		Iterator<Span_T> segs = this._intv2Types.keySet().iterator();
		while(segs.hasNext()){
			Span_T seg = segs.next();
			ArrayList<Mention> mentions = this._intv2Types.get(seg);
			for(Mention mention : mentions){
				span.label(seg._bOffset, seg._eOffset, mention);
			}
		}
		
		return span;
		
	}
	
	public IESentence(int bOffset, int eOffset, IEDocument doc){
		this._bOffset = bOffset;
		this._eOffset = eOffset;
		this._doc = doc;
		this._sentence_orignal = doc.getText().substring(this._bOffset, this._eOffset+1);
		this._span2Types = new HashMap<Span_T, ArrayList<Mention>>();
		this._intv2Types = new HashMap<Span_T, ArrayList<Mention>>();
	}
	
	public void setIEWords(IEWord[] words){
		this._words = words;
	}
	
	public IEWord[] getIEWords(){
		return this._words;
	}
	
	public void setUIUCSentence(Sentence s){
		this._s = s;
	}
	
	public Sentence getUIUCSentence(){
		return this._s;
	}
	
	public boolean annotate(int bOffset_mention, int eOffset_mention, int bOffset_head, int eOffset_head, SemanticTag type){
		if(bOffset_mention>=this._bOffset && eOffset_mention<=this._eOffset){
			Span_T span;
			ArrayList<Mention> mentions;
			
//			mentions.add(type);
			
			int bIndex = -1;
			int eIndex = -1;
			int head_bIndex = -1;
			int head_eIndex = -1;
			for(int k = 0; k<this._words.length; k++){
				IEWord word = this._words[k];
				if(bOffset_mention>=word.getBOffset() && bOffset_mention<=word.getEOffset())
					bIndex = k;
				if(bOffset_head>=word.getBOffset() && bOffset_head<=word.getEOffset())
					head_bIndex = k;
				
				if(eOffset_mention>=word.getBOffset() && eOffset_mention<=word.getEOffset())
					eIndex = k+1;
				if(eOffset_head>=word.getBOffset() && eOffset_head<=word.getEOffset())
					head_eIndex = k+1;
			}
			if(bIndex==-1 || eIndex==-1 || head_bIndex==-1 || head_eIndex==-1){
				System.err.println(bOffset_mention);
				System.err.println(eOffset_mention);
				System.err.println(bOffset_head);
				System.err.println(eOffset_head);
				System.err.println(this.getUIUCSentence());
//				span = new Span_T(0, eOffset_mention);
//				System.err.println(span);
				
				throw new RuntimeException("bIndex="+bIndex+";eIndex="+eIndex
						+";head_bIndex="+head_bIndex+";head_eIndex="+head_eIndex);
			}
			
			Mention mention = new Mention(bIndex, eIndex, head_bIndex, head_eIndex, type);
//			System.err.println(mention+"\t"+bOffset_mention+","+eOffset_mention);

			span = new Span_T(bOffset_mention, eOffset_mention);
			if(!this._span2Types.containsKey(span))
				this._span2Types.put(span, new ArrayList<Mention>());
			mentions = this._span2Types.get(span);
//			if(mentions.contains(mention)){
////				System.err.println(mentions);
//				throw new RuntimeException("redundant:"+bOffset_mention+","+eOffset_mention+"\t"+this._doc.getFileName());
//			}
			mentions.add(mention);
			
			span = new Span_T(bIndex, eIndex);
			if(!this._intv2Types.containsKey(span))
				this._intv2Types.put(span, new ArrayList<Mention>());
			mentions = this._intv2Types.get(span);
//			if(mentions.contains(mention)){
//				System.err.println("-----");
//				System.err.println(mentions);
//				throw new RuntimeException("redundant:"+bOffset_mention+","+eOffset_mention+"\t"+this._doc.getFileName());
//			}
			mentions.add(mention);
			
			return true;
		}
		return false;
	}
	
	public IEDocument getDocument(){
		return this._doc;
	}
	
	public int getBOffset(){
		return this._bOffset;
	}
	
	public int getEOffset(){
		return this._eOffset;
	}
	
	public String getSentence(){
		return this._sentence_orignal;
	}
	
	@Override
	public int hashCode(){
		int code = this._doc.hashCode() + 7;
		code ^= this._bOffset + 7;
		code ^= this._eOffset + 7;
		return code;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof IESentence){
			IESentence sent = (IESentence)o;
			return this._doc.equals(sent._doc)
					&& this._bOffset == sent._bOffset
					&& this._eOffset == sent._eOffset;
		}
		return false;
	}
	
	@Override
	public String toString(){
		return this._bOffset+","+this._eOffset+"\t"+this._sentence_orignal;
	}
	
	public ArrayList<Mention> getMentions(int bIndex, int eIndex){
		return this._intv2Types.get(new Span_T(bIndex, eIndex));
	}
	
	private class Span_T{
		private int _bOffset;
		private int _eOffset;
		
		public Span_T(int bOffset, int eOffset){
			this._bOffset = bOffset;
			this._eOffset = eOffset;
		}
		
		@Override
		public boolean equals(Object o){
			if(o instanceof Span_T){
				Span_T span = (Span_T) o;
				return this._bOffset == span._bOffset
						&& this._eOffset == span._eOffset;
			}
			return false;
		}
		
		@Override
		public int hashCode(){
			int code = this._bOffset + 7;
			code ^= this._eOffset + 7;
			return code;
		}
	}
}

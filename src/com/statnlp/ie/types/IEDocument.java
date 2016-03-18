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
import java.util.LinkedList;
import java.util.List;

import edu.illinois.cs.cogcomp.lbj.pos.POSTagger;
import LBJ2.nlp.Sentence;
import LBJ2.nlp.SentenceSplitter;
import LBJ2.nlp.Word;
import LBJ2.nlp.seg.Token;
import LBJ2.parse.LinkedVector;

public class IEDocument implements Serializable{
	
	private static final long serialVersionUID = -948670660673282352L;
	
	private String _filename;
	private String _text_original;
	private ArrayList<IESentence> _sentences;
	
	public IEDocument(String filename, String text_original){
		this._filename = filename;
		this._text_original = text_original;
		this._sentences = new ArrayList<IESentence>();
	}
	
	public String getFileName(){
		return this._filename;
	}
	
	public boolean annotate(int bOffset_mention, int eOffset_mention, int bOffset_head, int eOffset_head, SemanticTag tag){
		for(int k = 0; k<this._sentences.size(); k++){
			IESentence sentence = this._sentences.get(k);
			if(sentence.annotate(bOffset_mention, eOffset_mention, bOffset_head, eOffset_head, tag)){
				return true;
			}
		}
		return false;
	}
	
	public String getText(){
		return this._text_original;
	}
	
	public ArrayList<IESentence> getSentences(){
		return this._sentences;
	}
	
	public void toSentences(){
		ArrayList<IESentence> sentences;
		sentences = this.first_pass();
		sentences = this.second_pass(sentences);
		this._sentences = sentences;
		this.processPOS();
	}
	
	public void processPOS(){
		POSTagger tagger = new POSTagger();
		for(int k = 0; k<this._sentences.size(); k++){
			IESentence sentence = this._sentences.get(k);
			IEWord[] words = sentence.getIEWords();
			List<Token> tokens = this.toTokens(sentence);
			if(words.length!=tokens.size())
				throw new RuntimeException("The lengths do not match.");
			
			for(int i = 0; i<words.length; i++){
				Token tok = tokens.get(i);
				String tag = tagger.discreteValue(tok);
				words[i].setPosTag(tag);
			}
		}
	}
	
	private List<Token> toTokens(IESentence sentence){
		boolean opendblquote = false;
		List<Token> uiucTokens = new LinkedList<Token>();

		IEWord[] ie_words = sentence.getIEWords();
		
		Word wprevious = null;
		Token tprevious = null;
		
		for (IEWord ie_word : ie_words) {
			String wordSurfaceForm = ie_word.getForm();

			if (wordSurfaceForm.equals("\"")) {
				wordSurfaceForm = opendblquote ? "``" : "''";
				opendblquote = !opendblquote;
			} else if (wordSurfaceForm.equals("(")) {
				wordSurfaceForm = "-LRB-";
			} else if (wordSurfaceForm.equals(")")) {
				wordSurfaceForm = "-RRB-";
			} else if (wordSurfaceForm.equals("{")) {
				wordSurfaceForm = "-LCB-";
			} else if (wordSurfaceForm.equals("}")) {
				wordSurfaceForm = "-RCB-";
			} else if (wordSurfaceForm.equals("[")) {
				wordSurfaceForm = "-LSB-";
			} else if (wordSurfaceForm.equals("]")) {
				wordSurfaceForm = "-RSB-";
			}

			Word wcurrent = new Word(wordSurfaceForm, wprevious);
			LBJ2.nlp.seg.Token tcurrent = new LBJ2.nlp.seg.Token(wcurrent,
					tprevious, "");
			uiucTokens.add(tcurrent);

			if (tprevious != null) {
				tprevious.next = tcurrent;
			}
			wprevious = wcurrent;
			tprevious = tcurrent;
		}
		
		return uiucTokens;
	}

	
	private boolean isEmpty(IESentence sent){
		String text = sent.getDocument().getText();
		int bOffset = sent.getBOffset();
		int eOffset = sent.getEOffset();
		char[] ch = text.toCharArray();
		for(int index = bOffset; index<=eOffset; index++){
			if(!Character.isSpace(ch[index])){
				return false;
			}
		}
		return true;
	}
	
	private IESentence trim(IESentence sent){
		String text = sent.getDocument().getText();
		int bOffset = sent.getBOffset();
		int eOffset = sent.getEOffset();
		char[] ch = text.toCharArray();
		while(Character.isSpace(ch[bOffset])){
			bOffset++;
		}
		while(Character.isSpace(ch[eOffset])){
			eOffset--;
		}
		return new IESentence(bOffset, eOffset, sent.getDocument());
	}
	
	private ArrayList<IESentence> first_pass(){
		ArrayList<IESentence> sentences = new ArrayList<IESentence>();
		
		char[] ch = this._text_original.toCharArray();
		int bOffset = 0;
		for(int pos = 0; pos<ch.length; pos++){
			if(pos==ch.length-1){
				IESentence sent = new IESentence(bOffset, pos, this);
				if(!isEmpty(sent)){
					sent = trim(sent);
					sentences.add(sent);
				}
			}
			else if(ch[pos]=='\n'){
				if(ch[pos+1]=='\n'){
					IESentence sent = new IESentence(bOffset, pos-1, this);
					if(!isEmpty(sent)){
						sent = trim(sent);
						sentences.add(sent);
						bOffset = pos+2;
						pos = pos+1;
					}
				}
			}
		}
		
		return sentences;
	}
	

	private ArrayList<IESentence> second_pass(ArrayList<IESentence> sents){
		
		ArrayList<IESentence> results = new ArrayList<IESentence>();
		
		IEDocument doc = sents.get(0).getDocument();
		for(int k = 0; k<sents.size(); k++){
			IESentence sent = sents.get(k);
			String[] sents_str = new String[1];
			sents_str[0] = sent.getSentence();
			
			SentenceSplitter splitter = new SentenceSplitter(sents_str);
			for (Sentence s : splitter.splitAll()) {
				IESentence sent_new = new IESentence(sent.getBOffset()+s.start, sent.getBOffset()+s.end, doc);
//				System.err.println("(1)SENT:\t"+sent_new+"\n(2)SENT:\t"+s+"\n");
				sent_new.setUIUCSentence(s);
				results.add(sent_new);
				LinkedVector words = s.wordSplit();
				IEWord[] ie_words = new IEWord[words.size()];
				for(int i = 0; i<words.size(); i++){
					ie_words[i] = new IEWord(sent.getBOffset()+words.get(i).start, sent.getBOffset()+words.get(i).end, sent);
//					System.err.println("[["+ie_words[i]+"]]"+"\t"+"{{"+words.get(i)+"}}"+"\t"+words.get(i).start+"\t"+words.get(i).end);
					if(!ie_words[i].getForm().equals(words.get(i).toString())){
						throw new RuntimeException("mismatch..");
					}
				}
				sent_new.setIEWords(ie_words);
			}
//			System.err.println();
		}
		
		return results;
		
	}
	
	private ArrayList<IESentence> second_pass_old(ArrayList<IESentence> sents){
		ArrayList<IESentence> results = new ArrayList<IESentence>();
		for(IESentence sent : sents){
			String text = sent.getDocument().getText();
			int startOffset = sent.getBOffset();
			int endOffset = sent.getEOffset();
			int bOffset = startOffset;
			for(int pos = startOffset; pos+2<= endOffset; pos++){
				if(this.isEndOfSentence(text, pos) && this.isStartOfSentence(text, pos+2)){
//					System.err.println("["+text.substring(pos-10, pos+1)+"]");
//					System.err.println("["+text.substring(pos+2, pos+10)+"]");
					IESentence sent_new = new IESentence(bOffset, pos, this);
					results.add(sent_new);
					bOffset = pos+2;
					pos = pos+1;
				}
			}
//			System.err.println(".."+bOffset+"\t"+endOffset);
			IESentence sent_new = new IESentence(bOffset, endOffset, this);
			results.add(sent_new);
		}
		return results;
		
	}

	private boolean isEndOfSentence(String s, int eOffset){
		String str;
		if(eOffset+2< s.length()){
			str = s.substring(0, eOffset+2);
			if(this.endsWithSpecialWords(str, eOffset)){
				return false;
			}
			if(str.endsWith(". ")){
				return true;
			}
			else if(str.endsWith(".\n")){
				return true;
			}
			else if(str.endsWith(".'' ")){
				return true;
			}
			else if(str.endsWith(".''\n")){
				return true;
			}
		}
		return false;
	}
	
	private boolean endsWithSpecialWords(String s, int index){
		char[] ch = s.toCharArray();
		
		if(ch[index]!='.'){
			return false;
		}
		if(index+1>=s.length() || !Character.isSpace(ch[index+1])){
			return false;
		}
		int bIndex = -1;
		for(int pos = index-1; pos>=0; pos--){
			if(Character.isSpace(ch[pos])){
				bIndex = pos;
				break;
			}
		}
		bIndex = bIndex+1;
		if(bIndex==index){
			return false;
		}
		while(!Character.isLetterOrDigit(ch[bIndex]) && bIndex<index){
			bIndex++;
		}
		if(bIndex==index){
			return false;
		}
		if(Character.isUpperCase(ch[bIndex])){
			return true;
		}
		
//		String str = s.substring(index+1);
//		String[] specialWords = new String[]{"p.m.", "a.m.", "pm.", "am."};
//		for(String word : specialWords){
//			if(str.endsWith(" "+word+" ") || str.equals(word+" ")){
//				return true;
//			}
//		}
		
		return false;
	}

	private boolean isStartOfSentence(String s, int bOffset){
		char[] ch = s.toCharArray();
		if(Character.isLowerCase(ch[bOffset])){
			return false;
		}
		if(ch[bOffset]=='`'){
			if(ch.length-bOffset>=3){
				if(ch[bOffset+1]=='`'){
					if(Character.isLowerCase(ch[bOffset+2])){
						return false;
					}
					return true;
				}
				return false;
			} else {
				return false;
			}
		}
		return true;
	}
	
	private void addSentence(IESentence sent){
		this._sentences.add(sent);
	}
	
	public ArrayList<IESentence> getIESentences(){
		return this._sentences;
	}
	
	@Override
	public int hashCode(){
		return this._filename.hashCode() + 7;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof IEDocument){
			IEDocument doc = (IEDocument)o;
			return this._filename.equals(doc._filename);
		}
		return false;
	}
	
	@Override
	public String toString(){
		return this._text_original;
	}
	
}

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
package com.statnlp.ie.util;

import java.util.LinkedList;
import java.util.List;

import LBJ2.nlp.seg.Token;
import LBJ2.nlp.Sentence;
import LBJ2.nlp.SentenceSplitter;
import LBJ2.nlp.Word;
import LBJ2.parse.LinkedVector;
import edu.illinois.cs.cogcomp.lbj.pos.POSTagger;

public class Test {
	
	void myMethod(String[] textToTag) {
		POSTagger tagger = new POSTagger();

		boolean isInQuotation = false;
		List<Token> uiucTokens = convertTextToUiucTokens(textToTag, isInQuotation);

		for (Token tok : uiucTokens) {
			// determine the POS tag of the token...
			String tag = tagger.discreteValue(tok);

			// print out (<POS> <WORD>)...
			System.out.print("(" + tag + " " + tok.form + ")");
		}

	}
	
	public static void main(String args[]){
		
		String text[] = new String[]{"This is a dog. What about that one? Mr. John said."};
		
		List<Token> tokens = convertTextToUiucTokens(text, false);
		
	}

	/**
	 * Converts a text span to LBJ2 Token objects. This simplistic example
	 * splits the text into sentences and then words, and generates a single
	 * list of the words from the concatenated sentences. This involves
	 * substituting some punctuation characters.
	 * 
	 * A fuller example would maintain sentence boundaries, and would also
	 * return the current state of the isInQuote variable.
	 * 
	 * @param text
	 *            the text to tokenize
	 * @param isInQuote
	 *            indicates whether the current text span starts within a direct
	 *            speech element.
	 * @return an ordered list of tokens for the input text
	 */

	public static List<Token> convertTextToUiucTokens(String[] text, boolean isInQuote) {
		boolean opendblquote = !isInQuote;

		List<Token> uiucTokens = new LinkedList<Token>();

		SentenceSplitter splitter = new SentenceSplitter(text);
		List<Sentence> sentences = new LinkedList<Sentence>();

		for (Sentence s : splitter.splitAll()) {
			LinkedVector words = s.wordSplit();
			
			System.err.println("sent:\t"+s);
			
			/*
			Word wprevious = null;
			Token tprevious = null;
			
			for (Token deftToken : deftSentenceTokens) {
				String wordSurfaceForm = deftToken.getValue();

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
				Token uiucTok = new Token(deftToken.getSequenceId(),
						tcurrent, deftToken.getCharOffset());
				uiucTokens.add(uiucTok);

				if (tprevious != null) {
					tprevious.next = tcurrent;
				}
				wprevious = wcurrent;
				tprevious = tcurrent;
			}
			*/
		}

		return uiucTokens;
	}

}
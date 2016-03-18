/** Statistical Natural Language Processing System
    Copyright (C) 2014-2015  Lu, Wei

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
/**
 * 
 */
package com.statnlp.cws;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @author wei_lu
 *
 */
public class CWSOutputTokenList implements Serializable{
	
	private static final long serialVersionUID = -1641569848371670637L;
	
	private ArrayList<CWSOutputToken> _tokens;
	
	private CWSOutputTokenList _prefix;//excluded the last token.
	private CWSOutputTokenList _suffix;//excluded the first token.
	
	public CWSOutputTokenList(ArrayList<CWSOutputToken> tokens){
		this._tokens = tokens;
	}
	
	public ArrayList<CWSOutputToken> getTokens(){
		return this._tokens;
	}
	
	public int size(){
		return this._tokens.size();
	}
	
	public boolean containsOnlyStartNodes(){
		for(int k = 0; k<this._tokens.size(); k++){
			if(!this._tokens.get(k).equals(CWSOutputToken._START)){
				return false;
			}
		}
		return true;
	}
	
	public CWSOutputTokenList getPrefix(){
		if(this._prefix!=null) 
			return this._prefix;
		ArrayList<CWSOutputToken> prefix_tokens = new ArrayList<CWSOutputToken>();
		for(int k = 0; k<this.size()-1; k++){
			prefix_tokens.add(this.getTokens().get(k));
		}
		this._prefix = new CWSOutputTokenList(prefix_tokens);
		return this._prefix;
	}
	
	public CWSOutputTokenList getSuffix(){
		if(this._suffix!=null) 
			return this._suffix;
		ArrayList<CWSOutputToken> suffix_tokens = new ArrayList<CWSOutputToken>();
		for(int k = 1; k<this.size(); k++){
			suffix_tokens.add(this.getTokens().get(k));
		}
		this._suffix = new CWSOutputTokenList(suffix_tokens);
		return this._suffix;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof CWSOutputTokenList){
			if(this._tokens.size()!=((CWSOutputTokenList)o)._tokens.size()){
				return false;
			}
			for(int k = 0; k<this._tokens.size(); k++){
				if(!this._tokens.get(k).equals(((CWSOutputTokenList)o)._tokens.get(k))){
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		int code = 7;
		for(int k = 0; k<this._tokens.size(); k++){
			code ^= this._tokens.get(k).hashCode() + 7;
		}
		return code;
	}
	
	@Override
	public String toString() {
		return this._tokens.toString();
	}

}

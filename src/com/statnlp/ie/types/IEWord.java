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

public class IEWord implements Serializable{
	
	private static final long serialVersionUID = 198127035327218273L;
	
	private IESentence _sent;
	private int _bOffset;
	private int _eOffset;
	private String _form;
	private String _posTag;
	
	public IEWord(int bOffset, int eOffset, IESentence sent){
		this._sent = sent;
		this._bOffset = bOffset;
		this._eOffset = eOffset;
		this._form = sent.getDocument().getText().substring(this._bOffset, this._eOffset+1);
	}
	
	public String getPosTag(){
		return this._posTag;
	}
	
	public void setPosTag(String posTag){
		this._posTag = posTag;
	}
	
	public int getBOffset(){
		return this._bOffset;
	}
	
	public int getEOffset(){
		return this._eOffset;
	}
	
	public String getForm(){
		return this._form;
	}
	
	public IESentence getIESentence(){
		return this._sent;
	}
	
	@Override
	public String toString(){
		return this._bOffset+","+this._eOffset+":"+this._form;
	}
	
}
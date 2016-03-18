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
package com.statnlp.commons.ml.classification.logisticregression;

import java.io.Serializable;

public class OutputLabel implements Serializable{
	
	private static final long serialVersionUID = -3117239519201360941L;
	
	private String _form;
	private int _id;
	
	public OutputLabel(String form){
		this._form = form;
	}
	
	public void setId(int id){
		this._id = id;
	}
	
	public int getId(){
		return this._id;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof OutputLabel){
			OutputLabel output = (OutputLabel)o;
			return this._form.equals(output._form);
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return this._form.hashCode() + 7;
	}
	
	@Override
	public String toString(){
		return this._form;
	}
	
}
